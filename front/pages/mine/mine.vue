<template>
  <view class="mine-page">
    <view class="profile-header"
      ><view class="user-row" @tap="go('/pages/login/login')"
        ><view class="avatar-box"
          ><image
            class="avatar-img"
            src="/static/images/default-avatar.png" /></view
        ><view class="user-info"
          ><view class="name-line"
            ><text class="user-name">{{
              user.token ? "我的生鲜生活" : "点击登录 / 注册"
            }}</text></view
          ><text class="user-id">{{
            user.token ? maskedPhone : "欢迎来到生鲜自提商城"
          }}</text></view
        ></view
      ></view
    >
    <view class="menu-card order-card"
      ><view class="card-head" @tap="go('/pages/order/order')"
        ><text class="ch-title">我的订单</text
        ><text class="ch-all">查看全部 ›</text></view
      ><view class="order-grid"
        ><view
          class="grid-item"
          v-for="tab in orderTabs"
          :key="tab.name"
          @tap="go('/pages/order/order?status=' + tab.status)"
          ><view class="icon-wrap"
            ><text class="pink-icon">{{ tab.icon }}</text></view
          ><text class="grid-label">{{ tab.name }}</text></view
        ></view
      ></view
    >
    <view class="vip-banner-card" @tap="go('/pages/groupon/list')"
      ><view class="vbc-text"
        ><text class="vbc-title">邻里好物，一起拼团</text></view
      ><button class="vbc-btn">去逛逛</button></view
    >
    <view class="menu-card list-menu-card"
      ><view
        class="list-item"
        v-for="item in menus"
        :key="item.name"
        @tap="go(item.url)"
        ><view class="li-left"
          ><text class="li-icon">{{ item.icon }}</text
          ><text class="li-title">{{ item.name }}</text></view
        ><text class="li-arrow">›</text></view
      ></view
    >
    <view v-if="user.token" class="menu-card list-menu-card"
      ><view class="list-item" @tap="logout"
        ><text class="li-title">退出登录</text
        ><text class="li-arrow">›</text></view
      ></view
    >
  </view>
</template>
<script>
export default {
  data: () => ({
    orderTabs: [
      { name: "待付款", status: 1, icon: "💳" },
      { name: "待履约", status: 2, icon: "📦" },
      { name: "已完成", status: 5, icon: "✓" },
      { name: "已取消", status: 6, icon: "↩" },
    ],
    menus: [
      { name: "我的收货地址", icon: "🏠", url: "/pages/address/list" },
      { name: "我的社区拼团", icon: "🛒", url: "/pages/groupon/my" },
      { name: "切换自提门店", icon: "📍", url: "/pages/pickup/pickup" },
      { name: "登录与密码设置", icon: "🔐", url: "/pages/login/login" },
    ],
  }),
  computed: {
    user() {
      return this.$store.state.user;
    },
    maskedPhone() {
      const p = this.user.phone || "";
      return p.length === 11 ? p.slice(0, 3) + "****" + p.slice(7) : p;
    },
  },
  methods: {
    go(url) {
      uni.navigateTo({ url });
    },
    logout() {
      this.$store.commit("setClearUser");
      uni.showToast({ title: "已退出登录", icon: "none" });
    },
  },
};
</script>
<style scoped>
.mine-page {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: calc(60rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(60rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

/* 顶部绿色生鲜沉浸式个人头部 */
.profile-header {
  position: relative;
  height: 320rpx;
  background: linear-gradient(135deg, #25b873 0%, #1ea062 100%);
  padding: 60rpx 36rpx 0;
  box-sizing: border-box;
  box-shadow: 0 4rpx 20rpx rgba(37, 184, 115, 0.2);
}

.user-row {
  display: flex;
  align-items: center;
}

.avatar-box {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid #ffffff;
  overflow: hidden;
  background-color: #e8f7f0;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.user-info {
  flex: 1;
  margin-left: 24rpx;
}

.name-line {
  display: flex;
  align-items: center;
  margin-bottom: 10rpx;
}

.user-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
  margin-right: 14rpx;
}

.user-id {
  font-size: 24rpx;
  color: #e5f7ed;
}

/* 通用白底卡片 */
.menu-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  margin: 0 24rpx 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(31, 35, 41, 0.04);
  border: 1rpx solid #edf1ee;
  box-sizing: border-box;
}

/* 订单卡片：向上悬浮覆盖部分绿色背景 */
.order-card {
  position: relative;
  z-index: 10;
  margin-top: -40rpx;
  padding: 28rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.ch-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2329;
}

.ch-all {
  font-size: 24rpx;
  color: #86909c;
}

.order-grid {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8rpx 0;
}

.icon-wrap {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pink-icon {
  font-size: 44rpx;
  color: #25b873;
  line-height: 1;
}

.grid-label {
  font-size: 24rpx;
  color: #1f2329;
  margin-top: 10rpx;
}

/* 拼团推广卡 */
.vip-banner-card {
  margin: 0 24rpx 20rpx;
  background-color: #e8f7f0;
  border: 1rpx solid #c9ebd8;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2rpx 12rpx rgba(37, 184, 115, 0.08);
}

.vbc-text {
  flex: 1;
}

.vbc-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1f7d4b;
}

.vbc-btn {
  height: 60rpx;
  line-height: 60rpx;
  background-color: #25b873;
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: 30rpx;
  padding: 0 32rpx;
  margin: 0;
  border: none;
  box-shadow: 0 4rpx 10rpx rgba(37, 184, 115, 0.25);
}

.vbc-btn::after {
  border: none;
}

/* 菜单列表卡片 */
.list-menu-card {
  padding: 0 28rpx;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100rpx;
  border-bottom: 1rpx solid #f2f5f3;
}

.list-item:last-child {
  border-bottom: none;
}

.li-left {
  display: flex;
  align-items: center;
}

.li-icon {
  font-size: 34rpx;
  margin-right: 18rpx;
}

.li-title {
  font-size: 28rpx;
  color: #1f2329;
  font-weight: 500;
}

.li-arrow {
  font-size: 32rpx;
  color: #c9cdd4;
}
</style>
