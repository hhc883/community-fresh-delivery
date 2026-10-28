// 路由：/login 与五个业务页（大盘 / 商品管理 / 订单履约 / 拼团管理 / 统计报表），一条全局前置守卫。
// 页面-接口对应关系见 docs/contracts/api-v1.md。
// 依赖方向单向：router → api，api 绝不 import router（401 跳转靠本模块末尾注册的回调）。
// 业务页一律动态 import（懒加载），首屏包只装壳。
import { createRouter, createWebHistory } from 'vue-router'
import { getStoredToken, setUnauthorizedHandler } from '../api'

const routes = [
  // 根路径不做「无条件跳大盘」：登录态是否存在由守卫判（无 token 时下面这条会立刻被守卫改道 /login）
  { path: '/', redirect: '/overview' },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/overview',
    name: 'overview',
    component: () => import('../views/Overview.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/dishes',
    name: 'dishes',
    component: () => import('../views/Dishes.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/Orders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/groupon',
    name: 'groupon',
    component: () => import('../views/GrouponPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/reports',
    name: 'reports',
    component: () => import('../views/Reports.vue'),
    meta: { requiresAuth: true }
  },
  // 未知路径统一改道大盘：回到已知态更实在
  { path: '/:pathMatch(.*)*', redirect: '/overview' }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫：无 admin_token 且目标不是 /login → 跳登录。
// 请求层的 401（token 过期/被吊销）是第二条兜底，两处判定读的是同一个 getStoredToken()。
router.beforeEach((to) => {
  const hasToken = !!getStoredToken()
  if (!hasToken && to.path !== '/login') {
    return { path: '/login' }
  }
  if (hasToken && to.path === '/login') {
    return { path: '/overview' }
  }
  return true
})

export default router

// 401 → 回登录页（api 层清完 token 后调用，见 api/index.js 的 goToLogin）。
// 用 replace 而非 push：登录失效前的那一页不该留在历史里，否则浏览器后退又会撞回 401。
setUnauthorizedHandler(() => {
  if (window.parent !== window) {
    document.cookie = 'token=; Max-Age=0; path=/';
    window.parent.location.replace('/#/login');
    return;
  }
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
})
