<template>
  <div class="patent-manage-container">
    <!-- 页面标题和操作按钮 -->
    <div class="page-header">
      <h2 class="page-title">我的专利</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showUploadDialog">
          <el-icon><Plus /></el-icon>
          上传新专利
        </el-button>
        <el-button @click="refreshList">
          <el-icon><Refresh /></el-icon>
          刷新列表
        </el-button>
      </div>
    </div>

    <!-- 个人专利统计 -->
    <div class="stats-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ userPatents.length }}</div>
              <div class="stat-label">我的专利总数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ getVisibilityCount('PUBLIC') }}</div>
              <div class="stat-label">公开专利</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ getVisibilityCount('PRIVATE') }}</div>
              <div class="stat-label">私有专利</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ userPatents.filter(p => p.category === 'solar').length }}</div>
              <div class="stat-label">太阳能专利</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 个人专利搜索区域 -->
    <div class="search-section">
      <el-form :model="searchForm" inline>
        <el-form-item label="搜索我的专利">
          <el-input 
            v-model="searchForm.query" 
            placeholder="请输入标题/公开号进行搜索"
            @keyup.enter="searchUserPatents"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchUserPatents">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 专利列表 -->
    <div class="patent-list">
      <el-table 
        :data="filteredPatents" 
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无专利数据，请上传您的专利"
      >
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="category" label="类别" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publicNum" label="公开号" width="150" show-overflow-tooltip />
        <el-table-column prop="applicant" label="申请人" width="120" show-overflow-tooltip />
        <el-table-column prop="inventor" label="发明人" width="120" show-overflow-tooltip />
        <el-table-column prop="visibility" label="可见性" width="100">
          <template #default="{ row }">
            <el-tag :type="getVisibilityType(row.visibility)">{{ getVisibilityText(row.visibility) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
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
        :total="filteredPatents.length"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; text-align: center;"
      />
    </div>

    <!-- 专利上传/编辑对话框 -->
    <el-dialog 
      v-model="uploadDialogVisible" 
      :title="uploadDialogTitle" 
      width="70%"
      :before-close="closeUploadDialog"
    >
      <el-form 
        ref="patentFormRef" 
        :model="patentForm" 
        :rules="patentRules"
        label-width="120px"
      >
        <el-form-item label="专利类别" prop="category">
          <el-select v-model="patentForm.category" placeholder="请选择专利类别" style="width: 100%">
            <el-option label="风能" value="wind" />
            <el-option label="太阳能" value="solar" />
            <el-option label="生物质能" value="biomass" />
            <el-option label="氢能" value="hydrogen" />
            <el-option label="锂电池" value="lilon" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="标题" prop="title">
          <el-input v-model="patentForm.title" placeholder="请输入专利标题" />
        </el-form-item>
        
        <el-form-item label="公开号">
          <el-input v-model="patentForm.publicNum" placeholder="请输入专利公开号（可选）" />
        </el-form-item>
        
        <el-form-item label="摘要">
          <el-input 
            v-model="patentForm.abstractText" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入专利摘要（可选）" 
          />
        </el-form-item>
        
        <el-form-item label="IPC分类">
          <el-input v-model="patentForm.ipc" placeholder="请输入IPC分类（可选）" />
        </el-form-item>
        
        <el-form-item label="CPC分类">
          <el-input v-model="patentForm.cpc" placeholder="请输入CPC分类（可选）" />
        </el-form-item>
        
        <el-form-item label="申请人">
          <el-input v-model="patentForm.applicant" placeholder="请输入申请人（可选）" />
        </el-form-item>
        
        <el-form-item label="发明人">
          <el-input v-model="patentForm.inventor" placeholder="请输入发明人（可选）" />
        </el-form-item>
        
        <el-form-item label="可见性">
          <el-select v-model="patentForm.visibility" placeholder="请选择可见性" style="width: 100%">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="私有" value="PRIVATE" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeUploadDialog">取消</el-button>
          <el-button type="primary" @click="submitPatent" :loading="uploading">
            {{ uploadDialogTitle === '上传专利' ? '上传' : '保存' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { patentApi } from '@/api'

// 专利表单类型定义 - 匹配后端接口
interface PatentFormData {
  category: string
  title: string
  publicNum?: string
  abstractText?: string
  ipc?: string
  cpc?: string
  applicant?: string
  inventor?: string
  visibility?: string
  id?: number
  ownerUserId?: number
  createdAt?: string
}

// 搜索表单类型
interface SearchForm {
  query: string
}

// 响应式数据
const loading = ref(false)
const uploading = ref(false)
const uploadDialogVisible = ref(false)
const uploadDialogTitle = ref('上传专利')
const patentFormRef = ref<FormInstance>()
const currentPage = ref(1)
const pageSize = ref(10)

// 表单数据
const patentForm = reactive<PatentFormData>({
  category: '',
  title: '',
  publicNum: '',
  abstractText: '',
  ipc: '',
  cpc: '',
  applicant: '',
  inventor: '',
  visibility: 'PUBLIC'
})

const searchForm = reactive<SearchForm>({
  query: ''
})

// 用户个人专利数据（从后端API获取）
const userPatents = ref<PatentFormData[]>([])

// 加载用户个人专利列表
const loadUserPatents = async () => {
  loading.value = true
  try {
    const response = await patentApi.getUserPatentsList({
      owner: 'me',
      query: searchForm.query,
      page: currentPage.value,
      size: pageSize.value
    })
    
    if (response && response.data) {
      userPatents.value = response.data
      console.log('用户专利数据加载成功:', userPatents.value.length)
    } else {
      console.error('API返回的数据格式异常:', response)
      ElMessage.error('数据格式异常')
      userPatents.value = []
    }
  } catch (error) {
    console.error('加载用户专利失败:', error)
    ElMessage.error('加载专利列表失败')
    userPatents.value = []
  } finally {
    loading.value = false
  }
}

// 表单验证规则
const patentRules: FormRules = {
  category: [
    { required: true, message: '请选择专利类别', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' }
  ]
}

// 计算属性：过滤后的专利列表（支持搜索）
const filteredPatents = computed(() => {
  if (!searchForm.query.trim()) {
    return userPatents.value
  }
  
  const query = searchForm.query.toLowerCase()
  return userPatents.value.filter(patent => 
    patent.title.toLowerCase().includes(query) ||
    (patent.publicNum && patent.publicNum.toLowerCase().includes(query)) ||
    (patent.applicant && patent.applicant.toLowerCase().includes(query))
  )
})

// 获取状态统计（根据可见性统计）
const getVisibilityCount = (visibility: string) => {
  return userPatents.value.filter(p => p.visibility === visibility).length
}

// 获取可见性标签类型
const getVisibilityType = (visibility?: string) => {
  switch (visibility) {
    case 'PUBLIC': return 'success'
    case 'PRIVATE': return 'warning'
    default: return 'info'
  }
}

// 获取可见性文本
const getVisibilityText = (visibility?: string) => {
  switch (visibility) {
    case 'PUBLIC': return '公开'
    case 'PRIVATE': return '私有'
    default: return '未知'
  }
}

// 搜索用户专利
const searchUserPatents = () => {
  currentPage.value = 1
  loadUserPatents()
}

// 重置搜索
const resetSearch = () => {
  searchForm.query = ''
  currentPage.value = 1
  loadUserPatents()
}

// 刷新列表
const refreshList = () => {
  loadUserPatents()
  ElMessage.success('列表已刷新')
}

// 显示上传对话框
const showUploadDialog = () => {
  uploadDialogTitle.value = '上传专利'
  // 重置表单
  Object.assign(patentForm, {
    category: '',
    title: '',
    publicNum: '',
    abstractText: '',
    ipc: '',
    cpc: '',
    applicant: '',
    inventor: '',
    visibility: 'PUBLIC',
    id: undefined
  })
  uploadDialogVisible.value = true
}

// 关闭上传对话框
const closeUploadDialog = () => {
  uploadDialogVisible.value = false
  patentFormRef.value?.clearValidate()
}

// 提交专利
const submitPatent = async () => {
  if (!patentFormRef.value) return
  
  try {
    await patentFormRef.value.validate()
    uploading.value = true
    
    if (patentForm.id) {
      // 编辑专利
      await patentApi.updateUserPatent(patentForm.id, {
        category: patentForm.category,
        title: patentForm.title,
        publicNum: patentForm.publicNum,
        abstractText: patentForm.abstractText,
        ipc: patentForm.ipc,
        cpc: patentForm.cpc,
        applicant: patentForm.applicant,
        inventor: patentForm.inventor,
        visibility: patentForm.visibility
      })
      ElMessage.success('专利更新成功')
    } else {
      // 上传新专利
      await patentApi.createUserPatent({
        category: patentForm.category,
        title: patentForm.title,
        publicNum: patentForm.publicNum,
        abstractText: patentForm.abstractText,
        ipc: patentForm.ipc,
        cpc: patentForm.cpc,
        applicant: patentForm.applicant,
        inventor: patentForm.inventor,
        visibility: patentForm.visibility
      })
      ElMessage.success('专利上传成功')
    }
    
    closeUploadDialog()
    loadUserPatents() // 重新加载列表
  } catch (error) {
    console.error('提交专利失败:', error)
    ElMessage.error('提交失败，请检查表单')
  } finally {
    uploading.value = false
  }
}

// 查看专利详情
const viewPatent = async (patent: PatentFormData) => {
  if (!patent.id) {
    ElMessage.warning('无法获取专利详情')
    return
  }
  
  try {
    const response = await patentApi.getUserPatentDetail(patent.id)
    const detail = response.data
    
    ElMessageBox.alert(
      `<div>
        <p><strong>标题：</strong>${detail.title}</p>
        <p><strong>类别：</strong>${detail.category}</p>
        <p><strong>公开号：</strong>${detail.publicNum || '未填写'}</p>
        <p><strong>摘要：</strong>${detail.abstractText || '未填写'}</p>
        <p><strong>申请人：</strong>${detail.applicant || '未填写'}</p>
        <p><strong>发明人：</strong>${detail.inventor || '未填写'}</p>
        <p><strong>IPC分类：</strong>${detail.ipc || '未填写'}</p>
        <p><strong>CPC分类：</strong>${detail.cpc || '未填写'}</p>
        <p><strong>可见性：</strong>${getVisibilityText(detail.visibility)}</p>
        <p><strong>创建时间：</strong>${detail.createdAt || '未知'}</p>
      </div>`,
      '专利详情',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
    )
  } catch (error) {
    console.error('获取专利详情失败:', error)
    ElMessage.error('获取专利详情失败')
  }
}

// 编辑专利
const editPatent = (patent: PatentFormData) => {
  uploadDialogTitle.value = '编辑专利'
  Object.assign(patentForm, patent)
  uploadDialogVisible.value = true
}

// 删除专利
const deletePatent = async (patent: PatentFormData) => {
  if (!patent.id) {
    ElMessage.warning('无法删除该专利')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除专利"${patent.title}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await patentApi.deleteUserPatentById(patent.id)
    ElMessage.success('专利删除成功')
    loadUserPatents() // 重新加载列表
  } catch (error) {
    // 用户取消删除
    console.error('删除专利失败:', error)
  }
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

// 页面加载时初始化
onMounted(() => {
  console.log('专利管理页面已加载')
  loadUserPatents()
})

// 样式定义
const styles = `
.patent-manage-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  color: #303133;
}

.stats-section {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-content {
  padding: 10px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.search-section {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.patent-list {
  background: white;
  border-radius: 4px;
  padding: 20px;
}
`

// 添加样式到页面
const styleElement = document.createElement('style')
styleElement.textContent = styles
document.head.appendChild(styleElement)
</script>