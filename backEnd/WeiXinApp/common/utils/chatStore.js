// WeiXinApp/common/utils/chatStore.js
// localStorage 封装：会话/设置/活动 id
// Storage keys: sf_chat_sessions, sf_chat_active_id, sf_chat_settings

const KEYS = {
  sessions: 'sf_chat_sessions',
  activeId: 'sf_chat_active_id',
  settings: 'sf_chat_settings'
}

const DEFAULT_SETTINGS = {
  apiKey: '',
  defaultModel: 'Qwen/Qwen2.5-7B-Instruct',
  systemPrompt: '你是苍穹外卖的 AI 助手。回答简洁、友好。'
}

// ---------- settings ----------

function getSettings() {
  try {
    var stored = wx.getStorageSync(KEYS.settings)
    if (!stored) return Object.assign({}, DEFAULT_SETTINGS)
    return Object.assign({}, DEFAULT_SETTINGS, stored)
  } catch (e) {
    return Object.assign({}, DEFAULT_SETTINGS)
  }
}

function setSettings(s) {
  try {
    wx.setStorageSync(KEYS.settings, s)
    return true
  } catch (e) {
    return false
  }
}

// ---------- sessions ----------

function listSessions() {
  try {
    return wx.getStorageSync(KEYS.sessions) || []
  } catch (e) {
    return []
  }
}

function saveSessions(list) {
  try {
    wx.setStorageSync(KEYS.sessions, list)
    return true
  } catch (e) {
    return false
  }
}

function getActiveId() {
  try {
    return wx.getStorageSync(KEYS.activeId) || null
  } catch (e) {
    return null
  }
}

function setActiveId(id) {
  try {
    wx.setStorageSync(KEYS.activeId, id)
  } catch (e) {}
}

// 取当前激活的 session，没有则建一个
function getActiveSession() {
  var list = listSessions()
  var id = getActiveId()
  if (id) {
    for (var i = 0; i < list.length; i++) {
      if (list[i].id === id) return list[i]
    }
  }
  // 没有 active 或找不到，建一个
  return createNewSession()
}

function createNewSession() {
  var s = getSettings()
  var now = Date.now()
  var session = {
    id: 'session_' + now,
    title: '新对话',
    model: s.defaultModel,
    systemPrompt: s.systemPrompt,
    messages: [],
    createdAt: now,
    updatedAt: now
  }
  var list = listSessions()
  list.unshift(session)
  saveSessions(list)
  setActiveId(session.id)
  return session
}

function updateSession(session) {
  session.updatedAt = Date.now()
  var list = listSessions()
  for (var i = 0; i < list.length; i++) {
    if (list[i].id === session.id) {
      list[i] = session
      break
    }
  }
  saveSessions(list)
}

function deleteSession(id) {
  var list = listSessions().filter(function (s) { return s.id !== id })
  saveSessions(list)
  if (getActiveId() === id) {
    setActiveId(list.length > 0 ? list[0].id : null)
  }
}

function clearAll() {
  saveSessions([])
  setActiveId(null)
}

module.exports = {
  getSettings: getSettings,
  setSettings: setSettings,
  listSessions: listSessions,
  getActiveId: getActiveId,
  setActiveId: setActiveId,
  getActiveSession: getActiveSession,
  createNewSession: createNewSession,
  updateSession: updateSession,
  deleteSession: deleteSession,
  clearAll: clearAll,
  DEFAULT_SETTINGS: DEFAULT_SETTINGS
}
