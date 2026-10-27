<template>
  <main class="fresh-not-found">
    <img src="/branding/fresh-mark.svg" width="64" height="64" alt="社区鲜选" />
    <p class="fresh-not-found__code">404</p>
    <h1>页面未找到</h1>
    <p class="fresh-not-found__hint">地址可能已变更，请返回工作台继续操作。</p>
    <router-link to="/dashboard">返回工作台</router-link>
  </main>
</template>
<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
@Component({ name: 'Page404' })
export default class extends Vue {}
</script>
<style scoped>
.fresh-not-found { min-height: 100vh; box-sizing: border-box; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 32px; text-align: center; background: #f4f7f4; color: #213c2b; }
.fresh-not-found__code { margin: 24px 0 0; font-size: 64px; font-weight: 700; color: #27744d; }
.fresh-not-found h1 { margin: 12px 0; font-size: 26px; }
.fresh-not-found__hint { color: #65766a; line-height: 1.6; }
.fresh-not-found a { margin-top: 20px; padding: 13px 28px; border-radius: 8px; background: #27744d; color: white; text-decoration: none; }
.fresh-not-found a:focus-visible { outline: 3px solid #86b99a; outline-offset: 4px; }
</style>
