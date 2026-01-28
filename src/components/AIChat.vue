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
      
      <div class="chat-list">
        <div 
          v-for="chat in chatHistory" 
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
      </div>
    </div>

    <!-- 聊天主区域 -->
    <div class="chat-main">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="chat-info">
          <h3>{{ currentChat?.title || '新对话' }}</h3>
          <el-button 
            v-if="currentChat" 
            type="text" 
            size="small" 
            @click="editChatTitle"
          >
            <el-icon><Edit /></el-icon>
            重命名
          </el-button>
        </div>
        <el-button type="primary" @click="toggleSidebar">
          <el-icon><Menu /></el-icon>
        </el-button>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
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
      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题..."
          @keydown.enter.prevent="sendMessage"
          :disabled="loading"
        />
        <div class="input-actions">
          <el-button 
            type="primary" 
            @click="sendMessage" 
            :loading="loading"
            :disabled="!inputMessage.trim()"
          >
            发送
          </el-button>
          <el-button @click="clearInput">清空</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Menu, Loading } from '@element-plus/icons-vue'

// 消息类型定义
interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

// 对话类型定义
interface Chat {
  id: string
  title: string
  messages: Message[]
  createdAt: number
  updatedAt: number
}

// 响应式数据
const sidebarWidth = ref(280)
const sidebarVisible = ref(true)
const inputMessage = ref('')
const loading = ref(false)
const renameDialogVisible = ref(false)
const newChatTitle = ref('')
const messageListRef = ref<HTMLElement>()

// 模拟用户头像和AI头像
const userAvatar = ref('')
const aiAvatar = ref('')

// 对话历史数据（模拟数据，实际应该从localStorage或后端获取）
const chatHistory = ref<Chat[]>([
  {
    id: '1',
    title: '关于太阳能专利的咨询',
    messages: [
      {
        id: '1-1',
        role: 'user',
        content: '请帮我分析一下太阳能光伏板的技术发展趋势',
        timestamp: Date.now() - 3600000
      },
      {
        id: '1-2',
        role: 'assistant',
        content: '太阳能光伏板技术近年来发展迅速，主要趋势包括：1）效率提升，单晶硅电池效率已超过25%；2）成本下降，规模化生产使成本大幅降低；3）新型材料，如钙钛矿电池等。',
        timestamp: Date.now() - 3500000
      }
    ],
    createdAt: Date.now() - 3600000,
    updatedAt: Date.now() - 3500000
  }
])

// 当前对话
const currentChat = ref<Chat | null>(null)

// 计算属性：按更新时间排序的对话历史
const sortedChatHistory = computed(() => {
  return [...chatHistory.value].sort((a, b) => b.updatedAt - a.updatedAt)
})

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  sidebarVisible.value = !sidebarVisible.value
  sidebarWidth.value = sidebarVisible.value ? 280 : 0
}

// 创建新对话
const createNewChat = () => {
  const newChat: Chat = {
    id: Date.now().toString(),
    title: '新对话',
    messages: [],
    createdAt: Date.now(),
    updatedAt: Date.now()
  }
  
  chatHistory.value.unshift(newChat)
  currentChat.value = newChat
  saveToLocalStorage()
}

// 选择对话
const selectChat = (chat: Chat) => {
  currentChat.value = chat
}

// 编辑对话标题
const editChatTitle = () => {
  if (!currentChat.value) return
  newChatTitle.value = currentChat.value.title
  renameDialogVisible.value = true
}

// 保存对话标题
const saveChatTitle = () => {
  if (!currentChat.value || !newChatTitle.value.trim()) return
  
  currentChat.value.title = newChatTitle.value.trim()
  currentChat.value.updatedAt = Date.now()
  renameDialogVisible.value = false
  saveToLocalStorage()
  
  ElMessage.success('对话标题已更新')
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
      
      saveToLocalStorage()
      ElMessage.success('对话已删除')
    }
  } catch (error) {
    // 用户取消删除
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return
  
  // 如果没有当前对话，创建新对话
  if (!currentChat.value) {
    createNewChat()
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
    // 模拟AI回复（实际应该调用AI API）
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: generateAIResponse(userInput),
      timestamp: Date.now()
    }
    
    currentChat.value!.messages.push(aiMessage)
    currentChat.value!.updatedAt = Date.now()
    
    // 如果是新对话且没有标题，自动生成标题
    if (currentChat.value!.title === '新对话' && currentChat.value!.messages.length === 2) {
      currentChat.value!.title = userInput.substring(0, 20) + (userInput.length > 20 ? '...' : '')
    }
    
    saveToLocalStorage()
    scrollToBottom()
  } catch (error) {
    console.error('AI回复失败:', error)
    ElMessage.error('AI回复失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 生成AI回复（模拟）
const generateAIResponse = (userInput: string): string => {
  // 这里应该调用真实的AI API
  // 暂时返回模拟回复
  const responses = [
    '关于您的问题，我了解到专利查询主要涉及技术领域分析、专利状态查询、法律保护等方面。',
    '根据您的查询，我可以为您提供专利技术分析、竞争对手分析、市场趋势预测等服务。',
    '专利查询需要综合考虑技术特征、法律状态、市场价值等多个维度。',
    '我可以帮您分析专利的技术创新点、保护范围、商业价值等信息。',
    '对于专利咨询，建议您提供更具体的技术领域或专利号，我可以给出更精准的分析。'
  ]
  
  return responses[Math.floor(Math.random() * responses.length)]
}

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
  loadFromLocalStorage()
  if (chatHistory.value.length === 0) {
    createNewChat()
  }
  scrollToBottom()
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

.chat-list {
  padding: 8px 0;
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

.input-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
`

// 添加样式到页面
const styleElement = document.createElement('style')
styleElement.textContent = styles
document.head.appendChild(styleElement)
</script>