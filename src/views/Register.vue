<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">高校知识产权运营服务平台</h1>
        <p class="auth-subtitle">创建您的账号</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        class="register-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名(3-20个字符，字母/数字/下划线)"
            prefix-icon="User"
            size="large"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码(6-20个字符，需包含字母+数字)"
            prefix-icon="Lock"
            show-password
            size="large"
            @keyup.enter="handleRegister"
          />
          <div class="password-strength" v-if="registerForm.password">
            <div class="strength-bar" :style="{ width: strengthWidth + '%' }"></div>
            <span class="strength-text">{{ strengthText }}</span>
          </div>
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            show-password
            size="large"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号（选填）"
            prefix-icon="Cellphone"
            size="large"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱（选填）"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称（选填）"
            prefix-icon="Avatar"
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleRegister"
            size="large"
            class="register-button"
          >
            注册
          </el-button>
          <el-button
            type="default"
            size="large"
            class="reset-button"
            @click="resetForm"
          >
            重置
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <el-link type="primary" @click="goToLogin">
            已有账号？立即登录
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import type { RegisterParams } from '../types/auth'

const router = useRouter()
const registerFormRef = ref()
const loading = ref(false)
const strengthWidth = ref(0)
const strengthText = ref('')

// 注册表单数据
const registerForm = reactive<RegisterParams & { 
  confirmPassword: string
}>({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  nickname: ''
})

// 密码强度校验
const checkPasswordStrength = (password: string) => {
  let score = 0
  // 长度
  if (password.length >= 8) score += 25
  else if (password.length >= 6) score += 10
  
  // 包含字母
  if (/[a-zA-Z]/.test(password)) score += 25
  
  // 包含数字
  if (/\d/.test(password)) score += 25
  
  // 包含特殊字符
  if (/[^a-zA-Z0-9]/.test(password)) score += 25
  
  // 更新强度显示
  strengthWidth.value = score
  if (score < 50) {
    strengthText.value = '弱'
  } else if (score < 80) {
    strengthText.value = '中'
  } else {
    strengthText.value = '强'
  }
}

// 监听密码变化
watch(() => registerForm.password, (val) => {
  if (val) {
    checkPasswordStrength(val)
  } else {
    strengthWidth.value = 0
    strengthText.value = ''
  }
})

// 用户名格式校验
const validateUsername = (rule: any, value: string, callback: any) => {
  const reg = /^[a-zA-Z0-9_]{3,20}$/
  if (value && !reg.test(value)) {
    callback(new Error('用户名仅支持字母、数字、下划线，长度3-20位'))
  } else {
    callback()
  }
}

// 密码强度校验
const validatePasswordStrength = (rule: any, value: string, callback: any) => {
  if (value) {
    const hasLetter = /[a-zA-Z]/.test(value)
    const hasNumber = /\d/.test(value)
    if (!hasLetter || !hasNumber) {
      callback(new Error('密码需同时包含字母和数字'))
    } else {
      callback()
    }
  } else {
    callback()
  }
}

// 注册表单验证规则
const registerRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { 
      validator: (rule: any, value: string, callback: any) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' },
    { required: false }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur', required: false }
  ],
  nickname: [
    { max: 20, message: '昵称长度不超过 20 个字符', trigger: 'blur', required: false }
  ]
})

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    // 表单验证
    await registerFormRef.value.validate()
    
    // 设置加载状态
    loading.value = true
    
    // 准备注册数据（移除confirmPassword字段）
    const { confirmPassword, ...registerData } = registerForm
    
    // 调用注册API
    await authApi.register(registerData)
    
    // 显示注册成功提示
    ElMessage.success('注册成功，请登录')
    
    // 跳转到登录页面
    router.push('/login')
  } catch (error: any) {
    // 处理错误
    if (error && typeof error === 'object' && error.message) {
      ElMessage.error(error.message)
    } else if (typeof error === 'string') {
      ElMessage.error(error)
    } else {
      ElMessage.error('注册失败，请稍后重试')
    }
  } finally {
    // 关闭加载状态
    loading.value = false
  }
}



// 重置表单
const resetForm = () => {
  registerFormRef.value?.resetFields()
  strengthWidth.value = 0
  strengthText.value = ''
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}


</script>

<style scoped>
.register-form {
  width: 100%;
}

.register-button {
  width: 70%;
  margin-right: 8px;
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

.reset-button {
  width: calc(30% - 8px);
}

.register-button:hover {
  background-color: var(--primary-hover);
  border-color: var(--primary-hover);
}

.register-button:active {
  background-color: var(--primary-active);
  border-color: var(--primary-active);
}

.register-footer {
  text-align: center;
  margin-top: var(--spacing-lg);
}

.password-strength {
  margin-top: 8px;
  height: 20px;
  line-height: 20px;
}

.strength-bar {
  display: inline-block;
  height: 6px;
  border-radius: 3px;
  background: #52c41a;
  vertical-align: middle;
  margin-right: 8px;
  transition: width 0.3s;
}

.strength-text {
  font-size: 12px;
  color: var(--text-secondary);
  vertical-align: middle;
}
</style>