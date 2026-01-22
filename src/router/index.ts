import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'

// 路由懒加载（按模块分包）
const Home = () => import(/* webpackChunkName: "home" */ '../views/Home.vue')
const Login = () => import(/* webpackChunkName: "auth" */ '../views/Login.vue')
const Register = () => import(/* webpackChunkName: "auth" */ '../views/Register.vue')

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { 
      title: '首页',
      requiresAuth: false, // 首页无需登录，但部分功能需要
      transition: 'fade'
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { 
      title: '登录',
      requiresAuth: false,
      transition: 'slide-right'
    },
    // 已登录用户禁止访问登录页
    beforeEnter: (to, from, next) => {
      if (localStorage.getItem('token')) {
        ElMessage.warning('您已登录，无需重复登录')
        next('/')
      } else {
        next()
      }
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { 
      title: '注册',
      requiresAuth: false,
      transition: 'slide-right'
    },
    // 已登录用户禁止访问注册页
    beforeEnter: (to, from, next) => {
      if (localStorage.getItem('token')) {
        ElMessage.warning('您已登录，无需注册')
        next('/')
      } else {
        next()
      }
    }
  },
  // 404路由
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 路由滚动行为
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 全局路由守卫 - 权限校验 + 页面标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
  }

  // 权限校验（示例：部分页面需要登录）
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router