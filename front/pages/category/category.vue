<template>
  <view class="cat-page">
    <view class="top-search-wrap">
      <view class="search-pill" @click="goSearch">
        <text class="search-icon">🔍</text>
        <text class="search-text">搜索商品/自提点/店铺...</text>
      </view>
    </view>

    <view class="top-icon-strip">
      <view
        class="tis-item"
        v-for="t in topShortcuts"
        :key="t.name"
        @click="selectTopShortcut(t)"
      >
        <view class="tis-circle" :style="{ backgroundColor: t.bg }">
          <text class="tis-emoji">{{ t.emoji }}</text>
        </view>
        <text class="tis-name" :class="{ active: currentTopKey === t.key }">{{
          t.name
        }}</text>
      </view>
      <view class="tis-expand" @click="toggleExpand">
        <text class="tis-expand-text">分类</text>
        <text class="tis-expand-icon">∨</text>
      </view>
    </view>

    <view class="cat-main">
      <scroll-view class="cat-sidebar" scroll-y>
        <view
          class="sidebar-item"
          v-for="(cat, idx) in categories"
          :key="cat.id"
          :class="{ active: activeCatIndex === idx }"
          @click="activeCatIndex = idx"
        >
          <view class="active-indicator" v-if="activeCatIndex === idx"></view>
          <text class="sidebar-text">{{ cat.name }}</text>
        </view>
      </scroll-view>

      <view class="cat-content">
        <view class="sort-header">
          <text
            class="sh-tab"
            :class="{ active: sortBy === 'default' }"
            @click="setSort('default')"
            >综合</text
          >
          <text
            class="sh-tab"
            :class="{ active: sortBy === 'stock' }"
            @click="setSort('stock')"
            >库存</text
          >
          <text
            class="sh-tab"
            :class="{ active: sortBy === 'name' }"
            @click="setSort('name')"
            >名称</text
          >
          <text
            class="sh-tab"
            :class="{ active: sortBy === 'price' }"
            @click="togglePriceSort"
            >价格 {{ priceAsc ? "↑" : "↓" }}</text
          >
        </view>

        <scroll-view class="goods-scroll" scroll-y>
          <view v-if="loading" class="empty">商品加载中…</view
          ><view v-else-if="error" class="error" @tap="load"
            >{{ error }} · 点击重试</view
          ><view v-else-if="!currentCategoryGoods.length" class="empty"
            >该分类暂无商品</view
          ><view v-else class="goods-list">
            <view
              class="goods-row"
              v-for="item in currentCategoryGoods"
              :key="item.id"
              @click="goDetail(item)"
            >
              <image class="goods-thumb" :src="item.image" mode="aspectFill" />
              <view class="goods-meta">
                <text class="goods-title">{{ item.name }}</text>
                <text class="goods-spec"
                  >{{ item.unit || "份" }} · 库存 {{ item.stock }}</text
                >
                <view class="goods-price-row">
                  <view class="price-val-box">
                    <text class="price-symbol">¥</text>
                    <text class="price-num">{{ $fmtMoney(item.price) }}</text>
                  </view>

                  <view class="cart-btn" @click.stop="addToCart(item)">
                    <text class="cart-icon">🛒</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { getCategories, getDishes } from "../../api/index.js";
export default {
  data: () => ({
    activeCatIndex: 0,
    currentTopKey: "all",
    sortBy: "default",
    priceAsc: true,
    categories: [],
    allGoods: [],
    loading: true,
    error: "",
    topShortcuts: [
      { name: "全部商品", key: "all", emoji: "🍎", bg: "#FFECE8" },
      { name: "生鲜选购", key: "fresh", emoji: "🥬", bg: "#E8F3FF" },
      { name: "我的订单", key: "order", emoji: "📦", bg: "#FFEBF3" },
      { name: "自提门店", key: "pickup", emoji: "🏪", bg: "#E8F7F0" },
      { name: "社区拼团", key: "group", emoji: "📢", bg: "#FFF6E8" },
    ],
  }),
  computed: {
    currentCategoryGoods() {
      const id = this.categories[this.activeCatIndex]?.id;
      let list = id
        ? this.allGoods.filter((x) => String(x.categoryId) === String(id))
        : [...this.allGoods];
      if (this.sortBy === "price")
        list.sort(
          (a, b) =>
            (Number(a.price) - Number(b.price)) * (this.priceAsc ? 1 : -1),
        );
      if (this.sortBy === "stock")
        list.sort((a, b) => Number(b.stock) - Number(a.stock));
      if (this.sortBy === "name")
        list.sort((a, b) => a.name.localeCompare(b.name, "zh"));
      return list;
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
        const [c, d] = await Promise.all([getCategories(), getDishes({})]);
        this.categories = [{ id: "", name: "全部" }, ...(c.data || [])];
        this.allGoods = Array.isArray(d.data) ? d.data : [];
        if (this.activeCatIndex >= this.categories.length)
          this.activeCatIndex = 0;
      } catch (e) {
        this.error = e.message || "加载失败";
      } finally {
        this.loading = false;
      }
    },
    goSearch() {
      uni.navigateTo({ url: "/pages/search/search" });
    },
    selectTopShortcut(t) {
      if (t.key === "group") uni.navigateTo({ url: "/pages/groupon/list" });
      else if (t.key === "order") uni.navigateTo({ url: "/pages/order/order" });
      else if (t.key === "pickup")
        uni.navigateTo({ url: "/pages/pickup/pickup" });
      else {
        this.currentTopKey = t.key;
        this.activeCatIndex = 0;
      }
    },
    toggleExpand() {
      this.activeCatIndex = 0;
    },
    setSort(s) {
      this.sortBy = s;
    },
    togglePriceSort() {
      this.priceAsc = this.sortBy === "price" ? !this.priceAsc : true;
      this.sortBy = "price";
    },
    goDetail(item) {
      uni.navigateTo({
        url: "/pages/detail/detail?id=" + encodeURIComponent(item.id),
      });
    },
    addToCart(item) {
      if (Number(item.stock) <= 0) {
        uni.showToast({ title: "商品暂时缺货", icon: "none" });
        return;
      }
      this.$store.commit("addToCart", item);
    },
  },
};
</script>
<style lang="scss" scoped>
.cat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #ffffff;
}

.top-search-wrap {
  padding: 16rpx 24rpx;
  background-color: #ffffff;
}

.search-pill {
  display: flex;
  align-items: center;
  height: 68rpx;
  background-color: #f7f8fa;
  border-radius: 34rpx;
  padding: 0 24rpx;
}

.search-icon {
  font-size: 26rpx;
  color: #86909c;
  margin-right: 12rpx;
}

.search-text {
  font-size: 26rpx;
  color: #86909c;
}

/* 顶部圆形快捷入口栏 */
.top-icon-strip {
  display: flex;
  align-items: center;
  padding: 12rpx 20rpx 16rpx;
  border-bottom: 1rpx solid #f2f4f7;
}

.tis-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.tis-circle {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6rpx;
}

.tis-emoji {
  font-size: 36rpx;
}

.tis-name {
  font-size: 20rpx;
  color: #4e5969;
}

.tis-name.active {
  color: #25b873;
  font-weight: 700;
}

.tis-expand {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 10rpx;
  border-left: 1rpx solid #f2f4f7;
}

.tis-expand-text {
  font-size: 20rpx;
  color: #86909c;
}

.tis-expand-icon {
  font-size: 20rpx;
  color: #86909c;
}

/* 主体 */
.cat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧栏 */
.cat-sidebar {
  width: 170rpx;
  height: 100%;
  background-color: #f7f8fa;
}

.sidebar-item {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-size: 26rpx;
  color: #4e5969;
}

.sidebar-item.active {
  background-color: #ffffff;
  color: #25b873;
  font-weight: 700;
}

.active-indicator {
  position: absolute;
  left: 0;
  top: 26rpx;
  bottom: 26rpx;
  width: 6rpx;
  background-color: #25b873;
  border-radius: 0 4rpx 4rpx 0;
}

/* 右侧内容 */
.cat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sort-header {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 72rpx;
  border-bottom: 1rpx solid #f2f4f7;
  font-size: 24rpx;
  color: #86909c;
}

.sh-tab.active {
  color: #25b873;
  font-weight: 700;
}

.goods-scroll {
  flex: 1;
  height: 0;
}

.goods-list {
  padding: 16rpx 20rpx 40rpx;
}

.goods-row {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f7f8fa;
}

.goods-thumb {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
  background-color: #f7f8fa;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.goods-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1f2329;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.goods-spec {
  font-size: 20rpx;
  color: #86909c;
  margin-top: 4rpx;
}

.goods-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}

.price-val-box {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 22rpx;
  color: #25b873;
  font-weight: 700;
}

.price-num {
  font-size: 32rpx;
  color: #25b873;
  font-weight: 800;
  font-family: Arial, Helvetica, sans-serif;
}

.cart-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-icon {
  font-size: 36rpx;
  color: #25b873;
}
</style>
