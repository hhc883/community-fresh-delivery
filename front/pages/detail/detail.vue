<template>
  <view class="detail-page">
    <scroll-view class="detail-scroll" scroll-y>
      <!-- 商品大图轮播 -->
      <view class="swiper-wrap">
        <swiper class="goods-swiper" :circular="true" @change="onSwiperChange">
          <swiper-item v-for="(img, idx) in swiperImages" :key="idx">
            <image class="swiper-img" :src="img" mode="aspectFill" />
          </swiper-item>
        </swiper>
        <view class="swiper-indicator">
          <text>{{ currentSwiper + 1 }}/{{ swiperImages.length }}</text>
        </view>
      </view>

      <!-- 双色渐变活动价格条 (紫-蓝) -->
      <view class="promo-banner">
        <view class="pb-left">
          <text class="pb-tag">活动价</text>
          <view class="pb-price-row">
            <text class="pb-symbol">¥</text>
            <text class="pb-price">{{ currentPrice }}</text>
          </view>
        </view>
        <view class="pb-right">
          <text class="pb-title">双旦限时直降¥100</text>
          <text class="pb-sub">限时疯抢 · 数量有限</text>
        </view>
      </view>

      <!-- 标题与原价销量 -->
      <view class="goods-main-card">
        <text class="goods-title">{{ goods.name }}</text>
        <view class="price-sales-row">
          <text class="orig-price">¥{{ goods.originalPrice }}</text>
          <text class="sales-text">已售 {{ goods.soldCount || '1500+' }} 件</text>
        </view>
      </view>

      <!-- 规格、配送与自提卡片 -->
      <view class="section-card">
        <!-- 已选规格行（带行内加减步进器） -->
        <view class="spec-row" @click="openSkuModal('select')">
          <view class="sr-left">
            <text class="sr-label">已选</text>
            <text class="sr-val">{{ selectedSpecSummary }}</text>
          </view>
          <!-- 行内数量步进器 -->
          <view class="inline-stepper" @click.stop>
            <view class="step-btn minus" :class="{ disabled: qty <= 1 }" @click="decreaseQty">
              <text class="btn-symbol">-</text>
            </view>
            <text class="step-num">{{ qty }}</text>
            <view class="step-btn plus" @click="increaseQty">
              <text class="btn-symbol">+</text>
            </view>
          </view>
        </view>

        <view class="divider"></view>

        <!-- 配送服务行 -->
        <view class="service-row">
          <text class="sr-label">配送</text>
          <text class="service-text">免运费 · 次日到店自提</text>
        </view>

        <view class="divider"></view>

        <!-- 自提网点行 -->
        <view class="station-row" @click="goPickup">
          <text class="sr-label">自提网点</text>
          <view class="station-val">
            <text class="station-name">{{ pickupPoint.name || '高黎柱发士多店' }}</text>
            <text class="arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 图文详情分割线 -->
      <view class="detail-divider">
        <view class="line"></view>
        <text class="detail-title">图文详情</text>
        <view class="line"></view>
      </view>

      <!-- 图文内容展示 -->
      <view class="detail-content">
        <view class="desc-box">
          <text class="desc-tag">品质承诺</text>
          <text class="desc-text">严格遵循生鲜社区直采与数码正品检验标准，全程品质溯源，冷链恒温存储，次日准时自提。</text>
        </view>
        <image
          class="detail-img"
          v-for="(img, idx) in swiperImages"
          :key="'desc-' + idx"
          :src="img"
          mode="widthFix"
        />
      </view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="bottom-action-bar">
      <view class="left-icons">
        <view class="icon-btn" @click="goService">
          <text class="ib-icon">🎧</text>
          <text class="ib-label">客服</text>
        </view>
        <view class="icon-btn" @click="goCart">
          <view class="cart-badge-wrap">
            <text class="ib-icon">🛒</text>
            <text class="cart-badge" v-if="cartCount > 0">{{ cartCount }}</text>
          </view>
          <text class="ib-label">购物车</text>
        </view>
      </view>

      <view class="right-buttons">
        <!-- 绿色加入购物车 -->
        <button class="btn-cart-green" @click="onAddToCart">
          加入购物车
        </button>
        <!-- 蓝色立即购买 -->
        <button class="btn-buy-blue" @click="onBuyNow">
          立即购买
        </button>
      </view>
    </view>

    <!-- SKU 选择弹窗 (点击已选规格展开更多选项) -->
    <view class="sku-modal-mask" v-if="showSku" @click="showSku = false">
      <view class="sku-modal-content" @click.stop>
        <view class="sku-top">
          <image class="sku-img" :src="swiperImages[0]" mode="aspectFill" />
          <view class="sku-info">
            <view class="sku-price">
              <text class="sku-symbol">¥</text>
              <text class="sku-val">{{ currentPrice }}</text>
            </view>
            <text class="sku-stock">库存 999 件</text>
            <text class="sku-tip">已选：{{ selectedSpecSummary }}</text>
          </view>
          <text class="sku-close" @click="showSku = false">×</text>
        </view>

        <!-- 规格维度一：颜色 -->
        <view class="spec-section">
          <text class="spec-sec-title">颜色</text>
          <view class="spec-chips">
            <view
              class="spec-chip"
              v-for="c in colors"
              :key="c"
              :class="{ active: selectedColor === c }"
              @click="selectedColor = c"
            >
              <text>{{ c }}</text>
            </view>
          </view>
        </view>

        <!-- 规格维度二：版本 -->
        <view class="spec-section">
          <text class="spec-sec-title">规格版本</text>
          <view class="spec-chips">
            <view
              class="spec-chip"
              v-for="v in versions"
              :key="v"
              :class="{ active: selectedVersion === v }"
              @click="selectedVersion = v"
            >
              <text>{{ v }}</text>
            </view>
          </view>
        </view>

        <!-- 数量步进器 -->
        <view class="sku-stepper-row">
          <text class="ssr-label">购买数量</text>
          <view class="stepper">
            <view class="btn-step" :class="{ disabled: qty <= 1 }" @click="decreaseQty"><text>-</text></view>
            <text class="step-val">{{ qty }}</text>
            <view class="btn-step" @click="increaseQty"><text>+</text></view>
          </view>
        </view>

        <view class="sku-bottom-actions">
          <button class="sku-btn-cart" @click="confirmAddToCart">加入购物车</button>
          <button class="sku-btn-buy" @click="confirmBuyNow">立即购买</button>
        </view>
      </view>
    </view>

    <!-- 悬浮购物车组件 -->
    <FloatingCart ref="floatingCart" />
  </view>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import { getDishDetail } from '@/api/index.js'
import FloatingCart from '@/components/FloatingCart.vue'

export default {
  components: {
    FloatingCart
  },
  data() {
    return {
      goodsId: 2,
      currentSwiper: 0,
      showSku: false,
      skuAction: 'select',
      qty: 1,
      selectedColor: '晴空翠',
      selectedVersion: '4+128G',
      colors: ['晴空翠', '碧空蓝', '砂石黑'],
      versions: ['4+64G', '4+128G', '6+128G'],
      goods: {
        id: 2,
        name: 'Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量 1300万高清相机',
        price: 599,
        originalPrice: 699,
        soldCount: 1580,
        tag: '双旦限时直降¥100'
      },
      swiperImages: [
        '/static/images/png/dish-2.png',
        '/static/images/png/dish-1.png',
        '/static/images/png/dish-3.png'
      ]
    }
  },
  computed: {
    ...mapState(['pickupPoint', 'cart']),
    ...mapGetters(['cartCount']),
    currentPrice() {
      if (this.selectedVersion === '4+64G') return 499
      if (this.selectedVersion === '6+128G') return 699
      return 599
    },
    selectedSpecSummary() {
      return `${this.selectedColor} ${this.selectedVersion}`
    }
  },
  onLoad(options) {
    if (options.id) {
      this.goodsId = options.id
      this.fetchDetail(options.id)
    }
  },
  methods: {
    async fetchDetail(id) {
      try {
        const res = await getDishDetail(id)
        if (res && res.data && res.data.data) {
          const d = res.data.data
          this.goods = {
            id: d.id,
            name: d.name,
            price: d.price || 599,
            originalPrice: Math.round((d.price || 599) * 1.2),
            soldCount: d.sold || 1580,
            tag: '限时直降'
          }
          if (d.image) {
            this.swiperImages = [d.image, '/static/images/png/dish-1.png', '/static/images/png/dish-2.png']
          }
        }
      } catch (e) {
        // Fallback to local default
      }
    },
    onSwiperChange(e) {
      this.currentSwiper = e.detail.current
    },
    decreaseQty() {
      if (this.qty > 1) this.qty--
    },
    increaseQty() {
      this.qty++
    },
    openSkuModal(action) {
      this.skuAction = action
      this.showSku = true
    },
    onAddToCart() {
      this.confirmAddToCart()
    },
    onBuyNow() {
      this.confirmBuyNow()
    },
    confirmAddToCart() {
      const item = {
        id: `${this.goods.id}-${this.selectedColor}-${this.selectedVersion}`,
        dishId: this.goods.id,
        name: this.goods.name,
        flavor: `${this.selectedColor} ${this.selectedVersion}`,
        price: this.currentPrice,
        image: this.swiperImages[0],
        number: this.qty,
        quantity: this.qty,
        checked: true
      }
      this.$store.commit('addToCart', item)
      this.showSku = false
      if (this.$refs.floatingCart) {
        this.$refs.floatingCart.animateAdd(this.qty || 1)
      }
    },
    confirmBuyNow() {
      this.showSku = false
      const checkoutItem = {
        id: `${this.goods.id}-${this.selectedColor}-${this.selectedVersion}`,
        dishId: this.goods.id,
        name: this.goods.name,
        flavor: `${this.selectedColor} ${this.selectedVersion}`,
        price: this.currentPrice,
        image: this.swiperImages[0],
        quantity: this.qty,
        number: this.qty
      }
      uni.setStorageSync('checkout_items', [checkoutItem])
      uni.navigateTo({
        url: '/pages/order/confirm'
      })
    },
    goService() {
      uni.navigateTo({ url: '/pages/service/service' })
    },
    goPickup() {
      uni.navigateTo({ url: '/pages/pickup/pickup' })
    },
    goCart() {
      uni.switchTab({ url: '/pages/cart/cart' })
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #F7F8FA;
}

.detail-scroll {
  flex: 1;
  overflow: hidden;
}

/* 轮播大图 */
.swiper-wrap {
  position: relative;
  width: 100%;
  height: 700rpx;
  background-color: #FFFFFF;

  .goods-swiper {
    width: 100%;
    height: 100%;

    .swiper-img {
      width: 100%;
      height: 100%;
    }
  }

  .swiper-indicator {
    position: absolute;
    right: 28rpx;
    bottom: 28rpx;
    background: rgba(0, 0, 0, 0.45);
    color: #FFFFFF;
    font-size: 22rpx;
    padding: 6rpx 18rpx;
    border-radius: 20rpx;
  }
}

/* 紫蓝渐变活动价格条 */
.promo-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 110rpx;
  background: linear-gradient(90deg, #4A1FB8 0%, #2938C4 60%, #1565C0 100%);
  padding: 0 28rpx;
  color: #FFFFFF;

  .pb-left {
    display: flex;
    flex-direction: column;
    justify-content: center;

    .pb-tag {
      font-size: 20rpx;
      line-height: 1;
      opacity: 0.9;
      margin-bottom: 6rpx;
    }

    .pb-price-row {
      display: flex;
      align-items: baseline;

      .pb-symbol {
        font-size: 28rpx;
        font-weight: 700;
        margin-right: 4rpx;
      }

      .pb-price {
        font-size: 52rpx;
        font-weight: 800;
        line-height: 1;
      }
    }
  }

  .pb-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    justify-content: center;

    .pb-title {
      font-size: 28rpx;
      font-weight: 700;
      color: #FFFFFF;
      margin-bottom: 6rpx;
    }

    .pb-sub {
      font-size: 20rpx;
      opacity: 0.85;
    }
  }
}

/* 标题与原价销量卡片 */
.goods-main-card {
  background-color: #FFFFFF;
  padding: 24rpx 28rpx;

  .goods-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #1F2329;
    line-height: 44rpx;
    margin-bottom: 16rpx;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }

  .price-sales-row {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .orig-price {
      font-size: 26rpx;
      color: #999999;
      text-decoration: line-through;
    }

    .sales-text {
      font-size: 24rpx;
      color: #8F959E;
    }
  }
}

/* 规格、配送与自提卡片 */
.section-card {
  margin-top: 18rpx;
  background-color: #FFFFFF;
  padding: 0 28rpx;

  .divider {
    height: 1rpx;
    background-color: #F0F2F5;
  }

  .spec-row,
  .service-row,
  .station-row {
    display: flex;
    align-items: center;
    height: 96rpx;

    .sr-label {
      width: 140rpx;
      font-size: 26rpx;
      color: #8F959E;
    }
  }

  .spec-row {
    justify-content: space-between;

    .sr-left {
      display: flex;
      align-items: center;
      flex: 1;

      .sr-val {
        font-size: 26rpx;
        color: #1F2329;
        font-weight: 500;
      }
    }

    /* 行内步进器 */
    .inline-stepper {
      display: flex;
      align-items: center;

      .step-btn {
        width: 44rpx;
        height: 44rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        &.minus {
          background-color: #EFEFEF;
          color: #666666;

          &.disabled {
            opacity: 0.5;
          }
        }

        &.plus {
          background-color: #25B873;
          color: #FFFFFF;
        }

        .btn-symbol {
          font-size: 28rpx;
          line-height: 1;
          font-weight: 700;
        }
      }

      .step-num {
        font-size: 26rpx;
        font-weight: 600;
        color: #1F2329;
        margin: 0 16rpx;
        min-width: 32rpx;
        text-align: center;
      }
    }
  }

  .service-row {
    .service-text {
      font-size: 26rpx;
      color: #1F2329;
    }
  }

  .station-row {
    justify-content: space-between;

    .station-val {
      display: flex;
      align-items: center;
      flex: 1;
      justify-content: space-between;

      .station-name {
        font-size: 26rpx;
        color: #1F2329;
      }

      .arrow {
        font-size: 32rpx;
        color: #C9CDD4;
        margin-left: 8rpx;
      }
    }
  }
}

/* 图文详情分割线 */
.detail-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36rpx 0 20rpx;

  .line {
    width: 60rpx;
    height: 1rpx;
    background-color: #D8D8D8;
  }

  .detail-title {
    font-size: 26rpx;
    color: #666666;
    margin: 0 18rpx;
    font-weight: 500;
  }
}

.detail-content {
  background-color: #FFFFFF;
  padding: 20rpx;

  .desc-box {
    background-color: #F7F8FA;
    padding: 20rpx;
    border-radius: 12rpx;
    margin-bottom: 20rpx;

    .desc-tag {
      font-size: 24rpx;
      font-weight: 700;
      color: #25B873;
      margin-bottom: 8rpx;
      display: block;
    }

    .desc-text {
      font-size: 24rpx;
      color: #666666;
      line-height: 36rpx;
    }
  }

  .detail-img {
    width: 100%;
    display: block;
    margin-bottom: 12rpx;
    border-radius: 8rpx;
  }
}

/* 底部操作栏 */
.bottom-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 104rpx;
  background-color: #FFFFFF;
  border-top: 1rpx solid #EBEBEB;
  padding: 0 24rpx;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);

  .left-icons {
    display: flex;
    align-items: center;

    .icon-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-right: 36rpx;

      .ib-icon {
        font-size: 38rpx;
        line-height: 1;
      }

      .ib-label {
        font-size: 20rpx;
        color: #666666;
        margin-top: 6rpx;
      }

      .cart-badge-wrap {
        position: relative;

        .cart-badge {
          position: absolute;
          top: -10rpx;
          right: -18rpx;
          background-color: #FF4D4F;
          color: #FFFFFF;
          font-size: 18rpx;
          height: 26rpx;
          min-width: 26rpx;
          line-height: 26rpx;
          border-radius: 13rpx;
          text-align: center;
          padding: 0 6rpx;
        }
      }
    }
  }

  .right-buttons {
    display: flex;
    align-items: center;
    flex: 1;
    justify-content: flex-end;

    .btn-cart-green,
    .btn-buy-blue {
      height: 76rpx;
      line-height: 76rpx;
      border-radius: 38rpx;
      font-size: 28rpx;
      font-weight: 600;
      color: #FFFFFF;
      padding: 0 34rpx;
      margin: 0 8rpx;
      border: none;

      &::after {
        border: none;
      }
    }

    /* 加入购物车：高饱和鲜绿 */
    .btn-cart-green {
      background-color: #25B873;
    }

    /* 立即购买：高饱和鲜蓝 */
    .btn-buy-blue {
      background-color: #1677FF;
    }
  }
}

/* SKU 弹窗 */
.sku-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 999;
  display: flex;
  align-items: flex-end;

  .sku-modal-content {
    width: 100%;
    background-color: #FFFFFF;
    border-radius: 28rpx 28rpx 0 0;
    padding: 30rpx 28rpx;
    box-sizing: border-box;

    .sku-top {
      display: flex;
      position: relative;
      padding-bottom: 24rpx;
      border-bottom: 1rpx solid #F0F2F5;

      .sku-img {
        width: 150rpx;
        height: 150rpx;
        border-radius: 12rpx;
        background-color: #F7F8FA;
      }

      .sku-info {
        flex: 1;
        margin-left: 20rpx;
        display: flex;
        flex-direction: column;
        justify-content: flex-end;

        .sku-price {
          color: #FA5151;
          font-weight: 700;

          .sku-symbol {
            font-size: 26rpx;
          }

          .sku-val {
            font-size: 40rpx;
          }
        }

        .sku-stock {
          font-size: 22rpx;
          color: #8F959E;
          margin-top: 6rpx;
        }

        .sku-tip {
          font-size: 24rpx;
          color: #1F2329;
          margin-top: 4rpx;
        }
      }

      .sku-close {
        font-size: 44rpx;
        color: #8F959E;
        line-height: 1;
        padding: 10rpx;
      }
    }

    .spec-section {
      margin-top: 24rpx;

      .spec-sec-title {
        font-size: 26rpx;
        font-weight: 600;
        color: #1F2329;
        margin-bottom: 14rpx;
        display: block;
      }

      .spec-chips {
        display: flex;
        flex-wrap: wrap;

        .spec-chip {
          padding: 12rpx 24rpx;
          background-color: #F7F8FA;
          border-radius: 30rpx;
          font-size: 24rpx;
          color: #1F2329;
          margin-right: 16rpx;
          margin-bottom: 16rpx;
          border: 1rpx solid transparent;

          &.active {
            background-color: #E8F7F0;
            color: #25B873;
            border-color: #25B873;
            font-weight: 600;
          }
        }
      }
    }

    .sku-stepper-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 20rpx;
      padding-top: 20rpx;
      border-top: 1rpx solid #F0F2F5;

      .ssr-label {
        font-size: 26rpx;
        font-weight: 600;
        color: #1F2329;
      }

      .stepper {
        display: flex;
        align-items: center;

        .btn-step {
          width: 50rpx;
          height: 50rpx;
          background-color: #F2F3F5;
          border-radius: 8rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 32rpx;
          color: #1F2329;

          &.disabled {
            opacity: 0.4;
          }
        }

        .step-val {
          font-size: 28rpx;
          font-weight: 600;
          color: #1F2329;
          margin: 0 20rpx;
          min-width: 36rpx;
          text-align: center;
        }
      }
    }

    .sku-bottom-actions {
      display: flex;
      align-items: center;
      margin-top: 36rpx;
      gap: 16rpx;

      .sku-btn-cart,
      .sku-btn-buy {
        flex: 1;
        height: 80rpx;
        line-height: 80rpx;
        border-radius: 40rpx;
        font-size: 28rpx;
        font-weight: 600;
        color: #FFFFFF;
        border: none;

        &::after {
          border: none;
        }
      }

      .sku-btn-cart {
        background-color: #25B873;
      }

      .sku-btn-buy {
        background-color: #1677FF;
      }
    }
  }
}
</style>
