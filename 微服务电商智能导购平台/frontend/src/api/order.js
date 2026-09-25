import request from './request'

export const orderApi = {
  /** 创建订单 */
  create(data) {
    return request.post('/order/orders', data)
  },

  /** 订单列表 */
  getList(params) {
    return request.get('/order/orders', { params })
  },

  /** 订单详情 */
  getDetail(orderId) {
    return request.get(`/order/orders/${orderId}`)
  },

  /** 取消订单 */
  cancel(orderId, data) {
    return request.put(`/order/orders/${orderId}/cancel`, data)
  },

  /** 确认收货 */
  confirmReceive(orderId) {
    return request.put(`/order/orders/${orderId}/confirm-receive`)
  },

  /** 模拟支付 */
  pay(orderId) {
    return request.put(`/order/orders/${orderId}/pay`)
  },
}
