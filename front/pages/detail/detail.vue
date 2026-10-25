<template>
  <view class="core"
    ><view v-if="loading" class="empty">商品加载中…</view
    ><view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    ><view v-else-if="dish" class="card stack"
      ><image
        v-if="dish.image"
        class="product-image"
        :src="dish.image"
        mode="aspectFill"
      /><text class="title">{{ dish.name }}</text
      ><text class="price">¥{{ $fmtMoney(dish.price) }}</text
      ><text class="sub"
        >规格：{{ dish.unit || "份" }} · 库存：{{ dish.stock }}</text
      ><text class="sub" v-if="dish.origin">产地：{{ dish.origin }}</text
      ><text>{{ dish.description || "暂无商品介绍" }}</text
      ><button class="primary" :disabled="Number(dish.stock) <= 0" @tap="add">
        加入购物车</button
      ><button class="secondary" :disabled="Number(dish.stock) <= 0" @tap="buy">
        立即购买</button
      ><button
        v-if="Number(dish.isGroupon) === 1"
        class="secondary"
        @tap="group"
      >
        查看拼团
      </button></view
    ><view v-else class="empty">商品不存在或已下架</view
  ></view>
</template>
<script>
import { getDishDetail } from "../../api/index.js";
export default {
  data: () => ({ id: "", dish: null, loading: true, error: "" }),
  onLoad(o) {
    this.id = o.id || "";
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        if (!this.id) throw new Error("缺少商品标识");
        this.dish = (await getDishDetail(this.id)).data;
      } catch (e) {
        this.error = e.message || "商品加载失败";
        this.dish = null;
      } finally {
        this.loading = false;
      }
    },
    add() {
      if (!this.dish) return;
      this.$store.commit("addToCart", this.dish);
    },
    buy() {
      if (!this.dish) return;
      uni.navigateTo({
        url: "/pages/order/confirm?dishId=" + encodeURIComponent(this.id),
      });
    },
    group() {
      if (!this.dish) return;
      uni.navigateTo({
        url: "/pages/groupon/detail?dishId=" + encodeURIComponent(this.id),
      });
    },
  },
};
</script>
