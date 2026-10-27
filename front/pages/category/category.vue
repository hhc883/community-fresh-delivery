<template>
  <view class="cat-page">
    <view class="top-search-wrap">
      <view class="search-pill" @click="goSearch">
        <image
          class="cat-search-icon"
          src="/static/icons/lucide/search-muted.png"
          mode="aspectFit"
        />
        <text class="search-text">搜索生鲜品类/基地蔬菜/时令水果...</text>
      </view>
    </view>

    <!-- 顶部分类快捷切换栏 -->
    <view class="top-icon-strip">
      <view
        class="tis-item"
        v-for="t in topShortcuts"
        :key="t.name"
        @click="selectTopShortcut(t)"
      >
        <view class="tis-circle" :style="{ backgroundColor: t.bg }">
          <image
            v-if="t.key === 'order'"
            class="tis-svg"
            src="/static/icons/lucide/package.png"
            mode="aspectFit"
          />
          <image
            v-else-if="t.key === 'pickup'"
            class="tis-svg"
            src="/static/icons/lucide/store.png"
            mode="aspectFit"
          />
          <image
            v-else-if="t.key === 'group'"
            class="tis-svg"
            src="/static/icons/lucide/users.png"
            mode="aspectFit"
          />
          <image
            v-else-if="t.key === 'fresh'"
            class="tis-svg"
            src="/static/icons/lucide/shopping-bag.png"
            mode="aspectFit"
          />
          <image
            v-else
            class="tis-svg"
            src="/static/icons/lucide/store.png"
            mode="aspectFit"
          />
        </view>
        <text class="tis-name" :class="{ active: currentTopKey === t.key }">{{
          t.name
        }}</text>
      </view>
      <view class="tis-expand" @click="toggleExpand">
        <text class="tis-expand-text">全部</text>
        <image
          class="tis-expand-arrow"
          src="/static/icons/lucide/chevron-right-muted.png"
          mode="aspectFit"
        />
      </view>
    </view>

    <view class="cat-main">
      <!-- 左侧分类导航 -->
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

      <!-- 右侧商品明细列表 -->
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
          <view v-if="loading" class="empty">商品加载中…</view>
          <view v-else-if="error" class="error" @tap="load"
            >{{ error }} · 点击重试</view
          >
          <view v-else-if="!currentCategoryGoods.length" class="empty"
            >该分类暂无商品</view
          >
          <view v-else class="goods-list">
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
                    <image
                      class="cart-add-icon"
                      src="/static/icons/lucide/plus-white.png"
                      mode="aspectFit"
                    />
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
      { name: "全部商品", key: "all", emoji: "🍎", bg: "#EAF5EE" },
      { name: "生鲜选购", key: "fresh", emoji: "🥬", bg: "#EAF5EE" },
      { name: "我的订单", key: "order", emoji: "📦", bg: "#EAF5EE" },
      { name: "自提门店", key: "pickup", emoji: "🏪", bg: "#EAF5EE" },
      { name: "社区拼团", key: "group", emoji: "📢", bg: "#FFF2EC" },
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

<style scoped>
.cat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #ffffff;
}

.top-search-wrap {
  padding: 16rpx 24rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #eef2ef;
}

.search-pill {
  display: flex;
  align-items: center;
  height: 68rpx;
  background-color: #f6f8f5;
  border-radius: 34rpx;
  padding: 0 24rpx;
  border: 1rpx solid #e1e8e2;
}

.cat-search-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 14rpx;
  flex-shrink: 0;
}

.search-text {
  font-size: 26rpx;
  color: #778579;
}

/* 顶部圆形快捷入口栏 */
.top-icon-strip {
  display: flex;
  align-items: center;
  padding: 12rpx 20rpx 16rpx;
  border-bottom: 1rpx solid #edf1ee;
  background-color: #ffffff;
}

.tis-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.tis-circle {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6rpx;
}

.tis-svg {
  width: 36rpx;
  height: 36rpx;
}

.tis-name {
  font-size: 22rpx;
  color: #556257;
}

.tis-name.active {
  color: #27744d;
  font-weight: 700;
}

.tis-expand {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10rpx 0 16rpx;
  border-left: 1rpx solid #eef2ef;
}

.tis-expand-text {
  font-size: 22rpx;
  color: #778579;
}

.tis-expand-arrow {
  width: 24rpx;
  height: 24rpx;
  margin-left: 4rpx;
}

/* 主体分栏 */
.cat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧栏 */
.cat-sidebar {
  width: 170rpx;
  height: 100%;
  background-color: #f6f8f5;
  border-right: 1rpx solid #eef2ef;
}

.sidebar-item {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-size: 26rpx;
  color: #556257;
}

.sidebar-item.active {
  background-color: #ffffff;
  color: #27744d;
  font-weight: 700;
}

.active-indicator {
  position: absolute;
  left: 0;
  top: 24rpx;
  bottom: 24rpx;
  width: 6rpx;
  background-color: #27744d;
  border-radius: 0 4rpx 4rpx 0;
}

.sidebar-text {
  padding: 0 12rpx;
  text-align: center;
}

/* 右侧内容 */
.cat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #ffffff;
}

.sort-header {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 72rpx;
  border-bottom: 1rpx solid #eef2ef;
  font-size: 24rpx;
  color: #778579;
}

.sh-tab.active {
  color: #27744d;
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
  border-bottom: 1rpx solid #f2f5f2;
}

.goods-thumb {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
  background-color: #f1f4f1;
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
  color: #19241d;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.goods-spec {
  font-size: 22rpx;
  color: #778579;
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
  color: #ff5722;
  font-weight: 700;
}

.price-num {
  font-size: 32rpx;
  color: #ff5722;
  font-weight: 700;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.cart-btn {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background-color: #27744d;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-add-icon {
  width: 24rpx;
  height: 24rpx;
}
</style>
