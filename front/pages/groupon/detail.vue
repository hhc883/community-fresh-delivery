<template>
  <view class="core">
    <view class="groupon-detail-header">
      <text class="title">拼团详情</text>
      <text class="sub">邀请邻里一起拼团，享受特惠价格。</text>
    </view>

    <view v-if="loading" class="empty">正在读取拼团详情…</view>
    <view v-else-if="error" class="error" @tap="load">{{ error }} · 点击重试</view>

    <!-- 商品与拼团信息卡片 -->
    <view v-if="dish" class="card stack dish-group-card">
      <image
        v-if="dish.image"
        class="product-image dish-group-img"
        :src="dish.image"
        mode="aspectFill"
      />
      <text class="title">{{ dish.name }}</text>
      <view class="price-row">
        <text class="price-symbol">¥</text>
        <text class="price-val">{{ $fmtMoney(dish.grouponPrice) }}</text>
        <text class="group-size-tag">{{ dish.grouponSize }}人成团</text>
      </view>
      <text class="sub"
        >成团有效期：{{ dish.grouponValidHours }} 小时</text
      >
      <text class="sub dish-desc-text" v-if="dish.description">{{ dish.description }}</text>

      <view class="group-action-row">
        <button v-if="Number(dish.isGroupon) === 1" class="primary btn-open-group" @tap="open">
          我要发起拼团
        </button>
        <button class="secondary btn-single-buy" @tap="single">
          单独购买 ¥{{ $fmtMoney(dish.price) }}
        </button>
      </view>
    </view>

    <!-- 当前拼团卡片（已指定团时） -->
    <view v-if="group" class="card stack current-group-card">
      <view class="row">
        <text class="product-name">当前团状态</text>
        <text
          :class="['group-status-badge', Number(group.status) === 1 ? 'status-active' : 'status-end']"
          >{{ labels[group.status] || "未知状态" }}</text
        >
      </view>
      <view class="progress-box">
        <text class="progress-main"
          >已拼 {{ group.currentSize }} / {{ group.requiredSize }} 人</text
        >
        <text class="progress-sub" v-if="Number(group.status) === 1">还差 {{ group.lackSize }} 人成团</text>
      </view>
      <text class="sub">截止时间（北京时间）：{{ group.expireTime }}</text>

      <view class="member-list-box">
        <text class="member-title">参团成员</text>
        <view class="line row" v-for="(m, index) in group.members || []" :key="index">
          <text class="member-name">{{ m.userName || "团员" }}</text>
          <text class="member-role"
            >{{ Number(m.isLeader) === 1 ? "团长" : "团员" }} · {{ m.number }} 份</text
          >
        </view>
      </view>

      <button
        v-if="Number(group.status) === 1"
        class="primary"
        @tap="join(group)"
      >
        参与此团
      </button>
      <button
        v-if="group.orderId"
        class="secondary"
        @tap="order(group.orderId)"
      >
        查看我的关联订单
      </button>
    </view>

    <!-- 可加入的团列表（未指定团时） -->
    <view v-if="!group" class="joinable-section">
      <text class="product-name joinable-title">正在进行中的团</text>
      <view v-if="!groups.length && !loading && !error" class="empty">
        暂无正在进行中的拼团，您可以发起新团。
      </view>
      <view v-for="g in groups" :key="g.id" class="card stack joinable-card">
        <view class="row">
          <text class="leader-text">{{ g.leaderName || "邻里团长" }} 发起的拼团</text>
          <text class="lack-tag">差 {{ g.lackSize }} 人</text>
        </view>
        <text class="sub"
          >进度：{{ g.currentSize }} / {{ g.requiredSize }} 人 · 截止：{{ g.expireTime }}</text
        >
        <button class="primary join-btn" @tap="join(g)">加入此团</button>
      </view>
    </view>

    <button class="secondary refresh-btn" @tap="load">刷新拼团状态</button>
    <text class="sub bottom-hint-text"
      >拼团需在规定时间内达到所需人数即算成团，成团后将按订单方式履约。</text
    >
  </view>
</template>

<script>
import {
  getDishDetail,
  getGrouponDetail,
  getJoinableGroupons,
} from "../../api/index.js";
export default {
  data: () => ({
    dishId: "",
    id: "",
    dish: null,
    group: null,
    groups: [],
    loading: true,
    error: "",
    labels: { 1: "进行中", 2: "已成团", 3: "已失败", 4: "已过期" },
  }),
  onLoad(o) {
    this.dishId = o.dishId || "";
    this.id = o.id || o.grouponId || "";
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        if (this.id) {
          this.group = (await getGrouponDetail(this.id)).data;
          this.dishId = this.group.dishId;
        }
        if (!this.dishId) throw new Error("缺少商品或拼团标识");
        this.dish = (await getDishDetail(this.dishId)).data;
        if (!this.id) {
          this.groups = (await getJoinableGroupons(this.dishId)).data || [];
        }
      } catch (e) {
        this.error = e.message || "读取失败";
      } finally {
        this.loading = false;
      }
    },
    open() {
      uni.navigateTo({
        url:
          "/pages/groupon/confirm?mode=open&dishId=" +
          encodeURIComponent(this.dishId),
      });
    },
    join(g) {
      uni.navigateTo({
        url:
          "/pages/groupon/confirm?mode=join&dishId=" +
          encodeURIComponent(this.dishId) +
          "&grouponId=" +
          encodeURIComponent(g.id),
      });
    },
    single() {
      uni.navigateTo({
        url: "/pages/order/confirm?dishId=" + encodeURIComponent(this.dishId),
      });
    },
    order(id) {
      uni.navigateTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(id),
      });
    },
  },
};
</script>

<style scoped>
.groupon-detail-header {
  margin-bottom: 20rpx;
}

.dish-group-card {
  padding: 24rpx;
}

.dish-group-img {
  width: 100%;
  height: 380rpx;
  border-radius: 12rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin: 6rpx 0;
}

.price-symbol {
  font-size: 24rpx;
  color: #ff5722;
  font-weight: 700;
}

.price-val {
  font-size: 40rpx;
  color: #ff5722;
  font-weight: 700;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.group-size-tag {
  margin-left: 14rpx;
  background-color: #fff2ec;
  color: #ff5722;
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.dish-desc-text {
  margin: 4rpx 0 12rpx;
}

.group-action-row {
  display: flex;
  gap: 16rpx;
  margin-top: 10rpx;
}

.btn-open-group {
  flex: 1;
  margin: 0;
}

.btn-single-buy {
  flex: 1;
  margin: 0;
}

.current-group-card {
  padding: 24rpx;
}

.group-status-badge {
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
}

.status-active {
  background-color: #eaf5ee;
  color: #27744d;
}

.status-end {
  background-color: #f0f4f1;
  color: #778579;
}

.progress-box {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 10rpx 0;
}

.progress-main {
  font-size: 28rpx;
  font-weight: 600;
  color: #19241d;
}

.progress-sub {
  font-size: 24rpx;
  color: #ff5722;
  font-weight: 600;
}

.member-list-box {
  background-color: #f6f8f5;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin: 14rpx 0;
  border: 1rpx solid #e1e8e2;
}

.member-title {
  font-size: 24rpx;
  color: #778579;
  display: block;
  margin-bottom: 8rpx;
}

.member-name {
  font-size: 26rpx;
  color: #19241d;
}

.member-role {
  font-size: 24rpx;
  color: #556257;
}

.joinable-section {
  margin-top: 24rpx;
}

.joinable-title {
  display: block;
  margin-bottom: 16rpx;
}

.joinable-card {
  padding: 22rpx;
}

.leader-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #19241d;
}

.lack-tag {
  background-color: #fff2ec;
  color: #ff5722;
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.join-btn {
  margin: 12rpx 0 0;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
}

.refresh-btn {
  margin-top: 24rpx;
}

.bottom-hint-text {
  text-align: center;
  margin-top: 14rpx;
}
</style>
