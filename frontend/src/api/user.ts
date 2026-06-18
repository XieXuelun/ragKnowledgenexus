import request from "./request";

export function getUserInfo() {
  return request.get<any, { code: number; errorMessage: string; data: any }>("/api/v1/user/info");
}

export function getUserList(params: { page: number; pageSize: number; keyword?: string; status?: number; deptId?: number }) {
  return request.get<any, { code: number; errorMessage: string; data: any }>("/api/v1/user/page", { params });
}

export function getDepartmentTree(parentId?: number) {
  return request.get<any, { code: number; errorMessage: string; data: any[] }>("/api/v1/department/tree", {
    params: parentId ? { parentId } : {},
  });
}

export function updateUser(data: any) {
  return request.post("/api/v1/user/update", data);
}

export function deleteUser(id: number) {
  return request.delete("/api/v1/user/delete/" + id);
}

export function setStatus(data: { id: number; status: number }) {
  return request.post("/api/v1/user/status", data);
}

export function uploadAvatar(file: File) {
  var fd = new FormData();
  fd.append("file", file);
  return request.post("/api/v1/user/avatar", fd, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}
