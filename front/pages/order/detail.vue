<template>
  <view class="core"
    ><text class="title">订单详情</text
    ><view v-if="loading" class="empty">读取订单…</view
    ><view v-if="error" class="error" @tap="load">{{ error }} · 点击重试</view
    ><template v-if="order"
      ><view class="hero"
        ><text class="title">{{ labels[order.status] || "未知状态" }}</text
        ><text class="sub"
          >{{ payLabels[order.payStatus] }} ·
          {{ isPickup ? "社区自提" : "配送到家" }}</text
        ><text class="sub" v-if="order.grouponId && String(order.grouponId) !== '0'"
          >团购订单需成团并付款后履约</text
        ></view
      ><view class="card stack"
        ><text>订单号：{{ order.number || order.orderNumber }}</text
        ><text>下单时间：{{ order.orderTime }}</text
        ><text class="price">实付 ¥{{ $fmtMoney(order.amount) }}</text
        ><text v-if="isPickup"
          >自提点：{{ order.pickupPointName || order.pickupPointAddress || ("自提点 #" + order.pickupPointId) }}</text
        ><text v-if="isPickup && order.pickupPointAddress && order.pickupPointName">{{
          order.pickupPointAddress
        }}</text
        ><text v-if="!isPickup && order.address">收货地址：{{ order.address }}</text
        ><text
          >联系人：{{ order.pickupPersonName || order.consignee || "-" }}
          {{ order.pickupPersonPhone || order.phone || "" }}</text
        ><text
          v-if="isPickup && order.pickupCode && Number(order.payStatus) === 1"
          class="code"
          >{{ order.pickupCode }}</text
        ><text v-if="Number(order.verifyStatus) === 1"
          >已核销 {{ order.verifyTime }}</text
        ><text v-if="order.cancelReason"
          >取消原因：{{ order.cancelReason }}</text
        ></view
      ><view class="card"
        ><view
          class="line row"
          v-for="d in order.orderDetailList || []"
          :key="d.id"
          ><text>{{ d.name }} ×{{ d.number }}</text
          ><text>¥{{ $fmtMoney(d.amount) }}</text></view
        ></view
      ><button v-if="Number(order.status) === 1" class="primary" @tap="pay">
        去模拟支付</button
      ><button
        v-if="[1, 2].includes(Number(order.status))"
        class="secondary"
        :loading="cancelling"
        :disabled="cancelling"
        @tap="cancel"
      >
        取消订单</button
      ><button class="secondary" @tap="load">刷新状态</button></template
    ></view
  >
</template>
<script>
import { getOrderDetail, cancelOrder } from "../../api/index.js";
export default {
  data: () => ({
    id: "",
    order: null,
    loading: true,
    error: "",
    cancelling: false,
    labels: {
      1: "待付款",
      2: "待接单 / 待履约",
      3: "已接单",
      4: "配送中",
      5: "已完成",
      6: "已取消",
    },
    payLabels: { 0: "未付款", 1: "已模拟付款", 2: "已模拟退款" },
  }),
  computed: {
    isPickup() {
      if (!this.order) return false;
      // deliveryType: 1 为配送到家，2 为社区自提。
      // 配送订单绝不显示提货码与自提点
      if (Number(this.order.deliveryType) === 1) return false;
      if (Number(this.order.deliveryType) === 2) return true;
      return Boolean(this.order.pickupPointId);
    },
  },
  onLoad(o) {
    this.id = o.id || "";
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        if (!this.id) throw new Error("缺少订单号");
        this.order = (await getOrderDetail(this.id)).data;
      } catch (e) {
        this.error = e.message;
        this.order = null;
      } finally {
        this.loading = false;
      }
    },
    pay() {
      uni.navigateTo({
        url: "/pages/order/pay?orderId=" + encodeURIComponent(this.id),
      });
    },
    cancel() {
      if (this.cancelling) return;
      uni.showModal({
        title: "取消订单",
        content: "确认取消此订单？已付款订单只记录模拟退款。",
        success: async (r) => {
          if (!r.confirm) return;
          this.cancelling = true;
          try {
            await cancelOrder(this.id);
            await this.load();
          } catch (e) {
            this.error = e.message || "取消失败";
          } finally {
            this.cancelling = false;
          }
        },
      });
    },
  },
};
</script>
