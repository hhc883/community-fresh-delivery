<template>
  <view class="cat-page">
    <!-- 顶部白色搜索条 -->
    <view class="top-search-wrap">
      <view class="search-pill" @click="goSearch">
        <text class="search-icon">🔍</text>
        <text class="search-text">搜索商品/自提点/店铺...</text>
      </view>
    </view>

    <!-- 顶部 5 列分类快捷圆形图标栏 -->
    <view class="top-icon-strip">
      <view class="tis-item" v-for="t in topShortcuts" :key="t.name" @click="selectTopShortcut(t)">
        <view class="tis-circle" :style="{ backgroundColor: t.bg }">
          <text class="tis-emoji">{{ t.emoji }}</text>
        </view>
        <text class="tis-name" :class="{ active: currentTopKey === t.key }">{{ t.name }}</text>
      </view>
      <view class="tis-expand" @click="toggleExpand">
        <text class="tis-expand-text">分类</text>
        <text class="tis-expand-icon">∨</text>
      </view>
    </view>

    <!-- 主体左右分栏 -->
    <view class="cat-main">
      <!-- 左侧分类侧栏 -->
      <scroll-view class="cat-sidebar" scroll-y>
        <view
          class="sidebar-item"
          v-for="(cat, idx) in categories"
          :key="cat.id"
          :class="{ active: activeCatIndex === idx }"
          @click="activeCatIndex = idx"
        >
          <view class="active-indicator" v-if="activeCatIndex === idx"></view>
          <text class="sidebar-text">{{ cat.name }}</text>
        </view>
      </scroll-view>

      <!-- 右侧商品区 -->
      <view class="cat-content">
        <!-- 排序条（综合/销量/新品/价格） -->
        <view class="sort-header">
          <text class="sh-tab" :class="{ active: sortBy === 'default' }" @click="setSort('default')">综合</text>
          <text class="sh-tab" :class="{ active: sortBy === 'sales' }" @click="setSort('sales')">销量</text>
          <text class="sh-tab" :class="{ active: sortBy === 'new' }" @click="setSort('new')">新品</text>
          <text class="sh-tab" :class="{ active: sortBy === 'price' }" @click="togglePriceSort">价格 {{ priceAsc ? '↑' : '↓' }}</text>
        </view>

        <!-- 商品流 -->
        <scroll-view class="goods-scroll" scroll-y>
          <view class="goods-list">
            <view
              class="goods-row"
              v-for="item in currentCategoryGoods"
              :key="item.id"
              @click="goDetail(item)"
            >
              <image class="goods-thumb" :src="item.image" mode="aspectFill" />
              <view class="goods-meta">
                <text class="goods-title">{{ item.name }}</text>
                <text class="goods-spec">{{ item.spec }}</text>
                <view class="goods-price-row">
                  <view class="price-val-box">
                    <text class="price-symbol">¥</text>
                    <text class="price-num">{{ item.price }}</text>
                  </view>
                  <!-- 绿色购物车小按钮 -->
                  <view class="cart-btn" @click.stop="addToCart(item)">
                    <text class="cart-icon">🛒</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 悬浮购物车组件 -->
    <FloatingCart ref="floatingCart" />
  </view>
</template>

<script>
import FloatingCart from '@/components/FloatingCart.vue'

export default {
  components: {
    FloatingCart
  },
  data() {
    return {
      currentTopKey: 'all',
      activeCatIndex: 0,
      sortBy: 'default',
      priceAsc: true,
      topShortcuts: [
        { name: '全部', key: 'all', emoji: '🍎', bg: '#FFECE8' },
        { name: '今日特惠', key: 'discount', emoji: '📱', bg: '#E8F3FF' },
        { name: '限时秒杀', key: 'seckill', emoji: '💖', bg: '#FFEBF3' },
        { name: '满减促销', key: 'promo', emoji: '🥩', bg: '#E8F7F0' },
        { name: '拼团抽奖', key: 'lottery', emoji: '📢', bg: '#FFF6E8' }
      ],
      categories: [
        { id: 1, name: '推荐' },
        { id: 2, name: '手机数码' },
        { id: 3, name: '电脑办公' },
        { id: 4, name: '家用电器' },
        { id: 5, name: '生鲜果蔬' },
        { id: 6, name: '食品饮料' },
        { id: 7, name: '母婴玩具' },
        { id: 8, name: '美妆个护' }
      ],
      allGoods: [
        {
          id: 1,
          catId: 1,
          name: 'Apple/苹果 iPhone 12 Pro Max (128G) 银色/石墨色/金色/海蓝色 支持5G',
          spec: '原装国行正品 全网通',
          price: 9299,
          image: '/static/images/png/dish-1.png'
        },
        {
          id: 7,
          catId: 1,
          name: 'Redmi 9 4GB+64GB 碳素黑 全高清大屏 5020mAh大电量',
          spec: '碳素黑 4+64G',
          price: 799,
          image: '/static/images/png/dish-2.png'
        },
        {
          id: 4,
          catId: 1,
          name: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量 1300万高清相机',
          spec: '晴空翠 4+128G',
          price: 599,
          image: '/static/images/png/dish-2.png'
        },
        {
          id: 8,
          catId: 1,
          name: 'Redmi 9 4GB+128GB 霓虹蓝 后置全场景AI四摄',
          spec: '霓虹蓝 4+128G',
          price: 799,
          image: '/static/images/png/dish-2.png'
        },
        {
          id: 2,
          catId: 1,
          name: 'Apple/苹果 iPhone 12 (64G/128G) 紫色/白色/蓝色 双卡双待',
          spec: '双卡双待 现货速提',
          price: 6299,
          image: '/static/images/png/dish-1.png'
        }
      ]
    }
  },
  computed: {
    currentCategoryGoods() {
      let list = [...this.allGoods]
      if (this.sortBy === 'sales') {
        list.reverse()
      } else if (this.sortBy === 'price') {
        list.sort((a, b) => this.priceAsc ? a.price - b.price : b.price - a.price)
      }
      return list
    }
  },
  methods: {
    goSearch() {
      uni.navigateTo({ url: '/pages/search/search' })
    },
    selectTopShortcut(t) {
      this.currentTopKey = t.key
    },
    toggleExpand() {
      uni.showToast({ title: '全部分类已展开', icon: 'none' })
    },
    setSort(type) {
      this.sortBy = type
    },
    togglePriceSort() {
      this.sortBy = 'price'
      this.priceAsc = !this.priceAsc
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
.cat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #FFFFFF;
}

.top-search-wrap {
  padding: 16rpx 24rpx;
  background-color: #FFFFFF;
}

.search-pill {
  display: flex;
  align-items: center;
  height: 68rpx;
  background-color: #F7F8FA;
  border-radius: 34rpx;
  padding: 0 24rpx;
}

.search-icon {
  font-size: 26rpx;
  color: #86909C;
  margin-right: 12rpx;
}

.search-text {
  font-size: 26rpx;
  color: #86909C;
}

/* 顶部圆形快捷入口栏 */
.top-icon-strip {
  display: flex;
  align-items: center;
  padding: 12rpx 20rpx 16rpx;
  border-bottom: 1rpx solid #F2F4F7;
}

.tis-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.tis-circle {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6rpx;
}

.tis-emoji {
  font-size: 36rpx;
}

.tis-name {
  font-size: 20rpx;
  color: #4E5969;
}

.tis-name.active {
  color: #25B873;
  font-weight: 700;
}

.tis-expand {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 10rpx;
  border-left: 1rpx solid #F2F4F7;
}

.tis-expand-text {
  font-size: 20rpx;
  color: #86909C;
}

.tis-expand-icon {
  font-size: 20rpx;
  color: #86909C;
}

/* 主体 */
.cat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧栏 */
.cat-sidebar {
  width: 170rpx;
  height: 100%;
  background-color: #F7F8FA;
}

.sidebar-item {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-size: 26rpx;
  color: #4E5969;
}

.sidebar-item.active {
  background-color: #FFFFFF;
  color: #25B873;
  font-weight: 700;
}

.active-indicator {
  position: absolute;
  left: 0;
  top: 26rpx;
  bottom: 26rpx;
  width: 6rpx;
  background-color: #25B873;
  border-radius: 0 4rpx 4rpx 0;
}

/* 右侧内容 */
.cat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sort-header {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 72rpx;
  border-bottom: 1rpx solid #F2F4F7;
  font-size: 24rpx;
  color: #86909C;
}

.sh-tab.active {
  color: #25B873;
  font-weight: 700;
}

.goods-scroll {
  flex: 1;
  height: 0;
}

.goods-list {
  padding: 16rpx 20rpx 40rpx;
}

.goods-row {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F7F8FA;
}

.goods-thumb {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
  background-color: #F7F8FA;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.goods-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1F2329;
  line-height: 36rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.goods-spec {
  font-size: 20rpx;
  color: #86909C;
  margin-top: 4rpx;
}

.goods-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}

.price-val-box {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 22rpx;
  color: #25B873;
  font-weight: 700;
}

.price-num {
  font-size: 32rpx;
  color: #25B873;
  font-weight: 800;
  font-family: Arial, Helvetica, sans-serif;
}

.cart-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-icon {
  font-size: 36rpx;
  color: #25B873;
}
</style>
