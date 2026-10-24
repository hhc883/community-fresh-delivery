<template>
  <view class="pickup-page">
    <!-- 顶部搜索栏 -->
    <view class="search-bar">
      <view class="search-input-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索自提网点名称或地址"
          placeholder-style="color:#C9CDD4;"
        />
      </view>
    </view>

    <!-- 网点列表 -->
    <view class="station-list">
      <view
        class="station-card"
        v-for="st in filteredStations"
        :key="st.id"
        @click="openStationModal(st)"
      >
        <view class="sc-main">
          <image class="st-thumb" :src="st.image || '/static/images/png/dish-1.png'" mode="aspectFill" />
          <view class="st-info">
            <view class="st-name-row">
              <text class="st-name">{{ st.name }}</text>
            </view>
            <text class="st-dist">距离您 {{ st.distance || '260m' }}</text>
            <text class="st-addr">{{ st.address }}</text>
          </view>
        </view>
        <view class="sc-bottom">
          <text class="st-contact">联系人：{{ st.contact }} {{ st.phone }}</text>
          <view class="curr-badge" v-if="currentStation.id === st.id">
            <text>当前自提点</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 自提点弹窗 (精确还原 crop_r3_c1) -->
    <view class="station-modal-mask" v-if="showModal" @click="showModal = false">
      <view class="station-modal-content" @click.stop>
        <view class="sm-header">
          <view class="sm-title-row">
            <text class="green-dot">🏪</text>
            <text class="sm-title">当前自提点</text>
          </view>
          <text class="sm-close" @click="showModal = false">×</text>
        </view>

        <view class="sm-body">
          <text class="sm-station-name">{{ activeStation.name }}</text>

          <view class="sm-image-row">
            <image class="sm-img" :src="activeStation.image || '/static/images/png/dish-1.png'" mode="aspectFill" />
            <view class="sm-dist-box">
              <text class="sm-dist-text">距离您 {{ activeStation.distance || '260m' }}</text>
            </view>
          </view>

          <text class="sm-address-text">{{ activeStation.address }}</text>
        </view>

        <!-- 底部取消与选择按钮 -->
        <view class="sm-buttons">
          <button class="sm-btn btn-cancel" @click="showModal = false">取消</button>
          <button class="sm-btn btn-select-green" @click="confirmSelectStation">选择</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getStores } from '@/api/index.js'

export default {
  data() {
    return {
      keyword: '',
      showModal: false,
      activeStation: {
        id: 1,
        name: '高黎柱发士多店 (请自备袋子谢谢)',
        address: '广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号',
        distance: '260m',
        contact: '柱哥',
        phone: '13800138000',
        image: '/static/images/png/dish-1.png'
      },
      stations: [
        {
          id: 1,
          name: '高黎柱发士多店 (请自备袋子谢谢)',
          address: '广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号',
          distance: '260m',
          contact: '柱哥',
          phone: '13800138000',
          image: '/static/images/png/dish-1.png'
        },
        {
          id: 2,
          name: '容桂天佑城服务站',
          address: '广东省佛山市顺德区容桂桂洲大道中63号天佑城首层',
          distance: '1.2km',
          contact: '林站长',
          phone: '13900139000',
          image: '/static/images/png/dish-2.png'
        },
        {
          id: 3,
          name: '文秀社区便民生鲜自提点',
          address: '广东省佛山市顺德区大良文秀路18号',
          distance: '2.5km',
          contact: '陈阿姨',
          phone: '13700137000',
          image: '/static/images/png/dish-3.png'
        }
      ]
    }
  },
  computed: {
    ...mapState({
      currentStation: state => state.pickupPoint
    }),
    filteredStations() {
      if (!this.keyword) return this.stations
      const kw = this.keyword.trim().toLowerCase()
      return this.stations.filter(s =>
        (s.name && s.name.toLowerCase().includes(kw)) ||
        (s.address && s.address.toLowerCase().includes(kw))
      )
    }
  },
  onLoad() {
    this.fetchStations()
  },
  methods: {
    async fetchStations() {
      try {
        const res = await getStores()
        if (res && res.data && res.data.data && res.data.data.records) {
          const apiList = res.data.data.records.map((s, idx) => ({
            id: s.id,
            name: s.name,
            address: s.address,
            distance: `${(idx + 1) * 350}m`,
            contact: s.contact || '团长',
            phone: s.phone || '13800000000',
            image: s.pic || '/static/images/png/dish-1.png'
          }))
          if (apiList.length > 0) this.stations = apiList
        }
      } catch (e) {}
    },
    openStationModal(st) {
      this.activeStation = st
      this.showModal = true
    },
    confirmSelectStation() {
      this.$store.commit('SET_PICKUP_POINT', {
        id: this.activeStation.id,
        name: this.activeStation.name,
        address: this.activeStation.address,
        distance: this.activeStation.distance
      })
      this.showModal = false
      uni.showToast({
        title: '已切换自提点',
        icon: 'success'
      })
      setTimeout(() => {
        uni.navigateBack()
      }, 800)
    }
  }
}
</script>

<style lang="scss" scoped>
.pickup-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

/* 搜索栏 */
.search-bar {
  margin-bottom: 24rpx;

  .search-input-box {
    display: flex;
    align-items: center;
    background-color: #FFFFFF;
    height: 80rpx;
    border-radius: 40rpx;
    padding: 0 28rpx;
    box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.02);

    .search-icon {
      font-size: 32rpx;
      margin-right: 16rpx;
    }

    .search-input {
      flex: 1;
      font-size: 26rpx;
      color: #1F2329;
    }
  }
}

/* 网点列表 */
.station-list {
  .station-card {
    background-color: #FFFFFF;
    border-radius: 16rpx;
    padding: 24rpx 28rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);

    .sc-main {
      display: flex;

      .st-thumb {
        width: 140rpx;
        height: 140rpx;
        border-radius: 12rpx;
        background-color: #F7F8FA;
      }

      .st-info {
        flex: 1;
        margin-left: 20rpx;
        display: flex;
        flex-direction: column;
        justify-content: space-between;

        .st-name {
          font-size: 28rpx;
          font-weight: 700;
          color: #1F2329;
          line-height: 40rpx;
        }

        .st-dist {
          font-size: 24rpx;
          color: #25B873;
          font-weight: 500;
        }

        .st-addr {
          font-size: 24rpx;
          color: #8F959E;
          line-height: 34rpx;
        }
      }
    }

    .sc-bottom {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 18rpx;
      padding-top: 18rpx;
      border-top: 1rpx solid #F0F2F5;

      .st-contact {
        font-size: 24rpx;
        color: #666666;
      }

      .curr-badge {
        background-color: #E8F7F0;
        padding: 4rpx 16rpx;
        border-radius: 20rpx;

        text {
          font-size: 22rpx;
          color: #25B873;
          font-weight: 600;
        }
      }
    }
  }
}

/* 自提点弹窗 (根据 crop_r3_c1 精准设计) */
.station-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;

  .station-modal-content {
    width: 100%;
    background-color: #FFFFFF;
    border-radius: 24rpx;
    padding: 32rpx;
    box-sizing: border-box;

    .sm-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24rpx;

      .sm-title-row {
        display: flex;
        align-items: center;

        .green-dot {
          font-size: 32rpx;
          margin-right: 12rpx;
        }

        .sm-title {
          font-size: 30rpx;
          font-weight: 700;
          color: #1F2329;
        }
      }

      .sm-close {
        font-size: 40rpx;
        color: #C9CDD4;
        line-height: 1;
        padding: 6rpx;
      }
    }

    .sm-body {
      .sm-station-name {
        font-size: 30rpx;
        font-weight: 700;
        color: #1F2329;
        line-height: 44rpx;
        display: block;
        margin-bottom: 20rpx;
      }

      .sm-image-row {
        display: flex;
        align-items: center;
        margin-bottom: 16rpx;

        .sm-img {
          width: 120rpx;
          height: 120rpx;
          border-radius: 12rpx;
          background-color: #F7F8FA;
        }

        .sm-dist-box {
          margin-left: 20rpx;

          .sm-dist-text {
            font-size: 26rpx;
            color: #25B873;
            font-weight: 600;
          }
        }
      }

      .sm-address-text {
        font-size: 26rpx;
        color: #666666;
        line-height: 38rpx;
        margin-bottom: 36rpx;
        display: block;
      }
    }

    .sm-buttons {
      display: flex;
      align-items: center;
      gap: 20rpx;

      .sm-btn {
        flex: 1;
        height: 76rpx;
        line-height: 76rpx;
        border-radius: 38rpx;
        font-size: 28rpx;
        font-weight: 600;
        margin: 0;

        &::after {
          border: none;
        }

        &.btn-cancel {
          background-color: #EFEFEF;
          color: #666666;
          border: none;
        }

        &.btn-select-green {
          background-color: #25B873;
          color: #FFFFFF;
          border: none;
        }
      }
    }
  }
}
</style>
