<template>
  <view class="cart-page">
    <view class="station-bar" @click="goPickup">
      <image
        class="station-icon-img"
        src="/static/icons/lucide/store.png"
        mode="aspectFit"
      />
      <text class="station-text"
        >当前自提点：{{ pickupPoint.name || "请选择就近自提点" }}</text
      >
      <image
        class="station-arrow-img"
        src="/static/icons/lucide/chevron-right.png"
        mode="aspectFit"
      />
    </view>

    <!-- 购物车空态 -->
    <view class="empty-state" v-if="cart.length === 0">
      <view class="empty-icon-wrap">
        <image
          class="empty-cart-img"
          src="/static/icons/lucide/shopping-cart-muted.png"
          mode="aspectFit"
        />
      </view>
      <text class="empty-title">购物车空空如也</text>
      <text class="empty-sub">晨采新鲜果蔬与心仪好物正等您挑选</text>
      <button class="btn-go-shop" @click="goShop">选购今日生鲜</button>
    </view>

    <!-- 购物车商品列表 -->
    <view class="cart-container" v-else>
      <view class="cart-card">
        <view class="card-header">
          <view class="header-left" @click="toggleAllChecked">
            <view class="check-box" :class="{ checked: isAllChecked }">
              <image
                v-if="isAllChecked"
                class="check-tick-img"
                src="/static/icons/lucide/check-white.png"
                mode="aspectFit"
              />
            </view>
            <text class="shop-name">社区生鲜自提商城</text>
          </view>
          <text class="btn-clear" @click="confirmClearCart">清空所选</text>
        </view>

        <view class="item-list">
          <view class="cart-item" v-for="item in cart" :key="item.id">
            <view
              class="check-box"
              :class="{ checked: item.checked !== false }"
              @click="toggleChecked(item.id)"
            >
              <image
                v-if="item.checked !== false"
                class="check-tick-img"
                src="/static/icons/lucide/check-white.png"
                mode="aspectFit"
              />
            </view>

            <image
              class="item-img"
              :src="item.image"
              mode="aspectFill"
              @click="goDetail(item.id)"
            />

            <view class="item-info">
              <text class="item-title" @click="goDetail(item.id)">{{
                item.name
              }}</text>
              <view class="item-spec" v-if="item.unit || item.flavor">
                <text>{{ item.flavor || "规格: " + (item.unit || "份") }}</text>
              </view>
              <view class="item-bottom">
                <view class="item-price-box">
                  <text class="price-symbol">¥</text>
                  <text class="price-val">{{ $fmtMoney(item.price) }}</text>
                </view>

                <view class="stepper">
                  <view class="step-btn" @click="subQty(item)">
                    <image
                      class="step-icon"
                      src="/static/icons/lucide/minus.png"
                      mode="aspectFit"
                    />
                  </view>
                  <text class="step-num">{{ item.quantity }}</text>
                  <view class="step-btn" @click="addQty(item)">
                    <image
                      class="step-icon"
                      src="/static/icons/lucide/plus.png"
                      mode="aspectFit"
                    />
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="service-tip">
        <image
          class="tip-icon"
          src="/static/icons/lucide/clock.png"
          mode="aspectFit"
        />
        <text>金额以提交订单时服务端真实计价为准，请以订单详情为准。</text>
      </view>
    </view>

    <!-- 底部固定结算条 -->
    <view class="bottom-bar" v-if="cart.length > 0">
      <view class="bar-left" @click="toggleAllChecked">
        <view class="check-box" :class="{ checked: isAllChecked }">
          <image
            v-if="isAllChecked"
            class="check-tick-img"
            src="/static/icons/lucide/check-white.png"
            mode="aspectFit"
          />
        </view>
        <text class="all-text">全选</text>
      </view>
      <view class="bar-center">
        <view class="total-row">
          <text class="total-label">合计:</text>
          <text class="total-symbol">¥</text>
          <text class="total-price">{{ $fmtMoney(checkedTotalPrice) }}</text>
        </view>
        <text class="save-tip" v-if="cartSaving > 0"
          >已省 ¥{{ $fmtMoney(cartSaving) }}</text
        >
      </view>
      <button
        class="btn-checkout"
        :disabled="checkedCount === 0"
        @click="goCheckout"
      >
        去结算({{ checkedCount }})
      </button>
    </view>
  </view>
</template>

<script>
export default {
  computed: {
    cart() {
      return this.$store.state.cart;
    },
    pickupPoint() {
      return this.$store.state.pickupPoint;
    },
    selected() {
      return this.cart.filter((i) => i.checked !== false);
    },
    isAllChecked() {
      return this.cart.length > 0 && this.selected.length === this.cart.length;
    },
    checkedCount() {
      return this.selected.reduce((n, i) => n + i.quantity, 0);
    },
    checkedTotalPrice() {
      return this.selected.reduce(
        (n, i) => n + i.quantity * Number(i.price),
        0,
      );
    },
    cartSaving() {
      return 0;
    },
  },
  methods: {
    goPickup() {
      uni.navigateTo({ url: "/pages/pickup/pickup" });
    },
    goShop() {
      uni.switchTab({ url: "/pages/index/index" });
    },
    goDetail(id) {
      uni.navigateTo({
        url: "/pages/detail/detail?id=" + encodeURIComponent(id),
      });
    },
    toggleChecked(id) {
      this.$store.commit("toggleChecked", id);
    },
    toggleAllChecked() {
      const checked = this.isAllChecked;
      for (const i of this.cart) {
        if ((i.checked !== false) === checked) this.toggleChecked(i.id);
      }
    },
    confirmClearCart() {
      uni.showModal({
        title: "清空购物车",
        content: "确定移除全部商品？",
        success: (r) => {
          if (r.confirm)
            for (const i of [...this.cart])
              this.$store.commit("removeFromCart", i.id);
        },
      });
    },
    subQty(i) {
      if (i.quantity === 1) {
        uni.showModal({
          title: "移除商品",
          content: "是否将此商品从购物车移除？",
          success: (r) => {
            if (r.confirm) this.$store.commit("removeFromCart", i.id);
          },
        });
      } else
        this.$store.commit("updateQuantity", {
          dishId: i.id,
          quantity: i.quantity - 1,
        });
    },
    addQty(i) {
      if (i.stock != null && i.quantity >= Number(i.stock)) {
        uni.showToast({ title: "已达到当前库存", icon: "none" });
        return;
      }
      this.$store.commit("updateQuantity", {
        dishId: i.id,
        quantity: i.quantity + 1,
      });
    },
    goCheckout() {
      if (this.selected.length) uni.navigateTo({ url: "/pages/order/confirm" });
    },
  },
};
</script>

<style scoped>
.cart-page {
  min-height: 100vh;
  background-color: #f6f8f5;
  padding-bottom: calc(160rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.station-bar {
  display: flex;
  align-items: center;
  background-color: #eaf5ee;
  padding: 18rpx 24rpx;
  font-size: 24rpx;
  color: #19241d;
  border-bottom: 1rpx solid #d4ebdc;
}

.station-icon-img {
  width: 28rpx;
  height: 28rpx;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.station-text {
  flex: 1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-arrow-img {
  width: 24rpx;
  height: 24rpx;
  margin-left: 8rpx;
  flex-shrink: 0;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 180rpx;
}

.empty-icon-wrap {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
  border: 1rpx solid #e1e8e2;
}

.empty-cart-img {
  width: 68rpx;
  height: 68rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #19241d;
  margin-bottom: 8rpx;
}

.empty-sub {
  font-size: 24rpx;
  color: #778579;
  margin-bottom: 36rpx;
}

.btn-go-shop {
  background-color: #27744d;
  color: #ffffff;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 600;
  padding: 0 60rpx;
  height: 76rpx;
  line-height: 76rpx;
  border: none;
  box-shadow: 0 4rpx 14rpx rgba(39, 116, 77, 0.22);
}

.btn-go-shop::after {
  border: none;
}

/* 列表容器 */
.cart-container {
  padding: 20rpx 24rpx;
}

.cart-card {
  background-color: #ffffff;
  border-radius: 18rpx;
  padding: 24rpx;
  border: 1rpx solid #e5ebe6;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #eef2ef;
}

.header-left {
  display: flex;
  align-items: center;
}

.shop-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #19241d;
  margin-left: 16rpx;
}

.btn-clear {
  font-size: 24rpx;
  color: #778579;
}

.item-list {
  display: flex;
  flex-direction: column;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f2f5f2;
}

.cart-item:last-child {
  border-bottom: none;
}

.check-box {
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  border: 2rpx solid #c0ccc2;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  flex-shrink: 0;
}

.check-box.checked {
  background-color: #27744d;
  border-color: #27744d;
}

.check-tick-img {
  width: 22rpx;
  height: 22rpx;
}

.item-img {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
  margin-left: 16rpx;
  margin-right: 16rpx;
  background-color: #f1f4f1;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 150rpx;
}

.item-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #19241d;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.item-spec {
  font-size: 22rpx;
  color: #778579;
  background-color: #f6f8f5;
  align-self: flex-start;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.item-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-price-box {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 22rpx;
  color: #ff5722;
  font-weight: 700;
}

.price-val {
  font-size: 32rpx;
  color: #ff5722;
  font-weight: 700;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.stepper {
  display: flex;
  align-items: center;
  border: 1rpx solid #e1e8e2;
  border-radius: 8rpx;
  overflow: hidden;
}

.step-btn {
  width: 48rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f6f8f5;
}

.step-icon {
  width: 22rpx;
  height: 22rpx;
}

.step-num {
  min-width: 56rpx;
  text-align: center;
  font-size: 26rpx;
  color: #19241d;
  font-weight: 600;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.service-tip {
  display: flex;
  align-items: center;
  padding: 20rpx 8rpx;
  font-size: 22rpx;
  color: #778579;
}

.tip-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 10rpx;
  flex-shrink: 0;
}

/* 底部结算栏 */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  /* #ifdef H5 */
  bottom: 50px;
  /* #endif */
  height: calc(104rpx + constant(safe-area-inset-bottom));
  height: calc(104rpx + env(safe-area-inset-bottom));
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  background-color: #ffffff;
  border-top: 1rpx solid #e5ebe6;
  display: flex;
  align-items: center;
  padding-left: 24rpx;
  padding-right: 24rpx;
  z-index: 99;
  box-sizing: border-box;
}

.bar-left {
  display: flex;
  align-items: center;
}

.all-text {
  font-size: 26rpx;
  color: #4b5a4f;
  margin-left: 12rpx;
}

.bar-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-right: 20rpx;
}

.total-row {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 24rpx;
  color: #19241d;
  font-weight: 500;
}

.total-symbol {
  font-size: 24rpx;
  color: #ff5722;
  font-weight: 700;
  margin-left: 6rpx;
}

.total-price {
  font-size: 36rpx;
  color: #ff5722;
  font-weight: 700;
  font-family: "DIN Alternate", -apple-system, Arial, sans-serif;
}

.save-tip {
  font-size: 20rpx;
  color: #ff5722;
}

.btn-checkout {
  background-color: #27744d;
  color: #ffffff;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 600;
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 44rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(39, 116, 77, 0.25);
}

.btn-checkout::after {
  border: none;
}

.btn-checkout[disabled] {
  background-color: #bce1ca !important;
  color: #ffffff !important;
  box-shadow: none !important;
  opacity: 0.7;
}
</style>
