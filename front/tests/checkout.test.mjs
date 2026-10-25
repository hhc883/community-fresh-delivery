import test from "node:test";
import assert from "node:assert/strict";
import { checkout } from "../services/checkout.mjs";

test("retrying an interrupted checkout replays one intent without trusting client prices", async () => {
  const storage = new Map();
  const requests = [];
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
    request: (options) => {
      requests.push(options.data);
      if (requests.length === 1) options.fail();
      else
        options.success({
          statusCode: 200,
          data: {
            code: 200,
            data: { id: "9007199254740993", orderAmount: 12.5 },
          },
        });
    },
  };
  const intent = {
    items: [{ id: "9007199254740991", quantity: 2, price: 0.01 }],
    deliveryType: "delivery",
    addressBookId: "12",
    amount: 0.02,
  };
  await assert.rejects(checkout(intent), (error) => error.code === -1);
  const result = await checkout(intent);
  assert.equal(result.id, "9007199254740993");
  assert.equal(requests[0].clientRequestId, requests[1].clientRequestId);
  assert.match(
    requests[0].clientRequestId,
    /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
  );
  assert.deepEqual(requests[0].items, [
    { dishId: "9007199254740991", number: 2 },
  ]);
  assert.equal(requests[0].deliveryType, 1);
  assert.equal(requests[0].amount, undefined);
});
