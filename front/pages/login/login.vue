<template>
  <view class="login-page">
    <view class="brand-area"
      ><text class="brand-emoji">🛒</text
      ><text class="brand-title">生鲜自提商城</text
      ><text class="brand-sub">新鲜生活，从这里开始</text></view
    >
    <view v-if="error" class="error">{{ error }}</view>
    <template v-if="!loggedIn"
      ><view class="tabs"
        ><view class="tab-item" @tap="mode = 'code'"
          ><text :class="['tab-text', { 'tab-text-active': mode === 'code' }]"
            >验证码登录</text
          ><view v-if="mode === 'code'" class="tab-underline" /></view
        ><view class="tab-item" @tap="mode = 'password'"
          ><text
            :class="['tab-text', { 'tab-text-active': mode === 'password' }]"
            >密码登录</text
          ><view v-if="mode === 'password'" class="tab-underline" /></view
      ></view>
      <view class="login-form"
        ><view class="input-capsule"
          ><input
            class="capsule-input"
            type="number"
            v-model="phone"
            maxlength="11"
            placeholder="请输入手机号码"
        /></view>
        <view v-if="mode === 'code'" class="input-capsule"
          ><input
            class="capsule-input"
            v-model="code"
            type="number"
            maxlength="6"
            placeholder="请输入验证码"
          /><button
            class="btn-send-code"
            :disabled="sending || countdown > 0"
            @tap="send"
          >
            {{
              sending
                ? "发送中…"
                : countdown > 0
                  ? countdown + "秒后重试"
                  : "获取验证码"
            }}
          </button></view
        >
        <view v-else class="input-capsule"
          ><input
            class="capsule-input"
            v-model="password"
            :password="true"
            placeholder="请输入登录密码"
        /></view>
        <button
          class="btn-main"
          :loading="working"
          :disabled="working"
          @tap="login"
        >
          登录 / 注册</button
        ><text class="form-hint">未注册手机号验证通过后将自动注册</text
        ><text v-if="mode === 'code'" class="form-hint form-hint-loose"
          >当前为开发验证码模式，尚未接入真实短信。</text
        ></view
      ></template
    >
    <view v-else class="set-pwd-card"
      ><text class="set-pwd-title">设置登录密码</text
      ><text class="set-pwd-desc">设置后可使用密码登录</text
      ><view class="input-capsule card-input"
        ><input
          class="capsule-input"
          v-model="newPassword"
          :password="true"
          maxlength="20"
          placeholder="新密码（6—20位）" /></view
      ><view class="input-capsule card-input"
        ><input
          class="capsule-input"
          v-model="confirmPassword"
          :password="true"
          maxlength="20"
          placeholder="再次输入密码" /></view
      ><button
        class="btn-main"
        :loading="working"
        :disabled="working"
        @tap="savePassword"
      >
        保存密码</button
      ><text class="set-pwd-skip" @tap="back">返回继续选购</text></view
    >
  </view>
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
<style scoped>
/* 多多买菜风格：白底 + 主红 #E02020 + 圆角胶囊输入框 */
.login-page {
  min-height: 100vh;
  background-color: #ffffff;
  box-sizing: border-box;
  padding: 0 56rpx 60rpx;
}

/* ==================== 1. 顶部品牌区 ==================== */
.brand-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 140rpx;
}
.brand-emoji {
  font-size: 108rpx;
  line-height: 1;
}
.brand-title {
  margin-top: 24rpx;
  font-size: 44rpx;
  font-weight: 700;
  color: #333333;
  line-height: 1;
}
.brand-sub {
  margin-top: 18rpx;
  font-size: 26rpx;
  color: #999999;
  line-height: 1;
}

/* ==================== 0. 邀请横幅（橙红渐变卡，缩品牌区留白避免页面过高） ==================== */
.invite-banner {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: -56rpx;
  background: linear-gradient(90deg, #ff6600, #e02020);
  border-radius: 24rpx;
  padding: 24rpx 28rpx;
}
.invite-banner-icon {
  flex-shrink: 0;
  font-size: 52rpx;
  line-height: 1;
}
.invite-banner-texts {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.invite-banner-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.3;
}
.invite-banner-sub {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.92);
  line-height: 1.3;
}

/* ==================== 2. tab 切换（红色下划线激活态） ==================== */
.tabs {
  display: flex;
  justify-content: center;
  gap: 96rpx;
  margin-top: 72rpx;
}
.tab-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 160rpx;
  padding-bottom: 18rpx;
}
.tab-text {
  font-size: 30rpx;
  color: #666666;
  line-height: 1;
}
.tab-text-active {
  color: #e02020;
  font-weight: 700;
}
.tab-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 6rpx;
  border-radius: 6rpx;
  background-color: #e02020;
}

/* ==================== 3/4. 登录表单（圆角胶囊输入框） ==================== */
.login-form {
  margin-top: 56rpx;
}
.input-capsule {
  display: flex;
  align-items: center;
  height: 92rpx;
  margin-bottom: 24rpx;
  background-color: #f4f7f5;
  border: 1rpx solid #e3e9e5;
  border-radius: 999rpx;
  padding: 0 12rpx 0 36rpx;
  box-sizing: border-box;
}
.capsule-input {
  flex: 1;
  min-width: 0;
  height: 92rpx;
  line-height: 92rpx;
  font-size: 30rpx;
  color: #1f2329;
}
.btn-send-code {
  flex-shrink: 0;
  margin: 0;
  padding: 0 28rpx;
  height: 68rpx;
  line-height: 68rpx;
  background-color: #e8f7f0;
  color: #25b873;
  border: 1rpx solid #c9ebd8;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 999rpx;
}
.btn-send-code.btn-send-disabled,
.btn-send-code[disabled] {
  background-color: #f2f5f3;
  color: #a0b3a7;
  border-color: #e2e8e4;
  opacity: 0.7;
}
.form-hint {
  display: block;
  margin: 0 24rpx;
  font-size: 22rpx;
  color: #86909c;
  line-height: 1.5;
}
/* 密码 tab 的提示位于登录按钮下方，与上方拉开间距 */
.form-hint-loose {
  margin-top: 24rpx;
}
.btn-main {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin-top: 40rpx;
  padding: 0;
  background-color: #25b873;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 700;
  border: none;
  border-radius: 999rpx;
  box-shadow: 0 6rpx 16rpx rgba(37, 184, 115, 0.25);
}
.btn-main::after {
  border: none;
}
.btn-main.btn-main-disabled,
.btn-main[disabled] {
  background-color: #b5e4cc !important;
  color: #ffffff !important;
  box-shadow: none !important;
  opacity: 0.75;
}

/* ==================== 5. 设置密码卡区 ==================== */
.set-pwd-card {
  margin-top: 48rpx;
  padding: 36rpx 32rpx;
  background-color: #ffffff;
  border: 1rpx solid #edf1ee;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(31, 35, 41, 0.04);
}
.set-pwd-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2329;
  line-height: 1;
}
.set-pwd-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: #86909c;
  line-height: 1.5;
}
.set-pwd-card .input-capsule {
  margin-top: 24rpx;
  margin-bottom: 0;
  background-color: #f4f7f5;
}
.set-pwd-card .btn-main {
  margin-top: 32rpx;
}
.set-pwd-skip {
  display: block;
  margin-top: 24rpx;
  font-size: 26rpx;
  color: #25b873;
  font-weight: 500;
  text-align: center;
}
</style>
<style scoped>
.btn-main,
.tab-underline {
  background: #25b873;
}
.tab-text-active,
.btn-send-code {
  color: #25b873;
}
.brand-sub {
  color: #86909c;
}
.login-page {
  background: #f7f8fa;
}
.error {
  margin: 24rpx;
}
</style>
