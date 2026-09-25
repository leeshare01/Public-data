import { http, HttpResponse } from 'msw'
import {
  users, cartItems, orders, addresses,
  getUserFromToken, wrap, paginate, nextId,
} from '../data'

export const orderHandlers = [
  // ---- 创建订单 ----
  http.post('/api/order/orders', async ({ request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const { addressId, skuIds, remark } = await request.json()

    // 从购物车提取商品
    const orderItems = cartItems
      .filter(i => i.userId === user.id && (skuIds || []).includes(i.skuId))
      .map(i => ({
        productName: i.productName,
        skuCode: `SKU-${i.skuId}`,
        specValues: i.specInfo || '',
        price: i.price,
        quantity: i.quantity,
        subtotal: i.price * i.quantity,
        image: i.image,
      }))

    if (orderItems.length === 0) {
      return HttpResponse.json({ code: 400, message: '没有选中的商品', data: null }, { status: 400 })
    }

    const addr = addresses.find(a => a.id === addressId) || addresses[0]
    const orderId = nextId('nextOrderId')

    const order = {
      id: orderId,
      orderNo: String(nextId('nextOrderNo')),
      userId: user.id,
      totalAmount: orderItems.reduce((s, i) => s + i.subtotal, 0),
      payAmount: orderItems.reduce((s, i) => s + i.subtotal, 0),
      status: 0,
      statusText: '待付款',
      consigneeName: addr?.receiverName || user.nickname,
      consigneePhone: addr?.receiverPhone || user.phone,
      consigneeAddress: addr ? `${addr.province}${addr.city}${addr.district}${addr.detailAddress}` : '',
      createTime: new Date().toLocaleString('zh-CN', { hour12: false }),
      items: orderItems,
      statusTimeline: [
        { status: 0, time: new Date().toLocaleString('zh-CN', { hour12: false }), desc: '订单创建' },
      ],
    }
    orders.push(order)

    // 从购物车移除
    const ids = new Set(skuIds || [])
    const rmIndices = cartItems.map((i, idx) => (i.userId === user.id && ids.has(i.skuId)) ? idx : -1).filter(i => i >= 0).reverse()
    rmIndices.forEach(i => cartItems.splice(i, 1))

    return HttpResponse.json(wrap({
      orderId: order.id,
      orderNo: order.orderNo,
      payAmount: order.payAmount,
      status: order.status,
    }))
  }),

  // ---- 订单列表 ----
  http.get('/api/order/orders', ({ request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page')) || 1
    const size = Number(url.searchParams.get('size')) || 10
    const status = url.searchParams.get('status')

    let list = orders.filter(o => o.userId === user.id)
    if (status !== null && status !== '') {
      list = list.filter(o => o.status === Number(status))
    }
    list.sort((a, b) => b.id - a.id)

    return HttpResponse.json(wrap(paginate(list, page, size)))
  }),

  // ---- 订单详情 ----
  http.get('/api/order/orders/:id', ({ params, request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const order = orders.find(o => o.id === Number(params.id) && o.userId === user.id)
    if (!order) {
      return HttpResponse.json({ code: 404, message: '订单不存在', data: null }, { status: 404 })
    }
    return HttpResponse.json(wrap(order))
  }),

  // ---- 取消订单 ----
  http.put('/api/order/orders/:id/cancel', async ({ params, request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const { reason } = await request.json()
    const order = orders.find(o => o.id === Number(params.id) && o.userId === user.id)
    if (!order) {
      return HttpResponse.json({ code: 404, message: '订单不存在', data: null }, { status: 404 })
    }
    if (order.status !== 0) {
      return HttpResponse.json({ code: 400, message: '当前状态不允许取消', data: null }, { status: 400 })
    }
    order.status = 4
    order.statusText = '已取消'
    order.statusTimeline.push({
      status: 4,
      time: new Date().toLocaleString('zh-CN', { hour12: false }),
      desc: `订单取消：${reason || '用户取消'}`,
    })
    return HttpResponse.json(wrap(null))
  }),

  // ---- 确认收货 ----
  http.put('/api/order/orders/:id/confirm-receive', ({ params, request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const order = orders.find(o => o.id === Number(params.id) && o.userId === user.id)
    if (!order) return HttpResponse.json({ code: 404, message: '订单不存在', data: null }, { status: 404 })
    order.status = 3
    order.statusText = '已完成'
    return HttpResponse.json(wrap(null))
  }),
]

function checkAuth(request) {
  const auth = request.headers.get('Authorization') || ''
  return getUserFromToken(auth.replace('Bearer ', ''))
}

function unauthorized() {
  return HttpResponse.json({ code: 401, message: '未认证', data: null, timestamp: Date.now() }, { status: 401 })
}
