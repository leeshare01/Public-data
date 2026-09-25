/**
 * Mock 运行时数据（模拟后端数据库）
 * 所有可变数据通过 getter/setter 方法访问，
 * 避免 ES Module 导入只读绑定的问题。
 */
import { mockProducts } from './products'

// ========================= 内部状态 =========================
const _state = {
  nextUserId: 1002,
  nextCartId: 1,
  nextOrderId: 5001,
  nextOrderNo: 202607180001,
  nextConvId: 1,
  nextAddrId: 20,
}

// ========================= 计数器方法 =========================
export function nextId(name) {
  return _state[name]++
}

// ========================= 用户 =========================
export const users = [
  {
    id: 1001, username: 'demo', password: '123456',
    nickname: 'Demo用户', phone: '13800138000',
    email: 'demo@shop.com', avatar: '', gender: 0, role: 'USER',
  },
]

export const tokens = {}
tokens['eyJhbGciOiJIUzI1NiJ9.demo'] = 1001

export function genToken(userId) {
  return `eyJhbGciOiJIUzI1NiJ9.user_${userId}_${Date.now()}`
}

export function findUserByAccount(account) {
  return users.find(u =>
    u.username === account || u.email === account || u.phone === account
  )
}

export function getUserFromToken(token) {
  const userId = tokens[token]
  return users.find(u => u.id === userId)
}

// ========================= 商品 =========================
export const products = mockProducts.map((p, i) => ({
  ...p,
  id: p.id,
  mainImage: p.image,
  categoryId: (() => {
    const map = {
    1: 11,
    2: 11,
    3: 11,
    4: 11,
    10: 12,
    11: 12,
    20: 13,
    22: 13,
    24: 14,
    26: 14,
    30: 15,
    31: 15,
    33: 15,
    34: 15,
    36: 16,
    37: 16,
    42: 17,
    44: 17,
    47: 18,
    48: 18,
    52: 21,
    58: 22,
    60: 22,
    65: 23,
    66: 23,
    72: 24,
    73: 24,
    78: 25,
    79: 25,
    80: 25,
    84: 31,
    86: 31,
    90: 32,
    91: 32,
    93: 32,
    96: 33,
    97: 33,
    102: 34,
    103: 34,
    108: 35,
    114: 41,
    115: 41,
    116: 41,
    117: 41,
    118: 41,
    120: 42,
    121: 42,
    122: 42,
    123: 42,
    125: 42,
    };
    return map[p.id] || 1;
  })(),
  sales: p.sales || Math.floor(Math.random() * 5000),
  status: 1,
  skuList: p.colors?.flatMap(c =>
    (p.sizes || ['默认']).map((s, si) => ({
      id: p.id * 100 + si + 1,
      specValues: p.sizes ? { '颜色': c, '尺寸': s } : { '颜色': c },
      price: p.price + (si * 10),
      stock: Math.floor(Math.random() * 200) + 10,
      image: p.image,
    }))
  ) || [{
    id: p.id * 100 + 1,
    specValues: {},
    price: p.price,
    stock: Math.floor(Math.random() * 200) + 10,
    image: p.image,
  }],
}))

export const categories = [
  { id: 1, name: '男装', level: 1, icon: '', children: [
    { id: 11, name: 'T恤', level: 2, children: [] },
    { id: 12, name: '衬衫', level: 2, children: [] },
    { id: 13, name: '外套', level: 2, children: [] },
    { id: 14, name: '裤子', level: 2, children: [] },
  ]},
  { id: 2, name: '女装', level: 1, icon: '', children: [
    { id: 15, name: '连衣裙', level: 2, children: [] },
    { id: 16, name: '上衣', level: 2, children: [] },
    { id: 17, name: '外套', level: 2, children: [] },
    { id: 18, name: '裙子', level: 2, children: [] },
  ]},
  { id: 3, name: '数码', level: 1, icon: '', children: [
    { id: 21, name: '手机配件', level: 2, children: [] },
    { id: 22, name: '耳机', level: 2, children: [] },
    { id: 23, name: '电脑外设', level: 2, children: [] },
    { id: 24, name: '充电配件', level: 2, children: [] },
    { id: 25, name: '智能穿戴', level: 2, children: [] },
  ]},
  { id: 4, name: '家居', level: 1, icon: '', children: [
    { id: 31, name: '厨房用品', level: 2, children: [] },
    { id: 32, name: '家纺', level: 2, children: [] },
    { id: 33, name: '家具装饰', level: 2, children: [] },
    { id: 34, name: '浴室用品', level: 2, children: [] },
    { id: 35, name: '收纳', level: 2, children: [] },
  ]},
  { id: 5, name: '食品', level: 1, icon: '', children: [
    { id: 41, name: '零食', level: 2, children: [] },
    { id: 42, name: '饮品', level: 2, children: [] },
  ]},
]

// ========================= 购物车 =========================
export const cartItems = []

// ========================= 订单 =========================
export const orders = []

// ========================= AI 对话 =========================
export const conversations = []

// ========================= 地址 =========================
export const addresses = [
  {
    id: 10, receiverName: '张三', receiverPhone: '13800138000',
    province: '广东省', city: '广州市', district: '天河区',
    detailAddress: '科技路100号', isDefault: true,
  },
]

// ========================= 工具 =========================
export function wrap(data) {
  return {
    code: 200, message: 'success',
    data,
    timestamp: Date.now(),
  }
}

export function paginate(list, page = 1, size = 10) {
  const start = (page - 1) * size
  return {
    records: list.slice(start, start + size),
    total: list.length,
    page,
    size,
    pages: Math.ceil(list.length / size),
  }
}
