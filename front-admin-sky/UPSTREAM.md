# 苍穹外卖管理端来源

原版可编辑源码来自 https://github.com/HeFeng-Lab/project-sky-admin-vue-ts ，固定提交 `37d34c2fa5fb87b218672bb0f2f6a18861408f32`。保留项目附带 MIT LICENSE 及版权声明。没有导入上游的 Git 元数据、依赖、构建产物、运行诊断报告或测试目录。

本项目保留原版登录、黄色顶栏、深色侧栏、工作台、统计、订单、商品、套餐、分类和员工页面。新增侧栏入口在同源 iframe 内挂载现有 Vue3 生鲜模块，避免把 Vue2 与 Vue3 运行时混装进一个应用。它们共用后端管理员 JWT；token 不进入 URL。退出或登录失效会清理两端会话。

接口前缀为 `/admin`；图片上传同样使用 `/admin/common/upload`。商品原版表单补充库存、单位、产地和生鲜标记；拼团价格等扩展字段从“生鲜与库存”管理。源码构建采用原 Vue CLI / TypeScript 装饰器，移除 fibers 和未使用的测试插件，并以独立 Node 启动脚本兼容 Node 22 的 OpenSSL。构建只做转译与打包，不代表完整 TypeScript 类型检查或业务验收通过。

```sh
npm ci --legacy-peer-deps --ignore-scripts
npm run build
```

主站为本目录的 `dist/`；扩展模块需先在 `../front-admin` 构建，再同源部署至 `/extensions/`。本机完整预览见根 README。开发服务器端口5177，可先构建扩展模块，再使用根脚本预览全部功能；单独 `npm run dev` 只提供苍穹主站。
