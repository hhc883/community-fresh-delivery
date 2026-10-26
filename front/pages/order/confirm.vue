<template>
  <view class="core"
    ><text class="title">{{
      mode ? (mode === "open" ? "发起拼团" : "参与拼团") : "确认订单"
    }}</text
    ><view v-if="!loggedIn" class="card"
      ><text>请先登录后结算</text
      ><button class="primary" @tap="login">去登录</button></view
    ><template v-else
      ><view v-if="error" class="error">{{ error }}</view
      ><view v-if="loading" class="empty">读取结算信息…</view
      ><template v-else
        ><view class="card"
          ><view v-for="i in items" :key="i.id" class="line row"
            ><text>{{ i.name }}</text
            ><text
              >×{{ directDishId || mode ? quantity : i.quantity }} · 单价 ¥{{
                $fmtMoney(mode ? i.grouponPrice : i.price)
              }}</text
            ></view
          ><view v-if="directDishId || mode" class="row"
            ><text>购买数量</text
            ><input class="field" type="number" v-model="quantity" /></view
          ><text class="sub"
            >预估 ¥{{
              $fmtMoney(estimate)
            }}，服务器按当前商品价格生成最终订单。</text
          ></view
        ><view class="card"
          ><text class="product-name">履约方式</text
          ><view class="chips"
            ><text
              :class="['chip', deliveryType === 'pickup' ? 'active' : '']"
              @tap="deliveryType = 'pickup'"
              >社区自提</text
            ><text
              :class="['chip', deliveryType === 'delivery' ? 'active' : '']"
              @tap="deliveryType = 'delivery'"
              >配送到家</text
            ></view
          ><view v-if="deliveryType === 'pickup'"
            ><picker
              :range="stores"
              range-key="name"
              :value="storeIndex"
              @change="storeIndex = Number($event.detail.value)"
              ><view class="field">{{
                currentStore ? currentStore.name : "暂无可用自提点"
              }}</view></picker
            ><text v-if="currentStore" class="sub">{{
              currentStore.address
            }}</text
            ><input
              class="field"
              v-model="pickupPersonName"
              placeholder="提货人姓名" /><input
              class="field"
              v-model="pickupPersonPhone"
              type="number"
              maxlength="11"
              placeholder="提货人手机号" /></view
          ><view v-else @tap="address"
            ><view class="field">{{
              selectedAddress
                ? selectedAddress.consignee + " " + selectedAddress.phone
                : "选择收货地址 →"
            }}</view
            ><text class="sub" v-if="selectedAddress">{{
              formatAddress(selectedAddress)
            }}</text></view
          ></view
        ><view class="card"
          ><input
            class="field"
            v-model="remark"
            maxlength="200"
            placeholder="备注（选填）" /></view
        ><button
          class="primary"
          :loading="submitting"
          :disabled="submitting || submitted || !items.length"
          @tap="submit"
        >
          提交{{ mode ? "拼团" : "" }}订单</button
        ><text class="sub"
          >提交后进入模拟支付；网络中断可重试，同一购买意图复用请求标识。</text
        ></template
      ></template
    ></view
  >
</template>
<script>
import { getDishDetail, getStores, getAddressList } from "../../api/index.js";
import { checkout } from "../../services/checkout.mjs";
export default {
  data: () => ({
    mode: "",
    grouponId: "",
    directDishId: "",
    quantity: 1,
    items: [],
    stores: [],
    storeIndex: 0,
    deliveryType: "pickup",
    selectedAddress: null,
    pickupPersonName: "",
    pickupPersonPhone: "",
    remark: "",
    loading: true,
    submitting: false,
    submitted: false,
    error: "",
  }),
  computed: {
    loggedIn() {
      return !!this.$store.state.user.token;
    },
    currentStore() {
      return this.stores[this.storeIndex] || null;
    },
    estimate() {
      return this.items.reduce(
        (n, i) =>
          n +
          Number(this.mode ? i.grouponPrice : i.price) *
            Number(this.directDishId || this.mode ? this.quantity : i.quantity),
        0,
      );
    },
  },
  onLoad(o) {
    this.mode = o.mode || "";
    this.grouponId = o.grouponId || "";
    this.directDishId = o.dishId || "";
    this.pickupPersonPhone = this.$store.state.user.phone || "";
    this.addressHandler = (a) => (this.selectedAddress = a);
    uni.$on("selectAddress", this.addressHandler);
  },
  onShow() {
    if (this.loggedIn && this.loading) this.load();
  },
  onUnload() {
    uni.$off("selectAddress", this.addressHandler);
  },
  methods: {
    formatAddress(a) {
      return [a.provinceName, a.cityName, a.districtName, a.detail]
        .filter(Boolean)
        .join("");
    },
    login() {
      uni.navigateTo({ url: "/pages/login/login" });
    },
    address() {
      uni.navigateTo({ url: "/pages/address/list?from=order" });
    },
    async load() {
      this.error = "";
      try {
        if (this.directDishId) {
          const d = (await getDishDetail(this.directDishId)).data;
          this.items = d ? [{ ...d, quantity: 1 }] : [];
        } else
          this.items = this.$store.state.cart
            .filter((i) => i.checked !== false)
            .map((i) => ({ ...i }));
        const [s, a] = await Promise.allSettled([
          getStores(),
          getAddressList(),
        ]);
        if (s.status === "fulfilled") {
          const data = s.value.data;
          this.stores = (
            Array.isArray(data) ? data : data?.records || []
          ).filter((p) => p.status === "open" || Number(p.status) === 1);
          const selectedStoreId = this.$store.state.pickupPoint?.id;
          const selectedIndex = this.stores.findIndex(p => String(p.id) === String(selectedStoreId));
          if (selectedIndex >= 0) this.storeIndex = selectedIndex;
        } else this.error = s.reason.message;
        if (a.status === "fulfilled") {
          const list = a.value.data || [];
          if (!this.selectedAddress)
            this.selectedAddress =
              list.find((x) => Number(x.isDefault) === 1) || list[0] || null;
        } else this.error = a.reason.message;
      } catch (e) {
        this.error = e.message || "结算信息读取失败";
      } finally {
        this.loading = false;
      }
    },
    async submit() {
      if (this.submitting || this.submitted) return;
      this.error = "";
      const q = Number(this.quantity);
      if ((this.directDishId || this.mode) && (!Number.isInteger(q) || q < 1)) {
        this.error = "请输入正整数数量";
        return;
      }
      if (
        this.deliveryType === "pickup" &&
        (!this.currentStore ||
          !this.pickupPersonName.trim() ||
          !/^1[3-9]\d{9}$/.test(this.pickupPersonPhone))
      ) {
        this.error = "请选择可用自提点并填写提货人姓名、正确手机号";
        return;
      }
      if (this.deliveryType === "delivery" && !this.selectedAddress) {
        this.error = "请选择收货地址";
        return;
      }
      this.submitting = true;
      try {
        const items = this.items.map((i) => ({
          ...i,
          quantity: this.directDishId || this.mode ? q : i.quantity,
        }));
        const order = await checkout({
          items,
          mode: this.mode || undefined,
          dishId: this.directDishId,
          grouponId: this.grouponId,
          quantity: q,
          deliveryType: this.deliveryType,
          addressBookId: this.selectedAddress?.id,
          pickupPointId: this.currentStore?.id,
          pickupPersonName: this.pickupPersonName,
          pickupPersonPhone: this.pickupPersonPhone,
          remark: this.remark,
        });
        if (!this.directDishId && !this.mode)
          this.$store.commit(
            "clearPurchased",
            items.map((i) => String(i.id)),
          );
        this.submitted = true;
        uni.redirectTo({
          url: "/pages/order/pay?orderId=" + encodeURIComponent(order.id),
        });
      } catch (e) {
        this.error = e.message || "订单提交失败";
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>
