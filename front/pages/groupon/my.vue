<template>
  <view class="core">
    <view class="my-groupon-header">
      <text class="title">我的拼团</text>
      <text class="sub">查看参与及发起的社区拼团状态与成团进度。</text>
    </view>

    <!-- 未登录提示 -->
    <view v-if="!loggedIn" class="card">
      <text class="login-tip-text">请先登录后查看我的拼团记录</text>
      <button class="primary" @tap="login">去登录</button>
    </view>

    <template v-else>
      <!-- 状态筛选胶囊 -->
      <view class="chips">
        <text
          v-for="(label, value) in filters"
          :key="value"
          :class="['chip', String(status) === value ? 'active' : '']"
          @tap="filter(value)"
          >{{ label }}</text
        >
      </view>

      <view v-if="loading" class="empty">正在读取我的拼团记录…</view>
      <view v-else-if="error" class="error" @tap="load"
        >{{ error }} · 点击重试</view
      >
      <view v-else-if="!rows.length" class="empty">暂无相关拼团记录</view>

      <!-- 拼团记录列表 -->
      <view v-for="g in rows" :key="g.id" class="card stack my-group-card">
        <view class="row card-head-row">
          <text class="product-name group-dish-title">{{ g.dishName }}</text>
          <text
            :class="['status-badge', Number(g.status) === 1 ? 'badge-ongoing' : (Number(g.status) === 2 ? 'badge-success' : 'badge-ended')]"
            >{{ labels[g.status] }}</text
          >
        </view>

        <view class="row progress-row">
          <text class="progress-info"
            >当前进度：{{ g.currentSize }} / {{ g.requiredSize }} 人</text
          >
          <text v-if="Number(g.status) === 1" class="lack-info">还差 {{ g.lackSize }} 人</text>
        </view>

        <text class="sub time-sub">截止时间：{{ g.expireTime }}（北京时间）</text>

        <view class="row price-box-row">
          <text class="price-title">拼团单价：</text>
          <view class="price">
            <text class="price-symbol">¥</text>
            <text class="price-val">{{ $fmtMoney(g.grouponPrice) }}</text>
          </view>
        </view>

        <view class="card-btn-actions">
          <button class="secondary btn-detail" @tap="detail(g)">查看拼团详情</button>
          <button v-if="g.orderId" class="primary btn-order" @tap="order(g)">
            查看关联订单 / 支付
          </button>
        </view>
      </view>

      <text class="sub bottom-tip"
        >若拼团到期未成自动关闭，已付款部分记录退款，具体可在订单详情中查看。</text
      >
    </template>
  </view>
</template>

<script>
import { getMyGroupons } from "../../api/index.js";
export default {
  data: () => ({
    status: "",
    rows: [],
    loading: true,
    error: "",
    filters: { "": "全部", 1: "进行中", 2: "已成团", 3: "已失败", 4: "已过期" },
    labels: { 1: "进行中", 2: "已成团", 3: "已失败", 4: "已过期" },
  }),
  computed: {
    loggedIn() {
      return Boolean(this.$store.state.user?.token);
    },
  },
  onShow() {
    if (this.loggedIn) {
      this.load();
    } else {
      this.loading = false;
    }
  },
  methods: {
    login() {
      uni.navigateTo({ url: "/pages/login/login" });
    },
    filter(s) {
      this.status = s;
      this.load();
    },
    async load() {
      if (!this.loggedIn) return;
      this.loading = true;
      this.error = "";
      try {
        this.rows = (await getMyGroupons(this.status || undefined)).data || [];
      } catch (e) {
        this.error = e.message || "拼团加载失败";
        this.rows = [];
      } finally {
        this.loading = false;
      }
    },
    detail(g) {
      uni.navigateTo({
        url: "/pages/groupon/detail?id=" + encodeURIComponent(g.id),
      });
    },
    order(g) {
      uni.navigateTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(g.orderId),
      });
    },
  },
};
</script>

<style scoped>
.my-groupon-header {
  margin-bottom: 20rpx;
}

.login-tip-text {
  font-size: 28rpx;
  color: #19241d;
  display: block;
  text-align: center;
  margin: 20rpx 0;
}

.my-group-card {
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.card-head-row {
  border-bottom: 1rpx solid #eef2ef;
  padding-bottom: 14rpx;
}

.group-dish-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #19241d;
  flex: 1;
  margin-right: 12rpx;
}

.status-badge {
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
}

.badge-ongoing {
  background-color: #fff2ec;
  color: #ff5722;
}

.badge-success {
  background-color: #eaf5ee;
  color: #27744d;
}

.badge-ended {
  background-color: #f0f4f1;
  color: #778579;
}

.progress-row {
  margin-top: 10rpx;
}

.progress-info {
  font-size: 26rpx;
  color: #19241d;
  font-weight: 500;
}

.lack-info {
  font-size: 24rpx;
  color: #ff5722;
  font-weight: 600;
}

.time-sub {
  margin-bottom: 6rpx;
}

.price-box-row {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding: 10rpx 0;
}

.price-title {
  font-size: 24rpx;
  color: #778579;
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

.card-btn-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 10rpx;
}

.btn-detail {
  flex: 1;
  margin: 0;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
}

.btn-order {
  flex: 1;
  margin: 0;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
}

.bottom-tip {
  display: block;
  text-align: center;
  margin: 24rpx 0 16rpx;
}
</style>
