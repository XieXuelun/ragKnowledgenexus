package com.xxr.service;

import com.xxr.document.pojo.DocChunk;
import java.util.List;
import java.util.Map;

public interface QdrantIndexService {

    /**
     * Upsert vectors and payloads to Qdrant.
     * @param kbId knowledge base ID (collection = kb_{kbId})
     * @param chunkVectorMap chunkId -> float[] vector (1024D)
     * @param chunkMap chunkId -> DocChunk (full chunk for payload metadata)
     * @return chunkId -> qdrantPointId mapping
     */
    Map<Long, String> upsertVectors(Long kbId, Map<Long, float[]> chunkVectorMap,
                                    Map<Long, DocChunk> chunkMap);

    /**
     * Search similar vectors in Qdrant by kbId.
     */
    List<SearchResult> search(Long kbId, float[] queryVector, int topK);

    /** Delete entire collection for a knowledge base */
    void deleteByKbId(Long kbId);

    /** Delete points for a specific document */
    void deleteByDocId(Long docId);

    class SearchResult {
        private final Long chunkId;
        private final float score;

        public SearchResult(Long chunkId, float score) {
            this.chunkId = chunkId;
            this.score = score;
        }

        public Long getChunkId() { return chunkId; }
        public float getScore() { return score; }
    }
}