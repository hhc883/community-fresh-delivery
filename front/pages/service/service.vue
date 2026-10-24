<template>
  <view class="service-page">
    <!-- 顶部联系方式双圆形入口卡片 -->
    <view class="top-contact-card">
      <view class="circle-buttons-row">
        <!-- 平台电话 -->
        <view class="contact-circle-item" @click="makeCall">
          <view class="green-circle-icon">
            <text class="icon-sym">📞</text>
          </view>
          <text class="circle-label">平台电话</text>
        </view>

        <!-- 在线客服 -->
        <view class="contact-circle-item" @click="openChat">
          <view class="green-circle-icon">
            <text class="icon-sym">💬</text>
          </view>
          <text class="circle-label">在线客服</text>
        </view>
      </view>

      <!-- 服务时间 -->
      <text class="service-time-text">服务时间 09:00 - 21:00</text>
    </view>

    <!-- 灰色分隔标题：常见问题 -->
    <view class="faq-section-title">
      <text>常见问题</text>
    </view>

    <!-- 常见问题列表卡片 -->
    <view class="faq-list">
      <view
        class="faq-item"
        v-for="(item, idx) in faqList"
        :key="idx"
        @click="toggleFaq(idx)"
      >
        <view class="faq-q">
          <text class="q-title">{{ item.q }}</text>
          <text class="arrow">{{ openIndex === idx ? '▲' : '›' }}</text>
        </view>
        <view class="faq-a" v-if="openIndex === idx">
          <text class="a-content">{{ item.a }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      openIndex: -1,
      faqList: [
        {
          q: '如何前往自提点取货？',
          a: '下单成功后，在“我的订单”中查看8位自提码或提货凭证。商品送达自提点后，前往自提网点向团长出示提货码即可验货领取。'
        },
        {
          q: '生鲜商品有坏损如何处理？',
          a: '我们郑重承诺“坏损包赔”与“极速退款”。提货时或提货后24小时内，在订单详情点击“申请售后”，上传商品实拍照片，秒级审核退款。'
        },
        {
          q: '自提网点的营业时间是？',
          a: '各社区网点营业时间一般为每日 07:00 至 21:30，建议您在预约的时段前往取货以获得最佳保鲜履约服务。'
        },
        {
          q: '如何修改提货人信息？',
          a: '在下单页面点击“提货人”或进入“我的 - 个人资料”即可随时更新提货人姓名与手机号。'
        }
      ]
    }
  },
  methods: {
    toggleFaq(idx) {
      this.openIndex = this.openIndex === idx ? -1 : idx
    },
    makeCall() {
      uni.makePhoneCall({
        phoneNumber: '4008886688'
      })
    },
    openChat() {
      uni.showModal({
        title: '在线客服',
        content: '人工客服当前繁忙，服务时间为 09:00 - 21:00，您可以直接拨打平台电话咨询！',
        showCancel: false,
        confirmColor: '#25B873'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.service-page {
  min-height: 100vh;
  background-color: #F7F8FA;
}

/* 顶部联系方式双圆形入口卡片 */
.top-contact-card {
  background-color: #FFFFFF;
  padding: 50rpx 0 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  .circle-buttons-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 120rpx;
    margin-bottom: 28rpx;

    .contact-circle-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .green-circle-icon {
        width: 120rpx;
        height: 120rpx;
        border-radius: 50%;
        border: 4rpx solid #25B873;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16rpx;
        background-color: #FFFFFF;

        .icon-sym {
          font-size: 52rpx;
          line-height: 1;
        }
      }

      .circle-label {
        font-size: 26rpx;
        color: #1F2329;
        font-weight: 500;
      }
    }
  }

  .service-time-text {
    font-size: 24rpx;
    color: #8F959E;
  }
}

/* 灰色分隔条标题 */
.faq-section-title {
  padding: 24rpx 30rpx 16rpx;

  text {
    font-size: 26rpx;
    color: #8F959E;
  }
}

/* 常见问题列表 */
.faq-list {
  background-color: #FFFFFF;
  padding: 0 30rpx;

  .faq-item {
    border-bottom: 1rpx solid #F0F2F5;

    .faq-q {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 96rpx;

      .q-title {
        font-size: 28rpx;
        color: #1F2329;
      }

      .arrow {
        font-size: 32rpx;
        color: #C9CDD4;
      }
    }

    .faq-a {
      padding: 0 0 24rpx;

      .a-content {
        font-size: 26rpx;
        color: #666666;
        line-height: 40rpx;
      }
    }
  }
}
</style>
