<template>
  <view class="points-page">
    <!-- 顶部积分卡片 -->
    <view class="points-header">
      <view class="ph-top">
        <text class="ph-label">当前可用积分</text>
        <view class="ph-val-row">
          <text class="ph-val">80</text>
          <button class="btn-checkin" @click="doCheckin"><text>{{ hasCheckin ? '今日已签到' : '每日签到 +5' }}</text></button>
        </view>
      </view>
      <view class="ph-bottom">
        <text class="ph-tip">购物下单可得等额积分 · 积分可抵扣现金与兑换好礼</text>
      </view>
    </view>

    <!-- 积分兑好礼列表 -->
    <view class="exchange-section">
      <view class="es-title"><text>积分兑换专区</text></view>
      <view class="goods-grid">
        <view class="goods-card" v-for="item in pointsGoods" :key="item.id">
          <image class="goods-img" :src="item.image" mode="aspectFill" />
          <view class="goods-body">
            <text class="goods-name">{{ item.name }}</text>
            <view class="goods-price-row">
              <text class="goods-points">{{ item.points }} 积分</text>
              <button class="btn-exchange" @click="exchange(item)">兑换</button>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      hasCheckin: false,
      pointsGoods: [
        {
          id: 101,
          name: '¥10 无门槛生鲜立减券',
          points: 100,
          image: '/static/images/png/dish-3.png'
        },
        {
          id: 102,
          name: '新鲜鸡蛋 10枚装 礼盒',
          points: 300,
          image: '/static/images/png/dish-4.png'
        },
        {
          id: 103,
          name: '深海大虾 500g 体验装',
          points: 500,
          image: '/static/images/png/dish-6.png'
        },
        {
          id: 104,
          name: 'Redmi 9A 双旦大额特惠减免券',
          points: 200,
          image: '/static/images/png/dish-2.png'
        }
      ]
    }
  },
  methods: {
    doCheckin() {
      if (this.hasCheckin) return
      this.hasCheckin = true
      uni.showToast({ title: '签到成功 +5 积分', icon: 'success' })
    },
    exchange(item) {
      uni.showModal({
        title: '积分兑换',
        content: `确定消耗 ${item.points} 积分兑换「${item.name}」吗？`,
        success: (res) => {
          if (res.confirm) {
            uni.showToast({ title: '兑换成功，已放入卡券包', icon: 'success' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.points-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 24rpx;
  box-sizing: border-box;
}

.points-header {
  background: linear-gradient(135deg, #25B873 0%, #1ea062 100%);
  color: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(37, 184, 115, 0.2);
}

.ph-label {
  font-size: 24rpx;
  opacity: 0.9;
}

.ph-val-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}

.ph-val {
  font-size: 54rpx;
  font-weight: 800;
  font-family: Arial, Helvetica, sans-serif;
}

.btn-checkin {
  background-color: #FFFFFF;
  color: #25B873;
  font-size: 24rpx;
  font-weight: 700;
  border-radius: 30rpx;
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 24rpx;
  margin: 0;
}

.ph-bottom {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.2);
}

.ph-tip {
  font-size: 20rpx;
  opacity: 0.85;
}

.exchange-section {
  display: flex;
  flex-direction: column;
}

.es-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1F2329;
  margin-bottom: 20rpx;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.goods-card {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.goods-img {
  width: 100%;
  height: 240rpx;
  background-color: #F7F8FA;
}

.goods-body {
  padding: 16rpx;
}

.goods-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #1F2329;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  height: 72rpx;
  margin-bottom: 12rpx;
}

.goods-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.goods-points {
  font-size: 26rpx;
  color: #FF5E1E;
  font-weight: 700;
}

.btn-exchange {
  background-color: #25B873;
  color: #FFFFFF;
  font-size: 22rpx;
  font-weight: 600;
  border-radius: 24rpx;
  height: 48rpx;
  line-height: 48rpx;
  padding: 0 20rpx;
  margin: 0;
}
</style>
