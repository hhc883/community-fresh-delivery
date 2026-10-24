// WeiXinApp/pages/groupon/detail.js
// 社区团购 —— 某商品的可参团列表 + 拼团详情
var grouponApi = require('../../common/utils/groupon.js')

Page({
  data: {
    dishId: null,
    grouponId: null,
    joinableList: [],
    detail: null,
    loading: true,
    countdownText: ''
  },

  onLoad: function (options) {
    // 两种进入方式：带 dishId（查看可参团列表）或带 grouponId（查看某团详情）
    if (options.dishId) {
      this.setData({ dishId: options.dishId })
      this.loadJoinable()
    } else if (options.grouponId) {
      this.setData({ grouponId: options.grouponId })
      this.loadDetail(options.grouponId)
    }
  },

  onShow: function () {
    // 返回本页时刷新，保证参团后人数/状态是最新的
    if (this.data.dishId) {
      this.loadJoinable()
    } else if (this.data.grouponId) {
      this.loadDetail(this.data.grouponId)
    }
  },

  onUnload: function () {
    this.clearTimer()
  },

  /** 加载该商品可加入的团 */
  loadJoinable: function () {
    var that = this
    this.setData({ loading: true })
    grouponApi.getJoinable(this.data.dishId).then(function (res) {
      var list = (res.data || []).map(function (g) {
        g.countdownText = that.formatRemain(g.remainMillis)
        return g
      })
      that.setData({ joinableList: list, loading: false })
      that.startTimer()
    }).catch(function () {
      that.setData({ loading: false })
    })
  },

  /** 加载某个团的详情 */
  loadDetail: function (id) {
    var that = this
    this.setData({ loading: true })
    grouponApi.getDetail(id).then(function (res) {
      var d = res.data || {}
      d.countdownText = that.formatRemain(d.remainMillis)
      that.setData({ detail: d, loading: false })
      that.startTimer()
    }).catch(function () {
      that.setData({ loading: false })
    })
  },

  /** 倒计时：每秒刷新剩余时间 */
  startTimer: function () {
    var that = this
    this.clearTimer()
    this._timer = setInterval(function () {
      if (that.data.detail) {
        var d = that.data.detail
        var remain = (d.remainMillis || 0) - 1000
        d.remainMillis = remain > 0 ? remain : 0
        d.countdownText = that.formatRemain(d.remainMillis)
        that.setData({ detail: d })
      }
      var list = that.data.joinableList
      if (list && list.length) {
        var changed = false
        list = list.map(function (g) {
          var remain = (g.remainMillis || 0) - 1000
          remain = remain > 0 ? remain : 0
          if (remain !== g.remainMillis) changed = true
          g.remainMillis = remain
          g.countdownText = that.formatRemain(remain)
          return g
        })
        if (changed) that.setData({ joinableList: list })
      }
    }, 1000)
  },

  clearTimer: function () {
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
  },

  /** 毫秒 -> "xx小时xx分xx秒" */
  formatRemain: function (millis) {
    if (!millis || millis <= 0) return '已结束'
    var total = Math.floor(millis / 1000)
    var h = Math.floor(total / 3600)
    var m = Math.floor((total % 3600) / 60)
    var s = total % 60
    return (h > 0 ? h + '小时' : '') + m + '分' + s + '秒'
  },

  /** 参团 */
  onJoin: function (e) {
    var grouponId = e.currentTarget.dataset.id
    var g = null
    for (var i = 0; i < this.data.joinableList.length; i++) {
      if (String(this.data.joinableList[i].id) === String(grouponId)) {
        g = this.data.joinableList[i]
        break
      }
    }
    if (!g) return
    wx.navigateTo({
      url: '/pages/groupon/confirm?grouponId=' + g.id + '&mode=join'
        + '&dishId=' + g.dishId
        + '&name=' + encodeURIComponent(g.dishName || '')
        + '&price=' + g.grouponPrice
        + '&image=' + encodeURIComponent(g.dishImage || '')
    })
  },

  /** 查看成员 */
  onViewMembers: function () {
    if (!this.data.detail) return
    var names = (this.data.detail.members || []).map(function (m) {
      return (m.isLeader === 1 ? '👑 ' : '') + (m.userName || '用户')
    })
    wx.showModal({
      title: '参团成员',
      content: names.join('\n') || '暂无成员',
      showCancel: false
    })
  },

  goList: function () {
    wx.navigateTo({ url: '/pages/groupon/list' })
  }
})
