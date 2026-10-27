<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 左侧生鲜视觉展示 -->
      <div class="login-hero">
        <img
          src="/branding/fresh-produce.webp"
          alt="社区生鲜"
          class="hero-image"
        />
        <div class="hero-overlay">
          <div class="hero-content">
            <div class="hero-badge">社区团购 · 配送自提</div>
            <h2 class="hero-title">新鲜品质，每日鲜选</h2>
            <p class="hero-desc">
              微信小程序社区团购 · 社区自提 · 生鲜配送
            </p>
          </div>
        </div>
      </div>

      <!-- 右侧运营登录表单 -->
      <div class="login-form-pane">
        <div class="brand-header">
          <img
            src="/branding/fresh-mark.svg"
            alt="社区鲜选"
            class="brand-icon"
          />
          <div class="brand-text">
            <h1 class="brand-name">社区鲜选</h1>
            <span class="brand-subtitle">生鲜运营管理中台</span>
          </div>
        </div>

        <el-form
          ref="loginForm"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          label-position="top"
        >
          <el-form-item label="运营账号" prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              auto-complete="off"
              placeholder="请输入管理员或运营账号"
              prefix-icon="el-icon-user"
              size="medium"
            />
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入登录密码"
              prefix-icon="el-icon-lock"
              show-password
              size="medium"
              @keyup.enter.native="handleLogin"
            />
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button
              :loading="loading"
              class="login-btn"
              size="medium"
              type="primary"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>安全登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span>社区鲜选数字生鲜运营支撑系统</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import { Route } from 'vue-router'
import { Form as ElForm, Input } from 'element-ui'
import { UserModule } from '@/store/modules/user'
import { isValidUsername } from '@/utils/validate'

@Component({
  name: 'Login',
})
export default class extends Vue {
  private validateUsername = (rule: any, value: string, callback: Function) => {
    if (!value) {
      callback(new Error('请输入用户名'))
    } else {
      callback()
    }
  }
  private validatePassword = (rule: any, value: string, callback: Function) => {
    if (value.length < 6) {
      callback(new Error('密码必须在6位以上'))
    } else {
      callback()
    }
  }
  private loginForm = {
    username: '',
    password: '',
  } as {
    username: String
    password: String
  }

  loginRules = {
    username: [{ validator: this.validateUsername, trigger: 'blur' }],
    password: [{ validator: this.validatePassword, trigger: 'blur' }],
  }
  private loading = false
  private redirect?: string

  @Watch('$route', { immediate: true })
  private onRouteChange(route: Route) {}

  // 登录
  private handleLogin() {
    ;(this.$refs.loginForm as ElForm).validate(async (valid: boolean) => {
      if (valid) {
        this.loading = true
        await UserModule.Login(this.loginForm as any)
          .then((res: any) => {
            if (String(res.code) === '1') {
              this.$router.push('/')
            } else {
              // this.$message.error(res.msg)
              this.loading = false
            }
          })
          .catch((error) => {
            this.$message.error(error.message || '登录失败，请重试')
            this.loading = false
          })
      } else {
        return false
      }
    })
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #F4F6F8;
  padding: 24px;
}

.login-card {
  width: 960px;
  max-width: 95vw;
  min-height: 540px;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
  border: 1px solid #E2E8F0;
  display: flex;
  overflow: hidden;
}

.login-hero {
  flex: 1.1;
  position: relative;
  background: #132B1E;
  display: flex;
  overflow: hidden;

  .hero-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .hero-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(
      180deg,
      rgba(19, 43, 30, 0.25) 0%,
      rgba(19, 43, 30, 0.85) 100%
    );
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    padding: 40px;
    color: #FFFFFF;
  }

  .hero-content {
    max-width: 380px;

    .hero-badge {
      display: inline-block;
      padding: 4px 12px;
      background: rgba(39, 116, 77, 0.7);
      backdrop-filter: blur(4px);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 20px;
      font-size: 12px;
      font-weight: 500;
      color: #E8F5E9;
      margin-bottom: 14px;
    }

    .hero-title {
      font-size: 26px;
      font-weight: 600;
      margin: 0 0 10px;
      line-height: 1.3;
      letter-spacing: 0.5px;
    }

    .hero-desc {
      font-size: 13px;
      color: #C5E1D4;
      line-height: 1.6;
      margin: 0;
    }
  }
}

.login-form-pane {
  flex: 0.9;
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: #FFFFFF;

  .brand-header {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 32px;

    .brand-icon {
      width: 44px;
      height: 44px;
      border-radius: 10px;
    }

    .brand-text {
      display: flex;
      flex-direction: column;

      .brand-name {
        margin: 0;
        font-size: 22px;
        font-weight: 600;
        color: #1A202C;
        letter-spacing: 0.5px;
      }

      .brand-subtitle {
        font-size: 13px;
        color: #27744D;
        font-weight: 500;
        margin-top: 3px;
      }
    }
  }

  .login-form {
    width: 100%;

    ::v-deep .el-form-item__label {
      font-size: 13px;
      font-weight: 500;
      color: #475569;
      padding-bottom: 6px;
      line-height: 1;
    }

    ::v-deep .el-input__inner {
      height: 40px;
      line-height: 40px;
      border-radius: 6px;
      border: 1px solid #CBD5E1;
      font-size: 13px;
      padding-left: 36px;
      transition: all 0.2s;

      &:focus {
        border-color: #27744D;
        box-shadow: 0 0 0 2px rgba(39, 116, 77, 0.15);
      }
    }

    ::v-deep .el-input__prefix {
      left: 10px;
      color: #94A3B8;
      display: flex;
      align-items: center;
    }

    .submit-item {
      margin-top: 32px;
      margin-bottom: 0;
    }

    .login-btn {
      width: 100%;
      height: 42px;
      font-size: 14px;
      font-weight: 600;
      letter-spacing: 1px;
      border-radius: 6px;
      background-color: #27744D !important;
      border-color: #27744D !important;
      color: #FFFFFF !important;
      box-shadow: 0 4px 12px rgba(39, 116, 77, 0.25);
      transition: all 0.2s;

      &:hover,
      &:focus {
        background-color: #318a5d !important;
        border-color: #318a5d !important;
        box-shadow: 0 6px 16px rgba(39, 116, 77, 0.35);
      }

      &:active {
        background-color: #1f5f3e !important;
      }
    }
  }

  .login-footer {
    text-align: center;
    font-size: 12px;
    color: #94A3B8;
    margin-top: 24px;
  }
}

@media (max-width: 768px) {
  .login-card {
    flex-direction: column;
    min-height: auto;
  }
  .login-hero {
    height: 180px;
    flex: none;
  }
  .login-form-pane {
    padding: 30px 24px;
  }
}
</style>
