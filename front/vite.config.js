// Vite 配置（uniapp CLI 构建链，F-53 新增）
// 用途：让 front/ 既能在浏览器里跑 H5（网页版测试），也能编译成微信小程序（mp-weixin）。
//
// 【为什么输入目录是 front/ 自己而不是 src/】
// uniapp CLI 工程默认把源码放在 <root>/src/，而本仓 front/ 沿用 HBuilderX 目录形态
// （App.vue / main.js / manifest.json / pages.json 直接在 front/ 根下）。
// 搬迁到 src/ 会让 README 里「HBuilderX 导入 front/ 目录」的既有用法失效，
// 且页面内大量 `@/` 别名与相对路径都要重算。故用 UNI_INPUT_DIR 把输入目录指回 front/ 根，
// 零搬迁、零路径改动（见 scripts/uni-cli.js：它负责把 UNI_INPUT_DIR 设成绝对路径）。
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    // 端口钉死 5173 + strictPort：5173 是 README 登记的 C 端地址，
    // 且已登记进 backEnd CorsConfig 的 allowedOrigins（5173~5177）。
    // H5 uses same-origin requests; mini-programs use api/config.js.
    proxy: Object.fromEntries(['/api', '/user'].map(prefix => [prefix, {
      target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080'
    }])),
    port: 5173,
    strictPort: true,
    watch: {
      // 【为什么必须忽略这些临时文件】
      // 实测（本机 Windows + Node 24）：编辑器/工具用「写临时文件再原子改名」保存时，
      // 会短暂产生 .xxx.tmpdir/ 目录。Vite 的 chokidar 恰好在其上注册 watcher，
      // 而该临时目录随即被改名/删除 → node:fs 抛 EBUSY 且 FSWatcher 未捕获，
      // 整个 dev server 直接崩掉（实测崩过两次，报错形如
      //   Error: EBUSY: resource busy or locked, watch '...\.avatar.js.<pid>.<uuid>.tmpdir\avatar.js.tmp'
      //   Emitted 'error' event on FSWatcher instance → Node.js v24.21.0 退出）。
      // 这不是本仓代码问题，是 watch 机制对瞬时目录的竞态；忽略掉就不会去 watch 它们。
      ignored: [
        '**/.*.tmpdir/**',
        '**/*.tmpdir/**',
        '**/node_modules/**',
        '**/dist/**',
        // 构建产物与备份目录同理：内容变动不需要触发热更新
        '**/.backup-dead-vendor/**',
        '**/unpackage/**'
      ]
    }
  }
})
