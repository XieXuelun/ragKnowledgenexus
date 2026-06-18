import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

const request: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: { "Content-Type": "application/json" },
});

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem("token");
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => {
    // Backend always returns HTTP 200; check business code field
    if (response.data && response.data.code !== undefined && response.data.code !== 200) {
      ElMessage.error(response.data.errorMessage || 'Request failed');
      return Promise.reject(response.data);
    }
    return response.data;
  },
  (error: AxiosError<{ errorMessage?: string }>) => {
    const status = error.response?.status;
    const msg = error.response?.data?.errorMessage || error.message;
    if (status === 401) {
      localStorage.removeItem("token");
      router.push("/login");
      ElMessage.error("登录已过期，请重新登录");
    } else if (status === 403) {
      ElMessage.error("无权限访问");
    } else if (status === 500) {
      ElMessage.error("服务器错误，请稍后重试");
    } else {
      ElMessage.error(msg || "请求失败");
    }
    return Promise.reject(error);
  }
);

export default request;
