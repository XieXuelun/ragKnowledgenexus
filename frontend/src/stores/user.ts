import { defineStore } from "pinia";
import { ref } from "vue";
import { login as loginApi, logout as logoutApi } from "@/api/auth";
import { getUserInfo as getInfoApi } from "@/api/user";

export interface UserInfo {
  id: string;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  avatarUrl: string;
  role: number;
  deptId: number;
  deptName: string;
  status: number;
  createTime: string;
  updateTime?: string;
}

export const useUserStore = defineStore("user", () => {
  var token = ref<string | null>(localStorage.getItem("token"));
  var userInfo = ref<UserInfo | null>(null);

  function setToken(val: string | null) {
    token.value = val;
    if (val) {
      localStorage.setItem("token", val);
    } else {
      localStorage.removeItem("token");
    }
  }

  async function login(params: { username: string; password: string }) {
    var res = await loginApi(params);
    // res is already unwrapped by axios interceptor: { code, errorMessage, data }
    // data = { token: "...", user: { ... } }
    setToken(res.data.token);
    if (res.data.user) {
      userInfo.value = { ...res.data.user };
    } else {
      await fetchUserInfo();
    }
    return res;
  }

  async function fetchUserInfo() {
    var res = await getInfoApi();
    userInfo.value = { ...res.data };
  }

  async function logout() {
    try {
      await logoutApi();
    } finally {
      setToken(null);
      userInfo.value = null;
    }
  }

  return { token, userInfo, setToken, login, fetchUserInfo, logout };
});
