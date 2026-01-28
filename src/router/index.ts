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
    beforeEnter: (_to, _from, next) => {
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
    beforeEnter: (_to, _from, next) => {
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
  },
  {
    path: '/demand',
    name: 'Demand',
    component: () => import('../views/DemandPublish.vue'), // 懒加载需求发布页面
    meta: { 
      title: '需求发布',
      requiresAuth: true, // 需求发布需要登录
      transition: 'fade'
    }
  },
  {
    path: '/evaluation',
    name: 'Evaluation',
    component: () => import('../views/Evaluation.vue'), // 懒加载价值评估页面
    meta: { 
      title: '价值评估',
      requiresAuth: true, // 价值评估需要登录
      transition: 'fade'
    }
  },
  {
    path: '/patent',
    name: 'PatentSearch',
    component: () => import('../views/PatentSearch.vue'), // 懒加载专利查询页面
    meta: { 
      title: '专利查询',
      requiresAuth: false, // 专利查询无需登录
      transition: 'fade'
    }
  },
  {
    path: '/patent/manage',
    name: 'PatentManage',
    component: () => import('../views/PatentManage.vue'), // 懒加载专利管理页面
    meta: { 
      title: '专利管理',
      requiresAuth: true, // 专利管理需要登录
      transition: 'fade'
    }
  },
  {
    path: '/expert',
    name: 'Expert',
    component: () => import('../views/ExpertManage.vue'), // 懒加载专家管理页面
    meta: { 
      title: '专家管理',
      requiresAuth: true, // 假设专家管理需要登录
      transition: 'fade'
    }
  },
  {
    path: '/requirement',
    name: 'Requirement',
    component: () => import('../views/RequirementManage.vue'), // 懒加载需求管理页面
    meta: { 
      title: '需求管理',
      requiresAuth: true, // 假设需求管理需要登录
      transition: 'fade'
    }
  },
  {
    path: '/transformation',
    name: 'Transformation',
    component: () => import('../views/TransformationManage.vue'), // 懒加载转化成果管理页面
    meta: { 
      title: '转化成果管理',
      requiresAuth: true, // 假设转化成果管理需要登录
      transition: 'fade'
    }
  },
  {
    path: '/valuation',
    name: 'Valuation',
    component: () => import('../views/ValuationManage.vue'), // 懒加载价值评估管理页面
    meta: { 
      title: '价值评估管理',
      requiresAuth: true, // 假设价值评估管理需要登录
      transition: 'fade'
    }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'), // 懒加载个人中心页面
    meta: { 
      title: '个人中心',
      requiresAuth: true, // 个人中心需要登录
      transition: 'fade'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 路由滚动行为
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 全局路由守卫 - 权限校验 + 页面标题
router.beforeEach(async (to, _from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    const appTitle = import.meta.env.VITE_APP_TITLE || '高校知识产权运营服务平台'
    document.title = `${to.meta.title} - ${appTitle}`
  }

  const token = localStorage.getItem('token')
  
  // 已登录用户访问登录/注册页，跳转到首页
  if ((to.path === '/login' || to.path === '/register') && token) {
    ElMessage.warning('您已登录，无需重复操作')
    next('/')
    return
  }

  // 需要登录的页面但未登录
  if (to.meta.requiresAuth && !token) {
    ElMessage.warning('请先登录')
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 需要登录的页面且已登录，验证token有效性
  if (to.meta.requiresAuth && token) {
    try {
      // 验证token格式和基本有效性
      const isValidToken = validateTokenFormat(token)
      if (!isValidToken) {
        localStorage.removeItem('token')
        ElMessage.warning('登录状态已失效，请重新登录')
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
      }
      
      // 可以在这里添加后端token验证（可选）
      // await validateTokenWithBackend(token)
      
    } catch (error) {
      localStorage.removeItem('token')
      ElMessage.warning('登录状态验证失败，请重新登录')
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})

// 验证token格式
function validateTokenFormat(token: string): boolean {
  if (!token || typeof token !== 'string') {
    return false
  }
  
  // 检查是否为JWT格式（三段式）
  const parts = token.split('.')
  if (parts.length !== 3) {
    return false
  }
  
  // 检查token长度（基本验证）
  if (token.length < 10) {
    return false
  }
  
  return true
}

export default router