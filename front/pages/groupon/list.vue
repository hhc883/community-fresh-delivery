<template>
  <view class="core">
    <!-- 顶部拼团大厅看板 -->
    <view class="hero">
      <view class="hero-top-row">
        <image
          class="hero-icon"
          src="/static/icons/lucide/users-white.png"
          mode="aspectFit"
        />
        <text class="title">邻里拼团大厅</text>
      </view>
      <text class="sub"
        >选择商品开团或参与现有拼团；成团后安排履约，若拼团到期未成自动退款。</text
      >
    </view>

    <!-- 我的拼团入口 -->
    <view class="my-groupon-bar" @tap="mine">
      <view class="mgb-left">
        <image
          class="mgb-icon"
          src="/static/icons/lucide/users.png"
          mode="aspectFit"
        />
        <text class="mgb-title">查看我的拼团记录</text>
      </view>
      <image
        class="mgb-arrow"
        src="/static/icons/lucide/chevron-right-muted.png"
        mode="aspectFit"
      />
    </view>

    <view v-if="loading" class="empty">正在加载拼团商品…</view>
    <view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    >
    <view v-else-if="!rows.length" class="empty">暂无可参与的拼团商品</view>

    <!-- 拼团商品流 -->
    <view
      v-for="d in rows"
      :key="d.id"
      class="card groupon-card"
      @tap="detail(d)"
    >
      <view class="groupon-item-row">
        <image
          v-if="d.image"
          class="groupon-thumb"
          :src="d.image"
          mode="aspectFill"
        />
        <view class="groupon-info">
          <text class="product-name">{{ d.name }}</text>
          <view class="groupon-tags-row">
            <text class="groupon-badge">{{ d.grouponSize }}人成团</text>
            <text class="groupon-stock">库存 {{ d.stock }}</text>
          </view>
          <view class="groupon-price-box">
            <text class="price-symbol">¥</text>
            <text class="price-val">{{ $fmtMoney(d.grouponPrice) }}</text>
            <text v-if="d.price" class="price-del">¥{{ $fmtMoney(d.price) }}</text>
          </view>
        </view>
      </view>

      <view class="groupon-btn-row">
        <view class="open-group-btn">
          <text>查看拼团详情</text>
          <image
            class="btn-arrow"
            src="/static/icons/lucide/chevron-right.png"
            mode="aspectFit"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getGrouponDishList } from "../../api/index.js";
export default {
  data: () => ({ rows: [], loading: true, error: "" }),
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        this.rows = (await getGrouponDishList()).data || [];
      } catch (e) {
        this.error = e.message;
      } finally {
        this.loading = false;
      }
    },
    detail(d) {
      uni.navigateTo({
        url: "/pages/groupon/detail?dishId=" + encodeURIComponent(d.id),
      });
    },
    mine() {
      uni.navigateTo({ url: "/pages/groupon/my" });
    },
  },
};
</script>

<style scoped>
.hero-top-row {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}

.hero-icon {
  width: 38rpx;
  height: 38rpx;
  margin-right: 12rpx;
}

.my-groupon-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #e5ebe6;
}

.mgb-left {
  display: flex;
  align-items: center;
}

.mgb-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
}

.mgb-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #19241d;
}

.mgb-arrow {
  width: 24rpx;
  height: 24rpx;
}

.groupon-card {
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.groupon-item-row {
  display: flex;
}

.groupon-thumb {
  width: 170rpx;
  height: 170rpx;
  border-radius: 12rpx;
  background-color: #f1f4f1;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.groupon-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.groupon-tags-row {
  display: flex;
  align-items: center;
  margin: 6rpx 0;
}

.groupon-badge {
  background-color: #fff2ec;
  color: #ff5722;
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
  margin-right: 12rpx;
}

.groupon-stock {
  font-size: 22rpx;
  color: #778579;
}

.groupon-price-box {
  display: flex;
  align-items: baseline;
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

.price-del {
  font-size: 22rpx;
  color: #8c9b8f;
  text-decoration: line-through;
  margin-left: 12rpx;
}

.groupon-btn-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid #eef2ef;
}

.open-group-btn {
  display: inline-flex;
  align-items: center;
  font-size: 24rpx;
  color: #27744d;
  font-weight: 600;
  padding: 8rpx 18rpx;
  background-color: #eaf5ee;
  border-radius: 24rpx;
}

.btn-arrow {
  width: 20rpx;
  height: 20rpx;
  margin-left: 4rpx;
}
</style>
