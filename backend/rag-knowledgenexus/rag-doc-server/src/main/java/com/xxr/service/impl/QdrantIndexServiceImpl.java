package com.xxr.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xxr.document.pojo.DocChunk;
import com.xxr.properties.QdrantProperties;
import com.xxr.service.QdrantIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantIndexServiceImpl implements QdrantIndexService {

    private final QdrantProperties props;
    private OkHttpClient client;
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Map<Long, String> upsertVectors(Long kbId, Map<Long, float[]> chunkVectorMap,
                                           Map<Long, DocChunk> chunkMap) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (chunkVectorMap.isEmpty()) return result;

        String collectionName = collectionName(kbId);
        try {
            ensureCollection(collectionName);

            JSONArray pointsArray = new JSONArray();
            for (Map.Entry<Long, float[]> entry : chunkVectorMap.entrySet()) {
                Long chunkId = entry.getKey();
                float[] vector = entry.getValue();
                DocChunk chunk = chunkMap.get(chunkId);

                JSONArray vectorArray = new JSONArray();
                for (float v : vector) vectorArray.add(v);

                JSONObject payload = new JSONObject();
                payload.put("chunk_id", chunkId);
                if (chunk != null) {
                    payload.put("kb_id", chunk.getKbId());
                    payload.put("doc_id", chunk.getDocId());
                    payload.put("chunk_index", chunk.getChunkIndex());
                    String content = chunk.getContent();
                    if (content != null && !content.isEmpty()) {
                        payload.put("content", content.length() > 500 ? content.substring(0, 500) : content);
                    }
                }

                JSONObject point = new JSONObject();
                point.put("id", chunkId);
                point.put("vector", vectorArray);
                point.put("payload", payload);
                pointsArray.add(point);
            }

            JSONObject body = new JSONObject();
            body.put("points", pointsArray);

            String url = baseUrl() + "/collections/" + collectionName + "/points?wait=true";
            String respBody = doPut(url, body.toJSONString());
            log.debug("Qdrant upsert response: {}", respBody);

            for (Long chunkId : chunkVectorMap.keySet()) {
                result.put(chunkId, String.valueOf(chunkId));
            }
            log.info("Upserted {} vectors to Qdrant [{}]", chunkVectorMap.size(), collectionName);
        } catch (Exception e) {
            log.error("Qdrant upsert failed [{}]: {}", collectionName, e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<SearchResult> search(Long kbId, float[] queryVector, int topK) {
        List<SearchResult> results = new ArrayList<>();
        String collectionName = collectionName(kbId);
        try {
            JSONArray vectorArray = new JSONArray();
            for (float v : queryVector) vectorArray.add(v);

            JSONObject body = new JSONObject();
            body.put("vector", vectorArray);
            body.put("limit", topK);
            body.put("with_payload", true);

            // Filter by kbId in payload
            JSONObject filter = new JSONObject();
            JSONArray must = new JSONArray();
            JSONObject match = new JSONObject();
            match.put("key", "kb_id");
            JSONObject matchValue = new JSONObject();
            matchValue.put("value", kbId);
            match.put("match", matchValue);
            must.add(match);
            filter.put("must", must);
            body.put("filter", filter);

            String url = baseUrl() + "/collections/" + collectionName + "/points/search";
            String respBody = doPost(url, body.toJSONString());
            JSONObject resp = JSON.parseObject(respBody);
            JSONArray resultArray = resp.getJSONArray("result");
            if (resultArray != null) {
                for (int i = 0; i < resultArray.size(); i++) {
                    JSONObject item = resultArray.getJSONObject(i);
                    long pointId = item.getLongValue("id");
                    float score = item.getFloatValue("score");
                    results.add(new SearchResult(pointId, score));
                }
            }
            log.debug("Qdrant search on [{}] returned {} results", collectionName, results.size());
        } catch (Exception e) {
            log.error("Qdrant search failed [{}]: {}", collectionName, e.getMessage(), e);
        }
        return results;
    }

    @Override
    public void deleteByKbId(Long kbId) {
        String collectionName = collectionName(kbId);
        try {
            doDelete(baseUrl() + "/collections/" + collectionName);
            log.info("Deleted Qdrant collection [{}]", collectionName);
        } catch (Exception e) {
            log.warn("Failed to delete Qdrant collection [{}]: {}", collectionName, e.getMessage());
        }
    }

    @Override
    public void deleteByDocId(Long docId) {
        log.warn("deleteByDocId({}) not implemented per-doc — delete collection by kbId or manage via application layer", docId);
    }

    private void ensureCollection(String collectionName) throws IOException {
        Request checkReq = new Request.Builder()
                .url(baseUrl() + "/collections/" + collectionName)
                .get().build();
        try (Response resp = client.newCall(checkReq).execute()) {
            if (resp.isSuccessful()) return;
        }

        JSONObject vectorsConfig = new JSONObject();
        vectorsConfig.put("size", props.getDimension());
        vectorsConfig.put("distance", "Cosine");

        JSONObject body = new JSONObject();
        body.put("vectors", vectorsConfig);

        String url = baseUrl() + "/collections/" + collectionName;
        String respBody = doPut(url, body.toJSONString());
        log.info("Created Qdrant collection [{}] (dim={}, distance=Cosine) resp={}", collectionName, props.getDimension(), respBody);
    }

    private String baseUrl() {
        return (props.isUseTls() ? "https" : "http") + "://" + props.getHost() + ":" + props.getHttpPort();
    }

    private static String collectionName(Long kbId) {
        return "kb_" + kbId;
    }

    private String doPut(String url, String body) throws IOException {
        Request request = new Request.Builder()
                .url(url).put(RequestBody.create(body, JSON_TYPE)).build();
        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful())
                throw new IOException("Qdrant PUT " + url + " failed: " + response.code() + " " + respBody);
            return respBody;
        }
    }

    private String doPost(String url, String body) throws IOException {
        Request request = new Request.Builder()
                .url(url).post(RequestBody.create(body, JSON_TYPE)).build();
        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful())
                throw new IOException("Qdrant POST " + url + " failed: " + response.code() + " " + respBody);
            return respBody;
        }
    }

    private String doDelete(String url) throws IOException {
        Request request = new Request.Builder().url(url).delete().build();
        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful())
                throw new IOException("Qdrant DELETE " + url + " failed: " + response.code() + " " + respBody);
            return respBody;
        }
    }
}