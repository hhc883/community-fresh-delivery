<template>
  <view class="floating-cart-container" :class="{ 'has-items': cartCount > 0 || showBubble }" @click="goCart">
    <!-- 加入成功气泡提示 -->
    <view class="cart-bubble" v-if="showBubble" @click.stop="goCart">
      <text class="bubble-text">已成功加入购物车</text>
      <text class="bubble-arrow">›</text>
    </view>

    <!-- +1 悬浮升空动画 -->
    <view class="plus-one" v-if="showPlusOne">
      <text class="plus-text">+{{ addQty }}</text>
    </view>

    <!-- 悬浮购物车圆形按钮 -->
    <view class="cart-circle-btn" :class="{ 'bounce-anim': isBouncing }">
      <text class="cart-icon">🛒</text>
      <!-- 数量角标 -->
      <view class="cart-badge" v-if="cartCount > 0">
        <text class="badge-num">{{ cartCount > 99 ? '99+' : cartCount }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'FloatingCart',
  data() {
    return {
      isBouncing: false,
      showPlusOne: false,
      showBubble: false,
      addQty: 1,
      bubbleTimer: null
    }
  },
  computed: {
    ...mapGetters(['cartCount'])
  },
  methods: {
    goCart() {
      uni.switchTab({
        url: '/pages/cart/cart'
      })
    },
    // 供父页面调用的加入动画触发方法
    animateAdd(qty = 1) {
      this.addQty = qty || 1
      this.isBouncing = true
      this.showPlusOne = true
      this.showBubble = true

      setTimeout(() => {
        this.isBouncing = false
      }, 400)

      setTimeout(() => {
        this.showPlusOne = false
      }, 1000)

      if (this.bubbleTimer) clearTimeout(this.bubbleTimer)
      this.bubbleTimer = setTimeout(() => {
        this.showBubble = false
      }, 2500)
    }
  }
}
</script>

<style lang="scss" scoped>
.floating-cart-container {
  position: fixed;
  right: 28rpx;
  bottom: 160rpx;
  z-index: 999;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;

  /* 提示气泡 */
  .cart-bubble {
    display: flex;
    align-items: center;
    background: rgba(31, 35, 41, 0.88);
    backdrop-filter: blur(8px);
    color: #FFFFFF;
    padding: 12rpx 20rpx;
    border-radius: 30rpx;
    margin-right: 14rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.15);
    animation: fadeInRight 0.3s ease;

    .bubble-text {
      font-size: 22rpx;
      font-weight: 500;
      color: #FFFFFF;
    }

    .bubble-arrow {
      font-size: 26rpx;
      margin-left: 6rpx;
      color: #25B873;
      font-weight: 700;
    }
  }

  /* +1 飞升动画 */
  .plus-one {
    position: absolute;
    top: -30rpx;
    right: 24rpx;
    pointer-events: none;
    animation: flyUpFade 0.9s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;

    .plus-text {
      font-size: 32rpx;
      font-weight: 800;
      color: #FA5151;
      text-shadow: 0 2rpx 8rpx rgba(250, 81, 81, 0.35);
    }
  }

  /* 圆形购物车悬浮球 */
  .cart-circle-btn {
    position: relative;
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #25B873 0%, #18804E 100%);
    box-shadow: 0 8rpx 24rpx rgba(37, 184, 115, 0.45);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);

    &:active {
      transform: scale(0.92);
    }

    &.bounce-anim {
      animation: cartBounce 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    }

    .cart-icon {
      font-size: 46rpx;
      line-height: 1;
      filter: drop-shadow(0 2rpx 4rpx rgba(0, 0, 0, 0.2));
    }

    /* 红色角标 */
    .cart-badge {
      position: absolute;
      top: -6rpx;
      right: -6rpx;
      min-width: 34rpx;
      height: 34rpx;
      line-height: 34rpx;
      padding: 0 8rpx;
      background-color: #FA5151;
      color: #FFFFFF;
      border-radius: 17rpx;
      border: 3rpx solid #FFFFFF;
      text-align: center;
      box-sizing: border-box;

      .badge-num {
        font-size: 20rpx;
        font-weight: 700;
      }
    }
  }
}

@keyframes cartBounce {
  0% { transform: scale(1); }
  50% { transform: scale(1.28); }
  100% { transform: scale(1); }
}

@keyframes flyUpFade {
  0% {
    transform: translateY(0) scale(0.8);
    opacity: 1;
  }
  50% {
    transform: translateY(-40rpx) scale(1.2);
    opacity: 1;
  }
  100% {
    transform: translateY(-80rpx) scale(1.3);
    opacity: 0;
  }
}

@keyframes fadeInRight {
  from {
    opacity: 0;
    transform: translateX(20rpx);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
