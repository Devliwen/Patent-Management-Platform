<template>
  <a-config-provider :locale="locale">
    <!-- 路由视图 + 全局样式 -->
    <router-view v-slot="{ Component, route }">
      <transition 
        :name="getTransitionName(route)" 
        mode="out-in"
        appear
      >
        <keep-alive :include="cachedViews">
          <component 
            :is="Component" 
            :key="route.path" 
            v-if="route.meta.keepAlive !== false"
          />
        </keep-alive>
      </transition>
    </router-view>

    <!-- 全局加载遮罩（可全局调用） -->
    <div class="global-loading" v-if="isLoading">
      <div class="loading-spinner"></div>
    </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, type RouteLocationNormalized } from 'vue-router'
import zhCN from '@arco-design/web-vue/es/locale/lang/zh-cn'

const locale = zhCN

interface AppRouteMeta {
  title?: string
  requiresAuth?: boolean
  transition?: string // 明确为字符串类型
  keepAlive?: boolean
}

// 2. 扩展Vue Router的类型（让route.meta自动提示RouteMeta的属性）
declare module 'vue-router' {
  interface RouteMeta extends AppRouteMeta {} // 不再递归，而是继承自定义接口
}


// 定义需要缓存的视图（可扩展）
const cachedViews = ref<string[]>(['Home'])
// 全局加载状态
const isLoading = ref<boolean>(false)
const route = useRoute()

/**
 * 获取过渡动画名称
 * @param currentRoute 当前路由实例（使用官方类型约束）
 * @returns 字符串类型的动画名称（100%保证返回string）
 */
const getTransitionName = (currentRoute: RouteLocationNormalized): string => {
  // 层层兜底，确保返回值一定是string
  if (!currentRoute?.meta) return 'fade'
  if (typeof currentRoute.meta.transition !== 'string') return 'fade'
  return currentRoute.meta.transition || 'fade'
}

// 监听路由变化
const handleRouteChange = (): void => {
  isLoading.value = true
  setTimeout(() => {
    isLoading.value = false
  }, 300)
}

onMounted(() => {
  window.addEventListener('route-change', handleRouteChange)
})

onUnmounted(() => {
  window.removeEventListener('route-change', handleRouteChange)
})

// 扩展：可添加全局方法/状态供子组件调用
defineExpose({
  // 手动控制加载状态
  setLoading: (status: boolean): void => {
    isLoading.value = status
  },
  // 添加需要缓存的页面
  addCachedView: (viewName: string): void => {
    if (!cachedViews.value.includes(viewName)) {
      cachedViews.value.push(viewName)
    }
  }
})
</script>

<style>
/* 主题颜色变量 */
:root {
  /* 主色调 */
  --primary-color: #1890ff;
  --primary-light: #40a9ff;
  --primary-dark: #096dd9;
  --primary-hover: #40a9ff;
  --primary-active: #096dd9;
  
  /* 辅助颜色 */
  --success-color: #52c41a;
  --warning-color: #faad14;
  --error-color: #f5222d;
  --info-color: #1890ff;
  
  /* 文本颜色 */
  --text-primary: #262626;
  --text-regular: #595959;
  --text-secondary: #8c8c8c;
  --text-placeholder: #bfbfbf;
  
  /* 背景颜色 */
  --bg-primary: #ffffff;
  --bg-secondary: #f5f5f5;
  --bg-tertiary: #f0f2f5;
  --bg-disabled: #f5f5f5;
  
  /* 边框颜色 */
  --border-color: #d9d9d9;
  --border-light: #e8e8e8;
  --border-dark: #8c8c8c;
  
  /* 阴影 */
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.09);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.15);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.2);
  
  /* 圆角 */
  --border-radius-sm: 2px;
  --border-radius-md: 4px;
  --border-radius-lg: 8px;
  
  /* 间距 */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  
  /* 字体大小 */
  --font-size-xs: 12px;
  --font-size-sm: 14px;
  --font-size-md: 16px;
  --font-size-lg: 18px;
  --font-size-xl: 24px;
  --font-size-xxl: 32px;

  /* 过渡动画 */
  --transition-fast: 0.2s;
  --transition-normal: 0.3s;
  --transition-slow: 0.5s;
}

/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
  background-color: var(--bg-tertiary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  height: 100%;
}

/* 链接样式 */
a {
  color: var(--primary-color);
  text-decoration: none;
  transition: color var(--transition-normal);
}

a:hover {
  color: var(--primary-hover);
}

a:active {
  color: var(--primary-active);
}

/* 表单元素基础样式 */
input, button, select, textarea {
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  outline: none;
}

/* 按钮基础样式 */
button {
  cursor: pointer;
  border: none;
  background: none;
  transition: all var(--transition-fast);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 容器样式 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--spacing-md);
}

/* 响应式容器适配 */
@media (max-width: 768px) {
  .container {
    padding: 0 var(--spacing-sm);
  }
}

/* 卡片样式 */
.card {
  background-color: var(--bg-primary);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-lg);
  transition: box-shadow var(--transition-normal);
}

.card:hover {
  box-shadow: var(--shadow-md);
}

/* 页面标题样式 */
.page-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
}

/* 辅助类 */
.text-center {
  text-align: center;
}

.text-left {
  text-align: left;
}

.text-right {
  text-align: right;
}

/* 间距辅助类（完整补充） */
.mt-1 { margin-top: var(--spacing-xs); }
.mt-2 { margin-top: var(--spacing-sm); }
.mt-3 { margin-top: var(--spacing-md); }
.mt-4 { margin-top: var(--spacing-lg); }
.mt-5 { margin-top: var(--spacing-xl); }

.mb-1 { margin-bottom: var(--spacing-xs); }
.mb-2 { margin-bottom: var(--spacing-sm); }
.mb-3 { margin-bottom: var(--spacing-md); }
.mb-4 { margin-bottom: var(--spacing-lg); }
.mb-5 { margin-bottom: var(--spacing-xl); }

.ml-1 { margin-left: var(--spacing-xs); }
.ml-2 { margin-left: var(--spacing-sm); }
.ml-3 { margin-left: var(--spacing-md); }
.ml-4 { margin-left: var(--spacing-lg); }
.ml-5 { margin-left: var(--spacing-xl); }

.mr-1 { margin-right: var(--spacing-xs); }
.mr-2 { margin-right: var(--spacing-sm); }
.mr-3 { margin-right: var(--spacing-md); }
.mr-4 { margin-right: var(--spacing-lg); }
.mr-5 { margin-right: var(--spacing-xl); }

.p-1 { padding: var(--spacing-xs); }
.p-2 { padding: var(--spacing-sm); }
.p-3 { padding: var(--spacing-md); }
.p-4 { padding: var(--spacing-lg); }
.p-5 { padding: var(--spacing-xl); }

/* 过渡动画样式 */
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal);
}

/* 全局加载遮罩样式 */
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-light);
  border-top: 4px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 全局滚动条样式优化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--bg-secondary);
  border-radius: var(--border-radius-sm);
}

::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: var(--border-radius-sm);
}

::-webkit-scrollbar-thumb:hover {
  background: var(--primary-light);
}
</style>