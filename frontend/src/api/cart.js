import request from './request'

export const cartApi = {
  /** 获取购物车列表 */
  getItems() {
    return request.get('/cart/items')
  },

  /** 添加商品到购物车 */
  addItem(data) {
    return request.post('/cart/items', data)
  },

  /** 修改商品数量 */
  updateItem(skuId, data) {
    return request.put(`/cart/items/${skuId}`, data)
  },

  /** 删除购物车商品 */
  removeItem(skuId) {
    return request.delete(`/cart/items/${skuId}`)
  },

  /** 选中/取消选中 */
  selectItem(skuId, data) {
    return request.put(`/cart/items/${skuId}/select`, data)
  },

  /** 全选/全不选 */
  selectAll(params) {
    return request.put('/cart/select-all', null, { params })
  },

  /** 清空购物车 */
  clear() {
    return request.delete('/cart/items')
  },

  /** 获取选中商品汇总 */
  getSummary() {
    return request.get('/cart/summary')
  },
}
