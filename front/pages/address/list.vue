<template>
  <view class="address-container">
    <view v-if="error" class="error" @tap="loadAddresses">{{ error }} · 点击重试</view>
    <view v-if="loading" class="empty">读取地址中…</view>
    <view class="address-list" v-if="!loading && !error && addressList.length > 0">
      <view
        class="address-card"
        v-for="item in addressList"
        :key="item.id"
        @tap="handleSelect(item)"
      >
        <view class="card-main">
          <view class="user-row">
            <text class="name">{{ item.consignee }}</text>
            <text class="phone">{{ item.phone }}</text>
            <text class="tag tag-default" v-if="item.isDefault === 1">默认</text>
            <text class="tag tag-label" v-if="item.label">{{ item.label }}</text>
          </view>
          <view class="address-text">
            {{ formatAddress(item) }}
          </view>
        </view>
        <view class="card-actions" @tap.stop>
          <view class="default-check" @tap="handleSetDefault(item)">
            <view :class="['radio-circle', item.isDefault === 1 ? 'checked' : '']">
              <image
                v-if="item.isDefault === 1"
                class="check-icon-img"
                src="/static/icons/lucide/check-white.png"
                mode="aspectFit"
              />
            </view>
            <text class="default-label">{{ item.isDefault === 1 ? '默认地址' : '设为默认' }}</text>
          </view>
          <view class="btn-group">
            <view class="action-btn edit-btn" @tap="handleEdit(item)">
              <text class="btn-text">编辑</text>
            </view>
            <view class="action-btn delete-btn" @tap="handleDelete(item)">
              <text class="btn-text">删除</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else-if="!loading && !error">
      <image class="empty-img" src="/static/icons/lucide/map-pin-muted.png" mode="aspectFit" />
      <text class="empty-title">暂无收货地址</text>
      <text class="empty-desc">添加收货地址，送货到家更便捷</text>
    </view>

    <!-- 底部按钮 -->
    <view class="bottom-bar">
      <button class="add-btn" @tap="handleAdd">
        <image class="plus-icon-img" src="/static/icons/lucide/plus-white.png" mode="aspectFit" />
        <text>新增收货地址</text>
      </button>
    </view>
  </view>
</template>

<script>
import { getAddressList, setDefaultAddress, deleteAddress } from '../../api/index.js'

export default {
  data() {
    return {
      addressList: [],
      loading: true,
      error: '',
      fromOrder: false
    }
  },
  onLoad(options) {
    if (options && options.from === 'order') {
      this.fromOrder = true
    }
  },
  onShow() {
    this.loadAddresses()
  },
  methods: {
    formatAddress(item) {
      const parts = [
        item.provinceName,
        item.cityName,
        item.districtName,
        item.detail
      ].filter(Boolean)
      return parts.join('') || item.detail || '地址信息'
    },
    async loadAddresses() {
      this.loading = true
      this.error = ''
      try {
        const res = await getAddressList()
        this.addressList = Array.isArray(res.data) ? res.data : (res.data && res.data.records) || []
      } catch (err) {
        this.error = err.message || '加载地址失败'
        uni.showToast({ title: err.message || '加载地址失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    handleSelect(item) {
      if (this.fromOrder) {
        uni.$emit('selectAddress', item)
        uni.navigateBack()
      }
    },
    handleAdd() {
      uni.navigateTo({ url: '/pages/address/edit' })
    },
    handleEdit(item) {
      uni.navigateTo({ url: `/pages/address/edit?id=${item.id}` })
    },
    async handleSetDefault(item) {
      if (item.isDefault === 1) return
      try {
        await setDefaultAddress(item.id)
        uni.showToast({ title: '已设为默认', icon: 'success' })
        this.loadAddresses()
      } catch (err) {
        uni.showToast({ title: err.message || '设置失败', icon: 'none' })
      }
    },
    handleDelete(item) {
      uni.showModal({
        title: '提示',
        content: `确定删除收货地址「${item.consignee}」吗？`,
        confirmColor: '#FF4D4F',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteAddress(item.id)
              uni.showToast({ title: '删除成功', icon: 'success' })
              this.loadAddresses()
            } catch (err) {
              uni.showToast({ title: err.message || '删除失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.address-container {
  min-height: 100vh;
  background-color: #f6f8f5;
  padding: 24rpx;
  padding-bottom: calc(160rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.address-card {
  background: #ffffff;
  border-radius: 18rpx;
  padding: 28rpx;
  border: 1rpx solid #e5ebe6;
  box-sizing: border-box;
}

.card-main {
  border-bottom: 1rpx solid #eef2ef;
  padding-bottom: 20rpx;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
  color: #19241d;
}

.phone {
  font-size: 26rpx;
  color: #556257;
}

.tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  line-height: 1.2;
}

.tag-default {
  background-color: #eaf5ee;
  color: #27744d;
  font-weight: 600;
}

.tag-label {
  background-color: #f0f4f1;
  color: #4b5a4f;
}

.address-text {
  font-size: 26rpx;
  color: #333f36;
  line-height: 1.5;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18rpx;
}

.default-check {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.radio-circle {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  border: 2rpx solid #c0ccc2;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.radio-circle.checked {
  background-color: #27744d;
  border-color: #27744d;
}

.check-icon-img {
  width: 20rpx;
  height: 20rpx;
}

.default-label {
  font-size: 24rpx;
  color: #778579;
}

.btn-group {
  display: flex;
  gap: 28rpx;
}

.action-btn {
  font-size: 26rpx;
  color: #556257;
}

.action-btn.delete-btn {
  color: #c93424;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 180rpx;
}

.empty-img {
  width: 140rpx;
  height: 140rpx;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #19241d;
  margin-bottom: 8rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: #778579;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 20rpx 32rpx calc(20rpx + constant(safe-area-inset-bottom));
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e5ebe6;
  z-index: 100;
  box-sizing: border-box;
}

.add-btn {
  background: #27744d;
  color: #ffffff;
  height: 84rpx;
  border-radius: 42rpx;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  box-shadow: 0 4rpx 14rpx rgba(39, 116, 77, 0.22);
}

.add-btn::after {
  border: none;
}

.plus-icon-img {
  width: 28rpx;
  height: 28rpx;
  margin-right: 10rpx;
}
</style>
