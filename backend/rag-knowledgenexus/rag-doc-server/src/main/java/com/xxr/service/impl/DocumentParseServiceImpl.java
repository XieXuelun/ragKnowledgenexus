package com.xxr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.constant.DocumentParseStatusConstants;
import com.xxr.document.pojo.DocChunk;
import com.xxr.document.pojo.DocDocument;
import com.xxr.enums.EmbedStatusEnum;
import com.xxr.mapper.ChunkMapper;
import com.xxr.mapper.DocumentMapper;
import com.xxr.service.ChunkService;
import com.xxr.service.DocumentParseService;
import com.xxr.service.VectorizationService;
import com.xxr.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentParseServiceImpl implements DocumentParseService {
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private ChunkService chunkService;
    @Autowired
    private ChunkMapper chunkMapper;
    @Autowired
    private VectorizationService vectorizationService;
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP_SIZE = 50;
    /**
     * 异步解析文档
     *
     * @param document 文档
     */
    @Override
    @Async("documentParseExecutor")
    public void parseDocumentAsync(DocDocument  document) {
        //校验参数
        if (document == null) {
            throw new IllegalArgumentException("文档不能为空");
        }
        log.info("开始异步解析文档: docId={}", document.getId());
        try {
            parseDocument(document);
        } catch (Exception e) {
            log.error("异步解析文档失败: docId={}, error={}", document.getId(), e.getMessage(), e);
        }


    }

    /**
     * 重新解析文档
     * @param docDocument
     */
    @Override
    /*@Async("documentParseExecutor")*/  // 异步执行
    public void reparseDocument(DocDocument docDocument) {
        //删除旧的分片
        chunkMapper.deleteByDocId(docDocument.getId());
        //更新解析状态
        docDocument.setParseStatus(DocumentParseStatusConstants.PARSING);
        docDocument.setParseError(null);
        docDocument.setUpdateTime(LocalDateTime.now());
        //更新文档信息
        documentMapper.updateById(docDocument);
        //解析文档
        parseDocument(docDocument);

    }

    private ResponseResult parseDocument(DocDocument document) {
        try {
            InputStream inputStream = minioUtil.getObject(document.getMinioPath());
            if (inputStream == null) {
                document.setParseStatus(DocumentParseStatusConstants.FAILED);
                document.setParseError("无法读取文件");
                documentMapper.updateById(document);
                return ResponseResult.errorResult(AppHttpCodeEnum.OPERATION_ERROR, "无法读取文件");
            }
            //获取文件类型
            String content;//文档内容
            String fileType = document.getFileType();
            if ("pdf".equalsIgnoreCase(fileType)) {
                content = parsePdf(inputStream);
            } else if ("docx".equalsIgnoreCase(fileType) || "doc".equalsIgnoreCase(fileType)) {
                content = parseDocx(inputStream);
            } else if ("txt".equalsIgnoreCase(fileType)) {
                content = parseTxt(inputStream);
            } else {
                document.setParseStatus(DocumentParseStatusConstants.FAILED);
                document.setParseError("不支持的文件类型: " + fileType);
                documentMapper.updateById(document);
                return ResponseResult.errorResult(AppHttpCodeEnum.OPERATION_ERROR, "不支持的文件类型");
            }
            //分片
            List<DocChunk> chunks = semanticChunking(content, document.getId(), document.getKbId());
            //保存分片
            saveChunks(chunks);
            //更新文档状态
            document.setParseStatus(DocumentParseStatusConstants.SUCCESS);
            document.setParseError(null);
            document.setChunkCount(chunks.size());
            document.setWordCount(content.length());
            documentMapper.updateById(document);
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

        } catch (Exception e) {
            document.setParseStatus(DocumentParseStatusConstants.FAILED);
            document.setParseError(e.getMessage());
            documentMapper.updateById(document);
            return ResponseResult.errorResult(AppHttpCodeEnum.OPERATION_ERROR, "解析失败: " + e.getMessage());
        }

    }
    private String parsePdf(InputStream inputStream) throws Exception {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\t");
                        }
                    }
                    content.append("\n");
                }
            }

            return content.toString();
        }
    }

    private String parseTxt(InputStream inputStream) throws Exception {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     *
     * 分块策略：
     *
     *   按中文标点 (。！？\n) 切分成"段落"
     *   遍历段落，累积到当前块
     *   当 currentChunk > 500 字符 → 生成一个 chunk
     *   保留尾部 50 字符作为 overlap（上下文重叠）
     *   继续下一个块...
     * @param content
     * @param docId
     * @param kbId
     * @return
     */
    private List<DocChunk> semanticChunking(String content, Long docId, Long kbId) {
        List<DocChunk> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        int chunkIndex = 0;
        int pageNum = 1;

        // 第一层：按自然段落切分（双换行 = 作者已分好的段落边界）
        String[] paragraphs = content.split("\\n\\s*\\n");

        for (String paragraph : paragraphs) {
            paragraph = normalizeWhitespace(paragraph);
            if (paragraph.isEmpty()) {
                continue;
            }

            // 段落不超限，直接作为一个分片
            if (paragraph.length() <= CHUNK_SIZE) {
                chunks.add(createChunk(paragraph, docId, kbId, chunkIndex++, pageNum));
                continue;
            }

            // 段落超限，第二层：按句子切分后逐句组块
            String[] sentences = paragraph.split("(?<=[。！？；?!;])");
            StringBuilder buffer = new StringBuilder();

            for (String sentence : sentences) {
                sentence = normalizeWhitespace(sentence);
                if (sentence.isEmpty()) {
                    continue;
                }

                // 追加到当前缓冲容器
                buffer.append(sentence);

                // 后置检查：追加后可能超限，需要拆解
                while (buffer.length() > CHUNK_SIZE) {
                    // 从 CHUNK_SIZE 位置往回找最近的合法切分点
                    int cutPos = findCutPosition(buffer.toString());
                    String chunkText = buffer.substring(0, cutPos + 1);
                    chunks.add(createChunk(chunkText, docId, kbId, chunkIndex++, pageNum));

                    // 保留 overlap 作为下一块的起点
                    int keepStart = Math.max(0, cutPos + 1 - OVERLAP_SIZE);
                    String remainder = buffer.substring(keepStart);
                    buffer = new StringBuilder(normalizeWhitespace(remainder));
                }
            }

            // 段落末尾余量
            String finalText = normalizeWhitespace(buffer.toString());
            if (!finalText.isEmpty()) {
                chunks.add(createChunk(finalText, docId, kbId, chunkIndex++, pageNum));
            }
        }

        return chunks;
    }



    /**
     * 从后往前在 CHUNK_SIZE 附近找最近的句子边界作为切分点。
     * 优先在标点处切，找不到则退回一半位置硬切。
     */
    private int findCutPosition(String text) {
        int pos = CHUNK_SIZE;
        // 兜底：至少回退到一半位置
        int floor = CHUNK_SIZE / 2;

        while (pos > floor) {
            char c = text.charAt(pos);
            if (c == '。' || c == '！' || c == '？' || c == '；'
                    || c == '.' || c == '!' || c == '?' || c == ';'
                    || c == '\n') {
                return pos;
            }
            pos--;
        }
        // 没有找到标点，在 CHUNK_SIZE 处硬切
        return CHUNK_SIZE;
    }

    /**
     * 空白归一化：全角空格 + 连续空白符 → 单个空格，首尾 trim。
     */
    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\s\u3000]+", " ").trim();
    }



    private DocChunk createChunk(String content, Long docId, Long kbId, int index, int pageNum) {
        DocChunk chunk = new DocChunk();
        chunk.setDocId(docId);
        chunk.setKbId(kbId);
        chunk.setChunkIndex(index);
        chunk.setContent(content.trim());
        chunk.setContentLength(content.length());
        chunk.setEmbedStatus(EmbedStatusEnum.PENDING.getCode());
        chunk.setPageNum(pageNum);
        chunk.setIsDeleted(DeleteConstants.NOT_DELETED);
        chunk.setCreateTime(LocalDateTime.now());
        return chunk;
    }

    private void saveChunks(List<DocChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        Long docId = chunks.get(0).getDocId();
        //批量插入分片
        chunkService.saveBatch(chunks);
        //异步向量化
        vectorizationService.vectorizeByDocId(docId);

    }
}

