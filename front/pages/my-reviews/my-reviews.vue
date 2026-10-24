<template>
  <view class="reviews-page">
    <view v-if="reviews.length > 0" class="review-list">
      <view
        class="review-item"
        v-for="r in reviews"
        :key="r.id"
        @click="goDetail(r)"
      >
        <view class="review-top">
          <text class="review-dish">{{ r.dishName || '生鲜优选商品' }}</text>
          <text class="review-badge">{{ r.auditStatus || '已发布' }}</text>
        </view>
        <view class="review-stars">
          <text v-for="s in 5" :key="s" :class="s <= (r.rating || 5) ? 'star active' : 'star'">★</text>
        </view>
        <text class="review-content">{{ r.content }}</text>
        <view class="review-bottom">
          <text class="review-time">{{ r.createdAt || '2021-03-21' }}</text>
        </view>
      </view>
    </view>

    <!-- 空状态：完美还原 crop_r3_c2.png -->
    <view v-else class="empty-state">
      <view class="empty-icon-wrap">
        <text class="empty-icon">📁</text>
      </view>
      <text class="empty-text">暂无数据</text>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getMyReviews } from '@/api/index.js'

export default {
  data() {
    return {
      reviews: []
    }
  },
  computed: {
    ...mapState(['user'])
  },
  onShow() {
    this.loadReviews()
  },
  methods: {
    async loadReviews() {
      try {
        const res = await getMyReviews()
        if (res && res.data && res.data.data) {
          this.reviews = res.data.data
        }
      } catch (e) {
        // Keep empty state to match crop_r3_c2
      }
    },
    goDetail(r) {
      if (r.dishId) {
        uni.navigateTo({ url: `/pages/detail/detail?id=${r.dishId}` })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.reviews-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.review-list {
  padding: 24rpx;

  .review-item {
    background-color: #FFFFFF;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    border: 1rpx solid #F0F2F5;

    .review-top {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12rpx;

      .review-dish {
        font-size: 28rpx;
        font-weight: 600;
        color: #1F2329;
      }

      .review-badge {
        font-size: 22rpx;
        color: #25B873;
      }
    }

    .review-stars {
      display: flex;
      margin-bottom: 12rpx;

      .star {
        font-size: 26rpx;
        color: #D8D8D8;
        margin-right: 6rpx;

        &.active {
          color: #FFB800;
        }
      }
    }

    .review-content {
      font-size: 26rpx;
      color: #666666;
      line-height: 38rpx;
    }

    .review-bottom {
      margin-top: 16rpx;

      .review-time {
        font-size: 22rpx;
        color: #8F959E;
      }
    }
  }
}

/* 完美还原 crop_r3_c2 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 80vh;

  .empty-icon-wrap {
    width: 180rpx;
    height: 180rpx;
    border-radius: 24rpx;
    background-color: #F7F8FA;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 30rpx;

    .empty-icon {
      font-size: 80rpx;
      opacity: 0.6;
    }
  }

  .empty-text {
    font-size: 28rpx;
    color: #8F959E;
  }
}
</style>
