<template>
  <view class="core">
    <view class="address-edit-header">
      <text class="title">{{ id ? "编辑收货地址" : "新增收货地址" }}</text>
      <text class="sub">用于生鲜配送到家履约，请填写真实有效的联系方式与门牌地址。</text>
    </view>

    <view v-if="error" class="error">{{ error }}</view>

    <view class="card stack form-card">
      <view class="form-item">
        <text class="field-label">收货人</text>
        <input
          class="field form-input"
          v-model="form.consignee"
          maxlength="32"
          placeholder="收货人姓名"
        />
      </view>

      <view class="form-item">
        <text class="field-label">联系电话</text>
        <input
          class="field form-input"
          v-model="form.phone"
          type="number"
          maxlength="11"
          placeholder="11 位手机号码"
        />
      </view>

      <view class="form-item">
        <text class="field-label">省 / 直辖市</text>
        <input
          class="field form-input"
          v-model="form.provinceName"
          maxlength="32"
          placeholder="例如：上海市 / 浙江省"
        />
      </view>

      <view class="form-item">
        <text class="field-label">城市</text>
        <input
          class="field form-input"
          v-model="form.cityName"
          maxlength="32"
          placeholder="例如：上海市 / 杭州市"
        />
      </view>

      <view class="form-item">
        <text class="field-label">区 / 县</text>
        <input
          class="field form-input"
          v-model="form.districtName"
          maxlength="32"
          placeholder="例如：浦东新区 / 西湖区"
        />
      </view>

      <view class="form-item">
        <text class="field-label">详细地址</text>
        <textarea
          class="field form-textarea"
          v-model="form.detail"
          maxlength="200"
          placeholder="街道、小区、楼幢、门牌号及楼层"
        />
      </view>

      <view class="form-item">
        <text class="field-label">地址标签</text>
        <input
          class="field form-input"
          v-model="form.label"
          maxlength="16"
          placeholder="例如：家、公司、学校（选填）"
        />
      </view>
    </view>

    <text class="sub save-tip-sub">保存后可在地址列表设为默认收货地址。</text>

    <button
      class="primary"
      :loading="saving"
      :disabled="saving || loading"
      @tap="save"
    >
      保存地址
    </button>
  </view>
</template>

<script>
import {
  getAddressDetail,
  addAddress,
  updateAddress,
} from "../../api/index.js";
export default {
  data: () => ({
    id: "",
    error: "",
    saving: false,
    loading: false,
    form: {
      consignee: "",
      phone: "",
      sex: "1",
      provinceName: "",
      cityName: "",
      districtName: "",
      detail: "",
      label: "",
    },
  }),
  async onLoad(o) {
    this.id = o.id || "";
    if (this.id) {
      this.loading = true;
      try {
        this.form = (await getAddressDetail(this.id)).data;
      } catch (e) {
        this.error = e.message;
      } finally {
        this.loading = false;
      }
    }
  },
  methods: {
    async save() {
      if (this.saving || this.loading) return;
      this.error = "";
      if (
        !this.form.consignee.trim() ||
        !/^1[3-9]\d{9}$/.test(this.form.phone) ||
        !this.form.provinceName.trim() ||
        !this.form.cityName.trim() ||
        !this.form.districtName.trim() ||
        !this.form.detail.trim()
      ) {
        this.error = "请完整填写姓名、正确手机号、省市区和详细地址";
        return;
      }
      this.saving = true;
      try {
        const data = { ...this.form };
        delete data.userId;
        delete data.isDefault;
        await (this.id
          ? updateAddress({ ...data, id: this.id })
          : addAddress(data));
        uni.showToast({ title: "地址已保存", icon: "success" });
        uni.navigateBack();
      } catch (e) {
        this.error = e.message || "保存失败";
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>

<style scoped>
.address-edit-header {
  margin-bottom: 20rpx;
}

.form-card {
  padding: 24rpx;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.field-label {
  font-size: 24rpx;
  color: #556257;
  font-weight: 500;
  margin-bottom: 4rpx;
}

.form-input {
  margin: 6rpx 0 16rpx;
}

.form-textarea {
  margin: 6rpx 0 16rpx;
}

.save-tip-sub {
  display: block;
  text-align: center;
  margin: 16rpx 0 24rpx;
}
</style>
