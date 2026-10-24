// WeiXinApp/common/utils/request.js
// 社区团购业务请求封装
//
// 说明：项目原有业务页（index/order/my 等）是 uni-app 编译产物，
// 无法直接改动源码；本文件与 pages/groupon/* 均为原生小程序写法。

var config = require('./config.js')

// 用户端令牌名，与后端 application.yml 中 sky.jwt.user-token-name 一致
var TOKEN_KEY = 'authentication'

function getBaseUrl() {
  return config.baseUrl
}

function getToken() {
  try {
    return wx.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

/**
 * 统一请求方法
 * @param {string} url    相对路径，如 /user/groupon/mine
 * @param {string} method GET/POST/PUT/DELETE
 * @param {object} data   请求体或查询参数
 * @returns {Promise<object>} 后端 Result 对象 { code, msg, data }
 */
function request(url, method, data) {
  return new Promise(function (resolve, reject) {
    var header = { 'Content-Type': 'application/json' }
    var token = getToken()
    if (token) {
      header[TOKEN_KEY] = token
    }

    wx.request({
      url: getBaseUrl() + url,
      method: method || 'GET',
      data: data || {},
      header: header,
      success: function (res) {
        var body = res.data || {}
        if (body.code === 1) {
          resolve(body)
        } else if (res.statusCode === 401) {
          wx.showToast({ title: '请先登录', icon: 'none' })
          reject(body)
        } else {
          wx.showToast({ title: body.msg || '请求失败', icon: 'none' })
          reject(body)
        }
      },
      fail: function (err) {
        wx.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
        reject(err)
      }
    })
  })
}

function get(url, data) {
  return request(url, 'GET', data)
}

function post(url, data) {
  return request(url, 'POST', data)
}

function put(url, data) {
  return request(url, 'PUT', data)
}

function del(url, data) {
  return request(url, 'DELETE', data)
}

module.exports = {
  request: request,
  get: get,
  post: post,
  put: put,
  del: del,
  getBaseUrl: getBaseUrl,
  baseUrl: getBaseUrl()
}
