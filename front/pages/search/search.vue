<template>
  <view class="page-container"
    ><view class="nav-bar"
      ><view class="nav-back" @tap="back"
        ><text class="nav-back-arrow">‹</text></view
      ><view class="nav-search-box"
        ><view class="nav-search-icon" /><input
          class="nav-search-input"
          v-model="keyword"
          :focus="true"
          placeholder="输入商品名称"
          confirm-type="search"
          @confirm="search"
        /><view class="nav-search-divider" /><view
          class="nav-search-go"
          @tap="search"
          ><text class="nav-search-go-text">搜索</text></view
        ></view
      ></view
    >
    <view class="page-body"
      ><view v-if="history.length" class="ss-block"
        ><view class="ss-head"
          ><text class="ss-title">最近搜索</text
          ><text @tap="clearHistory">清空</text></view
        ><view class="ss-chips"
          ><view
            class="ss-chip"
            v-for="word in history"
            :key="word"
            @tap="selectWord(word)"
            ><text class="ss-chip-text">{{ word }}</text></view
          ></view
        ></view
      >
      <view v-if="loading" class="empty">正在搜索…</view
      ><view v-else-if="error" class="error" @tap="search"
        >{{ error }} · 点击重试</view
      ><view v-else-if="searched && !rows.length" class="empty"
        >没有找到相关商品，换个关键词试试</view
      >
      <view v-if="rows.length" class="ss-block"
        ><view class="ss-head"
          ><text class="ss-title">{{
            searched ? "搜索结果" : "在售好物"
          }}</text></view
        ><view class="rank-item" v-for="dish in rows" :key="dish.id"
          ><view class="rank-img" @tap="detail(dish)"
            ><image
              v-if="dish.image"
              class="rank-photo"
              :src="dish.image"
              mode="aspectFill"
            /><text v-else>暂无图片</text></view
          ><view class="rank-body"
            ><text class="rank-name" @tap="detail(dish)">{{ dish.name }}</text
            ><text class="rank-sold"
              >{{ dish.unit || "份" }} · 库存 {{ dish.stock }}</text
            ><view class="rank-bottom"
              ><view class="rank-price-line"
                ><text class="rank-symbol">¥</text
                ><text class="rank-price">{{
                  $fmtMoney(dish.price)
                }}</text></view
              ><button
                class="add-button"
                :disabled="Number(dish.stock) <= 0"
                @tap="add(dish)"
              >
                ＋
              </button></view
            ></view
          ></view
        ></view
      ></view
    ></view
  >
</template>
<script>
import { getDishes } from "../../api/index.js";
export default {
  data: () => ({
    keyword: "",
    rows: [],
    history: [],
    loading: false,
    error: "",
    searched: false,
  }),
  onLoad() {
    try {
      const raw = uni.getStorageSync("search_history");
      const list = typeof raw === "string" ? JSON.parse(raw) : raw;
      this.history = Array.isArray(list)
        ? list.filter((x) => typeof x === "string").slice(0, 10)
        : [];
    } catch {}
    this.load();
  },
  methods: {
    back() {
      uni.navigateBack({
        fail: () => uni.switchTab({ url: "/pages/index/index" }),
      });
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const r = await getDishes({
          keyword: this.keyword.trim() || undefined,
        });
        this.rows = Array.isArray(r.data) ? r.data : [];
      } catch (e) {
        this.error = e.message || "搜索失败";
      } finally {
        this.loading = false;
      }
    },
    search() {
      const word = this.keyword.trim();
      this.searched = !!word;
      if (word) {
        this.history = [word, ...this.history.filter((w) => w !== word)].slice(
          0,
          10,
        );
        try {
          uni.setStorageSync("search_history", this.history);
        } catch {}
      }
      this.load();
    },
    selectWord(word) {
      this.keyword = word;
      this.search();
    },
    clearHistory() {
      uni.showModal({
        title: "清空搜索历史",
        content: "确定清空当前设备的搜索记录？",
        success: (r) => {
          if (r.confirm) {
            this.history = [];
            try {
              uni.removeStorageSync("search_history");
            } catch {}
          }
        },
      });
    },
    detail(d) {
      uni.navigateTo({
        url: "/pages/detail/detail?id=" + encodeURIComponent(d.id),
      });
    },
    add(d) {
      this.$store.commit("addToCart", d);
    },
  },
};
</script>
<style scoped>
/* ==================== 独立搜索页 — 多多买菜风格（购物车/商品卡样式复制自首页，scoped 隔离同名类不冲突） ==================== */

.page-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* ==================== 顶部白色导航（fixed 常驻：< 返回 + 浅灰描边圆角搜索框 + 红字"搜索"；底部极浅灰发丝线分隔） ==================== */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  padding-top: calc(16rpx + env(safe-area-inset-top));
  background-color: #ffffff;
  border-bottom: 1rpx solid #f0f0f0;
}
.nav-back {
  flex-shrink: 0;
  width: 48rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-back-arrow {
  font-size: 44rpx;
  font-weight: 700;
  color: #333333;
  line-height: 1;
}
/* 浅灰描边圆角胶囊：高 64rpx 圆角 40rpx（border-box 描边不增高），内排 红放大镜 + input(flex:1) + 竖分隔线 + 红字"搜索" */
.nav-search-box {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 10rpx;
  height: 64rpx;
  padding: 0 24rpx;
  background-color: #ffffff;
  border: 1rpx solid #eeeeee;
  border-radius: 40rpx;
  overflow: hidden;
}
/* 红色放大镜：CSS 绘制（红圆环 + 45° 手柄），白底上比 emoji 醒目且各端渲染一致 */
.nav-search-icon {
  position: relative;
  flex-shrink: 0;
  width: 24rpx;
  height: 24rpx;
  box-sizing: border-box;
  border: 4rpx solid #e02020;
  border-radius: 50%;
}
.nav-search-icon::after {
  content: "";
  position: absolute;
  right: -6rpx;
  bottom: -5rpx;
  width: 12rpx;
  height: 4rpx;
  background-color: #e02020;
  border-radius: 2rpx;
  transform: rotate(45deg);
}
.nav-search-input {
  flex: 1;
  min-width: 0;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
  color: #333333;
}
.nav-search-divider {
  flex-shrink: 0;
  width: 1rpx;
  height: 32rpx;
  background-color: #eeeeee;
}
.nav-search-go {
  flex-shrink: 0;
  align-self: stretch;
  display: flex;
  align-items: center;
  padding: 0 20rpx;
}
.nav-search-go-text {
  font-size: 28rpx;
  font-weight: 700;
  color: #e02020;
}

/* 内容区：顶部让位 fixed 导航（96rpx + 安全区 + 16rpx 间距）；底部默认零留白（空车无购物车栏时最后一张榜单卡紧贴页底） */
.page-body {
  padding: calc(112rpx + env(safe-area-inset-top)) 0 0;
}
/* 购物车栏在场（非空车，与 cart-bar 的 v-if 同源）时底部留出栏高 120rpx + 20rpx 呼吸，避免固定栏遮住最后内容 */
.page-body-with-bar {
  padding-bottom: 140rpx;
}

/* ==================== 发现页三大块通用（白卡圆角块） ==================== */
.ss-block {
  background-color: #ffffff;
  border-radius: 16rpx;
  margin: 20rpx 24rpx 0;
  padding: 24rpx;
}
.ss-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}
.ss-title-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.ss-title-icon {
  font-size: 26rpx;
}
.ss-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #333333;
}
.ss-trash {
  font-size: 30rpx;
  padding: 4rpx 8rpx;
  line-height: 1;
}
.ss-refresh {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 0;
}
.ss-refresh-icon {
  font-size: 24rpx;
}
.ss-refresh-text {
  font-size: 22rpx;
  color: #999999;
}

/* 胶囊组通用 */
.ss-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
/* a. 最近搜索词胶囊 */
.ss-chip {
  max-width: 100%;
  box-sizing: border-box;
  background-color: #f5f5f5;
  border-radius: 28rpx;
  padding: 10rpx 26rpx;
}
.ss-chip-text {
  font-size: 24rpx;
  color: #333333;
  display: inline-block;
  max-width: 420rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* b. 搜索发现词胶囊：emoji + 词 + 可选红字"热"角标 */
.sd-chip {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6rpx;
  background-color: #f5f5f5;
  border-radius: 28rpx;
  padding: 10rpx 24rpx;
}
.sd-chip-emoji {
  font-size: 24rpx;
}
.sd-chip-text {
  font-size: 24rpx;
  color: #333333;
  display: inline-block;
  max-width: 320rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 红字"热"角标：右上角小圆牌（带小尾角，多多买菜样式） */
.sd-chip-hot {
  position: absolute;
  top: -10rpx;
  right: -8rpx;
  min-width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  box-sizing: border-box;
  padding: 0 6rpx;
  background-color: #e02020;
  color: #ffffff;
  font-size: 16rpx;
  font-weight: 700;
  text-align: center;
  border-radius: 14rpx 14rpx 14rpx 2rpx;
}

/* ==================== c. 地区热卖榜单（横向滑动两张 85vw 榜单卡） ==================== */
.ss-rank {
  padding: 24rpx 0 0;
} /* 底部留白收敛：榜单卡下不再垫 24rpx 白边，滚动到底时卡紧贴页底 */
.ss-rank .ss-head {
  padding: 0 24rpx;
}
.rank-scroll {
  white-space: nowrap;
  cursor: grab;
} /* PC 鼠标拖拽手型提示（触屏不受影响） */
/* 隐藏横向滚动条（与首页秒杀区同款处理） */
.rank-scroll ::v-deep .uni-scroll-view::-webkit-scrollbar {
  display: none;
}
.rank-card {
  display: inline-block;
  width: 85vw;
  margin-left: 24rpx;
  box-sizing: border-box;
  background-color: #ffffff;
  border: 1rpx solid #eeeeee;
  border-radius: 16rpx;
  vertical-align: top;
  white-space: normal;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.rank-card:last-child {
  margin-right: 24rpx;
}
.rank-card-head {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 20rpx 24rpx;
  min-height: 48rpx;
  box-sizing: border-box;
}
.rank-card-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #333333;
}
.rank-card-sep {
  font-size: 24rpx;
  color: #dddddd;
}
.rank-card-sub {
  font-size: 22rpx;
  color: #999999;
}
/* 榜单 item：排名角标 + 图 + 标题/已售/价格 + 加购按钮 */
.rank-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  border-top: 1rpx solid #f7f7f7;
}
/* 排名角标：1 金 2 银 3 铜，4+ 灰 */
.rank-no {
  flex-shrink: 0;
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  border-radius: 8rpx;
  background-color: #f0f0f0;
  color: #cccccc;
  font-size: 20rpx;
  font-weight: 700;
  text-align: center;
}
.rank-no-1 {
  background-color: #ffb800;
  color: #ffffff;
}
.rank-no-2 {
  background-color: #bfc5cd;
  color: #ffffff;
}
.rank-no-3 {
  background-color: #d2955c;
  color: #ffffff;
}
.rank-img {
  flex-shrink: 0;
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rank-photo {
  width: 100%;
  height: 100%;
  display: block;
}
.rank-emoji {
  font-size: 56rpx;
}
.rank-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.rank-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank-sold {
  font-size: 20rpx;
  color: #999999;
}
.rank-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.rank-price-line {
  display: flex;
  align-items: baseline;
}
.rank-symbol {
  font-size: 20rpx;
  font-weight: 700;
  color: #e02020;
}
.rank-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #e02020;
  line-height: 1;
}
.rank-action {
  flex-shrink: 0;
}
.rank-add {
  min-width: 132rpx;
  height: 52rpx;
  padding: 0 18rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff5a3c, #e02020);
  border-radius: 26rpx;
}
.rank-add text {
  font-size: 20rpx;
  font-weight: 700;
  color: #ffffff;
}
.rank-empty {
  padding: 40rpx 0;
  text-align: center;
}
.rank-empty text {
  font-size: 24rpx;
  color: #cccccc;
}

/* ==================== 榜单加载失败错误态 ==================== */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  padding: 120rpx 0;
}
.error-text {
  font-size: 26rpx;
  color: #999999;
}
.error-retry {
  font-size: 26rpx;
  font-weight: 600;
  color: #e02020;
  border: 1rpx solid #e02020;
  border-radius: 32rpx;
  padding: 10rpx 44rpx;
}

/* ==================== 搜索结果（同首页商品卡样式） ==================== */
.search-result-head {
  display: flex;
  align-items: center;
  padding: 24rpx 24rpx 8rpx;
}
.srh-text {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: #666666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.srh-cancel {
  flex-shrink: 0;
  margin-left: 16rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #e02020;
  padding: 6rpx 8rpx;
}
.dish-list {
  padding: 0 24rpx;
}
.dish-card {
  display: flex;
  background-color: #ffffff;
  border-radius: 12rpx;
  border: 1rpx solid #eeeeee;
  box-sizing: border-box;
  padding: 16rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.dish-img {
  width: 240rpx;
  height: 240rpx;
  flex-shrink: 0;
  border-radius: 8rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dish-emoji {
  font-size: 100rpx;
}
.dish-photo {
  width: 100%;
  height: 100%;
  display: block;
}
.dish-body {
  flex: 1;
  min-width: 0;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
}
.dish-title-row {
  display: flex;
  align-items: center;
  min-width: 0;
}
.dish-seckill-tag {
  flex-shrink: 0;
  margin-right: 8rpx;
  font-size: 18rpx;
  font-weight: 700;
  color: #ffffff;
  background-color: #e02020;
  border-radius: 4rpx;
  padding: 0 8rpx;
  line-height: 30rpx;
}
.dish-name {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  font-weight: 600;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dish-tags {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #999999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dish-sold-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 8rpx;
}
.dish-rush {
  font-size: 20rpx;
  font-weight: 600;
  color: #e02020;
  flex-shrink: 0;
}
.dish-sold {
  font-size: 20rpx;
  color: #999999;
}
.dish-promo-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 8rpx;
  min-height: 30rpx;
}
.tag-cut {
  font-size: 18rpx;
  color: #e02020;
  font-weight: 600;
  border: 1rpx solid #e02020;
  border-radius: 6rpx;
  padding: 0 8rpx;
  line-height: 26rpx;
}
.tag-limit {
  font-size: 18rpx;
  color: #999999;
  border: 1rpx solid #dddddd;
  border-radius: 6rpx;
  padding: 0 8rpx;
  line-height: 26rpx;
}
.dish-rate {
  font-size: 20rpx;
  color: #999999;
}
.dish-price-row {
  margin-top: auto;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
.price-left {
  display: flex;
  align-items: baseline;
  min-width: 0;
}
.price-seckill-label {
  font-size: 20rpx;
  font-weight: 700;
  color: #e02020;
  margin-right: 4rpx;
}
.price-symbol {
  font-size: 20rpx;
  font-weight: 700;
  color: #e02020;
}
.price-big {
  font-size: 38rpx;
  font-weight: 700;
  color: #e02020;
  line-height: 1;
}
.dish-action {
  flex-shrink: 0;
  margin-left: 16rpx;
}
.add-btn {
  min-width: 160rpx;
  height: 56rpx;
  padding: 0 22rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff5a3c, #e02020);
  border-radius: 28rpx;
}
.add-btn text {
  font-size: 22rpx;
  font-weight: 700;
  color: #ffffff;
}
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}
.empty-state text {
  font-size: 26rpx;
  color: #cccccc;
}

/* ==================== 步进器（榜单/结果列表/购物车弹层共用，与首页同款） ==================== */
.stepper {
  display: flex;
  align-items: center;
}
.step-btn {
  width: 46rpx;
  height: 46rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-btn text {
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1;
}
.step-minus {
  border: 1rpx solid #e02020;
  background-color: #ffffff;
}
.step-minus text {
  color: #e02020;
}
.step-plus {
  background-color: #e02020;
}
.step-plus text {
  color: #ffffff;
}
.step-num {
  min-width: 56rpx;
  text-align: center;
  font-size: 26rpx;
  font-weight: 700;
  color: #333333;
}

/* ==================== 底部购物车栏（复刻首页 cart-bar：🛒+角标 | 价格 | 去支付） ==================== */
.cart-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  background-color: #ffffff;
  display: flex;
  align-items: stretch;
  overflow: hidden;
  box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.08);
  z-index: 100;
}
.cart-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 16rpx 0 24rpx;
  flex: 2;
  min-width: 0;
}
.cart-icon-box {
  position: relative;
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cart-icon {
  font-size: 44rpx;
  line-height: 1;
}
.cart-badge {
  position: absolute;
  top: -4rpx;
  right: -4rpx;
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 6rpx;
  box-sizing: border-box;
  background-color: #e02020;
  border-radius: 15rpx;
  font-size: 20rpx;
  color: #ffffff;
  text-align: center;
  line-height: 30rpx;
}
.cart-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4rpx;
  margin-left: auto;
  min-width: 0;
}
.cart-price-line {
  display: flex;
  align-items: baseline;
}
.cart-total-symbol {
  font-size: 26rpx;
  color: #e02020;
  font-weight: 700;
}
.cart-total-price {
  font-size: 40rpx;
  font-weight: 700;
  color: #e02020;
  line-height: 1;
}
.cart-reduced {
  font-size: 20rpx;
  color: #999999;
}
/* uniapp button 覆盖默认样式（去边框/圆角/内边距，::after 伪元素边框一并去掉） */
.btn-pay {
  flex: 3;
  min-width: 0;
  margin: 0;
  background-color: #e02e24;
  color: #ffffff;
  border: none;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  padding: 16rpx;
  line-height: 1.3;
}
.btn-pay::after {
  border: none;
}
.pay-main {
  font-size: 30rpx;
  font-weight: 700;
  color: #ffffff;
}
.pay-sub-wrap {
  display: flex;
  align-items: center;
}
.pay-coupon {
  font-size: 22rpx;
  background-color: #ffd84d;
  color: #c23a0f;
  border-radius: 8rpx;
  padding: 2rpx 10rpx;
  font-weight: 600;
}

/* ==================== 弹窗通用 ==================== */
.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.popup-mask-center {
  align-items: center;
  z-index: 300;
}

/* ==================== 购物车弹层（复刻首页 cart-popup，80vh） ==================== */
.cart-popup {
  width: 100%;
  background-color: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.service-bar {
  flex-shrink: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  background-color: #f5f5f5;
  padding: 14rpx 24rpx;
}
.service-item {
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: 20rpx;
  color: #666666;
}
.service-check {
  color: #07c160;
  font-weight: 700;
}
.pickup-row {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 20rpx 24rpx 4rpx;
}
.pr-icon {
  font-size: 28rpx;
  flex-shrink: 0;
}
.pr-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pr-phone {
  font-size: 24rpx;
  color: #666666;
}
.pr-arrow {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 30rpx;
  color: #cccccc;
}
.cart-title-row {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx 4rpx;
}
.cart-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #333333;
}
.cart-title-right {
  display: flex;
  align-items: center;
  gap: 28rpx;
}
.cart-clear {
  font-size: 24rpx;
  font-weight: 600;
  color: #666666;
}
.cart-edit {
  font-size: 24rpx;
  font-weight: 600;
  color: #e02020;
}
.cart-close {
  font-size: 32rpx;
  color: #cccccc;
}
.helper-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 12rpx 24rpx 0;
  padding: 10rpx 20rpx;
  background-color: #fff7f0;
  border-radius: 10rpx;
}
.hb-title {
  font-size: 22rpx;
  font-weight: 700;
  color: #e02020;
}
.hb-divider {
  font-size: 22rpx;
  color: #f0d8c0;
}
.hb-text {
  font-size: 22rpx;
  color: #e0662a;
}
.cart-items {
  flex: 1;
  min-height: 0;
  padding: 0 24rpx;
  box-sizing: border-box;
}
.cart-items-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.check-circle {
  width: 36rpx;
  height: 36rpx;
  flex-shrink: 0;
  border: 2rpx solid #dddddd;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
}
.check-circle.checked {
  border: none;
  background-color: #e02020;
}
.check-circle text {
  font-size: 20rpx;
  font-weight: 700;
  color: #ffffff;
  line-height: 1;
}
.ci-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.ci-img {
  width: 120rpx;
  height: 120rpx;
  flex-shrink: 0;
  border-radius: 8rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ci-img text {
  font-size: 56rpx;
}
.ci-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.ci-name-row {
  display: flex;
  align-items: center;
  min-width: 0;
}
.ci-seckill-tag {
  flex-shrink: 0;
  margin-right: 8rpx;
  font-size: 16rpx;
  font-weight: 700;
  color: #ffffff;
  background-color: #e02020;
  border-radius: 4rpx;
  padding: 0 6rpx;
  line-height: 26rpx;
}
.ci-name {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  font-weight: 600;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ci-desc-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  overflow: hidden;
  white-space: nowrap;
}
.ci-restore {
  font-size: 20rpx;
  font-weight: 600;
  color: #e02020;
  flex-shrink: 0;
}
.ci-sold {
  font-size: 20rpx;
  color: #999999;
  flex-shrink: 0;
}
.ci-limit {
  font-size: 20rpx;
  color: #999999;
}
.ci-price-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}
.ci-original {
  font-size: 20rpx;
  color: #bbbbbb;
  text-decoration: line-through;
}
.ci-price-line {
  display: flex;
  align-items: baseline;
}
.ci-symbol {
  font-size: 18rpx;
  font-weight: 700;
  color: #e02020;
}
.ci-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #e02020;
  line-height: 1;
}
.ci-discount {
  font-size: 18rpx;
  font-weight: 600;
  color: #e02020;
  background-color: #ffece8;
  border-radius: 6rpx;
  padding: 2rpx 8rpx;
}
.ci-action {
  flex-shrink: 0;
}
.ci-delete {
  display: inline-block;
  font-size: 22rpx;
  font-weight: 600;
  color: #e02020;
  border: 1rpx solid #e02020;
  border-radius: 24rpx;
  padding: 6rpx 20rpx;
}
.cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}
.cart-empty-icon {
  font-size: 88rpx;
  opacity: 0.4;
}
.cart-empty-text {
  font-size: 26rpx;
  color: #999999;
}
.cart-total-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #f0f0f0;
}
.check-all {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-shrink: 0;
}
.check-all-text {
  font-size: 24rpx;
  color: #333333;
}
.ctb-price {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2rpx;
  margin-right: 16rpx;
}
.ctb-price-line {
  display: flex;
  align-items: baseline;
}
.ctb-price-num {
  font-size: 36rpx;
  font-weight: 700;
  color: #e02020;
  line-height: 1;
}
.ctb-saving {
  font-size: 20rpx;
  color: #999999;
}
/* uniapp button 覆盖默认样式（含 ::after 伪元素边框） */
.btn-pay-popup {
  flex-shrink: 0;
  margin: 0;
  min-width: 200rpx;
  height: 76rpx;
  line-height: 76rpx;
  background-color: #e02e24;
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  border: none;
  border-radius: 38rpx;
  padding: 0 36rpx;
  text-align: center;
}
.btn-pay-popup::after {
  border: none;
}
/* 空车时支付按钮置灰禁用（点击仍走 payFromCart 的 toast 提示） */
.btn-pay-popup.btn-pay-disabled {
  background-color: #cccccc;
}

/* ==================== 修改提货人弹窗（与首页同款） ==================== */
.person-popup {
  width: 590rpx;
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 40rpx 32rpx 32rpx;
  position: relative;
}
.person-close {
  position: absolute;
  top: 20rpx;
  right: 24rpx;
  font-size: 30rpx;
  color: #cccccc;
  padding: 8rpx;
}
.person-title {
  display: block;
  text-align: center;
  font-size: 30rpx;
  font-weight: 700;
  color: #333333;
  margin-bottom: 28rpx;
}
.btn-save-person {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  margin-top: 8rpx;
  padding: 0;
  background-color: #e02020;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
  border-radius: 44rpx;
}
.btn-save-person::after {
  border: none;
}
.form-group {
  margin-bottom: 24rpx;
}
.form-label {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #666666;
  margin-bottom: 12rpx;
}
.form-input {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background-color: #f5f5f5;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333333;
  box-sizing: border-box;
}
</style>

<style scoped>
.add-button {
  margin: 0;
  padding: 0 20rpx;
  border-radius: 50%;
  background: #25b873;
  color: white;
}
.rank-price,
.rank-symbol,
.nav-search-go-text {
  color: #25b873;
}
.nav-search-icon {
  border-color: #25b873;
}
.page-container {
  background: #f7f8fa;
}
</style>
