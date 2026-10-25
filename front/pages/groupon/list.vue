<template>
  <view class="core"
    ><view class="hero"
      ><text class="title">邻里一起拼</text
      ><text class="sub"
        >选商品开团或加入现有团。先模拟支付，成团后履约；未成团到期关闭并记录模拟退款。</text
      ></view
    ><button class="secondary" @tap="mine">我的拼团</button
    ><view v-if="loading" class="empty">读取拼团商品…</view
    ><view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    ><view v-else-if="!rows.length" class="empty">暂无可拼团商品</view
    ><view v-for="d in rows" :key="d.id" class="card" @tap="detail(d)"
      ><view class="row"
        ><image
          v-if="d.image"
          class="product-image"
          style="width: 180rpx; height: 180rpx"
          :src="d.image"
          mode="aspectFill"
        /><view class="stack"
          ><text class="product-name">{{ d.name }}</text
          ><text class="price">¥{{ $fmtMoney(d.grouponPrice) }}</text
          ><text class="sub"
            >{{ d.grouponSize }} 人成团 · 库存 {{ d.stock }}</text
          ></view
        ></view
      ><button class="secondary">查看正在拼的团 →</button></view
    ></view
  >
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
