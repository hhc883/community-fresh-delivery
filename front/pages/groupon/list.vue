<template>
  <view class="groupon-page">
    <!-- 顶部拼团 Banner -->
    <view class="header-banner">
      <view class="banner-content">
        <text class="banner-title">🥬 社区生鲜拼团</text>
        <text class="banner-subtitle">邻里一起拼 · 低至5折 · 次日新鲜自提</text>
      </view>
      <view class="my-groupon-pill" @tap="goMyGroupon">
        <text class="pill-text">我的拼团</text>
        <text class="pill-arrow">›</text>
      </view>
    </view>

    <!-- 搜索栏 -->
    <view class="search-box">
      <view class="search-inner">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索拼团生鲜好物"
          placeholder-class="placeholder"
          @confirm="handleSearch"
        />
        <text class="clear-btn" v-if="keyword" @tap="keyword = ''; handleSearch()">×</text>
      </view>
      <button class="search-action-btn" @tap="handleSearch">搜索</button>
    </view>

    <!-- 商品列表 -->
    <view class="list-container" v-if="dishList.length > 0">
      <view class="groupon-card" v-for="item in filteredList" :key="item.id">
        <image
          class="card-img"
          :src="item.image || '/static/images/png/dish-1.png'"
          mode="aspectFill"
          @tap="goDetail(item)"
        />
        <view class="card-body">
          <text class="goods-title" @tap="goDetail(item)">{{ item.name }}</text>
          
          <view class="tags-row">
            <text class="tag tag-spec" v-if="item.unit">{{ item.unit }}</text>
            <text class="tag tag-origin" v-if="item.origin">产地: {{ item.origin }}</text>
            <text class="tag tag-fresh" v-if="item.isFresh === 1">生鲜保鲜</text>
            <text class="tag tag-stock" v-if="item.stock != null">仅剩{{ item.stock }}件</text>
          </view>

          <view class="price-action-row">
            <view class="price-box">
              <view class="groupon-badge">{{ item.grouponSize || 2 }}人团</view>
              <view class="main-price">
                <text class="symbol">¥</text>
                <text class="val">{{ item.grouponPrice || item.price }}</text>
              </view>
              <text class="orig-price">¥{{ item.price }}</text>
            </view>
            <view class="action-btns">
              <button class="btn btn-open" @tap="handleOpen(item)">开团</button>
              <button class="btn btn-join" @tap="goDetail(item)">去参团</button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else-if="!loading">
      <image class="empty-img" src="/static/images/dish-2.svg" mode="aspectFit" />
      <text class="empty-text">当前暂无拼团商品活动</text>
      <text class="empty-sub">去首页看看其他生鲜优选吧</text>
      <button class="btn-go-home" @tap="goHome">去商城首页</button>
    </view>
  </view>
</template>

<script>
import { getGrouponDishList } from '../../api/index.js'

export default {
  data() {
    return {
      keyword: '',
      loading: true,
      dishList: []
    }
  },
  computed: {
    filteredList() {
      if (!this.keyword || !this.keyword.trim()) return this.dishList
      const kw = this.keyword.trim().toLowerCase()
      return this.dishList.filter(d => (d.name && d.name.toLowerCase().includes(kw)))
    }
  },
  onShow() {
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await getGrouponDishList()
        if (res && res.data) {
          const raw = Array.isArray(res.data) ? res.data : (res.data.records || [])
          this.dishList = raw.map(d => {
            let img = d.image
            if (img && !img.startsWith('http') && !img.startsWith('/static')) {
              img = `/static/images/png/dish-${(d.id % 28) + 1}.png`
            }
            return {
              ...d,
              image: img || '/static/images/png/dish-1.png'
            }
          })
        }
      } catch (err) {
        uni.showToast({ title: err.message || '加载拼团商品失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      // 触发计算属性过滤
    },
    goDetail(item) {
      uni.navigateTo({ url: `/pages/groupon/detail?dishId=${item.id}` })
    },
    handleOpen(item) {
      // 直接发起开团确认页
      uni.navigateTo({
        url: `/pages/groupon/confirm?mode=open&dishId=${item.id}&price=${item.grouponPrice || item.price}&name=${encodeURIComponent(item.name)}&image=${encodeURIComponent(item.image)}`
      })
    },
    goMyGroupon() {
      uni.navigateTo({ url: '/pages/groupon/my' })
    },
    goHome() {
      uni.switchTab({ url: '/pages/index/index' })
    }
  }
}
</script>

<style lang="scss" scoped>
.groupon-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 60rpx;
}

.header-banner {
  background: linear-gradient(135deg, #FF6B35 0%, #FF8F3D 100%);
  padding: 40rpx 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #FFFFFF;
}

.banner-content {
  display: flex;
  flex-direction: column;
}

.banner-title {
  font-size: 38rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.banner-subtitle {
  font-size: 24rpx;
  opacity: 0.9;
}

.my-groupon-pill {
  background: rgba(255, 255, 255, 0.25);
  border: 1rpx solid rgba(255, 255, 255, 0.5);
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.pill-text {
  font-size: 26rpx;
  font-weight: 500;
}

.pill-arrow {
  font-size: 28rpx;
}

.search-box {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #FFFFFF;
  gap: 16rpx;
}

.search-inner {
  flex: 1;
  height: 72rpx;
  background-color: #F2F3F5;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #1D2129;
}

.clear-btn {
  font-size: 32rpx;
  color: #86909C;
  padding: 0 8rpx;
}

.search-action-btn {
  background: #FF6B35;
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 600;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  padding: 0 32rpx;
  border: none;
}

.list-container {
  padding: 20rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.groupon-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  display: flex;
  padding: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
}

.card-img {
  width: 220rpx;
  height: 220rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
  background: #F2F3F5;
}

.card-body {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1F2329;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin: 10rpx 0;
}

.tag {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.tag-spec {
  background: #F2F3F5;
  color: #4E5969;
}

.tag-origin {
  background: #E8F3FF;
  color: #165DFF;
}

.tag-fresh {
  background: #E8F8F0;
  color: #25B873;
}

.tag-stock {
  background: #FFF7E8;
  color: #FF7D00;
}

.price-action-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.price-box {
  display: flex;
  flex-direction: column;
}

.groupon-badge {
  display: inline-block;
  background: #FFE8E0;
  color: #FF5722;
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  margin-bottom: 4rpx;
  width: fit-content;
}

.main-price {
  display: flex;
  align-items: baseline;
  color: #FF5722;

  .symbol {
    font-size: 24rpx;
    font-weight: 700;
  }
  .val {
    font-size: 38rpx;
    font-weight: 800;
  }
}

.orig-price {
  font-size: 22rpx;
  color: #86909C;
  text-decoration: line-through;
}

.action-btns {
  display: flex;
  gap: 12rpx;
}

.btn {
  font-size: 24rpx;
  font-weight: 600;
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 24rpx;
  border-radius: 30rpx;
  border: none;

  &.btn-open {
    background: #FFF0EB;
    color: #FF5722;
    border: 1rpx solid #FF5722;
  }

  &.btn-join {
    background: linear-gradient(135deg, #FF6B35 0%, #FF8F3D 100%);
    color: #FFFFFF;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 140rpx;
}

.empty-img {
  width: 220rpx;
  height: 220rpx;
  opacity: 0.6;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #1D2129;
  margin-bottom: 8rpx;
}

.empty-sub {
  font-size: 26rpx;
  color: #86909C;
  margin-bottom: 32rpx;
}

.btn-go-home {
  background: #25B873;
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 36rpx;
  padding: 0 48rpx;
}
</style>
