import request from "./request";

export function login(data: { username: string; password: string }) {
  return request.post<any, { code: number; errorMessage: string; data: { token: string; user: any } }>("/api/v1/user/login", data);
}

export function logout() {
  return request.post("/api/v1/user/logout");
}

export function register(data: { username: string; password: string; realName?: string; email?: string; role?: number; deptId?: number }) {
  return request.post("/api/v1/user/register", data);
}

export function refreshToken() {
  return request.post("/api/v1/user/refresh");
}

