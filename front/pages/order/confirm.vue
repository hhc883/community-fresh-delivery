<template>
  <view class="confirm-page">
    <!-- 履约方式切换 Tab -->
    <view class="delivery-tabs">
      <view
        :class="['tab-item', deliveryType === 'pickup' ? 'active' : '']"
        @click="deliveryType = 'pickup'"
      >
        <text class="tab-text">🏬 到店自提</text>
      </view>
      <view
        :class="['tab-item', deliveryType === 'delivery' ? 'active' : '']"
        @click="deliveryType = 'delivery'"
      >
        <text class="tab-text">🛵 送货到家</text>
      </view>
    </view>

    <!-- 模式A：到店自提卡片 -->
    <view class="card consignee-card" v-if="deliveryType === 'pickup'" @click="goPersonEdit">
      <view class="cc-left">
        <view class="user-row">
          <text class="user-icon">👤</text>
          <text class="user-name">提货人：{{ person.name || '张三' }}</text>
          <text class="user-phone">{{ person.phone || '13800138000' }}</text>
        </view>
        <view class="addr-text">
          自提网点：{{ pickupPoint.name || '高黎柱发士多店(请自备袋子谢谢)' }}
        </view>
        <view class="addr-sub">
          {{ pickupPoint.address || person.address || '广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号' }}
        </view>
      </view>
      <text class="cc-arrow">›</text>
    </view>

    <!-- 模式B：送货到家地址卡片 -->
    <view class="card consignee-card delivery-card" v-else @click="goSelectAddress">
      <view class="cc-left" v-if="selectedAddress">
        <view class="user-row">
          <text class="user-icon">📍</text>
          <text class="user-name">{{ selectedAddress.consignee }}</text>
          <text class="user-phone">{{ selectedAddress.phone }}</text>
          <text class="tag-default" v-if="selectedAddress.isDefault === 1">默认</text>
        </view>
        <view class="addr-text">
          {{ formatAddress(selectedAddress) }}
        </view>
      </view>
      <view class="cc-left empty-addr" v-else>
        <text class="user-icon">📍</text>
        <text class="empty-addr-text">请选择或添加收货地址</text>
      </view>
      <text class="cc-arrow">›</text>
    </view>

    <!-- 自提/配送时间选择行 -->
    <view class="card time-card" @click="showTimeModal = true">
      <text class="tc-label">{{ deliveryType === 'pickup' ? '自提时间' : '期望送达' }}</text>
      <view class="tc-val">
        <text class="tc-time">{{ selectedDateText }} {{ selectedTimeSlot }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 商品明细卡片 -->
    <view class="card goods-card">
      <view class="goods-item" v-for="item in orderItems" :key="item.id">
        <image class="goods-img" :src="item.image || '/static/images/png/dish-2.png'" mode="aspectFill" />
        <view class="goods-info">
          <text class="goods-name">{{ item.name || 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量' }}</text>
          <text class="goods-spec">{{ item.flavor || '晴空翠 4+128G' }}</text>
          <view class="goods-bottom">
            <text class="goods-price">¥{{ $fmtMoney(item.price || 599) }}</text>
            <text class="goods-qty">x {{ item.quantity || 1 }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 优惠券与费用明细卡片 -->
    <view class="card fee-card">
      <!-- 优惠券 -->
      <view class="fee-row coupon-row" @click="goCoupons">
        <view class="coupon-left">
          <text class="coupon-tag">券</text>
          <text class="coupon-title">优惠券</text>
        </view>
        <view class="coupon-right">
          <text class="coupon-status">暂无可用优惠券</text>
          <text class="arrow">›</text>
        </view>
      </view>

      <view class="card-divider"></view>

      <!-- 商品总额 -->
      <view class="fee-row">
        <text class="fee-label">商品总额</text>
        <text class="fee-val">¥{{ $fmtMoney(rawTotalPrice) }}</text>
      </view>

      <!-- 运费 -->
      <view class="fee-row">
        <text class="fee-label">包装/运费</text>
        <text class="fee-val highlight-free">+¥0</text>
      </view>
    </view>

    <!-- 底部结算栏 -->
    <view class="bottom-submit-bar">
      <view class="bsb-left">
        <text class="bsb-label">合计:</text>
        <text class="bsb-symbol">¥</text>
        <text class="bsb-price">{{ $fmtMoney(finalPayPrice) }}</text>
      </view>
      <button class="btn-submit-green" :loading="submitting" @click="onSubmitOrder">
        提交订单
      </button>
    </view>

    <!-- 自提时间选择抽屉弹窗 (完全还原设计图) -->
    <view class="time-modal-mask" v-if="showTimeModal" @click="showTimeModal = false">
      <view class="time-modal-content" @click.stop>
        <view class="tm-header">
          <text class="tm-btn cancel" @click="showTimeModal = false">取消</text>
          <text class="tm-title">选择自提时段</text>
          <text class="tm-btn confirm" @click="confirmTime">确定</text>
        </view>

        <view class="tm-body">
          <!-- 左侧日期列表 -->
          <scroll-view class="date-col" scroll-y>
            <view
              class="date-item"
              v-for="(d, idx) in dateList"
              :key="idx"
              :class="{ active: tempDateIndex === idx }"
              @click="tempDateIndex = idx"
            >
              <text class="date-text">{{ d.label }}</text>
            </view>
          </scroll-view>

          <!-- 右侧时间段列表 -->
          <scroll-view class="slot-col" scroll-y>
            <view
              class="slot-item"
              v-for="(slot, idx) in timeSlots"
              :key="idx"
              :class="{ active: tempSlotIndex === idx }"
              @click="tempSlotIndex = idx"
            >
              <text class="slot-text">{{ slot }}</text>
              <text class="slot-check" v-if="tempSlotIndex === idx">✓</text>
            </view>
          </scroll-view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { createOrder, getDefaultAddress } from '@/api/index.js'

export default {
  data() {
    return {
      submitting: false,
      showTimeModal: false,
      deliveryType: 'pickup', // 'pickup' | 'delivery'
      selectedAddress: null,
      tempDateIndex: 0,
      tempSlotIndex: 3, // 默认 16:00-18:00
      selectedDateIndex: 0,
      selectedSlotIndex: 3,
      person: {
        name: '张三',
        phone: '13800138000',
        address: '广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号'
      },
      orderItems: [
        {
          id: '2-default',
          dishId: 2,
          name: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量',
          flavor: '晴空翠 4+128G',
          price: 599.00,
          quantity: 1,
          image: '/static/images/png/dish-2.png'
        }
      ],
      dateList: [
        { label: '今日 (05月20日)' },
        { label: '明日 (05月21日)' },
        { label: '后天 (05月22日)' },
        { label: '大后天 (05月23日)' }
      ],
      timeSlots: [
        '09:00 - 11:00',
        '11:00 - 13:00',
        '14:00 - 16:00',
        '16:00 - 18:00',
        '18:00 - 20:00',
        '20:00 - 21:30'
      ]
    }
  },
  computed: {
    ...mapState(['pickupPoint']),
    selectedDateText() {
      return this.dateList[this.selectedDateIndex].label.split(' ')[0]
    },
    selectedTimeSlot() {
      return this.timeSlots[this.selectedSlotIndex]
    },
    rawTotalPrice() {
      return this.orderItems.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0)
    },
    finalPayPrice() {
      return this.rawTotalPrice
    }
  },
  onLoad() {
    // 读取本地提货人缓存
    const p = uni.getStorageSync('pickup_person')
    if (p) {
      try {
        const parsed = typeof p === 'string' ? JSON.parse(p) : p
        if (parsed.name) this.person.name = parsed.name
        if (parsed.phone) this.person.phone = parsed.phone
        if (parsed.address) this.person.address = parsed.address
      } catch (e) {}
    }

    // 默认尝试加载默认收货地址
    this.loadDefaultAddress()

    // 监听地址选择事件
    uni.$on('selectAddress', (addr) => {
      this.selectedAddress = addr
      this.deliveryType = 'delivery'
    })

    // 读取结算商品缓存
    const checkoutItems = uni.getStorageSync('checkout_items')
    if (checkoutItems && checkoutItems.length > 0) {
      this.orderItems = checkoutItems
    }
  },
  onUnload() {
    uni.$off('selectAddress')
  },
  methods: {
    formatAddress(item) {
      if (!item) return ''
      return [item.provinceName, item.cityName, item.districtName, item.detail].filter(Boolean).join('') || item.detail
    },
    async loadDefaultAddress() {
      try {
        const res = await getDefaultAddress()
        if (res.data) {
          this.selectedAddress = res.data
        }
      } catch (e) {}
    },
    goSelectAddress() {
      uni.navigateTo({ url: '/pages/address/list?from=order' })
    },
    goPersonEdit() {
      uni.navigateTo({
        url: '/pages/order/person'
      })
    },
    goCoupons() {
      uni.navigateTo({
        url: '/pages/coupons/coupons'
      })
    },
    confirmTime() {
      this.selectedDateIndex = this.tempDateIndex
      this.selectedSlotIndex = this.tempSlotIndex
      this.showTimeModal = false
    },
    async onSubmitOrder() {
      if (this.submitting) return
      
      if (this.deliveryType === 'delivery' && !this.selectedAddress) {
        return uni.showToast({ title: '请先选择送货上门地址', icon: 'none' })
      }

      this.submitting = true

      const isPickup = this.deliveryType === 'pickup'
      const orderPayload = {
        amount: this.finalPayPrice,
        deliveryStatus: isPickup ? 1 : 2,
        orderTime: new Date().toISOString(),
        orderDetailList: this.orderItems.map(item => ({
          name: item.name,
          dishId: item.dishId || 2,
          number: item.quantity || 1,
          amount: item.price,
          image: item.image,
          dishFlavor: item.flavor
        }))
      }

      if (isPickup) {
        orderPayload.pickupPointId = (this.pickupPoint && this.pickupPoint.id) || 1
        orderPayload.pickupPointName = (this.pickupPoint && this.pickupPoint.name) || '高黎柱发士多店'
        orderPayload.pickupPersonName = this.person.name
        orderPayload.pickupPersonPhone = this.person.phone
        orderPayload.pickupTimeSlot = `${this.selectedDateText} ${this.selectedTimeSlot}`
      } else {
        orderPayload.addressBookId = this.selectedAddress.id
        orderPayload.consignee = this.selectedAddress.consignee
        orderPayload.phone = this.selectedAddress.phone
        orderPayload.address = this.formatAddress(this.selectedAddress)
      }

      try {
        let orderId = 'ORD' + Date.now()
        try {
          const res = await createOrder(orderPayload)
          if (res && res.data) {
            const dataObj = res.data.data || res.data
            orderId = dataObj.id || dataObj.orderNumber || orderId
          }
        } catch (e) {
          // Local fallback
        }

        uni.redirectTo({
          url: `/pages/order/pay?orderId=${orderId}&amount=${this.finalPayPrice}`
        })
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.confirm-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 20rpx 24rpx 140rpx;
  box-sizing: border-box;
}

.delivery-tabs {
  display: flex;
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);

  .tab-item {
    flex: 1;
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12rpx;
    transition: all 0.2s;

    .tab-text {
      font-size: 28rpx;
      font-weight: 500;
      color: #646A73;
    }

    &.active {
      background: linear-gradient(135deg, #25B873 0%, #1DA362 100%);
      
      .tab-text {
        color: #FFFFFF;
        font-weight: 600;
      }
    }
  }
}

.card {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);
}

.addr-sub {
  font-size: 24rpx;
  color: #8F959E;
  margin-top: 6rpx;
}

.tag-default {
  background-color: #E8F8F0;
  color: #25B873;
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  margin-left: 12rpx;
}

.empty-addr {
  display: flex;
  align-items: center;
  gap: 12rpx;

  .empty-addr-text {
    font-size: 30rpx;
    font-weight: 600;
    color: #25B873;
  }
}

/* 提货人信息卡片 */
.consignee-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .cc-left {
    flex: 1;

    .user-row {
      display: flex;
      align-items: center;
      margin-bottom: 12rpx;

      .user-icon {
        font-size: 32rpx;
        margin-right: 12rpx;
      }

      .user-name {
        font-size: 32rpx;
        font-weight: 700;
        color: #1F2329;
        margin-right: 20rpx;
      }

      .user-phone {
        font-size: 30rpx;
        font-weight: 600;
        color: #1F2329;
      }
    }

    .addr-text {
      font-size: 26rpx;
      color: #8F959E;
      line-height: 38rpx;
    }
  }

  .cc-arrow {
    font-size: 36rpx;
    color: #C9CDD4;
    margin-left: 16rpx;
  }
}

/* 自提时间卡片 */
.time-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx;

  .tc-label {
    font-size: 28rpx;
    font-weight: 600;
    color: #1F2329;
  }

  .tc-val {
    display: flex;
    align-items: center;

    .tc-time {
      font-size: 28rpx;
      color: #25B873;
      font-weight: 500;
    }

    .arrow {
      font-size: 32rpx;
      color: #C9CDD4;
      margin-left: 10rpx;
    }
  }
}

/* 商品明细卡片 */
.goods-card {
  padding: 24rpx 28rpx;

  .goods-item {
    display: flex;

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
        font-size: 28rpx;
        font-weight: 600;
        color: #1F2329;
        line-height: 40rpx;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
      }

      .goods-spec {
        font-size: 24rpx;
        color: #8F959E;
        margin-top: 6rpx;
      }

      .goods-bottom {
        display: flex;
        align-items: baseline;
        justify-content: space-between;

        .goods-price {
          font-size: 32rpx;
          font-weight: 700;
          color: #FA5151;
        }

        .goods-qty {
          font-size: 26rpx;
          color: #8F959E;
        }
      }
    }
  }
}

/* 费用明细卡片 */
.fee-card {
  padding: 20rpx 28rpx;

  .fee-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 72rpx;

    .fee-label {
      font-size: 26rpx;
      color: #1F2329;
    }

    .fee-val {
      font-size: 26rpx;
      color: #1F2329;

      &.highlight-free {
        color: #25B873;
        font-weight: 500;
      }
    }
  }

  .coupon-row {
    .coupon-left {
      display: flex;
      align-items: center;

      .coupon-tag {
        background-color: #FF7D00;
        color: #FFFFFF;
        font-size: 20rpx;
        padding: 2rpx 10rpx;
        border-radius: 6rpx;
        margin-right: 12rpx;
      }

      .coupon-title {
        font-size: 26rpx;
        color: #1F2329;
      }
    }

    .coupon-right {
      display: flex;
      align-items: center;

      .coupon-status {
        font-size: 26rpx;
        color: #8F959E;
      }

      .arrow {
        font-size: 30rpx;
        color: #C9CDD4;
        margin-left: 8rpx;
      }
    }
  }

  .card-divider {
    height: 1rpx;
    background-color: #F0F2F5;
    margin: 10rpx 0;
  }
}

/* 底部结算提交栏 */
.bottom-submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background-color: #FFFFFF;
  border-top: 1rpx solid #EBEBEB;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28rpx;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 100;

  .bsb-left {
    display: flex;
    align-items: baseline;

    .bsb-label {
      font-size: 28rpx;
      color: #1F2329;
      margin-right: 6rpx;
    }

    .bsb-symbol {
      font-size: 26rpx;
      font-weight: 700;
      color: #FA5151;
    }

    .bsb-price {
      font-size: 40rpx;
      font-weight: 800;
      color: #FA5151;
    }
  }

  .btn-submit-green {
    width: 280rpx;
    height: 80rpx;
    line-height: 80rpx;
    background-color: #18804E; /* 深草绿风格提交按钮 */
    border-radius: 40rpx;
    color: #FFFFFF;
    font-size: 30rpx;
    font-weight: 600;
    margin: 0;
    border: none;

    &::after {
      border: none;
    }
  }
}

/* 自提时间弹窗抽屉 */
.time-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;

  .time-modal-content {
    width: 100%;
    background-color: #FFFFFF;
    border-radius: 24rpx 24rpx 0 0;
    overflow: hidden;

    .tm-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 90rpx;
      padding: 0 30rpx;
      border-bottom: 1rpx solid #F0F2F5;

      .tm-title {
        font-size: 30rpx;
        font-weight: 600;
        color: #1F2329;
      }

      .tm-btn {
        font-size: 28rpx;
        padding: 10rpx;

        &.cancel {
          color: #8F959E;
        }

        &.confirm {
          color: #1677FF;
          font-weight: 600;
        }
      }
    }

    .tm-body {
      display: flex;
      height: 520rpx;

      .date-col {
        width: 300rpx;
        background-color: #F7F8FA;

        .date-item {
          height: 96rpx;
          display: flex;
          align-items: center;
          padding-left: 28rpx;

          .date-text {
            font-size: 26rpx;
            color: #666666;
          }

          &.active {
            background-color: #FFFFFF;

            .date-text {
              color: #1F2329;
              font-weight: 700;
            }
          }
        }
      }

      .slot-col {
        flex: 1;
        background-color: #FFFFFF;

        .slot-item {
          height: 86rpx;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 0 36rpx;
          border-bottom: 1rpx solid #F8F9FA;

          .slot-text {
            font-size: 26rpx;
            color: #1F2329;
          }

          &.active {
            .slot-text {
              color: #25B873;
              font-weight: 700;
            }

            .slot-check {
              color: #25B873;
              font-size: 28rpx;
              font-weight: 700;
            }
          }
        }
      }
    }
  }
}
</style>
