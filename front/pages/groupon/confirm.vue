<template>
  <view class="core">
    <view class="confirm-header">
      <text class="title">{{
        mode ? (mode === "open" ? "发起社区拼团" : "参与社区拼团") : "确认订单"
      }}</text>
      <text class="sub">选择配送到家或社区自提</text>
    </view>

    <!-- 未登录提示 -->
    <view v-if="!loggedIn" class="card">
      <text class="login-tip-text">请先登录后结算拼团商品</text>
      <button class="primary" @tap="login">去登录</button>
    </view>

    <template v-else>
      <view v-if="error" class="error">{{ error }}</view>
      <view v-if="loading" class="empty">读取结算与门店信息…</view>
      <template v-else>
        <!-- 履约方式切换 -->
        <view class="card">
          <text class="product-name">配送履约方式</text>
          <view class="chips">
            <view
              :class="['chip', deliveryType === 'pickup' ? 'active' : '']"
              @tap="deliveryType = 'pickup'"
            >
              <image
                v-if="deliveryType === 'pickup'"
                class="chip-icon"
                src="/static/icons/lucide/store-white.png"
                mode="aspectFit"
              />
              <image
                v-else
                class="chip-icon"
                src="/static/icons/lucide/store.png"
                mode="aspectFit"
              />
              <text>社区自提</text>
            </view>
            <view
              :class="['chip', deliveryType === 'delivery' ? 'active' : '']"
              @tap="deliveryType = 'delivery'"
            >
              <image
                v-if="deliveryType === 'delivery'"
                class="chip-icon"
                src="/static/icons/lucide/truck-white.png"
                mode="aspectFit"
              />
              <image
                v-else
                class="chip-icon"
                src="/static/icons/lucide/truck.png"
                mode="aspectFit"
              />
              <text>送货到家</text>
            </view>
          </view>

          <!-- 门店自提表单 -->
          <view v-if="deliveryType === 'pickup'">
            <picker
              :range="stores"
              range-key="name"
              :value="storeIndex"
              @change="storeIndex = Number($event.detail.value)"
            >
              <view class="field picker-field">
                <text class="picker-val">{{
                  currentStore ? currentStore.name : "暂无可用自提点"
                }}</text>
                <image
                  class="picker-arrow"
                  src="/static/icons/lucide/chevron-right.png"
                  mode="aspectFit"
                />
              </view>
            </picker>
            <text v-if="currentStore" class="sub store-addr-sub"
              >提货地址：{{ currentStore.address }}</text
            >
            <input
              class="field"
              v-model="pickupPersonName"
              placeholder="提货人姓名"
            />
            <input
              class="field"
              v-model="pickupPersonPhone"
              type="number"
              maxlength="11"
              placeholder="提货人手机号"
            />
          </view>

          <!-- 送货到家地址 -->
          <view v-else @tap="address">
            <view class="field address-field">
              <text class="address-text">{{
                selectedAddress
                  ? selectedAddress.consignee + " " + selectedAddress.phone
                  : "选择收货地址"
              }}</text>
              <image
                class="picker-arrow"
                src="/static/icons/lucide/chevron-right.png"
                mode="aspectFit"
              />
            </view>
            <text class="sub" v-if="selectedAddress">{{
              formatAddress(selectedAddress)
            }}</text>
          </view>
        </view>

        <!-- 商品清单 -->
        <view class="card">
          <text class="product-name">拼团商品明细</text>
          <view v-for="i in items" :key="i.id" class="line row">
            <text class="item-name-text">{{ i.name }}</text>
            <text class="item-calc-text"
              >×{{ directDishId || mode ? quantity : i.quantity }} · 单价 ¥{{
                $fmtMoney(mode ? i.grouponPrice : i.price)
              }}</text
            >
          </view>
          <view v-if="directDishId || mode" class="row qty-setting-row">
            <text class="qty-label">购买数量</text>
            <input class="field qty-input" type="number" v-model="quantity" />
          </view>
          <view class="total-estimate-row">
            <text class="estimate-tip">预估合计金额：</text>
            <text class="price"
              ><text class="price-symbol">¥</text
              >{{ $fmtMoney(estimate) }}</text
            >
          </view>
          <text class="sub"
            >实际金额以提交后生成的订单为准。</text
          >
        </view>

        <!-- 备注 -->
        <view class="card">
          <input
            class="field"
            v-model="remark"
            maxlength="200"
            placeholder="订单备注（选填）"
          />
        </view>

        <!-- 提交按键 -->
        <button
          class="primary"
          :loading="submitting"
          :disabled="submitting || submitted || !items.length"
          @tap="submit"
        >
          提交{{ mode ? "拼团" : "" }}订单（¥{{ $fmtMoney(estimate) }}）
        </button>
        <text class="sub submit-hint"
          >提交后进入模拟支付流程，网络中断后可重试。</text
        >
      </template>
    </template>
  </view>
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

<style scoped>
.confirm-header {
  margin-bottom: 24rpx;
}

.login-tip-text {
  font-size: 28rpx;
  color: #19241d;
  display: block;
  text-align: center;
  margin: 20rpx 0;
}

.chip {
  display: inline-flex;
  align-items: center;
}

.chip-icon {
  width: 26rpx;
  height: 26rpx;
  margin-right: 8rpx;
}

.picker-field,
.address-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-val,
.address-text {
  flex: 1;
  color: #19241d;
}

.picker-arrow {
  width: 28rpx;
  height: 28rpx;
  margin-left: 12rpx;
  flex-shrink: 0;
}

.store-addr-sub {
  margin: 6rpx 0 16rpx 8rpx;
}

.item-name-text {
  font-weight: 600;
  color: #19241d;
}

.item-calc-text {
  color: #778579;
  font-size: 24rpx;
}

.qty-setting-row {
  margin: 16rpx 0;
}

.qty-label {
  font-size: 26rpx;
  color: #19241d;
}

.qty-input {
  width: 140rpx;
  text-align: center;
  margin: 0;
}

.total-estimate-row {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  margin-top: 18rpx;
  margin-bottom: 6rpx;
}

.estimate-tip {
  font-size: 26rpx;
  color: #19241d;
  font-weight: 500;
}

.submit-hint {
  text-align: center;
  margin-top: 12rpx;
}
</style>
