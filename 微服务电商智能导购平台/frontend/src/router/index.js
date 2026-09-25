import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import Home from '../views/Home.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue'),
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/Cart.vue'),
  },
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('../views/OrderList.vue'),
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('../views/OrderDetail.vue'),
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('../views/Favorites.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue'),
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/admin/AdminDashboard.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    component: () => import('../views/admin/AdminProducts.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/products/add',
    name: 'AdminProductAdd',
    component: () => import('../views/admin/AdminProductForm.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/products/:id/edit',
    name: 'AdminProductEdit',
    component: () => import('../views/admin/AdminProductForm.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('../views/admin/AdminOrders.vue'),
    meta: { requiresAdmin: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：已登录才能访问需要登录的页面
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 管理员页面检查
  if (to.meta.requiresAdmin) {
    if (!userStore.isLoggedIn) {
      next('/')
      return
    }
    if (!userStore.isAdmin) {
      next('/')
      return
    }
  }

  next()
})

export default router
