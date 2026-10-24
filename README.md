# 🥬 fresh-pickup 生鲜配送

> 多多买菜风格的社区团购生鲜配送应用 · uniapp Vue3 H5 + Spring Boot 3 + MySQL
> 自提模式 · 限时秒杀 · 优惠券核销 · 多门店 · 手机号验证码登录

![技术栈](https://img.shields.io/badge/uniapp-Vue3-2B9939) ![后端](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F) ![数据库](https://img.shields.io/badge/MySQL-8-4479A1) ![鉴权](https://img.shields.io/badge/JWT-HS256-orange)

## ✨ 功能特性

**用户端**
- 🛒 多多买菜风格首页：金刚区 / 超级秒杀（服务端时钟倒计时）/ 优惠券三卡 / 全部分类弹层
- 🔍 独立搜索页：最近搜索（居中确认框清空）/ 搜索发现（换一换）/ 地区热卖榜单横滑
- 📦 购物车全局：任意页面加购 → 底栏联动 → 弹层勾选/步进器/删除/凑单助手
- 🏪 自提模式：自提点切换（停业置灰）/ 提货人信息 / 订单门店快照
- 💳 交易闭环：下单幂等（clientRequestId）/ 库存原子扣减防超卖 / 限购后端校验 / 模拟收银台
- ⭐ 评价系统：评分 / 敏感词 DFA 过滤（硬词拦截、软词待审）
- 🖼 截单服务端裁决：23:00 前下单次日自提（pickup_date 服务端裁决，改本机时钟无效）

**管理与安全**
- 🔐 手机号验证码登录（JWT 7 天）/ 权限矩阵（匿名读、登录写）/ Bucket4j 限流 429
- 🛡 服务端计价（改价攻击无效）/ 统一异常脱敏 / 评价 XSS 入库转义 / CORS 白名单
- 🧰 管理薄版：X-Admin-Key 鉴权的订单核销 / 商品改价 / 补库存接口 + SQL 运营手册
- 📗 外部字典系统对接：分类/券模板/金刚区/服务标签 4 组枚举走字典代理（Caffeine 缓存 + 断网降级）

## 🏗 技术架构

| 层级 | 技术 |
|------|------|
| 前端 | uniapp (Vue3) + Vuex + Vite，H5 端（小程序条件编译已预留） |
| 后端 | Spring Boot 3 + MyBatis-Plus + spring-boot-starter-validation |
| 安全 | JWT（jjwt）+ AuthInterceptor 权限矩阵 + Bucket4j 限流 + CORS 白名单 |
| 数据 | MySQL 8（13 张表）+ 逻辑删除 |
| 外部依赖 | 字典系统（枚举托管，可替换为任意配置源，FALLBACK 内置兜底） |

## 📁 目录结构

```
├── front/                       # uniapp 前端（H5 + 微信小程序双端，F-53 起 CLI 化）
│   ├── pages/                   # index(首页) search(搜索) detail(详情)
│   │                            # order(订单) review(评价) pickup(自提点) qualification(资质)
│   ├── api/                     # request 封装 + 接口 + 字典对接 + config
│   ├── utils/time.js            # 截单/倒计时共用工具
│   ├── scripts/uni-cli.js       # uniapp CLI 包装（钉死 UNI_INPUT_DIR 绝对路径）
│   ├── static/images/png/       # 商品图 PNG（28 张，小程序 image 不支持 SVG）
│   └── static/images/map/       # 小程序地图 marker 图标（营业/停业/用户）
├── backEnd/                     # Spring Boot 后端
│   └── src/main/java/com/fresh/
│       ├── controller/          # 14 个控制器（含 Admin 管理薄版）
│       ├── service/             # 业务（库存原子扣减/幂等/计价/敏感词/字典代理）
│       ├── pay/                 # PayService 抽象（Mock 实现 / 微信骨架）
│       ├── interceptor/         # JWT 鉴权 + 限流
│       └── dto/ common/         # 入参白名单 + 统一信封 + 全局异常
├── deploy/                      # 部署配置（Nginx / backup.sh / systemd）
└── scripts/smoke.sh             # 一键冒烟（登录→下单→查单→取消）
```

## 🚀 快速开始

### 环境要求
- JDK 17+、Maven 3.6+、MySQL 8、HBuilderX（方式 B 前端运行）
- 方式 A 另需 Node 18+；小程序端需微信开发者工具

### 1. 数据库
```sql
CREATE DATABASE fresh_db DEFAULT CHARACTER SET utf8mb4;
-- 执行 backEnd/sql/init.sql（建表+种子数据，已含 m3~m20 全部结构与 PNG 图片路径）
```

### 2. 后端配置
```bash
# backEnd/src/main/resources/application-dev.yml 填入你的数据库密码
spring.datasource.password: your-db-password
```

### 3. 启动
```bash
# 后端（3001 端口）
cd backEnd && mvn spring-boot:run

# 前端（二选一）
#   方式 A：命令行（H5 与微信小程序双端，F-53 新增）
cd front && npm install

npm run dev:h5            # 网页版 → http://localhost:5173

#   小程序（注意 dev 与 build 的区别，产物目录与后端地址都不同）：
npm run dev:mp-weixin     # 开发构建（watch 常驻）→ front/dist/dev/mp-weixin，后端指向 localhost:3001
npm run build:mp-weixin   # 生产构建（一次性）  → front/dist/build/mp-weixin，后端指向 api/config.js 的 PROD_BASE_URL
#   微信开发者工具导入上面任一目录即可（本地联调请用 dev 产物）

#   方式 B：HBuilderX 导入 front/ 目录 → 运行到浏览器（5173 端口）
```

> **双端说明（F-53）**：`front/` 已改造为 uniapp CLI 工程，同一份源码可编译 H5 与微信小程序。
> - 商品图已由 SVG 转为 PNG（小程序 `image` 不支持 SVG），静态图在 `front/static/images/png/`；
>   库里 `dish.image` 存 `/static/images/png/dish-<id>.png`。
> - 小程序端自提点地图用原生 `<map>` 组件 + `uni.getLocation`（H5 端仍走高德 JS API），
>   头像设置走 `uni.chooseMedia` + canvas 裁剪（H5 端仍走 `input[type=file]`）。
> - 小程序真机调试需在微信公众平台配置 request 合法域名（`http://localhost:3001` 仅开发者工具可用，
>   且需勾选「不校验合法域名」）。

### 4. 冒烟验证
```bash
bash scripts/smoke.sh   # 登录→下单→查单→取消 全链路断言
```

## 🔑 默认配置（dev）

| 项 | 值 | 说明 |
|----|-----|------|
| 登录验证码 | `123456` | dev 固定验证码（auth.dev-code），生产请接入云短信 |
| 管理密钥 | `admin-dev-key` | X-Admin-Key 头（环境变量 ADMIN_KEY） |
| 截单时间 | 每日 23:00 | 23:00 前下单次日自提 |

## 📄 License

MIT
