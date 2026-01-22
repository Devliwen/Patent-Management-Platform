<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">高校知识产权运营服务平台</h1>
        <p class="auth-subtitle">登录您的账号</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名(3-20个字符)"
            prefix-icon="User"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码(6-20个字符，建议包含字母+数字)"
            prefix-icon="Lock"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="验证码" prop="captcha" v-if="showCaptcha">
          <div class="captcha-wrapper">
            <el-input
              v-model="loginForm.captcha"
              placeholder="请输入验证码"
              prefix-icon="VerificationCode"
              size="large"
              @keyup.enter="handleLogin"
            />
            <el-button 
              type="default" 
              class="captcha-btn"
              :disabled="captchaCountdown > 0"
              @click="getCaptcha"
            >
              {{ captchaCountdown > 0 ? `${captchaCountdown}s后重新获取` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            size="large"
            class="login-button"
          >
            登录
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

        <div class="login-footer">
          <el-link type="primary" @click="goToRegister">
            没有账号？立即注册
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import type { LoginParams } from '../types/auth'

const router = useRouter()
const loginFormRef = ref()
const loading = ref(false)
const showCaptcha = ref(false) // 登录失败后显示验证码
const captchaCountdown = ref(0)
let countdownTimer: number | null = null

// 登录表单数据
const loginForm = reactive<LoginParams & { captcha: string }>({
  username: '',
  password: '',
  captcha: ''
})

// 密码强度校验规则
const validatePasswordStrength = (value: string, callback: Function) => {
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

// 登录表单验证规则
const loginRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ]
})

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    // 表单验证（验证码按需验证）
    const validateFields = showCaptcha ? ['username', 'password', 'captcha'] : ['username', 'password']
    await loginFormRef.value.validateField(validateFields)
    
    // 设置加载状态
    loading.value = true
    
    // 调用登录API
    const token = await authApi.login(loginForm)
    
    // 保存token到localStorage
    localStorage.setItem('token', token)
    
    // 显示登录成功提示
    ElMessage.success('登录成功')
    
    // 跳转到之前的页面（如有）
    const redirect = router.currentRoute.value.query.redirect as string
    router.push(redirect || '/')
  } catch (error: any) {
    // 登录失败显示验证码
    showCaptcha.value = true
    // 处理错误
    if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('登录失败，请检查用户名和密码')
    }
  } finally {
    // 关闭加载状态
    loading.value = false
  }
}

// 获取验证码
const getCaptcha = async () => {
  if (!loginForm.username) {
    ElMessage.warning('请先输入用户名')
    return
  }
  
  try {
    // 模拟获取手机号（实际项目中需绑定手机号）
    const phone = '13800138000' // 示例，实际需从用户信息获取
    await authApi.getCaptcha(phone)
    
    // 开始倒计时
    captchaCountdown.value = 60
    countdownTimer = setInterval(() => {
      captchaCountdown.value--
      if (captchaCountdown.value <= 0) {
        clearInterval(countdownTimer!)
        countdownTimer = null
      }
    }, 1000)
    
    ElMessage.success('验证码已发送，请注意查收')
  } catch (error: any) {
    ElMessage.error(error.message || '获取验证码失败')
  }
}

// 重置表单
const resetForm = () => {
  loginFormRef.value?.resetFields()
  loginForm.captcha = ''
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}

// 页面卸载时清除倒计时
onMounted(() => {
  return () => {
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
  }
})
</script>

<style scoped>
.login-form {
  width: 100%;
}

.login-button {
  width: 70%;
  margin-right: 8px;
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

.reset-button {
  width: calc(30% - 8px);
}

.login-button:hover {
  background-color: var(--primary-hover);
  border-color: var(--primary-hover);
}

.login-button:active {
  background-color: var(--primary-active);
  border-color: var(--primary-active);
}

.login-footer {
  text-align: center;
  margin-top: var(--spacing-lg);
}

.captcha-wrapper {
  display: flex;
  gap: 8px;
}

.captcha-btn {
  flex-shrink: 0;
  width: 120px;
}
</style>