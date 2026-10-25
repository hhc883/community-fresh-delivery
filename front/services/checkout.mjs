import { submitOrder, openGroupon, joinGroupon } from "../api/index.js";

function requestId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function")
    return crypto.randomUUID();
  // WeChat runtimes may not provide Web Crypto. This identifier is an idempotency
  // key, never an authentication secret; still use the UUID v4 wire format.
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (char) => {
    const value = Math.floor(Math.random() * 16);
    return (char === "x" ? value : (value & 3) | 8).toString(16);
  });
}

// Persist the request identity before sending: a timeout may still have created the order.
// Reopening checkout with the same intent must replay the same server idempotency key.
export async function checkout(intent) {
  const payload = {
    deliveryType: intent.deliveryType === "delivery" ? 1 : 2,
    remark: intent.remark || "",
  };
  if (payload.deliveryType === 1)
    payload.addressBookId = String(intent.addressBookId || "");
  else {
    payload.pickupPointId = String(intent.pickupPointId || "");
    payload.pickupPersonName = (intent.pickupPersonName || "").trim();
    payload.pickupPersonPhone = (intent.pickupPersonPhone || "").trim();
  }
  if (intent.mode) {
    payload.number = Number(intent.quantity);
    if (intent.mode === "join") payload.grouponId = String(intent.grouponId);
    else payload.dishId = String(intent.dishId);
  } else {
    payload.items = intent.items.map((item) => ({
      dishId: String(item.dishId || item.id),
      number: Number(item.quantity || item.number),
    }));
  }
  const fingerprint = JSON.stringify(payload);
  const key = "pending-checkout:" + (uni.getStorageSync("user")?.id || "guest");
  let pending = uni.getStorageSync(key);
  if (!pending || pending.fingerprint !== fingerprint) {
    pending = { fingerprint, clientRequestId: requestId() };
    uni.setStorageSync(key, pending);
  }
  payload.clientRequestId = pending.clientRequestId;
  const response = await (intent.mode === "open"
    ? openGroupon(payload)
    : intent.mode === "join"
      ? joinGroupon(payload)
      : submitOrder(payload));
  const data = response.data;
  const orderId = intent.mode ? data?.orderId : data?.id;
  if (!orderId) throw new Error("服务未返回真实订单号，请到我的订单核对后重试");
  uni.removeStorageSync(key);
  return { ...data, id: String(orderId) };
}
