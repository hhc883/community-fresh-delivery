<template>
  <view class="core">
    <view class="hero"
      ><text class="title">社区鲜选</text
      ><text class="sub">选购生鲜 · 社区拼团 · 配送到家 / 到店自提</text></view
    >
    <view class="row"
      ><input
        class="field"
        v-model="keyword"
        placeholder="搜索商品"
        confirm-type="search"
        @confirm="load"
      /><button class="secondary" @tap="load">搜索</button></view
    >
    <view class="chips"
      ><text
        :class="['chip', !categoryId ? 'active' : '']"
        @tap="selectCategory('')"
        >全部</text
      ><text
        v-for="c in categories"
        :key="c.id"
        :class="['chip', String(categoryId) === String(c.id) ? 'active' : '']"
        @tap="selectCategory(c.id)"
        >{{ c.name }}</text
      ></view
    >
    <button class="secondary" @tap="goGroups">社区拼团大厅 →</button>
    <text class="sub">商品展示价仅供参考，成交价以服务器生成的订单为准。</text>
    <view v-if="loading" class="empty">正在读取商品…</view
    ><view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    >
    <view v-else-if="!dishes.length" class="empty">暂无符合条件的商品</view>
    <view v-else class="grid"
      ><view class="card" v-for="dish in dishes" :key="dish.id"
        ><view @tap="detail(dish)"
          ><image
            v-if="dish.image"
            class="product-image"
            :src="dish.image"
            mode="aspectFill"
          /><view v-else class="product-image empty">暂无图片</view
          ><text class="product-name">{{ dish.name }}</text
          ><text class="sub"
            >{{ dish.unit || "份" }} · 库存
            {{ dish.stock == null ? "待确认" : dish.stock }}</text
          ><text class="price">¥{{ $fmtMoney(dish.price) }}</text></view
        ><button
          class="primary"
          :disabled="Number(dish.stock) <= 0"
          @tap="add(dish)"
        >
          加入购物车
        </button></view
      ></view
    >
  </view>
</template>
<script>
import { getDishes, getCategories } from "../api/index.js";
export default {
  data: () => ({
    keyword: "",
    categoryId: "",
    categories: [],
    dishes: [],
    loading: false,
    error: "",
  }),
  mounted() {
    this.load();
    getCategories()
      .then((r) => (this.categories = r.data || []))
      .catch((e) => (this.error = e.message));
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const r = await getDishes({
          keyword: this.keyword,
          categoryId: this.categoryId || undefined,
        });
        this.dishes = Array.isArray(r.data) ? r.data : [];
      } catch (e) {
        this.error = e.message || "读取失败";
      } finally {
        this.loading = false;
      }
    },
    selectCategory(id) {
      this.categoryId = id;
      this.load();
    },
    detail(d) {
      uni.navigateTo({
        url: "/pages/detail/detail?id=" + encodeURIComponent(d.id),
      });
    },
    add(d) {
      this.$store.commit("addToCart", d);
    },
    goGroups() {
      uni.navigateTo({ url: "/pages/groupon/list" });
    },
  },
};
</script>
