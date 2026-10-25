<template>
  <view class="core"
    ><text class="title">我的拼团</text
    ><view v-if="!loggedIn" class="card"
      ><text>请先登录后查看我的拼团记录</text
      ><button class="primary" @tap="login">去登录</button></view
    ><template v-else
      ><view class="chips"
        ><text
          v-for="(label, value) in filters"
          :key="value"
          :class="['chip', String(status) === value ? 'active' : '']"
          @tap="filter(value)"
          >{{ label }}</text
        ></view
      ><view v-if="loading" class="empty">读取我的拼团…</view
      ><view v-else-if="error" class="error" @tap="load"
        >{{ error }} · 点击重试</view
      ><view v-else-if="!rows.length" class="empty">暂无拼团记录</view
      ><view v-for="g in rows" :key="g.id" class="card stack"
        ><view class="row"
          ><text class="product-name">{{ g.dishName }}</text
          ><text>{{ labels[g.status] }}</text></view
        ><text
          >{{ g.currentSize }} / {{ g.requiredSize }} 人 · 还差
          {{ g.lackSize }} 人</text
        ><text class="sub">截止：{{ g.expireTime }}（北京时间）</text
        ><text class="price">拼团单价 ¥{{ $fmtMoney(g.grouponPrice) }}</text
        ><button class="secondary" @tap="detail(g)">查看拼团详情</button
        ><button v-if="g.orderId" class="primary" @tap="order(g)">
          查看我的订单 / 支付
        </button></view
      ><text class="sub"
        >未成团到期关闭，已付款部分记录模拟退款，具体金额请查看订单。</text
      ></template
    ></view
  >
</template>
<script>
import { getMyGroupons } from "../../api/index.js";
export default {
  data: () => ({
    status: "",
    rows: [],
    loading: true,
    error: "",
    filters: { "": "全部", 1: "进行中", 2: "已成团", 3: "已失败", 4: "已过期" },
    labels: { 1: "进行中", 2: "已成团", 3: "已失败", 4: "已过期" },
  }),
  computed: {
    loggedIn() {
      return Boolean(this.$store.state.user?.token);
    },
  },
  onShow() {
    if (this.loggedIn) {
      this.load();
    } else {
      this.loading = false;
    }
  },
  methods: {
    login() {
      uni.navigateTo({ url: "/pages/login/login" });
    },
    filter(s) {
      this.status = s;
      this.load();
    },
    async load() {
      if (!this.loggedIn) return;
      this.loading = true;
      this.error = "";
      try {
        this.rows = (await getMyGroupons(this.status || undefined)).data || [];
      } catch (e) {
        this.error = e.message || "拼团加载失败";
        this.rows = [];
      } finally {
        this.loading = false;
      }
    },
    detail(g) {
      uni.navigateTo({
        url: "/pages/groupon/detail?id=" + encodeURIComponent(g.id),
      });
    },
    order(g) {
      uni.navigateTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(g.orderId),
      });
    },
  },
};
</script>
