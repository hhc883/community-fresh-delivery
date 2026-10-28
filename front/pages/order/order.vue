<template>
  <view class="core">
    <view class="order-list-header">
      <text class="title">我的订单</text>
      <text class="sub">交易记录及金额均来自服务器，支持随时查看履约与提货凭证。</text>
    </view>

    <!-- 未登录 -->
    <view v-if="!loggedIn" class="card">
      <text class="login-tip-text">请先登录后查看订单与生鲜提货凭证</text>
      <button class="primary" @tap="login">去登录</button>
    </view>

    <template v-else>
      <!-- 状态筛选胶囊 -->
      <view class="chips">
        <text
          v-for="s in filters"
          :key="s.value"
          :class="['chip', status === s.value ? 'active' : '']"
          @tap="filter(s.value)"
          >{{ s.label }}</text
        >
      </view>

      <view v-if="loading" class="empty">正在读取订单列表…</view>
      <view v-if="error" class="error" @tap="load">{{ error }} · 点击重试</view>
      <view v-if="!loading && !error && !rows.length" class="empty">
        暂无该状态下的订单记录
      </view>

      <!-- 订单卡片列表 -->
      <view class="card order-item-card" v-for="o in rows" :key="o.id" @tap="detail(o)">
        <view class="row order-head-row">
          <text class="order-no">单号：{{ o.number || o.orderNumber }}</text>
          <text
            :class="['order-badge', o.status === 1 ? 'badge-warn' : (o.status === 5 ? 'badge-success' : 'badge-default')]"
            >{{ labels[o.status] || "未知状态" }}</text
          >
        </view>

        <view class="order-details-box">
          <view class="line line-goods" v-for="d in o.orderDetailList || []" :key="d.id">
            <text class="goods-name">{{ d.name }}</text>
            <text class="goods-num">×{{ d.number }}</text>
          </view>
        </view>

        <view class="row order-meta-row">
          <text class="sub meta-time">{{ o.orderTime }}</text>
          <view class="delivery-badge">
            <image
              v-if="Number(o.deliveryType) === 2 || (Number(o.deliveryType) !== 1 && o.pickupPointId)"
              class="del-type-icon"
              src="/static/icons/lucide/store.png"
              mode="aspectFit"
            />
            <image
              v-else
              class="del-type-icon"
              src="/static/icons/lucide/truck.png"
              mode="aspectFit"
            />
            <text>{{ Number(o.deliveryType) === 2 || (Number(o.deliveryType) !== 1 && o.pickupPointId) ? "门店自提" : "配送到家" }}</text>
          </view>
        </view>

        <view class="total order-total-row">
          <text class="pay-tag">{{ payLabels[o.payStatus] }}</text>
          <text class="price-symbol">¥</text>
          <text class="price-val">{{ $fmtMoney(o.amount) }}</text>
        </view>

        <view class="order-action-row">
          <view class="detail-link">
            <text>查看订单详情</text>
            <image
              class="detail-arrow"
              src="/static/icons/lucide/chevron-right.png"
              mode="aspectFit"
            />
          </view>
        </view>
      </view>

      <!-- 分页控制 -->
      <view v-if="!loading && !error && rows.length" class="row pagination-row">
        <button class="secondary page-btn" :disabled="page === 1" @tap="turn(-1)">
          上一页
        </button>
        <text class="page-num">{{ page }} / {{ Math.max(1, Math.ceil(total / 10)) }}</text>
        <button class="secondary page-btn" :disabled="page * 10 >= total" @tap="turn(1)">
          下一页
        </button>
      </view>
    </template>
  </view>
</template>

<script>
import { getOrders } from "../../api/index.js";
export default {
  data: () => ({
    rows: [],
    page: 1,
    total: 0,
    status: "",
    loading: false,
    error: "",
    labels: {
      1: "待付款",
      2: "待接单",
      3: "已接单",
      4: "配送中",
      5: "已完成",
      6: "已取消",
    },
    payLabels: { 0: "未付款", 1: "已付款", 2: "模拟退款" },
    filters: [
      { value: "", label: "全部" },
      { value: 1, label: "待付款" },
      { value: 2, label: "待履约" },
      { value: 5, label: "已完成" },
      { value: 6, label: "已取消" },
    ],
  }),
  computed: {
    loggedIn() {
      return Boolean(this.$store.state.user?.token);
    },
  },
  onLoad(options) {
    const status = Number(options.status);
    if ([1, 2, 5, 6].includes(status)) this.status = status;
  },
  onShow() {
    if (this.loggedIn) {
      this.load();
    }
  },
  methods: {
    login() {
      uni.navigateTo({ url: "/pages/login/login" });
    },
    async load() {
      if (!this.loggedIn) return;
      this.loading = true;
      this.error = "";
      try {
        const d = (await getOrders(this.page, 10, this.status)).data;
        this.rows = d?.records || [];
        this.total = Number(d?.total) || 0;
      } catch (e) {
        this.error = e.message || "订单加载失败";
        this.rows = [];
      } finally {
        this.loading = false;
      }
    },
    filter(s) {
      this.status = s;
      this.page = 1;
      this.load();
    },
    turn(n) {
      this.page += n;
      this.load();
    },
    detail(o) {
      uni.navigateTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(o.id),
      });
    },
  },
};
</script>

<style scoped>
.order-list-header {
  margin-bottom: 20rpx;
}

.login-tip-text {
  font-size: 28rpx;
  color: #19241d;
  display: block;
  text-align: center;
  margin: 20rpx 0;
}

.order-item-card {
  padding: 24rpx;
}

.order-head-row {
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #eef2ef;
}

.order-no {
  font-size: 24rpx;
  color: #556257;
  font-family: monospace, Arial, sans-serif;
}

.order-badge {
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}

.badge-warn {
  background-color: #fff2ec;
  color: #ff5722;
}

.badge-success {
  background-color: #eaf5ee;
  color: #27744d;
}

.badge-default {
  background-color: #f0f4f1;
  color: #778579;
}

.order-details-box {
  padding: 10rpx 0;
}

.line-goods {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f6f8f5;
}

.goods-name {
  font-size: 26rpx;
  color: #19241d;
  font-weight: 500;
}

.goods-num {
  font-size: 24rpx;
  color: #778579;
}

.order-meta-row {
  margin-top: 12rpx;
  margin-bottom: 8rpx;
}

.meta-time {
  font-size: 22rpx;
  color: #778579;
}

.delivery-badge {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: #27744d;
  background-color: #eaf5ee;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.del-type-icon {
  width: 22rpx;
  height: 22rpx;
  margin-right: 6rpx;
}

.order-total-row {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding: 14rpx 0 10rpx;
}

.pay-tag {
  font-size: 22rpx;
  color: #778579;
  font-weight: 400;
  margin-right: 12rpx;
}

.price-symbol {
  font-size: 22rpx;
  color: #ff5722;
  font-weight: 700;
}

.price-val {
  font-size: 34rpx;
  color: #ff5722;
  font-weight: 700;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.order-action-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10rpx;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  font-size: 24rpx;
  color: #27744d;
  font-weight: 600;
  padding: 8rpx 16rpx;
  background-color: #eaf5ee;
  border-radius: 24rpx;
}

.detail-arrow {
  width: 22rpx;
  height: 22rpx;
  margin-left: 4rpx;
}

.pagination-row {
  margin: 30rpx 0 20rpx;
}

.page-btn {
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
  padding: 0 28rpx;
}

.page-num {
  font-size: 26rpx;
  color: #556257;
  font-weight: 500;
}
</style>
