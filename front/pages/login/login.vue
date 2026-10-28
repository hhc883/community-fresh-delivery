<template>
  <view class="login-page">
    <view class="brand-area">
      <view class="brand-logo-wrap">
        <image
          class="brand-logo-svg"
          src="/static/icons/lucide/store.png"
          mode="aspectFit"
        />
      </view>
      <text class="brand-title">社区生鲜自提商城</text>
      <text class="brand-sub">时令鲜品 · 邻里拼团 · 到店核销</text>
    </view>

    <view v-if="error" class="error">{{ error }}</view>

    <template v-if="!loggedIn">
      <view class="tabs">
        <view class="tab-item" @tap="mode = 'code'">
          <text :class="['tab-text', { 'tab-text-active': mode === 'code' }]"
            >验证码登录</text
          >
          <view v-if="mode === 'code'" class="tab-underline" />
        </view>
        <view class="tab-item" @tap="mode = 'password'">
          <text
            :class="['tab-text', { 'tab-text-active': mode === 'password' }]"
            >密码登录</text
          >
          <view v-if="mode === 'password'" class="tab-underline" />
        </view>
      </view>

      <view class="login-form">
        <view class="input-capsule">
          <input
            class="capsule-input"
            type="number"
            v-model="phone"
            maxlength="11"
            placeholder="请输入手机号码"
          />
        </view>

        <view v-if="mode === 'code'" class="input-capsule">
          <input
            class="capsule-input"
            v-model="code"
            type="number"
            maxlength="6"
            placeholder="请输入验证码"
          />
          <button
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
          </button>
        </view>

        <view v-else class="input-capsule">
          <input
            class="capsule-input"
            v-model="password"
            :password="true"
            placeholder="请输入登录密码"
          />
        </view>

        <button
          class="btn-main"
          :loading="working"
          :disabled="working"
          @tap="login"
        >
          登录 / 注册
        </button>
        <text class="form-hint">未注册手机号验证通过后将自动注册</text>
        <text v-if="mode === 'code'" class="form-hint form-hint-loose"
          >当前为开发验证码模式，尚未接入真实短信。</text
        >
      </view>
    </template>

    <view v-else class="set-pwd-card">
      <text class="set-pwd-title">设置登录密码</text>
      <text class="set-pwd-desc">设置后可使用手机号与密码直接登录</text>
      <view class="input-capsule card-input">
        <input
          class="capsule-input"
          v-model="newPassword"
          :password="true"
          maxlength="20"
          placeholder="新密码（6—20位）"
        />
      </view>
      <view class="input-capsule card-input">
        <input
          class="capsule-input"
          v-model="confirmPassword"
          :password="true"
          maxlength="20"
          placeholder="再次输入密码"
        />
      </view>
      <button
        class="btn-main"
        :loading="working"
        :disabled="working"
        @tap="savePassword"
      >
        保存密码
      </button>
      <text class="set-pwd-skip" @tap="back">返回继续选购</text>
    </view>
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
.login-page {
  min-height: 100vh;
  background-color: #f6f8f5;
  box-sizing: border-box;
  padding: 0 56rpx 60rpx;
}

/* 顶部品牌区 */
.brand-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;
}

.brand-logo-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background-color: #eaf5ee;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #cce8d6;
}

.brand-logo-svg {
  width: 60rpx;
  height: 60rpx;
}

.brand-title {
  margin-top: 24rpx;
  font-size: 38rpx;
  font-weight: 700;
  color: #19241d;
  line-height: 1.3;
}

.brand-sub {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #778579;
}

/* Tab 切换 */
.tabs {
  display: flex;
  justify-content: center;
  gap: 80rpx;
  margin-top: 60rpx;
}

.tab-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 160rpx;
  padding-bottom: 16rpx;
}

.tab-text {
  font-size: 30rpx;
  color: #778579;
}

.tab-text-active {
  color: #27744d;
  font-weight: 700;
}

.tab-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 44rpx;
  height: 6rpx;
  border-radius: 6rpx;
  background-color: #27744d;
}

/* 表单 */
.login-form {
  margin-top: 48rpx;
}

.input-capsule {
  display: flex;
  align-items: center;
  height: 92rpx;
  margin-bottom: 24rpx;
  background-color: #ffffff;
  border: 1rpx solid #e1e8e2;
  border-radius: 46rpx;
  padding: 0 12rpx 0 32rpx;
  box-sizing: border-box;
}

.capsule-input {
  flex: 1;
  min-width: 0;
  height: 92rpx;
  line-height: 92rpx;
  font-size: 28rpx;
  color: #19241d;
}

.btn-send-code {
  flex-shrink: 0;
  margin: 0;
  padding: 0 24rpx;
  height: 68rpx;
  line-height: 68rpx;
  background-color: #eaf5ee;
  color: #27744d;
  border: 1rpx solid #bce1ca;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 34rpx;
}

.btn-send-code::after {
  border: none;
}

.btn-send-code[disabled] {
  background-color: #f0f4f1;
  color: #9cb1a2;
  border-color: #dfe7e1;
  opacity: 0.7;
}

.form-hint {
  display: block;
  margin: 0 24rpx;
  font-size: 22rpx;
  color: #778579;
  line-height: 1.5;
}

.form-hint-loose {
  margin-top: 20rpx;
}

.btn-main {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin-top: 36rpx;
  padding: 0;
  background-color: #27744d;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  border-radius: 44rpx;
  box-shadow: 0 4rpx 14rpx rgba(39, 116, 77, 0.22);
}

.btn-main::after {
  border: none;
}

.btn-main[disabled] {
  background-color: #bce1ca !important;
  color: #ffffff !important;
  box-shadow: none !important;
  opacity: 0.7;
}

/* 设置密码卡片 */
.set-pwd-card {
  margin-top: 48rpx;
  padding: 36rpx 32rpx;
  background-color: #ffffff;
  border: 1rpx solid #e5ebe6;
  border-radius: 20rpx;
}

.set-pwd-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #19241d;
}

.set-pwd-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #778579;
}

.set-pwd-card .card-input {
  margin-top: 20rpx;
  margin-bottom: 0;
}

.set-pwd-card .btn-main {
  margin-top: 32rpx;
}

.set-pwd-skip {
  display: block;
  margin-top: 24rpx;
  font-size: 26rpx;
  color: #27744d;
  font-weight: 500;
  text-align: center;
}
</style>
