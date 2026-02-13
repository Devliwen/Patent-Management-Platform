<template>
  <div class="home-container">
    <!-- 路由过渡动画 -->
    <transition :name="$route.meta.transition || 'fade'" mode="out-in">
      <!-- 骨架屏（加载中） -->
      <HomeSkeleton v-if="loading" />

      <!-- 首页内容（加载完成） -->
      <div v-else>
        <!-- 浮动AI咨询图标 -->
        <div 
          class="ai-float-button" 
          :class="{ active: showAIChat }"
          @click="toggleAIChat"
        >
          <el-icon class="ai-icon"><ChatDotRound /></el-icon>
          <span class="ai-text">AI咨询</span>
        </div>

        <!-- AI咨询面板 -->
        <div 
          v-if="showAIChat" 
          class="ai-chat-overlay"
          @click="closeAIChat"
        >
          <div 
            class="ai-chat-container" 
            @click.stop
            :style="{ width: aiChatWidth + 'px', right: '0' }"
          >
            <!-- AI聊天组件 -->
            <AIChat />
            
            <!-- 关闭按钮 -->
            <div class="close-button" @click="closeAIChat">
              <el-icon><Close /></el-icon>
            </div>
          </div>
        </div>

        <!-- 导航栏 -->
        <header class="home-header">
          <div class="container">
            <div class="header-content">
              <div class="logo">
                <el-icon class="logo-icon"><Cpu /></el-icon>
                <span class="logo-text">高校知识产权运营服务平台</span>
              </div>
              
              <nav class="nav-menu">
                <el-menu mode="horizontal" :default-active="activeNav" class="home-nav" @select="handleNavSelect">
                  <el-menu-item index="home">
                    <el-icon><House /></el-icon>
                    <span>首页</span>
                  </el-menu-item>
                  <el-menu-item index="patent">
                    <el-icon><Document /></el-icon>
                    <span>专利查询</span>
                  </el-menu-item>
                  <el-menu-item index="demand">
                    <el-icon><Message /></el-icon>
                    <span>需求发布</span>
                  </el-menu-item>
                  <el-menu-item index="evaluation">
                    <el-icon><DataAnalysis /></el-icon>
                    <span>价值评估</span>
                  </el-menu-item>
                  <el-menu-item index="patent/manage">
                    <el-icon><Document /></el-icon>
                    <span>我的专利</span>
                  </el-menu-item>
                  <el-menu-item index="requirement">
                    <el-icon><Message /></el-icon>
                    <span>需求管理</span>
                  </el-menu-item>
                  <el-menu-item index="expert">
                    <el-icon><UserFilled /></el-icon>
                    <span>专家对接</span>
                  </el-menu-item>
                  <el-menu-item index="expert">
                    <el-icon><User /></el-icon>
                    <span>专家管理</span>
                  </el-menu-item>
                  <el-menu-item index="transformation">
                    <el-icon><TrendCharts /></el-icon>
                    <span>转化成果</span>
                  </el-menu-item>
                </el-menu>
              </nav>
              
              <div class="header-actions">
                <el-button type="default" @click="goToLogin" v-if="!isLogin">登录</el-button>
                <el-button type="primary" @click="goToRegister" v-if="!isLogin">注册</el-button>
                <el-dropdown v-else>
                  <el-button type="primary">
                    {{ userInfo.nickname || userInfo.username }}
                    <el-icon><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="goToProfile">个人中心</el-dropdown-item>
                      <el-dropdown-item @click="logout" divided>退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </header>

        <!-- 主要内容 -->
        <main class="home-main">
          <!-- 横幅 -->
          <section class="home-banner">
            <div class="container">
              <div class="banner-content">
                <h2 class="banner-title">连接知识产权，推动创新发展</h2>
                <p class="banner-description">
                  高校知识产权运营服务平台，整合专利资源、专家资源，
                  实现知识产权成果的高效转化与价值最大化。
                </p>
                <div class="banner-actions">
                  <el-button type="default" size="large" @click="goToPatentSearch">
                    立即搜索专利
                  </el-button>
                  <el-button type="default" size="large" @click="goToRequirementPublish">
                    立即发布需求
                  </el-button>
                </div>
              </div>
            </div>
          </section>

          <!-- 核心功能 -->
          <section class="home-features">
            <div class="container">
              <h3 class="section-title">核心功能</h3>
              <div class="features-grid">
                <FeatureCard 
                  v-for="(item, index) in features" 
                  :key="index"
                  :icon="item.icon"
                  :title="item.title"
                  :description="item.description"
                  @click="handleFeatureClick(item.to)"
                />
              </div>
            </div>
          </section>

          <!-- 平台优势 -->
          <section class="home-advantages">
            <div class="container">
              <h3 class="section-title">平台优势</h3>
              <div class="advantages-content">
                <div class="advantage-item" v-for="(item, index) in advantages" :key="index">
                  <el-icon class="advantage-icon" size="48"><component :is="item.icon" /></el-icon>
                  <div class="advantage-info">
                    <h4 class="advantage-title">{{ item.title }}</h4>
                    <p class="advantage-description">{{ item.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- 成功案例 -->
          <section class="home-cases">
            <div class="container">
              <h3 class="section-title">成功案例</h3>
              <div class="cases-grid">
                <el-card class="case-card" hoverable v-for="(item, index) in cases" :key="index">
                  <template #header>
                    <div class="case-header">
                      <span class="case-title">{{ item.title }}</span>
                      <el-tag type="success">{{ item.status }}</el-tag>
                    </div>
                  </template>
                  <div class="case-content">
                    <p class="case-description">{{ item.description }}</p>
                    <div class="case-meta">
                      <span class="case-company">合作企业：{{ item.company }}</span>
                      <span class="case-time">转化时间：{{ item.time }}</span>
                    </div>
                  </div>
                </el-card>
              </div>
              <div class="case-more">
                <el-button type="link" size="large" @click="loadMoreCases">
                  查看更多案例 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </section>

          <!-- 行动召唤 -->
          <section class="home-cta">
            <div class="container">
              <div class="cta-content">
                <h3 class="cta-title">开启您的知识产权之旅</h3>
                <p class="cta-description">
                  注册成为平台用户，享受专利查询、专家对接、需求发布等全方位服务，
                  让您的知识产权成果创造更大价值。
                </p>
                <el-button v-if="!isLogin" type="primary" size="large" @click="goToRegister">立即注册</el-button>
                <el-button v-else type="primary" size="large" @click="goToProfile">个人中心</el-button>
              </div>
            </div>
          </section>
        </main>

        <!-- 页脚 -->
        <footer class="home-footer">
          <div class="container">
            <div class="footer-content">
              <div class="footer-section">
                <div class="logo">
                  <el-icon class="logo-icon"><Cpu /></el-icon>
                  <span class="logo-text">高校知识产权运营服务平台</span>
                </div>
                <p class="footer-description">
                  致力于推动高校知识产权成果转化，促进产学研一体化发展，
                  为创新驱动发展战略提供有力支撑。
                </p>
              </div>
              
              <div class="footer-section">
                <h4 class="footer-title">快速链接</h4>
                <ul class="footer-links">
                  <li><a href="#" @click.prevent="handleNavSelect('patent')">专利查询</a></li>
                  <li><a href="#" @click.prevent="handleNavSelect('transformation')">成果转化</a></li>
                  <li><a href="#" @click.prevent="handleNavSelect('demand')">需求发布</a></li>
                  <li><a href="#" @click.prevent="handleNavSelect('evaluation')">价值评估</a></li>
                  <li><a href="#" @click.prevent="loadMoreCases">成功案例</a></li>
                </ul>
              </div>
              
              <div class="footer-section">
                <h4 class="footer-title">关于我们</h4>
                <ul class="footer-links">
                  <li><a href="#">平台介绍</a></li>
                  <li><a href="#">服务条款</a></li>
                  <li><a href="#">隐私政策</a></li>
                  <li><a href="#">联系我们</a></li>
                  <li><a href="#">常见问题</a></li>
                </ul>
              </div>
              
              <div class="footer-section">
                <h4 class="footer-title">联系方式</h4>
                <div class="footer-contact">
                  <div class="contact-item">
                    <el-icon><Phone /></el-icon>
                    <span>400-123-4567</span>
                  </div>
                  <div class="contact-item">
                    <el-icon><Message /></el-icon>
                    <span>xxxxxx.edu.cn</span>
                  </div>
                  <div class="contact-item">
                    <el-icon><Location /></el-icon>
                    <span>天津市北辰区河北工业大学</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="footer-bottom">
              <p>&copy; 2026 高校知识产权运营服务平台. All Rights Reserved.</p>
            </div>
          </div>
        </footer>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Close } from '@element-plus/icons-vue'
import { authApi } from '../api'
import type { FeatureItem, AdvantageItem, CaseItem } from '../types'
import HomeSkeleton from '../components/HomeSkeleton.vue'
import FeatureCard from '../components/FeatureCard.vue'
import AIChat from '../components/AIChat.vue'
import {
  Cpu, House, Document, UserFilled, DataAnalysis, Star, Lightning, Lock, Service,
  User, EditPen, DocumentChecked, TrendCharts, Phone, Message, Location, 
  ArrowRight, ArrowDown, Money
} from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(true)
const isLogin = ref(!!localStorage.getItem('token'))
const userInfo = reactive({
  username: '',
  nickname: '',
  userId: 0
})

// AI咨询功能相关
const showAIChat = ref(false)
const aiChatWidth = ref(400)
const isResizing = ref(false)

// 更新登录状态的函数
function updateLoginStatus() {
  isLogin.value = !!localStorage.getItem('token')
}

// 监听 localStorage 变化，检测登录状态改变
window.addEventListener('storage', (e) => {
  if (e.key === 'token') {
    if (e.newValue) {
      // 如果变为登录状态，更新状态并获取用户信息
      updateLoginStatus()
      // 在 storage 事件中，避免因获取用户信息失败而影响其他标签页
      loadUserInfo().catch(error => {
        console.error('在 storage 事件中获取用户信息失败:', error)
        // 只有在明确是 401 错误时才更新登录状态
        if (error?.response?.data?.code === 401 || error?.message?.includes('登录状态失效')) {
          localStorage.removeItem('token')
          updateLoginStatus()
        }
      })
    } else {
      // 如果变为未登录状态，更新状态并清空用户信息
      updateLoginStatus()
      Object.assign(userInfo, { username: '', nickname: '', userId: 0 })
    }
  }
})

// 封装获取用户信息的函数
async function loadUserInfo() {
  try {
    const userData = await authApi.getCurrentUser()
    Object.assign(userInfo, userData)
    return userData // 返回用户数据以便调用方处理
  } catch (error: any) {
    console.error('获取用户信息失败:', error)
    // 只有在明确是 401 错误（token 无效）时才删除 token
    if (error?.response?.data?.code === 401 || error?.message?.includes('登录状态失效')) {
      localStorage.removeItem('token')
      updateLoginStatus() // 更新登录状态
    }
    throw error // 重新抛出错误，以便调用方可以处理
  }
}
// 当前激活的导航项
const activeNav = ref('home')

// 核心功能数据（可后续对接接口）
const features = ref<FeatureItem[]>([
  {
    icon: markRaw(DocumentChecked),
    title: '专利查询',
    description: '提供全面的专利数据库检索服务，支持多条件组合搜索，快速找到您需要的知识产权成果。',
    to: '/patent'
  },
  {
    icon: markRaw(User),
    title: '成果转化',
    description: '汇聚各领域专家资源，实现需求与专家的精准匹配，助力技术难题解决与项目合作。',
    to: '/transformation'
  },
  {
    icon: markRaw(EditPen),
    title: '需求发布',
    description: '便捷发布技术需求、合作需求，系统智能匹配相关专利与专家，提高对接效率。',
    to: '/demand'
  },
  {
    icon: markRaw(TrendCharts),
    title: '价值评估',
    description: '基于多维度指标的专利价值智能评估系统，为知识产权转化提供科学参考依据。',
    to: '/evaluation'
  }
])

// 平台优势数据
const advantages = ref<AdvantageItem[]>([
  {
    icon: markRaw(Star),
    title: '资源丰富',
    description: '整合全国高校专利数据库，汇聚各领域专家资源，提供全面的知识产权服务支持。'
  },
  {
    icon: markRaw(Lightning),
    title: '智能匹配',
    description: '基于人工智能技术的需求与资源精准匹配算法，提高对接效率与成功率。'
  },
  {
    icon: markRaw(Lock),
    title: '安全可靠',
    description: '严格的数据安全保护机制，保障用户信息与知识产权成果安全，提供可信的交易环境。'
  },
  {
    icon: markRaw(Service),
    title: '专业服务',
    description: '拥有专业的知识产权服务团队，提供从专利申请到转化的全流程服务，助力创新成果落地。'
  }
])

// 成功案例数据
const cases = ref<CaseItem[]>([
  {
    title: '新型材料专利转化',
    status: '已完成',
    description: '某高校研发的新型环保材料专利，通过平台匹配到相关企业，成功实现技术转让，转化金额达500万元。',
    company: 'XX科技有限公司',
    time: '2025-06-15'
  },
  {
    title: '人工智能算法合作',
    status: '已完成',
    description: '高校人工智能实验室的图像识别算法，与企业达成技术合作协议，共同开发智能安防系统。',
    company: 'XX智能科技',
    time: '2025-08-20'
  },
  {
    title: '生物医药技术授权',
    status: '已完成',
    description: '某医学院的新药研发技术，通过平台对接多家制药企业，最终实现技术授权，授权金额达1200万元。',
    company: 'XX制药集团',
    time: '2025-10-08'
  }
])

// AI咨询功能相关函数
const toggleAIChat = () => {
  showAIChat.value = !showAIChat.value
}

const closeAIChat = () => {
  showAIChat.value = false
}

// 开始调整大小
const startResize = (e: MouseEvent) => {
  isResizing.value = true
  document.body.classList.add('resizing')
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  e.preventDefault()
}

// 处理调整大小
const handleResize = (e: MouseEvent) => {
  if (!isResizing.value) return
  
  const containerWidth = window.innerWidth
  const minWidth = 100 // 最小宽度
  const maxWidth = 1200 // 最大宽度
  
  // 直接使用鼠标的X坐标作为左边界位置
  let newWidth = containerWidth - e.clientX
  
  // 限制宽度范围
  if (newWidth < minWidth) newWidth = minWidth
  if (newWidth > maxWidth) newWidth = maxWidth
  
  aiChatWidth.value = newWidth
}

// 停止调整大小
const stopResize = () => {
  isResizing.value = false
  document.body.classList.remove('resizing')
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
}

// 页面加载时获取用户信息
onMounted(async () => {
  try {
    // 检查是否有 token
    const token = localStorage.getItem('token');
    if (token && typeof token === 'string') {
      // 只有token是有效字符串时才尝试获取用户信息
      await loadUserInfo();
      // 如果获取用户信息失败，loadUserInfo 内部会处理错误和状态更新
    } else {
      // 没有 token 或 token 无效，确保登录状态为 false
      if (token !== null) {
        localStorage.removeItem('token');
      }
      updateLoginStatus();
    }
  } catch (error: any) {
    console.error('获取用户信息失败:', error)
    // 只有在明确是 401 错误（token 无效）时才更新登录状态
    if (error?.response?.data?.code === 401 || error?.message?.includes('登录状态失效')) {
      updateLoginStatus() // 更新登录状态
    }
  } finally {
    loading.value = false
  }
})

// 导航菜单选择
const handleNavSelect = (index: string) => {
  activeNav.value = index
  // 实际路由跳转逻辑
  switch (index) {
    case 'home':
      router.push('/')
      break
    case 'patent':
      console.log('patent')
      console.log(isLogin.value)
      if (isLogin.value) {
        router.push('/patent')
      } else {
        ElMessage.warning('请先登录后使用该功能')
        router.push('/login?redirect=/patent')
      }
      break
    case 'patent/manage':
      if (isLogin.value) {
        router.push('/patent/manage')
      } else {
        ElMessage.warning('请先登录后使用专利管理功能')
        router.push('/login?redirect=/patent/manage')
      }
      break
    case 'expert':
      if (isLogin.value) {
        router.push('/expert')
      } else {
        ElMessage.warning('请先登录后使用该功能')
        router.push('/login?redirect=/expert')
      }
      break
    case 'demand':
      if (isLogin.value) {
        router.push('/demand')
      } else {
        ElMessage.warning('请先登录后使用该功能')
        router.push('/login?redirect=/demand')
      }
      break
    case 'evaluation':
      if (isLogin.value) {
        router.push('/evaluation')
      } else {
        ElMessage.warning('请先登录后使用该功能')
        router.push('/login?redirect=/evaluation')
      }
      break
      case 'requirement':
        if (isLogin.value) {
          router.push('/requirement')
        } else {
          ElMessage.warning('请先登录后使用该功能')
          router.push('/login?redirect=/requirement')
        }
        break
      case 'transformation':
        if (isLogin.value) {
          router.push('/transformation')
        } else {
          ElMessage.warning('请先登录后使用该功能')
          router.push('/login?redirect=/transformation')
        }
        break
  }
}

// 功能卡片点击
const handleFeatureClick = (to: string) => {
  if (to && isLogin.value) {
    router.push(to)
  } else if (to && !isLogin.value) {
    ElMessage.warning('请先登录后使用该功能')
    router.push('/login?redirect=' + to)
  }
}

// 加载更多案例
const loadMoreCases = () => {
  ElMessage.info('更多案例正在加载中...')
  // 实际项目中对接接口加载更多
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}

// 跳转到专利查询
const goToPatentSearch = () => {
  if (isLogin.value) {
    router.push('/patent')
  } else {
    ElMessage.warning('请先登录后使用专利查询功能')
    router.push('/login?redirect=/patent')
  }
}

// 跳转到专家查询
const goToRequirementPublish = () => {
  if (isLogin.value) {
    router.push('/demand')
  } else {
    ElMessage.warning('请先登录后使用需求发布功能')
    router.push('/login?redirect=/demand')
  }
}

// 跳转到专家查询
const goToExpertSearch = () => {
  if (isLogin.value) {
    router.push('/expert')
  } else {
    ElMessage.warning('请先登录后使用专家对接功能')
    router.push('/login?redirect=/expert')
  }
}

// 跳转到个人中心
const goToProfile = () => {
  router.push('/profile')
}

// 退出登录
const logout = () => {
  localStorage.removeItem('token')
  updateLoginStatus() // 更新登录状态
  Object.assign(userInfo, { username: '', nickname: '', userId: 0 }) // 清空用户信息
  ElMessage.success('退出登录成功')
  router.push('/')
}
</script>

<style scoped>
/* AI咨询功能样式 */
.ai-float-button {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1000;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 50px;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.ai-float-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(64, 158, 255, 0.6);
}

.ai-float-button.active {
  background: linear-gradient(135deg, #67c23a 0%, #409eff 100%);
}

.ai-icon {
  font-size: 20px;
}

.ai-text {
  font-size: 14px;
}

.ai-chat-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
  animation: fadeIn 0.3s ease;
}

.ai-chat-container {
  height: 100%;
  background: white;
  position: absolute;
  box-shadow: -2px 0 20px rgba(0, 0, 0, 0.1);
  animation: slideInRight 0.3s ease;
  /* 确保右边界固定在页面最右边界 */
  right: 0;
  top: 0;
}

.resize-handle.left {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 8px;
  background: #e4e7ed;
  cursor: col-resize;
  z-index: 10;
  transition: background 0.3s;
}

.resize-handle.left:hover {
  background: #409eff;
}

.resize-handle.left:active {
  background: #337ecc;
}

.close-button {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 20;
  transition: all 0.3s;
}

.close-button:hover {
  background: #409eff;
  color: white;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideInRight {
  from { 
    transform: translateX(100%); 
    opacity: 0; 
  }
  to { 
    transform: translateX(0); 
    opacity: 1; 
  }
}

/* 原有样式保留，移除重复定义的部分 */
/* 路由过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.slide-right-enter-active, .slide-right-leave-active {
  transition: all 0.3s ease;
}
.slide-right-enter-from {
  transform: translateX(30px);
  opacity: 0;
}
.slide-right-leave-to {
  transform: translateX(-30px);
  opacity: 0;
}

/* 原有样式保留，移除重复定义的部分 */
.home-header {
  background-color: var(--bg-primary);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: 600;
  color: var(--primary-color);
}

.logo-icon {
  font-size: 28px;
  margin-right: 8px;
}

.logo-text {
  color: var(--text-primary);
}

.nav-menu {
  flex: 1;
  max-width: 600px;
  margin: 0 24px;
}

.home-nav {
  border-bottom: none;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 横幅样式 */
.home-banner {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: white;
  padding: 80px 0;
  text-align: center;
}

.banner-title {
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 24px;
}

.banner-description {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 40px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.banner-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* 核心功能样式 */
.home-features {
  padding: 60px 0;
}

.section-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 40px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

/* 平台优势样式 */
.home-advantages {
  background-color: var(--bg-secondary);
  padding: 60px 0;
}

.advantages-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.advantage-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 24px;
  background-color: var(--bg-primary);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-sm);
}

.advantage-icon {
  color: var(--primary-color);
  margin-top: 4px;
}

.advantage-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.advantage-description {
  color: var(--text-secondary);
  line-height: 1.6;
}

/* 成功案例样式 */
.home-cases {
  padding: 60px 0;
}

.cases-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.case-card {
  height: 100%;
}

.case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.case-title {
  font-weight: 600;
  color: var(--text-primary);
}

.case-description {
  margin-bottom: 16px;
  color: var(--text-regular);
  line-height: 1.6;
}

.case-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.case-more {
  text-align: center;
}

/* 行动召唤样式 */
.home-cta {
  background-color: var(--primary-color);
  color: white;
  padding: 60px 0;
  text-align: center;
}

.cta-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 16px;
}

.cta-description {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 32px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

/* 页脚样式 */
.home-footer {
  background-color: var(--text-primary);
  color: white;
  padding: 60px 0 30px;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 32px;
  margin-bottom: 32px;
}

.footer-section .logo {
  margin-bottom: 16px;
}

.footer-section .logo-text {
  color: white;
}

.footer-description {
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin-bottom: 16px;
}

.footer-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: white;
}

.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-links li {
  margin-bottom: 12px;
}

.footer-links a {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: color 0.3s;
}

.footer-links a:hover {
  color: var(--primary-light);
}

.footer-contact {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.footer-bottom {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-wrap: wrap;
    height: auto;
    padding: 12px 0;
  }
  
  .nav-menu {
    order: 3;
    width: 100%;
    margin: 12px 0 0;
  }
  
  .banner-title {
    font-size: 28px;
  }
  
  .banner-description {
    font-size: 16px;
  }
  
  .banner-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .advantages-content {
    gap: 16px;
  }
  
  .advantage-item {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .cases-grid {
    grid-template-columns: 1fr;
  }
}

/* 自定义 link 类型按钮样式，模拟原 text 样式 */
:deep(.el-button--link) {
  color: #666; /* 原 text 按钮文字颜色 */
  text-decoration: none; /* 去掉下划线 */
  background: transparent; /* 透明背景 */
}

:deep(.el-button--link:hover) {
  color: #409eff; /* hover 颜色和原 text 一致 */
  background: #f5f7fa;
}
</style>