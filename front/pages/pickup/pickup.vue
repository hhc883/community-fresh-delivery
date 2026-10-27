<template>
  <view class="core">
    <view class="hero">
      <view class="pickup-hero-head">
        <image
          class="pickup-hero-svg"
          src="/static/icons/lucide/store-white.png"
          mode="aspectFit"
        />
        <text class="title">选择就近自提网点</text>
      </view>
      <text class="sub">生鲜商品将直接送达您选中的门店，到店出示提货凭证即可领取。</text>
    </view>

    <view v-if="loading" class="empty">正在加载自提网点…</view>
    <view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    >
    <view v-else-if="!stores.length" class="empty">暂无可用的自提网点</view>

    <!-- 门店列表 -->
    <view
      v-for="store in stores"
      :key="store.id"
      :class="['card', 'store-card', currentPointId === String(store.id) ? 'store-selected' : '']"
      @tap="select(store)"
    >
      <view class="row store-head-row">
        <view class="store-name-box">
          <image
            class="store-icon"
            src="/static/icons/lucide/store.png"
            mode="aspectFit"
          />
          <text class="product-name store-name">{{ store.name }}</text>
        </view>
        <view class="select-badge" v-if="currentPointId === String(store.id)">
          <text>当前选择</text>
        </view>
        <image
          v-else
          class="store-arrow"
          src="/static/icons/lucide/chevron-right-muted.png"
          mode="aspectFit"
        />
      </view>

      <view class="store-addr-box">
        <image
          class="addr-icon"
          src="/static/icons/lucide/map-pin.png"
          mode="aspectFit"
        />
        <text class="sub addr-text">{{ store.address || "自提地址暂未提供" }}</text>
      </view>

      <view v-if="store.phone" class="store-phone-box">
        <image
          class="phone-icon"
          src="/static/icons/lucide/phone.png"
          mode="aspectFit"
        />
        <text class="sub phone-text">{{ store.phone }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getStores } from "../../api/index.js";
export default {
  data: () => ({ stores: [], loading: true, error: "" }),
  computed: {
    currentPointId() {
      return String(this.$store.state.pickupPoint?.id || "");
    },
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const r = await getStores();
        this.stores = Array.isArray(r.data) ? r.data : r.data?.records || [];
      } catch (e) {
        this.error = e.message || "门店读取失败";
      } finally {
        this.loading = false;
      }
    },
    select(store) {
      this.$store.commit("setPickupPoint", store);
      uni.navigateBack({
        fail: () => uni.switchTab({ url: "/pages/index/index" }),
      });
    },
  },
};
</script>

<style scoped>
.pickup-hero-head {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}

.pickup-hero-svg {
  width: 38rpx;
  height: 38rpx;
  margin-right: 12rpx;
}

.store-card {
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid #e5ebe6;
  box-sizing: border-box;
}

.store-card.store-selected {
  border-color: #27744d;
  background-color: #fcfffd;
  box-shadow: 0 4rpx 14rpx rgba(39, 116, 77, 0.1);
}

.store-head-row {
  margin-bottom: 12rpx;
}

.store-name-box {
  display: flex;
  align-items: center;
  flex: 1;
}

.store-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 10rpx;
  flex-shrink: 0;
}

.store-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #19241d;
}

.select-badge {
  background-color: #27744d;
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.store-arrow {
  width: 28rpx;
  height: 28rpx;
}

.store-addr-box,
.store-phone-box {
  display: flex;
  align-items: flex-start;
  margin-top: 6rpx;
}

.addr-icon,
.phone-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 8rpx;
  margin-top: 4rpx;
  flex-shrink: 0;
}

.addr-text,
.phone-text {
  flex: 1;
  line-height: 1.4;
}
</style>
