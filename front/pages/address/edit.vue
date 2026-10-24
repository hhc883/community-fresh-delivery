<template>
  <view class="edit-container">
    <view class="form-card">
      <view class="form-item">
        <text class="label">收货人</text>
        <input
          class="input"
          type="text"
          v-model="form.consignee"
          placeholder="请填写收货人姓名"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <text class="label">性别</text>
        <view class="sex-radio-group">
          <view
            :class="['sex-tag', form.sex === '1' || form.sex === 1 ? 'active' : '']"
            @tap="form.sex = 1"
          >
            先生
          </view>
          <view
            :class="['sex-tag', form.sex === '0' || form.sex === 0 ? 'active' : '']"
            @tap="form.sex = 0"
          >
            女士
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">手机号码</text>
        <input
          class="input"
          type="number"
          maxlength="11"
          v-model="form.phone"
          placeholder="请填写11位手机号码"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <text class="label">所在地区</text>
        <picker mode="region" @change="handleRegionChange" :value="regionValue">
          <view class="picker-text" :class="{ 'placeholder': !hasRegion }">
            {{ regionText }}
          </view>
        </picker>
      </view>

      <view class="form-item no-border">
        <text class="label">详细地址</text>
        <textarea
          class="textarea"
          v-model="form.detail"
          placeholder="街道门牌、楼层房间号等信息"
          placeholder-class="placeholder"
          auto-height
        />
      </view>
    </view>

    <!-- 标签与默认设置 -->
    <view class="form-card extra-card">
      <view class="form-item">
        <text class="label">标签</text>
        <view class="tag-selector">
          <view
            v-for="t in tagOptions"
            :key="t"
            :class="['tag-item', form.label === t ? 'active' : '']"
            @tap="form.label = (form.label === t ? '' : t)"
          >
            {{ t }}
          </view>
        </view>
      </view>

      <view class="form-item no-border switch-item">
        <view class="switch-info">
          <text class="switch-title">设为默认收货地址</text>
          <text class="switch-desc">每次下单时会默认使用该地址</text>
        </view>
        <switch
          :checked="form.isDefault === 1"
          @change="e => form.isDefault = e.detail.value ? 1 : 0"
          color="#25B873"
        />
      </view>
    </view>

    <!-- 保存按钮 -->
    <view class="save-bar">
      <button class="save-btn" :loading="saving" @tap="handleSave">
        保存并使用
      </button>
    </view>
  </view>
</template>

<script>
import { addAddress, updateAddress, getAddressDetail } from '../../api/index.js'

export default {
  data() {
    return {
      id: null,
      saving: false,
      tagOptions: ['家', '公司', '学校'],
      form: {
        consignee: '',
        sex: 1,
        phone: '',
        provinceCode: '440000',
        provinceName: '广东省',
        cityCode: '440600',
        cityName: '佛山市',
        districtCode: '440606',
        districtName: '顺德区',
        detail: '',
        label: '家',
        isDefault: 0
      }
    }
  },
  computed: {
    hasRegion() {
      return Boolean(this.form.provinceName && this.form.cityName && this.form.districtName)
    },
    regionText() {
      if (this.hasRegion) {
        return `${this.form.provinceName} ${this.form.cityName} ${this.form.districtName}`
      }
      return '请选择省、市、区'
    },
    regionValue() {
      if (this.hasRegion) {
        return [this.form.provinceName, this.form.cityName, this.form.districtName]
      }
      return ['广东省', '佛山市', '顺德区']
    }
  },
  onLoad(options) {
    if (options && options.id) {
      this.id = options.id
      uni.setNavigationBarTitle({ title: '编辑收货地址' })
      this.loadDetail(options.id)
    } else {
      uni.setNavigationBarTitle({ title: '新增收货地址' })
    }
  },
  methods: {
    async loadDetail(id) {
      try {
        const res = await getAddressDetail(id)
        if (res.data) {
          this.form = Object.assign({}, this.form, res.data)
        }
      } catch (err) {
        uni.showToast({ title: err.message || '获取地址信息失败', icon: 'none' })
      }
    },
    handleRegionChange(e) {
      const values = e.detail.value
      if (Array.isArray(values) && values.length >= 3) {
        this.form.provinceName = values[0]
        this.form.cityName = values[1]
        this.form.districtName = values[2]
      }
    },
    async handleSave() {
      const { consignee, phone, detail } = this.form
      if (!consignee || !consignee.trim()) {
        return uni.showToast({ title: '请输入收货人姓名', icon: 'none' })
      }
      if (!phone || !/^1\d{10}$/.test(phone.trim())) {
        return uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
      }
      if (!this.hasRegion) {
        return uni.showToast({ title: '请选择所在地区', icon: 'none' })
      }
      if (!detail || !detail.trim()) {
        return uni.showToast({ title: '请输入详细地址', icon: 'none' })
      }

      this.saving = true
      try {
        if (this.id) {
          await updateAddress(this.form)
          uni.showToast({ title: '保存成功', icon: 'success' })
        } else {
          await addAddress(this.form)
          uni.showToast({ title: '添加成功', icon: 'success' })
        }
        setTimeout(() => {
          uni.navigateBack()
        }, 600)
      } catch (err) {
        uni.showToast({ title: err.message || '保存失败', icon: 'none' })
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.edit-container {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding: 24rpx;
  padding-bottom: 160rpx;
  box-sizing: border-box;
}

.form-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 10rpx 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 24rpx;
}

.form-item {
  display: flex;
  align-items: center;
  min-height: 96rpx;
  border-bottom: 1rpx solid #F0F2F5;
  padding: 12rpx 0;

  &.no-border {
    border-bottom: none;
  }
}

.label {
  width: 160rpx;
  font-size: 28rpx;
  color: #1F2329;
  font-weight: 500;
  flex-shrink: 0;
}

.input {
  flex: 1;
  font-size: 28rpx;
  color: #1D2129;
}

.textarea {
  flex: 1;
  font-size: 28rpx;
  color: #1D2129;
  min-height: 80rpx;
  line-height: 1.5;
}

.placeholder {
  color: #C9CDD4;
}

.picker-text {
  flex: 1;
  font-size: 28rpx;
  color: #1D2129;
}

.sex-radio-group {
  display: flex;
  gap: 20rpx;
}

.sex-tag {
  padding: 8rpx 32rpx;
  border-radius: 30rpx;
  background-color: #F2F3F5;
  color: #4E5969;
  font-size: 26rpx;
  transition: all 0.2s;

  &.active {
    background-color: #E8F8F0;
    color: #25B873;
    font-weight: 500;
    border: 1rpx solid #25B873;
  }
}

.tag-selector {
  display: flex;
  gap: 20rpx;
}

.tag-item {
  padding: 8rpx 28rpx;
  border-radius: 24rpx;
  background-color: #F2F3F5;
  color: #4E5969;
  font-size: 26rpx;

  &.active {
    background-color: #E8F8F0;
    color: #25B873;
    font-weight: 500;
    border: 1rpx solid #25B873;
  }
}

.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
}

.switch-info {
  display: flex;
  flex-direction: column;
}

.switch-title {
  font-size: 28rpx;
  color: #1F2329;
  font-weight: 500;
  margin-bottom: 6rpx;
}

.switch-desc {
  font-size: 22rpx;
  color: #86909C;
}

.save-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  padding: 20rpx 32rpx env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.save-btn {
  background: linear-gradient(135deg, #25B873 0%, #1DA362 100%);
  color: #FFFFFF;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}
</style>
