<template>
  <view class="detail-page"
    ><view v-if="loading" class="empty">商品加载中…</view
    ><view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    ><template v-else-if="dish">
      <scroll-view scroll-y class="detail-scroll"
        ><view class="hero-swiper"
          ><image
            v-if="dish.image"
            style="width: 100%; height: 680rpx"
            :src="dish.image"
            mode="aspectFill"
          /><view v-else class="empty">暂无商品图片</view></view
        >
        <view class="promo-banner"
          ><view class="pb-left"
            ><text class="pb-tag">商品价格</text
            ><view class="pb-price-row"
              ><text class="pb-symbol">¥</text
              ><text class="pb-price">{{ $fmtMoney(dish.price) }}</text></view
            ></view
          ><view class="pb-right"
            ><text class="pb-title">新鲜好物</text
            ><text class="pb-sub"
              >库存 {{ dish.stock }} {{ dish.unit || "份" }}</text
            ></view
          ></view
        >
        <view class="goods-main-card"
          ><text class="goods-title">{{ dish.name }}</text
          ><view class="price-sales-row"
            ><text class="sales-text"
              >{{ dish.unit || "份"
              }}<text v-if="dish.origin"> · 产地 {{ dish.origin }}</text></text
            ></view
          ></view
        >
        <view class="section-card"
          ><view class="service-row"
            ><text class="sr-label">履约方式</text
            ><text class="service-text">配送到家 / 门店自提</text></view
          ><view class="divider" /><view class="station-row" @tap="goPickup"
            ><text class="sr-label">自提网点</text
            ><view class="station-val"
              ><text class="station-name">{{
                pickupPoint.name || "请选择自提点"
              }}</text
              ><text class="arrow">›</text></view
            ></view
          ><view
            v-if="Number(dish.isGroupon) === 1"
            class="station-row"
            @tap="group"
            ><text class="sr-label">社区拼团</text
            ><text class="station-name">查看可参与的团购 ›</text></view
          ></view
        >
        <view class="detail-divider"
          ><view class="line" /><text class="detail-title">商品详情</text
          ><view class="line" /></view
        ><view class="detail-content"
          ><view class="desc-box"
            ><text class="desc-text">{{
              dish.description || "暂无商品介绍"
            }}</text></view
          ><image
            v-if="dish.image"
            class="detail-img"
            :src="dish.image"
            mode="widthFix"
        /></view> </scroll-view
      ><view class="bottom-action-bar"
        ><view class="left-icons"
          ><view class="icon-btn" @tap="goCart"
            ><text class="ib-icon">🛒</text
            ><text class="ib-label">购物车 {{ cartCount || "" }}</text></view
          ></view
        ><view class="right-buttons"
          ><button
            class="btn-cart-green"
            :disabled="Number(dish.stock) <= 0"
            @tap="add"
          >
            加入购物车</button
          ><button
            class="btn-buy-blue"
            :disabled="Number(dish.stock) <= 0"
            @tap="buy"
          >
            立即购买
          </button></view
        ></view
      > </template
    ><view v-else class="empty">商品不存在或已下架</view></view
  >
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
  background-color: #f7f8fa;
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
  background: linear-gradient(135deg, #25b873 0%, #1ea062 100%);
  padding: 0 28rpx;
  color: #ffffff;
  box-shadow: 0 4rpx 16rpx rgba(37, 184, 115, 0.2);
}

.pb-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.pb-tag {
  font-size: 20rpx;
  line-height: 1;
  color: #e5f7ed;
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
  font-weight: 800;
  line-height: 1;
  font-family: Arial, Helvetica, sans-serif;
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
  color: #e5f7ed;
}

/* 标题与产地销量卡片 */
.goods-main-card {
  background-color: #ffffff;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #edf1ee;
}

.goods-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2329;
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
  color: #86909c;
}

/* 规格、配送与自提卡片 */
.section-card {
  margin-top: 18rpx;
  background-color: #ffffff;
  padding: 0 28rpx;
  border-top: 1rpx solid #edf1ee;
  border-bottom: 1rpx solid #edf1ee;
}

.divider {
  height: 1rpx;
  background-color: #f2f5f3;
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
  color: #86909c;
  font-weight: 500;
}

.service-text {
  font-size: 26rpx;
  color: #1f2329;
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
  color: #25b873;
  font-weight: 600;
}

.arrow {
  font-size: 30rpx;
  color: #c9cdd4;
  margin-left: 8rpx;
}

/* 图文详情分割线 */
.detail-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36rpx 0 20rpx;
}

.detail-divider .line {
  width: 60rpx;
  height: 1rpx;
  background-color: #d8deda;
}

.detail-title {
  font-size: 26rpx;
  color: #86909c;
  margin: 0 18rpx;
  font-weight: 500;
}

.detail-content {
  background-color: #ffffff;
  padding: 24rpx;
  margin-bottom: 40rpx;
}

.desc-box {
  background-color: #f7f9f8;
  padding: 20rpx 24rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid #eef2f0;
}

.desc-text {
  font-size: 26rpx;
  color: #4e5969;
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
  border-top: 1rpx solid #edf1ee;
  box-shadow: 0 -4rpx 16rpx rgba(31, 35, 41, 0.05);
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

.ib-icon {
  font-size: 38rpx;
  line-height: 1;
}

.ib-label {
  font-size: 20rpx;
  color: #4e5969;
  margin-top: 6rpx;
}

.right-buttons {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.btn-cart-green,
.btn-buy-blue {
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 38rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #ffffff;
  padding: 0 34rpx;
  margin: 0;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(37, 184, 115, 0.25);
}

.btn-cart-green::after,
.btn-buy-blue::after {
  border: none;
}

.btn-cart-green {
  background-color: #25b873;
}

.btn-buy-blue {
  background-color: #1ea062;
}

.btn-cart-green[disabled],
.btn-buy-blue[disabled] {
  background-color: #c9cdd4 !important;
  color: #ffffff !important;
  box-shadow: none !important;
  opacity: 0.7;
}
</style>
