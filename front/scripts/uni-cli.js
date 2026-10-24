#!/usr/bin/env node
/**
 * uniapp CLI 包装器（F-53 新增）
 *
 * 【为什么需要这个文件】
 * uniapp CLI 默认约定源码放在 <root>/src/，靠 UNI_INPUT_DIR 覆盖。
 * 实测（本机 Windows + @dcloudio/vite-plugin-uni 3.0.0-alpha-5030220261016001）：
 *   UNI_INPUT_DIR=.        → 页面能编译，但 static/ 目录**不会**被拷进产物
 *                            （vite-plugin-uni/dist/plugins/copy.js 用 chokidar 按
 *                              pathToGlob(inputDir) 扫 static/**，相对路径「.」生成的 glob
 *                              匹配不到文件，静默跳过——不报错，产物里就是没有图）
 *   UNI_INPUT_DIR=<绝对路径> → static/ 正常拷贝
 *
 * 而 cross-env 只能传字面量、算不出 cwd，所以用本包装器在运行时把 UNI_INPUT_DIR
 * 设成 __dirname（= front/ 自身），再把参数原样转交给 uni CLI。
 *
 * 用法（见 package.json scripts）：
 *   node scripts/uni-cli.js                    # 等价 uni         → H5 dev
 *   node scripts/uni-cli.js build -p mp-weixin # 等价 uni build -p mp-weixin
 */
const path = require('path')
const { spawnSync } = require('child_process')

// front/ 根目录：本文件在 front/scripts/ 下，故上溯一级
const projectRoot = path.resolve(__dirname, '..')

// 关键：必须是绝对路径，否则 static/ 不会被拷贝（原因见文件头注释）
process.env.UNI_INPUT_DIR = projectRoot

// uni CLI 的真实入口（vite-plugin-uni 的 bin 字段）
const uniBin = require.resolve('@dcloudio/vite-plugin-uni/bin/uni.js', {
  paths: [projectRoot]
})

const result = spawnSync(process.execPath, [uniBin, ...process.argv.slice(2)], {
  stdio: 'inherit',
  cwd: projectRoot,
  env: process.env
})

// 透传子进程退出码，保证 CI/门禁能拿到真实成败
process.exit(result.status === null ? 1 : result.status)
