<template>
  <div class="requirement-square-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">需求广场</h2>
      <div class="header-actions">
        <el-button type="primary" @click="goToDemandPublish">
          <el-icon><Plus /></el-icon>
          发布需求
        </el-button>
      </div>
    </div>

    <!-- 需求搜索区域 -->
    <div class="search-section">
      <div class="search-header">
        <span class="search-title">需求搜索</span>
      </div>
      <el-form :model="searchForm" inline>
        <el-form-item label="搜索关键词">
          <el-input 
            v-model="searchForm.query" 
            placeholder="请输入标题/关键词"
            @keyup.enter="searchRequirements"
            style="width: 300px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchRequirements">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
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
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewRequirement(row)">查看详情</el-button>
            <el-button size="small" type="success" @click="startMatching(row)">智能匹配</el-button>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { requirementApi, patentApi, expertApi } from '@/api'
import type { Requirement, CreateRequirementParams, MatchedPatent, MatchedExpert } from '@/types'

const router = useRouter()

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

// 匹配对话框相关
const matchingDialogVisible = ref(false)
const activeTab = ref('patents')
const matchedPatents = ref<MatchedPatent[]>([])
const matchedExperts = ref<MatchedExpert[]>([])
const patentLoading = ref(false)
const expertLoading = ref(false)

// 跳转到需求发布页面
const goToDemandPublish = () => {
  router.push('/demand')
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
    const response = await requirementApi.getAllRequirements({
      page: currentPage.value,
      size: pageSize.value,
      query: searchForm.query
    })
    
    if (response) {
      requirementList.value = response
      // 这里假设后端返回的是分页数据，实际可能需要根据后端接口调整
      // 如果后端返回分页信息，应该使用 response.data 和 response.total
      total.value = response.length
      console.log('需求数据加载成功:', requirementList.value.length)
    } else {
      console.error('API返回的数据格式异常:', response)
      ElMessage.error('数据格式异常')
      requirementList.value = []
      total.value = 0
    }
  } catch (error: any) {
    console.error('获取需求列表失败:', error)
    ElMessage.error(error.message || '获取需求列表失败')
    requirementList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.query = ''
  currentPage.value = 1
  searchRequirements()
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  searchRequirements()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  searchRequirements()
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

// 关闭匹配对话框
const closeMatchingDialog = () => {
  matchingDialogVisible.value = false
  matchedPatents.value = []
  matchedExperts.value = []
  currentRequirement.value = {} as Requirement
}


// 初始化数据
onMounted(() => {
  // 初始化加载需求列表
  searchRequirements()
})
</script>

<style scoped>
.requirement-square-container {
  padding: 20px;
  background-color: var(--el-bg-color-page);
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.page-title {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-section {
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.search-header {
  padding: 18px 20px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.search-title {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.search-section .el-form {
  padding: 20px;
}

.requirement-list {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.match-content {
  min-height: 400px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .requirement-square-container {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  
  .search-section .el-form-item {
    margin-bottom: 10px;
  }
}
</style>