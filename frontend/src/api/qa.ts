import request from "./request";

export function createConversation(kbId: number, title?: string) {
  return request.post<any, { code: number; errorMessage: string; data: any }>("/api/v1/qa/conversation/create", {
    kbId,
    title: title || "",
  });
}

export function askQuestion(data: {
  conversationId: number;
  question: string;
  searchType?: string;
  topK?: number;
}) {
  return request.post<any, { code: number; errorMessage: string; data: any }>("/api/v1/qa/ask", data);
}

export function getConversationList(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<any, { code: number; errorMessage: string; data: any }>("/api/v1/qa/conversation/list", { params });
}

export function getConversationDetail(conversationId: number) {
  return request.get("/api/v1/qa/conversation/detail/" + conversationId);
}

export function feedback(messageId: number, score: number) {
  return request.post("/api/v1/qa/feedback", { messageId: messageId, score: score });
}
