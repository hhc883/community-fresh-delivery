<template>
  <view class="core"
    ><text class="title">我的订单</text
    ><text class="sub">交易记录及金额均来自服务器。退款为模拟记账。</text
    ><view v-if="!loggedIn" class="card"
      ><text>请先登录后查看订单与消费记录</text
      ><button class="primary" @tap="login">去登录</button></view
    ><template v-else
      ><view class="chips"
        ><text
          v-for="s in filters"
          :key="s.value"
          :class="['chip', status === s.value ? 'active' : '']"
          @tap="filter(s.value)"
          >{{ s.label }}</text
        ></view
      ><view v-if="loading" class="empty">读取订单…</view
      ><view v-if="error" class="error" @tap="load">{{ error }} · 点击重试</view
      ><view v-if="!loading && !error && !rows.length" class="empty"
        >暂无订单记录</view
      ><view class="card" v-for="o in rows" :key="o.id" @tap="detail(o)"
        ><view class="row"
          ><text>{{ o.number || o.orderNumber }}</text
          ><text>{{ labels[o.status] || "未知状态" }}</text></view
        ><view class="line" v-for="d in o.orderDetailList || []" :key="d.id"
          ><text>{{ d.name }} ×{{ d.number }}</text></view
        ><text class="sub"
          >{{ o.orderTime }} · {{ Number(o.deliveryType) === 2 || (Number(o.deliveryType) !== 1 && o.pickupPointId) ? "自提" : "配送" }}</text
        ><view class="total"
          >¥{{ $fmtMoney(o.amount) }} · {{ payLabels[o.payStatus] }}</view
        ><button class="secondary">查看详情 →</button></view
      ><view v-if="!loading && !error && rows.length" class="row"
        ><button class="secondary" :disabled="page === 1" @tap="turn(-1)">
          上一页</button
        ><text>{{ page }} / {{ Math.max(1, Math.ceil(total / 10)) }}</text
        ><button class="secondary" :disabled="page * 10 >= total" @tap="turn(1)">
          下一页
        </button></view
      ></template
    ></view
  >
</template>
<script>
import { getOrders } from "../../api/index.js";
export default {
  data: () => ({
    rows: [],
    page: 1,
    total: 0,
    status: "",
    loading: false,
    error: "",
    labels: {
      1: "待付款",
      2: "待接单",
      3: "已接单",
      4: "配送中",
      5: "已完成",
      6: "已取消",
    },
    payLabels: { 0: "未付款", 1: "已付款", 2: "模拟退款" },
    filters: [
      { value: "", label: "全部" },
      { value: 1, label: "待付款" },
      { value: 2, label: "待履约" },
      { value: 5, label: "已完成" },
      { value: 6, label: "已取消" },
    ],
  }),
  computed: {
    loggedIn() {
      return Boolean(this.$store.state.user?.token);
    },
  },
  onLoad(options) {
    const status = Number(options.status);
    if ([1, 2, 5, 6].includes(status)) this.status = status;
  },
  onShow() {
    if (this.loggedIn) {
      this.load();
    }
  },
  methods: {
    login() {
      uni.navigateTo({ url: "/pages/login/login" });
    },
    async load() {
      if (!this.loggedIn) return;
      this.loading = true;
      this.error = "";
      try {
        const d = (await getOrders(this.page, 10, this.status)).data;
        this.rows = d?.records || [];
        this.total = Number(d?.total) || 0;
      } catch (e) {
        this.error = e.message || "订单加载失败";
        this.rows = [];
      } finally {
        this.loading = false;
      }
    },
    filter(s) {
      this.status = s;
      this.page = 1;
      this.load();
    },
    turn(n) {
      this.page += n;
      this.load();
    },
    detail(o) {
      uni.navigateTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(o.id),
      });
    },
  },
};
</script>
