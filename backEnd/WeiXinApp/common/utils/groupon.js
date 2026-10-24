// WeiXinApp/common/utils/groupon.js
// 社区团购接口封装

var http = require('./request.js')

module.exports = {
  /** 拼团商品列表（社区团购首页） */
  getGrouponDishList: function (keyword) {
    return http.get('/user/groupon/dish/list', keyword ? { keyword: keyword } : {})
  },

  /**
   * 开团：以拼团价购买商品并成为团长
   * @param {object} data { dishId, number, addressBookId, pickupPointId, remark }
   */
  openGroupon: function (data) {
    return http.post('/user/groupon/open', data)
  },

  /**
   * 参团：加入他人已开的团
   * @param {object} data { grouponId, number, addressBookId, pickupPointId, remark }
   */
  joinGroupon: function (data) {
    return http.post('/user/groupon/join', data)
  },

  /** 拼团详情 */
  getDetail: function (id) {
    return http.get('/user/groupon/detail/' + id)
  },

  /** 某商品当前可加入的团 */
  getJoinable: function (dishId) {
    return http.get('/user/groupon/joinable/' + dishId)
  },

  /** 我的拼团 */
  getMine: function (status) {
    return http.get('/user/groupon/mine', status ? { status: status } : {})
  },

  /** 自提点列表 */
  getPickupPoints: function (community) {
    return http.get('/user/pickupPoint/list', community ? { community: community } : {})
  },

  /** 收货地址列表 */
  getAddressList: function () {
    return http.get('/user/addressBook/list')
  }
}
