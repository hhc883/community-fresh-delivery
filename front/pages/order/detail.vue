<template>
  <view class="core">
    <view class="detail-header">
      <text class="title">订单详情</text>
      <text class="sub">订单履约凭证与状态实时流转记录</text>
    </view>

    <view v-if="loading" class="empty">正在读取订单状态…</view>
    <view v-if="error" class="error" @tap="load">{{ error }} · 点击重试</view>

    <template v-if="order">
      <!-- 状态横幅看板 -->
      <view class="hero">
        <text class="title">{{ labels[order.status] || "未知状态" }}</text>
        <text class="sub"
          >{{ payLabels[order.payStatus] }} ·
          {{ isPickup ? "社区自提" : "配送到家" }}</text
        >
        <text
          class="sub groupon-hint-text"
          v-if="order.grouponId && String(order.grouponId) !== '0'"
          >社区拼团订单需成团并付款后完成配送履约</text
        >
      </view>

      <!-- 提货凭证与履约信息卡片 -->
      <view class="card stack">
        <view class="info-row">
          <text class="info-label">订单编号：</text>
          <text class="info-val font-mono">{{
            order.number || order.orderNumber
          }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">下单时间：</text>
          <text class="info-val">{{ order.orderTime }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">订单金额：</text>
          <text class="price"
            ><text class="price-symbol">¥</text
            >{{ $fmtMoney(order.amount) }}</text
          >
        </view>

        <!-- 自提点信息 -->
        <view v-if="isPickup" class="station-detail-box">
          <view class="station-title-row">
            <image
              class="box-icon"
              src="/static/icons/lucide/store.png"
              mode="aspectFit"
            />
            <text class="station-box-name">{{
              order.pickupPointName ||
              order.pickupPointAddress ||
              "自提网点 #" + order.pickupPointId
            }}</text>
          </view>
          <text
            v-if="
              isPickup && order.pickupPointAddress && order.pickupPointName
            "
            class="sub station-box-addr"
            >{{ order.pickupPointAddress }}</text
          >
        </view>

        <!-- 配送地址 -->
        <view v-if="!isPickup && order.address" class="delivery-address-box">
          <image
            class="box-icon"
            src="/static/icons/lucide/map-pin.png"
            mode="aspectFit"
          />
          <text class="del-addr-text">收货地址：{{ order.address }}</text>
        </view>

        <view class="info-row">
          <text class="info-label">联系人：</text>
          <text class="info-val"
            >{{ order.pickupPersonName || order.consignee || "-" }}
            {{ order.pickupPersonPhone || order.phone || "" }}</text
          >
        </view>

        <!-- 提货核销码（仅限自提且已付款） -->
        <view
          v-if="isPickup && order.pickupCode && Number(order.payStatus) === 1"
          class="pickup-code-section"
        >
          <text class="code-title">门店自提核销凭证码</text>
          <view class="code">{{ order.pickupCode }}</view>
          <text class="sub code-hint"
            >到店请出示此核销码供团长或店员验证取货</text
          >
        </view>

        <view v-if="Number(order.verifyStatus) === 1" class="verify-badge">
          <image
            class="verify-icon"
            src="/static/icons/lucide/check.png"
            mode="aspectFit"
          />
          <text>已于 {{ order.verifyTime }} 完成到店核销提货</text>
        </view>

        <view v-if="order.cancelReason" class="cancel-reason-box">
          <text class="cancel-label">取消原因：</text>
          <text class="cancel-text">{{ order.cancelReason }}</text>
        </view>
      </view>

      <!-- 购买商品清单 -->
      <view class="card">
        <text class="product-name">商品明细清单</text>
        <view
          class="line row item-line"
          v-for="d in order.orderDetailList || []"
          :key="d.id"
        >
          <text class="detail-goods-name">{{ d.name }} ×{{ d.number }}</text>
          <text class="detail-goods-price">¥{{ $fmtMoney(d.amount) }}</text>
        </view>
      </view>

      <!-- 操作按钮流 -->
      <button v-if="Number(order.status) === 1" class="primary" @tap="pay">
        去模拟支付
      </button>
      <button
        v-if="[1, 2].includes(Number(order.status))"
        class="secondary cancel-btn"
        :loading="cancelling"
        :disabled="cancelling"
        @tap="cancel"
      >
        取消订单
      </button>
      <button
        v-if="order.grouponId && String(order.grouponId) !== '0'"
        class="secondary"
        @tap="group"
      >
        查看拼团进度
      </button>
      <button class="secondary" @tap="load">刷新状态</button>
    </template>
  </view>
</template>

<script>
import { getOrderDetail, cancelOrder } from "../../api/index.js";
export default {
  data: () => ({
    id: "",
    order: null,
    loading: true,
    error: "",
    cancelling: false,
    labels: {
      1: "待付款",
      2: "待接单 / 待履约",
      3: "已接单",
      4: "配送中",
      5: "已完成",
      6: "已取消",
    },
    payLabels: { 0: "未付款", 1: "已模拟付款", 2: "已模拟退款" },
  }),
  computed: {
    isPickup() {
      if (!this.order) return false;
      // deliveryType: 1 为配送到家，2 为社区自提。
      // 配送订单绝不显示提货码与自提点
      if (Number(this.order.deliveryType) === 1) return false;
      if (Number(this.order.deliveryType) === 2) return true;
      return Boolean(this.order.pickupPointId);
    },
  },
  onLoad(o) {
    this.id = o.id || "";
  },
  onShow() {
    this.load();
  },
  methods: {
    group() {
      uni.navigateTo({
        url:
          "/pages/groupon/detail?id=" + encodeURIComponent(this.order.grouponId),
      });
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        if (!this.id) throw new Error("缺少订单号");
        this.order = (await getOrderDetail(this.id)).data;
      } catch (e) {
        this.error = e.message;
        this.order = null;
      } finally {
        this.loading = false;
      }
    },
    pay() {
      uni.navigateTo({
        url: "/pages/order/pay?orderId=" + encodeURIComponent(this.id),
      });
    },
    cancel() {
      if (this.cancelling) return;
      uni.showModal({
        title: "取消订单",
        content: "确认取消此订单？已付款订单只记录模拟退款。",
        success: async (r) => {
          if (!r.confirm) return;
          this.cancelling = true;
          try {
            await cancelOrder(this.id);
            await this.load();
          } catch (e) {
            this.error = e.message || "取消失败";
          } finally {
            this.cancelling = false;
          }
        },
      });
    },
  },
};
</script>

<style scoped>
.detail-header {
  margin-bottom: 20rpx;
}

.groupon-hint-text {
  margin-top: 8rpx;
  font-size: 22rpx;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0;
  font-size: 26rpx;
}

.info-label {
  color: #778579;
}

.info-val {
  color: #19241d;
  font-weight: 500;
}

.font-mono {
  font-family: monospace, Arial, sans-serif;
}

.station-detail-box,
.delivery-address-box {
  background-color: #f6f8f5;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin: 12rpx 0;
  border: 1rpx solid #e1e8e2;
}

.station-title-row,
.delivery-address-box {
  display: flex;
  align-items: center;
}

.box-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 10rpx;
  flex-shrink: 0;
}

.station-box-name,
.del-addr-text {
  font-size: 26rpx;
  font-weight: 600;
  color: #19241d;
}

.station-box-addr {
  margin-top: 6rpx;
  padding-left: 38rpx;
}

.pickup-code-section {
  margin: 16rpx 0;
  text-align: center;
}

.code-title {
  display: block;
  font-size: 24rpx;
  color: #27744d;
  font-weight: 600;
  margin-bottom: 6rpx;
}

.code-hint {
  text-align: center;
}

.verify-badge {
  display: flex;
  align-items: center;
  background-color: #eaf5ee;
  padding: 12rpx 20rpx;
  border-radius: 10rpx;
  color: #27744d;
  font-size: 24rpx;
  font-weight: 500;
  margin: 10rpx 0;
}

.verify-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 8rpx;
}

.cancel-reason-box {
  background-color: #fdf2f1;
  border: 1rpx solid #fad2ce;
  border-radius: 10rpx;
  padding: 12rpx 20rpx;
  font-size: 24rpx;
  color: #c93424;
  margin: 10rpx 0;
}

.item-line {
  padding: 16rpx 0;
}

.detail-goods-name {
  font-size: 26rpx;
  color: #19241d;
  font-weight: 500;
}

.detail-goods-price {
  font-size: 26rpx;
  color: #ff5722;
  font-weight: 600;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.cancel-btn {
  color: #c93424;
  border-color: #fed2ce;
  background-color: #fff6f5;
}
</style>
