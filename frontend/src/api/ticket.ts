import request from "./request";

export function getTicketList(params: {
  page: number;
  pageSize: number;
  status?: number;
}) {
  return request.get<any, { code: number; errorMessage: string; data: any; total: number }>("/api/v1/ticket/list", { params });
}

export function createTicket(data: { kbId: number; question: string; remark?: string }) {
  return request.post("/api/v1/ticket/create", data);
}

export function resolveTicket(id: number, data: { reply?: string; status: number }) {
  return request.put("/api/v1/ticket/" + id + "/resolve", data);
}
