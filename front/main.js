import { createSSRApp } from "vue";
import { createStore } from "vuex";
import App from "./App.vue";
import { setUnauthorizedHandler } from "./api/index.js";
export function safeGetStorage(key) {
  try {
    return uni.getStorageSync(key);
  } catch {
    return null;
  }
}
export function safeSetStorage(key, value) {
  try {
    uni.setStorageSync(key, value);
  } catch {
    uni.showToast({ title: "无法保存本地数据，请检查存储空间", icon: "none" });
  }
}
export function formatMoney(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(2) : "0.00";
}
function restored(key, fallback) {
  try {
    const raw = safeGetStorage(key);
    return (typeof raw === "string" ? JSON.parse(raw) : raw) || fallback;
  } catch {
    return fallback;
  }
}
const store = createStore({
  state: {
    cart: (() => {
      const list = restored("cart", []);
      return Array.isArray(list)
        ? list.filter(item => item && item.id != null && Number.isInteger(Number(item.quantity)) && Number(item.quantity) > 0)
            .map(item => ({ ...item, id: String(item.id), quantity: Number(item.quantity), checked: item.checked !== false }))
        : [];
    })(),
    user: (() => { const user = restored("user", {}); return user && typeof user === 'object' && typeof user.token === 'string' ? user : {}; })(),
    pickupPoint: {},
    pickupPerson: {},
  },
  mutations: {
    addToCart(state, dish) {
      const item = state.cart.find((i) => String(i.id) === String(dish.id));
      if (item) item.quantity++;
      else
        state.cart.push({
          id: String(dish.id),
          name: dish.name,
          price: Number(dish.price),
          image: dish.image,
          stock: dish.stock,
          quantity: 1,
          checked: true,
        });
      safeSetStorage("cart", state.cart);
      uni.showToast({ title: "已加入购物车", icon: "success" });
    },
    updateQuantity(state, { dishId, quantity }) {
      const item = state.cart.find((i) => String(i.id) === String(dishId));
      if (item) item.quantity = Math.max(1, Math.floor(quantity));
      safeSetStorage("cart", state.cart);
    },
    removeFromCart(state, id) {
      state.cart = state.cart.filter((i) => String(i.id) !== String(id));
      safeSetStorage("cart", state.cart);
    },
    toggleChecked(state, id) {
      const item = state.cart.find((i) => String(i.id) === String(id));
      if (item) item.checked = !item.checked;
      safeSetStorage("cart", state.cart);
    },
    clearPurchased(state, ids) {
      state.cart = state.cart.filter((i) => !ids.includes(String(i.id)));
      safeSetStorage("cart", state.cart);
    },
    setUser(state, user) {
      state.user = user;
      safeSetStorage("user", user);
    },
    setClearUser(state) {
      const oldUserId = state.user?.id;
      state.user = {};
      state.pickupPoint = {};
      state.pickupPerson = {};
      try {
        uni.removeStorageSync("user");
        if (oldUserId) {
          uni.removeStorageSync("pending-checkout:" + String(oldUserId));
        }
        uni.removeStorageSync("pending-checkout:guest");
      } catch {}
    },
    setPickupPoint(state, p) {
      state.pickupPoint = p;
    },
    setPickupPerson(state, p) {
      state.pickupPerson = p;
    },
  },
  getters: { cartCount: (s) => s.cart.reduce((n, i) => n + i.quantity, 0) },
});
setUnauthorizedHandler(() => store.commit("setClearUser"));
export function createApp() {
  const app = createSSRApp(App);
  app.use(store);
  app.config.globalProperties.$fmtMoney = formatMoney;
  return { app };
}
