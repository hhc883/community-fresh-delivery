<template>
  <view class="page-container">
    <!-- 顶部常驻搜索导航栏 -->
    <view class="nav-bar">
      <view class="nav-back" @tap="back">
        <image
          class="nav-back-icon"
          src="/static/icons/lucide/chevron-left.png"
          mode="aspectFit"
        />
      </view>
      <view class="nav-search-box">
        <image
          class="nav-search-svg"
          src="/static/icons/lucide/search-muted.png"
          mode="aspectFit"
        />
        <input
          class="nav-search-input"
          v-model="keyword"
          :focus="true"
          placeholder="输入商品名称"
          confirm-type="search"
          @confirm="search"
        />
        <view class="nav-search-divider" />
        <view class="nav-search-go" @tap="search">
          <text class="nav-search-go-text">搜索</text>
        </view>
      </view>
    </view>

    <view class="page-body">
      <!-- 历史搜索词 -->
      <view v-if="history.length" class="ss-block">
        <view class="ss-head">
          <view class="ss-title-row">
            <image
              class="history-icon"
              src="/static/icons/lucide/clock.png"
              mode="aspectFit"
            />
            <text class="ss-title">最近搜索</text>
          </view>
          <text class="ss-clear-text" @tap="clearHistory">清空</text>
        </view>
        <view class="ss-chips">
          <view
            class="ss-chip"
            v-for="word in history"
            :key="word"
            @tap="selectWord(word)"
          >
            <text class="ss-chip-text">{{ word }}</text>
          </view>
        </view>
      </view>

      <view v-if="loading" class="empty">正在搜索商品…</view>
      <view v-else-if="error" class="error" @tap="search"
        >{{ error }} · 点击重试</view
      >
      <view v-else-if="searched && !rows.length" class="empty"
        >没有找到相关生鲜商品，换个关键词试试</view
      >

      <!-- 搜索结果列表 -->
      <view v-if="rows.length" class="ss-block">
        <view class="ss-head">
          <text class="ss-title">{{
            searched ? "搜索结果" : "在售好物"
          }}</text>
        </view>
        <view class="rank-item" v-for="dish in rows" :key="dish.id">
          <view class="rank-img" @tap="detail(dish)">
            <image
              v-if="dish.image"
              class="rank-photo"
              :src="dish.image"
              mode="aspectFill"
            />
            <text v-else class="empty-photo-text">暂无图片</text>
          </view>
          <view class="rank-body">
            <text class="rank-name" @tap="detail(dish)">{{ dish.name }}</text>
            <text class="rank-sold"
              >{{ dish.unit || "份" }} · 库存 {{ dish.stock }}</text
            >
            <view class="rank-bottom">
              <view class="rank-price-line">
                <text class="rank-symbol">¥</text>
                <text class="rank-price">{{
                  $fmtMoney(dish.price)
                }}</text>
              </view>
              <button
                class="add-button"
                :disabled="Number(dish.stock) <= 0"
                @tap="add(dish)"
              >
                <image
                  class="add-btn-icon"
                  src="/static/icons/lucide/plus-white.png"
                  mode="aspectFit"
                />
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getDishes } from "../../api/index.js";
export default {
  data: () => ({
    keyword: "",
    rows: [],
    history: [],
    loading: false,
    error: "",
    searched: false,
  }),
  onLoad() {
    try {
      const raw = uni.getStorageSync("search_history");
      const list = typeof raw === "string" ? JSON.parse(raw) : raw;
      this.history = Array.isArray(list)
        ? list.filter((x) => typeof x === "string").slice(0, 10)
        : [];
    } catch {}
    this.load();
  },
  methods: {
    back() {
      uni.navigateBack({
        fail: () => uni.switchTab({ url: "/pages/index/index" }),
      });
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const r = await getDishes({
          keyword: this.keyword.trim() || undefined,
        });
        this.rows = Array.isArray(r.data) ? r.data : [];
      } catch (e) {
        this.error = e.message || "搜索失败";
      } finally {
        this.loading = false;
      }
    },
    search() {
      const word = this.keyword.trim();
      this.searched = !!word;
      if (word) {
        this.history = [word, ...this.history.filter((w) => w !== word)].slice(
          0,
          10,
        );
        try {
          uni.setStorageSync("search_history", this.history);
        } catch {}
      }
      this.load();
    },
    selectWord(word) {
      this.keyword = word;
      this.search();
    },
    clearHistory() {
      uni.showModal({
        title: "清空搜索历史",
        content: "确定清空当前设备的搜索记录？",
        success: (r) => {
          if (r.confirm) {
            this.history = [];
            try {
              uni.removeStorageSync("search_history");
            } catch {}
          }
        },
      });
    },
    detail(d) {
      uni.navigateTo({
        url: "/pages/detail/detail?id=" + encodeURIComponent(d.id),
      });
    },
    add(d) {
      this.$store.commit("addToCart", d);
    },
  },
};
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #f6f8f5;
}

/* 顶部导航 */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  padding-top: calc(16rpx + constant(safe-area-inset-top));
  padding-top: calc(16rpx + env(safe-area-inset-top));
  background-color: #ffffff;
  border-bottom: 1rpx solid #eef2ef;
  box-sizing: border-box;
}

.nav-back {
  flex-shrink: 0;
  width: 48rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-back-icon {
  width: 36rpx;
  height: 36rpx;
}

.nav-search-box {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 10rpx;
  height: 68rpx;
  padding: 0 20rpx;
  background-color: #f6f8f5;
  border: 1rpx solid #e1e8e2;
  border-radius: 34rpx;
}

.nav-search-svg {
  width: 28rpx;
  height: 28rpx;
  flex-shrink: 0;
}

.nav-search-input {
  flex: 1;
  min-width: 0;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
  color: #19241d;
}

.nav-search-divider {
  flex-shrink: 0;
  width: 1rpx;
  height: 32rpx;
  background-color: #d8deda;
}

.nav-search-go {
  flex-shrink: 0;
  padding: 0 16rpx;
}

.nav-search-go-text {
  font-size: 26rpx;
  font-weight: 600;
  color: #27744d;
}

.page-body {
  padding-top: calc(112rpx + constant(safe-area-inset-top));
  padding-top: calc(112rpx + env(safe-area-inset-top));
  padding-bottom: 40rpx;
}

/* 历史卡片 */
.ss-block {
  background-color: #ffffff;
  border-radius: 18rpx;
  margin: 20rpx 24rpx 0;
  padding: 24rpx;
  border: 1rpx solid #e5ebe6;
  box-sizing: border-box;
}

.ss-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.ss-title-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.history-icon {
  width: 26rpx;
  height: 26rpx;
}

.ss-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #19241d;
}

.ss-clear-text {
  font-size: 24rpx;
  color: #778579;
}

.ss-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.ss-chip {
  background-color: #f6f8f5;
  border-radius: 28rpx;
  padding: 8rpx 22rpx;
  border: 1rpx solid #e1e8e2;
}

.ss-chip-text {
  font-size: 24rpx;
  color: #333f36;
}

/* 列表 item */
.rank-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid #f2f5f2;
}

.rank-item:first-child {
  border-top: none;
}

.rank-img {
  flex-shrink: 0;
  width: 140rpx;
  height: 140rpx;
  border-radius: 12rpx;
  background-color: #f1f4f1;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-photo {
  width: 100%;
  height: 100%;
}

.empty-photo-text {
  font-size: 20rpx;
  color: #778579;
}

.rank-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.rank-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #19241d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-sold {
  font-size: 22rpx;
  color: #778579;
}

.rank-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4rpx;
}

.rank-price-line {
  display: flex;
  align-items: baseline;
}

.rank-symbol {
  font-size: 20rpx;
  font-weight: 700;
  color: #ff5722;
}

.rank-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ff5722;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.add-button {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background-color: #27744d;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  box-shadow: 0 4rpx 10rpx rgba(39, 116, 77, 0.25);
  margin: 0;
}

.add-button::after {
  border: none;
}

.add-btn-icon {
  width: 24rpx;
  height: 24rpx;
}
</style>
