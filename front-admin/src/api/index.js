// ==================== API 请求封装（运营端唯一网络出口） ====================
// 契约来源：docs/contracts/api-v1.md（已冻结 v1）。
// 基线决定：管理后台直连 backEnd/（Spring Boot 2.7.3，com/sky，端口 8080），复用既有 /admin/** 接口；
// 不再对接 backEnd_sb3_backup 的 /api/admin/v2/*（那套接口在真实后端不存在）。
//
// 信封语义（/admin/**，Result 类）：
//   成功  HTTP 200 + {code:1, msg/message, data}
//   失败  HTTP 200 + {code:0, msg/message}（GlobalExceptionHandler 统一返回，不抛非 200）
//   登录失效  HTTP 401 + {code:401, message:"请先登录"}（JwtTokenAdminInterceptor 直写）
// 前端只认 code===1 为成功；401 清登录态跳登录页；其余一律按失败透出后端 message。
export const BASE_URL = import.meta.env?.VITE_API_BASE_URL ?? 'http://localhost:8080'

// 分层超时：查询类 10s；提交类 20s（登录走 PBKDF2 验密，属提交档）。
const TIMEOUT_QUERY = 10000
const TIMEOUT_SUBMIT = 20000

// 登录态 storage 键：本仓唯一真源就是这个常量，其余一律通过 getStoredToken/clearStoredToken/logout 走。
export const TOKEN_STORAGE_KEY = 'admin_token'

const MSG_UNAUTHORIZED = '请先登录'
const MSG_NETWORK = '网络异常'
const MSG_TIMEOUT = '请求超时，请稍后重试'

// ==================== 登录态读写（localStorage 唯一出口，全程守卫） ====================

export function getStoredToken() {
  try {
    const cached = localStorage.getItem(TOKEN_STORAGE_KEY)
    return typeof cached === 'string' ? cached : ''
  } catch (e) {
    return ''
  }
}

export function setStoredToken(token) {
  try {
    localStorage.setItem(TOKEN_STORAGE_KEY, token || '')
  } catch (e) {
    // 落盘失败只影响「刷新后要不要重新登录」，本次会话的内存态照常可用
  }
}

export function clearStoredToken() {
  try {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
  } catch (e) {
    // 存储异常静默：清态的下一步就是回登录页
  }
}

// ==================== 401 跳登录的注册器（api 不反向 import router） ====================
let unauthorizedHandler = null
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = typeof fn === 'function' ? fn : null
}

function goToLogin() {
  if (unauthorizedHandler) {
    try {
      unauthorizedHandler()
    } catch (e) {
      // 跳转回调异常不影响 reject 语义
    }
  }
}

// ==================== 退出登录 ====================
// 员工 JWT 无状态：调后端 logout 端点做记录，本地一律清 token + 回登录页。
export function logout() {
  const hadToken = !!getStoredToken()
  clearStoredToken()
  if (hadToken) {
    // 尽力通知后端即可，失败不影响登出结果
    request({ url: '/admin/employee/logout', method: 'POST' }).catch(() => {})
  }
  goToLogin()
}

/**
 * 统一信封分流（本层唯一的信封判定处）。
 * @param {{statusCode:number, data:*}} res 归一后的响应（fetch 的 status + JSON.parse 结果）
 * @param {boolean} loginRequest 登录请求的 401 属业务失败：不清态、不跳转，message 原样交登录页渲染
 */
function handleEnvelope(res, resolve, reject, loginRequest) {
  const body = (res && res.data && typeof res.data === 'object') ? res.data : null
  const message = (body && typeof body.message === 'string' && body.message ? body.message
    : (body && typeof body.msg === 'string' && body.msg ? body.msg : ''))
  const data = (body && body.data !== undefined) ? body.data : null

  // HTTP 401：token 缺失/伪造/过期 → 清态 + 跳登录 + reject
  if (res && res.statusCode === 401) {
    if (loginRequest) {
      reject({ code: 401, message: message || MSG_UNAUTHORIZED, data })
      return
    }
    clearStoredToken()
    goToLogin()
    reject({ code: 401, message: message || MSG_UNAUTHORIZED })
    return
  }
  // /admin/** 的业务成功只有一枚：code===1
  if (res && res.statusCode === 200 && body && Number(body.code) === 1) {
    resolve(body)
    return
  }
  // 其余（code=0 业务失败 / 4xx 5xx / JSON 不可解析）一律 reject 带 data
  reject({
    code: body ? Number(body.code) || 0 : -1,
    message: message || '服务开小差了，请稍后重试',
    data
  })
}

/**
 * 统一请求封装：token 存在时带 Authorization: Bearer <token>；
 * 登录请求传 loginRequest:true → 强制不带 Authorization。
 * 超时用 AbortController 实现。
 */
function request(options) {
  const method = (options.method || 'GET').toUpperCase()
  const loginRequest = options.loginRequest === true
  const isSubmit = method !== 'GET'
  const timeoutMs = isSubmit ? TIMEOUT_SUBMIT : TIMEOUT_QUERY

  const headers = { 'Content-Type': 'application/json' }
  if (!loginRequest) {
    const token = getStoredToken()
    if (token) headers.Authorization = 'Bearer ' + token
  } else {
    delete headers.Authorization
  }

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)

  return new Promise((resolve, reject) => {
    fetch(BASE_URL + buildUrl(options.url, options.params), {
      method,
      headers,
      body: options.data !== undefined && method !== 'GET' ? JSON.stringify(options.data) : undefined,
      signal: controller.signal
    })
      .then((res) => res.json().then(
        (json) => ({ statusCode: res.status, data: json }),
        () => ({ statusCode: res.status, data: null })
      ))
      .then((normalized) => {
        clearTimeout(timer)
        handleEnvelope(normalized, resolve, reject, loginRequest)
      })
      .catch((err) => {
        clearTimeout(timer)
        // 网络层失败 / 主动 abort 一律 reject：绝不 resolve(null) 把「没连通」渲染成「没有数据」
        const aborted = err && err.name === 'AbortError'
        reject({ code: -1, message: aborted ? MSG_TIMEOUT : MSG_NETWORK, data: null })
      })
  })
}

function buildUrl(url, params) {
  if (!params) return url
  const search = new URLSearchParams()
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value !== '' && value !== null && value !== undefined) search.append(key, String(value))
  })
  const qs = search.toString()
  return qs ? url + '?' + qs : url
}

// id 一律 encodeURIComponent(String(id)) 原样透传，绝不 Number() 化。
function pathId(id) {
  return encodeURIComponent(String(id))
}

/**
 * 等待时长的显示单位换算（登录页锁定倒计时用）：
 * <60 秒 → 「N 秒」；≥60 秒 → 「约 M 分钟」（向上取整，低估等待＝误导）。
 * 入参只认有限正数，其余回 ''，由调用方决定隐藏该行。
 */
export function waitDurationText(seconds) {
  const n = Number(seconds)
  if (!Number.isFinite(n) || n <= 0) return ''
  if (n < 60) return n + ' 秒'
  return '大约 ' + Math.ceil(n / 60) + ' 分钟'
}

// ==================== 员工登录（匿名端点，唯一不带 Authorization 的请求） ====================

/**
 * 员工登录：POST /admin/employee/login，body {username,password}
 * 成功 data {id, userName, name, token}；失败 code=0「用户名或密码错误」等，message 原样透出。
 */
export function adminLogin(username, password) {
  return request({
    url: '/admin/employee/login',
    method: 'POST',
    data: { username, password },
    loginRequest: true
  })
}

// ==================== 大盘（/admin/workspace） ====================

// 今日经营数据：营业额、有效订单、订单完成率、平均客单价、新增用户
export const getBusinessData = () => request({ url: '/admin/workspace/businessData' })
// 各状态订单计数：待接单/已接单/派送中/待审核取消
export const getOrderOverview = () => request({ url: '/admin/workspace/overviewOrders' })
// 起售/停售商品计数
export const getDishOverview = () => request({ url: '/admin/workspace/overviewDishes' })

// ==================== 分类（/admin/category） ====================

export function getCategoryList(type) {
  return request({ url: '/admin/category/list', params: { type } })
}
export function getCategoryPage(params) {
  return request({ url: '/admin/category/page', params })
}
export function createCategory(data) {
  return request({ url: '/admin/category', method: 'POST', data })
}
export function updateCategory(data) {
  return request({ url: '/admin/category', method: 'PUT', data })
}
export function setCategoryStatus(id, status) {
  return request({ url: '/admin/category/status/' + status, method: 'POST', params: { id } })
}
export function deleteCategory(id) {
  return request({ url: '/admin/category', method: 'DELETE', params: { id } })
}

// ==================== 商品（/admin/dish，DishDTO 含库存与团购字段） ====================

export function getDishPage(params) {
  // params: {page,pageSize,name,categoryId,status}
  return request({ url: '/admin/dish/page', params })
}
export function getDish(id) {
  return request({ url: '/admin/dish/' + pathId(id) })
}
export function createDish(data) {
  return request({ url: '/admin/dish', method: 'POST', data })
}
export function updateDish(data) {
  return request({ url: '/admin/dish', method: 'PUT', data })
}
export function setDishStatus(id, status) {
  return request({ url: '/admin/dish/status/' + status, method: 'POST', params: { id } })
}
export function deleteDishes(ids) {
  // 后端 @RequestParam Long[] ids → ids=1&ids=2
  const search = new URLSearchParams()
  ids.forEach((id) => search.append('ids', String(id)))
  return request({ url: '/admin/dish?' + search.toString(), method: 'DELETE' })
}

// ==================== 订单履约（/admin/order） ====================

export function getOrderPage(params) {
  // params: {page,pageSize,number,phone,status,beginTime,endTime}
  // status: 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消；beginTime/endTime: yyyy-MM-dd HH:mm:ss
  return request({ url: '/admin/order/conditionSearch', params })
}
export function getOrderDetail(id) {
  return request({ url: '/admin/order/details/' + pathId(id) })
}
export const confirmOrder = (id) => request({ url: '/admin/order/confirm', method: 'PUT', data: { id } })
export function rejectOrder(id, rejectionReason) {
  return request({ url: '/admin/order/rejection', method: 'PUT', data: { id, rejectionReason } })
}
export function cancelOrder(id, cancelReason) {
  return request({ url: '/admin/order/cancel', method: 'PUT', data: { id, cancelReason } })
}
export const deliveryOrder = (id) => request({ url: '/admin/order/delivery/' + pathId(id), method: 'PUT' })
export const completeOrder = (id) => request({ url: '/admin/order/complete/' + pathId(id), method: 'PUT' })
// 提货码核销自提订单：data {pickupCode, pickupPointId?}；重复核销由服务端拒绝（verify_status 判定）
export function verifyOrder(data) {
  return request({ url: '/admin/order/verify', method: 'POST', data })
}
// 待接单/已接单/派送中计数
export const getOrderStatistics = () => request({ url: '/admin/order/statistics' })

// ==================== 拼团管理（/admin/groupon） ====================

export function getGrouponPage(params) {
  // params: {page,pageSize,status,keyword}；status 1拼团中 2已成团 3已失败 4已过期
  return request({ url: '/admin/groupon/page', params })
}
// 手动校验并成团；已成团时服务端幂等返回 data=false，不产生第二次效果
export const completeGroupon = (id) => request({ url: '/admin/groupon/complete/' + pathId(id), method: 'PUT' })
// 关闭全部超时未成团活动；data 为关闭数量
export const closeExpiredGroupons = () => request({ url: '/admin/groupon/closeExpired', method: 'PUT' })

// ==================== 报表统计（/admin/report，begin/end: yyyy-MM-dd） ====================

export function getTurnoverReport(begin, end) {
  return request({ url: '/admin/report/turnoverStatistics', params: { begin, end } })
}
export function getOrderReport(begin, end) {
  return request({ url: '/admin/report/ordersStatistics', params: { begin, end } })
}
export function getUserReport(begin, end) {
  return request({ url: '/admin/report/userStatistics', params: { begin, end } })
}
export function getSalesTop10(begin, end) {
  return request({ url: '/admin/report/top10', params: { begin, end } })
}
// 新增契约接口：{ongoing,success,failed,expired,memberCount,grouponOrderCount,grouponTurnover}
export function getGrouponStatistics(begin, end) {
  return request({ url: '/admin/report/grouponStatistics', params: { begin, end } })
}
