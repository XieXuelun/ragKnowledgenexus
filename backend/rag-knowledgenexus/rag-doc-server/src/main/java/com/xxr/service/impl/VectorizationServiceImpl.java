package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xxr.document.pojo.DocChunk;
import com.xxr.enums.EmbedStatusEnum;
import com.xxr.mapper.ChunkMapper;
import com.xxr.service.EmbeddingService;
import com.xxr.service.QdrantIndexService;
import com.xxr.service.VectorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorizationServiceImpl implements VectorizationService {

    private final ChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final QdrantIndexService qdrantIndexService;

    private static final int BATCH_SIZE = 25;

    @Override
    @Async("documentParseExecutor")
    public void vectorizeByDocId(Long docId) {
        log.info("Start vectorizing docId={}", docId);
        List<DocChunk> pending = selectPending(docId);
        if (pending.isEmpty()) {
            log.info("No pending chunks for docId={}", docId);
            return;
        }
        processChunks(pending);
    }

    private void processChunks(List<DocChunk> chunks) {
        Long kbId = chunks.get(0).getKbId();
        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, chunks.size());
            List<DocChunk> batch = chunks.subList(i, end);
            try {
                List<String> texts = batch.stream().map(DocChunk::getContent).collect(Collectors.toList());
                List<float[]> vectors = embeddingService.embedBatch(texts);
                if (vectors.size() != batch.size()) {
                    log.error("Embedding size mismatch: expected {} got {}", batch.size(), vectors.size());
                    markFailed(batch);
                    continue;
                }
                // Build chunkId -> vector map and chunkId -> chunk map
                Map<Long, float[]> vectorMap = new LinkedHashMap<>();
                Map<Long, DocChunk> chunkMap = new LinkedHashMap<>();
                for (int j = 0; j < batch.size(); j++) {
                    DocChunk chunk = batch.get(j);
                    vectorMap.put(chunk.getId(), vectors.get(j));
                    chunkMap.put(chunk.getId(), chunk);
                }
                // Write to Qdrant
                Map<Long, String> qdrantPointIdMap = qdrantIndexService.upsertVectors(kbId, vectorMap, chunkMap);
                markSuccess(batch, qdrantPointIdMap);
                log.info("Batch {}/{} done: {} chunks, kbId={}, Qdrant OK",
                        (i / BATCH_SIZE + 1),
                        (int) Math.ceil((double) chunks.size() / BATCH_SIZE),
                        batch.size(), kbId);
            } catch (Exception e) {
                log.error("Batch vectorization failed for kbId={}: {}", kbId, e.getMessage(), e);
                markFailed(batch);
            }
        }
    }

    private void markFailed(List<DocChunk> batch) {
        List<Long> ids = batch.stream().map(DocChunk::getId).collect(Collectors.toList());
        LambdaUpdateWrapper<DocChunk> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(DocChunk::getEmbedStatus, EmbedStatusEnum.FAILED.getCode())
                .in(DocChunk::getId, ids);
        chunkMapper.update(null, wrapper);
    }

    private void markSuccess(List<DocChunk> batch, Map<Long, String> pointIdMap) {
        for (DocChunk c : batch) {
            String qdrantId = pointIdMap.get(c.getId());
            if (qdrantId != null) {
                c.setVectorId(qdrantId);
                c.setEmbedStatus(EmbedStatusEnum.SUCCESS.getCode());
                chunkMapper.updateById(c);
            } else {
                c.setEmbedStatus(EmbedStatusEnum.FAILED.getCode());
                chunkMapper.updateById(c);
            }
        }
    }

    private List<DocChunk> selectPending(Long docId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<DocChunk>()
                .eq(DocChunk::getDocId, docId)
                .eq(DocChunk::getEmbedStatus, EmbedStatusEnum.PENDING.getCode()));
    }
}