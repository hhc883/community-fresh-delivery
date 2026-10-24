<template>
  <view class="order-detail-page">
    <!-- 顶部订单状态栏 -->
    <view class="status-header">
      <text class="sh-status">待自提</text>
      <text class="sh-arrow">›</text>
    </view>

    <!-- 卡片 1: 提货码卡片 -->
    <view class="detail-card code-card">
      <view class="cc-left">
        <text class="cc-label">提货码：</text>
        <text class="cc-code">{{ order.pickupCode || '57865482' }}</text>
      </view>
      <button class="cc-copy-btn" @click="copyCode">复制</button>
    </view>

    <!-- 卡片 2: 自提网点与履约信息 -->
    <view class="detail-card station-card">
      <view class="sc-head">
        <text class="sc-title">自提网点信息</text>
        <view class="sc-nav" @click="openNavigation">
          <text class="nav-icon">📍</text>
          <text class="nav-text">导航</text>
        </view>
      </view>

      <view class="station-name-row" @click="goPickup">
        <text class="station-name">{{ order.pickupPointName || '高黎柱发士多店' }}</text>
        <text class="arrow">›</text>
      </view>

      <view class="sc-info-row">
        <text class="row-label">自提时间</text>
        <text class="row-val highlight-time">{{ order.pickupTime || '次日自提 16:00-18:00' }}</text>
      </view>

      <view class="sc-info-row align-start">
        <text class="row-label">自提网点</text>
        <view class="addr-box">
          <text class="addr-contact">联系人：柱哥 13800138000</text>
          <text class="addr-detail">详细地址：广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号</text>
        </view>
      </view>

      <view class="sc-info-row">
        <text class="row-label">提货人</text>
        <text class="row-val">李某 13800138000</text>
      </view>
    </view>

    <!-- 卡片 3: 商品清单与费用明细 -->
    <view class="detail-card goods-card">
      <view class="goods-row">
        <image class="goods-img" :src="goods.image || '/static/images/png/dish-2.png'" mode="aspectFill" />
        <view class="goods-info">
          <text class="goods-name">{{ goods.name }}</text>
          <text class="goods-spec">{{ goods.spec }}</text>
          <view class="goods-price-qty">
            <text class="goods-qty">{{ goods.quantity }} x </text>
            <text class="goods-price">¥ {{ goods.price }}</text>
          </view>
        </view>
      </view>

      <!-- 售后与再次购买操作按钮 -->
      <view class="goods-actions">
        <button class="ga-btn" @click="applyAfterSale">申请售后</button>
        <button class="ga-btn" @click="buyAgain">再次购买</button>
      </view>

      <view class="card-divider"></view>

      <!-- 费用明细明细项 -->
      <view class="fee-row">
        <text class="fee-label">商品金额</text>
        <text class="fee-val">¥ 699</text>
      </view>
      <view class="fee-row">
        <text class="fee-label">运费</text>
        <text class="fee-val">+¥ 0</text>
      </view>
      <view class="fee-row">
        <text class="fee-label">包装/其他</text>
        <text class="fee-val">+¥ 0</text>
      </view>
      <view class="fee-row">
        <text class="fee-label">限时直降</text>
        <text class="fee-val discount-val">-¥ 100</text>
      </view>
      <view class="fee-row total-pay-row">
        <text class="fee-label">实付金额</text>
        <text class="fee-val total-pay-val">¥ 599</text>
      </view>
    </view>

    <!-- 卡片 4: 订单基础信息 -->
    <view class="detail-card order-info-card">
      <view class="info-row">
        <text class="info-label">订单编号</text>
        <view class="info-val-copy">
          <text class="info-val">{{ order.id || '20210321152030018593' }}</text>
        </view>
      </view>
      <view class="info-row">
        <text class="info-label">创建时间</text>
        <text class="info-val">{{ order.createTime || '2021-03-21 15:20:30' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">支付方式</text>
        <text class="info-val">微信支付</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getOrderDetail } from '@/api/index.js'

export default {
  data() {
    return {
      orderId: '20210321152030018593',
      order: {
        id: '20210321152030018593',
        status: 'pending_pickup',
        pickupCode: '57865482',
        pickupPointName: '高黎柱发士多店',
        pickupTime: '次日自提 16:00-18:00',
        createTime: '2021-03-21 15:20:30'
      },
      goods: {
        id: 2,
        name: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量...',
        spec: '晴空翠 4+128G',
        price: '599.00',
        quantity: 1,
        image: '/static/images/png/dish-2.png'
      }
    }
  },
  onLoad(options) {
    if (options.id) {
      this.orderId = options.id
      this.fetchDetail(options.id)
    }
  },
  methods: {
    async fetchDetail(id) {
      try {
        const res = await getOrderDetail(id)
        if (res && res.data && res.data.data) {
          const d = res.data.data
          this.order.id = d.number || d.id || id
          this.order.createTime = d.orderTime || this.order.createTime
          this.order.pickupCode = d.pickupCode || '57865482'
          if (d.orderDetailList && d.orderDetailList.length > 0) {
            const first = d.orderDetailList[0]
            this.goods = {
              name: first.name,
              spec: first.dishFlavor || '标准规格',
              price: first.amount || 599,
              quantity: first.number || 1,
              image: first.image || '/static/images/png/dish-2.png'
            }
          }
        }
      } catch (e) {
        // Fallback to mock
      }
    },
    copyCode() {
      uni.setClipboardData({
        data: this.order.pickupCode || '57865482',
        success: () => {
          uni.showToast({ title: '提货码已复制', icon: 'success' })
        }
      })
    },
    openNavigation() {
      uni.openLocation({
        latitude: 22.7845,
        longitude: 113.2982,
        name: '高黎柱发士多店',
        address: '广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号'
      })
    },
    goPickup() {
      uni.navigateTo({ url: '/pages/pickup/pickup' })
    },
    applyAfterSale() {
      uni.showToast({ title: '售后通道已开启', icon: 'none' })
    },
    buyAgain() {
      uni.navigateTo({ url: `/pages/detail/detail?id=${this.goods.id || 2}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 24rpx;
  box-sizing: border-box;
}

/* 顶部状态栏 */
.status-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  padding-left: 8rpx;

  .sh-status {
    font-size: 36rpx;
    font-weight: 700;
    color: #1F2329;
  }

  .sh-arrow {
    font-size: 36rpx;
    color: #1F2329;
    margin-left: 10rpx;
  }
}

/* 通用卡片容器 */
.detail-card {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);
}

/* 卡片 1: 提货码卡片 */
.code-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .cc-left {
    display: flex;
    align-items: baseline;

    .cc-label {
      font-size: 28rpx;
      color: #1F2329;
      font-weight: 500;
    }

    .cc-code {
      font-size: 36rpx;
      font-weight: 700;
      color: #1F2329;
      letter-spacing: 2rpx;
    }
  }

  .cc-copy-btn {
    height: 56rpx;
    line-height: 54rpx;
    padding: 0 26rpx;
    border-radius: 28rpx;
    border: 1rpx solid #D8D8D8;
    background-color: #FFFFFF;
    color: #666666;
    font-size: 24rpx;
    margin: 0;

    &::after {
      border: none;
    }
  }
}

/* 卡片 2: 自提网点卡片 */
.station-card {
  .sc-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16rpx;

    .sc-title {
      font-size: 28rpx;
      font-weight: 700;
      color: #1F2329;
    }

    .sc-nav {
      display: flex;
      align-items: center;

      .nav-icon {
        font-size: 26rpx;
        margin-right: 6rpx;
      }

      .nav-text {
        font-size: 24rpx;
        color: #25B873;
        font-weight: 500;
      }
    }
  }

  .station-name-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 16rpx;
    border-bottom: 1rpx solid #F0F2F5;
    margin-bottom: 16rpx;

    .station-name {
      font-size: 28rpx;
      font-weight: 600;
      color: #1F2329;
    }

    .arrow {
      font-size: 32rpx;
      color: #C9CDD4;
    }
  }

  .sc-info-row {
    display: flex;
    margin-bottom: 14rpx;

    &.align-start {
      align-items: flex-start;
    }

    .row-label {
      width: 140rpx;
      font-size: 26rpx;
      color: #8F959E;
    }

    .row-val {
      flex: 1;
      font-size: 26rpx;
      color: #1F2329;

      &.highlight-time {
        color: #1F2329;
        font-weight: 500;
      }
    }

    .addr-box {
      flex: 1;
      display: flex;
      flex-direction: column;

      .addr-contact {
        font-size: 26rpx;
        color: #1F2329;
        margin-bottom: 6rpx;
      }

      .addr-detail {
        font-size: 24rpx;
        color: #8F959E;
        line-height: 36rpx;
      }
    }
  }
}

/* 卡片 3: 商品清单与费用 */
.goods-card {
  .goods-row {
    display: flex;
    padding-bottom: 20rpx;

    .goods-img {
      width: 140rpx;
      height: 140rpx;
      border-radius: 12rpx;
      background-color: #F7F8FA;
    }

    .goods-info {
      flex: 1;
      margin-left: 20rpx;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .goods-name {
        font-size: 26rpx;
        color: #1F2329;
        font-weight: 500;
        line-height: 38rpx;
      }

      .goods-spec {
        font-size: 22rpx;
        color: #8F959E;
      }

      .goods-price-qty {
        display: flex;
        align-items: baseline;

        .goods-qty {
          font-size: 24rpx;
          color: #8F959E;
          margin-right: 6rpx;
        }

        .goods-price {
          font-size: 28rpx;
          font-weight: 600;
          color: #1F2329;
        }
      }
    }
  }

  .goods-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 16rpx;
    margin-bottom: 20rpx;

    .ga-btn {
      height: 56rpx;
      line-height: 54rpx;
      padding: 0 24rpx;
      border-radius: 28rpx;
      border: 1rpx solid #D8D8D8;
      background-color: #FFFFFF;
      color: #666666;
      font-size: 24rpx;
      margin: 0;

      &::after {
        border: none;
      }
    }
  }

  .card-divider {
    height: 1rpx;
    background-color: #F0F2F5;
    margin-bottom: 20rpx;
  }

  .fee-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56rpx;

    .fee-label {
      font-size: 26rpx;
      color: #8F959E;
    }

    .fee-val {
      font-size: 26rpx;
      color: #1F2329;

      &.discount-val {
        color: #FF7D00;
      }
    }

    &.total-pay-row {
      margin-top: 10rpx;

      .fee-label {
        font-size: 28rpx;
        font-weight: 600;
        color: #1F2329;
      }

      .total-pay-val {
        font-size: 32rpx;
        font-weight: 700;
        color: #FF7D00;
      }
    }
  }
}

/* 卡片 4: 订单编号 */
.order-info-card {
  .info-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 60rpx;

    .info-label {
      font-size: 26rpx;
      color: #8F959E;
    }

    .info-val {
      font-size: 26rpx;
      color: #1F2329;
    }
  }
}
</style>
