import axios from "axios";
import { Message } from "element-ui";
import { removeToken, getToken } from "@/utils/cookies";
import router from "@/router";
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || "/admin",
  timeout: 20000,
});
service.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers["token"] = token;
  return config;
});
function expired() {
  removeToken();
  router.replace("/login");
}
service.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (response.config.responseType === "blob") return response;
    if (body && (body.code === 401 || body.status === 401)) {
      expired();
      return Promise.reject(
        new Error(body.message || body.msg || "请重新登录"),
      );
    }
    // Original pages read response.data.code and display their own business failures.
    return response;
  },
  (error) => {
    if (error.response && error.response.status === 401) expired();
    const body = error.response && error.response.data;
    Message.error(
      (body && (body.message || body.msg)) || "网络连接失败，请重试",
    );
    return Promise.reject(error);
  },
);
export default service;
