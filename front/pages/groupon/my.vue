<template>
  <view class="my-groupon-page">
    <!-- 顶部状态 Tabs -->
    <view class="tabs-header">
      <view
        v-for="tab in tabs"
        :key="tab.status"
        :class="['tab-item', currentTab === tab.status ? 'active' : '']"
        @tap="changeTab(tab.status)"
      >
        <text class="tab-label">{{ tab.label }}</text>
      </view>
    </view>

    <!-- 列表内容 -->
    <view class="groupon-list" v-if="filteredList.length > 0">
      <view class="groupon-card" v-for="item in filteredList" :key="item.id" @tap="goDetail(item)">
        <view class="card-head">
          <view class="head-left">
            <text class="role-badge" :class="item.isLeader ? 'leader' : 'member'">
              {{ item.isLeader ? '👑 团长' : '👤 团员' }}
            </text>
            <text class="order-time">{{ formatTime(item.createTime) }}</text>
          </view>
          <text :class="['status-badge', getStatusClass(item.status)]">
            {{ getStatusText(item.status) }}
          </text>
        </view>

        <view class="goods-body">
          <image class="goods-img" :src="item.dishImage || '/static/images/png/dish-1.png'" mode="aspectFill" />
          <view class="goods-info">
            <text class="goods-name">{{ item.dishName || '社区拼团生鲜' }}</text>
            <view class="progress-box">
              <text class="progress-text">
                已拼 {{ item.currentSize || 1 }}/{{ item.requiredSize || 2 }} 人
              </text>
              <text class="lack-tip" v-if="item.status === 1">
                还差 {{ item.lackSize || 1 }} 人成团
              </text>
            </view>
            <view class="price-row">
              <text class="price-txt">拼团价: ¥{{ item.grouponPrice || item.price }}</text>
              <text class="num-txt">x{{ item.quantity || 1 }}</text>
            </view>
          </view>
        </view>

        <view class="card-footer" @tap.stop>
          <view class="total-wrap">
            <text class="total-label">实付:</text>
            <text class="total-price">¥{{ item.totalAmount || item.grouponPrice || item.price }}</text>
          </view>
          <view class="footer-btns">
            <button
              class="btn btn-secondary"
              v-if="item.status === 1"
              @tap="handleInvite(item)"
            >
              邀请好友拼单
            </button>
            <button
              class="btn btn-primary"
              v-if="item.status === 2"
              @tap="goOrderDetail(item)"
            >
              查看提货码
            </button>
            <button
              class="btn btn-secondary"
              v-if="item.status === 3 || item.status === 4"
              @tap="goGrouponHall"
            >
              再次开团
            </button>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else-if="!loading">
      <image class="empty-img" src="/static/images/dish-3.svg" mode="aspectFit" />
      <text class="empty-title">暂无相关拼团记录</text>
      <text class="empty-desc">赶紧去拼团专区看看邻居们在拼什么吧</text>
      <button class="btn-go-hall" @tap="goGrouponHall">去拼团专区</button>
    </view>
  </view>
</template>

<script>
import { getMyGroupons } from '../../api/index.js'

export default {
  data() {
    return {
      currentTab: null, // null: 全部, 1: 拼团中, 2: 拼团成功, 3: 拼团失败
      tabs: [
        { label: '全部', status: null },
        { label: '拼团中', status: 1 },
        { label: '已成团', status: 2 },
        { label: '拼团失败', status: 3 }
      ],
      loading: true,
      grouponList: []
    }
  },
  computed: {
    filteredList() {
      if (this.currentTab === null) return this.grouponList
      if (this.currentTab === 3) {
        return this.grouponList.filter(g => g.status === 3 || g.status === 4)
      }
      return this.grouponList.filter(g => g.status === this.currentTab)
    }
  },
  onShow() {
    this.loadData()
  },
  methods: {
    formatTime(t) {
      if (!t) return '刚刚'
      return String(t).replace('T', ' ').slice(0, 16)
    },
    getStatusText(status) {
      switch (status) {
        case 1: return '拼团中 · 待成团'
        case 2: return '🎉 拼团成功'
        case 3: return '拼团失败'
        case 4: return '已超时退款'
        default: return '进行中'
      }
    },
    getStatusClass(status) {
      switch (status) {
        case 1: return 'status-pending'
        case 2: return 'status-success'
        case 3:
        case 4: return 'status-fail'
        default: return 'status-pending'
      }
    },
    async loadData() {
      this.loading = true
      try {
        const res = await getMyGroupons(this.currentTab)
        if (res && res.data) {
          const list = Array.isArray(res.data) ? res.data : (res.data.records || [])
          this.grouponList = list
        }
      } catch (err) {
        // 如果后端暂无记录，兜底提供本地示例
        this.grouponList = [
          {
            id: 101,
            dishId: 2,
            dishName: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量',
            dishImage: '/static/images/png/dish-2.png',
            status: 1,
            isLeader: true,
            currentSize: 1,
            requiredSize: 2,
            lackSize: 1,
            grouponPrice: '499.00',
            totalAmount: '499.00',
            quantity: 1,
            orderId: '202610220001',
            createTime: '2026-10-24 18:30'
          }
        ]
      } finally {
        this.loading = false
      }
    },
    changeTab(s) {
      this.currentTab = s
    },
    goDetail(item) {
      uni.navigateTo({ url: `/pages/groupon/detail?dishId=${item.dishId || 2}` })
    },
    handleInvite(item) {
      uni.showModal({
        title: '邀请好友拼单',
        content: `快分享给好友！还差 ${item.lackSize || 1} 人即可享受拼团优惠！`,
        confirmText: '复制链接',
        confirmColor: '#FF5722',
        success: (res) => {
          if (res.confirm) {
            uni.setClipboardData({
              data: `【社区拼团】我正在拼「${item.dishName}」，仅需¥${item.grouponPrice}，快来参团！`,
              success: () => {
                uni.showToast({ title: '口令已复制', icon: 'success' })
              }
            })
          }
        }
      })
    },
    goOrderDetail(item) {
      uni.navigateTo({ url: `/pages/order/detail?id=${item.orderId || '202610220001'}` })
    },
    goGrouponHall() {
      uni.navigateTo({ url: '/pages/groupon/list' })
    }
  }
}
</script>

<style lang="scss" scoped>
.my-groupon-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 60rpx;
}

.tabs-header {
  position: sticky;
  top: 0;
  display: flex;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  z-index: 10;
}

.tab-item {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  .tab-label {
    font-size: 28rpx;
    color: #646A73;
    font-weight: 500;
  }

  &.active {
    .tab-label {
      color: #FF5722;
      font-weight: 700;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      width: 48rpx;
      height: 4rpx;
      background: #FF5722;
      border-radius: 2rpx;
    }
  }
}

.groupon-list {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.groupon-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #F0F2F5;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.role-badge {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;

  &.leader {
    background: #FFF7E8;
    color: #FF7D00;
    font-weight: 600;
  }
  &.member {
    background: #E8F3FF;
    color: #165DFF;
  }
}

.order-time {
  font-size: 24rpx;
  color: #86909C;
}

.status-badge {
  font-size: 24rpx;
  font-weight: 600;

  &.status-pending {
    color: #FF5722;
  }
  &.status-success {
    color: #25B873;
  }
  &.status-fail {
    color: #86909C;
  }
}

.goods-body {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F8F9FA;
  gap: 20rpx;
}

.goods-img {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
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
  font-size: 28rpx;
  font-weight: 600;
  color: #1F2329;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.progress-box {
  display: flex;
  gap: 16rpx;
  font-size: 22rpx;

  .progress-text {
    color: #4E5969;
  }
  .lack-tip {
    color: #FF5722;
    font-weight: 500;
  }
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .price-txt {
    font-size: 26rpx;
    font-weight: 700;
    color: #1D2129;
  }
  .num-txt {
    font-size: 24rpx;
    color: #86909C;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
}

.total-wrap {
  display: flex;
  align-items: baseline;

  .total-label {
    font-size: 24rpx;
    color: #646A73;
    margin-right: 6rpx;
  }
  .total-price {
    font-size: 32rpx;
    font-weight: 700;
    color: #1F2329;
  }
}

.footer-btns {
  display: flex;
  gap: 16rpx;
}

.btn {
  font-size: 24rpx;
  font-weight: 600;
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 24rpx;
  border-radius: 30rpx;
  border: none;

  &.btn-secondary {
    background: #FFF0EB;
    color: #FF5722;
    border: 1rpx solid #FF5722;
  }

  &.btn-primary {
    background: linear-gradient(135deg, #25B873 0%, #1DA362 100%);
    color: #FFFFFF;
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
  font-weight: 600;
  color: #1D2129;
  margin-bottom: 8rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #86909C;
  margin-bottom: 32rpx;
}

.btn-go-hall {
  background: #FF5722;
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 36rpx;
  padding: 0 48rpx;
}
</style>
