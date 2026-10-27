<template>
  <view class="mine-page">
    <!-- 顶部生鲜绿意个人头部 -->
    <view class="profile-header">
      <view class="user-row" @tap="go('/pages/login/login')">
        <view class="avatar-box">
          <image class="avatar-img" src="/static/images/default-avatar.png" />
        </view>
        <view class="user-info">
          <view class="name-line">
            <text class="user-name">{{
              user.token ? "我的生鲜生活" : "点击登录 / 注册"
            }}</text>
          </view>
          <text class="user-id">{{
            user.token ? maskedPhone : "欢迎来到社区生鲜自提商城"
          }}</text>
        </view>
        <image
          class="header-arrow"
          src="/static/icons/lucide/chevron-right-white.png"
          mode="aspectFit"
        />
      </view>
    </view>

    <!-- 订单履约中心卡片 -->
    <view class="menu-card order-card">
      <view class="card-head" @tap="go('/pages/order/order')">
        <text class="ch-title">我的订单</text>
        <view class="ch-all-box">
          <text class="ch-all">全部订单</text>
          <image
            class="all-arrow"
            src="/static/icons/lucide/chevron-right-muted.png"
            mode="aspectFit"
          />
        </view>
      </view>
      <view class="order-grid">
        <view
          class="grid-item"
          v-for="tab in orderTabs"
          :key="tab.name"
          @tap="go('/pages/order/order?status=' + tab.status)"
        >
          <view class="icon-wrap">
            <image
              v-if="tab.status === 1"
              class="tab-svg"
              src="/static/icons/lucide/clock.png"
              mode="aspectFit"
            />
            <image
              v-else-if="tab.status === 2"
              class="tab-svg"
              src="/static/icons/lucide/package.png"
              mode="aspectFit"
            />
            <image
              v-else-if="tab.status === 5"
              class="tab-svg"
              src="/static/icons/lucide/check.png"
              mode="aspectFit"
            />
            <image
              v-else
              class="tab-svg"
              src="/static/icons/lucide/chevron-left-muted.png"
              mode="aspectFit"
            />
          </view>
          <text class="grid-label">{{ tab.name }}</text>
        </view>
      </view>
    </view>

    <!-- 拼团推荐卡 -->
    <view class="vip-banner-card" @tap="go('/pages/groupon/list')">
      <view class="vbc-text">
        <text class="vbc-title">邻里生鲜好物，一起拼团更划算</text>
        <text class="vbc-sub">查看拼团商品与当前成团进度</text>
      </view>
      <button class="vbc-btn">去拼团</button>
    </view>

    <!-- 常用服务菜单卡片 -->
    <view class="menu-card list-menu-card">
      <view
        class="list-item"
        v-for="item in menus"
        :key="item.name"
        @tap="go(item.url)"
      >
        <view class="li-left">
          <image
            v-if="item.name === '我的收货地址'"
            class="menu-svg"
            src="/static/icons/lucide/map-pin.png"
            mode="aspectFit"
          />
          <image
            v-else-if="item.name === '我的社区拼团'"
            class="menu-svg"
            src="/static/icons/lucide/users.png"
            mode="aspectFit"
          />
          <image
            v-else-if="item.name === '切换自提门店'"
            class="menu-svg"
            src="/static/icons/lucide/store.png"
            mode="aspectFit"
          />
          <image
            v-else
            class="menu-svg"
            src="/static/icons/lucide/clock.png"
            mode="aspectFit"
          />
          <text class="li-title">{{ item.name }}</text>
        </view>
        <image
          class="li-arrow-img"
          src="/static/icons/lucide/chevron-right-muted.png"
          mode="aspectFit"
        />
      </view>
    </view>

    <!-- 退出登录 -->
    <view v-if="user.token" class="menu-card list-menu-card">
      <view class="list-item" @tap="logout">
        <text class="li-title logout-text">退出当前登录</text>
        <image
          class="li-arrow-img"
          src="/static/icons/lucide/chevron-right-muted.png"
          mode="aspectFit"
        />
      </view>
    </view>
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
  background-color: #f6f8f5;
  padding-bottom: calc(60rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(60rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

/* 顶部绿色生鲜头部 */
.profile-header {
  position: relative;
  height: 300rpx;
  background: linear-gradient(135deg, #27744d 0%, #1c5838 100%);
  padding: 50rpx 36rpx 0;
  box-sizing: border-box;
}

.user-row {
  display: flex;
  align-items: center;
}

.avatar-box {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.85);
  overflow: hidden;
  background-color: #eaf5ee;
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
  margin-bottom: 8rpx;
}

.user-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
}

.user-id {
  font-size: 24rpx;
  color: #d8eee1;
}

.header-arrow {
  width: 32rpx;
  height: 32rpx;
}

/* 卡片 */
.menu-card {
  background-color: #ffffff;
  border-radius: 18rpx;
  margin: 0 24rpx 20rpx;
  border: 1rpx solid #e5ebe6;
  box-sizing: border-box;
}

.order-card {
  position: relative;
  z-index: 10;
  margin-top: -36rpx;
  padding: 26rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.ch-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #19241d;
}

.ch-all-box {
  display: flex;
  align-items: center;
}

.ch-all {
  font-size: 24rpx;
  color: #778579;
}

.all-arrow {
  width: 24rpx;
  height: 24rpx;
  margin-left: 4rpx;
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
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background-color: #eaf5ee;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-svg {
  width: 34rpx;
  height: 34rpx;
}

.grid-label {
  font-size: 24rpx;
  color: #19241d;
  margin-top: 10rpx;
}

/* 拼团横幅 */
.vip-banner-card {
  margin: 0 24rpx 20rpx;
  background-color: #eaf5ee;
  border: 1rpx solid #cce8d6;
  border-radius: 18rpx;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.vbc-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.vbc-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #1e5e3a;
  margin-bottom: 4rpx;
}

.vbc-sub {
  font-size: 20rpx;
  color: #55725e;
}

.vbc-btn {
  height: 56rpx;
  line-height: 56rpx;
  background-color: #27744d;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 28rpx;
  padding: 0 30rpx;
  margin: 0 0 0 16rpx;
  border: none;
}

.vbc-btn::after {
  border: none;
}

/* 列表卡片 */
.list-menu-card {
  padding: 0 28rpx;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 96rpx;
  border-bottom: 1rpx solid #eef2ef;
}

.list-item:last-child {
  border-bottom: none;
}

.li-left {
  display: flex;
  align-items: center;
}

.menu-svg {
  width: 34rpx;
  height: 34rpx;
  margin-right: 18rpx;
}

.li-title {
  font-size: 28rpx;
  color: #19241d;
  font-weight: 500;
}

.logout-text {
  color: #c93424;
}

.li-arrow-img {
  width: 28rpx;
  height: 28rpx;
}
</style>
