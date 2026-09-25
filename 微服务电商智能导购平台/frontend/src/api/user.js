import request from './request'

export const userApi = {
  /** 注册 */
  register(data) {
    return request.post('/user/register', data)
  },

  /** 登录 */
  login(data) {
    return request.post('/user/login', data)
  },

  /** 获取当前用户信息 */
  getMe() {
    return request.get('/user/me')
  },

  /** 更新个人信息 */
  updateProfile(data) {
    return request.put('/user/profile', data)
  },

  /** 修改密码 */
  updatePassword(data) {
    return request.put('/user/password', data)
  },

  /** 退出登录 */
  logout() {
    return request.post('/user/logout')
  },

  /** 获取地址列表 */
  getAddresses() {
    return request.get('/user/addresses')
  },

  /** 新增地址 */
  addAddress(data) {
    return request.post('/user/addresses', data)
  },

  /** 更新地址 */
  updateAddress(id, data) {
    return request.put(`/user/addresses/${id}`, data)
  },

  /** 删除地址 */
  deleteAddress(id) {
    return request.delete(`/user/addresses/${id}`)
  },

  /** 设置默认地址 */
  setDefaultAddress(id) {
    return request.put(`/user/addresses/${id}/default`)
  },

  /** 管理员 - 用户总数 */
  getUserCount() {
    return request.get('/user/admin/count')
  },

  /** 管理员 - 用户列表 */
  getAdminUsers(params) {
    return request.get('/user/admin/list', { params })
  },
}
