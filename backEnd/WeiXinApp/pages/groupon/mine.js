// WeiXinApp/pages/groupon/mine.js
// 社区团购 —— 我的拼团
var grouponApi = require('../../common/utils/groupon.js')

var STATUS_TEXT = {
  1: '拼团中',
  2: '已成团',
  3: '已失败',
  4: '已过期'
}

Page({
  data: {
    tabs: [
      { label: '全部', status: null },
      { label: '拼团中', status: 1 },
      { label: '已成团', status: 2 },
      { label: '未成团', status: 4 }
    ],
    activeIndex: 0,
    list: [],
    loading: true
  },

  onShow: function () {
    this.loadList()
  },

  onTab: function (e) {
    this.setData({ activeIndex: Number(e.currentTarget.dataset.index) }, this.loadList)
  },

  loadList: function () {
    var that = this
    this.setData({ loading: true })
    var status = this.data.tabs[this.data.activeIndex].status

    grouponApi.getMine(status).then(function (res) {
      var list = (res.data || []).map(function (g) {
        g.statusText = STATUS_TEXT[g.status] || '未知'
        return g
      })
      that.setData({ list: list, loading: false })
    }).catch(function () {
      that.setData({ loading: false })
    })
  },

  onDetail: function (e) {
    wx.navigateTo({
      url: '/pages/groupon/detail?grouponId=' + e.currentTarget.dataset.id
    })
  },

  goList: function () {
    wx.navigateTo({ url: '/pages/groupon/list' })
  }
})
