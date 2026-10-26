<template>
  <view class="mine-page">
    <view class="profile-header"
      ><view class="user-row" @tap="go('/pages/login/login')"
        ><view class="avatar-box"
          ><image
            class="avatar-img"
            src="/static/images/default-avatar.png" /></view
        ><view class="user-info"
          ><view class="name-line"
            ><text class="user-name">{{
              user.token ? "我的生鲜生活" : "点击登录 / 注册"
            }}</text></view
          ><text class="user-id">{{
            user.token ? maskedPhone : "欢迎来到生鲜自提商城"
          }}</text></view
        ></view
      ></view
    >
    <view class="menu-card order-card"
      ><view class="card-head" @tap="go('/pages/order/order')"
        ><text class="ch-title">我的订单</text
        ><text class="ch-all">查看全部 ›</text></view
      ><view class="order-grid"
        ><view
          class="grid-item"
          v-for="tab in orderTabs"
          :key="tab.name"
          @tap="go('/pages/order/order?status=' + tab.status)"
          ><view class="icon-wrap"
            ><text class="pink-icon">{{ tab.icon }}</text></view
          ><text class="grid-label">{{ tab.name }}</text></view
        ></view
      ></view
    >
    <view class="vip-banner-card" @tap="go('/pages/groupon/list')"
      ><view class="vbc-text"
        ><text class="vbc-title">邻里好物，一起拼团</text></view
      ><button class="vbc-btn">去逛逛</button></view
    >
    <view class="menu-card list-menu-card"
      ><view
        class="list-item"
        v-for="item in menus"
        :key="item.name"
        @tap="go(item.url)"
        ><view class="li-left"
          ><text class="li-icon">{{ item.icon }}</text
          ><text class="li-title">{{ item.name }}</text></view
        ><text class="li-arrow">›</text></view
      ></view
    >
    <view v-if="user.token" class="menu-card list-menu-card"
      ><view class="list-item" @tap="logout"
        ><text class="li-title">退出登录</text
        ><text class="li-arrow">›</text></view
      ></view
    >
  </view>
</template>
<script>
export default {
  data: () => ({
    orderTabs: [
      { name: "待付款", status: 1, icon: "💳" },
      { name: "待履约", status: 2, icon: "📦" },
      { name: "已完成", status: 5, icon: "✓" },
      { name: "已取消", status: 6, icon: "↩" },
    ],
    menus: [
      { name: "我的收货地址", icon: "🏠", url: "/pages/address/list" },
      { name: "我的社区拼团", icon: "🛒", url: "/pages/groupon/my" },
      { name: "切换自提门店", icon: "📍", url: "/pages/pickup/pickup" },
      { name: "登录与密码设置", icon: "🔐", url: "/pages/login/login" },
    ],
  }),
  computed: {
    user() {
      return this.$store.state.user;
    },
    maskedPhone() {
      const p = this.user.phone || "";
      return p.length === 11 ? p.slice(0, 3) + "****" + p.slice(7) : p;
    },
  },
  methods: {
    go(url) {
      uni.navigateTo({ url });
    },
    logout() {
      this.$store.commit("setClearUser");
      uni.showToast({ title: "已退出登录", icon: "none" });
    },
  },
};
</script>
<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50rpx;
}

/* 顶部薰衣草粉紫渐变卡片 */
.profile-header {
  position: relative;
  height: 300rpx;
  background: linear-gradient(135deg, #a8bee8 0%, #d8bfd8 50%, #f5c7b8 100%);
  padding: 80rpx 36rpx 0;
  box-sizing: border-box;

  .user-row {
    display: flex;
    align-items: center;

    .avatar-box {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      border: 4rpx solid #ffffff;
      overflow: hidden;
      background-color: #e2e8f0;
      box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);

      .avatar-img {
        width: 100%;
        height: 100%;
      }
    }

    .user-info {
      flex: 1;
      margin-left: 24rpx;

      .name-line {
        display: flex;
        align-items: center;
        margin-bottom: 10rpx;

        .user-name {
          font-size: 34rpx;
          font-weight: 700;
          color: #1f2329;
          margin-right: 14rpx;
        }

        .vip-badge {
          background-color: rgba(31, 35, 41, 0.7);
          padding: 4rpx 14rpx;
          border-radius: 20rpx;

          text {
            font-size: 20rpx;
            color: #ffffff;
          }
        }
      }

      .user-id {
        font-size: 24rpx;
        color: #4e5969;
      }
    }
  }
}

/* 资产数据三列卡片 (悬浮向上提20rpx) */
.assets-card {
  position: relative;
  z-index: 10;
  margin: -30rpx 24rpx 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  padding: 30rpx 0;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);

  .asset-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;

    .asset-val {
      font-size: 38rpx;
      font-weight: 700;
      color: #1f2329;
      line-height: 1;
    }

    .asset-name {
      font-size: 24rpx;
      color: #8f959e;
      margin-top: 12rpx;
    }
  }

  .asset-divider {
    width: 2rpx;
    height: 48rpx;
    background-color: #f0f2f5;
  }
}

/* 通用白底卡片 */
.menu-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  margin: 0 24rpx 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);
}

/* 订单卡片 */
.order-card {
  padding: 24rpx 28rpx;

  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24rpx;

    .ch-title {
      font-size: 30rpx;
      font-weight: 700;
      color: #1f2329;
    }

    .ch-right {
      display: flex;
      align-items: center;

      .ch-all {
        font-size: 24rpx;
        color: #8f959e;
      }

      .ch-arrow {
        font-size: 30rpx;
        color: #c9cdd4;
        margin-left: 6rpx;
      }
    }
  }

  .order-grid {
    display: flex;
    align-items: center;
    justify-content: space-around;

    .grid-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .icon-wrap {
        position: relative;
        width: 60rpx;
        height: 60rpx;
        display: flex;
        align-items: center;
        justify-content: center;

        .pink-icon {
          font-size: 42rpx;
          line-height: 1;
        }

        .badge {
          position: absolute;
          top: -4rpx;
          right: -12rpx;
          background-color: #ff4d4f;
          color: #ffffff;
          font-size: 20rpx;
          height: 28rpx;
          min-width: 28rpx;
          line-height: 28rpx;
          text-align: center;
          border-radius: 14rpx;
          padding: 0 6rpx;
          font-weight: 700;
        }
      }

      .grid-label {
        font-size: 24rpx;
        color: #1f2329;
        margin-top: 12rpx;
      }
    }
  }
}

/* VIP 横幅 */
.vip-banner-card {
  margin: 0 24rpx 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.02);

  .vbc-text {
    margin-bottom: 16rpx;

    .vbc-title {
      font-size: 24rpx;
      color: #333333;
      text-align: center;
    }
  }

  .vbc-btn {
    width: 60%;
    height: 64rpx;
    line-height: 64rpx;
    background-color: #25b873;
    color: #ffffff;
    font-size: 26rpx;
    font-weight: 600;
    border-radius: 32rpx;
    border: none;

    &::after {
      border: none;
    }
  }
}

/* 浏览足迹 */
.footprint-card {
  padding: 24rpx 28rpx;

  .fp-header {
    margin-bottom: 20rpx;

    .fp-title-row {
      display: flex;
      align-items: center;

      .fp-icon {
        font-size: 30rpx;
        margin-right: 10rpx;
      }

      .fp-title {
        font-size: 28rpx;
        font-weight: 700;
        color: #1f2329;
      }
    }
  }

  .fp-scroll {
    white-space: nowrap;
    width: 100%;

    .fp-item {
      display: inline-block;
      width: 150rpx;
      height: 150rpx;
      border-radius: 12rpx;
      margin-right: 18rpx;
      background-color: #f7f8fa;
      overflow: hidden;

      .fp-img {
        width: 100%;
        height: 100%;
      }
    }
  }
}

/* 功能列表 */
.list-menu-card {
  padding: 0 28rpx;

  .divider {
    height: 1rpx;
    background-color: #f0f2f5;
  }

  .list-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 96rpx;

    .li-left {
      display: flex;
      align-items: center;

      .li-icon {
        font-size: 32rpx;
        margin-right: 16rpx;
      }

      .li-title {
        font-size: 28rpx;
        color: #1f2329;
      }
    }

    .li-arrow {
      font-size: 32rpx;
      color: #c9cdd4;
    }
  }
}
</style>

<style scoped>
.profile-header {
  background: linear-gradient(135deg, #25b873, #67d5a0);
}
.vip-banner-card {
  background: #e8f7f0;
}
.vbc-title {
  color: #238852;
}
.vbc-btn {
  background: #25b873;
}
.pink-icon {
  color: #25b873;
}
.order-card {
  margin-top: -30rpx;
}
.list-item + .list-item {
  border-top: 1px solid #f2f3f5;
}
</style>
