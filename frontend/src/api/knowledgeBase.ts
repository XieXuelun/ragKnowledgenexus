import request from "./request";

export function getKBList(params: {
  page?: number;
  pageSize?: number;
  keyword?: string;
  deptId?: number;
  visibility?: number;
  sortBy?: string;
  sortOrder?: string;
}) {
  return request.get<any, { code: number; errorMessage: string; data: any }>("/api/v1/user/knowledge-base/list", { params });
}

export function getKBDetail(id: string | number) {
  return request.get("/api/v1/user/knowledge-base/" + id);
}

export function createKB(data: { name: string; description?: string; visibility?: number; icon?: string }) {
  return request.post("/api/v1/user/knowledge-base/create", data);
}

export function updateKB(data: { id: string | number; name?: string; description?: string; visibility?: number }) {
  return request.post("/api/v1/user/knowledge-base/update", data);
}

export function deleteKB(id: string | number) {
  return request.delete("/api/v1/user/knowledge-base/" + id);
}

export function getKBNameList() {
  return request.get("/api/v1/user/knowledge-base/name");
}

export function favoriteKB(kbId: string | number) {
  return request.put("/api/v1/user/knowledge-base/" + kbId + "/favorite");
}
