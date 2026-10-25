// Set VITE_API_BASE_URL before building for a remote device or deployed backend.
// H5 can use '' through VITE_API_BASE_URL= for same-origin reverse proxy.
export const BASE_URL =
  import.meta.env?.VITE_API_BASE_URL ?? "http://localhost:8080";
export const AMAP_KEY = "";
export const AMAP_SECURITY_CODE = "";
