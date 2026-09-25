import { http, HttpResponse } from 'msw'
import { products, cartItems, getUserFromToken, wrap, nextId } from '../data'

export const cartHandlers = [
  // ---- 获取购物车 ----
  http.get('/api/cart/items', ({ request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const items = cartItems.filter(i => i.userId === user.id).map(i => ({
      skuId: i.skuId,
      productId: i.productId,
      productName: i.productName,
      specInfo: i.specInfo || '',
      image: i.image,
      price: i.price,
      quantity: i.quantity,
      selected: i.selected ?? true,
      stock: i.stock || 999,
    }))

    return HttpResponse.json(wrap({
      items,
      totalAmount: items.reduce((s, i) => s + i.price * i.quantity, 0),
      totalCount: items.reduce((s, i) => s + i.quantity, 0),
    }))
  }),

  // ---- 添加购物车 ----
  http.post('/api/cart/items', async ({ request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const { skuId, quantity = 1 } = await request.json()

    // 找到商品
    let prod = null
    for (const p of products) {
      if (p.id === skuId || p.skuList?.some(s => s.id === skuId)) {
        prod = p
        break
      }
    }
    if (!prod) return HttpResponse.json({ code: 404, message: '商品不存在', data: null }, { status: 404 })

    const existing = cartItems.find(i => i.userId === user.id && i.skuId === skuId)
    if (existing) {
      existing.quantity += quantity
    } else {
      cartItems.push({
        id: nextId('nextCartId'), userId: user.id, skuId: skuId || prod.id,
        productId: prod.id, productName: prod.name,
        specInfo: '',
        image: prod.image, price: prod.price,
        quantity, selected: true, stock: 999,
      })
    }
    return HttpResponse.json(wrap(null))
  }),

  // ---- 修改数量 ----
  http.put('/api/cart/items/:skuId', async ({ params, request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const { quantity } = await request.json()
    const item = cartItems.find(i => i.userId === user.id && i.skuId === Number(params.skuId))
    if (item) item.quantity = quantity
    return HttpResponse.json(wrap(null))
  }),

  // ---- 删除购物车商品 ----
  http.delete('/api/cart/items/:skuId', ({ params, request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const idx = cartItems.findIndex(i => i.userId === user.id && i.skuId === Number(params.skuId))
    if (idx >= 0) cartItems.splice(idx, 1)
    return HttpResponse.json(wrap(null))
  }),

  // ---- 选中/取消选中 ----
  http.put('/api/cart/items/:skuId/select', async ({ params, request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const { selected } = await request.json()
    const item = cartItems.find(i => i.userId === user.id && i.skuId === Number(params.skuId))
    if (item) item.selected = selected
    return HttpResponse.json(wrap(null))
  }),

  // ---- 全选/全不选 ----
  http.put('/api/cart/select-all', ({ request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()
    const url = new URL(request.url)
    const selected = url.searchParams.get('selected') === 'true'
    cartItems.filter(i => i.userId === user.id).forEach(i => i.selected = selected)
    return HttpResponse.json(wrap(null))
  }),

  // ---- 清空购物车 ----
  http.delete('/api/cart/items', ({ request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()
    // 原地修改
    const indices = cartItems.map((i, idx) => i.userId === user.id ? idx : -1).filter(i => i >= 0).reverse()
    indices.forEach(i => cartItems.splice(i, 1))
    return HttpResponse.json(wrap({ items: [], totalAmount: 0, totalCount: 0 }))
  }),

  // ---- 购物车汇总 ----
  http.get('/api/cart/summary', ({ request }) => {
    const user = checkAuth(request)
    if (!user) return unauthorized()

    const selected = cartItems.filter(i => i.userId === user.id && i.selected)
    return HttpResponse.json(wrap({
      items: selected,
      totalAmount: selected.reduce((s, i) => s + i.price * i.quantity, 0),
      totalCount: selected.reduce((s, i) => s + i.quantity, 0),
    }))
  }),
]

function checkAuth(request) {
  const auth = request.headers.get('Authorization') || ''
  return getUserFromToken(auth.replace('Bearer ', ''))
}

function unauthorized() {
  return HttpResponse.json({ code: 401, message: '未认证', data: null, timestamp: Date.now() }, { status: 401 })
}
