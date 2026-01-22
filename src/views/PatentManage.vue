<template>
  <div class="patent-manage-container">
    <!-- 专利搜索区域 -->
    <div class="search-section">
      <el-form :model="searchForm" inline>
        <el-form-item label="专利类别">
          <el-select v-model="searchForm.category" placeholder="请选择专利类别" clearable>
            <el-option label="风能" value="wind" />
            <el-option label="太阳能" value="solar" />
            <el-option label="生物质能" value="biomass" />
            <el-option label="氢能" value="hydrogen" />
            <el-option label="锂电池" value="lilon" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索关键词">
          <el-input 
            v-model="searchForm.query" 
            placeholder="请输入标题/摘要/申请人/发明人"
            @keyup.enter="searchPatents"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchPatents">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openAddDialog">新增专利</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 专利列表 -->
    <div class="patent-list">
      <el-table 
        :data="patentList" 
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="publicNum" label="公开号" width="150" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="applicant" label="申请人" width="150" show-overflow-tooltip />
        <el-table-column prop="inventor" label="发明人" width="120" show-overflow-tooltip />
        <el-table-column prop="ipc" label="IPC" width="120" show-overflow-tooltip />
        <el-table-column prop="appliDate" label="申请日期" width="120" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="viewPatent(row)">查看</el-button>
            <el-button size="small" type="primary" @click="editPatent(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePatent(row)">删除</el-button>
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

    <!-- 新增/编辑专利对话框 -->
    <el-dialog 
      v-model="formDialogVisible" 
      :title="dialogTitle" 
      width="60%" 
      :before-close="closeFormDialog"
    >
      <el-form 
        :model="patentForm" 
        :rules="patentRules" 
        ref="patentFormRef" 
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专利类别" prop="category">
              <el-select v-model="patentForm.category" placeholder="请选择专利类别" style="width: 100%">
                <el-option label="风能" value="wind" />
                <el-option label="太阳能" value="solar" />
                <el-option label="生物质能" value="biomass" />
                <el-option label="氢能" value="hydrogen" />
                <el-option label="锂电池" value="lilon" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公开号" prop="publicNum">
              <el-input v-model="patentForm.publicNum" placeholder="请输入公开号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="标题" prop="title">
              <el-input v-model="patentForm.title" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="申请人" prop="applicant">
              <el-input v-model="patentForm.applicant" placeholder="请输入申请人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发明人" prop="inventor">
              <el-input v-model="patentForm.inventor" placeholder="请输入发明人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="IPC分类">
              <el-input v-model="patentForm.ipc" placeholder="请输入IPC分类" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="CPC分类">
              <el-input v-model="patentForm.cpc" placeholder="请输入CPC分类" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="申请号">
              <el-input v-model="patentForm.appliNum" placeholder="请输入申请号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请日期">
              <el-input v-model="patentForm.appliDate" placeholder="请输入申请日期" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="公布日期">
              <el-input v-model="patentForm.publicDate" placeholder="请输入公布日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法律状态">
              <el-input v-model="patentForm.legalStatus" placeholder="请输入法律状态" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="摘要">
          <el-input 
            v-model="patentForm.abstractText" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入摘要"
          />
        </el-form-item>
        <el-form-item label="详情">
          <el-input 
            v-model="patentForm.patentDetails" 
            type="textarea" 
            :rows="6" 
            placeholder="请输入详情"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeFormDialog">取消</el-button>
          <el-button type="primary" @click="savePatent">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { patentApi } from '@/api'
import type { PatentBase, PatentCategory, PatentQueryParams, PatentCreateUpdateParams } from '@/types'

// 搜索表单
const searchForm = reactive({
  category: '' as PatentCategory | '',
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

// 表单对话框相关
const formDialogVisible = ref(false)
const patentForm = reactive<Omit<PatentCreateUpdateParams, 'category'> & { category: PatentCategory | '' }>({
  publicNum: '',
  title: '',
  abstractText: '',
  applicant: '',
  inventor: '',
  ipc: '',
  appliNum: '',
  appliDate: '',
  publicDate: '',
  cpc: '',
  nec: '',
  legalStatus: '',
  latestLegalStatus: '',
  status: '',
  type: '',
  applicantAddress: '',
  patentee: '',
  patenteeAddress: '',
  agent: '',
  patentDetails: '',
  category: ''
})
const patentFormRef = ref()

// 表单验证规则
const patentRules = {
  category: [
    { required: true, message: '请选择专利类别', trigger: 'change' }
  ],
  publicNum: [
    { required: true, message: '请输入公开号', trigger: 'blur' }
  ],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' }
  ],
  applicant: [
    { required: true, message: '请输入申请人', trigger: 'blur' }
  ]
}

// 搜索专利
const searchPatents = async () => {
  loading.value = true
  try {
    const params: PatentQueryParams = {
      category: searchForm.category as PatentCategory,
      query: searchForm.query
    }
    
    if (!params.category) {
      ElMessage.warning('请选择专利类别')
      return
    }
    
    const result = await patentApi.getPatents(params)
    patentList.value = result
    total.value = result.length // 这里简单处理总数，实际应从后端返回
  } catch (error: any) {
    ElMessage.error(error.message || '获取专利列表失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.category = ''
  searchForm.query = ''
  patentList.value = []
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

// 编辑专利
const editPatent = (row: PatentBase) => {
  // 复制数据到表单
  Object.assign(patentForm, row)
  // 如果 row 中有 category 字段则使用，否则留空
  patentForm.category = (row as any).category ? (row as any).category as PatentCategory : ''
  dialogTitle.value = '编辑专利'
  formDialogVisible.value = true
}

// 删除专利
const deletePatent = (row: PatentBase) => {
  ElMessageBox.confirm(
    `确定要删除专利 "${row.title}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 这里需要后端提供删除接口
      // await patentApi.deletePatent(row.category as PatentCategory, row.publicNum)
      ElMessage.success('删除成功')
      searchPatents() // 重新搜索
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
  Object.keys(patentForm).forEach(key => {
    if (key !== 'category') {
      (patentForm as any)[key] = ''
    }
  })
  patentForm.category = ''
  dialogTitle.value = '新增专利'
  formDialogVisible.value = true
}

// 保存专利
const savePatent = async () => {
  if (!patentFormRef.value) return
  
  try {
    await patentFormRef.value.validate()
    
    if (!patentForm.category) {
      ElMessage.error('请选择专利类别')
      return
    }
    
    const params: PatentCreateUpdateParams = {
      ...patentForm,
      category: patentForm.category as PatentCategory
    }
    
    await patentApi.createOrUpdatePatent(params)
    ElMessage.success('保存成功')
    closeFormDialog()
    searchPatents() // 重新搜索
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error(error.message || '保存失败')
    }
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  currentPatent.value = {} as PatentBase
}

// 关闭表单对话框
const closeFormDialog = () => {
  formDialogVisible.value = false
  if (patentFormRef.value) {
    patentFormRef.value.resetFields()
  }
}

// 分页相关方法
const handleSizeChange = (val: number) => {
  pageSize.value = val
  searchPatents()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  searchPatents()
}

// 初始化数据
onMounted(() => {
  // 可以初始化加载默认数据，比如全部风能专利
  // searchForm.category = 'wind'
  // searchPatents()
})
</script>

<style scoped>
.patent-manage-container {
  padding: 20px;
}

.search-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.patent-list {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.abstract-content, .details-content {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>