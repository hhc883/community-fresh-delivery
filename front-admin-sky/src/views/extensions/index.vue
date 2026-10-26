<template>
  <iframe
    :key="page"
    class="business-extension"
    :src="src"
    title="生鲜业务管理"
  />
</template>
<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import { getToken } from "@/utils/cookies";
@Component
export default class extends Vue {
  get page() {
    return this.$route.meta.extension || "groupon";
  }
  get src() {
    const token = getToken();
    if (token) localStorage.setItem("admin_token", token);
    return "/extensions/" + this.page + "?embedded=1";
  }
}
</script>
<style scoped>
.business-extension {
  display: block;
  width: 100%;
  height: calc(100vh - 100px);
  border: 0;
  background: #f5f6f8;
}
</style>
