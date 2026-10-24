<template>
  <view class="profile-page">
    <view class="form-list">
      <!-- 头像 -->
      <view class="form-row avatar-row" @click="changeAvatar">
        <text class="row-label">头像</text>
        <view class="row-right">
          <image class="avatar-circle" :src="avatarUrl || '/static/images/default-avatar.png'" mode="aspectFill" />
        </view>
      </view>

      <view class="divider"></view>

      <!-- 昵称 -->
      <view class="form-row">
        <text class="row-label">昵称</text>
        <view class="row-right">
          <input class="row-input" v-model="nickname" placeholder="张三" />
        </view>
      </view>

      <view class="divider"></view>

      <!-- 性别 -->
      <picker :range="genderOptions" @change="onGenderChange">
        <view class="form-row">
          <text class="row-label">性别</text>
          <view class="row-right">
            <text class="row-val">{{ gender }}</text>
            <text class="arrow">›</text>
          </view>
        </view>
      </picker>

      <view class="divider"></view>

      <!-- 生日 -->
      <picker mode="date" :value="birthday" @change="onBirthdayChange">
        <view class="form-row">
          <text class="row-label">生日</text>
          <view class="row-right">
            <text class="row-val">{{ birthday }}</text>
            <text class="arrow">›</text>
          </view>
        </view>
      </picker>

      <view class="divider"></view>

      <!-- 手机号 -->
      <view class="form-row">
        <text class="row-label">手机号</text>
        <view class="row-right">
          <text class="row-val">{{ maskPhone(phone) }}</text>
        </view>
      </view>
    </view>

    <!-- 保存按钮 -->
    <view class="btn-wrap">
      <button class="btn-save" @click="saveProfile">保存修改</button>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'

export default {
  data() {
    return {
      avatarUrl: '',
      nickname: '张三',
      gender: '男',
      birthday: '2000-01-01',
      phone: '13800138000',
      genderOptions: ['保密', '男', '女']
    }
  },
  computed: {
    ...mapState(['user'])
  },
  onLoad() {
    if (this.user && this.user.phone) {
      this.phone = this.user.phone
    }
    const cachedProfile = uni.getStorageSync('user_profile')
    if (cachedProfile) {
      try {
        const p = typeof cachedProfile === 'string' ? JSON.parse(cachedProfile) : cachedProfile
        if (p.nickname) this.nickname = p.nickname
        if (p.gender) this.gender = p.gender
        if (p.birthday) this.birthday = p.birthday
        if (p.avatarUrl) this.avatarUrl = p.avatarUrl
      } catch (e) {}
    }
  },
  methods: {
    maskPhone(p) {
      if (!p || p.length < 11) return '138****0000'
      return p.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },
    onGenderChange(e) {
      this.gender = this.genderOptions[e.detail.value]
    },
    onBirthdayChange(e) {
      this.birthday = e.detail.value
    },
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        success: (res) => {
          this.avatarUrl = res.tempFilePaths[0]
        }
      })
    },
    saveProfile() {
      uni.setStorageSync('user_profile', {
        nickname: this.nickname,
        gender: this.gender,
        birthday: this.birthday,
        avatarUrl: this.avatarUrl
      })
      uni.showToast({
        title: '个人信息已更新',
        icon: 'success'
      })
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.form-list {
  padding: 0 32rpx;

  .divider {
    height: 1rpx;
    background-color: #F0F2F5;
  }

  .form-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 108rpx;

    .row-label {
      font-size: 30rpx;
      color: #1F2329;
    }

    .row-right {
      display: flex;
      align-items: center;

      .avatar-circle {
        width: 80rpx;
        height: 80rpx;
        border-radius: 50%;
        background-color: #F0F2F5;
      }

      .row-input {
        text-align: right;
        font-size: 28rpx;
        color: #1F2329;
      }

      .row-val {
        font-size: 28rpx;
        color: #1F2329;
      }

      .arrow {
        font-size: 32rpx;
        color: #C9CDD4;
        margin-left: 12rpx;
      }
    }
  }
}

.btn-wrap {
  padding: 80rpx 32rpx;

  .btn-save {
    width: 100%;
    height: 88rpx;
    line-height: 88rpx;
    background-color: #25B873;
    border-radius: 44rpx;
    color: #FFFFFF;
    font-size: 30rpx;
    font-weight: 600;
    border: none;

    &::after {
      border: none;
    }
  }
}
</style>
