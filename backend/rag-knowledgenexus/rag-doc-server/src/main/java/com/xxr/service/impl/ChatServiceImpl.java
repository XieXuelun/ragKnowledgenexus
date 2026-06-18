package com.xxr.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xxr.properties.ChatProperties;
import com.xxr.qa.dto.ChatResult;
import com.xxr.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatProperties props;
    private OkHttpClient client;
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    public ChatServiceImpl(ChatProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public ChatResult chat(String systemPrompt, String userMessage) {
        long start = System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("model", props.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        Request request = new Request.Builder()
                .url(props.getApiUrl())
                .header("Authorization", "Bearer " + props.getApiKey())
                .post(RequestBody.create(body.toJSONString(), JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            long elapsed = System.currentTimeMillis() - start;

            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                log.error("Chat API returned {}: {}", response.code(), err);
                return new ChatResult("抱歉，AI 服务暂时不可用。", 0, props.getModel(), elapsed);
            }
            String respBody = response.body().string();
            JSONObject resp = JSON.parseObject(respBody);
            JSONArray choices = resp.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("Chat API response missing choices: {}", respBody);
                return new ChatResult("抱歉，生成回答失败。", 0, props.getModel(), elapsed);
            }

            String content = choices.getJSONObject(0).getJSONObject("message").getString("content");

            // Parse token usage from response
            int tokenUsage = 0;
            JSONObject usage = resp.getJSONObject("usage");
            if (usage != null) {
                tokenUsage = usage.getIntValue("total_tokens", 0);
            }

            // Use model from response if available, fallback to configured model
            String model = resp.getString("model");
            if (model == null || model.isEmpty()) {
                model = props.getModel();
            }

            log.info("Chat API success: model={}, tokens={}, time={}ms", model, tokenUsage, elapsed);
            return new ChatResult(content, tokenUsage, model, elapsed);
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Chat API call failed: {}", e.getMessage(), e);
            return new ChatResult("抱歉，请求 AI 服务时发生错误。", 0, props.getModel(), elapsed);
        }
    }
}