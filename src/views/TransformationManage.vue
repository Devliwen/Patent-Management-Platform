<template>
  <div class="transformation-manage-container">
    <!-- 搜索区域 -->
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
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="成功" value="Success" />
            <el-option label="进行中" value="In Progress" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchTransformations">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openAddDialog">添加成果</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 转化成果列表 -->
    <div class="transformation-list">
      <el-table 
        :data="transformationList" 
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
        <el-table-column prop="expertId" label="专家ID" width="100" />
        <el-table-column prop="requirementId" label="需求ID" width="100" />
        <el-table-column prop="partnerOrgId" label="合作方ID" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="benefitAmount" label="转化效益(元)" width="120">
          <template #default="{ row }">
            {{ row.benefitAmount ? formatBenefitAmount(row.benefitAmount) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="transformationDate" label="转化日期" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="viewTransformation(row)">查看</el-button>
            <el-button size="small" type="primary" @click="editTransformation(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTransformation(row)">删除</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      :title="dialogTitle" 
      width="60%" 
      :before-close="closeDetailDialog"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ currentTransformation.id }}</el-descriptions-item>
        <el-descriptions-item label="专利类别">
          <el-tag :type="getCategoryTagType(currentTransformation.patentCategory)">
            {{ getCategoryText(currentTransformation.patentCategory) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="专利公开号">{{ currentTransformation.patentPublicNum }}</el-descriptions-item>
        <el-descriptions-item label="专家ID" v-if="currentTransformation.expertId">{{ currentTransformation.expertId }}</el-descriptions-item>
        <el-descriptions-item label="需求ID" v-if="currentTransformation.requirementId">{{ currentTransformation.requirementId }}</el-descriptions-item>
        <el-descriptions-item label="合作方ID" v-if="currentTransformation.partnerOrgId">{{ currentTransformation.partnerOrgId }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2" v-if="currentTransformation.description">
          {{ currentTransformation.description }}
        </el-descriptions-item>
        <el-descriptions-item label="转化日期" v-if="currentTransformation.transformationDate">{{ currentTransformation.transformationDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentTransformation.status)">
            {{ currentTransformation.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="转化效益(元)" v-if="currentTransformation.benefitAmount">
          {{ formatBenefitAmount(currentTransformation.benefitAmount) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="formDialogVisible" 
      :title="dialogTitle" 
      width="50%" 
      :before-close="closeFormDialog"
    >
      <el-form 
        :model="transformationForm" 
        :rules="transformationRules" 
        ref="transformationFormRef" 
        label-width="120px"
      >
        <el-form-item label="专利类别" prop="patentCategory">
          <el-select v-model="transformationForm.patentCategory" placeholder="请选择专利类别" style="width: 100%">
            <el-option label="风能" value="wind" />
            <el-option label="太阳能" value="solar" />
            <el-option label="生物质能" value="biomass" />
            <el-option label="氢能" value="hydrogen" />
            <el-option label="锂电池" value="lilon" />
          </el-select>
        </el-form-item>
        <el-form-item label="专利公开号" prop="patentPublicNum">
          <el-input v-model="transformationForm.patentPublicNum" placeholder="请输入专利公开号" />
        </el-form-item>
        <el-form-item label="专家ID">
          <el-input v-model.number="transformationForm.expertId" placeholder="请输入专家ID" />
        </el-form-item>
        <el-form-item label="需求ID">
          <el-input v-model.number="transformationForm.requirementId" placeholder="请输入需求ID" />
        </el-form-item>
        <el-form-item label="合作方ID">
          <el-input v-model.number="transformationForm.partnerOrgId" placeholder="请输入合作方ID" />
        </el-form-item>
        <el-form-item label="转化日期">
          <el-date-picker
            v-model="transformationForm.transformationDate"
            type="date"
            placeholder="选择转化日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="transformationForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="成功" value="Success" />
            <el-option label="进行中" value="In Progress" />
          </el-select>
        </el-form-item>
        <el-form-item label="转化效益(元)">
          <el-input-number 
            v-model="transformationForm.benefitAmount" 
            :precision="2" 
            :min="0" 
            placeholder="请输入转化效益金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input 
            v-model="transformationForm.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入转化成果描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeFormDialog">取消</el-button>
          <el-button type="primary" @click="saveTransformation">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { transformationApi } from '@/api'
import type { TransformationResult, CreateTransformationParams } from '@/types'

// 搜索表单
const searchForm = reactive({
  patentCategory: '' as '' | 'wind' | 'solar' | 'biomass' | 'hydrogen' | 'lilon',
  status: '' as '' | 'Success' | 'In Progress'
})

// 转化成果列表相关
const transformationList = ref<TransformationResult[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情对话框相关
const detailDialogVisible = ref(false)
const dialogTitle = ref('')
const currentTransformation = ref<TransformationResult>({} as TransformationResult)

// 表单对话框相关
const formDialogVisible = ref(false)
const transformationForm = reactive<CreateTransformationParams>({
  patentCategory: 'wind' as const,
  patentPublicNum: '',
  expertId: undefined,
  requirementId: undefined,
  partnerOrgId: undefined,
  description: '',
  transformationDate: '',
  status: 'In Progress',
  benefitAmount: undefined
})
const transformationFormRef = ref()

// 表单验证规则
const transformationRules = {
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
  return status === 'Success' ? 'success' : 'warning'
}

// 格式化效益金额
const formatBenefitAmount = (amount: number) => {
  return new Intl.NumberFormat('zh-CN').format(amount)
}

// 搜索转化成果
const searchTransformations = async () => {
  loading.value = true
  try {
    // 这里先获取所有转化成果，后续可实现后端搜索功能
    const result = await transformationApi.getAllTransformations()
    transformationList.value = result
    
    // 根据搜索条件过滤数据
    let filteredList = [...result]
    if (searchForm.patentCategory) {
      filteredList = filteredList.filter(item => item.patentCategory === searchForm.patentCategory)
    }
    if (searchForm.status) {
      filteredList = filteredList.filter(item => item.status === searchForm.status)
    }
    
    transformationList.value = filteredList
    total.value = filteredList.length
  } catch (error: any) {
    ElMessage.error(error.message || '获取转化成果列表失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.patentCategory = ''
  searchForm.status = ''
  searchTransformations()
}

// 查看转化成果详情
const viewTransformation = (row: TransformationResult) => {
  currentTransformation.value = { ...row }
  dialogTitle.value = '转化成果详情'
  detailDialogVisible.value = true
}

// 编辑转化成果
const editTransformation = (row: TransformationResult) => {
  // 复制数据到表单
  Object.assign(transformationForm, row)
  dialogTitle.value = '编辑转化成果'
  formDialogVisible.value = true
}

// 删除转化成果
const deleteTransformation = (row: TransformationResult) => {
  ElMessageBox.confirm(
    `确定要删除转化成果 "${row.patentPublicNum}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 这里需要后端提供删除接口
      // await transformationApi.deleteTransformation(row.id)
      ElMessage.success('删除成功')
      searchTransformations() // 重新搜索
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
  Object.keys(transformationForm).forEach(key => {
    if (key === 'patentCategory') {
      (transformationForm as any)[key] = 'wind'
    } else if (key === 'status') {
      (transformationForm as any)[key] = 'In Progress'
    } else {
      (transformationForm as any)[key] = key === 'benefitAmount' ? undefined : ''
    }
  })
  transformationForm.expertId = undefined
  transformationForm.requirementId = undefined
  transformationForm.partnerOrgId = undefined
  transformationForm.benefitAmount = undefined
  
  dialogTitle.value = '添加转化成果'
  formDialogVisible.value = true
}

// 保存转化成果
const saveTransformation = async () => {
  if (!transformationFormRef.value) return
  
  try {
    await transformationFormRef.value.validate()
    
    if (currentTransformation.value.id) {
      // 编辑模式 - 需要后端提供更新接口
      // await transformationApi.updateTransformation(currentTransformation.value.id, transformationForm)
      ElMessage.warning('编辑功能需要后端提供更新接口')
    } else {
      // 创建模式
      await transformationApi.createTransformation(transformationForm)
      ElMessage.success('添加成功')
    }
    
    closeFormDialog()
    searchTransformations() // 重新搜索
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentTransformation.value = {} as TransformationResult
}

// 关闭表单对话框
const closeFormDialog = () => {
  formDialogVisible.value = false
  if (transformationFormRef.value) {
    transformationFormRef.value.resetFields()
  }
  currentTransformation.value = {} as TransformationResult
}

// 分页相关方法
const handleSizeChange = (val: number) => {
  pageSize.value = val
  searchTransformations()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  searchTransformations()
}

// 初始化数据
onMounted(() => {
  searchTransformations()
})
</script>

<style scoped>
.transformation-manage-container {
  padding: 20px;
}

.search-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.transformation-list {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}
</style>