<template>
  <view class="order-page">
    <!-- 顶部四状态选项卡 (全部 / 待付款 / 待自提 / 已完成) -->
    <view class="tab-header">
      <view
        class="tab-item"
        v-for="t in tabs"
        :key="t.key"
        :class="{ active: currentTab === t.key }"
        @click="switchTab(t.key)"
      >
        <text class="tab-text">{{ t.label }}</text>
        <view class="tab-indicator" v-if="currentTab === t.key"></view>
      </view>
    </view>

    <!-- 订单列表展示 -->
    <view class="order-list">
      <view
        class="order-card"
        v-for="order in filteredOrders"
        :key="order.id"
        @click="goDetail(order)"
      >
        <!-- 订单卡片顶部：创建时间与状态 -->
        <view class="card-top">
          <text class="order-time">{{ order.createTime }}</text>
          <text class="order-status" :class="order.status">{{ getStatusText(order.status) }}</text>
        </view>

        <!-- 商品明细 -->
        <view class="goods-row" v-for="(item, idx) in order.items" :key="idx">
          <image class="goods-img" :src="item.image || '/static/images/png/dish-2.png'" mode="aspectFill" />
          <view class="goods-info">
            <text class="goods-name">{{ item.name }}</text>
            <text class="goods-spec">{{ item.spec }}</text>
          </view>
          <view class="goods-price-qty">
            <text class="goods-price">¥ {{ item.price }}</text>
            <text class="goods-qty">x {{ item.quantity }}</text>
          </view>
        </view>

        <!-- 费用小计行 -->
        <view class="summary-row">
          <text class="sum-text">共 {{ getTotalQty(order) }} 件商品</text>
          <text class="sum-text">运费: ¥ {{ order.freight || 0 }}</text>
          <view class="sum-pay">
            <text class="pay-label">实付:</text>
            <text class="pay-price">¥ {{ order.payAmount }}</text>
          </view>
        </view>

        <!-- 底部操作按钮 -->
        <view class="actions-row" @click.stop>
          <!-- 待付款 -->
          <template v-if="order.status === 'pending_pay'">
            <button class="btn-outline" @click="onCancelOrder(order)">取消订单</button>
            <button class="btn-green" @click="onPayOrder(order)">立即付款</button>
          </template>

          <!-- 待自提 -->
          <template v-else-if="order.status === 'pending_pickup'">
            <button class="btn-outline" @click="onRefund(order)">申请退款</button>
            <button class="btn-green" @click="goDetail(order)">查看提货码</button>
          </template>

          <!-- 已完成 -->
          <template v-else>
            <button class="btn-outline" @click="goReview(order)">去评价</button>
          </template>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getOrders, cancelOrder } from '@/api/index.js'

export default {
  data() {
    return {
      currentTab: 'all',
      tabs: [
        { key: 'all', label: '全部' },
        { key: 'pending_pay', label: '待付款' },
        { key: 'pending_pickup', label: '待自提' },
        { key: 'completed', label: '已完成' }
      ],
      mockOrders: [
        {
          id: '202103231527070192',
          createTime: '2021-03-23 15:27:07',
          status: 'pending_pay',
          freight: 0,
          payAmount: 603,
          items: [
            {
              name: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量...',
              spec: '晴空翠 4+128G',
              price: 599.00,
              quantity: 1,
              image: '/static/images/png/dish-2.png'
            }
          ]
        },
        {
          id: '202103211632000881',
          createTime: '2021-03-21 16:32:00',
          status: 'completed',
          freight: 0,
          payAmount: 599,
          items: [
            {
              name: 'Redmi 9A 4GB+64GB 碧空蓝 5000mAh大电量...',
              spec: '碧空蓝 4+64G',
              price: 499.00,
              quantity: 1,
              image: '/static/images/png/dish-1.png'
            }
          ]
        },
        {
          id: '20210321152030018593',
          createTime: '2021-03-21 15:20:30',
          status: 'pending_pickup',
          freight: 0,
          payAmount: 599,
          items: [
            {
              name: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量...',
              spec: '晴空翠 4+128G',
              price: 599.00,
              quantity: 1,
              image: '/static/images/png/dish-2.png'
            }
          ]
        }
      ],
      orders: []
    }
  },
  computed: {
    filteredOrders() {
      const list = this.orders.length > 0 ? this.orders : this.mockOrders
      if (this.currentTab === 'all') return list
      return list.filter(o => o.status === this.currentTab)
    }
  },
  onLoad(options) {
    if (options.tab) {
      this.currentTab = options.tab
    }
    this.fetchOrderList()
  },
  methods: {
    async fetchOrderList() {
      try {
        const res = await getOrders()
        if (res && res.data && res.data.data && res.data.data.records) {
          this.orders = res.data.data.records.map(o => ({
            id: String(o.id || o.number),
            createTime: o.orderTime || '2021-03-23 15:27:07',
            status: o.status === 1 ? 'pending_pay' : (o.status === 2 || o.status === 3 ? 'pending_pickup' : 'completed'),
            freight: 0,
            payAmount: o.amount || 603,
            items: (o.orderDetailList || []).map(d => ({
              name: d.name || '生鲜优选商品',
              spec: d.dishFlavor || '标准规格',
              price: d.amount || 599,
              quantity: d.number || 1,
              image: d.image || '/static/images/png/dish-2.png'
            }))
          }))
        }
      } catch (e) {
        // Fallback to mockOrders
      }
    },
    switchTab(key) {
      this.currentTab = key
    },
    getStatusText(status) {
      if (status === 'pending_pay') return '待付款'
      if (status === 'pending_pickup') return '待自提'
      if (status === 'completed') return '已完成'
      return '已完成'
    },
    getTotalQty(order) {
      return (order.items || []).reduce((acc, cur) => acc + (cur.quantity || 1), 0)
    },
    goDetail(order) {
      uni.navigateTo({
        url: `/pages/order/detail?id=${order.id}`
      })
    },
    onPayOrder(order) {
      uni.navigateTo({
        url: `/pages/order/pay?orderId=${order.id}&amount=${order.payAmount}`
      })
    },
    async onCancelOrder(order) {
      uni.showModal({
        title: '提示',
        content: '确定要取消该订单吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelOrder(order.id)
            } catch (e) {}
            order.status = 'cancelled'
            uni.showToast({ title: '订单已取消', icon: 'success' })
          }
        }
      })
    },
    onRefund(order) {
      uni.showToast({ title: '售后申请已提交', icon: 'success' })
    },
    goReview(order) {
      uni.navigateTo({
        url: `/pages/review/review?orderId=${order.id}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.order-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 40rpx;
}

/* 顶部选项卡 */
.tab-header {
  display: flex;
  background-color: #FFFFFF;
  height: 88rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 1rpx 6rpx rgba(0, 0, 0, 0.02);

  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;

    .tab-text {
      font-size: 28rpx;
      color: #666666;
    }

    &.active {
      .tab-text {
        color: #25B873;
        font-weight: 700;
      }

      .tab-indicator {
        position: absolute;
        bottom: 6rpx;
        width: 48rpx;
        height: 6rpx;
        background-color: #25B873;
        border-radius: 3rpx;
      }
    }
  }
}

/* 订单列表卡片 */
.order-list {
  padding: 20rpx 24rpx;

  .order-card {
    background-color: #FFFFFF;
    border-radius: 16rpx;
    padding: 24rpx 28rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.02);

    .card-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-bottom: 20rpx;
      border-bottom: 1rpx solid #F0F2F5;

      .order-time {
        font-size: 24rpx;
        color: #8F959E;
      }

      .order-status {
        font-size: 26rpx;
        font-weight: 600;

        &.pending_pay,
        &.pending_pickup {
          color: #FA5151; /* 鲜红/橙红 */
        }

        &.completed {
          color: #8F959E;
        }
      }
    }

    .goods-row {
      display: flex;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #F8F9FA;

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

        .goods-name {
          font-size: 26rpx;
          color: #1F2329;
          font-weight: 500;
          line-height: 38rpx;
          display: -webkit-box;
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 2;
          overflow: hidden;
        }

        .goods-spec {
          font-size: 22rpx;
          color: #8F959E;
          margin-top: 8rpx;
        }
      }

      .goods-price-qty {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        margin-left: 16rpx;

        .goods-price {
          font-size: 28rpx;
          font-weight: 600;
          color: #1F2329;
        }

        .goods-qty {
          font-size: 24rpx;
          color: #8F959E;
          margin-top: 8rpx;
        }
      }
    }

    .summary-row {
      display: flex;
      align-items: baseline;
      justify-content: flex-end;
      padding: 18rpx 0;
      gap: 16rpx;

      .sum-text {
        font-size: 24rpx;
        color: #8F959E;
      }

      .sum-pay {
        display: flex;
        align-items: baseline;

        .pay-label {
          font-size: 24rpx;
          color: #1F2329;
          margin-right: 4rpx;
        }

        .pay-price {
          font-size: 32rpx;
          font-weight: 700;
          color: #FF7D00; /* 实付金额橙色高亮 */
        }
      }
    }

    .actions-row {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      padding-top: 16rpx;
      gap: 16rpx;

      .btn-outline,
      .btn-green {
        height: 60rpx;
        line-height: 60rpx;
        border-radius: 30rpx;
        font-size: 26rpx;
        padding: 0 28rpx;
        margin: 0;

        &::after {
          border: none;
        }
      }

      .btn-outline {
        background-color: #FFFFFF;
        border: 1rpx solid #D8D8D8;
        color: #666666;
      }

      .btn-green {
        background-color: #25B873;
        color: #FFFFFF;
        font-weight: 600;
        border: none;
      }
    }
  }
}
</style>
