<template>
  <view class="detail-page">
    <view v-if="loading" class="empty">商品加载中…</view>
    <view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    >
    <template v-else-if="dish">
      <scroll-view scroll-y class="detail-scroll">
        <!-- 顶部商品实物大图 -->
        <view class="hero-swiper">
          <image
            v-if="dish.image"
            style="width: 100%; height: 680rpx"
            :src="dish.image"
            mode="aspectFill"
          />
          <view v-else class="empty">暂无生鲜实物图</view>
        </view>

        <!-- 价格与库存看板 -->
        <view class="promo-banner">
          <view class="pb-left">
            <text class="pb-tag">商品价格</text>
            <view class="pb-price-row">
              <text class="pb-symbol">¥</text>
              <text class="pb-price">{{ $fmtMoney(dish.price) }}</text>
            </view>
          </view>
          <view class="pb-right">
            <text class="pb-title">新鲜好物</text>
            <text class="pb-sub"
              >库存 {{ dish.stock }} {{ dish.unit || "份" }}</text
            >
          </view>
        </view>

        <!-- 商品标题与基本属性 -->
        <view class="goods-main-card">
          <text class="goods-title">{{ dish.name }}</text>
          <view class="price-sales-row">
            <text class="sales-text"
              >计量规格：{{ dish.unit || "份"
              }}<text v-if="dish.origin"> · 产地 {{ dish.origin }}</text></text
            >
          </view>
        </view>

        <!-- 履约保障与自提网点卡片 -->
        <view class="section-card">
          <view class="service-row">
            <text class="sr-label">履约方式</text>
            <view class="service-val-box">
              <image
                class="sr-icon"
                src="/static/icons/lucide/check.png"
                mode="aspectFit"
              />
              <text class="service-text">配送到家 / 门店自提</text>
            </view>
          </view>
          <view class="divider" />
          <view class="station-row" @tap="goPickup">
            <text class="sr-label">提货网点</text>
            <view class="station-val">
              <image
                class="sr-icon"
                src="/static/icons/lucide/store.png"
                mode="aspectFit"
              />
              <text class="station-name">{{
                pickupPoint.name || "请选择就近自提网点"
              }}</text>
              <image
                class="arrow-img"
                src="/static/icons/lucide/chevron-right.png"
                mode="aspectFit"
              />
            </view>
          </view>
          <view
            v-if="Number(dish.isGroupon) === 1"
            class="station-row"
            @tap="group"
          >
            <text class="sr-label">社区拼团</text>
            <view class="station-val">
              <image
                class="sr-icon"
                src="/static/icons/lucide/users.png"
                mode="aspectFit"
              />
              <text class="station-name">查看正在拼团的商品详情</text>
              <image
                class="arrow-img"
                src="/static/icons/lucide/chevron-right.png"
                mode="aspectFit"
              />
            </view>
          </view>
        </view>

        <!-- 图文详情分割线 -->
        <view class="detail-divider">
          <view class="divider-line" />
          <text class="detail-title">商品图文介绍</text>
          <view class="divider-line" />
        </view>

        <view class="detail-content">
          <view class="desc-box">
            <text class="desc-text">{{
              dish.description || "暂无商品介绍"
            }}</text>
          </view>
          <image
            v-if="dish.image"
            class="detail-img"
            :src="dish.image"
            mode="widthFix"
          />
        </view>
      </scroll-view>

      <!-- 底部固定行动栏 -->
      <view class="bottom-action-bar">
        <view class="left-icons">
          <view class="icon-btn" @tap="goCart">
            <image
              class="bottom-cart-img"
              src="/static/icons/lucide/shopping-cart.png"
              mode="aspectFit"
            />
            <text class="ib-label"
              >购物车{{ cartCount ? " (" + cartCount + ")" : "" }}</text
            >
          </view>
        </view>
        <view class="right-buttons">
          <button
            class="btn-cart-green"
            :disabled="Number(dish.stock) <= 0"
            @tap="add"
          >
            加入购物车
          </button>
          <button
            class="btn-buy-orange"
            :disabled="Number(dish.stock) <= 0"
            @tap="buy"
          >
            立即购买
          </button>
        </view>
      </view>
    </template>
    <view v-else class="empty">商品不存在或已下架</view>
  </view>
</template>

<script>
import { getDishDetail } from "../../api/index.js";
export default {
  data: () => ({ id: "", dish: null, loading: true, error: "" }),
  onLoad(o) {
    this.id = o.id || "";
    this.load();
  },
  computed: {
    pickupPoint() {
      return this.$store.state.pickupPoint;
    },
    cartCount() {
      return this.$store.getters.cartCount;
    },
  },
  methods: {
    goPickup() {
      uni.navigateTo({ url: "/pages/pickup/pickup" });
    },
    goCart() {
      uni.switchTab({ url: "/pages/cart/cart" });
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        if (!this.id) throw new Error("缺少商品标识");
        this.dish = (await getDishDetail(this.id)).data;
      } catch (e) {
        this.error = e.message || "商品加载失败";
        this.dish = null;
      } finally {
        this.loading = false;
      }
    },
    add() {
      if (!this.dish) return;
      this.$store.commit("addToCart", this.dish);
    },
    buy() {
      if (!this.dish) return;
      uni.navigateTo({
        url: "/pages/order/confirm?dishId=" + encodeURIComponent(this.id),
      });
    },
    group() {
      if (!this.dish) return;
      uni.navigateTo({
        url: "/pages/groupon/detail?dishId=" + encodeURIComponent(this.id),
      });
    },
  },
};
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f6f8f5;
}

.detail-scroll {
  flex: 1;
  height: calc(100vh - 120rpx);
  box-sizing: border-box;
}

/* 轮播大图 */
.hero-swiper {
  width: 100%;
  height: 680rpx;
  background-color: #ffffff;
}

/* 绿色生鲜活动价格条 */
.promo-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 116rpx;
  background: linear-gradient(135deg, #27744d 0%, #1c5838 100%);
  padding: 0 28rpx;
  color: #ffffff;
}

.pb-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.pb-tag {
  font-size: 20rpx;
  line-height: 1;
  color: #d8eee1;
  margin-bottom: 6rpx;
}

.pb-price-row {
  display: flex;
  align-items: baseline;
}

.pb-symbol {
  font-size: 28rpx;
  font-weight: 700;
  margin-right: 4rpx;
}

.pb-price {
  font-size: 52rpx;
  font-weight: 700;
  line-height: 1;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.pb-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
}

.pb-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 6rpx;
}

.pb-sub {
  font-size: 22rpx;
  color: #d8eee1;
}

/* 标题与属性卡片 */
.goods-main-card {
  background-color: #ffffff;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #eef2ef;
}

.goods-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #19241d;
  line-height: 44rpx;
  margin-bottom: 12rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.price-sales-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sales-text {
  font-size: 24rpx;
  color: #778579;
}

/* 规格、配送与自提卡片 */
.section-card {
  margin-top: 18rpx;
  background-color: #ffffff;
  padding: 0 28rpx;
  border-top: 1rpx solid #eef2ef;
  border-bottom: 1rpx solid #eef2ef;
}

.divider {
  height: 1rpx;
  background-color: #eef2ef;
}

.service-row,
.station-row {
  display: flex;
  align-items: center;
  height: 96rpx;
}

.sr-label {
  width: 140rpx;
  font-size: 26rpx;
  color: #778579;
  font-weight: 500;
}

.service-val-box {
  display: flex;
  align-items: center;
}

.sr-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 8rpx;
  flex-shrink: 0;
}

.service-text {
  font-size: 26rpx;
  color: #19241d;
  font-weight: 500;
}

.station-val {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.station-name {
  font-size: 26rpx;
  color: #27744d;
  font-weight: 600;
}

.arrow-img {
  width: 24rpx;
  height: 24rpx;
  margin-left: 8rpx;
  flex-shrink: 0;
}

/* 图文详情分割线 */
.detail-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36rpx 0 20rpx;
}

.divider-line {
  width: 60rpx;
  height: 1rpx;
  background-color: #d8deda;
}

.detail-title {
  font-size: 26rpx;
  color: #778579;
  margin: 0 18rpx;
  font-weight: 500;
}

.detail-content {
  background-color: #ffffff;
  padding: 24rpx;
  margin-bottom: 40rpx;
}

.desc-box {
  background-color: #f6f8f5;
  padding: 20rpx 24rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid #e1e8e2;
}

.desc-text {
  font-size: 26rpx;
  color: #4b5a4f;
  line-height: 40rpx;
}

.detail-img {
  width: 100%;
  display: block;
  margin-bottom: 12rpx;
  border-radius: 12rpx;
}

/* 底部操作栏 */
.bottom-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: calc(108rpx + constant(safe-area-inset-bottom));
  height: calc(108rpx + env(safe-area-inset-bottom));
  padding: 0 24rpx constant(safe-area-inset-bottom);
  padding: 0 24rpx env(safe-area-inset-bottom);
  background-color: #ffffff;
  border-top: 1rpx solid #e5ebe6;
  box-sizing: border-box;
  z-index: 99;
}

.left-icons {
  display: flex;
  align-items: center;
}

.icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 16rpx;
}

.bottom-cart-img {
  width: 40rpx;
  height: 40rpx;
}

.ib-label {
  font-size: 20rpx;
  color: #4b5a4f;
  margin-top: 4rpx;
}

.right-buttons {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.btn-cart-green,
.btn-buy-orange {
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 38rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #ffffff;
  padding: 0 34rpx;
  margin: 0;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(39, 116, 77, 0.2);
}

.btn-cart-green::after,
.btn-buy-orange::after {
  border: none;
}

.btn-cart-green {
  background-color: #27744d;
}

.btn-buy-orange {
  background-color: #ff5722;
  box-shadow: 0 4rpx 12rpx rgba(255, 87, 34, 0.25);
}

.btn-cart-green[disabled],
.btn-buy-orange[disabled] {
  background-color: #c9cdd4 !important;
  color: #ffffff !important;
  box-shadow: none !important;
  opacity: 0.7;
}
</style>
