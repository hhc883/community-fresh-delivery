// Vite 配置
// 端口钉死 5177 + strictPort：5173-5176 留给其他端（C 端 / 商家端 / 自提点端 + 顺延位），
// 同跑时谁先起谁占前面的口，本端必须钉死 5177 才不会顺延到未登记的端口上。
//
// 刻意 **不配 server.proxy**：backEnd/（sky，8080）的 CORS 配置为 allowedOriginPatterns("*")，
// 前端直连 BASE_URL（见 src/api/index.js，真源只有那一个常量）。
// 配了 proxy 就等于同一份后端地址存在两处真值（vite.config.js 与 src/api/index.js），
// 多一种通道就多一份漂移面。
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5177,
    strictPort: true
  }
})
