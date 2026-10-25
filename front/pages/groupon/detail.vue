<template>
  <view class="core"
    ><view v-if="loading" class="empty">读取拼团详情…</view
    ><view v-if="error" class="error" @tap="load">{{ error }} · 点击重试</view
    ><view v-if="dish" class="card stack"
      ><image
        v-if="dish.image"
        class="product-image"
        :src="dish.image"
        mode="aspectFill"
      /><text class="title">{{ dish.name }}</text
      ><text class="price">拼团价 ¥{{ $fmtMoney(dish.grouponPrice) }}</text
      ><text class="sub"
        >{{ dish.grouponSize }} 人成团 · 有效期
        {{ dish.grouponValidHours }} 小时</text
      ><text class="sub">{{ dish.description }}</text
      ><button v-if="Number(dish.isGroupon) === 1" class="primary" @tap="open">
        我要开团</button
      ><button class="secondary" @tap="single">
        单独购买 ¥{{ $fmtMoney(dish.price) }}
      </button></view
    ><view v-if="group" class="card stack"
      ><text class="product-name"
        >本团：{{ labels[group.status] || "未知状态" }}</text
      ><text
        >{{ group.currentSize }} / {{ group.requiredSize }} 人 · 还差
        {{ group.lackSize }} 人</text
      ><text class="sub">截止时间（北京时间）：{{ group.expireTime }}</text
      ><text class="sub" v-for="(m, index) in group.members || []" :key="index"
        >{{ m.userName || "团员" }} ·
        {{ Number(m.isLeader) === 1 ? "团长" : "团员" }} ·
        {{ m.number }} 份</text
      ><button
        v-if="Number(group.status) === 1"
        class="primary"
        @tap="join(group)"
      >
        参与此团</button
      ><button
        v-if="group.orderId"
        class="secondary"
        @tap="order(group.orderId)"
      >
        查看我的关联订单
      </button></view
    ><text v-if="!group" class="product-name">可加入的团</text
    ><view v-if="!group && !groups.length && !loading && !error" class="empty"
      >暂无可加入的团，可以发起新团。</view
    ><view v-if="!group" v-for="g in groups" :key="g.id" class="card stack"
      ><text
        >{{ g.leaderName || "团长" }} · {{ g.currentSize }} /
        {{ g.requiredSize }} 人</text
      ><text>还差 {{ g.lackSize }} 人成团</text
      ><text class="sub">截止：{{ g.expireTime }}（北京时间）</text
      ><button class="primary" @tap="join(g)">加入此团</button></view
    ><button class="secondary" @tap="load">刷新拼团状态</button
    ><text class="sub"
      >支付与退款仅为模拟记账。拼团人数和状态以服务器为准。</text
    ></view
  >
</template>
<script>
import {
  getDishDetail,
  getGrouponDetail,
  getJoinableGroupons,
} from "../../api/index.js";
export default {
  data: () => ({
    dishId: "",
    id: "",
    dish: null,
    group: null,
    groups: [],
    loading: true,
    error: "",
    labels: { 1: "进行中", 2: "已成团", 3: "已失败", 4: "已过期" },
  }),
  onLoad(o) {
    this.dishId = o.dishId || "";
    this.id = o.id || o.grouponId || "";
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        if (this.id) {
          this.group = (await getGrouponDetail(this.id)).data;
          this.dishId = this.group.dishId;
        }
        if (!this.dishId) throw new Error("缺少商品或拼团标识");
        this.dish = (await getDishDetail(this.dishId)).data;
        if (!this.id) {
          this.groups = (await getJoinableGroupons(this.dishId)).data || [];
        }
      } catch (e) {
        this.error = e.message || "读取失败";
      } finally {
        this.loading = false;
      }
    },
    open() {
      uni.navigateTo({
        url:
          "/pages/groupon/confirm?mode=open&dishId=" +
          encodeURIComponent(this.dishId),
      });
    },
    join(g) {
      uni.navigateTo({
        url:
          "/pages/groupon/confirm?mode=join&dishId=" +
          encodeURIComponent(this.dishId) +
          "&grouponId=" +
          encodeURIComponent(g.id),
      });
    },
    single() {
      uni.navigateTo({
        url: "/pages/order/confirm?dishId=" + encodeURIComponent(this.dishId),
      });
    },
    order(id) {
      uni.navigateTo({
        url: "/pages/order/detail?id=" + encodeURIComponent(id),
      });
    },
  },
};
</script>
