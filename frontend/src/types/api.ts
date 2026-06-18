import type { AxiosResponse } from "axios";

export interface ApiResponse<T = unknown> {
  code: number;
  msg: string;
  data: T;
}

export type ApiPromise<T = unknown> = Promise<AxiosResponse<ApiResponse<T>>>;

export interface PageData<T = unknown> {
  total: number;
  list: T[];
}
