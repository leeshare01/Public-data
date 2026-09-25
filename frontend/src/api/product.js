import request from './request'

export const productApi = {
  /** 获取分类树 */
  getCategoryTree() {
    return request.get('/product/category/tree')
  },

  /** 获取一级分类列表 */
  getCategories(params) {
    return request.get('/product/category/top', { params })
  },

  /** 商品列表（分页+搜索+筛选+排序） */
  getProducts(params) {
    return request.get('/product/page', { params })
  },

  /** 商品详情 */
  getProductDetail(productId) {
    return request.get(`/product/${productId}`)
  },

  /** 关键词搜索（复用分页接口） */
  searchProducts(params) {
    return request.get('/product/page', { params })
  },

  /** 管理员 - 商品列表（含下架商品） */
  getAdminProducts(params) {
    return request.get('/product/admin/page', { params })
  },

  /** 管理员 - 获取商品详情（不限状态） */
  getAdminProductDetail(id) {
    return request.get(`/product/admin/${id}`)
  },

  /** 新增商品 */
  createProduct(data) {
    return request.post('/product', data)
  },

  /** 更新商品 */
  updateProduct(id, data) {
    return request.put(`/product/${id}`, data)
  },

  /** 删除商品 */
  deleteProduct(id) {
    return request.delete(`/product/${id}`)
  },

  /** 批量查询商品详情（按 ID 列表） */
  batchGetProducts(ids) {
    return request.post('/product/batch', ids)
  },
}
