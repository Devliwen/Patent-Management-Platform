<template>
  <div class="patent-search-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">专利查询</h2>
      <div class="header-actions">
        <el-button 
          type="primary" 
          @click="toggleAIChat" 
          :class="{ active: showAIChat }"
        >
          <el-icon><ChatDotRound /></el-icon>
          {{ showAIChat ? '关闭AI咨询' : '开启AI咨询' }}
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content" :class="{ 'with-chat': showAIChat }">
      <!-- 专利查询区域 -->
      <div 
        class="patent-section" 
        :style="{ width: showAIChat ? 'calc(100% - ' + aiChatWidth + 'px)' : '100%' }"
      >
        <!-- 专利搜索区域 -->
        <div class="search-section">
          <el-form :model="searchForm" inline>
            <el-form-item label="专利类别">
              <el-select 
                v-model="searchForm.category" 
                placeholder="请选择专利类别" 
                clearable 
                style="width: 200px"
              >
                <el-option label="风能" value="wind" />
                <el-option label="太阳能" value="solar" />
                <el-option label="生物质能" value="biomass" />
                <el-option label="氢能" value="hydrogen" />
                <el-option label="锂电池" value="lilon" />
              </el-select>
            </el-form-item>
            <el-form-item label="搜索关键词（专利号/标题/摘要/申请人/发明人）">
              <el-input 
                v-model="searchForm.query" 
                placeholder="请输入关键词"
                @keyup.enter="searchPatents"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchPatents">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 专利列表 -->
        <div class="patent-list">
          <el-table 
            :data="patentList" 
            v-loading="loading"
            style="width: 100%; table-layout: auto"  <!-- 关键修改：table-layout改为auto，支持自适应 -->
          >
            <!-- 公开号：移除固定width，增加min-width保证最小宽度 -->
            <el-table-column prop="publicNum" label="公开号" min-width="150" />
            <!-- 标题：移除固定width，增加min-width保证最小宽度 -->
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <!-- 其他列微调宽度，保证整体适配 -->
            <el-table-column prop="applicant" label="申请人" width="150" show-overflow-tooltip />
            <el-table-column prop="inventor" label="发明人" width="150" show-overflow-tooltip />
            <el-table-column prop="ipc" label="IPC" width="100" show-overflow-tooltip />
            <el-table-column prop="appliDate" label="申请日期" width="100" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="viewPatent(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            style="margin-top: 20px; text-align: center;"
          />
        </div>
      </div>

      <!-- AI咨询区域 - 核心修改：右侧边界固定在页面最右侧 -->
      <div 
        v-if="showAIChat" 
        class="ai-chat-section"
        :style="{ width: aiChatWidth + 'px', right: '0' }"
        @mousedown="(e) => startResize(e, 'border')"
      >
        <AIChat />
      </div>
    </div>

    <!-- 专利详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      :title="dialogTitle" 
      width="80%" 
      :before-close="closeDetailDialog"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="公开号">{{ currentPatent.publicNum }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentPatent.title }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentPatent.applicant }}</el-descriptions-item>
        <el-descriptions-item label="发明人">{{ currentPatent.inventor }}</el-descriptions-item>
        <el-descriptions-item label="IPC分类">{{ currentPatent.ipc }}</el-descriptions-item>
        <el-descriptions-item label="CPC分类" v-if="currentPatent.cpc">{{ currentPatent.cpc }}</el-descriptions-item>
        <el-descriptions-item label="申请号" v-if="currentPatent.appliNum">{{ currentPatent.appliNum }}</el-descriptions-item>
        <el-descriptions-item label="申请日期" v-if="currentPatent.appliDate">{{ currentPatent.appliDate }}</el-descriptions-item>
        <el-descriptions-item label="公布日期" v-if="currentPatent.publicDate">{{ currentPatent.publicDate }}</el-descriptions-item>
        <el-descriptions-item label="法律状态" v-if="currentPatent.legalStatus">{{ currentPatent.legalStatus }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2" v-if="currentPatent.abstractText">
          <div class="abstract-content">{{ currentPatent.abstractText }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="详情" :span="2" v-if="currentPatent.patentDetails">
          <div class="details-content">{{ currentPatent.patentDetails }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { patentApi } from '@/api'
import AIChat from '@/components/AIChat.vue'
import type { PatentBase, PatentCategory, PatentQueryParams } from '@/types'

// AI聊天功能相关
const showAIChat = ref(false)
const aiChatWidth = ref(400) // AI咨询区域宽度
const isResizing = ref(false)

// 搜索表单
const searchForm = reactive({
  category: <PatentCategory | ''>'',
  query: ''
})

// 专利列表相关
const patentList = ref<PatentBase[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情对话框相关
const detailDialogVisible = ref(false)
const dialogTitle = ref('')
const currentPatent = ref<PatentBase>({} as PatentBase)

// 切换AI聊天显示/隐藏
const toggleAIChat = () => {
  showAIChat.value = !showAIChat.value
}

// 开始调整大小 - 核心修改：判断点击位置是否在左侧边框区域
const startResize = (e: MouseEvent, type: string) => {
  // 只在AI面板左侧8px范围内触发拖动
  if (e.clientX - (e.target as HTMLElement).getBoundingClientRect().left > 8) {
    return
  }
  
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
  const minWidth = 200 // 最小宽度
  const maxWidth = 1200 // 最大宽度
  
  // 计算新的AI咨询面板宽度（从右侧边界到鼠标位置的距离）
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

// 搜索专利
const searchPatents = async () => {
  loading.value = true
  try {
    const params: PatentQueryParams = {
      category: searchForm.category,
      query: searchForm.query,
      page: currentPage.value,
      size: pageSize.value
    }
    
    const result = await patentApi.getPatents(params)
    
    // 调试信息：检查API返回的数据格式
    console.log('API返回数据:', result)
    
    // 后端返回的是分页对象，提取content数组
    if (result && typeof result === 'object' && 'content' in result) {
      patentList.value = result.content || []
      total.value = result.totalElements || 0
      
      console.log('提取的分页数据:', {
        contentLength: patentList.value.length,
        totalElements: total.value,
        pageable: result.pageable
      })
    } else {
      console.error('API返回的数据格式异常:', result)
      ElMessage.error('数据格式异常')
      patentList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('搜索专利失败:', error)
    ElMessage.error('搜索专利失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.category = ''
  searchForm.query = ''
  patentList.value = []
  total.value = 0
}

// 查看专利详情
const viewPatent = async (row: PatentBase) => {
  try {
    // 这里先直接使用row的数据，也可以从后端获取详细信息
    currentPatent.value = row
    dialogTitle.value = '专利详情'
    detailDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取专利详情失败')
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentPatent.value = {} as PatentBase
}

// 分页相关方法
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  searchPatents()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  searchPatents()
}

// 初始化数据
onMounted(() => {
  // 移除默认类别设置，让用户自行选择
  // searchPatents() // 不自动搜索，等待用户操作
})
</script>

<style scoped>
.patent-search-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.page-header {
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 20px;
}

.header-actions .el-button.active {
  background: #67c23a;
  border-color: #67c23a;
}

.main-content {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
}

.main-content.with-chat {
  flex-direction: row;
}

.patent-section {
  height: 100%;
  background: white;
  transition: width 0.3s ease;
  position: relative;
  overflow: hidden;
}

.search-section {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.patent-list {
  padding: 20px;
  height: calc(100% - 120px);
  overflow: auto;
  width: 100%;
  box-sizing: border-box;
}

/* 优化表格自适应样式 */
.patent-list .el-table {
  width: 100% !important;
  /* 移除固定最小宽度，让表格完全自适应 */
}

.patent-list .el-table__header-wrapper,
.patent-list .el-table__body-wrapper {
  width: 100%;
  overflow-x: auto;
}

/* AI咨询区域样式 - 核心修改：右侧边界固定在页面最右侧 */
.ai-chat-section {
  height: 100%;
  background: white;
  /* 左侧边框作为拖动线 */
  border-left: 8px solid #e4e7ed;
  overflow: hidden;
  transition: border-color 0.3s;
  position: absolute;
  right: 0;
  top: 0;
}

/* 只在边框区域显示拖动光标 */
.ai-chat-section::before {
  content: '';
  position: absolute;
  left: -8px; /* 覆盖边框区域 */
  top: 0;
  bottom: 0;
  width: 8px;
  cursor: col-resize;
  z-index: 1;
}

/* 悬停和拖动状态的边框样式 */
.ai-chat-section:hover {
  border-left-color: #409eff;
}

:global(.resizing) .ai-chat-section {
  border-left-color: #337ecc !important;
}

/* 确保AI聊天内容区域鼠标样式正常 */
.ai-chat-section >>> .ai-chat-container,
.ai-chat-section >>> .chat-content {
  cursor: default;
}

.abstract-content, .details-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

/* 全局样式，用于拖动时的光标 */
.resizing {
  cursor: col-resize !important;
  user-select: none !important;
}
</style>