// WeiXinApp/pages/groupon/list.js
// 社区团购 —— 拼团商品列表
var grouponApi = require('../../common/utils/groupon.js')
var config = require('../../common/utils/config.js')

Page({
  data: {
    dishList: [],
    loading: true,
    keyword: '',
    baseUrl: config.baseUrl
  },

  onShow: function () {
    this.loadList()
  },

  onPullDownRefresh: function () {
    var that = this
    this.loadList(function () {
      wx.stopPullDownRefresh()
    })
  },

  onKeyword: function (e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch: function () {
    this.loadList()
  },

  /**
   * 加载参与拼团的商品（后端 /user/groupon/dish/list 已按 isGroupon=1 过滤）
   */
  loadList: function (done) {
    var that = this
    this.setData({ loading: true })

    grouponApi.getGrouponDishList(this.data.keyword).then(function (res) {
      that.setData({ dishList: res.data || [], loading: false })
      if (done) done()
    }).catch(function () {
      that.setData({ loading: false })
      if (done) done()
    })
  },

  /** 查看该商品可加入的团 */
  onJoin: function (e) {
    var dishId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/groupon/detail?dishId=' + dishId
    })
  },

  /** 直接开团 */
  onOpen: function (e) {
    var dish = e.currentTarget.dataset.dish
    wx.navigateTo({
      url: '/pages/groupon/confirm?dishId=' + dish.id + '&mode=open'
        + '&name=' + encodeURIComponent(dish.name || '')
        + '&price=' + (dish.grouponPrice || dish.price || '')
        + '&image=' + encodeURIComponent(dish.image || '')
    })
  },

  goMine: function () {
    wx.navigateTo({ url: '/pages/groupon/mine' })
  }
})
