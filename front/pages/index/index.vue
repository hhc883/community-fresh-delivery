<template>
  <view class="home-page">
    <view
      class="green-header"
      :style="{ paddingTop: statusBarHeight + 8 + 'px' }"
    >
      <view class="search-row">
        <text class="loc-pin">📍</text>
        <view class="search-input-box" @click="goSearch">
          <text class="search-icon">🔍</text>
          <text class="search-placeholder">搜索商品/店铺/自提点...</text>
        </view>
      </view>

      <view class="station-strip" @click="goPickup">
        <text class="station-icon">🏪</text>
        <text class="station-text"
          >自提点：{{ pickupPoint.name || "请选择自提点" }}</text
        >
        <text class="station-arrow">›</text>
      </view>
    </view>

    <view class="main-content">
      <view class="banner-section">
        <swiper
          class="banner-swiper"
          :autoplay="true"
          :circular="true"
          :interval="4500"
          indicator-dots
          indicator-active-color="#25B873"
        >
          <swiper-item v-for="(b, idx) in banners" :key="idx">
            <view class="banner-card">
              <view class="bc-left">
                <text class="bc-tag">{{ b.tag }}</text>
                <text class="bc-title">{{ b.title }}</text>
                <text class="bc-desc">{{ b.desc }}</text>
              </view>
              <view class="bc-right">
                <image class="bc-img" :src="b.img" mode="aspectFill" />
              </view>
            </view>
          </swiper-item>
        </swiper>
      </view>

      <view class="guarantee-row">
        <view class="g-cell"
          ><text class="g-check">✓</text><text>新鲜果蔬</text></view
        >
        <view class="g-cell"
          ><text class="g-check">✓</text><text>社区拼团</text></view
        >
        <view class="g-cell"
          ><text class="g-check">✓</text><text>配送 / 自提</text></view
        >
      </view>

      <view class="kingkong-row">
        <view
          class="kk-item"
          v-for="k in kingkongs"
          :key="k.name"
          @click="onKingkongTap(k)"
        >
          <view class="kk-icon-circle" :style="{ backgroundColor: k.bg }">
            <text class="kk-emoji">{{ k.emoji }}</text>
          </view>
          <text class="kk-name">{{ k.name }}</text>
        </view>
      </view>

      <view class="notice-strip">
        <text class="notice-icon">📢</text>
        <text class="notice-text"
          >配送与自提信息请以订单详情和门店通知为准。</text
        >
      </view>

      <view class="section-title-row">
        <view class="title-bar"></view>
        <text class="title-text">推荐商品</text>
      </view>

      <view v-if="loading" class="empty">正在加载新鲜好物…</view
      ><view v-else-if="error" class="error" @tap="load"
        >{{ error }} · 点击重试</view
      ><view v-else-if="!goodsList.length" class="empty">暂无在售商品</view
      ><view v-else class="goods-waterfall">
        <view
          class="goods-card"
          v-for="item in goodsList"
          :key="item.id"
          @click="goDetail(item)"
        >
          <view class="gc-img-box">
            <image class="gc-img" :src="item.image" mode="aspectFill" />
            <view class="gc-promo-tag" v-if="item.promoTag"
              ><text>{{ item.promoTag }}</text></view
            >
          </view>

          <view class="gc-body">
            <text class="gc-title">{{ item.name }}</text>
            <view class="gc-bottom">
              <view class="gc-price-box">
                <text class="gc-symbol">¥</text>
                <text class="gc-price">{{ $fmtMoney(item.price) }}</text>
              </view>

              <view class="gc-cart-btn" @click.stop="addToCart(item)">
                <text class="cart-plus">+</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="tabbar-safe-placeholder"></view>
    </view>

    <view class="floating-groupon" @click="goGroupon">
      <view class="fg-badge"><text>HOT</text></view>
      <text class="fg-icon">🔥</text>
      <text class="fg-text">拼团</text>
    </view>

    <view v-if="cartCount" class="green-cart-float" @tap="goCart"
      >🛒 {{ cartCount }}</view
    >
  </view>
</template>

<script>
import { getDishes } from "../../api/index.js";
export default {
  data: () => ({
    statusBarHeight: 20,
    goodsList: [],
    loading: true,
    error: "",
    kingkongs: [
      { name: "今日推荐", key: "today", emoji: "📅", bg: "#FFECE8" },
      { name: "商品分类", key: "category", emoji: "🍎", bg: "#E8F3FF" },
      { name: "我的订单", key: "order", emoji: "📦", bg: "#FFEBF3" },
      { name: "生鲜果蔬", key: "fresh", emoji: "🥦", bg: "#E8F7F0" },
      { name: "拼团专区", key: "groupon", emoji: "📢", bg: "#FFF6E8" },
    ],
  }),
  computed: {
    pickupPoint() {
      return this.$store.state.pickupPoint;
    },
    cartCount() {
      return this.$store.getters.cartCount;
    },
    banners() {
      return [
        {
          tag: "社区鲜选",
          title: "把新鲜带回家",
          desc: "精选时令好物，发现身边的新鲜生活。",
          img: this.goodsList[0]?.image || "/static/images/png/dish-3.png",
        },
        {
          tag: "邻里拼团",
          title: "好物一起分享",
          desc: "查看真实团购商品与成团进度。",
          img: this.goodsList[1]?.image || "/static/images/png/dish-3.png",
        },
      ];
    },
  },
  onLoad() {
    try {
      this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20;
    } catch {}
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const r = await getDishes({});
        this.goodsList = Array.isArray(r.data) ? r.data : [];
      } catch (e) {
        this.error = e.message || "加载失败";
      } finally {
        this.loading = false;
      }
    },
    goSearch() {
      uni.navigateTo({ url: "/pages/search/search" });
    },
    goPickup() {
      uni.navigateTo({ url: "/pages/pickup/pickup" });
    },
    goCart() {
      uni.switchTab({ url: "/pages/cart/cart" });
    },
    goGroupon() {
      uni.navigateTo({ url: "/pages/groupon/list" });
    },
    onKingkongTap(k) {
      if (k.key === "groupon") this.goGroupon();
      else if (k.key === "order") uni.navigateTo({ url: "/pages/order/order" });
      else uni.switchTab({ url: "/pages/category/category" });
    },
    goDetail(item) {
      uni.navigateTo({
        url: "/pages/detail/detail?id=" + encodeURIComponent(item.id),
      });
    },
    addToCart(item) {
      if (Number(item.stock) <= 0) {
        uni.showToast({ title: "商品暂时缺货", icon: "none" });
        return;
      }
      this.$store.commit("addToCart", item);
    },
  },
};
</script>
<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #f7f8fa;
  box-sizing: border-box;
}

/* 顶部绿色头部（一体化沉浸式，包含状态栏占位，圆弧过渡） */
.green-header {
  background: linear-gradient(180deg, #25b873 0%, #1ea062 100%);
  padding: 16rpx 24rpx 28rpx;
  border-radius: 0 0 32rpx 32rpx;
  box-sizing: border-box;
  box-shadow: 0 4rpx 20rpx rgba(37, 184, 115, 0.2);
}

.search-row {
  display: flex;
  align-items: center;
}

.loc-pin {
  font-size: 34rpx;
  margin-right: 12rpx;
}

.search-input-box {
  flex: 1;
  display: flex;
  align-items: center;
  height: 68rpx;
  background-color: #ffffff;
  border-radius: 34rpx;
  padding: 0 24rpx;
  /* 留出微信小程序右侧胶囊按钮安全距离 */
  margin-right: 180rpx;
}

.search-icon {
  font-size: 26rpx;
  color: #86909c;
  margin-right: 12rpx;
}

.search-placeholder {
  font-size: 26rpx;
  color: #86909c;
}

.station-strip {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  color: #ffffff;
  font-size: 24rpx;
}

.station-icon {
  margin-right: 8rpx;
  font-size: 26rpx;
}

.station-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-arrow {
  font-size: 28rpx;
}

/* 主内容：原生自然排版，不使用嵌套 scroll-view */
.main-content {
  padding: 0 24rpx;
  box-sizing: border-box;
}

/* Banner */
.banner-section {
  margin-top: -10rpx;
  margin-bottom: 16rpx;
}

.banner-swiper {
  height: 240rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.banner-card {
  height: 100%;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.bc-left {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.bc-tag {
  align-self: flex-start;
  font-size: 20rpx;
  color: #25b873;
  background-color: #e8f7f0;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.bc-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #1f2329;
  margin-bottom: 6rpx;
}

.bc-desc {
  font-size: 20rpx;
  color: #86909c;
  line-height: 28rpx;
}

.bc-right {
  width: 160rpx;
  height: 160rpx;
  margin-left: 20rpx;
}

.bc-img {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
}

/* 服务承诺 */
.guarantee-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background-color: #ffffff;
  border-radius: 12rpx;
  padding: 16rpx 0;
  margin-bottom: 20rpx;
}

.g-cell {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: #25b873;
  font-weight: 500;
}

.g-check {
  margin-right: 6rpx;
  font-weight: 700;
}

/* 金刚区 */
.kingkong-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 16rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.kk-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.kk-icon-circle {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8rpx;
}

.kk-emoji {
  font-size: 44rpx;
}

.kk-name {
  font-size: 22rpx;
  color: #1f2329;
  font-weight: 500;
}

/* 公告 */
.notice-strip {
  display: flex;
  align-items: center;
  background-color: #e8f7f0;
  border-radius: 10rpx;
  padding: 14rpx 20rpx;
  margin-bottom: 20rpx;
  font-size: 22rpx;
  color: #25b873;
}

.notice-icon {
  margin-right: 12rpx;
  font-size: 26rpx;
}

.notice-text {
  flex: 1;
  font-weight: 500;
}

/* 标题 */
.section-title-row {
  display: flex;
  align-items: center;
  margin: 16rpx 0;
}

.title-bar {
  width: 8rpx;
  height: 32rpx;
  background-color: #25b873;
  border-radius: 4rpx;
  margin-right: 12rpx;
}

.title-text {
  font-size: 30rpx;
  font-weight: 800;
  color: #1f2329;
}

/* 瀑布流 */
.goods-waterfall {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.goods-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.gc-img-box {
  width: 100%;
  height: 320rpx;
  background-color: #f7f8fa;
  position: relative;
}

.gc-img {
  width: 100%;
  height: 100%;
}

.gc-promo-tag {
  position: absolute;
  left: 12rpx;
  top: 12rpx;
  background: linear-gradient(90deg, #ff6b35 0%, #ff2442 100%);
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 700;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.gc-body {
  padding: 16rpx;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.gc-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1f2329;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  height: 72rpx;
}

.gc-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}

.gc-price-box {
  display: flex;
  align-items: baseline;
}

.gc-symbol {
  font-size: 22rpx;
  color: #ff5e1e;
  font-weight: 700;
}

.gc-price {
  font-size: 34rpx;
  color: #ff5e1e;
  font-weight: 800;
  font-family: Arial, Helvetica, sans-serif;
}

.gc-cart-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background-color: #25b873;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(37, 184, 115, 0.35);
}

.cart-plus {
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1;
  margin-top: -2rpx;
}

/* 底部防 TabBar 遮挡留白 */
.tabbar-safe-placeholder {
  height: calc(140rpx + constant(safe-area-inset-bottom));
  height: calc(140rpx + env(safe-area-inset-bottom));
}

/* 悬浮拼团按钮（平铺常规类选择器，符合微信WXSS） */
.floating-groupon {
  position: fixed;
  right: 28rpx;
  bottom: 280rpx;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35 0%, #ff2442 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 20rpx rgba(255, 36, 66, 0.38);
  z-index: 998;
}

.floating-groupon .fg-badge {
  position: absolute;
  top: -8rpx;
  right: -4rpx;
  background: #ffd21e;
  color: #9e1000;
  font-size: 16rpx;
  font-weight: 800;
  padding: 2rpx 8rpx;
  border-radius: 12rpx;
  border: 2rpx solid #ffffff;
  line-height: 1.2;
}

.floating-groupon .fg-icon {
  font-size: 32rpx;
  line-height: 1;
}

.floating-groupon .fg-text {
  font-size: 20rpx;
  color: #ffffff;
  font-weight: 700;
  margin-top: 2rpx;
  line-height: 1;
}

.green-cart-float {
  position: fixed;
  bottom: 150rpx;
  left: 28rpx;
  background-color: #25b873;
  color: #ffffff;
  border-radius: 50rpx;
  padding: 20rpx 28rpx;
  font-size: 26rpx;
  font-weight: 600;
  box-shadow: 0 6rpx 18rpx rgba(37, 184, 115, 0.35);
  z-index: 998;
}
</style>
