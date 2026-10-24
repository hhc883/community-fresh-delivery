<template>
  <view class="cart-page">
    <!-- 顶部自提点提醒条 -->
    <view class="station-bar" @click="goPickup">
      <text class="station-icon">🏪</text>
      <text class="station-text">当前自提点：{{ pickupPoint.name || '请选择自提点' }}</text>
      <text class="station-arrow">›</text>
    </view>

    <!-- 购物车为空状态 -->
    <view class="empty-state" v-if="cart.length === 0">
      <view class="empty-icon-wrap">
        <text class="empty-emoji">🛒</text>
      </view>
      <text class="empty-title">购物车空空如也</text>
      <text class="empty-sub">快去挑选新鲜果蔬与心仪好物吧</text>
      <button class="btn-go-shop" @click="goShop">去逛逛</button>
    </view>

    <!-- 购物车列表 -->
    <view class="cart-container" v-else>
      <view class="cart-card">
        <!-- 头部店铺/全选行 -->
        <view class="card-header">
          <view class="header-left" @click="toggleAllChecked">
            <view class="check-box" :class="{ checked: isAllChecked }">
              <text v-if="isAllChecked" class="check-tick">✓</text>
            </view>
            <text class="shop-name">天天生鲜直采中心</text>
          </view>
          <text class="btn-clear" @click="confirmClearCart">清空</text>
        </view>

        <!-- 商品条目列表 -->
        <view class="item-list">
          <view class="cart-item" v-for="item in cart" :key="item.id">
            <!-- 单选框 -->
            <view class="check-box" :class="{ checked: item.checked }" @click="toggleChecked(item.id)">
              <text v-if="item.checked" class="check-tick">✓</text>
            </view>

            <!-- 商品图片 -->
            <image class="item-img" :src="item.image || '/static/images/png/dish-1.png'" mode="aspectFill" @click="goDetail(item.id)" />

            <!-- 商品详情与步进器 -->
            <view class="item-info">
              <text class="item-title" @click="goDetail(item.id)">{{ item.name }}</text>
              <view class="item-spec" v-if="item.unit || item.flavor">
                <text>{{ item.flavor || ('规格: ' + (item.unit || '份')) }}</text>
              </view>
              <view class="item-bottom">
                <view class="item-price-box">
                  <text class="price-symbol">¥</text>
                  <text class="price-val">{{ $fmtMoney(item.price) }}</text>
                </view>
                <!-- 步进器 -->
                <view class="stepper">
                  <view class="step-btn" @click="subQty(item)"><text>-</text></view>
                  <text class="step-num">{{ item.quantity }}</text>
                  <view class="step-btn" @click="addQty(item)"><text>+</text></view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 优惠与自提保障提示 -->
      <view class="service-tip">
        <text class="tip-dot">●</text>
        <text>当日23:00前下单，次日16:00后送达自提点凭码提货</text>
      </view>
    </view>

    <!-- 底部固定结算条 -->
    <view class="bottom-bar" v-if="cart.length > 0">
      <view class="bar-left" @click="toggleAllChecked">
        <view class="check-box" :class="{ checked: isAllChecked }">
          <text v-if="isAllChecked" class="check-tick">✓</text>
        </view>
        <text class="all-text">全选</text>
      </view>
      <view class="bar-center">
        <view class="total-row">
          <text class="total-label">合计:</text>
          <text class="total-symbol">¥</text>
          <text class="total-price">{{ $fmtMoney(checkedTotalPrice) }}</text>
        </view>
        <text class="save-tip" v-if="cartSaving > 0">已省 ¥{{ $fmtMoney(cartSaving) }}</text>
      </view>
      <button class="btn-checkout" :disabled="checkedCount === 0" @click="goCheckout">
        去结算({{ checkedCount }})
      </button>
    </view>
  </view>
</template>

<script>
import { mapState, mapGetters } from 'vuex'

export default {
  computed: {
    ...mapState(['cart', 'pickupPoint']),
    ...mapGetters(['cartSaving', 'checkedCount', 'checkedTotalPrice']),
    isAllChecked() {
      return this.cart.length > 0 && this.cart.every(i => i.checked)
    }
  },
  methods: {
    toggleChecked(id) {
      this.$store.commit('toggleChecked', id)
    },
    toggleAllChecked() {
      this.$store.commit('toggleAllChecked')
    },
    addQty(item) {
      this.$store.commit('updateQuantity', { dishId: item.id, quantity: item.quantity + 1 })
    },
    subQty(item) {
      if (item.quantity <= 1) {
        uni.showModal({
          title: '提示',
          content: '确定从购物车中删除该商品吗？',
          success: (res) => {
            if (res.confirm) {
              this.$store.commit('removeFromCart', item.id)
            }
          }
        })
      } else {
        this.$store.commit('updateQuantity', { dishId: item.id, quantity: item.quantity - 1 })
      }
    },
    confirmClearCart() {
      uni.showModal({
        title: '提示',
        content: '确定要清空购物车吗？',
        success: (res) => {
          if (res.confirm) {
            this.$store.commit('clearCart')
          }
        }
      })
    },
    goPickup() {
      uni.navigateTo({ url: '/pages/pickup/pickup' })
    },
    goShop() {
      uni.switchTab({ url: '/pages/index/index' })
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/detail/detail?id=${id}` })
    },
    goCheckout() {
      if (this.checkedCount === 0) {
        uni.showToast({ title: '请先勾选结算商品', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/order/confirm' })
    }
  }
}
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 140rpx;
  box-sizing: border-box;
}

.station-bar {
  display: flex;
  align-items: center;
  background-color: #E8F7F0;
  padding: 18rpx 24rpx;
  font-size: 24rpx;
  color: #1F2329;
}

.station-icon {
  margin-right: 12rpx;
  font-size: 28rpx;
}

.station-text {
  flex: 1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-arrow {
  color: #25B873;
  font-weight: 700;
  font-size: 28rpx;
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
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 24rpx;
}

.empty-emoji {
  font-size: 64rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1F2329;
  margin-bottom: 8rpx;
}

.empty-sub {
  font-size: 24rpx;
  color: #86909C;
  margin-bottom: 36rpx;
}

.btn-go-shop {
  background-color: #25B873;
  color: #FFFFFF;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 700;
  padding: 0 60rpx;
  height: 72rpx;
  line-height: 72rpx;
}

/* 列表容器 */
.cart-container {
  padding: 20rpx 24rpx;
}

.cart-card {
  background-color: #FFFFFF;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #F2F4F7;
}

.header-left {
  display: flex;
  align-items: center;
}

.shop-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #1F2329;
  margin-left: 16rpx;
}

.btn-clear {
  font-size: 24rpx;
  color: #86909C;
}

.item-list {
  display: flex;
  flex-direction: column;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #F7F8FA;
}

.cart-item:last-child {
  border-bottom: none;
}

.check-box {
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  border: 2rpx solid #C9CDD4;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.check-box.checked {
  background-color: #25B873;
  border-color: #25B873;
}

.check-tick {
  color: #FFFFFF;
  font-size: 22rpx;
  font-weight: 700;
}

.item-img {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
  margin-left: 16rpx;
  margin-right: 16rpx;
  background-color: #F7F8FA;
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
  color: #1F2329;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.item-spec {
  font-size: 22rpx;
  color: #86909C;
  background-color: #F7F8FA;
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
  color: #FF5E1E;
  font-weight: 700;
}

.price-val {
  font-size: 32rpx;
  color: #FF5E1E;
  font-weight: 700;
}

.stepper {
  display: flex;
  align-items: center;
  border: 1rpx solid #EBF0F2;
  border-radius: 8rpx;
  overflow: hidden;
}

.step-btn {
  width: 52rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #F7F8FA;
  font-size: 28rpx;
  color: #4E5969;
}

.step-num {
  min-width: 60rpx;
  text-align: center;
  font-size: 26rpx;
  color: #1F2329;
  font-weight: 600;
}

.service-tip {
  display: flex;
  align-items: center;
  padding: 20rpx 8rpx;
  font-size: 22rpx;
  color: #86909C;
}

.tip-dot {
  color: #25B873;
  font-size: 16rpx;
  margin-right: 8rpx;
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
  height: 104rpx;
  background-color: #FFFFFF;
  border-top: 1rpx solid #F2F4F7;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  z-index: 99;
  box-sizing: border-box;
}

.bar-left {
  display: flex;
  align-items: center;
}

.all-text {
  font-size: 26rpx;
  color: #4E5969;
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
  color: #1F2329;
  font-weight: 500;
}

.total-symbol {
  font-size: 24rpx;
  color: #FF5E1E;
  font-weight: 700;
  margin-left: 6rpx;
}

.total-price {
  font-size: 36rpx;
  color: #FF5E1E;
  font-weight: 700;
}

.save-tip {
  font-size: 20rpx;
  color: #FF5E1E;
}

.btn-checkout {
  background-color: #25B873;
  color: #FFFFFF;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 700;
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 44rpx;
}

.btn-checkout[disabled] {
  background-color: #C9CDD4;
  color: #FFFFFF;
}
</style>
