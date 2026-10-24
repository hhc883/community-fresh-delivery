// WeiXinApp/pages/groupon/confirm.js
// 社区团购 —— 确认下单页（开团 / 参团共用）
var grouponApi = require('../../common/utils/groupon.js')

Page({
  data: {
    mode: 'open', // open=开团, join=参团
    dishId: null,
    grouponId: null,
    dishName: '',
    dishImage: '',
    price: '',
    number: 1,
    remark: '',

    // 履约方式：pickup=社区自提点, address=送货到家
    deliveryType: 'pickup',
    pickupPoints: [],
    pickupPointIndex: -1,
    addresses: [],
    addressIndex: -1,

    submitting: false
  },

  onLoad: function (options) {
    this.setData({
      mode: options.mode || 'open',
      dishId: options.dishId ? Number(options.dishId) : null,
      grouponId: options.grouponId ? Number(options.grouponId) : null,
      dishName: decodeURIComponent(options.name || ''),
      dishImage: decodeURIComponent(options.image || ''),
      price: options.price || ''
    })
    wx.setNavigationBarTitle({
      title: this.data.mode === 'open' ? '确认开团' : '确认参团'
    })
    this.loadPickupPoints()
    this.loadAddresses()
  },

  loadPickupPoints: function () {
    var that = this
    grouponApi.getPickupPoints().then(function (res) {
      that.setData({ pickupPoints: res.data || [] })
    }).catch(function () {})
  },

  loadAddresses: function () {
    var that = this
    grouponApi.getAddressList().then(function (res) {
      that.setData({ addresses: res.data || [] })
    }).catch(function () {})
  },

  onDeliveryType: function (e) {
    this.setData({ deliveryType: e.currentTarget.dataset.type })
  },

  onPickupChange: function (e) {
    this.setData({ pickupPointIndex: Number(e.detail.value) })
  },

  onAddressChange: function (e) {
    this.setData({ addressIndex: Number(e.detail.value) })
  },

  onNumberChange: function (e) {
    var n = parseInt(e.detail.value, 10)
    if (isNaN(n) || n < 1) n = 1
    this.setData({ number: n })
  },

  onMinus: function () {
    if (this.data.number > 1) {
      this.setData({ number: this.data.number - 1 })
    }
  },

  onPlus: function () {
    this.setData({ number: this.data.number + 1 })
  },

  onRemark: function (e) {
    this.setData({ remark: e.detail.value })
  },

  /** 计算合计 */
  totalAmount: function () {
    var p = parseFloat(this.data.price)
    if (isNaN(p)) p = 0
    return (p * this.data.number).toFixed(2)
  },

  onSubmit: function () {
    var that = this
    if (this.data.submitting) return

    var payload = {
      number: this.data.number,
      remark: this.data.remark
    }

    // 履约方式校验
    if (this.data.deliveryType === 'pickup') {
      if (this.data.pickupPointIndex < 0) {
        wx.showToast({ title: '请选择自提点', icon: 'none' })
        return
      }
      payload.pickupPointId = this.data.pickupPoints[this.data.pickupPointIndex].id
    } else {
      if (this.data.addressIndex < 0) {
        wx.showToast({ title: '请选择收货地址', icon: 'none' })
        return
      }
      payload.addressBookId = this.data.addresses[this.data.addressIndex].id
    }

    var promise
    if (this.data.mode === 'open') {
      payload.dishId = this.data.dishId
      promise = grouponApi.openGroupon(payload)
    } else {
      payload.grouponId = this.data.grouponId
      promise = grouponApi.joinGroupon(payload)
    }

    this.setData({ submitting: true })
    promise.then(function (res) {
      that.setData({ submitting: false })
      var g = res.data || {}
      wx.showToast({
        title: that.data.mode === 'open' ? '开团成功' : '参团成功',
        icon: 'success'
      })
      // 跳转到拼团详情，方便查看还差几人成团
      setTimeout(function () {
        wx.redirectTo({ url: '/pages/groupon/detail?grouponId=' + g.id })
      }, 800)
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
