// Set VITE_API_BASE_URL before building for a remote device or deployed backend.
// H5 can use '' through VITE_API_BASE_URL= for same-origin reverse proxy.
let defaultApiBase = "http://127.0.0.1:8080";
// #ifdef H5
defaultApiBase = "";
// #endif
export const BASE_URL = import.meta.env?.VITE_API_BASE_URL ?? defaultApiBase;
export const AMAP_KEY = "";
export const AMAP_SECURITY_CODE = "";
