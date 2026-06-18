package com.xxr.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xxr.properties.EmbeddingProperties;
import com.xxr.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingProperties props;
    private OkHttpClient client;

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final int DIMENSION = 1024;
    private static final int API_MAX_BATCH = 10;
 
    public EmbeddingServiceImpl(EmbeddingProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        if (texts.size() <= API_MAX_BATCH) {
            return doEmbedBatch(texts);
        }
        List<float[]> all = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += API_MAX_BATCH) {
            int end = Math.min(i + API_MAX_BATCH, texts.size());
            all.addAll(doEmbedBatch(texts.subList(i, end)));
        }
        return all;
    }
 
    private List<float[]> doEmbedBatch(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();

     Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("input", texts);
        body.put("encoding_format", "float");

        String jsonBody = JSON.toJSONString(body);
        Request request = new Request.Builder()
                .url(props.getApiUrl())
                .header("Authorization", "Bearer " + props.getApiKey())
                .post(RequestBody.create(jsonBody, JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("Embedding API returned {}: {}", response.code(), errBody);
                return vectors;
            }

            String respBody = response.body().string();
            JSONObject respJson = JSON.parseObject(respBody);
            JSONArray dataArray = respJson.getJSONArray("data");
            if (dataArray == null) {
                log.error("Embedding API response missing 'data' field: {}", respBody);
                return vectors;
            }

            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject item = dataArray.getJSONObject(i);
                JSONArray embeddingArray = item.getJSONArray("embedding");
                float[] vec = new float[embeddingArray.size()];
                for (int j = 0; j < embeddingArray.size(); j++) {
                    vec[j] = embeddingArray.getFloatValue(j);
                }
                vectors.add(vec);
            }
        } catch (IOException e) {
            log.error("Embedding API call failed: {}", e.getMessage(), e);
        }
        return vectors;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }
}
