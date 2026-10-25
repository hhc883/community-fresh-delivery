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
              <text v-if="item.isDefault === 1" class="check-icon">✓</text>
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
      <image class="empty-img" src="/static/images/dish-1.svg" mode="aspectFit" />
      <text class="empty-title">暂无收货地址</text>
      <text class="empty-desc">添加收货地址，下单更便捷</text>
    </view>

    <!-- 底部按钮 -->
    <view class="bottom-bar">
      <button class="add-btn" @tap="handleAdd">
        <text class="plus-icon">+</text>
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

<style lang="scss" scoped>
.address-container {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 24rpx;
  padding-bottom: 160rpx;
  box-sizing: border-box;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.address-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.card-main {
  border-bottom: 1rpx solid #F0F2F5;
  padding-bottom: 24rpx;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1F2329;
}

.phone {
  font-size: 28rpx;
  color: #646A73;
}

.tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  line-height: 1.2;
}

.tag-default {
  background-color: #E8F8F0;
  color: #25B873;
  font-weight: 500;
}

.tag-label {
  background-color: #F2F3F5;
  color: #4E5969;
}

.address-text {
  font-size: 26rpx;
  color: #33383F;
  line-height: 1.5;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
}

.default-check {
  display: flex;
  align-items: center;
  gap: 12rpx;
  cursor: pointer;
}

.radio-circle {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  border: 2rpx solid #C9CDD4;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &.checked {
    background-color: #25B873;
    border-color: #25B873;
  }
}

.check-icon {
  color: #FFFFFF;
  font-size: 20rpx;
  font-weight: bold;
}

.default-label {
  font-size: 26rpx;
  color: #86909C;
}

.btn-group {
  display: flex;
  gap: 28rpx;
}

.action-btn {
  font-size: 26rpx;
  color: #646A73;
  cursor: pointer;

  &.delete-btn {
    color: #F53F3F;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 180rpx;
}

.empty-img {
  width: 200rpx;
  height: 200rpx;
  opacity: 0.6;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #1D2129;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #86909C;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  padding: 20rpx 32rpx env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.add-btn {
  background: linear-gradient(135deg, #25B873 0%, #1DA362 100%);
  color: #FFFFFF;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border: none;
}

.plus-icon {
  font-size: 36rpx;
  line-height: 1;
}
</style>
