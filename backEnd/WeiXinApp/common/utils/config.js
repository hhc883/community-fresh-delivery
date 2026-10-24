// WeiXinApp/common/utils/config.js
// 后端地址配置（单独抽出，避免直接 require webpack 产物 vendor.js）
//
// 重要：请根据运行环境修改 baseUrl
//  - 微信开发者工具模拟器：http://localhost:8080
//  - 真机预览/体验版：改成电脑的局域网 IP，如 http://192.168.1.10:8080
//    （手机上的 localhost 指向手机自己，连不到你的电脑）
//
// 注意：vendor.js 中原有的 baseUrl 也要同步修改，二者需保持一致。
//  位置：WeiXinApp/common/vendor.js 搜索 "const baseUrl"

var BASE_URL = 'http://localhost:8080'

module.exports = {
  baseUrl: BASE_URL
}
