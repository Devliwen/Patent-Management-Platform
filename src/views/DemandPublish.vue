<template>
  <div class="demand-publish-container">
    <div class="container">
      <el-card class="publish-card">
        <template #header>
          <div class="card-header">
            <span>发布技术需求</span>
          </div>
        </template>
        
        <el-form 
          :model="demandForm"
          :rules="demandRules"
          ref="demandFormRef"
          label-width="120px"
          class="demand-form"
        >
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="需求标题" prop="title">
                <el-input 
                  v-model="demandForm.title" 
                  placeholder="请输入需求标题"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="技术方向" prop="techDirection">
                <el-select 
                  v-model="demandForm.techDirection" 
                  placeholder="请选择技术方向" 
                  style="width: 100%"
                >
                  <el-option label="人工智能" value="人工智能" />
                  <el-option label="新能源" value="新能源" />
                  <el-option label="生物医药" value="生物医药" />
                  <el-option label="新材料" value="新材料" />
                  <el-option label="智能制造" value="智能制造" />
                  <el-option label="信息技术" value="信息技术" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="合作模式" prop="cooperationMode">
            <el-radio-group v-model="demandForm.cooperationMode">
              <el-radio label="技术转让">技术转让</el-radio>
              <el-radio label="合作开发">合作开发</el-radio>
              <el-radio label="技术服务">技术服务</el-radio>
              <el-radio label="技术咨询">技术咨询</el-radio>
              <el-radio label="其他">其他</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="关键词" prop="keywords">
            <el-input 
              v-model="demandForm.keywords" 
              type="textarea" 
              :rows="3" 
              placeholder="请输入关键词，多个关键词用逗号分隔，用于智能匹配专家和专利"
            />
          </el-form-item>
          
          <el-form-item label="需求详情" prop="description">
            <editor-content 
              :editor="editor" 
              class="editor-content border rounded p-3" 
              style="min-height: 200px; width: 100%; border: 1px solid #dcdfe6;" 
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitDemand" :loading="submitting">
              {{ submitting ? '发布中...' : '发布需求' }}
            </el-button>
            <el-button @click="resetForm">重置</el-button>
            <el-button @click="previewDemand">预览</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      
      <!-- 预览弹窗 -->
      <el-dialog v-model="previewVisible" title="需求预览" width="70%">
        <div class="preview-content">
          <h3>{{ demandForm.title }}</h3>
          <div class="preview-item">
            <label>技术方向:</label>
            <span>{{ demandForm.techDirection }}</span>
          </div>
          <div class="preview-item">
            <label>合作模式:</label>
            <span>{{ demandForm.cooperationMode }}</span>
          </div>
          <div class="preview-item">
            <label>关键词:</label>
            <span>{{ demandForm.keywords }}</span>
          </div>
          <div class="preview-item">
            <label>需求详情:</label>
            <div class="preview-detail" v-html="sanitizedDescription"></div>
          </div>
        </div>
        <template #footer>
          <el-button @click="previewVisible = false">关闭</el-button>
          <el-button type="primary" @click="confirmSubmit">确认发布</el-button>
        </template>
      </el-dialog>
    </div>

    <!-- 我的需求模块 -->
    <div class="container" style="margin-top: 30px;">
      <el-card class="my-demands-card">
        <template #header>
          <div class="card-header">
            <span>我的需求</span>
            <div class="header-actions">
              <el-button size="small" @click="refreshMyRequirements">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
              <el-button size="small" @click="toggleSortOrder">
                <el-icon><Sort /></el-icon>
                {{ sortOrder === 'asc' ? '升序' : '降序' }}
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="myRequirementsLoading" class="loading-container">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="myRequirements.length === 0" class="empty-container">
          <el-empty description="暂无已发布的需求" />
        </div>

        <div v-else class="my-demands-list">
          <el-table 
            :data="sortedRequirements" 
            style="width: 100%"
            :default-sort="{ prop: 'id', order: sortOrder === 'asc' ? 'ascending' : 'descending' }"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="techDirection" label="技术方向" width="120" show-overflow-tooltip />
            <el-table-column prop="cooperationMode" label="合作模式" width="120" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdDate" label="发布时间" width="150" sortable />
            <el-table-column label="操作" width="250">
              <template #default="{ row }">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <el-button size="small" type="primary" @click="viewMyRequirementDetail(row)">
                    查看详情
                  </el-button>
                  <el-button size="small" type="success" @click="startMatching(row)">
                    智能匹配
                  </el-button>
                  <el-button size="small" type="danger" @click="deleteMyRequirement(row)">
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>

    <!-- 我的需求详情对话框 -->
    <el-dialog 
      v-model="myRequirementDetailVisible" 
      :title="myRequirementDialogTitle" 
      width="60%" 
      :before-close="closeMyRequirementDetail"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ currentMyRequirement.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentMyRequirement.title }}</el-descriptions-item>
        <el-descriptions-item label="技术方向" v-if="currentMyRequirement.techDirection">{{ currentMyRequirement.techDirection }}</el-descriptions-item>
        <el-descriptions-item label="合作模式" v-if="currentMyRequirement.cooperationMode">{{ currentMyRequirement.cooperationMode }}</el-descriptions-item>
        <el-descriptions-item label="关键词" v-if="currentMyRequirement.keywords" :span="2">
          {{ currentMyRequirement.keywords }}
        </el-descriptions-item>
        <el-descriptions-item label="详情" v-if="currentMyRequirement.description" :span="2">
          <div v-html="sanitizedMyRequirementDescription"></div>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentMyRequirement.status)">
            {{ getStatusText(currentMyRequirement.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentMyRequirement.createdDate }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="closeMyRequirementDetail">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 智能匹配结果对话框 -->
    <el-dialog 
      v-model="matchingDialogVisible" 
      :title="`需求匹配结果 - ${currentMyRequirement.title}`" 
      width="80%" 
      :before-close="closeMatchingDialog"
    >
      <div class="match-content">
        <el-table :data="matchedPatents" v-loading="patentLoading" style="width: 100%">
          <el-table-column prop="category" label="专利类别" width="100" />
          <el-table-column prop="publicNum" label="公开号" width="150" />
          <el-table-column prop="title" label="标题" show-overflow-tooltip />
          <el-table-column prop="abstractText" label="摘要" show-overflow-tooltip />
        </el-table>
        
        <div v-if="matchedPatents.length === 0 && !patentLoading" class="empty-container" style="text-align: center; padding: 40px;">
          <el-empty description="未找到匹配的专利" />
        </div>
      </div>
      
      <template #footer>
        <el-button @click="closeMatchingDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { requirementApi, authApi } from '@/api'
import type { CreateRequirementParams, Requirement } from '@/types'
import DOMPurify from 'dompurify'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import { Refresh, Sort } from '@element-plus/icons-vue'

// 需求表单
const demandForm = reactive<CreateRequirementParams>({
  title: '',
  description: '',
  keywords: '',
  techDirection: '',
  cooperationMode: '技术转让'
})

const demandFormRef = ref()
const submitting = ref(false)
const previewVisible = ref(false)
const editor = ref<Editor>()
const currentUserInfo = ref<any>(null)

// 我的需求相关变量
const myRequirements = ref<Requirement[]>([])
const myRequirementsLoading = ref(false)
const sortOrder = ref<'asc' | 'desc'>('desc') // 默认按发布时间降序
const myRequirementDetailVisible = ref(false)
const myRequirementDialogTitle = ref('')
const currentMyRequirement = ref<Requirement>({} as Requirement)

// 智能匹配相关变量
const matchingDialogVisible = ref(false)
const matchedPatents = ref<any[]>([])
const patentLoading = ref(false)

// 安全渲染的计算属性
const sanitizedDescription = computed(() => {
  return DOMPurify.sanitize(demandForm.description || '')
})

// 我的需求详情安全渲染
const sanitizedMyRequirementDescription = computed(() => {
  return DOMPurify.sanitize(currentMyRequirement.value.description || '')
})

// 排序后的需求列表
const sortedRequirements = computed(() => {
  const requirements = [...myRequirements.value]
  return requirements.sort((a, b) => {
    return sortOrder.value === 'asc' ? a.id - b.id : b.id - a.id
  })
})

// 获取当前用户信息
const loadCurrentUserInfo = async () => {
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const userData = await authApi.getCurrentUser()
      currentUserInfo.value = userData
      console.log('当前用户信息:', userData)
      
      // 加载我的需求列表
      loadMyRequirements()
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 初始化编辑器和用户信息
onMounted(() => {
  editor.value = new Editor({
    content: demandForm.description || '',
    extensions: [
      StarterKit,
      Placeholder.configure({
        placeholder: '请详细描述您的技术需求...',
      }),
    ],
    onUpdate: ({ editor }) => {
      // 同步编辑器内容到表单数据
      demandForm.description = editor.getHTML()
    },
  })
  
  // 加载用户信息
  loadCurrentUserInfo()
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})

// 获取我的需求列表
const loadMyRequirements = async () => {
  if (!currentUserInfo.value) {
    return
  }
  
  myRequirementsLoading.value = true
  try {
    const response = await requirementApi.getRequirements({ 
      mine: true,
      page: 0,
      size: 100 
    })
    // 按照ID从小到大排列需求列表
    myRequirements.value = (response?.content || []).sort((a, b) => a.id - b.id)
  } catch (error: any) {
    console.error('获取我的需求失败:', error)
    ElMessage.error(error.message || '获取需求列表失败')
    myRequirements.value = []
  } finally {
    myRequirementsLoading.value = false
  }
}

// 刷新我的需求
const refreshMyRequirements = () => {
  loadMyRequirements()
}

// 切换排序顺序
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
}

// 查看我的需求详情
const viewMyRequirementDetail = (requirement: Requirement) => {
  currentMyRequirement.value = requirement
  myRequirementDialogTitle.value = `需求详情 - ${requirement.title}`
  myRequirementDetailVisible.value = true
}

// 关闭我的需求详情对话框
const closeMyRequirementDetail = () => {
  myRequirementDetailVisible.value = false
  currentMyRequirement.value = {} as Requirement
}

// 删除我的需求
const deleteMyRequirement = async (requirement: Requirement) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除需求"${requirement.title}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await requirementApi.deleteRequirement(requirement.id)
    ElMessage.success('删除成功')
    
    // 刷新需求列表
    loadMyRequirements()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除需求失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 开始智能匹配
const startMatching = async (requirement: Requirement) => {
  try {
    currentMyRequirement.value = requirement
    await loadMatchResults(requirement)
    matchingDialogVisible.value = true
  } catch (error: any) {
    console.error('开始匹配失败:', error)
    ElMessage.error(error.message || '开始匹配失败')
  }
}

// 加载匹配结果
const loadMatchResults = async (requirement: Requirement) => {
  patentLoading.value = true
  try {
    // 使用新的AI智能匹配接口
    const response = await requirementApi.aiMatchPatents({
      requirement: requirement.title + (requirement.description ? ' ' + requirement.description : ''),
      sessionId: `session_${requirement.id}`
    })
    
    // 调试：检查AI接口返回的数据结构
    console.log('=== AI接口返回数据详细分析 ===')
    console.log('完整的response对象:', response)
    console.log('response.patents类型:', typeof response.patents)
    console.log('response.patents长度:', response.patents?.length)
    
    if (response.patents && response.patents.length > 0) {
      const firstPatent = response.patents[0]
      console.log('第一个专利对象的完整结构:', firstPatent)
      console.log('第一个专利对象的字段列表:', Object.keys(firstPatent))
      console.log('publicNum字段值:', firstPatent.publicNum)
      console.log('abstractText字段值:', firstPatent.abstractText)
      console.log('category字段值:', firstPatent.category)
      console.log('title字段值:', firstPatent.title)
    }
    
    // 转换数据结构，适配前端显示（使用正确的字段名）
    matchedPatents.value = response.patents.map(patent => ({
      category: patent.category || '未知',
      publicNum: patent.public_num || '无公开号', // 注意：后端返回的是 public_num
      title: patent.title || '无标题',
      abstractText: patent.abstract || '无摘要信息' // 注意：后端返回的是 abstract
    }))
    
    console.log('转换后的专利列表:', matchedPatents.value)
    console.log('转换后第一个专利的字段:', matchedPatents.value[0])
  } catch (error: any) {
    console.error('获取匹配结果失败:', error)
    ElMessage.error(error.message || '获取匹配结果失败')
    matchedPatents.value = []
  } finally {
    patentLoading.value = false
  }
}

// 关闭匹配对话框
const closeMatchingDialog = () => {
  matchingDialogVisible.value = false
  matchedPatents.value = []
  currentMyRequirement.value = {} as Requirement
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return statusMap[status] || status
}

// 获取状态标签类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    'PENDING': 'info',
    'PROCESSING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return typeMap[status] || 'info'
}

// 表单验证规则 - 根据后端接口规范，只有title是必填字段
const demandRules = {
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度应在5-100个字符之间', trigger: 'blur' }
  ],
  techDirection: [
    { required: false, message: '请选择技术方向', trigger: 'change' }
  ],
  cooperationMode: [
    { required: false, message: '请选择合作模式', trigger: 'change' }
  ],
  keywords: [
    { required: false, message: '请输入关键词', trigger: 'blur' }
  ],
  description: [
    { required: false, message: '请输入需求详情', trigger: 'blur' }
  ]
}

// 预览表单
const previewForm = reactive<CreateRequirementParams>({...demandForm})

// 提交需求
const submitDemand = async () => {
  if (!demandFormRef.value) return
  
  try {
    await demandFormRef.value.validate()
    
    submitting.value = true
    
    // 构建请求数据，根据后端接口规范
    const requestData: CreateRequirementParams = {
      title: demandForm.title,
      description: demandForm.description || undefined,
      keywords: demandForm.keywords || undefined,
      techDirection: demandForm.techDirection || undefined,
      cooperationMode: demandForm.cooperationMode || undefined
    }
    
    // 调用后端接口发布需求
    await requirementApi.createRequirement(requestData)
    
    ElMessage.success('需求发布成功！')
    resetForm()
    
    // 刷新我的需求列表
    loadMyRequirements()
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      console.error('发布需求失败:', error)
      ElMessage.error(error.message || '发布需求失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

// 预览需求
const previewDemand = async () => {
  if (!demandFormRef.value) return
  
  try {
    await demandFormRef.value.validate()
    
    // 复制当前表单数据到预览表单
    Object.assign(previewForm, demandForm)
    previewVisible.value = true
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error('请完善必填信息')
    }
  }
}

// 确认提交
const confirmSubmit = () => {
  previewVisible.value = false
  submitDemand()
}

// 重置表单
const resetForm = () => {
  demandForm.title = ''
  demandForm.description = ''
  demandForm.keywords = ''
  demandForm.techDirection = ''
  demandForm.cooperationMode = '技术转让'
  
  if (demandFormRef.value) {
    demandFormRef.value.clearValidate()
  }
}



</script>

<style scoped>
.editor-content {
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  padding: 12px;
  min-height: 200px;
  background-color: var(--el-fill-color-blank);
}

.editor-content:focus {
  outline: none;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-3) inset;
}

.demand-publish-container {
  padding: 20px 0;
  background-color: var(--bg-secondary);
  min-height: calc(100vh - 64px);
}

.publish-card {
  max-width: 1200px;
  margin: 0 auto;
}

.my-demands-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.demand-form {
  max-width: 800px;
  margin: 0 auto;
}

.budget-text {
  margin-top: 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.editor-content {
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  padding: 12px;
  min-height: 200px;
  background-color: var(--el-fill-color-blank);
}

.editor-content:focus {
  outline: none;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-3) inset;
}

.preview-content {
  max-height: 500px;
  overflow-y: auto;
}

.loading-container {
  padding: 20px;
}

.empty-container {
  padding: 40px 0;
}

.my-demands-list {
  margin-top: 10px;
}

.preview-detail {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  padding: 10px;
  background-color: var(--el-fill-color-blank);
}

.preview-content h3 {
  margin-bottom: 20px;
  color: var(--text-primary);
}

.preview-item {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color-light);
}

.preview-item label {
  font-weight: 600;
  color: var(--text-secondary);
  margin-right: 10px;
}
</style>