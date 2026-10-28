<template>
  <view class="core">
    <view class="pay-header">
      <text class="title">模拟收银台</text>
      <text class="sub">本系统仅模拟支付与退款记账，不发起真实资金扣款。</text>
    </view>

    <view v-if="loading" class="empty">正在读取订单待付信息…</view>
    <view v-if="error" class="error">{{ error }}</view>

    <view v-if="order" class="card stack pay-card">
      <view class="row order-no-row">
        <text class="pay-label">待付订单：</text>
        <text class="pay-order-no">{{ order.number || order.orderNumber || order.id }}</text>
      </view>

      <view class="amount-hero-box">
        <text class="amount-tip">支付金额</text>
        <view class="price-box">
          <text class="price-symbol">¥</text>
          <text class="price-val">{{ $fmtMoney(order.amount) }}</text>
        </view>
      </view>

      <view class="method-box">
        <view class="method-left">
          <image
            class="method-icon"
            src="/static/icons/lucide/check.png"
            mode="aspectFit"
          />
          <text class="method-name">模拟支付</text>
        </view>
        <text class="method-desc">不会扣款</text>
      </view>

      <button
        v-if="Number(order.status) === 1 && Number(order.payStatus) === 0"
        class="primary btn-pay-action"
        :disabled="paying"
        :loading="paying"
        @tap="pay"
      >
        确认模拟支付 ¥{{ $fmtMoney(order.amount) }}
      </button>

      <view v-else class="processed-box">
        <text class="processed-text">该订单已处理完成，请查看详情。</text>
      </view>

      <button class="secondary btn-detail-action" @tap="detail">查看订单详情</button>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, mockPay } from "../../api/index.js";
export default {
  data: () => ({
    id: "",
    order: null,
    error: "",
    loading: true,
    paying: false,
  }),
  onLoad(o) {
    this.id = o.orderId || "";
    this.load();
  },
  methods: {
    async load() {
      try {
        if (!this.id) throw new Error("缺少订单号");
        this.order = (await getOrderDetail(this.id)).data;
      } catch (e) {
        this.error = e.message;
      } finally {
        this.loading = false;
      }
    },
    async pay() {
      if (this.paying) return;
      this.paying = true;
      this.error = "";
      try {
        await mockPay(this.id);
        uni.showToast({ title: "模拟支付成功", icon: "success" });
        uni.redirectTo({
          url: "/pages/order/detail?id=" + encodeURIComponent(this.id),
        });
      } catch (e) {
        this.error = e.message || "模拟支付失败，请重试";
      } finally {
        this.paying = false;
      }
    },
    detail() {
      uni.redirectTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(this.id),
      });
    },
  },
};
</script>

<style scoped>
.pay-header {
  margin-bottom: 24rpx;
}

.pay-card {
  padding: 32rpx 28rpx;
}

.order-no-row {
  border-bottom: 1rpx solid #eef2ef;
  padding-bottom: 16rpx;
}

.pay-label {
  font-size: 26rpx;
  color: #778579;
}

.pay-order-no {
  font-size: 26rpx;
  color: #19241d;
  font-family: monospace, Arial, sans-serif;
  font-weight: 500;
}

.amount-hero-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36rpx 0 28rpx;
}

.amount-tip {
  font-size: 24rpx;
  color: #778579;
  margin-bottom: 8rpx;
}

.price-box {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 32rpx;
  color: #ff5722;
  font-weight: 700;
  margin-right: 4rpx;
}

.price-val {
  font-size: 60rpx;
  color: #ff5722;
  font-weight: 700;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.method-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f6f8f5;
  border-radius: 14rpx;
  padding: 20rpx 24rpx;
  margin: 12rpx 0 24rpx;
  border: 1rpx solid #e1e8e2;
}

.method-left {
  display: flex;
  align-items: center;
}

.method-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 12rpx;
}

.method-name {
  font-size: 26rpx;
  color: #19241d;
  font-weight: 500;
}

.method-desc {
  font-size: 22rpx;
  color: #27744d;
  background-color: #eaf5ee;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  font-weight: 600;
}

.btn-pay-action {
  margin: 12rpx 0;
}

.processed-box {
  text-align: center;
  padding: 16rpx 0;
}

.processed-text {
  font-size: 26rpx;
  color: #778579;
}

.btn-detail-action {
  margin-top: 8rpx;
}
</style>
