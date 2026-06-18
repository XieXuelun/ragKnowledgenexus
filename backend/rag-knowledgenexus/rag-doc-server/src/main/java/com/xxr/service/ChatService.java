package com.xxr.service;

import com.xxr.qa.dto.ChatResult;

public interface ChatService {
    ChatResult chat(String systemPrompt, String userMessage);
}