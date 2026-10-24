<template>
  <view class="detail-page">
    <view v-if="loading" class="loading-state">
      <text>加载拼团详情中...</text>
    </view>

    <view v-else-if="dish" class="content">
      <!-- 顶部大图 -->
      <image class="banner-img" :src="dish.image" mode="aspectFill" />

      <!-- 核心价格卡片 -->
      <view class="price-card">
        <view class="price-row">
          <view class="groupon-price-wrap">
            <text class="symbol">¥</text>
            <text class="price-val">{{ dish.grouponPrice || dish.price }}</text>
            <text class="badge">{{ dish.grouponSize || 2 }}人拼团价</text>
          </view>
          <text class="orig-price">单买价 ¥{{ dish.price }}</text>
        </view>
        <view class="title-row">
          <text class="goods-title">{{ dish.name }}</text>
        </view>
        <view class="meta-row">
          <text class="meta-tag" v-if="dish.unit">规格: {{ dish.unit }}</text>
          <text class="meta-tag" v-if="dish.origin">产地: {{ dish.origin }}</text>
          <text class="meta-tag" v-if="dish.isFresh === 1">生鲜直达</text>
          <text class="meta-tag" v-if="dish.stock != null">库存: {{ dish.stock }}件</text>
        </view>
      </view>

      <!-- 正在开拼的团列表 (核心功能) -->
      <view class="active-groupons-card" v-if="joinableList.length > 0">
        <view class="card-header">
          <text class="ch-title">{{ joinableList.length }}人正在拼团，可直接参团</text>
          <text class="ch-sub">拼满即成团</text>
        </view>
        <view class="groupon-item" v-for="g in joinableList" :key="g.id">
          <view class="user-info">
            <view class="avatar-circle">
              <text class="avatar-char">{{ (g.leaderName || '邻居')[0] }}</text>
            </view>
            <text class="leader-name">{{ g.leaderName || '邻里达人' }}</text>
          </view>
          <view class="right-box">
            <view class="lack-info">
              <view class="lack-text">
                还差 <text class="highlight">{{ g.lackSize || 1 }}人</text> 成团
              </view>
              <view class="countdown-text" v-if="g.countdownText">
                剩余 {{ g.countdownText }}
              </view>
            </view>
            <button class="join-btn" @tap="handleJoin(g)">去参团</button>
          </view>
        </view>
      </view>

      <!-- 拼团玩法说明 -->
      <view class="rule-card">
        <view class="rule-header">
          <text class="rule-title">拼团玩法</text>
          <text class="rule-tip">未能成团全额退款</text>
        </view>
        <view class="steps-row">
          <view class="step-item">
            <text class="step-num">1</text>
            <text class="step-text">开团/参团</text>
          </view>
          <text class="step-arrow">›</text>
          <view class="step-item">
            <text class="step-num">2</text>
            <text class="step-text">邀请好友拼单</text>
          </view>
          <text class="step-arrow">›</text>
          <view class="step-item">
            <text class="step-num">3</text>
            <text class="step-text">满员极速发货</text>
          </view>
          <text class="step-arrow">›</text>
          <view class="step-item">
            <text class="step-num">4</text>
            <text class="step-text">次日自提/送达</text>
          </view>
        </view>
      </view>

      <!-- 商品图文描述 -->
      <view class="desc-card">
        <view class="desc-title">商品详情</view>
        <view class="desc-content">
          <text class="desc-text">{{ dish.description || '精选优质生鲜，基地直采，全程冷链，保证新鲜品质。' }}</text>
        </view>
      </view>

      <!-- 底部固定操作栏 -->
      <view class="bottom-action-bar">
        <view class="action-home" @tap="goHome">
          <text class="home-icon">🏠</text>
          <text class="home-text">首页</text>
        </view>
        <view class="action-groupons" @tap="goMyGroupon">
          <text class="my-icon">🛒</text>
          <text class="my-text">我的团</text>
        </view>
        <view class="action-btns">
          <button class="btn btn-single" @tap="handleSingleBuy">
            <view class="btn-price">¥{{ dish.price }}</view>
            <view class="btn-label">单独购买</view>
          </button>
          <button class="btn btn-open" @tap="handleOpen">
            <view class="btn-price">¥{{ dish.grouponPrice || dish.price }}</view>
            <view class="btn-label">我要开团</view>
          </button>
        </view>
      </view>
    </view>

    <!-- 错误状态 -->
    <view v-else class="empty-state">
      <text class="empty-text">商品不存在或拼团已结束</text>
      <button class="btn-back" @tap="goList">返回团购专区</button>
    </view>
  </view>
</template>

<script>
import { getGrouponDetail, getGrouponDishList, getDishDetail } from '../../api/index.js'

export default {
  data() {
    return {
      dishId: null,
      loading: true,
      dish: null,
      joinableList: [],
      timer: null
    }
  },
  onLoad(options) {
    if (options && options.dishId) {
      this.dishId = options.dishId
      this.loadDetail(options.dishId)
    }
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    async loadDetail(dishId) {
      this.loading = true
      try {
        // 并行加载商品信息与正在开拼的团
        const [dishRes, groupRes] = await Promise.allSettled([
          getDishDetail(dishId),
          getGrouponDetail(dishId)
        ])

        if (dishRes.status === 'fulfilled' && dishRes.value && dishRes.value.data) {
          const d = dishRes.value.data
          let img = d.image
          if (img && !img.startsWith('http') && !img.startsWith('/static')) {
            img = `/static/images/png/dish-${(dishId % 28) + 1}.png`
          }
          this.dish = {
            ...d,
            image: img || '/static/images/png/dish-1.png',
            grouponPrice: d.grouponPrice || (d.price ? (d.price * 0.8).toFixed(2) : '19.90'),
            grouponSize: d.grouponSize || 2
          }
        } else {
          // 兜底加载拼团列表查找
          const listRes = await getGrouponDishList()
          if (listRes && listRes.data) {
            const list = Array.isArray(listRes.data) ? listRes.data : (listRes.data.records || [])
            const found = list.find(item => String(item.id) === String(dishId))
            if (found) {
              this.dish = found
            }
          }
        }

        if (groupRes.status === 'fulfilled' && groupRes.value && groupRes.value.data) {
          const groups = Array.isArray(groupRes.value.data) ? groupRes.value.data : [groupRes.value.data]
          this.joinableList = groups.filter(g => g && g.status === 1)
          this.startCountdown()
        }
      } catch (err) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    startCountdown() {
      if (this.timer) clearInterval(this.timer)
      this.timer = setInterval(() => {
        const now = Date.now()
        this.joinableList.forEach(g => {
          if (!g.expireTime) {
            g.countdownText = '23:59:59'
            return
          }
          const end = new Date(g.expireTime).getTime()
          const diff = Math.max(0, Math.floor((end - now) / 1000))
          if (diff <= 0) {
            g.countdownText = '已截止'
            return
          }
          const h = String(Math.floor(diff / 3600)).padStart(2, '0')
          const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0')
          const s = String(diff % 60).padStart(2, '0')
          g.countdownText = `${h}:${m}:${s}`
        })
      }, 1000)
    },
    handleSingleBuy() {
      // 普通单买，进入普通确认页
      uni.setStorageSync('checkout_items', [{
        id: this.dish.id,
        dishId: this.dish.id,
        name: this.dish.name,
        price: Number(this.dish.price),
        quantity: 1,
        image: this.dish.image,
        flavor: this.dish.unit || '标准规格'
      }])
      uni.navigateTo({ url: '/pages/order/confirm' })
    },
    handleOpen() {
      uni.navigateTo({
        url: `/pages/groupon/confirm?mode=open&dishId=${this.dish.id}&price=${this.dish.grouponPrice || this.dish.price}&name=${encodeURIComponent(this.dish.name)}&image=${encodeURIComponent(this.dish.image)}`
      })
    },
    handleJoin(groupon) {
      uni.navigateTo({
        url: `/pages/groupon/confirm?mode=join&grouponId=${groupon.id}&dishId=${this.dish.id}&price=${groupon.grouponPrice || this.dish.grouponPrice || this.dish.price}&name=${encodeURIComponent(this.dish.name)}&image=${encodeURIComponent(this.dish.image)}`
      })
    },
    goHome() {
      uni.switchTab({ url: '/pages/index/index' })
    },
    goMyGroupon() {
      uni.navigateTo({ url: '/pages/groupon/my' })
    },
    goList() {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 140rpx;
}

.banner-img {
  width: 100%;
  height: 600rpx;
  background-color: #EAEBED;
}

.price-card {
  background: #FFFFFF;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.groupon-price-wrap {
  display: flex;
  align-items: baseline;
  color: #FF5722;

  .symbol {
    font-size: 32rpx;
    font-weight: 700;
  }
  .price-val {
    font-size: 54rpx;
    font-weight: 800;
    margin-right: 12rpx;
  }
  .badge {
    background: #FFE8E0;
    color: #FF5722;
    font-size: 22rpx;
    font-weight: 600;
    padding: 4rpx 12rpx;
    border-radius: 8rpx;
  }
}

.orig-price {
  font-size: 26rpx;
  color: #86909C;
  text-decoration: line-through;
}

.goods-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1F2329;
  line-height: 1.4;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.meta-tag {
  background: #F2F3F5;
  color: #4E5969;
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
}

.active-groupons-card {
  background: #FFFFFF;
  padding: 24rpx 30rpx;
  margin-bottom: 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #F0F2F5;

  .ch-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1F2329;
  }
  .ch-sub {
    font-size: 24rpx;
    color: #86909C;
  }
}

.groupon-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F8F9FA;

  &:last-child {
    border-bottom: none;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar-circle {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #FF6B35;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
}

.leader-name {
  font-size: 28rpx;
  color: #1D2129;
}

.right-box {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.lack-info {
  text-align: right;

  .lack-text {
    font-size: 24rpx;
    color: #1D2129;

    .highlight {
      color: #FF5722;
      font-weight: 600;
    }
  }

  .countdown-text {
    font-size: 20rpx;
    color: #86909C;
  }
}

.join-btn {
  background: #FF5722;
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 600;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 28rpx;
  border-radius: 28rpx;
  border: none;
}

.rule-card {
  background: #FFFFFF;
  padding: 24rpx 30rpx;
  margin-bottom: 20rpx;
}

.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;

  .rule-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1F2329;
  }
  .rule-tip {
    font-size: 24rpx;
    color: #25B873;
  }
}

.steps-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}

.step-num {
  width: 36rpx;
  height: 36rpx;
  background: #E8F8F0;
  color: #25B873;
  border-radius: 50%;
  font-size: 22rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-text {
  font-size: 22rpx;
  color: #646A73;
}

.step-arrow {
  color: #C9CDD4;
  font-size: 24rpx;
}

.desc-card {
  background: #FFFFFF;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .desc-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1F2329;
    margin-bottom: 14rpx;
  }
  .desc-text {
    font-size: 26rpx;
    color: #4E5969;
    line-height: 1.6;
  }
}

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: #FFFFFF;
  display: flex;
  align-items: center;
  padding: 0 24rpx env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}

.action-home, .action-groupons {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 20rpx;

  .home-icon, .my-icon {
    font-size: 36rpx;
  }
  .home-text, .my-text {
    font-size: 20rpx;
    color: #646A73;
    margin-top: 2rpx;
  }
}

.action-btns {
  flex: 1;
  display: flex;
  height: 80rpx;
  margin-left: 16rpx;
  border-radius: 40rpx;
  overflow: hidden;
}

.btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 0;

  .btn-price {
    font-size: 26rpx;
    font-weight: 700;
    line-height: 1.2;
  }
  .btn-label {
    font-size: 20rpx;
    line-height: 1.2;
  }

  &.btn-single {
    background: #FFF0EB;
    color: #FF5722;
  }

  &.btn-open {
    background: linear-gradient(135deg, #FF6B35 0%, #FF8F3D 100%);
    color: #FFFFFF;
  }
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
  font-size: 28rpx;
  color: #86909C;
}

.btn-back {
  margin-top: 24rpx;
  background: #25B873;
  color: #FFFFFF;
  font-size: 26rpx;
  border-radius: 30rpx;
}
</style>
