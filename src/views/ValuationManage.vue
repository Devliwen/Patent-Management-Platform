<template>
  <div class="valuation-manage-container">
    <!-- 专利搜索区域 -->
    <div class="search-section">
      <el-form :model="searchForm" inline>
        <el-form-item label="专利类别">
          <el-select v-model="searchForm.patentCategory" placeholder="请选择专利类别" clearable>
            <el-option label="风能" value="wind" />
            <el-option label="太阳能" value="solar" />
            <el-option label="生物质能" value="biomass" />
            <el-option label="氢能" value="hydrogen" />
            <el-option label="锂电池" value="lilon" />
          </el-select>
        </el-form-item>
        <el-form-item label="专利公开号">
          <el-input v-model="searchForm.patentPublicNum" placeholder="请输入专利公开号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchValuations">搜索评估报告</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openGenerateDialog">生成评估报告</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 评估报告列表 -->
    <div class="valuation-list">
      <el-table 
        :data="valuationList" 
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="patentCategory" label="专利类别" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.patentCategory)">
              {{ getCategoryText(row.patentCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="patentPublicNum" label="专利公开号" width="150" show-overflow-tooltip />
        <el-table-column prop="reportTitle" label="报告标题" show-overflow-tooltip />
        <el-table-column prop="valuationAmount" label="评估价值(元)" width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.valuationAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="modelVersion" label="模型版本" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="valuationDate" label="评估日期" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="viewReport(row)">查看详情</el-button>
            <el-button size="small" type="primary" @click="generateNewReport(row)">重新评估</el-button>
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

    <!-- 生成评估报告对话框 -->
    <el-dialog 
      v-model="generateDialogVisible" 
      :title="dialogTitle" 
      width="50%" 
      :before-close="closeGenerateDialog"
    >
      <el-form 
        :model="generateForm" 
        :rules="generateRules" 
        ref="generateFormRef" 
        label-width="120px"
      >
        <el-form-item label="专利类别" prop="patentCategory">
          <el-select v-model="generateForm.patentCategory" placeholder="请选择专利类别" style="width: 100%">
            <el-option label="风能" value="wind" />
            <el-option label="太阳能" value="solar" />
            <el-option label="生物质能" value="biomass" />
            <el-option label="氢能" value="hydrogen" />
            <el-option label="锂电池" value="lilon" />
          </el-select>
        </el-form-item>
        <el-form-item label="专利公开号" prop="patentPublicNum">
          <el-input v-model="generateForm.patentPublicNum" placeholder="请输入专利公开号" />
        </el-form-item>
        <el-form-item label="模型版本">
          <el-input v-model="generateForm.modelVersion" placeholder="请输入模型版本（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeGenerateDialog">取消</el-button>
          <el-button type="primary" @click="generateReport" :loading="generating">生成报告</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 评估报告详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      :title="dialogTitle" 
      width="70%" 
      :before-close="closeDetailDialog"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ currentReport.id }}</el-descriptions-item>
        <el-descriptions-item label="专利类别">
          <el-tag :type="getCategoryTagType(currentReport.patentCategory)">
            {{ getCategoryText(currentReport.patentCategory) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="专利公开号">{{ currentReport.patentPublicNum }}</el-descriptions-item>
        <el-descriptions-item label="报告标题">{{ currentReport.reportTitle }}</el-descriptions-item>
        <el-descriptions-item label="评估价值(元)">
          <strong style="color: var(--el-color-primary); font-size: 18px;">
            {{ formatCurrency(currentReport.valuationAmount) }}
          </strong>
        </el-descriptions-item>
        <el-descriptions-item label="货币单位">{{ currentReport.currency }}</el-descriptions-item>
        <el-descriptions-item label="模型版本">{{ currentReport.modelVersion }}</el-descriptions-item>
        <el-descriptions-item label="评估方法">{{ currentReport.evaluationMethod }}</el-descriptions-item>
        <el-descriptions-item label="评估员">{{ currentReport.evaluator }}</el-descriptions-item>
        <el-descriptions-item label="评估日期">{{ currentReport.valuationDate }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentReport.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentReport.status)">
            {{ currentReport.status }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 评分详情 -->
      <div class="score-details-section" v-if="currentReport.scoreDetails">
        <h4>评分详情</h4>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="score-item">
              <div class="score-value">{{ currentReport.scoreDetails.technologicalInnovation }}</div>
              <div class="score-label">技术创新</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="score-item">
              <div class="score-value">{{ currentReport.scoreDetails.marketPotential }}</div>
              <div class="score-label">市场潜力</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="score-item">
              <div class="score-value">{{ currentReport.scoreDetails.legalStatus }}</div>
              <div class="score-label">法律状态</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="score-item">
              <div class="score-value">{{ currentReport.scoreDetails.economicValue }}</div>
              <div class="score-label">经济价值</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 报告内容 -->
      <div class="report-content-section" v-if="currentReport.reportContent">
        <h4>报告内容</h4>
        <div class="report-content">{{ currentReport.reportContent }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { valuationApi } from '@/api'
import type { ValuationReport, GenerateValuationParams, QueryValuationParams } from '@/types'

// 搜索表单
const searchForm = reactive({
  patentCategory: '' as '' | 'wind' | 'solar' | 'biomass' | 'hydrogen' | 'lilon',
  patentPublicNum: ''
})

// 评估报告列表相关
const valuationList = ref<ValuationReport[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 生成报告对话框相关
const generateDialogVisible = ref(false)
const dialogTitle = ref('')
const generateForm = reactive<GenerateValuationParams>({
  patentCategory: 'wind' as const,
  patentPublicNum: '',
  modelVersion: 'v1'
})
const generateFormRef = ref()
const generating = ref(false)

// 详情对话框相关
const detailDialogVisible = ref(false)
const currentReport = ref<ValuationReport>({} as ValuationReport)

// 生成报告表单验证规则
const generateRules = {
  patentCategory: [
    { required: true, message: '请选择专利类别', trigger: 'change' }
  ],
  patentPublicNum: [
    { required: true, message: '请输入专利公开号', trigger: 'blur' },
    { min: 5, max: 50, message: '专利公开号长度应在5-50个字符之间', trigger: 'blur' }
  ]
}

// 获取专利类别文本
const getCategoryText = (category: string) => {
  const categoryMap: Record<string, string> = {
    'wind': '风能',
    'solar': '太阳能',
    'biomass': '生物质能',
    'hydrogen': '氢能',
    'lilon': '锂电池'
  }
  return categoryMap[category] || category
}

// 获取专利类别标签类型
const getCategoryTagType = (category: string) => {
  const typeMap: Record<string, string> = {
    'wind': 'primary',
    'solar': 'success',
    'biomass': 'warning',
    'hydrogen': 'danger',
    'lilon': 'info'
  }
  return typeMap[category] || 'info'
}

// 获取状态标签类型
const getStatusType = (status: string) => {
  return status === 'completed' || status === 'Completed' ? 'success' : 'warning'
}

// 格式化货币
const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2
  }).format(amount)
}

// 搜索评估报告
const searchValuations = async () => {
  if (!searchForm.patentCategory || !searchForm.patentPublicNum) {
    ElMessage.warning('请填写专利类别和专利公开号')
    return
  }
  
  loading.value = true
  try {
    const queryParams: QueryValuationParams = {
      patentCategory: searchForm.patentCategory,
      patentPublicNum: searchForm.patentPublicNum
    }
    
    const result = await valuationApi.getValuationReports(queryParams)
    valuationList.value = result
    total.value = result.length
  } catch (error: any) {
    ElMessage.error(error.message || '获取评估报告列表失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.patentCategory = ''
  searchForm.patentPublicNum = ''
  valuationList.value = []
  total.value = 0
}

// 打开生成报告对话框
const openGenerateDialog = () => {
  // 清空表单
  Object.keys(generateForm).forEach(key => {
    if (key === 'patentCategory') {
      (generateForm as any)[key] = 'wind'
    } else {
      (generateForm as any)[key] = key === 'modelVersion' ? 'v1' : ''
    }
  })
  
  dialogTitle.value = '生成专利价值评估报告'
  generateDialogVisible.value = true
}

// 生成评估报告
const generateReport = async () => {
  if (!generateFormRef.value) return
  
  try {
    await generateFormRef.value.validate()
    
    generating.value = true
    const result = await valuationApi.generateValuationReport(generateForm)
    
    ElMessage.success('评估报告生成成功')
    closeGenerateDialog()
    
    // 如果当前搜索条件匹配，则刷新列表
    if (searchForm.patentCategory === generateForm.patentCategory && 
        searchForm.patentPublicNum === generateForm.patentPublicNum) {
      searchValuations()
    }
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error(error.message || '生成评估报告失败')
    }
  } finally {
    generating.value = false
  }
}

// 查看报告详情
const viewReport = (row: ValuationReport) => {
  currentReport.value = { ...row }
  dialogTitle.value = `评估报告详情 - ${row.patentPublicNum}`
  detailDialogVisible.value = true
}

// 重新评估
const generateNewReport = (row: ValuationReport) => {
  // 预填表单
  generateForm.patentCategory = row.patentCategory
  generateForm.patentPublicNum = row.patentPublicNum
  generateForm.modelVersion = row.modelVersion || 'v1'
  
  dialogTitle.value = '重新生成专利价值评估报告'
  generateDialogVisible.value = true
}

// 关闭生成报告对话框
const closeGenerateDialog = () => {
  generateDialogVisible.value = false
  if (generateFormRef.value) {
    generateFormRef.value.resetFields()
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentReport.value = {} as ValuationReport
}

// 分页相关方法
const handleSizeChange = (val: number) => {
  pageSize.value = val
  searchValuations()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  searchValuations()
}

// 初始化数据
onMounted(() => {
  // 初始为空列表，等待用户搜索
})
</script>

<style scoped>
.valuation-manage-container {
  padding: 20px;
}

.search-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.valuation-list {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.score-details-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.score-item {
  text-align: center;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #f5f7fa;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-color-primary);
  margin-bottom: 5px;
}

.score-label {
  font-size: 14px;
  color: #606266;
}

.report-content-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.report-content {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>