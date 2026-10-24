<template>
  <view class="confirm-page">
    <!-- 商品信息卡片 -->
    <view class="card goods-card">
      <image class="goods-img" :src="dishImage" mode="aspectFill" />
      <view class="goods-info">
        <text class="goods-name">{{ dishName }}</text>
        <view class="mode-tag">
          <text>{{ mode === 'open' ? '🔥 发起拼团（成为团长）' : '👥 参与好友拼团' }}</text>
        </view>
        <view class="price-row">
          <text class="symbol">¥</text>
          <text class="val">{{ price }}</text>
          <text class="badge">拼团优惠</text>
        </view>
      </view>
    </view>

    <!-- 履约方式切换 -->
    <view class="card">
      <view class="card-title">履约方式</view>
      <view class="delivery-tabs">
        <view
          :class="['tab-item', deliveryType === 'pickup' ? 'active' : '']"
          @tap="deliveryType = 'pickup'"
        >
          🏬 社区自提点
        </view>
        <view
          :class="['tab-item', deliveryType === 'delivery' ? 'active' : '']"
          @tap="deliveryType = 'delivery'"
        >
          🛵 送货到家
        </view>
      </view>

      <!-- 自提点选择 -->
      <view v-if="deliveryType === 'pickup'" class="pickup-section">
        <picker
          mode="selector"
          :range="storeList"
          range-key="name"
          :value="selectedStoreIndex"
          @change="onStoreChange"
        >
          <view class="picker-box">
            <view class="box-left">
              <text class="box-label">自提门店</text>
              <text class="box-val">{{ currentStore ? currentStore.name : '请选择自提门店' }}</text>
            </view>
            <text class="box-arrow">›</text>
          </view>
        </picker>
        <view class="store-detail" v-if="currentStore">
          <text class="sd-addr">📍 {{ currentStore.address }}</text>
          <text class="sd-phone">📞 团长电话: {{ currentStore.phone || currentStore.contactPhone || '13800138000' }}</text>
        </view>
      </view>

      <!-- 送货地址选择 -->
      <view v-else class="delivery-section">
        <view class="addr-box" @tap="goSelectAddress">
          <view class="box-left" v-if="selectedAddress">
            <view class="user-line">
              <text class="name">{{ selectedAddress.consignee }}</text>
              <text class="phone">{{ selectedAddress.phone }}</text>
            </view>
            <text class="addr-line">{{ formatAddress(selectedAddress) }}</text>
          </view>
          <view class="box-left empty-box" v-else>
            <text class="empty-txt">请选择收货地址</text>
          </view>
          <text class="box-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 购买数量 -->
    <view class="card num-card">
      <text class="card-title">购买数量</text>
      <view class="stepper">
        <view :class="['step-btn', quantity <= 1 ? 'disabled' : '']" @tap="onMinus">−</view>
        <text class="step-num">{{ quantity }}</text>
        <view class="step-btn" @tap="onPlus">+</view>
      </view>
    </view>

    <!-- 备注 -->
    <view class="card">
      <view class="card-title">订单备注</view>
      <input
        class="remark-input"
        v-model="remark"
        placeholder="选填：如包装要求或自提时间等"
        placeholder-class="placeholder"
      />
    </view>

    <!-- 底部结算栏 -->
    <view class="bottom-submit-bar">
      <view class="bsb-left">
        <text class="bsb-label">合计:</text>
        <text class="bsb-symbol">¥</text>
        <text class="bsb-price">{{ (Number(price) * quantity).toFixed(2) }}</text>
      </view>
      <button class="btn-submit" :loading="submitting" @tap="handleSubmit">
        {{ mode === 'open' ? '立即开团并支付' : '确认参团并支付' }}
      </button>
    </view>
  </view>
</template>

<script>
import { getStores, getAddressList, getDefaultAddress, openGroupon, joinGroupon } from '../../api/index.js'

export default {
  data() {
    return {
      mode: 'open', // 'open' | 'join'
      dishId: null,
      grouponId: null,
      dishName: '优质生鲜商品',
      dishImage: '/static/images/png/dish-1.png',
      price: '19.90',
      quantity: 1,
      deliveryType: 'pickup',
      storeList: [],
      selectedStoreIndex: 0,
      selectedAddress: null,
      remark: '',
      submitting: false
    }
  },
  computed: {
    currentStore() {
      if (this.storeList.length > 0 && this.selectedStoreIndex >= 0) {
        return this.storeList[this.selectedStoreIndex]
      }
      return null
    }
  },
  onLoad(options) {
    if (options) {
      this.mode = options.mode || 'open'
      this.dishId = options.dishId
      this.grouponId = options.grouponId
      if (options.price) this.price = options.price
      if (options.name) this.dishName = decodeURIComponent(options.name)
      if (options.image) this.dishImage = decodeURIComponent(options.image)
    }

    this.loadStores()
    this.loadAddress()

    uni.$on('selectAddress', (addr) => {
      this.selectedAddress = addr
      this.deliveryType = 'delivery'
    })
  },
  onUnload() {
    uni.$off('selectAddress')
  },
  methods: {
    formatAddress(item) {
      if (!item) return ''
      return [item.provinceName, item.cityName, item.districtName, item.detail].filter(Boolean).join('') || item.detail
    },
    async loadStores() {
      try {
        const res = await getStores()
        if (res && res.data) {
          const list = Array.isArray(res.data) ? res.data : (res.data.records || res.data.list || [])
          this.storeList = list
        }
      } catch (e) {}
    },
    async loadAddress() {
      try {
        const res = await getDefaultAddress()
        if (res && res.data) {
          this.selectedAddress = res.data
        } else {
          const listRes = await getAddressList()
          const list = Array.isArray(listRes.data) ? listRes.data : []
          if (list.length > 0) this.selectedAddress = list[0]
        }
      } catch (e) {}
    },
    onStoreChange(e) {
      this.selectedStoreIndex = Number(e.detail.value)
    },
    goSelectAddress() {
      uni.navigateTo({ url: '/pages/address/list?from=order' })
    },
    onMinus() {
      if (this.quantity > 1) this.quantity--
    },
    onPlus() {
      this.quantity++
    },
    async handleSubmit() {
      if (this.submitting) return

      if (this.deliveryType === 'delivery' && !this.selectedAddress) {
        return uni.showToast({ title: '请选择送货地址', icon: 'none' })
      }

      this.submitting = true
      const totalAmount = (Number(this.price) * this.quantity).toFixed(2)

      try {
        let orderId = 'GRP' + Date.now()

        if (this.mode === 'open') {
          // 我要开团
          const payload = {
            dishId: Number(this.dishId),
            quantity: this.quantity,
            deliveryType: this.deliveryType === 'pickup' ? 1 : 2,
            pickupPointId: this.deliveryType === 'pickup' ? (this.currentStore ? this.currentStore.id : 1) : null,
            addressBookId: this.deliveryType === 'delivery' ? this.selectedAddress.id : null,
            remark: this.remark
          }
          const res = await openGroupon(payload)
          if (res && res.data) {
            orderId = res.data.orderId || res.data.id || orderId
          }
        } else {
          // 去参团
          const payload = {
            grouponId: Number(this.grouponId),
            quantity: this.quantity,
            deliveryType: this.deliveryType === 'pickup' ? 1 : 2,
            pickupPointId: this.deliveryType === 'pickup' ? (this.currentStore ? this.currentStore.id : 1) : null,
            addressBookId: this.deliveryType === 'delivery' ? this.selectedAddress.id : null,
            remark: this.remark
          }
          const res = await joinGroupon(payload)
          if (res && res.data) {
            orderId = res.data.orderId || res.data.id || orderId
          }
        }

        uni.showToast({ title: '拼团订单提交成功', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({
            url: `/pages/order/pay?orderId=${orderId}&amount=${totalAmount}`
          })
        }, 500)
      } catch (err) {
        uni.showToast({ title: err.message || '提交拼团失败', icon: 'none' })
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

.card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1F2329;
  margin-bottom: 16rpx;
}

.goods-card {
  display: flex;
  gap: 20rpx;
}

.goods-img {
  width: 180rpx;
  height: 180rpx;
  border-radius: 16rpx;
  background: #F2F3F5;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1F2329;
  line-height: 1.4;
}

.mode-tag {
  background: #FFF0EB;
  color: #FF5722;
  font-size: 22rpx;
  font-weight: 500;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  width: fit-content;
}

.price-row {
  display: flex;
  align-items: baseline;
  color: #FF5722;

  .symbol {
    font-size: 24rpx;
    font-weight: 700;
  }
  .val {
    font-size: 40rpx;
    font-weight: 800;
  }
  .badge {
    background: #FFE8E0;
    color: #FF5722;
    font-size: 20rpx;
    padding: 2rpx 10rpx;
    border-radius: 6rpx;
    margin-left: 12rpx;
  }
}

.delivery-tabs {
  display: flex;
  background-color: #F2F3F5;
  border-radius: 12rpx;
  padding: 6rpx;
  margin-bottom: 20rpx;

  .tab-item {
    flex: 1;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26rpx;
    font-weight: 500;
    color: #646A73;
    border-radius: 8rpx;
    transition: all 0.2s;

    &.active {
      background: #FFFFFF;
      color: #25B873;
      font-weight: 600;
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
    }
  }
}

.picker-box, .addr-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #F0F2F5;

  .box-left {
    display: flex;
    flex-direction: column;

    .box-label {
      font-size: 24rpx;
      color: #86909C;
      margin-bottom: 4rpx;
    }
    .box-val {
      font-size: 28rpx;
      font-weight: 600;
      color: #1D2129;
    }
  }

  .box-arrow {
    font-size: 32rpx;
    color: #C9CDD4;
  }
}

.store-detail {
  padding-top: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;

  .sd-addr {
    font-size: 24rpx;
    color: #4E5969;
  }
  .sd-phone {
    font-size: 24rpx;
    color: #86909C;
  }
}

.user-line {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 6rpx;

  .name {
    font-size: 30rpx;
    font-weight: 600;
    color: #1D2129;
  }
  .phone {
    font-size: 28rpx;
    color: #646A73;
  }
}

.addr-line {
  font-size: 26rpx;
  color: #4E5969;
  line-height: 1.4;
}

.empty-box {
  .empty-txt {
    font-size: 28rpx;
    font-weight: 600;
    color: #25B873;
  }
}

.num-card {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .card-title {
    margin-bottom: 0;
  }
}

.stepper {
  display: flex;
  align-items: center;
  border: 1rpx solid #E5E6EB;
  border-radius: 8rpx;
  overflow: hidden;

  .step-btn {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #F7F8FA;
    font-size: 32rpx;
    color: #1D2129;

    &.disabled {
      color: #C9CDD4;
    }
  }

  .step-num {
    width: 80rpx;
    text-align: center;
    font-size: 28rpx;
    font-weight: 600;
    color: #1D2129;
  }
}

.remark-input {
  background: #F7F8FA;
  height: 80rpx;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  color: #1D2129;
}

.bottom-submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: #FFFFFF;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32rpx env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.bsb-left {
  display: flex;
  align-items: baseline;

  .bsb-label {
    font-size: 26rpx;
    color: #646A73;
    margin-right: 8rpx;
  }
  .bsb-symbol {
    font-size: 28rpx;
    font-weight: 700;
    color: #FF5722;
  }
  .bsb-price {
    font-size: 44rpx;
    font-weight: 800;
    color: #FF5722;
  }
}

.btn-submit {
  background: linear-gradient(135deg, #FF6B35 0%, #FF8F3D 100%);
  color: #FFFFFF;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 44rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
}
</style>
