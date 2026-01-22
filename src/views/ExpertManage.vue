<template>
  <div class="expert-manage-container">
    <!-- 专家搜索区域 -->
    <div class="search-section">
      <el-form :model="searchForm" inline>
        <el-form-item label="搜索关键词">
          <el-input 
            v-model="searchForm.query" 
            placeholder="请输入姓名/领域"
            @keyup.enter="searchExperts"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchExperts">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openAddDialog">新增专家</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 专家列表 -->
    <div class="expert-list">
      <el-table 
        :data="expertList" 
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="field" label="领域" width="150" show-overflow-tooltip />
        <el-table-column prop="expertise" label="专长" show-overflow-tooltip />
        <el-table-column prop="achievements" label="成果" show-overflow-tooltip />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="viewExpert(row)">查看</el-button>
            <el-button size="small" type="primary" @click="editExpert(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteExpert(row)">删除</el-button>
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

    <!-- 专家详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      :title="dialogTitle" 
      width="60%" 
      :before-close="closeDetailDialog"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ currentExpert.id }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ currentExpert.name }}</el-descriptions-item>
        <el-descriptions-item label="领域">{{ currentExpert.field }}</el-descriptions-item>
        <el-descriptions-item label="专长">{{ currentExpert.expertise }}</el-descriptions-item>
        <el-descriptions-item label="成果" v-if="currentExpert.achievements" :span="2">
          {{ currentExpert.achievements }}
        </el-descriptions-item>
        <el-descriptions-item label="联系方式" v-if="currentExpert.contactInfo" :span="2">
          {{ currentExpert.contactInfo }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 新增/编辑专家对话框 -->
    <el-dialog 
      v-model="formDialogVisible" 
      :title="dialogTitle" 
      width="50%" 
      :before-close="closeFormDialog"
    >
      <el-form 
        :model="expertForm" 
        :rules="expertRules" 
        ref="expertFormRef" 
        label-width="100px"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="expertForm.name" placeholder="请输入专家姓名" />
        </el-form-item>
        <el-form-item label="领域">
          <el-input v-model="expertForm.field" placeholder="请输入专家领域" />
        </el-form-item>
        <el-form-item label="专长">
          <el-input v-model="expertForm.expertise" placeholder="请输入专家专长" />
        </el-form-item>
        <el-form-item label="成果">
          <el-input 
            v-model="expertForm.achievements" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入专家成果"
          />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="expertForm.contactInfo" placeholder="请输入联系方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeFormDialog">取消</el-button>
          <el-button type="primary" @click="saveExpert">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { expertApi } from '@/api'
import type { Expert } from '@/types'

// 搜索表单
const searchForm = reactive({
  query: ''
})

// 专家列表相关
const expertList = ref<Expert[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情对话框相关
const detailDialogVisible = ref(false)
const dialogTitle = ref('')
const currentExpert = ref<Expert>({} as Expert)

// 表单对话框相关
const formDialogVisible = ref(false)
const expertForm = reactive<Omit<Expert, 'id'>>({
  name: '',
  field: '',
  expertise: '',
  achievements: '',
  contactInfo: ''
})
const expertFormRef = ref()

// 表单验证规则
const expertRules = {
  name: [
    { required: true, message: '请输入专家姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度应在2-20个字符之间', trigger: 'blur' }
  ]
}

// 搜索专家
const searchExperts = async () => {
  loading.value = true
  try {
    const params = {
      query: searchForm.query
    }
    
    const result = await expertApi.getExperts(params)
    expertList.value = result
    total.value = result.length // 这里简单处理总数，实际应从后端返回
  } catch (error: any) {
    ElMessage.error(error.message || '获取专家列表失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.query = ''
  expertList.value = []
}

// 查看专家详情
const viewExpert = async (row: Expert) => {
  try {
    // 这里先直接使用row的数据，也可以从后端获取详细信息
    currentExpert.value = row
    dialogTitle.value = '专家详情'
    detailDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取专家详情失败')
  }
}

// 编辑专家
const editExpert = (row: Expert) => {
  // 复制数据到表单
  Object.assign(expertForm, row)
  dialogTitle.value = '编辑专家'
  formDialogVisible.value = true
}

// 删除专家
const deleteExpert = (row: Expert) => {
  ElMessageBox.confirm(
    `确定要删除专家 "${row.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 这里需要后端提供删除接口
      // await expertApi.deleteExpert(row.id)
      ElMessage.success('删除成功')
      searchExperts() // 重新搜索
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
  Object.keys(expertForm).forEach(key => {
    (expertForm as any)[key] = ''
  })
  dialogTitle.value = '新增专家'
  formDialogVisible.value = true
}

// 保存专家
const saveExpert = async () => {
  if (!expertFormRef.value) return
  
  try {
    await expertFormRef.value.validate()
    
    // 区分新增和编辑
    if (currentExpert.value.id) {
      // 更新专家（假设后端有更新接口）
      // await expertApi.updateExpert(currentExpert.value.id, expertForm)
    } else {
      // 创建专家
      await expertApi.createExpert(expertForm)
    }
    
    ElMessage.success('保存成功')
    closeFormDialog()
    searchExperts() // 重新搜索
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
  currentExpert.value = {} as Expert
}

// 关闭表单对话框
const closeFormDialog = () => {
  formDialogVisible.value = false
  if (expertFormRef.value) {
    expertFormRef.value.resetFields()
  }
  currentExpert.value = {} as Expert
}

// 分页相关方法
const handleSizeChange = (val: number) => {
  pageSize.value = val
  searchExperts()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  searchExperts()
}

// 初始化数据
onMounted(() => {
  // 初始化加载专家列表
  searchExperts()
})
</script>

<style scoped>
.expert-manage-container {
  padding: 20px;
}

.search-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.expert-list {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}
</style>