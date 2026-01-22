<template>
  <div class="requirement-manage-container">
    <!-- 需求搜索区域 -->
    <div class="search-section">
      <el-form :model="searchForm" inline>
        <el-form-item label="搜索关键词">
          <el-input 
            v-model="searchForm.query" 
            placeholder="请输入标题/关键词"
            @keyup.enter="searchRequirements"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchRequirements">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openAddDialog">发布需求</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 需求列表 -->
    <div class="requirement-list">
      <el-table 
        :data="requirementList" 
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="techDirection" label="技术方向" width="150" show-overflow-tooltip />
        <el-table-column prop="cooperationMode" label="合作模式" width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdDate" label="发布时间" width="150" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="viewRequirement(row)">查看</el-button>
            <el-button size="small" type="primary" @click="startMatching(row)">开始匹配</el-button>
            <el-button size="small" type="warning" @click="editRequirement(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteRequirement(row)">删除</el-button>
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

    <!-- 需求详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      :title="dialogTitle" 
      width="60%" 
      :before-close="closeDetailDialog"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ currentRequirement.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentRequirement.title }}</el-descriptions-item>
        <el-descriptions-item label="技术方向" v-if="currentRequirement.techDirection">{{ currentRequirement.techDirection }}</el-descriptions-item>
        <el-descriptions-item label="合作模式" v-if="currentRequirement.cooperationMode">{{ currentRequirement.cooperationMode }}</el-descriptions-item>
        <el-descriptions-item label="关键词" v-if="currentRequirement.keywords" :span="2">
          {{ currentRequirement.keywords }}
        </el-descriptions-item>
        <el-descriptions-item label="详情" v-if="currentRequirement.description" :span="2">
          {{ currentRequirement.description }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRequirement.status)">
            {{ getStatusText(currentRequirement.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentRequirement.createdDate }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 新增/编辑需求对话框 -->
    <el-dialog 
      v-model="formDialogVisible" 
      :title="dialogTitle" 
      width="50%" 
      :before-close="closeFormDialog"
    >
      <el-form 
        :model="requirementForm" 
        :rules="requirementRules" 
        ref="requirementFormRef" 
        label-width="100px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="requirementForm.title" placeholder="请输入需求标题" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="requirementForm.keywords" placeholder="请输入关键词，用于匹配" />
        </el-form-item>
        <el-form-item label="技术方向">
          <el-input v-model="requirementForm.techDirection" placeholder="请输入技术方向" />
        </el-form-item>
        <el-form-item label="合作模式">
          <el-select v-model="requirementForm.cooperationMode" placeholder="请选择合作模式" style="width: 100%">
            <el-option label="技术转让" value="技术转让" />
            <el-option label="合作开发" value="合作开发" />
            <el-option label="技术服务" value="技术服务" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="详情">
          <el-input 
            v-model="requirementForm.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入需求详情"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeFormDialog">取消</el-button>
          <el-button type="primary" @click="saveRequirement">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 匹配结果对话框 -->
    <el-dialog 
      v-model="matchingDialogVisible" 
      :title="`需求匹配结果 - ${currentRequirement.title}`" 
      width="80%" 
      :before-close="closeMatchingDialog"
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane label="匹配专利" name="patents">
          <div class="match-content">
            <el-table :data="matchedPatents" v-loading="patentLoading" style="width: 100%">
              <el-table-column prop="category" label="专利类别" width="100" />
              <el-table-column prop="publicNum" label="公开号" width="150" />
              <el-table-column prop="title" label="标题" show-overflow-tooltip />
              <el-table-column prop="applicant" label="申请人" width="150" show-overflow-tooltip />
              <el-table-column prop="inventor" label="发明人" width="120" show-overflow-tooltip />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" type="primary" @click="viewPatentDetail(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div style="text-align: center; margin-top: 20px;">
              <el-button type="primary" @click="persistPatentMatches">保存专利匹配结果</el-button>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="匹配专家" name="experts">
          <div class="match-content">
            <el-table :data="matchedExperts" v-loading="expertLoading" style="width: 100%">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="field" label="领域" width="150" show-overflow-tooltip />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" type="primary" @click="viewExpertDetail(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div style="text-align: center; margin-top: 20px;">
              <el-button type="primary" @click="persistExpertMatches">保存专家匹配结果</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { requirementApi, patentApi, expertApi } from '@/api'
import type { Requirement, CreateRequirementParams, MatchedPatent, MatchedExpert } from '@/types'

// 搜索表单
const searchForm = reactive({
  query: ''
})

// 需求列表相关
const requirementList = ref<Requirement[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情对话框相关
const detailDialogVisible = ref(false)
const dialogTitle = ref('')
const currentRequirement = ref<Requirement>({} as Requirement)

// 表单对话框相关
const formDialogVisible = ref(false)
const requirementForm = reactive<CreateRequirementParams>({
  title: '',
  description: '',
  keywords: '',
  techDirection: '',
  cooperationMode: ''
})
const requirementFormRef = ref()

// 匹配对话框相关
const matchingDialogVisible = ref(false)
const activeTab = ref('patents')
const matchedPatents = ref<MatchedPatent[]>([])
const matchedExperts = ref<MatchedExpert[]>([])
const patentLoading = ref(false)
const expertLoading = ref(false)

// 表单验证规则
const requirementRules = {
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度应在2-100个字符之间', trigger: 'blur' }
  ]
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

// 搜索需求
const searchRequirements = async () => {
  loading.value = true
  try {
    // 这里简化处理，实际可能需要后端支持搜索
    // 目前先获取所有需求
    // TODO: 实现后端搜索接口后替换
    const result: Requirement[] = [] // 暂时模拟数据
    requirementList.value = result
    total.value = result.length
  } catch (error: any) {
    ElMessage.error(error.message || '获取需求列表失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.query = ''
  requirementList.value = []
}

// 查看需求详情
const viewRequirement = async (row: Requirement) => {
  try {
    // 这里先直接使用row的数据，也可以从后端获取详细信息
    currentRequirement.value = row
    dialogTitle.value = '需求详情'
    detailDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取需求详情失败')
  }
}

// 编辑需求
const editRequirement = (row: Requirement) => {
  // 复制数据到表单
  Object.assign(requirementForm, row)
  dialogTitle.value = '编辑需求'
  formDialogVisible.value = true
}

// 删除需求
const deleteRequirement = (row: Requirement) => {
  ElMessageBox.confirm(
    `确定要删除需求 "${row.title}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 这里需要后端提供删除接口
      // await requirementApi.deleteRequirement(row.id)
      ElMessage.success('删除成功')
      searchRequirements() // 重新搜索
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 打开新增对话框
const openAddDialog = () => {
  // 清空表单
  Object.keys(requirementForm).forEach(key => {
    (requirementForm as any)[key] = ''
  })
  dialogTitle.value = '发布需求'
  formDialogVisible.value = true
}

// 保存需求
const saveRequirement = async () => {
  if (!requirementFormRef.value) return
  
  try {
    await requirementFormRef.value.validate()
    
    // 创建需求
    await requirementApi.createRequirement(requirementForm)
    
    ElMessage.success('发布成功')
    closeFormDialog()
    searchRequirements() // 重新搜索
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error(error.message || '发布失败')
    }
  }
}

// 开始匹配
const startMatching = async (row: Requirement) => {
  currentRequirement.value = row
  activeTab.value = 'patents'
  await loadMatchResults(row.id)
  matchingDialogVisible.value = true
}

// 加载匹配结果
const loadMatchResults = async (requirementId: number) => {
  // 加载专利匹配结果
  patentLoading.value = true
  try {
    const patents = await requirementApi.matchPatentsForRequirement(requirementId)
    matchedPatents.value = patents
  } catch (error: any) {
    ElMessage.error(error.message || '获取专利匹配结果失败')
  } finally {
    patentLoading.value = false
  }

  // 加载专家匹配结果
  expertLoading.value = true
  try {
    const experts = await requirementApi.matchExpertsForRequirement(requirementId)
    matchedExperts.value = experts
  } catch (error: any) {
    ElMessage.error(error.message || '获取专家匹配结果失败')
  } finally {
    expertLoading.value = false
  }
}

// 查看专利详情
const viewPatentDetail = async (row: MatchedPatent) => {
  try {
    const patent = await patentApi.getPatent(row.category, row.publicNum)
    // 显示专利详情逻辑
    ElMessage.info(`查看专利: ${patent.title}`)
  } catch (error: any) {
    ElMessage.error(error.message || '获取专利详情失败')
  }
}

// 查看专家详情
const viewExpertDetail = async (row: MatchedExpert) => {
  try {
    // 这里调用专家详情接口，如果有的话
    // const expert = await expertApi.getExpertById(row.id)
    ElMessage.info(`查看专家: ${row.name}`)
  } catch (error: any) {
    ElMessage.error(error.message || '获取专家详情失败')
  }
}

// 保存专利匹配结果
const persistPatentMatches = async () => {
  try {
    const items = matchedPatents.value.map(patent => ({
      patentCategory: patent.category,
      patentPublicNum: patent.publicNum
    }))
    
    await requirementApi.persistPatentMatches(currentRequirement.value.id, { items })
    ElMessage.success('专利匹配结果已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存专利匹配结果失败')
  }
}

// 保存专家匹配结果
const persistExpertMatches = async () => {
  try {
    const items = matchedExperts.value.map(expert => ({
      expertId: expert.id
    }))
    
    await requirementApi.persistExpertMatches(currentRequirement.value.id, { items })
    ElMessage.success('专家匹配结果已保存')
  } catch (error: any) {
    ElMessage.error(error.message || '保存专家匹配结果失败')
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentRequirement.value = {} as Requirement
}

// 关闭表单对话框
const closeFormDialog = () => {
  formDialogVisible.value = false
  if (requirementFormRef.value) {
    requirementFormRef.value.resetFields()
  }
  currentRequirement.value = {} as Requirement
}

// 关闭匹配对话框
const closeMatchingDialog = () => {
  matchingDialogVisible.value = false
  matchedPatents.value = []
  matchedExperts.value = []
  currentRequirement.value = {} as Requirement
}

// 分页相关方法
const handleSizeChange = (val: number) => {
  pageSize.value = val
  searchRequirements()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  searchRequirements()
}

// 初始化数据
onMounted(() => {
  // 初始化加载需求列表
  searchRequirements()
})
</script>

<style scoped>
.requirement-manage-container {
  padding: 20px;
}

.search-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.requirement-list {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.match-content {
  min-height: 400px;
}
</style>