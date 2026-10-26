<template>
  <view class="core"
    ><view class="hero"
      ><text class="title">选择自提门店</text
      ><text class="sub">选择下单时使用的自提点</text></view
    ><view v-if="loading" class="empty">正在加载门店…</view
    ><view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    ><view v-else-if="!stores.length" class="empty">暂无可用自提点</view
    ><view
      v-for="store in stores"
      :key="store.id"
      class="card"
      @tap="select(store)"
      ><view class="row"
        ><text class="product-name">{{ store.name }}</text
        ><text class="code">›</text></view
      ><text class="sub">{{ store.address || "地址暂未提供" }}</text
      ><text v-if="store.phone" class="sub">{{ store.phone }}</text></view
    ></view
  >
</template>
<script>
import { getStores } from "../../api/index.js";
export default {
  data: () => ({ stores: [], loading: true, error: "" }),
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const r = await getStores();
        this.stores = Array.isArray(r.data) ? r.data : [];
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
.card {
  border: 1px solid #edf4ef;
}
</style>
