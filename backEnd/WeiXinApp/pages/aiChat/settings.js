// WeiXinApp/pages/aiChat/settings.js
const chatStore = require('../../common/utils/chatStore.js')
const sf = require('../../common/utils/siliconflow.js')

Page({
  data: {
    apiKey: '',
    modelIndex: 0,
    systemPrompt: '',
    models: []
  },

  onLoad: function () {
    var s = chatStore.getSettings()
    var models = sf.MODELS
    var idx = 0
    for (var i = 0; i < models.length; i++) {
      if (models[i].id === s.defaultModel) { idx = i; break }
    }
    this.setData({
      apiKey: s.apiKey,
      modelIndex: idx,
      systemPrompt: s.systemPrompt,
      models: models
    })
  },

  onKeyInput: function (e) {
    this.setData({ apiKey: e.detail.value })
  },

  onModelChange: function (e) {
    this.setData({ modelIndex: parseInt(e.detail.value, 10) })
  },

  onPromptInput: function (e) {
    this.setData({ systemPrompt: e.detail.value })
  },

  onSave: function () {
    var models = this.data.models
    var idx = this.data.modelIndex
    var s = {
      apiKey: (this.data.apiKey || '').trim(),
      defaultModel: models[idx] ? models[idx].id : 'Qwen/Qwen2.5-7B-Instruct',
      systemPrompt: this.data.systemPrompt || ''
    }
    if (!s.apiKey) {
      wx.showToast({ title: '请填写 API Key', icon: 'none' })
      return
    }
    if (chatStore.setSettings(s)) {
      wx.showToast({ title: '已保存' })
      setTimeout(function () { wx.navigateBack() }, 600)
    } else {
      wx.showToast({ title: '保存失败', icon: 'none' })
    }
  }
})
