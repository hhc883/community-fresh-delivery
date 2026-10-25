<template>
  <view class="core"
    ><text class="title">购物车</text
    ><text class="sub">购物车保存在当前设备。提交时服务器校验价格与库存。</text
    ><view v-if="!cart.length" class="empty"
      >购物车还是空的，去首页挑选商品吧。</view
    ><view class="card" v-for="item in cart" :key="item.id"
      ><view class="row"
        ><checkbox :checked="item.checked !== false" @tap="toggle(item)" /><text
          class="product-name"
          >{{ item.name }}</text
        ><text class="price">¥{{ $fmtMoney(item.price) }}</text></view
      ><view class="row"
        ><button class="secondary" @tap="change(item, -1)">−</button
        ><text>{{ item.quantity }}</text
        ><button class="secondary" @tap="change(item, 1)">+</button
        ><button class="secondary" @tap="remove(item)">移除</button></view
      ></view
    ><view v-if="cart.length"
      ><text class="sub"
        >预估合计 ¥{{ $fmtMoney(total) }}，最终金额以订单为准。</text
      ><button class="primary" :disabled="!selected.length" @tap="confirm">
        结算 ({{ selected.length }} 种商品)
      </button></view
    ></view
  >
</template>
<script>
export default {
  computed: {
    cart() {
      return this.$store.state.cart;
    },
    selected() {
      return this.cart.filter((i) => i.checked !== false);
    },
    total() {
      return this.selected.reduce((n, i) => n + i.quantity * i.price, 0);
    },
  },
  methods: {
    toggle(i) {
      this.$store.commit("toggleChecked", i.id);
    },
    change(i, n) {
      if (i.quantity + n < 1) return;
      this.$store.commit("updateQuantity", {
        dishId: i.id,
        quantity: i.quantity + n,
      });
    },
    remove(i) {
      this.$store.commit("removeFromCart", i.id);
    },
    confirm() {
      uni.navigateTo({ url: "/pages/order/confirm" });
    },
  },
};
</script>
