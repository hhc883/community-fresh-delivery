<template>
  <view class="home-page">
    <!-- 顶部生鲜晨市头部（一体化沉浸式，包含状态栏占位） -->
    <view
      class="green-header"
      :style="{ paddingTop: statusBarHeight + 8 + 'px' }"
    >
      <view class="search-row">
        <view class="loc-pin-box" @click="goPickup">
          <image
            class="header-icon"
            src="/static/icons/lucide/map-pin-white.png"
            mode="aspectFit"
          />
        </view>
        <view class="search-input-box" @click="goSearch">
          <image
            class="search-icon-img"
            src="/static/icons/lucide/search-muted.png"
            mode="aspectFit"
          />
          <text class="search-placeholder">搜索新鲜果蔬/时令水产/自提点...</text>
        </view>
      </view>

      <view class="station-strip" @click="goPickup">
        <image
          class="station-icon-img"
          src="/static/icons/lucide/store-white.png"
          mode="aspectFit"
        />
        <text class="station-text"
          >当前自提点：{{ pickupPoint.name || "请选择就近自提点" }}</text
        >
        <image
          class="station-arrow-img"
          src="/static/icons/lucide/chevron-right-white.png"
          mode="aspectFit"
        />
      </view>
    </view>

    <view class="main-content">
      <!-- 晨间鲜选 Banner 轮播 -->
      <view class="banner-section">
        <swiper
          class="banner-swiper"
          :autoplay="true"
          :circular="true"
          :interval="4500"
          indicator-dots
          indicator-active-color="#27744D"
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

      <!-- 品质与履约说明 -->
      <view class="guarantee-row">
        <view class="g-cell">
          <image
            class="g-check-icon"
            src="/static/icons/lucide/check.png"
            mode="aspectFit"
          />
          <text>新鲜果蔬</text>
        </view>
        <view class="g-cell">
          <image
            class="g-check-icon"
            src="/static/icons/lucide/check.png"
            mode="aspectFit"
          />
          <text>邻里拼团</text>
        </view>
        <view class="g-cell">
          <image
            class="g-check-icon"
            src="/static/icons/lucide/check.png"
            mode="aspectFit"
          />
          <text>配送到家 / 社区自提</text>
        </view>
      </view>

      <!-- 金刚区快捷导航 -->
      <view class="kingkong-row">
        <view
          class="kk-item"
          v-for="k in kingkongs"
          :key="k.name"
          @click="onKingkongTap(k)"
        >
          <view class="kk-icon-circle" :style="{ backgroundColor: k.bg }">
            <image
              v-if="k.key === 'order'"
              class="kk-svg-icon"
              src="/static/icons/lucide/package.png"
              mode="aspectFit"
            />
            <image
              v-else-if="k.key === 'groupon'"
              class="kk-svg-icon"
              src="/static/icons/lucide/users.png"
              mode="aspectFit"
            />
            <image
              v-else-if="k.key === 'fresh'"
              class="kk-svg-icon"
              src="/static/icons/lucide/shopping-bag.png"
              mode="aspectFit"
            />
            <image
              v-else-if="k.key === 'category'"
              class="kk-svg-icon"
              src="/static/icons/lucide/store.png"
              mode="aspectFit"
            />
            <image
              v-else
              class="kk-svg-icon"
              src="/static/icons/lucide/clock.png"
              mode="aspectFit"
            />
          </view>
          <text class="kk-name">{{ k.name }}</text>
        </view>
      </view>

      <!-- 市集公告栏 -->
      <view class="notice-strip">
        <image
          class="notice-icon-img"
          src="/static/icons/lucide/clock.png"
          mode="aspectFit"
        />
        <text class="notice-text"
          >配送与自提信息请以订单详情和门店通知为准。</text
        >
      </view>

      <!-- 推荐商品双列市集 -->
      <view class="section-title-row">
        <view class="title-bar"></view>
        <text class="title-text">推荐商品</text>
        <text class="title-sub">时令精选 · 社区自提</text>
      </view>

      <view v-if="loading" class="empty">正在拉取早市新鲜好物…</view>
      <view v-else-if="error" class="error" @tap="load"
        >{{ error }} · 点击重试</view
      >
      <view v-else-if="!goodsList.length" class="empty">暂无在售生鲜商品</view>
      <view v-else class="goods-waterfall">
        <view
          class="goods-card"
          v-for="item in goodsList"
          :key="item.id"
          @click="goDetail(item)"
        >
          <view class="gc-img-box">
            <image class="gc-img" :src="item.image" mode="aspectFill" />
            <view class="gc-promo-tag" v-if="item.promoTag">
              <text>{{ item.promoTag }}</text>
            </view>
          </view>

          <view class="gc-body">
            <text class="gc-title">{{ item.name }}</text>
            <view class="gc-bottom">
              <view class="gc-price-box">
                <text class="gc-symbol">¥</text>
                <text class="gc-price">{{ $fmtMoney(item.price) }}</text>
              </view>

              <view class="gc-cart-btn" @click.stop="addToCart(item)">
                <image
                  class="gc-cart-icon"
                  src="/static/icons/lucide/plus-white.png"
                  mode="aspectFit"
                />
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="tabbar-safe-placeholder"></view>
    </view>

    <!-- 悬浮社区拼团入口 -->
    <view class="floating-groupon" @click="goGroupon">
      <view class="fg-badge"><text>HOT</text></view>
      <image
        class="fg-icon-img"
        src="/static/icons/lucide/users-white.png"
        mode="aspectFit"
      />
      <text class="fg-text">拼团</text>
    </view>

    <!-- 悬浮购物车计数 -->
    <view v-if="cartCount" class="green-cart-float" @tap="goCart">
      <image
        class="cart-float-icon"
        src="/static/icons/lucide/shopping-cart-white.png"
        mode="aspectFit"
      />
      <text class="cart-float-count">{{ cartCount }}</text>
    </view>
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
      { name: "今日推荐", key: "today", emoji: "📅", bg: "#EAF5EE" },
      { name: "商品分类", key: "category", emoji: "🍎", bg: "#EAF5EE" },
      { name: "我的订单", key: "order", emoji: "📦", bg: "#EAF5EE" },
      { name: "生鲜果蔬", key: "fresh", emoji: "🥦", bg: "#EAF5EE" },
      { name: "拼团专区", key: "groupon", emoji: "📢", bg: "#FFF2EC" },
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
  background-color: #f6f8f5;
  box-sizing: border-box;
}

/* 顶部绿色头部（生鲜主绿 #27744D，自然渐变与柔和边界） */
.green-header {
  background: linear-gradient(180deg, #27744d 0%, #1c5838 100%);
  padding: 16rpx 24rpx 28rpx;
  border-radius: 0 0 28rpx 28rpx;
  box-sizing: border-box;
}

.search-row {
  display: flex;
  align-items: center;
}

.loc-pin-box {
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.header-icon {
  width: 38rpx;
  height: 38rpx;
}

.search-input-box {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  height: 68rpx;
  background-color: #ffffff;
  border-radius: 34rpx;
  padding: 0 24rpx;
  /* #ifdef MP-WEIXIN */
  margin-right: 180rpx;
  /* #endif */
  /* #ifndef MP-WEIXIN */
  margin-right: 0;
  /* #endif */
  border: 1rpx solid #e1e8e2;
  box-sizing: border-box;
  overflow: hidden;
}

.search-icon-img {
  width: 30rpx;
  height: 30rpx;
  margin-right: 14rpx;
  flex-shrink: 0;
}

.search-placeholder {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: #778579;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.station-strip {
  display: flex;
  align-items: center;
  margin-top: 18rpx;
  padding: 8rpx 16rpx;
  background-color: rgba(255, 255, 255, 0.12);
  border-radius: 20rpx;
  color: #ffffff;
  font-size: 24rpx;
}

.station-icon-img {
  width: 28rpx;
  height: 28rpx;
  margin-right: 10rpx;
  flex-shrink: 0;
}

.station-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-arrow-img {
  width: 24rpx;
  height: 24rpx;
  margin-left: 8rpx;
  flex-shrink: 0;
}

/* 主内容 */
.main-content {
  padding: 0 24rpx;
  box-sizing: border-box;
}

/* Banner */
.banner-section {
  margin-top: -12rpx;
  margin-bottom: 20rpx;
}

.banner-swiper {
  height: 230rpx;
  border-radius: 18rpx;
  overflow: hidden;
}

.banner-card {
  height: 100%;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  border-radius: 18rpx;
  border: 1rpx solid #e5ebe6;
  box-sizing: border-box;
}

.bc-left {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.bc-tag {
  align-self: flex-start;
  font-size: 20rpx;
  color: #27744d;
  background-color: #eaf5ee;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.bc-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #19241d;
  margin-bottom: 6rpx;
}

.bc-desc {
  font-size: 22rpx;
  color: #778579;
  line-height: 30rpx;
}

.bc-right {
  width: 150rpx;
  height: 150rpx;
  margin-left: 20rpx;
}

.bc-img {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
}

/* 履约承诺行 */
.guarantee-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background-color: #ffffff;
  border-radius: 14rpx;
  padding: 18rpx 0;
  margin-bottom: 20rpx;
  border: 1rpx solid #e9eee9;
}

.g-cell {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: #27744d;
  font-weight: 500;
}

.g-check-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 8rpx;
  flex-shrink: 0;
}

/* 金刚区 */
.kingkong-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-radius: 18rpx;
  padding: 24rpx 16rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid #e5ebe6;
}

.kk-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.kk-icon-circle {
  width: 84rpx;
  height: 84rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
}

.kk-svg-icon {
  width: 40rpx;
  height: 40rpx;
}

.kk-name {
  font-size: 24rpx;
  color: #19241d;
  font-weight: 500;
}

/* 公告栏 */
.notice-strip {
  display: flex;
  align-items: center;
  background-color: #eaf5ee;
  border-radius: 12rpx;
  padding: 14rpx 20rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #d4ebdc;
}

.notice-icon-img {
  width: 28rpx;
  height: 28rpx;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.notice-text {
  flex: 1;
  font-size: 22rpx;
  color: #27744d;
  font-weight: 500;
}

/* 栏目标题 */
.section-title-row {
  display: flex;
  align-items: baseline;
  margin: 20rpx 0 16rpx;
}

.title-bar {
  width: 6rpx;
  height: 28rpx;
  background-color: #27744d;
  border-radius: 4rpx;
  margin-right: 12rpx;
  align-self: center;
}

.title-text {
  font-size: 30rpx;
  font-weight: 700;
  color: #19241d;
  margin-right: 12rpx;
}

.title-sub {
  font-size: 22rpx;
  color: #778579;
}

/* 双列商品流 */
.goods-waterfall {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
}

.goods-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
  border: 1rpx solid #e5ebe6;
  display: flex;
  flex-direction: column;
}

.gc-img-box {
  width: 100%;
  height: 310rpx;
  background-color: #f1f4f1;
  position: relative;
}

.gc-img {
  width: 100%;
  height: 100%;
}

.gc-promo-tag {
  position: absolute;
  left: 10rpx;
  top: 10rpx;
  background-color: #ff5722;
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 600;
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
  color: #19241d;
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
  color: #ff5722;
  font-weight: 700;
}

.gc-price {
  font-size: 34rpx;
  color: #ff5722;
  font-weight: 700;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.gc-cart-btn {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background-color: #27744d;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gc-cart-icon {
  width: 26rpx;
  height: 26rpx;
}

/* 底部防 TabBar 遮挡 */
.tabbar-safe-placeholder {
  height: calc(140rpx + constant(safe-area-inset-bottom));
  height: calc(140rpx + env(safe-area-inset-bottom));
}

/* 悬浮拼团按钮 */
.floating-groupon {
  position: fixed;
  right: 28rpx;
  bottom: 260rpx;
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff5722 0%, #e64a19 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 16rpx rgba(255, 87, 34, 0.35);
  z-index: 998;
}

.floating-groupon .fg-badge {
  position: absolute;
  top: -6rpx;
  right: -2rpx;
  background: #ffd21e;
  color: #9e1000;
  font-size: 16rpx;
  font-weight: 800;
  padding: 2rpx 8rpx;
  border-radius: 10rpx;
  line-height: 1.2;
}

.fg-icon-img {
  width: 32rpx;
  height: 32rpx;
}

.floating-groupon .fg-text {
  font-size: 18rpx;
  color: #ffffff;
  font-weight: 600;
  margin-top: 2rpx;
  line-height: 1;
}

/* 悬浮购物车 */
.green-cart-float {
  position: fixed;
  bottom: 150rpx;
  left: 28rpx;
  background-color: #27744d;
  color: #ffffff;
  border-radius: 40rpx;
  padding: 16rpx 26rpx;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  box-shadow: 0 4rpx 14rpx rgba(39, 116, 77, 0.3);
  z-index: 998;
}

.cart-float-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 8rpx;
}

.cart-float-count {
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
  font-size: 26rpx;
}

/* #ifdef H5 */
@media screen and (min-width: 481px) {
  .home-page {
    max-width: 480px;
    margin-left: auto;
    margin-right: auto;
  }

  .floating-groupon {
    right: calc(50% - 240px + 28rpx);
  }

  .green-cart-float {
    left: calc(50% - 240px + 28rpx);
  }
}
/* #endif */
</style>
