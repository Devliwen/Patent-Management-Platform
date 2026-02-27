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
          <el-form :model="searchForm">
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
            <el-form-item label="一句话搜索">
              <el-input 
                v-model="searchForm.smartQuery" 
                placeholder="请输入需求语句进行搜索（可包含专利公开号、标题、申请人、发明人等关键词）"
                @keyup.enter="searchPatents"
                type="textarea"
                :rows="1"
                :autosize="{ minRows: 1, maxRows: 3 }"
                style="width: 60%;"
              />
            </el-form-item>
            <el-form-item style="width: 80%; text-align: center; margin-top: 20px;">
              <el-button type="primary" @click="searchPatents" style="width: 120px; height: 35px;">搜索</el-button>
              <el-button @click="resetSearch" style="width: 120px; height: 35px; margin-left: 20px;">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 专利列表 -->
        <div class="patent-list-container">
          <div class="patent-table-wrapper">
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
          </div>
          
          <!-- 分页 -->
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            style="margin-top: 20px; text-align: center; position: sticky; bottom: 0; background: white; padding: 10px 0; z-index: 10;"
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
import type { PatentBase, PatentCategory } from '@/types'

// AI聊天功能相关
const showAIChat = ref(false)
const aiChatWidth = ref(400) // AI咨询区域宽度
const isResizing = ref(false)

// 搜索表单
const searchForm = reactive({
  category: <PatentCategory | ''>'',
  smartQuery: ''
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
    // 使用ES搜索（智能匹配）
    const esParams = {
      query: searchForm.smartQuery.trim(),
      category: searchForm.category || undefined,
      page: currentPage.value - 1, // ES接口从0开始
      size: pageSize.value
    }
    
    console.log('=== ES搜索调试信息 ===')
    console.log('请求参数:', JSON.stringify(esParams, null, 2))
    console.log('解码的查询内容:', decodeURIComponent(esParams.query))
    
    const startTime = Date.now()
    const esResponse = await patentApi.searchPatentsByES(esParams)
    const endTime = Date.now()
    
    console.log('请求耗时:', endTime - startTime + 'ms')
    console.log('ES接口完整响应:', JSON.stringify(esResponse, null, 2))
    
    // ES接口返回的数据结构 - 支持两种格式
    if (esResponse && (esResponse as any).code === 0 && (esResponse as any).data) {
      // 格式1: {code: 0, message: "操作成功", data: {total: 71, hits: [...]}}
      patentList.value = (esResponse as any).data.hits
      total.value = (esResponse as any).data.total
      console.log('ES搜索成功（格式1），返回数据数量:', (esResponse as any).data.hits.length)
      
      if ((esResponse as any).data.total === 0) {
        console.log('🔍 搜索结果为0（合法空结果）')
        ElMessage.info('未找到相关专利')
      }
    } else if (esResponse && (esResponse as any).total !== undefined && (esResponse as any).hits !== undefined) {
      // 格式2: {total: 71, hits: [...]} - 直接返回数据
      patentList.value = (esResponse as any).hits
      total.value = (esResponse as any).total
      console.log('ES搜索成功（格式2），返回数据数量:', (esResponse as any).hits.length)
      
      if ((esResponse as any).total === 0) {
        console.log('🔍 搜索结果为0（合法空结果）')
        ElMessage.info('未找到相关专利')
      }
    } else if (esResponse && (esResponse as any).code !== undefined) {
      // 后端返回了错误码
      console.error('ES搜索返回错误:', esResponse)
      ElMessage.error(`搜索失败: ${(esResponse as any).message || '未知错误'}`)
      patentList.value = []
      total.value = 0
    } else {
      // 真正的格式异常
      console.error('ES搜索返回数据格式异常:', esResponse)
      ElMessage.error('接口响应格式异常')
      patentList.value = []
      total.value = 0
    }
  } catch (error: any) {
    console.error('搜索专利失败，详细错误信息:')
    console.error('错误对象:', error)
    console.error('响应状态:', error.response?.status)
    console.error('响应数据:', error.response?.data)
    ElMessage.error(error.message || '搜索专利失败')
    patentList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.category = ''
  searchForm.smartQuery = ''
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

.patent-list-container {
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
  position: relative;
  min-height: 400px;
}

.patent-table-wrapper {
  max-height: calc(100vh - 300px);
  overflow: auto;
  margin-bottom: 20px;
}

/* 优化表格自适应样式 */
.patent-table-wrapper .el-table {
  width: 100% !important;
  /* 移除固定最小宽度，让表格完全自适应 */
}

.patent-table-wrapper .el-table__header-wrapper,
.patent-table-wrapper .el-table__body-wrapper {
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