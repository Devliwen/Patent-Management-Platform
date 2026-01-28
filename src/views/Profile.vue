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
              <img v-if="userInfo?.userProfile?.avatar" :src="userInfo?.userProfile?.avatar" class="avatar" />
              <el-avatar v-else :size="100" :style="{ backgroundColor: '#409eff' }" class="avatar-placeholder">
                {{ getUserInitial(userInfo?.userProfile?.nickname || userInfo?.userAccount?.username || '用户') }}
              </el-avatar>
            </el-upload>
            <div class="user-info-basic">
              <h3>{{ userInfo?.userProfile?.nickname || userInfo?.userAccount?.username || '未登录用户' }}</h3>
              <p class="username">@{{ userInfo?.userAccount?.username || 'unknown' }}</p>
              <el-tag type="info" size="small" style="margin-top: 8px;">{{ userInfo?.userAccount?.role || '未知角色' }}</el-tag>
            </div>
          </div>
          
          <el-divider />
          
          <div class="user-details">
            <div class="detail-item">
              <el-icon><User /></el-icon>
              <span>{{ userInfo?.userProfile?.realName || '未设置真实姓名' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Message /></el-icon>
              <span>{{ userInfo?.userAccount?.email || '未设置邮箱' }}</span>
            </div>
            <div class="detail-item">
              <el-icon><Phone /></el-icon>
              <span>{{ userInfo?.userAccount?.phone || '未设置手机号' }}</span>
            </div>
            <div class="detail-item" v-if="userInfo?.userProfile?.gender">
              <el-icon><Female v-if="userInfo?.userProfile?.gender === 'female'" />
                         <Male v-else-if="userInfo?.userProfile?.gender === 'male'" />
                         <User v-else /></el-icon>
              <span>{{ getGenderText(userInfo?.userProfile?.gender || '') }}</span>
            </div>
            <div class="detail-item" v-if="userInfo?.userProfile?.birthDate">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatDate(userInfo?.userProfile?.birthDate || '') }}</span>
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
            <el-tabs v-model="activeTab" class="profile-tabs">
              <el-tab-pane label="基本信息" name="basic">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="用户名" prop="username">
                      <el-input v-model="editForm.username" disabled />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="角色" prop="role">
                      <el-input v-model="editForm.role" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="昵称" prop="nickname">
                      <el-input v-model="editForm.nickname" :disabled="!isEditing" placeholder="请输入昵称" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="真实姓名" prop="realName">
                      <el-input v-model="editForm.realName" :disabled="!isEditing" placeholder="请输入真实姓名" />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="性别" prop="gender">
                      <el-select v-model="editForm.gender" :disabled="!isEditing" placeholder="请选择性别" style="width: 100%">
                        <el-option label="男" value="male" />
                        <el-option label="女" value="female" />
                        <el-option label="其他" value="other" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="生日" prop="birthDate">
                      <el-date-picker
                        v-model="editForm.birthDate"
                        :disabled="!isEditing"
                        type="date"
                        placeholder="选择生日"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-form-item label="个人简介" prop="bio">
                  <el-input 
                    v-model="editForm.bio" 
                    :disabled="!isEditing"
                    type="textarea" 
                    :rows="3" 
                    placeholder="请输入个人简介"
                  />
                </el-form-item>
              </el-tab-pane>
              
              <el-tab-pane label="联系方式" name="contact">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="邮箱" prop="email">
                      <el-input v-model="editForm.email" :disabled="!isEditing" placeholder="请输入邮箱" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="手机号" prop="phone">
                      <el-input v-model="editForm.phone" :disabled="!isEditing" placeholder="请输入手机号" />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-form-item label="地址" prop="address">
                  <el-input 
                    v-model="editForm.address" 
                    :disabled="!isEditing"
                    type="textarea" 
                    :rows="2" 
                    placeholder="请输入地址"
                  />
                </el-form-item>
                
                <el-form-item label="网站" prop="website">
                  <el-input 
                    v-model="editForm.website" 
                    :disabled="!isEditing"
                    placeholder="请输入个人网站或博客地址"
                  />
                </el-form-item>
              </el-tab-pane>
              
              <el-tab-pane label="专家信息" name="expert" v-if="userInfo?.expertProfile">
                <el-alert
                  title="专家资料"
                  type="info"
                  description="以下是您的专家资料信息，如需修改请联系管理员"
                  :closable="false"
                  show-icon
                />
                <div style="margin-top: 20px;">
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="姓名">
                        <el-input v-model="editForm.expertName" disabled />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="领域">
                        <el-input v-model="editForm.expertField" disabled />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  
                  <el-form-item label="专长">
                    <el-input 
                      v-model="editForm.expertExpertise" 
                      type="textarea" 
                      :rows="2" 
                      disabled
                    />
                  </el-form-item>
                  
                  <el-form-item label="成果">
                    <el-input 
                      v-model="editForm.expertAchievements" 
                      type="textarea" 
                      :rows="3" 
                      disabled
                    />
                  </el-form-item>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="机构信息" name="organization" v-if="userInfo?.mainOrganization">
                <el-alert
                  title="主机构信息"
                  type="info"
                  description="以下是您的主机构信息，如需修改请联系管理员"
                  :closable="false"
                  show-icon
                />
                <div style="margin-top: 20px;">
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="机构名称">
                        <el-input v-model="editForm.orgName" disabled />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="机构类型">
                        <el-input v-model="editForm.orgType" disabled />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  
                  <el-form-item label="机构描述">
                    <el-input 
                      v-model="editForm.orgDescription" 
                      type="textarea" 
                      :rows="2" 
                      disabled
                    />
                  </el-form-item>
                  
                  <el-form-item label="联系信息">
                    <el-input 
                      v-model="editForm.orgContactInfo" 
                      type="textarea" 
                      :rows="2" 
                      disabled
                    />
                  </el-form-item>
                </div>
              </el-tab-pane>
            </el-tabs>
            
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
  User, Message, Phone, Female, Male, Calendar
} from '@element-plus/icons-vue'

// 用户信息
const userInfo = ref<FullUserInfo | null>(null)
const loading = ref(false)
const isEditing = ref(false)
const activeTab = ref('basic')

// 编辑表单
const editForm = reactive<UpdateUserProfileParams & { 
  username?: string, 
  role?: string, 
  email?: string, 
  phone?: string,
  expertName?: string,
  expertField?: string,
  expertExpertise?: string,
  expertAchievements?: string,
  orgName?: string,
  orgType?: string,
  orgDescription?: string,
  orgContactInfo?: string
}>({
  nickname: '',
  realName: '',
  gender: undefined,
  birthDate: undefined,
  bio: '',
  address: '',
  website: '',
  socialLinks: {},
  preferences: {}
})

const editFormRef = ref()

// 表单验证规则
const editRules = {
  nickname: [
    { min: 2, max: 20, message: '昵称长度应在2-20个字符之间', trigger: 'blur' }
  ],
  realName: [
    { min: 2, max: 10, message: '真实姓名长度应在2-10个字符之间', trigger: 'blur' }
  ],
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
    
    // 初始化编辑表单
    editForm.nickname = data?.userProfile?.nickname || ''
    editForm.realName = data?.userProfile?.realName || ''
    editForm.gender = data?.userProfile?.gender
    editForm.birthDate = data?.userProfile?.birthDate || undefined
    editForm.bio = data?.userProfile?.bio || ''
    editForm.address = data?.userProfile?.address || ''
    editForm.website = data?.userProfile?.website || ''
    editForm.socialLinks = data?.userProfile?.socialLinks || {}
    editForm.preferences = data?.userProfile?.preferences || {}
    
    // 用户账户信息
    editForm.username = data?.userAccount?.username || ''
    editForm.role = data?.userAccount?.role || ''
    editForm.email = data?.userAccount?.email || ''
    editForm.phone = data?.userAccount?.phone || ''
    
    // 专家信息
    if (data?.expertProfile) {
      editForm.expertName = data.expertProfile.name
      editForm.expertField = data.expertProfile.field
      editForm.expertExpertise = data.expertProfile.expertise
      editForm.expertAchievements = data.expertProfile.achievements || ''
    }
    
    // 机构信息
    if (data?.mainOrganization) {
      editForm.orgName = data.mainOrganization.name
      editForm.orgType = data.mainOrganization.type
      editForm.orgDescription = data.mainOrganization.description || ''
      editForm.orgContactInfo = data.mainOrganization.contactInfo || ''
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
      realName: editForm.realName,
      gender: editForm.gender,
      birthDate: editForm.birthDate,
      bio: editForm.bio,
      address: editForm.address,
      website: editForm.website,
      socialLinks: editForm.socialLinks,
      preferences: editForm.preferences
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