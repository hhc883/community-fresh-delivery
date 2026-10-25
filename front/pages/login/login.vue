<template>
  <view class="core"
    ><view class="hero"
      ><text class="title">欢迎来到社区鲜选</text
      ><text class="sub">登录后管理地址、下单与参与拼团</text></view
    ><view v-if="error" class="error">{{ error }}</view
    ><view v-if="!loggedIn" class="card"
      ><view class="chips"
        ><text
          :class="['chip', mode === 'code' ? 'active' : '']"
          @tap="mode = 'code'"
          >验证码登录</text
        ><text
          :class="['chip', mode === 'password' ? 'active' : '']"
          @tap="mode = 'password'"
          >密码登录</text
        ></view
      ><input
        class="field"
        type="number"
        v-model="phone"
        maxlength="11"
        placeholder="手机号"
      /><view v-if="mode === 'code'"
        ><input
          class="field"
          type="number"
          v-model="code"
          maxlength="6"
          placeholder="验证码"
        /><button
          class="secondary"
          :disabled="sending || countdown > 0"
          @tap="send"
        >
          {{ countdown > 0 ? countdown + " 秒后重试" : "获取验证码" }}</button
        ><text class="sub"
          >当前仅支持开发验证码模式，具体验证码由后端开发环境配置。未接入真实短信。</text
        ></view
      ><input
        v-else
        class="field"
        v-model="password"
        :password="true"
        placeholder="登录密码"
      /><button
        class="primary"
        :loading="working"
        :disabled="working"
        @tap="login"
      >
        登录 / 注册
      </button></view
    ><view v-else class="card"
      ><text class="product-name">设置登录密码</text
      ><input
        class="field"
        v-model="newPassword"
        :password="true"
        placeholder="新密码（6—20 位）"
      /><input
        class="field"
        v-model="confirmPassword"
        :password="true"
        placeholder="再次输入新密码"
      /><button
        class="primary"
        :loading="working"
        :disabled="working"
        @tap="savePassword"
      >
        保存密码</button
      ><button class="secondary" @tap="back">返回继续选购</button></view
    ></view
  >
</template>
<script>
import {
  login,
  loginByPassword,
  sendCode,
  setPassword,
} from "../../api/index.js";
export default {
  data: () => ({
    mode: "code",
    phone: "",
    code: "",
    password: "",
    newPassword: "",
    confirmPassword: "",
    error: "",
    working: false,
    sending: false,
    countdown: 0,
    timer: null,
  }),
  computed: {
    loggedIn() {
      return !!this.$store.state.user.token;
    },
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer);
  },
  methods: {
    async send() {
      if (this.sending || this.countdown) return;
      this.error = "";
      if (!/^1[3-9]\d{9}$/.test(this.phone)) {
        this.error = "请输入正确手机号";
        return;
      }
      this.sending = true;
      try {
        await sendCode(this.phone);
        this.countdown = 60;
        this.timer = setInterval(() => {
          if (--this.countdown <= 0) {
            clearInterval(this.timer);
            this.timer = null;
          }
        }, 1000);
        uni.showToast({ title: "开发验证码请求成功", icon: "none" });
      } catch (e) {
        this.error = e.message;
      } finally {
        this.sending = false;
      }
    },
    async login() {
      if (this.working) return;
      this.error = "";
      if (!/^1[3-9]\d{9}$/.test(this.phone)) {
        this.error = "请输入正确手机号";
        return;
      }
      if (this.mode === "code" && !this.code) {
        this.error = "请输入验证码";
        return;
      }
      if (this.mode === "password" && !this.password) {
        this.error = "请输入密码";
        return;
      }
      this.working = true;
      try {
        const r = await (this.mode === "code"
          ? login(this.phone, this.code)
          : loginByPassword(this.phone, this.password));
        if (!r.data?.token) throw new Error("登录未返回令牌");
        this.$store.commit("setUser", {
          token: r.data.token,
          id: r.data.user?.id,
          phone: r.data.user?.phone || this.phone,
        });
        uni.showToast({ title: "登录成功", icon: "success" });
        this.back();
      } catch (e) {
        this.error = e.message || "登录失败";
      } finally {
        this.working = false;
      }
    },
    async savePassword() {
      if (this.working) return;
      if (
        this.newPassword.length < 6 ||
        this.newPassword.length > 20 ||
        this.newPassword !== this.confirmPassword
      ) {
        this.error = "请填写 6—20 位密码并确保两次一致";
        return;
      }
      this.working = true;
      try {
        await setPassword(this.newPassword);
        this.error = "";
        this.newPassword = "";
        this.confirmPassword = "";
        uni.showToast({ title: "密码已保存", icon: "success" });
      } catch (e) {
        this.error = e.message;
      } finally {
        this.working = false;
      }
    },
    back() {
      uni.navigateBack({
        fail: () => uni.switchTab({ url: "/pages/index/index" }),
      });
    },
  },
};
</script>
