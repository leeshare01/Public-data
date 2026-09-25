import { http, HttpResponse } from 'msw'
import {
  users, addresses, tokens,
  genToken, findUserByAccount, getUserFromToken, wrap, nextId,
} from '../data'

export const userHandlers = [
  // ---- 注册 ----
  http.post('/api/user/register', async ({ request }) => {
    const body = await request.json()
    const { username, password, phone, email, nickname } = body

    if (users.find(u => u.username === username)) {
      return HttpResponse.json({ code: 400, message: '用户名已存在', data: null, timestamp: Date.now() }, { status: 400 })
    }
    if (email && users.find(u => u.email === email)) {
      return HttpResponse.json({ code: 400, message: '邮箱已被注册', data: null, timestamp: Date.now() }, { status: 400 })
    }

    const newUser = {
      id: nextId('nextUserId'), username, password,
      nickname: nickname || username,
      phone: phone || '', email: email || '',
      avatar: '', gender: 0, role: 'USER',
    }
    users.push(newUser)
    return HttpResponse.json(wrap({ userId: newUser.id, username: newUser.username }))
  }),

  // ---- 登录 ----
  http.post('/api/user/login', async ({ request }) => {
    const body = await request.json()
    const { account, password } = body

    const user = findUserByAccount(account)
    if (!user) {
      return HttpResponse.json({ code: 400, message: '账号不存在', data: null, timestamp: Date.now() }, { status: 400 })
    }
    if (user.password !== password) {
      return HttpResponse.json({ code: 400, message: '密码错误', data: null, timestamp: Date.now() }, { status: 400 })
    }

    const token = genToken(user.id)
    tokens[token] = user.id
    return HttpResponse.json(wrap({
      token,
      userId: user.id,
      username: user.username,
      nickname: user.nickname,
      role: user.role,
    }))
  }),

  // ---- 获取当前用户信息 ----
  http.get('/api/user/me', ({ request }) => {
    const auth = request.headers.get('Authorization') || ''
    const token = auth.replace('Bearer ', '')
    const user = getUserFromToken(token)
    if (!user) {
      return HttpResponse.json({ code: 401, message: '未认证', data: null, timestamp: Date.now() }, { status: 401 })
    }
    return HttpResponse.json(wrap({
      id: user.id, username: user.username, nickname: user.nickname,
      phone: user.phone, email: user.email, avatar: user.avatar,
      gender: user.gender, role: user.role,
    }))
  }),

  // ---- 更新个人信息 ----
  http.put('/api/user/profile', async ({ request }) => {
    const auth = request.headers.get('Authorization') || ''
    const user = getUserFromToken(auth.replace('Bearer ', ''))
    if (!user) return HttpResponse.json({ code: 401, message: '未认证', data: null }, { status: 401 })

    const body = await request.json()
    Object.assign(user, body)
    return HttpResponse.json(wrap({ userId: user.id }))
  }),

  // ---- 修改密码 ----
  http.put('/api/user/password', async ({ request }) => {
    const auth = request.headers.get('Authorization') || ''
    const user = getUserFromToken(auth.replace('Bearer ', ''))
    if (!user) return HttpResponse.json({ code: 401, message: '未认证', data: null }, { status: 401 })

    const { oldPassword, newPassword } = await request.json()
    if (user.password !== oldPassword) {
      return HttpResponse.json({ code: 400, message: '原密码错误', data: null }, { status: 400 })
    }
    user.password = newPassword
    return HttpResponse.json(wrap(null))
  }),

  // ---- 退出登录 ----
  http.post('/api/user/logout', ({ request }) => {
    const auth = request.headers.get('Authorization') || ''
    delete tokens[auth.replace('Bearer ', '')]
    return HttpResponse.json(wrap(null))
  }),

  // ---- 获取地址列表 ----
  http.get('/api/user/addresses', () => {
    return HttpResponse.json(wrap(addresses))
  }),

  // ---- 新增地址 ----
  http.post('/api/user/addresses', async ({ request }) => {
    const body = await request.json()
    const addr = { id: nextId('nextAddrId'), ...body }
    if (body.isDefault) {
      addresses.forEach(a => a.isDefault = false)
    }
    addresses.push(addr)
    return HttpResponse.json(wrap({ id: addr.id }))
  }),

  // ---- 更新地址 ----
  http.put('/api/user/addresses/:id', async ({ params, request }) => {
    const body = await request.json()
    const addr = addresses.find(a => a.id === Number(params.id))
    if (!addr) return HttpResponse.json({ code: 404, message: '地址不存在', data: null }, { status: 404 })
    if (body.isDefault) {
      addresses.forEach(a => a.isDefault = false)
    }
    Object.assign(addr, body)
    return HttpResponse.json(wrap(null))
  }),

  // ---- 删除地址 ----
  http.delete('/api/user/addresses/:id', ({ params }) => {
    const idx = addresses.findIndex(a => a.id === Number(params.id))
    if (idx >= 0) addresses.splice(idx, 1)
    return HttpResponse.json(wrap(null))
  }),

  // ---- 设置默认地址 ----
  http.put('/api/user/addresses/:id/default', ({ params }) => {
    addresses.forEach(a => a.isDefault = false)
    const addr = addresses.find(a => a.id === Number(params.id))
    if (addr) addr.isDefault = true
    return HttpResponse.json(wrap(null))
  }),
]
