import request from "./request";

export function getDocumentList(params: {
  page: number;
  pageSize: number;
  kbId?: number;
  keyword?: string;
  fileType?: string;
  parseStatus?: number;
}) {
  return request.get<any, { code: number; errorMessage: string; data: any }>("/api/v1/document/documents", { params });
}

export function uploadDocument(kbId: string | number, file: File) {
  var formData = new FormData();
  formData.append("file", file);
  formData.append("kbId", String(kbId));
  return request.post("/api/v1/document/uploadDocument", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export function deleteDocument(docId: string | number) {
  return request.delete("/api/v1/document/" + docId);
}

export function getDocumentDetail(id: string | number) {
  return request.get("/api/v1/document/" + id);
}

export function getDocumentPreviewUrl(docId: string | number, expireSeconds?: number) {
  return request.get("/api/v1/document/documents/preview/" + docId, {
    params: expireSeconds ? { expire_seconds: expireSeconds } : {},
  });
}

export function updateDocumentTitle(docId: string | number, title: string) {
  return request.post("/api/v1/document/updateDocumentTitle/" + docId, { title: title });
}

export function reparseDocument(docId: string | number) {
  return request.post("/api/v1/document/" + docId + "/reparse");
}

/** 根据知识库ID查询所属文档列表（不分页） */
export function getDocumentsByKbId(kbId: string | number) {
  return request.get<any, { code: number; errorMessage: string; data: any[] }>("/api/v1/document/kb/" + kbId);
}
