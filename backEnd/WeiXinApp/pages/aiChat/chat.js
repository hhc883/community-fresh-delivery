// WeiXinApp/pages/aiChat/chat.js
const chatStore = require('../../common/utils/chatStore.js')
const sf = require('../../common/utils/siliconflow.js')

Page({
  data: {
    session: null,
    messages: [],
    input: '',
    sending: false,
    showDrawer: false,
    sessions: [],
    settings: null,
    lastMsgId: ''
  },

  onShow: function () {
    this._reload()
  },

  onInput: function (e) {
    this.setData({ input: e.detail.value })
  },

  onSend: function () {
    if (this.data.sending) return
    var text = (this.data.input || '').trim()
    if (!text) return
    if (!this.data.settings || !this.data.settings.apiKey) {
      wx.showToast({ title: '请先在设置里填 API Key', icon: 'none' })
      return
    }
    var session = this.data.session
    if (!session) return
    var now = Date.now()
    var userMsg = {
      id: 'msg_' + now + '_u',
      role: 'user',
      content: text,
      createdAt: now
    }
    var assistantMsg = {
      id: 'msg_' + now + '_a',
      role: 'assistant',
      content: '',
      createdAt: now,
      pending: true
    }
    session.messages.push(userMsg)
    session.messages.push(assistantMsg)
    if (session.messages.filter(function (m) { return m.role === 'user' }).length === 1) {
      session.title = text.length > 20 ? text.slice(0, 20) : text
    }
    chatStore.updateSession(session)
    this.setData({
      session: session,
      messages: session.messages,
      input: '',
      sending: true,
      lastMsgId: assistantMsg.id
    })

    var self = this
    var messages = sf.buildMessages(session)
    sf.chatStream(
      self.data.settings.apiKey,
      session.model,
      messages,
      {
        onChunk: function (delta) {
          var cur = self.data.session
          for (var i = 0; i < cur.messages.length; i++) {
            if (cur.messages[i].id === assistantMsg.id) {
              cur.messages[i].content += delta
              break
            }
          }
          self.setData({ messages: cur.messages })
        },
        onDone: function () {
          var cur = self.data.session
          for (var j = 0; j < cur.messages.length; j++) {
            if (cur.messages[j].id === assistantMsg.id) {
              cur.messages[j].pending = false
              break
            }
          }
          chatStore.updateSession(cur)
          self.setData({ messages: cur.messages, sending: false, session: cur })
          self._refreshSessionList()
        },
        onError: function (msg) {
          var cur = self.data.session
          for (var k = 0; k < cur.messages.length; k++) {
            if (cur.messages[k].id === assistantMsg.id) {
              cur.messages[k].pending = false
              cur.messages[k].error = msg
              break
            }
          }
          chatStore.updateSession(cur)
          self.setData({ messages: cur.messages, sending: false, session: cur })
          wx.showToast({ title: msg, icon: 'none' })
        }
      }
    )
  },

  onNewChat: function () {
    var s = chatStore.createNewSession()
    this.setData({ session: s, messages: s.messages, input: '', sending: false, lastMsgId: '' })
    this._refreshSessionList()
  },

  onOpenDrawer: function () {
    this._refreshSessionList()
    this.setData({ showDrawer: true })
  },

  onCloseDrawer: function () {
    this.setData({ showDrawer: false })
  },

  onPickSession: function (e) {
    var id = e.currentTarget.dataset.id
    chatStore.setActiveId(id)
    var list = chatStore.listSessions()
    var target = null
    for (var i = 0; i < list.length; i++) {
      if (list[i].id === id) { target = list[i]; break }
    }
    this.setData({
      session: target,
      messages: target ? target.messages : [],
      showDrawer: false,
      lastMsgId: ''
    })
  },

  onDeleteSession: function (e) {
    var id = e.currentTarget.dataset.id
    var self = this
    wx.showModal({
      title: '删除对话',
      content: '确定删除此对话？',
      success: function (res) {
        if (res.confirm) {
          chatStore.deleteSession(id)
          self._reload()
        }
      }
    })
  },

  onClearAll: function () {
    var self = this
    wx.showModal({
      title: '清空所有',
      content: '将清空所有对话历史，不可恢复。',
      success: function (res) {
        if (res.confirm) {
          chatStore.clearAll()
          self._reload()
        }
      }
    })
  },

  onGoSettings: function () {
    wx.navigateTo({ url: '/pages/aiChat/settings' })
  },

  _reload: function () {
    var settings = chatStore.getSettings()
    var session = chatStore.getActiveSession()
    this.setData({
      settings: settings,
      session: session,
      messages: session.messages
    })
    this._refreshSessionList()
  },

  _refreshSessionList: function () {
    this.setData({ sessions: chatStore.listSessions() })
  }
})
