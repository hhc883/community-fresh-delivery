<template>
  <view class="person-page">
    <view class="form-card">
      <view class="form-row">
        <text class="form-label">提货人</text>
        <input class="form-input" v-model="name" placeholder="请输入提货人姓名" />
      </view>
      <view class="form-row">
        <text class="form-label">手机号</text>
        <input class="form-input" v-model="phone" type="number" maxlength="11" placeholder="请输入提货手机号" />
      </view>
      <view class="form-row" @click="goPickup">
        <text class="form-label">所在自提点</text>
        <view class="form-val-arrow">
          <text class="val-text">{{ pickupPoint.name || '请选择所在自提点' }}</text>
          <text class="arrow">›</text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">详细地址</text>
        <input class="form-input" v-model="detailAddr" placeholder="例：15号楼架空层 / 门牌号" />
      </view>
      <view class="form-row switch-row">
        <text class="form-label">设为默认提货人</text>
        <switch :checked="isDefault" color="#25B873" @change="isDefault = $event.detail.value" />
      </view>
    </view>

    <view class="btn-box">
      <button class="btn-save" @click="save">保存</button>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'

export default {
  data() {
    return {
      name: '张三',
      phone: '13800138000',
      detailAddr: '15号',
      isDefault: true
    }
  },
  computed: {
    ...mapState(['pickupPoint', 'pickupPerson'])
  },
  onLoad() {
    if (this.pickupPerson) {
      if (this.pickupPerson.name) this.name = this.pickupPerson.name
      if (this.pickupPerson.phone) this.phone = this.pickupPerson.phone
    }
  },
  methods: {
    goPickup() {
      uni.navigateTo({ url: '/pages/pickup/pickup' })
    },
    save() {
      if (!this.name.trim() || !this.phone.trim()) {
        uni.showToast({ title: '姓名和手机号不能为空', icon: 'none' })
        return
      }
      this.$store.commit('setPickupPerson', {
        name: this.name.trim(),
        phone: this.phone.trim()
      })
      uni.showToast({ title: '已保存提货人信息', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 800)
    }
  }
}
</script>

<style lang="scss" scoped>
.person-page {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 24rpx;
  box-sizing: border-box;
}

.form-card {
  background-color: #FFFFFF;
  border-radius: 20rpx;
  padding: 0 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 104rpx;
  border-bottom: 1rpx solid #F7F8FA;
  font-size: 28rpx;
}

.form-row:last-child {
  border-bottom: none;
}

.form-label {
  width: 180rpx;
  color: #1F2329;
  font-weight: 500;
}

.form-input {
  flex: 1;
  text-align: right;
  color: #1F2329;
}

.form-val-arrow {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.val-text {
  color: #4E5969;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 440rpx;
}

.arrow {
  color: #C9CDD4;
  font-size: 30rpx;
  margin-left: 10rpx;
}

.switch-row {
  height: 110rpx;
}

.btn-box {
  margin-top: 60rpx;
}

.btn-save {
  background-color: #25B873;
  color: #FFFFFF;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  font-weight: 700;
  box-shadow: 0 6rpx 18rpx rgba(37, 184, 115, 0.3);
}
</style>
