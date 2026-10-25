<template>
  <view class="core"
    ><text class="title">模拟收银台</text
    ><view v-if="loading" class="empty">读取订单…</view
    ><view v-if="error" class="error">{{ error }}</view
    ><view v-if="order" class="card stack"
      ><text>订单 {{ order.number || order.orderNumber || order.id }}</text
      ><text class="price">¥{{ $fmtMoney(order.amount) }}</text
      ><text class="sub">本系统仅模拟支付与退款记账，不发起真实扣款。</text
      ><button
        v-if="Number(order.status) === 1 && Number(order.payStatus) === 0"
        class="primary"
        :disabled="paying"
        :loading="paying"
        @tap="pay"
      >
        确认模拟支付</button
      ><text v-else>订单已处理，请查看订单详情。</text
      ><button class="secondary" @tap="detail">查看订单详情</button></view
    ></view
  >
</template>
<script>
import { getOrderDetail, mockPay } from "../../api/index.js";
export default {
  data: () => ({
    id: "",
    order: null,
    error: "",
    loading: true,
    paying: false,
  }),
  onLoad(o) {
    this.id = o.orderId || "";
    this.load();
  },
  methods: {
    async load() {
      try {
        if (!this.id) throw new Error("缺少订单号");
        this.order = (await getOrderDetail(this.id)).data;
      } catch (e) {
        this.error = e.message;
      } finally {
        this.loading = false;
      }
    },
    async pay() {
      if (this.paying) return;
      this.paying = true;
      this.error = "";
      try {
        await mockPay(this.id);
        uni.showToast({ title: "模拟支付成功", icon: "success" });
        uni.redirectTo({
          url: "/pages/order/detail?id=" + encodeURIComponent(this.id),
        });
      } catch (e) {
        this.error = e.message || "模拟支付失败，请重试";
      } finally {
        this.paying = false;
      }
    },
    detail() {
      uni.redirectTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(this.id),
      });
    },
  },
};
</script>
