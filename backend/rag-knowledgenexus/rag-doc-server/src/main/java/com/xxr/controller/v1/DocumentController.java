package com.xxr.controller.v1;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.document.dtos.DocumentQueryDto;
import com.xxr.service.DocumentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/document")
@Api(tags = "文档管理")
public class DocumentController {
    @Autowired
    private DocumentService documentService;

    /**
     * 获取知识库中的文档列表
     * @param documentQueryDto
     * @return
     */
    @GetMapping("/documents")
    @ApiOperation("获取知识库中的文档列表")
    public ResponseResult getDocumentList(DocumentQueryDto documentQueryDto) {
        return documentService.getList(documentQueryDto);

    }

    /**
     * 上传文档
     */
    @PostMapping("/uploadDocument")
    @ApiOperation("上传知识库的文档")
    public ResponseResult uploadDocument (
            @RequestParam(value = "kbId") Long kbId,
            @RequestParam("file") MultipartFile file)  throws Exception {
        return documentService.uploadDocument(file,kbId);
    }
    @GetMapping("/{id}")
    @ApiOperation("查询文档详情")
    public ResponseResult getDocumentDetail(@PathVariable Long id) {
        return documentService.getDocumentDetail(id);
    }

    /**
     * 查询文档预览地址
     * @return
     */
    @GetMapping("/documents/preview/{doc_id}")
    @ApiOperation("获取文档预览地址")
    public ResponseResult getDocumentPreview(
            @PathVariable("doc_id") Long docId,
            @RequestParam(value = "expire_seconds", required = false) Integer expireSeconds) throws Exception {
        return documentService.getDocumentPreviewUrl(docId, expireSeconds);}
    @PostMapping("/updateDocumentTitle/{doc_id}")
    @ApiOperation("更新文档标题")
    public ResponseResult updateDocumentTitle(@PathVariable("doc_id") Long docId, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        return documentService.updateDocumentTitle(docId, title);}

    /**
     * 删除文档信息，逻辑删除
     * @param docId
     * @return
     */
    @DeleteMapping("/{doc_id}")
    @ApiOperation("删除文档")
    public ResponseResult deleteDocument(@PathVariable("doc_id") Long docId)  {
        return documentService.deleteDocument(docId);
    }

    /**
     * 重新解析文档
     * @param docId
     * @return
     */
    @PostMapping("/{doc_id}/reparse")
    @ApiOperation("重新解析文档")
    public ResponseResult reparseDocument(@PathVariable("doc_id") Long docId) {
        return documentService.reparseDocument(docId);
    }
    /**
     * 查询知识库所属文档
     *
     */
    @GetMapping("/kb/{kb_id}")
    @ApiOperation("查询知识库所属文档")
    public ResponseResult selectbyKbId(@PathVariable("kb_id")Long kbId){
        return documentService.selectbyKbId(kbId);
    }

}
