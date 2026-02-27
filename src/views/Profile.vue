<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 左侧个人信息卡片 -->
      <el-col :span="8">
        <el-card class="profile-card">
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              action="#"
              :show-file-list="false"
              :http-request="uploadAvatar"
              accept="image/*"
            >
              <img v-if="userInfo?.profile?.avatarUrl" :src="userInfo?.profile?.avatarUrl" class="avatar" />
              <el-avatar v-else :size="100" :style="{ backgroundColor: '#409eff' }" class="avatar-placeholder">
                {{ getUserInitial(userInfo?.profile?.nickname || userInfo?.user?.username || '用户') }}
              </el-avatar>
            </el-upload>
            <div class="user-info-basic">
            <h3>{{ userInfo?.profile?.nickname || userInfo?.user?.username || '未登录用户' }}</h3>
            <p class="username">@{{ userInfo?.user?.username || 'unknown' }}</p>
          </div>
        </div>
        </el-card>
      </el-col>

      <!-- 右侧详细信息和编辑面板 -->
      <el-col :span="16">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>个人资料</span>
              <el-button type="primary" @click="toggleEdit" size="small">
                {{ isEditing ? '取消编辑' : '编辑资料' }}
              </el-button>
            </div>
          </template>

          <el-form 
            :model="editForm" 
            :rules="editRules" 
            ref="editFormRef" 
            label-width="100px"
            v-loading="loading"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="editForm.username" disabled />
            </el-form-item>
            
            <el-form-item label="电话" prop="phone">
              <el-input v-model="editForm.phone" :disabled="!isEditing" placeholder="请输入电话" />
            </el-form-item>
            
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="editForm.email" :disabled="!isEditing" placeholder="请输入邮箱" />
            </el-form-item>
            

            
            <div class="form-actions" v-if="isEditing">
              <el-button @click="resetForm">重置</el-button>
              <el-button type="primary" @click="submitForm">保存更改</el-button>
            </div>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { profileApi } from '@/api'
import type { FullUserInfo, UpdateUserProfileParams } from '@/types'
import { 
  User, Message, Phone, Female, Male, Calendar, Document, InfoFilled
} from '@element-plus/icons-vue'

// 用户信息
const userInfo = ref<FullUserInfo | null>(null)
const loading = ref(false)
const isEditing = ref(false)
const activeTab = ref('basic')

// 编辑表单
const editForm = reactive<UpdateUserProfileParams & { username?: string; phone?: string; email?: string }>({
  nickname: '',
  avatarUrl: null,
  realName: null,
  idNumber: null,
  username: '',
  phone: '',
  email: ''
})

const editFormRef = ref()

// 表单验证规则
const editRules = {
  email: [
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

// 获取用户初始字母
const getUserInitial = (name: string) => {
  return name.charAt(0).toUpperCase()
}

// 获取性别文本
const getGenderText = (gender: string) => {
  if (!gender) return '未设置'
  const genderMap: Record<string, string> = {
    'male': '男',
    'female': '女',
    'other': '其他'
  }
  return genderMap[gender] || gender
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '未设置'
  try {
    return new Date(dateString).toLocaleDateString('zh-CN')
  } catch {
    return '日期格式错误'
  }
}

// 切换编辑状态
const toggleEdit = () => {
  if (isEditing.value) {
    // 取消编辑，恢复原始数据
    loadUserInfo()
    isEditing.value = false
  } else {
    isEditing.value = true
  }
}

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const data = await profileApi.getCurrentUserInfo()
    userInfo.value = data
    
    // 填充表单
      if (data) {
        // 用户基本信息
        // 类型断言：虽然 UserAccount 定义中可能没有 nickname，但实际接口可能返回
        const user = data.user as any
        editForm.nickname = user?.nickname || data.profile?.nickname || ''
        editForm.username = data.user?.username || ''
        editForm.phone = data.user?.phone || ''
        editForm.email = data.user?.email || ''
      
      // 用户档案信息
      editForm.avatarUrl = data.profile?.avatarUrl || null
      editForm.realName = data.profile?.realName || ''
      editForm.idNumber = data.profile?.idNumber || ''
    }
    

  } catch (error: any) {
    ElMessage.error(error.message || '获取用户信息失败')
    // 接口失败时，userInfo保持为null，模板会显示默认值
  } finally {
    loading.value = false
  }
}

// 上传头像
const uploadAvatar = (options: any) => {
  // 这里实现头像上传逻辑
  console.log('上传头像:', options.file)
  ElMessage.success('头像上传功能将在后续实现')
}

// 提交表单
const submitForm = async () => {
  if (!editFormRef.value) return
  
  try {
    await editFormRef.value.validate()
    
    // 提取需要更新的字段
    const updateData: UpdateUserProfileParams = {
      nickname: editForm.nickname,
      avatarUrl: editForm.avatarUrl,
      realName: editForm.realName,
      idNumber: editForm.idNumber
    }
    
    // 过滤掉空值
    Object.keys(updateData).forEach(key => {
      const k = key as keyof UpdateUserProfileParams
      if (updateData[k] === undefined || updateData[k] === null || updateData[k] === '') {
        delete updateData[k]
      }
    })
    
    await profileApi.updateUserProfile(updateData)
    ElMessage.success('资料更新成功')
    isEditing.value = false
    loadUserInfo() // 重新加载数据
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error(error.message || '更新资料失败')
    }
  }
}

// 重置表单
const resetForm = () => {
  loadUserInfo()
}

// 初始化数据
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  background-color: var(--bg-secondary);
  min-height: calc(100vh - 64px);
}

.profile-card {
  text-align: center;
}

.avatar-section {
  padding: 20px 0;
}

.avatar-uploader {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.user-info-basic h3 {
  margin: 10px 0 5px 0;
  font-size: 18px;
  color: var(--text-primary);
}

.username {
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.user-details {
  text-align: left;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px;
  border-radius: 4px;
  background-color: var(--bg-primary);
}

.detail-item .el-icon {
  margin-right: 10px;
  color: var(--primary-color);
}

.info-card {
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-tabs {
  margin-top: 20px;
}

.form-actions {
  text-align: right;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color-light);
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-primary);
}
</style>