<template>
  <view class="pay-page">
    <!-- 顶部金额与倒计时提醒 -->
    <view class="amount-header">
      <text class="amount-tip">应付金额</text>
      <view class="price-box">
        <text class="price-val">¥ {{ amount }}</text>
      </view>
      <text class="countdown-tip">请在30分钟内完成支付</text>
    </view>

    <!-- 支付方式选项列表 -->
    <view class="method-card">
      <!-- 微信支付 -->
      <view class="method-row" @click="selectedMethod = 'wechat'">
        <view class="mr-left">
          <view class="method-icon-wrap wechat-wrap">
            <text class="icon-chat">💬</text>
          </view>
          <text class="method-name">微信支付</text>
        </view>
        <view class="radio-box" :class="{ checked: selectedMethod === 'wechat' }">
          <text v-if="selectedMethod === 'wechat'" class="tick">✓</text>
        </view>
      </view>

      <view class="divider"></view>

      <!-- 余额支付 -->
      <view class="method-row disabled" @click="onBalanceTap">
        <view class="mr-left">
          <view class="method-icon-wrap balance-wrap">
            <text class="icon-balance">¥</text>
          </view>
          <view class="name-box">
            <text class="method-name">余额支付</text>
            <text class="method-sub">可用余额 ¥ 0</text>
          </view>
        </view>
        <view class="radio-box disabled-radio"></view>
      </view>

      <view class="divider"></view>

      <!-- 货到付款 / 自提付款 -->
      <view class="method-row" @click="selectedMethod = 'offline'">
        <view class="mr-left">
          <view class="method-icon-wrap cod-wrap">
            <text class="icon-cart">🛒</text>
          </view>
          <text class="method-name">货到付款</text>
        </view>
        <view class="radio-box" :class="{ checked: selectedMethod === 'offline' }">
          <text v-if="selectedMethod === 'offline'" class="tick">✓</text>
        </view>
      </view>
    </view>

    <!-- 确认支付按钮 -->
    <view class="btn-box">
      <button class="btn-confirm-pay" :loading="paying" @click="handlePay">
        确认支付
      </button>
    </view>
  </view>
</template>

<script>
import { mockPay } from '@/api/index.js'

export default {
  data() {
    return {
      orderId: '',
      amount: 603,
      selectedMethod: 'wechat',
      paying: false
    }
  },
  onLoad(options) {
    if (options.orderId) this.orderId = options.orderId
    if (options.amount) this.amount = Number(options.amount) || 603
  },
  methods: {
    onBalanceTap() {
      uni.showToast({
        title: '可用余额不足',
        icon: 'none'
      })
    },
    async handlePay() {
      if (this.paying) return
      this.paying = true

      try {
        try {
          await mockPay(this.orderId)
        } catch (e) {
          // Fallback mock
        }

        uni.showToast({
          title: '支付成功',
          icon: 'success',
          duration: 1500
        })

        setTimeout(() => {
          uni.redirectTo({
            url: `/pages/order/detail?id=${this.orderId || '20210321152030018593'}`
          })
        }, 1200)
      } finally {
        this.paying = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.pay-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 40rpx 32rpx;
  box-sizing: border-box;
}

/* 顶部金额区 */
.amount-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0 60rpx;

  .amount-tip {
    font-size: 28rpx;
    color: #8F959E;
    margin-bottom: 16rpx;
  }

  .price-box {
    display: flex;
    align-items: center;

    .price-val {
      font-size: 64rpx;
      font-weight: 700;
      color: #25B873; /* 鲜明绿色大字价格 */
      line-height: 1.1;
    }
  }

  .countdown-tip {
    font-size: 24rpx;
    color: #FF7D00; /* 橙色提示倒计时 */
    margin-top: 16rpx;
  }
}

/* 支付方式卡片 */
.method-card {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 0 32rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);

  .divider {
    height: 1rpx;
    background-color: #F0F2F5;
  }

  .method-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 120rpx;

    &.disabled {
      opacity: 0.8;
    }

    .mr-left {
      display: flex;
      align-items: center;

      .method-icon-wrap {
        width: 60rpx;
        height: 60rpx;
        border-radius: 12rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 24rpx;

        &.wechat-wrap {
          background-color: #25B873;
          color: #FFFFFF;
          border-radius: 50%;
          font-size: 32rpx;
        }

        &.balance-wrap {
          background-color: #FF7D00;
          color: #FFFFFF;
          border-radius: 12rpx;
          font-size: 34rpx;
          font-weight: 700;
        }

        &.cod-wrap {
          background-color: #25B873;
          color: #FFFFFF;
          border-radius: 12rpx;
          font-size: 32rpx;
        }
      }

      .method-name {
        font-size: 30rpx;
        font-weight: 500;
        color: #1F2329;
      }

      .name-box {
        display: flex;
        flex-direction: column;

        .method-sub {
          font-size: 22rpx;
          color: #8F959E;
          margin-top: 4rpx;
        }
      }
    }

    .radio-box {
      width: 44rpx;
      height: 44rpx;
      border-radius: 50%;
      border: 2rpx solid #D8D8D8;
      display: flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;

      &.checked {
        background-color: #25B873;
        border-color: #25B873;

        .tick {
          color: #FFFFFF;
          font-size: 26rpx;
          font-weight: 700;
        }
      }

      &.disabled-radio {
        background-color: #EFEFEF;
        border-color: #EFEFEF;
      }
    }
  }
}

/* 确认支付按钮 */
.btn-box {
  margin-top: 60rpx;

  .btn-confirm-pay {
    width: 100%;
    height: 90rpx;
    line-height: 90rpx;
    background-color: #25B873;
    border-radius: 16rpx;
    color: #FFFFFF;
    font-size: 32rpx;
    font-weight: 600;
    border: none;

    &::after {
      border: none;
    }
  }
}
</style>
