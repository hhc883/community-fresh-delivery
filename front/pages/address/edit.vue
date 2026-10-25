<template>
  <view class="core"
    ><text class="title">{{ id ? "编辑地址" : "新增地址" }}</text
    ><view v-if="error" class="error">{{ error }}</view
    ><view class="card stack"
      ><input
        class="field"
        v-model="form.consignee"
        maxlength="32"
        placeholder="收货人姓名" /><input
        class="field"
        v-model="form.phone"
        type="number"
        maxlength="11"
        placeholder="11 位手机号" /><input
        class="field"
        v-model="form.provinceName"
        maxlength="32"
        placeholder="省 / 直辖市" /><input
        class="field"
        v-model="form.cityName"
        maxlength="32"
        placeholder="城市" /><input
        class="field"
        v-model="form.districtName"
        maxlength="32"
        placeholder="区 / 县" /><textarea
        class="field"
        v-model="form.detail"
        maxlength="200"
        placeholder="街道、门牌号及楼层" /><input
        class="field"
        v-model="form.label"
        maxlength="16"
        placeholder="地址标签，如家或公司" /></view
    ><text class="sub">保存后可在地址列表设为默认地址。</text
    ><button
      class="primary"
      :loading="saving"
      :disabled="saving || loading"
      @tap="save"
    >
      保存地址
    </button></view
  >
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
