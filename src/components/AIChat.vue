<template>
  <div class="ai-chat-container">
    <!-- 对话记录侧边栏 -->
    <div class="chat-sidebar" :style="{ width: sidebarWidth + 'px' }">
      <div class="sidebar-header">
        <h3>对话记录</h3>
        <el-button type="primary" size="small" @click="createNewChat">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
      
      <div class="chat-list-container" @scroll="handleScroll">
        <div class="chat-list">
          <div 
            v-for="chat in sortedChatHistory" 
            :key="chat.id"
            class="chat-item"
            :class="{ active: currentChat?.id === chat.id }"
            @click="selectChat(chat)"
          >
            <div class="chat-title">{{ chat.title || '未命名对话' }}</div>
            <div class="chat-time">{{ formatTime(chat.updatedAt) }}</div>
            <div class="chat-actions">
              <el-button 
                type="danger" 
                size="small" 
                link 
                @click.stop="deleteChat(chat)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          
          <!-- 加载更多提示 -->
          <div v-if="loadingMore" class="load-more-loading">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <span>正在加载更多会话...</span>
          </div>
          
          <div v-else-if="hasMore" class="load-more-arrow" @click="loadMoreSessions">
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            <span>点击加载更多会话</span>
          </div>
          
          <div v-else-if="chatHistory.length > 0" class="no-more-tip">
            <span>没有更多会话了</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天主区域 -->
    <div class="chat-main">
      <!-- 未登录提示 -->
      <div v-if="!currentUserId" class="login-prompt">
        <div class="prompt-content">
          <el-icon class="prompt-icon"><Loading /></el-icon>
          <h3>请先登录</h3>
          <p>登录后即可使用AI咨询功能</p>
          <el-button type="primary" @click="goToLogin">前往登录</el-button>
        </div>
      </div>
      
      <!-- 聊天头部 -->
      <div v-else class="chat-header">
        <div class="chat-info">
          <h3>{{ currentChat?.title || '新对话' }}</h3>
          <el-button 
            v-if="currentChat" 
            type="link" 
            size="small" 
            @click="editChatTitle"
          >
            <el-icon><Edit /></el-icon>
            重命名
          </el-button>
        </div>
        
        <!-- 侧边栏切换按钮 -->
        <el-button 
          type="link" 
          size="small" 
          @click="toggleSidebar"
          class="sidebar-toggle" 
        >
          <el-icon><Menu /></el-icon>
        </el-button>
      </div>

      <!-- 消息列表 -->
      <div v-if="currentUserId" class="message-list" ref="messageListRef">
        <div 
          v-for="message in currentChat?.messages || []" 
          :key="message.id"
          class="message"
          :class="message.role"
        >
          <div class="message-avatar">
            <el-avatar 
              :size="32" 
              :src="message.role === 'user' ? userAvatar : aiAvatar"
            >
              {{ message.role === 'user' ? '用户' : 'AI' }}
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="message ai">
          <div class="message-avatar">
            <el-avatar :size="32" :src="aiAvatar">AI</el-avatar>
          </div>
          <div class="message-content">
            <div class="message-text">
              <el-icon class="loading-icon"><Loading /></el-icon>
              AI正在思考中...
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div v-if="currentUserId" class="input-area">
        <div class="input-container">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请输入您的问题..."
            @keydown.enter.prevent="sendMessage"
            :disabled="loading"
            class="message-input"
          />
          <div class="input-actions">
            <el-button 
              type="primary" 
              @click="sendMessage" 
              :loading="loading"
              :disabled="!inputMessage.trim()"
              class="send-button"
            >
              发送
            </el-button>
            <el-button @click="clearInput" class="clear-button">清空</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog 
      v-model="renameDialogVisible" 
      title="重命名对话" 
      width="400px"
    >
      <el-input 
        v-model="newChatTitle" 
        placeholder="请输入对话名称" 
      />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveChatTitle">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建新会话对话框 -->
    <el-dialog 
      v-model="createSessionDialogVisible" 
      title="创建新对话" 
      width="400px"
    >
      <el-input 
        v-model="newSessionTitle" 
        placeholder="请输入对话标题（可选）" 
        :maxlength="255"
        show-word-limit
      />
      <template #footer>
        <el-button @click="createSessionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateNewChat">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Menu, Loading, ArrowDown } from '@element-plus/icons-vue'
import { aiApi, authApi } from '@/api'
import router from '@/router'
import type { AiChatRequest, AiChatResponse, ChatSession, ChatMessage, UISession, UIMessage, ChatSessionResponse, ChatMessageResponse } from '@/types'

// 使用前端简化类型定义
interface Message extends UIMessage {}

interface Chat extends UISession {}

// 响应式数据
const sidebarWidth = ref(280)
const sidebarVisible = ref(true)
const inputMessage = ref('')
const loading = ref(false)
const renameDialogVisible = ref(false)
const newChatTitle = ref('')
const createSessionDialogVisible = ref(false)
const newSessionTitle = ref('')
const messageListRef = ref<HTMLElement>()

// 分页相关数据
const pageSize = ref(10) // 每页加载数量
const currentPage = ref(1) // 当前页码
const hasMore = ref(true) // 是否还有更多数据
const loadingMore = ref(false) // 是否正在加载更多

// 模拟用户头像和AI头像
const userAvatar = ref('')
const aiAvatar = ref('')

// 对话历史数据（从后端获取）
const chatHistory = ref<Chat[]>([])

// 当前对话
const currentChat = ref<Chat | null>(null)

// 当前用户ID（从localStorage获取）
const currentUserId = ref<number | null>(null)

// 初始化组件
const initComponent = async () => {
  // 检查是否已登录（检查token是否存在）
  const token = localStorage.getItem('token')
  console.log('AI咨询模块初始化 - token检查:', { 
    tokenExists: !!token, 
    tokenLength: token?.length,
    tokenPrefix: token ? token.substring(0, 20) + '...' : '无token'
  })
  
  if (!token) {
    console.log('用户未登录，无法使用AI咨询功能')
    return
  }
  
  // 首先尝试从localStorage获取用户信息（快速显示）
  const savedUserInfo = localStorage.getItem('userInfo')
  if (savedUserInfo) {
    try {
      const user = JSON.parse(savedUserInfo)
      if (user && user.id) {
        console.log('从localStorage获取用户信息:', user)
        currentUserId.value = user.id
        // 先显示界面，然后异步验证token
        // 初始化时自动加载最近的10条记录
        await loadUserSessions(false)
      }
    } catch (error) {
      console.error('解析localStorage用户信息失败:', error)
    }
  }
  
  // 异步验证token有效性并获取最新用户信息
  validateTokenAndGetUserInfo()
}

// 验证token有效性并获取用户信息
const validateTokenAndGetUserInfo = async () => {
  try {
    console.log('正在调用authApi.getCurrentUser()验证token...')
    const response = await authApi.getCurrentUser()
    console.log('获取用户信息响应:', response)
    
    // 根据后端实际返回的数据结构进行校验
    // 后端返回格式: {profile, expertProfile, primaryOrganization, user}
    const userInfo = response?.user || response
    const userId = userInfo?.id
    
    if (userId) {
      console.log('用户信息验证成功, 用户ID:', userId)
      currentUserId.value = userId
      // 更新用户信息到localStorage
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      
      // 重新加载用户的所有会话（确保数据最新）
      await loadUserSessions()
    } else {
      console.error('获取用户信息响应格式异常, 未找到用户ID:', response)
      // 不显示错误提示，保持当前状态
    }
  } catch (error: any) {
    console.error('获取用户信息失败:', error)
    
    // 处理认证失败的情况
    if (error.response?.status === 401) {
      console.log('token可能已过期，但由API拦截器统一处理')
      // 不立即删除token，让API拦截器统一处理token过期问题
      // 这样可以避免不同页面间的token状态不一致
      currentUserId.value = null
    } else {
      // 网络错误或其他错误，保持当前状态，不显示错误提示
      console.log('网络错误，保持当前登录状态')
    }
  }
}

// 加载用户的所有会话（支持分页）
const loadUserSessions = async (loadMore: boolean = false) => {
  if (!currentUserId.value) return
  
  if (loadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  
  try {
    const response = await aiApi.getSessions(currentPage.value, pageSize.value)
    console.log('获取会话列表响应:', response)
    
    // 简化校验逻辑，直接检查响应是否为数组或包含data数组
    let sessionsData: any[] = []
    
    if (Array.isArray(response)) {
      // 如果响应本身就是数组
      sessionsData = response
    } else if (response && Array.isArray(response.data)) {
      // 如果响应包含data数组
      sessionsData = response.data
    } else if (response && response.success && Array.isArray(response.data)) {
      // 如果响应包含success和data数组
      sessionsData = response.data
    }
    
    if (sessionsData.length > 0) {
      const sessions = sessionsData.map((session: any) => ({
        id: session.id?.toString() || Date.now().toString(),
        title: session.title || '未命名对话',
        messages: [], // 消息将在选择会话时加载
        createdAt: session.createdAt ? new Date(session.createdAt).getTime() : Date.now(),
        updatedAt: session.updatedAt ? new Date(session.updatedAt).getTime() : Date.now(),
        backendId: session.id
      }))
      
      console.log('转换后的会话列表:', sessions)
      
      if (loadMore) {
        // 加载更多，追加到现有列表
        chatHistory.value = [...chatHistory.value, ...sessions]
      } else {
        // 首次加载，替换整个列表
        chatHistory.value = sessions
      }
      
      // 判断是否还有更多数据
      hasMore.value = sessions.length === pageSize.value
      
      if (loadMore) {
        currentPage.value++
      }
    } else {
      console.error('加载会话失败，响应格式异常:', response)
      ElMessage.error('加载会话失败')
    }
  } catch (error) {
    console.error('加载会话失败:', error)
    ElMessage.error('加载会话失败')
  } finally {
    if (loadMore) {
      loadingMore.value = false
    } else {
      loading.value = false
    }
  }
}

// 计算属性：按更新时间排序的对话历史
const sortedChatHistory = computed(() => {
  return [...chatHistory.value].sort((a, b) => b.updatedAt - a.updatedAt)
})

// 处理滚动事件，实现下拉加载更多
const handleScroll = (event: Event) => {
  const container = event.target as HTMLElement
  const scrollTop = container.scrollTop
  const scrollHeight = container.scrollHeight
  const clientHeight = container.clientHeight
  
  // 当滚动到距离底部50px时触发加载更多
  if (scrollHeight - scrollTop - clientHeight < 50 && 
      hasMore.value && 
      !loadingMore.value && 
      !loading.value) {
    loadMoreSessions()
  }
}

// 加载更多会话
const loadMoreSessions = () => {
  if (hasMore.value && !loadingMore.value) {
    loadUserSessions(true)
  }
}

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  sidebarVisible.value = !sidebarVisible.value
  sidebarWidth.value = sidebarVisible.value ? 280 : 0
}

// 创建新对话 - 显示对话框让用户输入标题
const createNewChat = () => {
  if (!currentUserId.value) {
    ElMessage.error('请先登录后再创建对话')
    return
  }
  
  // 显示创建会话对话框
  createSessionDialogVisible.value = true
  newSessionTitle.value = ''
}

// 确认创建新对话
const confirmCreateNewChat = async () => {
  if (!currentUserId.value) return
  
  try {
    console.log('正在调用aiApi.createSession()...')
    const response = await aiApi.createSession(newSessionTitle.value.trim() || undefined)
    console.log('创建会话响应:', response)
    
    const newChat: Chat = {
      id: response.id.toString(),
      title: response.title || '新对话',
      messages: [],
      createdAt: new Date(response.createdAt).getTime(),
      updatedAt: new Date(response.updatedAt).getTime(),
      backendId: response.id
    }
    
    console.log('创建的新会话:', newChat)
    chatHistory.value.unshift(newChat)
    currentChat.value = newChat
    createSessionDialogVisible.value = false
    
    ElMessage.success('新对话已创建')
  } catch (error: any) {
    console.error('创建会话失败:', error)
    
    // 处理认证失败的情况
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.response?.status === 500) {
      ElMessage.error('服务器内部错误，请稍后重试')
    } else {
      ElMessage.error('创建会话失败，请检查网络连接')
    }
  }
}

// 选择对话
const selectChat = async (chat: Chat) => {
  currentChat.value = chat
  
  // 如果会话有后端ID，加载该会话的消息
  if (chat.backendId) {
    await loadSessionMessages(chat.backendId)
  }
}

// 加载会话消息
const loadSessionMessages = async (sessionId: number) => {
  try {
    const response = await aiApi.getMessages(sessionId)
    
    // 后端直接返回List<ChatMessageResponse>数组
    if (currentChat.value && Array.isArray(response)) {
      currentChat.value.messages = response.map((message: ChatMessageResponse) => ({
        id: message.id.toString(),
        role: message.role as 'user' | 'assistant' | 'system',
        content: message.content,
        timestamp: new Date(message.createdAt).getTime(),
        backendId: message.id
      }))
      
      console.log('成功加载消息数量:', currentChat.value.messages.length)
    } else {
      console.warn('消息数据格式异常:', response)
      ElMessage.warning('消息数据格式异常')
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
  }
}

// 编辑对话标题
const editChatTitle = () => {
  if (!currentChat.value) return
  newChatTitle.value = currentChat.value.title
  renameDialogVisible.value = true
}

// 保存对话标题
const saveChatTitle = async () => {
  if (!currentChat.value || !newChatTitle.value.trim()) return
  
  const newTitle = newChatTitle.value.trim()
  
  try {
      // 如果会话有后端ID，更新后端数据库
      if (currentChat.value.backendId) {
        const response = await aiApi.updateSessionTitle(currentChat.value.backendId, newTitle)
        
        // 检查后端响应是否成功（多种格式兼容）
        console.log('后端响应:', response)
        
        // 判断响应是否成功的多种方式（包括直接返回数据的情况）
        const isSuccess = (response.data !== null)
        
        if (isSuccess) {
          console.log('会话重命名成功:', response.data || response)
          
          // 提取数据（多种格式兼容）
          const responseData = response.data || response
          
          // 使用后端返回的更新后数据
          if (responseData && responseData.title) {
            currentChat.value.title = responseData.title
            currentChat.value.updatedAt = responseData.updatedAt 
              ? new Date(responseData.updatedAt).getTime()
              : Date.now()
          } else {
            // 如果后端没有返回有效数据，使用前端数据
            currentChat.value.title = newTitle
            currentChat.value.updatedAt = Date.now()
            console.warn('后端返回成功但数据格式异常，使用前端标题')
          }
        } else {
          // 提取错误信息（多种格式兼容）
          const errorMessage = response?.message
          
          console.error('重命名失败:', errorMessage)
          ElMessage.error(`重命名失败: ${errorMessage}`)
          return
        }
      } else {
        // 如果没有后端ID，只更新前端数据
        currentChat.value.title = newTitle
        currentChat.value.updatedAt = Date.now()
      }
      
      renameDialogVisible.value = false
      ElMessage.success('对话标题已更新')
  } catch (error: any) {
    console.error('更新标题失败:', error)
    
    // 提取具体的错误信息
    let errorMessage = '更新标题失败'
    
    if (error.response?.data?.message) {
      // 后端返回的具体错误消息
      errorMessage = `重命名失败: ${error.response.data.message}`
    } else if (error.response?.status === 500) {
      // 服务器内部错误
      errorMessage = '服务器内部错误，请稍后重试'
    } else if (error.message) {
      // Axios错误消息
      errorMessage = `重命名失败: ${error.message}`
    } else if (typeof error === 'string') {
      // 字符串错误
      errorMessage = `重命名失败: ${error}`
    }
    
    ElMessage.error(errorMessage)
  }
}

// 删除对话
const deleteChat = async (chat: Chat) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除对话"${chat.title}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 如果会话有后端ID，删除后端数据库中的会话
    if (chat.backendId) {
      await aiApi.deleteSession(chat.backendId)
    }
    
    const index = chatHistory.value.findIndex(c => c.id === chat.id)
    if (index > -1) {
      chatHistory.value.splice(index, 1)
      
      // 如果删除的是当前对话，切换到第一个对话或创建新对话
      if (currentChat.value?.id === chat.id) {
        currentChat.value = chatHistory.value[0] || null
        if (!currentChat.value) {
          createNewChat()
        }
      }
      
      ElMessage.success('对话已删除')
    }
  } catch (error) {
    // 用户取消删除
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return
  
  // 检查用户是否已登录
  if (!currentUserId.value) {
    ElMessage.error('请先登录后再使用AI咨询功能')
    return
  }
  
  // 如果没有当前对话，创建新对话
  if (!currentChat.value) {
    await createNewChat()
  }
  
  const userMessage: Message = {
    id: Date.now().toString(),
    role: 'user',
    content: inputMessage.value.trim(),
    timestamp: Date.now()
  }
  
  // 添加用户消息
  currentChat.value!.messages.push(userMessage)
  currentChat.value!.updatedAt = Date.now()
  
  const userInput = inputMessage.value.trim()
  inputMessage.value = ''
  loading.value = true
  
  // 滚动到底部
  scrollToBottom()
  
  try {
    // 构建AI聊天请求数据
    const requestData: AiChatRequest = {
      question: userInput,
      sessionId: currentChat.value!.backendId
    }
    
    // 调用后端AI聊天接口
    const response = await aiApi.chat(requestData)
    
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: response.answer,
      timestamp: Date.now(),
      backendId: response.messageId
    }
    
    currentChat.value!.messages.push(aiMessage)
    currentChat.value!.updatedAt = Date.now()
    
    // 如果会话标题为空或为默认标题，根据后端逻辑自动生成标题
    if (!currentChat.value!.title || currentChat.value!.title === '新对话' || currentChat.value!.title.trim() === '') {
      // 使用后端相同的标题生成逻辑
      const buildTitle = (question: string): string => {
        const s = question ? question.trim() : ''
        if (s.length <= 20) return s
        return s.substring(0, 20)
      }
      
      const generatedTitle = buildTitle(userInput)
      if (generatedTitle) {
        // 更新后端会话标题（使用独立的try-catch，避免影响主要流程）
        if (currentChat.value!.backendId) {
          try {
            const response = await aiApi.updateSessionTitle(currentChat.value!.backendId, generatedTitle)
            
            // 检查后端响应是否成功（多种格式兼容）
            console.log('自动更新标题后端响应:', response)            
            // 判断响应是否成功的多种方式（包括直接返回数据的情况）
            const isSuccess = (response.code === 0)
            
            if (isSuccess) {
              // 提取数据（多种格式兼容）
              const responseData = response.data || response
              
              if (responseData && responseData.title) {
                // 使用后端返回的更新后数据
                currentChat.value!.title = responseData.title
                currentChat.value!.updatedAt = responseData.updatedAt 
                  ? new Date(responseData.updatedAt).getTime()
                  : Date.now()
              } else {
                // 如果后端没有返回有效数据，使用前端生成的标题
                currentChat.value!.title = generatedTitle
                currentChat.value!.updatedAt = Date.now()
                console.warn('自动更新标题：后端返回成功但数据格式异常，使用前端标题')
              }
            } else {
              // 如果后端更新失败，使用前端生成的标题
              currentChat.value!.title = generatedTitle
              currentChat.value!.updatedAt = Date.now()
              
              // 提取错误信息（多种格式兼容）
              const errorMessage = response?.message || '未知错误'
              console.warn('自动更新标题失败，使用前端标题:', errorMessage)
            }
          } catch (updateError: any) {
            // 自动更新标题失败，但不影响主要流程
            console.warn('自动更新标题异常:', updateError)
            currentChat.value!.title = generatedTitle
            currentChat.value!.updatedAt = Date.now()
          }
        } else {
          // 如果没有后端ID，只更新前端数据
          currentChat.value!.title = generatedTitle
          currentChat.value!.updatedAt = Date.now()
        }
      }
    }
    
    scrollToBottom()
  } catch (error: any) {
    console.error('AI回复失败:', error)
    
    // 处理认证失败的情况
    if (error.response?.status === 401) {
      ElMessage.error('请先登录后再使用AI咨询功能')
    } else if (error.response?.status === 403) {
      ElMessage.error('没有权限使用AI咨询功能')
    } else {
      ElMessage.error('AI回复失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

// 不再需要模拟AI回复函数，已替换为真实API调用

// 清空输入
const clearInput = () => {
  inputMessage.value = ''
}

// 滚动到消息列表底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 格式化时间
const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - timestamp
  
  if (diff < 60000) { // 1分钟内
    return '刚刚'
  } else if (diff < 3600000) { // 1小时内
    return Math.floor(diff / 60000) + '分钟前'
  } else if (diff < 86400000) { // 1天内
    return Math.floor(diff / 3600000) + '小时前'
  } else {
    return date.toLocaleDateString()
  }
}

// 保存到本地存储
const saveToLocalStorage = () => {
  localStorage.setItem('aiChatHistory', JSON.stringify(chatHistory.value))
}

// 从本地存储加载
const loadFromLocalStorage = () => {
  const saved = localStorage.getItem('aiChatHistory')
  if (saved) {
    try {
      chatHistory.value = JSON.parse(saved)
      if (chatHistory.value.length > 0) {
        currentChat.value = chatHistory.value[0]
      }
    } catch (error) {
      console.error('加载对话历史失败:', error)
    }
  }
}

// 页面加载时初始化
onMounted(() => {
  initComponent()
})

// 样式定义
const styles = `
.ai-chat-container {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

.chat-sidebar {
  width: 280px;
  background: white;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s ease;
  overflow: hidden;
}

.chat-sidebar.collapsed {
  width: 0;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.chat-list-container {
  height: calc(100vh - 120px);
  overflow-y: auto;
}

.chat-list {
  padding: 8px 0;
}

/* 加载更多相关样式 */
.load-more-loading,
.load-more-arrow,
.no-more-tip {
  padding: 16px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.load-more-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.load-more-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.load-more-arrow:hover {
  color: #409eff;
  background: #f5f7fa;
}

.arrow-icon {
  transition: transform 0.3s;
}

.load-more-arrow:hover .arrow-icon {
  transform: translateY(2px);
}

.loading-icon {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.chat-item {
  padding: 12px 16px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all 0.3s;
}

.chat-item:hover {
  background: #f5f7fa;
}

.chat-item.active {
  background: #ecf5ff;
  border-left-color: #409eff;
}

.chat-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-time {
  font-size: 12px;
  color: #909399;
}

.chat-actions {
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.chat-item:hover .chat-actions {
  opacity: 1;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  /* 确保聊天框宽度正确响应容器宽度变化 */
  width: 100%;
  min-width: 0; /* 防止flex项目溢出 */
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-info h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.message {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.5;
  word-wrap: break-word;
}

.message.user .message-text {
  background: #409eff;
  color: white;
  border-bottom-right-radius: 2px;
}

.message.assistant .message-text {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 2px;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-area {
  padding: 20px;
  border-top: 1px solid #e4e7ed;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
}

.input-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 4px;
}

.send-button {
  min-width: 80px;
}

.clear-button {
  min-width: 80px;
}
`

// 添加样式到页面
const styleElement = document.createElement('style')
styleElement.textContent = styles
document.head.appendChild(styleElement)
</script>