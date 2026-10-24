<template>
  <view class="mine-page">
    <!-- 顶部薰衣草粉紫渐变身份区域 -->
    <view class="profile-header">
      <view class="user-row" @click="goProfile">
        <view class="avatar-box">
          <image class="avatar-img" :src="avatarSrc" mode="aspectFill" />
        </view>
        <view class="user-info">
          <view class="name-line">
            <text class="user-name">{{ userProfile.nickname || '张三' }}</text>
            <view class="vip-badge">
              <text>普通会员</text>
            </view>
          </view>
          <text class="user-id">用户ID：{{ maskPhone(user.phone || '13800138000') }}</text>
        </view>
      </view>
    </view>

    <!-- 资产数据双列卡片 (悬浮于背景下方) -->
    <view class="assets-card">
      <view class="asset-item" @click="onAssetTap('balance')">
        <text class="asset-val">0</text>
        <text class="asset-name">余额</text>
      </view>
      <view class="asset-divider"></view>
      <view class="asset-item" @click="goMyCoupons">
        <text class="asset-val">0</text>
        <text class="asset-name">优惠券</text>
      </view>
    </view>

    <!-- 我的订单卡片 -->
    <view class="menu-card order-card">
      <view class="card-head" @click="goOrderList('all')">
        <text class="ch-title">我的订单</text>
        <view class="ch-right">
          <text class="ch-all">全部</text>
          <text class="ch-arrow">›</text>
        </view>
      </view>
      <view class="order-grid">
        <!-- 待付款 (角标 1) -->
        <view class="grid-item" @click="goOrderList('pending_pay')">
          <view class="icon-wrap">
            <text class="pink-icon">💳</text>
            <text class="badge">1</text>
          </view>
          <text class="grid-label">待付款</text>
        </view>

        <!-- 待自提 (角标 2) -->
        <view class="grid-item" @click="goOrderList('pending_pickup')">
          <view class="icon-wrap">
            <text class="pink-icon">📦</text>
            <text class="badge">2</text>
          </view>
          <text class="grid-label">待自提</text>
        </view>

        <!-- 待评价 -->
        <view class="grid-item" @click="goOrderList('completed')">
          <view class="icon-wrap">
            <text class="pink-icon">⭐</text>
          </view>
          <text class="grid-label">待评价</text>
        </view>

        <!-- 退款/售后 -->
        <view class="grid-item" @click="goOrderList('refund')">
          <view class="icon-wrap">
            <text class="pink-icon">🔄</text>
          </view>
          <text class="grid-label">退款/售后</text>
        </view>
      </view>
    </view>

    <!-- 升级 VIP 专属促销横幅 -->
    <view class="vip-banner-card" @click="onVipTap">
      <view class="vbc-text">
        <text class="vbc-title">升级生鲜VIP会员，享天天特惠与专属折扣</text>
      </view>
      <button class="vbc-btn">立即开通VIP</button>
    </view>

    <!-- 浏览足迹横向卡片 -->
    <view class="menu-card footprint-card">
      <view class="fp-header">
        <view class="fp-title-row">
          <text class="fp-icon">🕒</text>
          <text class="fp-title">浏览足迹</text>
        </view>
      </view>
      <scroll-view class="fp-scroll" scroll-x>
        <view
          class="fp-item"
          v-for="item in footprintItems"
          :key="item.id"
          @click="goGoodsDetail(item.id)"
        >
          <image class="fp-img" :src="item.image" mode="aspectFill" />
        </view>
      </scroll-view>
    </view>

    <!-- 底部功能入口列表 -->
    <view class="menu-card list-menu-card">
      <!-- 切换自提点 -->
      <view class="list-item" @click="goPickup">
        <view class="li-left">
          <text class="li-icon green-icon">📍</text>
          <text class="li-title">切换自提点</text>
        </view>
        <text class="li-arrow">›</text>
      </view>

      <view class="divider"></view>

      <!-- 联系客服 -->
      <view class="list-item" @click="goService">
        <view class="li-left">
          <text class="li-icon purple-icon">📞</text>
          <text class="li-title">联系客服</text>
        </view>
        <text class="li-arrow">›</text>
      </view>

      <view class="divider"></view>

      <!-- 个人资料 -->
      <view class="list-item" @click="goProfile">
        <view class="li-left">
          <text class="li-icon blue-icon">⚙️</text>
          <text class="li-title">个人资料</text>
        </view>
        <text class="li-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'

export default {
  data() {
    return {
      avatarSrc: '/static/images/default-avatar.png',
      userProfile: {
        nickname: '张三'
      },
      footprintItems: [
        { id: 2, image: '/static/images/png/dish-2.png' },
        { id: 1, image: '/static/images/png/dish-1.png' },
        { id: 3, image: '/static/images/png/dish-3.png' },
        { id: 4, image: '/static/images/png/dish-4.png' }
      ]
    }
  },
  computed: {
    ...mapState(['user'])
  },
  onShow() {
    const p = uni.getStorageSync('user_profile')
    if (p) {
      try {
        const parsed = typeof p === 'string' ? JSON.parse(p) : p
        if (parsed.nickname) this.userProfile.nickname = parsed.nickname
        if (parsed.avatarUrl) this.avatarSrc = parsed.avatarUrl
      } catch (e) {}
    }
  },
  methods: {
    maskPhone(phone) {
      if (!phone || phone.length < 11) return '138****0000'
      return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },
    goProfile() {
      uni.navigateTo({ url: '/pages/mine/profile' })
    },
    onAssetTap(type) {
      if (type === 'balance') {
        uni.showToast({ title: '当前余额为 0 元', icon: 'none' })
      }
    },
    goMyCoupons() {
      uni.navigateTo({ url: '/pages/coupons/coupons' })
    },
    goOrderList(tab) {
      uni.navigateTo({ url: `/pages/order/order?tab=${tab}` })
    },
    onVipTap() {
      uni.showModal({
        title: '生鲜VIP会员',
        content: '开通VIP专享会员日全场8.8折与急速自提通道！',
        confirmText: '立即开通',
        confirmColor: '#25B873',
        success: (res) => {
          if (res.confirm) {
            uni.showToast({ title: '开通成功', icon: 'success' })
          }
        }
      })
    },
    goGoodsDetail(id) {
      uni.navigateTo({ url: `/pages/detail/detail?id=${id}` })
    },
    goPickup() {
      uni.navigateTo({ url: '/pages/pickup/pickup' })
    },
    goService() {
      uni.navigateTo({ url: '/pages/service/service' })
    }
  }
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 50rpx;
}

/* 顶部薰衣草粉紫渐变卡片 */
.profile-header {
  position: relative;
  height: 300rpx;
  background: linear-gradient(135deg, #A8BEE8 0%, #D8BFD8 50%, #F5C7B8 100%);
  padding: 80rpx 36rpx 0;
  box-sizing: border-box;

  .user-row {
    display: flex;
    align-items: center;

    .avatar-box {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      border: 4rpx solid #FFFFFF;
      overflow: hidden;
      background-color: #E2E8F0;
      box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);

      .avatar-img {
        width: 100%;
        height: 100%;
      }
    }

    .user-info {
      flex: 1;
      margin-left: 24rpx;

      .name-line {
        display: flex;
        align-items: center;
        margin-bottom: 10rpx;

        .user-name {
          font-size: 34rpx;
          font-weight: 700;
          color: #1F2329;
          margin-right: 14rpx;
        }

        .vip-badge {
          background-color: rgba(31, 35, 41, 0.7);
          padding: 4rpx 14rpx;
          border-radius: 20rpx;

          text {
            font-size: 20rpx;
            color: #FFFFFF;
          }
        }
      }

      .user-id {
        font-size: 24rpx;
        color: #4E5969;
      }
    }
  }
}

/* 资产数据三列卡片 (悬浮向上提20rpx) */
.assets-card {
  position: relative;
  z-index: 10;
  margin: -30rpx 24rpx 20rpx;
  background-color: #FFFFFF;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  padding: 30rpx 0;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);

  .asset-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;

    .asset-val {
      font-size: 38rpx;
      font-weight: 700;
      color: #1F2329;
      line-height: 1;
    }

    .asset-name {
      font-size: 24rpx;
      color: #8F959E;
      margin-top: 12rpx;
    }
  }

  .asset-divider {
    width: 2rpx;
    height: 48rpx;
    background-color: #F0F2F5;
  }
}

/* 通用白底卡片 */
.menu-card {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  margin: 0 24rpx 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);
}

/* 订单卡片 */
.order-card {
  padding: 24rpx 28rpx;

  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24rpx;

    .ch-title {
      font-size: 30rpx;
      font-weight: 700;
      color: #1F2329;
    }

    .ch-right {
      display: flex;
      align-items: center;

      .ch-all {
        font-size: 24rpx;
        color: #8F959E;
      }

      .ch-arrow {
        font-size: 30rpx;
        color: #C9CDD4;
        margin-left: 6rpx;
      }
    }
  }

  .order-grid {
    display: flex;
    align-items: center;
    justify-content: space-around;

    .grid-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .icon-wrap {
        position: relative;
        width: 60rpx;
        height: 60rpx;
        display: flex;
        align-items: center;
        justify-content: center;

        .pink-icon {
          font-size: 42rpx;
          line-height: 1;
        }

        .badge {
          position: absolute;
          top: -4rpx;
          right: -12rpx;
          background-color: #FF4D4F;
          color: #FFFFFF;
          font-size: 20rpx;
          height: 28rpx;
          min-width: 28rpx;
          line-height: 28rpx;
          text-align: center;
          border-radius: 14rpx;
          padding: 0 6rpx;
          font-weight: 700;
        }
      }

      .grid-label {
        font-size: 24rpx;
        color: #1F2329;
        margin-top: 12rpx;
      }
    }
  }
}

/* VIP 横幅 */
.vip-banner-card {
  margin: 0 24rpx 20rpx;
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);

  .vbc-text {
    margin-bottom: 16rpx;

    .vbc-title {
      font-size: 24rpx;
      color: #333333;
      text-align: center;
    }
  }

  .vbc-btn {
    width: 60%;
    height: 64rpx;
    line-height: 64rpx;
    background-color: #25B873;
    color: #FFFFFF;
    font-size: 26rpx;
    font-weight: 600;
    border-radius: 32rpx;
    border: none;

    &::after {
      border: none;
    }
  }
}

/* 浏览足迹 */
.footprint-card {
  padding: 24rpx 28rpx;

  .fp-header {
    margin-bottom: 20rpx;

    .fp-title-row {
      display: flex;
      align-items: center;

      .fp-icon {
        font-size: 30rpx;
        margin-right: 10rpx;
      }

      .fp-title {
        font-size: 28rpx;
        font-weight: 700;
        color: #1F2329;
      }
    }
  }

  .fp-scroll {
    white-space: nowrap;
    width: 100%;

    .fp-item {
      display: inline-block;
      width: 150rpx;
      height: 150rpx;
      border-radius: 12rpx;
      margin-right: 18rpx;
      background-color: #F7F8FA;
      overflow: hidden;

      .fp-img {
        width: 100%;
        height: 100%;
      }
    }
  }
}

/* 功能列表 */
.list-menu-card {
  padding: 0 28rpx;

  .divider {
    height: 1rpx;
    background-color: #F0F2F5;
  }

  .list-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 96rpx;

    .li-left {
      display: flex;
      align-items: center;

      .li-icon {
        font-size: 32rpx;
        margin-right: 16rpx;
      }

      .li-title {
        font-size: 28rpx;
        color: #1F2329;
      }
    }

    .li-arrow {
      font-size: 32rpx;
      color: #C9CDD4;
    }
  }
}
</style>
