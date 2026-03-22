import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import Layout from '../views/Layout.vue'
import PredictView from '../views/PredictView.vue'
import VisualizationView from '../views/VisualizationView.vue'
import TaskHistoryView from '../views/TaskHistoryView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/register',
      name: 'register',
      component: LoginView,
    },
    {
      path: '/',
      component: Layout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView,
        },
        {
          path: '/predict',
          name: 'predict',
          component: PredictView,
        },
        {
          path: '/notices',
          name: 'notices',
          component: () => import('../views/NoticeList.vue'),
        },
        {
          path: '/visualization',
          name: 'visualization',
          component: VisualizationView,
        },
        {
          path: '/tasks',
          name: 'tasks',
          component: TaskHistoryView,
        },
        {
          path: '/profile',
          name: 'profile',
          // route level code-splitting
          // this generates a separate chunk (Profile.[hash].js) for this route
          // which is lazy-loaded when the route is visited.
          component: () => import('../views/UserProfile.vue'),
        },
        {
          path: '/admin',
          name: 'admin',
          component: () => import('../views/AdminDashboard.vue'),
          meta: { requiresAdmin: true }
        }
        ,
        {
          path: '/admin/visualization',
          name: 'admin-visualization',
          redirect: '/visualization'
        }
      ]
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

// src/router/index.ts
// 路由守卫
router.beforeEach((to, from, next) => {
  // 获取认证状态
  const authStore = useAuthStore()
  const isAuthenticated = !!authStore.token

  // 白名单路由（允许未登录访问的页面）
  const whiteList = ['/login', '/register', '/', '/about', '/notices', '/predict']

  // 如果访问的是白名单路由，直接通过
  if (whiteList.includes(to.path)) {
    next()
    return
  }

  // 如果未认证，跳转到登录页
  if (!isAuthenticated) {
    if (!whiteList.includes(to.path)) {
      ElMessage.warning('请先登录')
    }
    next('/login')
    return
  }

  // 管理员权限检查
  if (to.matched.some(record => record.meta.requiresAdmin)) {
    const role = authStore.userInfo?.role
    if (role !== 'ADMIN') {
      ElMessage.error('无权访问管理员页面')
      next('/')
      return
    }
  }

  next()
})

export default router
