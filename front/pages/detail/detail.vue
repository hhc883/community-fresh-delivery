<template>
  <view class="detail-page"
    ><view v-if="loading" class="empty">商品加载中…</view
    ><view v-else-if="error" class="error" @tap="load"
      >{{ error }} · 点击重试</view
    ><template v-else-if="dish">
      <scroll-view scroll-y class="detail-scroll"
        ><view class="hero-swiper"
          ><image
            v-if="dish.image"
            style="width: 100%; height: 680rpx"
            :src="dish.image"
            mode="aspectFill"
          /><view v-else class="empty">暂无商品图片</view></view
        >
        <view class="promo-banner"
          ><view class="pb-left"
            ><text class="pb-tag">商品价格</text
            ><view class="pb-price-row"
              ><text class="pb-symbol">¥</text
              ><text class="pb-price">{{ $fmtMoney(dish.price) }}</text></view
            ></view
          ><view class="pb-right"
            ><text class="pb-title">新鲜好物</text
            ><text class="pb-sub"
              >库存 {{ dish.stock }} {{ dish.unit || "份" }}</text
            ></view
          ></view
        >
        <view class="goods-main-card"
          ><text class="goods-title">{{ dish.name }}</text
          ><view class="price-sales-row"
            ><text class="sales-text"
              >{{ dish.unit || "份"
              }}<text v-if="dish.origin"> · 产地 {{ dish.origin }}</text></text
            ></view
          ></view
        >
        <view class="section-card"
          ><view class="service-row"
            ><text class="sr-label">履约方式</text
            ><text class="service-text">配送到家 / 门店自提</text></view
          ><view class="divider" /><view class="station-row" @tap="goPickup"
            ><text class="sr-label">自提网点</text
            ><view class="station-val"
              ><text class="station-name">{{
                pickupPoint.name || "请选择自提点"
              }}</text
              ><text class="arrow">›</text></view
            ></view
          ><view
            v-if="Number(dish.isGroupon) === 1"
            class="station-row"
            @tap="group"
            ><text class="sr-label">社区拼团</text
            ><text class="station-name">查看可参与的团购 ›</text></view
          ></view
        >
        <view class="detail-divider"
          ><view class="line" /><text class="detail-title">商品详情</text
          ><view class="line" /></view
        ><view class="detail-content"
          ><view class="desc-box"
            ><text class="desc-text">{{
              dish.description || "暂无商品介绍"
            }}</text></view
          ><image
            v-if="dish.image"
            class="detail-img"
            :src="dish.image"
            mode="widthFix"
        /></view> </scroll-view
      ><view class="bottom-action-bar"
        ><view class="left-icons"
          ><view class="icon-btn" @tap="goCart"
            ><text class="ib-icon">🛒</text
            ><text class="ib-label">购物车 {{ cartCount || "" }}</text></view
          ></view
        ><view class="right-buttons"
          ><button
            class="btn-cart-green"
            :disabled="Number(dish.stock) <= 0"
            @tap="add"
          >
            加入购物车</button
          ><button
            class="btn-buy-blue"
            :disabled="Number(dish.stock) <= 0"
            @tap="buy"
          >
            立即购买
          </button></view
        ></view
      > </template
    ><view v-else class="empty">商品不存在或已下架</view></view
  >
</template>
<script>
import { getDishDetail } from "../../api/index.js";
export default {
  data: () => ({ id: "", dish: null, loading: true, error: "" }),
  onLoad(o) {
    this.id = o.id || "";
    this.load();
  },
  computed: {
    pickupPoint() {
      return this.$store.state.pickupPoint;
    },
    cartCount() {
      return this.$store.getters.cartCount;
    },
  },
  methods: {
    goPickup() {
      uni.navigateTo({ url: "/pages/pickup/pickup" });
    },
    goCart() {
      uni.switchTab({ url: "/pages/cart/cart" });
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        if (!this.id) throw new Error("缺少商品标识");
        this.dish = (await getDishDetail(this.id)).data;
      } catch (e) {
        this.error = e.message || "商品加载失败";
        this.dish = null;
      } finally {
        this.loading = false;
      }
    },
    add() {
      if (!this.dish) return;
      this.$store.commit("addToCart", this.dish);
    },
    buy() {
      if (!this.dish) return;
      uni.navigateTo({
        url: "/pages/order/confirm?dishId=" + encodeURIComponent(this.id),
      });
    },
    group() {
      if (!this.dish) return;
      uni.navigateTo({
        url: "/pages/groupon/detail?dishId=" + encodeURIComponent(this.id),
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f7f8fa;
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
  background-color: #ffffff;

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
    color: #ffffff;
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
  background: linear-gradient(90deg, #4a1fb8 0%, #2938c4 60%, #1565c0 100%);
  padding: 0 28rpx;
  color: #ffffff;

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
      color: #ffffff;
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
  background-color: #ffffff;
  padding: 24rpx 28rpx;

  .goods-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #1f2329;
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
      color: #8f959e;
    }
  }
}

/* 规格、配送与自提卡片 */
.section-card {
  margin-top: 18rpx;
  background-color: #ffffff;
  padding: 0 28rpx;

  .divider {
    height: 1rpx;
    background-color: #f0f2f5;
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
      color: #8f959e;
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
        color: #1f2329;
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
          background-color: #efefef;
          color: #666666;

          &.disabled {
            opacity: 0.5;
          }
        }

        &.plus {
          background-color: #25b873;
          color: #ffffff;
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
        color: #1f2329;
        margin: 0 16rpx;
        min-width: 32rpx;
        text-align: center;
      }
    }
  }

  .service-row {
    .service-text {
      font-size: 26rpx;
      color: #1f2329;
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
        color: #1f2329;
      }

      .arrow {
        font-size: 32rpx;
        color: #c9cdd4;
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
    background-color: #d8d8d8;
  }

  .detail-title {
    font-size: 26rpx;
    color: #666666;
    margin: 0 18rpx;
    font-weight: 500;
  }
}

.detail-content {
  background-color: #ffffff;
  padding: 20rpx;

  .desc-box {
    background-color: #f7f8fa;
    padding: 20rpx;
    border-radius: 12rpx;
    margin-bottom: 20rpx;

    .desc-tag {
      font-size: 24rpx;
      font-weight: 700;
      color: #25b873;
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
  background-color: #ffffff;
  border-top: 1rpx solid #ebebeb;
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
          background-color: #ff4d4f;
          color: #ffffff;
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
      color: #ffffff;
      padding: 0 34rpx;
      margin: 0 8rpx;
      border: none;

      &::after {
        border: none;
      }
    }

    /* 加入购物车：高饱和鲜绿 */
    .btn-cart-green {
      background-color: #25b873;
    }

    /* 立即购买：高饱和鲜蓝 */
    .btn-buy-blue {
      background-color: #1677ff;
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
    background-color: #ffffff;
    border-radius: 28rpx 28rpx 0 0;
    padding: 30rpx 28rpx;
    box-sizing: border-box;

    .sku-top {
      display: flex;
      position: relative;
      padding-bottom: 24rpx;
      border-bottom: 1rpx solid #f0f2f5;

      .sku-img {
        width: 150rpx;
        height: 150rpx;
        border-radius: 12rpx;
        background-color: #f7f8fa;
      }

      .sku-info {
        flex: 1;
        margin-left: 20rpx;
        display: flex;
        flex-direction: column;
        justify-content: flex-end;

        .sku-price {
          color: #fa5151;
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
          color: #8f959e;
          margin-top: 6rpx;
        }

        .sku-tip {
          font-size: 24rpx;
          color: #1f2329;
          margin-top: 4rpx;
        }
      }

      .sku-close {
        font-size: 44rpx;
        color: #8f959e;
        line-height: 1;
        padding: 10rpx;
      }
    }

    .spec-section {
      margin-top: 24rpx;

      .spec-sec-title {
        font-size: 26rpx;
        font-weight: 600;
        color: #1f2329;
        margin-bottom: 14rpx;
        display: block;
      }

      .spec-chips {
        display: flex;
        flex-wrap: wrap;

        .spec-chip {
          padding: 12rpx 24rpx;
          background-color: #f7f8fa;
          border-radius: 30rpx;
          font-size: 24rpx;
          color: #1f2329;
          margin-right: 16rpx;
          margin-bottom: 16rpx;
          border: 1rpx solid transparent;

          &.active {
            background-color: #e8f7f0;
            color: #25b873;
            border-color: #25b873;
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
      border-top: 1rpx solid #f0f2f5;

      .ssr-label {
        font-size: 26rpx;
        font-weight: 600;
        color: #1f2329;
      }

      .stepper {
        display: flex;
        align-items: center;

        .btn-step {
          width: 50rpx;
          height: 50rpx;
          background-color: #f2f3f5;
          border-radius: 8rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 32rpx;
          color: #1f2329;

          &.disabled {
            opacity: 0.4;
          }
        }

        .step-val {
          font-size: 28rpx;
          font-weight: 600;
          color: #1f2329;
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
        color: #ffffff;
        border: none;

        &::after {
          border: none;
        }
      }

      .sku-btn-cart {
        background-color: #25b873;
      }

      .sku-btn-buy {
        background-color: #1677ff;
      }
    }
  }
}
</style>

<style scoped>
.promo-banner {
  background: linear-gradient(120deg, #25b873, #72d29a);
}
.btn-buy-blue {
  background: #238852;
}
.detail-scroll {
  height: calc(100vh - 160rpx);
}
</style>
