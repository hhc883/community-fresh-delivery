<template>
  <view class="core"
    ><view class="hero"
      ><text class="title">我的鲜选</text
      ><text class="sub">{{
        user.token ? user.phone : "登录后查看订单、地址与我的拼团"
      }}</text></view
    ><button v-if="!user.token" class="primary" @tap="go('/pages/login/login')">
      登录 / 注册</button
    ><view v-else class="card"
      ><button class="secondary" @tap="go('/pages/order/order')">
        我的订单与消费记录</button
      ><button class="secondary" @tap="go('/pages/address/list')">
        管理收货地址</button
      ><button class="secondary" @tap="go('/pages/groupon/my')">我的拼团</button
      ><button class="secondary" @tap="go('/pages/login/login')">
        设置登录密码</button
      ><button class="secondary" @tap="logout">退出登录</button></view
    ><text class="sub"
      >当前为毕设核心版本，支付与退款均为模拟记账。优惠券、邀请奖励、秒杀、评价、独立商家及自提点运营端尚未开放。</text
    ></view
  >
</template>
<script>
export default {
  computed: {
    user() {
      return this.$store.state.user;
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
