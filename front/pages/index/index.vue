<template>
  <view class="home-page">
    <!-- 顶部生鲜绿一体化头部（适配微信小程序胶囊与状态栏） -->
    <view class="green-header" :style="{ paddingTop: (statusBarHeight + 8) + 'px' }">
      <!-- 搜索行 -->
      <view class="search-row">
        <text class="loc-pin">📍</text>
        <view class="search-input-box" @click="goSearch">
          <text class="search-icon">🔍</text>
          <text class="search-placeholder">搜索商品/店铺/自提点...</text>
        </view>
      </view>

      <!-- 自提点指示条 -->
      <view class="station-strip" @click="goPickup">
        <text class="station-icon">🏪</text>
        <text class="station-text">自提点：{{ pickupPoint.name || '高黎柱发士多店(请自备袋子谢谢)' }}</text>
        <text class="station-arrow">›</text>
      </view>
    </view>

    <!-- 主内容区：使用原生自然流排版，坚决避免嵌套 scroll-view 导致双滚动条/断层衔接感 -->
    <view class="main-content">
      <!-- 轮播海报 Banner（对齐 5.20 礼遇季鲜花与大促卡片） -->
      <view class="banner-section">
        <swiper class="banner-swiper" :autoplay="true" :circular="true" :interval="4500" indicator-dots indicator-active-color="#25B873">
          <swiper-item v-for="(b, idx) in banners" :key="idx">
            <view class="banner-card">
              <view class="bc-left">
                <text class="bc-tag">{{ b.tag }}</text>
                <text class="bc-title">{{ b.title }}</text>
                <text class="bc-desc">{{ b.desc }}</text>
              </view>
              <view class="bc-right">
                <image class="bc-img" :src="b.img" mode="aspectFill" />
              </view>
            </view>
          </swiper-item>
        </swiper>
      </view>

      <!-- 服务承诺三标签 -->
      <view class="guarantee-row">
        <view class="g-cell"><text class="g-check">✓</text><text>原产地直采/正品保证</text></view>
        <view class="g-cell"><text class="g-check">✓</text><text>极速退款</text></view>
        <view class="g-cell"><text class="g-check">✓</text><text>次日自提</text></view>
      </view>

      <!-- 金刚区 5 宫格大图标 -->
      <view class="kingkong-row">
        <view class="kk-item" v-for="k in kingkongs" :key="k.name" @click="onKingkongTap(k)">
          <view class="kk-icon-circle" :style="{ backgroundColor: k.bg }">
            <text class="kk-emoji">{{ k.emoji }}</text>
          </view>
          <text class="kk-name">{{ k.name }}</text>
        </view>
      </view>

      <!-- 截单公告喇叭条 -->
      <view class="notice-strip">
        <text class="notice-icon">📢</text>
        <text class="notice-text">截单提醒：每日23:00截单，次日16:00后到店自提！</text>
      </view>

      <!-- 推荐商品区块标题 -->
      <view class="section-title-row">
        <view class="title-bar"></view>
        <text class="title-text">推荐商品</text>
      </view>

      <!-- 双列商品瀑布流卡片（完全对齐设计图首页，共4款核心代表商品，避免与后续页面商品或次屏混淆） -->
      <view class="goods-waterfall">
        <view
          class="goods-card"
          v-for="item in goodsList"
          :key="item.id"
          @click="goDetail(item)"
        >
          <!-- 封面图 -->
          <view class="gc-img-box">
            <image class="gc-img" :src="item.image" mode="aspectFill" />
            <view class="gc-promo-tag" v-if="item.promoTag"><text>{{ item.promoTag }}</text></view>
          </view>

          <!-- 信息体 -->
          <view class="gc-body">
            <text class="gc-title">{{ item.name }}</text>
            <view class="gc-bottom">
              <view class="gc-price-box">
                <text class="gc-symbol">¥</text>
                <text class="gc-price">{{ item.price }}</text>
              </view>
              <!-- 绿色加购购物车按钮 -->
              <view class="gc-cart-btn" @click.stop="addToCart(item)">
                <text class="cart-plus">+</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部安全留白，避免 TabBar 遮挡 -->
      <view class="tabbar-safe-placeholder"></view>
    </view>

    <!-- 悬浮购物车组件 -->
    <FloatingCart ref="floatingCart" />
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getDishes } from '@/api/index.js'
import FloatingCart from '@/components/FloatingCart.vue'

export default {
  components: {
    FloatingCart
  },
  data() {
    return {
      statusBarHeight: 20,
      banners: [
        {
          tag: '5.20 礼遇季',
          title: '提前预定 享特惠',
          desc: '原产地鲜花直发，新鲜如初，次日自提，甜蜜直达。',
          img: '/static/images/png/dish-3.png'
        },
        {
          tag: '数码好礼',
          title: 'Redmi 9A 双旦直降',
          desc: '超大电量高清大屏，现货到店提货。',
          img: '/static/images/png/dish-2.png'
        }
      ],
      kingkongs: [
        { name: '今日推荐', key: 'today', emoji: '📅', bg: '#FFECE8' },
        { name: '手机数码', key: 'digital', emoji: '📱', bg: '#E8F3FF' },
        { name: '限时特惠', key: 'seckill', emoji: '💖', bg: '#FFEBF3' },
        { name: '生鲜果蔬', key: 'fresh', emoji: '🥦', bg: '#E8F7F0' },
        { name: '拼团专区', key: 'groupon', emoji: '📢', bg: '#FFF6E8' }
      ],
      goodsList: [
        {
          id: 1,
          name: 'Apple/苹果 iPhone 12 Pro Max (128G) 银色/石墨色/金色/海蓝色 支持5G',
          price: 9299,
          promoTag: '',
          image: '/static/images/png/dish-1.png'
        },
        {
          id: 2,
          name: 'Apple/苹果 iPhone 12 (64G/128G) 紫色/白色/蓝色 双卡双待',
          price: 6299,
          promoTag: '双旦特惠',
          image: '/static/images/png/dish-1.png'
        },
        {
          id: 3,
          name: 'Redmi 9A 4GB+64GB 砂石黑 5000mAh大电量长续航',
          price: 499,
          promoTag: '直降¥100',
          image: '/static/images/png/dish-2.png'
        },
        {
          id: 4,
          name: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量 1300万像素',
          price: 599,
          promoTag: '双旦限时直降¥100',
          image: '/static/images/png/dish-2.png'
        }
      ]
    }
  },
  computed: {
    ...mapState(['pickupPoint'])
  },
  created() {
    try {
      const info = uni.getSystemInfoSync()
      if (info && info.statusBarHeight) {
        this.statusBarHeight = info.statusBarHeight
      }
    } catch (e) {}
  },
  methods: {
    goSearch() {
      uni.navigateTo({ url: '/pages/search/search' })
    },
    goPickup() {
      uni.navigateTo({ url: '/pages/pickup/pickup' })
    },
    onKingkongTap(k) {
      uni.switchTab({ url: '/pages/category/category' })
    },
    goDetail(item) {
      uni.navigateTo({ url: `/pages/detail/detail?id=${item.id}` })
    },
    addToCart(item) {
      this.$store.commit('addToCart', item)
      if (this.$refs.floatingCart) {
        this.$refs.floatingCart.animateAdd(1)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  box-sizing: border-box;
}

/* 顶部绿色头部（一体化沉浸式，包含状态栏占位，圆弧过渡） */
.green-header {
  background-color: #25B873;
  padding: 16rpx 24rpx 24rpx;
  border-radius: 0 0 28rpx 28rpx;
  box-sizing: border-box;
}

.search-row {
  display: flex;
  align-items: center;
}

.loc-pin {
  font-size: 34rpx;
  margin-right: 12rpx;
}

.search-input-box {
  flex: 1;
  display: flex;
  align-items: center;
  height: 68rpx;
  background-color: #FFFFFF;
  border-radius: 34rpx;
  padding: 0 24rpx;
  /* 留出微信小程序右侧胶囊按钮安全距离 */
  margin-right: 180rpx;
}

.search-icon {
  font-size: 26rpx;
  color: #86909C;
  margin-right: 12rpx;
}

.search-placeholder {
  font-size: 26rpx;
  color: #86909C;
}

.station-strip {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  color: #FFFFFF;
  font-size: 24rpx;
}

.station-icon {
  margin-right: 8rpx;
  font-size: 26rpx;
}

.station-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-arrow {
  font-size: 28rpx;
}

/* 主内容：原生自然排版，不使用嵌套 scroll-view */
.main-content {
  padding: 0 24rpx;
  box-sizing: border-box;
}

/* Banner */
.banner-section {
  margin-top: -10rpx;
  margin-bottom: 16rpx;
}

.banner-swiper {
  height: 240rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.banner-card {
  height: 100%;
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.bc-left {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.bc-tag {
  align-self: flex-start;
  font-size: 20rpx;
  color: #25B873;
  background-color: #E8F7F0;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.bc-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #1F2329;
  margin-bottom: 6rpx;
}

.bc-desc {
  font-size: 20rpx;
  color: #86909C;
  line-height: 28rpx;
}

.bc-right {
  width: 160rpx;
  height: 160rpx;
  margin-left: 20rpx;
}

.bc-img {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
}

/* 服务承诺 */
.guarantee-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background-color: #FFFFFF;
  border-radius: 12rpx;
  padding: 16rpx 0;
  margin-bottom: 20rpx;
}

.g-cell {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: #25B873;
  font-weight: 500;
}

.g-check {
  margin-right: 6rpx;
  font-weight: 700;
}

/* 金刚区 */
.kingkong-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #FFFFFF;
  border-radius: 20rpx;
  padding: 24rpx 16rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.kk-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.kk-icon-circle {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8rpx;
}

.kk-emoji {
  font-size: 44rpx;
}

.kk-name {
  font-size: 22rpx;
  color: #1F2329;
  font-weight: 500;
}

/* 公告 */
.notice-strip {
  display: flex;
  align-items: center;
  background-color: #E8F7F0;
  border-radius: 10rpx;
  padding: 14rpx 20rpx;
  margin-bottom: 20rpx;
  font-size: 22rpx;
  color: #25B873;
}

.notice-icon {
  margin-right: 12rpx;
  font-size: 26rpx;
}

.notice-text {
  flex: 1;
  font-weight: 500;
}

/* 标题 */
.section-title-row {
  display: flex;
  align-items: center;
  margin: 16rpx 0;
}

.title-bar {
  width: 8rpx;
  height: 32rpx;
  background-color: #25B873;
  border-radius: 4rpx;
  margin-right: 12rpx;
}

.title-text {
  font-size: 30rpx;
  font-weight: 800;
  color: #1F2329;
}

/* 瀑布流 */
.goods-waterfall {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.goods-card {
  background-color: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.gc-img-box {
  width: 100%;
  height: 320rpx;
  background-color: #F7F8FA;
  position: relative;
}

.gc-img {
  width: 100%;
  height: 100%;
}

.gc-promo-tag {
  position: absolute;
  left: 12rpx;
  top: 12rpx;
  background: linear-gradient(90deg, #7C3AED 0%, #6D28D9 100%);
  color: #FFFFFF;
  font-size: 18rpx;
  font-weight: 700;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.gc-body {
  padding: 16rpx;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.gc-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1F2329;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  height: 72rpx;
}

.gc-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}

.gc-price-box {
  display: flex;
  align-items: baseline;
}

.gc-symbol {
  font-size: 22rpx;
  color: #25B873;
  font-weight: 700;
}

.gc-price {
  font-size: 34rpx;
  color: #25B873;
  font-weight: 800;
  font-family: Arial, Helvetica, sans-serif;
}

.gc-cart-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background-color: #25B873;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(37, 184, 115, 0.35);
}

.cart-plus {
  color: #FFFFFF;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1;
  margin-top: -2rpx;
}

/* 底部防 TabBar 遮挡留白 */
.tabbar-safe-placeholder {
  height: 140rpx;
}
</style>