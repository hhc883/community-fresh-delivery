// WeiXinApp/common/utils/siliconflow.js
// 调 SiliconFlow OpenAI 兼容 API
// 端点: https://api.siliconflow.cn/v1/chat/completions

var BASE_URL = 'https://api.siliconflow.cn/v1'

// 已知模型下拉（设置页用）
var MODELS = [
  { id: 'Qwen/Qwen2.5-7B-Instruct', name: 'Qwen2.5-7B（免费档）' },
  { id: 'Qwen/Qwen2.5-14B-Instruct', name: 'Qwen2.5-14B' },
  { id: 'Qwen/Qwen2.5-32B-Instruct', name: 'Qwen2.5-32B' },
  { id: 'Qwen/Qwen2.5-72B-Instruct', name: 'Qwen2.5-72B' },
  { id: 'deepseek-ai/DeepSeek-V2.5', name: 'DeepSeek-V2.5' },
  { id: 'deepseek-ai/DeepSeek-V3', name: 'DeepSeek-V3' },
  { id: 'THUDM/glm-4-9b-chat', name: 'GLM-4-9B' },
  { id: 'meta-llama/Meta-Llama-3.1-8B-Instruct', name: 'Llama-3.1-8B' }
]

// 构建 messages 数组（注入 system）
function buildMessages(session) {
  var arr = []
  if (session.systemPrompt) {
    arr.push({ role: 'system', content: session.systemPrompt })
  }
  for (var i = 0; i < session.messages.length; i++) {
    var m = session.messages[i]
    if (m.role !== 'system' && !m.error) {
      arr.push({ role: m.role, content: m.content })
    }
  }
  return arr
}

// 非流式一次性请求
function chatOnce(apiKey, model, messages) {
  return new Promise(function (resolve, reject) {
    wx.request({
      url: BASE_URL + '/chat/completions',
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + apiKey,
        'Content-Type': 'application/json'
      },
      data: {
        model: model,
        messages: messages,
        stream: false,
        temperature: 0.7,
        max_tokens: 2048
      },
      timeout: 60000,
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.choices && res.data.choices[0]) {
          resolve(res.data.choices[0].message.content || '')
        } else if (res.statusCode === 401 || res.statusCode === 403) {
          reject(new Error('API Key 无效或余额不足（HTTP ' + res.statusCode + '）'))
        } else {
          var msg = (res.data && res.data.error && res.data.error.message) || ('HTTP ' + res.statusCode)
          reject(new Error(msg))
        }
      },
      fail: function (err) {
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

// 流式：微信小程序原生不支持真 SSE，UniApp 在 enableChunked 时表现不稳定。
// 本期直接走非流式（chatOnce）。后续可加流式优化。
// 为保持接口一致，导出 chatStream（暂时等价 chatOnce），未来切换实现不影响调用方。
function chatStream(apiKey, model, messages, callbacks) {
  // callbacks: { onChunk, onDone, onError }
  chatOnce(apiKey, model, messages).then(function (text) {
    if (callbacks.onChunk) callbacks.onChunk(text)
    if (callbacks.onDone) callbacks.onDone(text)
  }).catch(function (err) {
    if (callbacks.onError) callbacks.onError(err.message || String(err))
  })
}

module.exports = {
  MODELS: MODELS,
  BASE_URL: BASE_URL,
  buildMessages: buildMessages,
  chatOnce: chatOnce,
  chatStream: chatStream
}
