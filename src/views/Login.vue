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

// 登录表单数据
const loginForm = reactive<LoginParams>({
  username: '',
  password: ''
})

// 密码强度校验规则
const validatePasswordStrength = (rule: any, value: string, callback: Function) => {
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
  ]
})

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    // 表单验证
    await loginFormRef.value.validate()
    
    // 设置加载状态
    loading.value = true
    
    // 调用登录API
    const response = await authApi.login(loginForm)
    
    // 提取token（处理可能的对象格式）
    let token: string
    if (typeof response === 'string') {
      token = response
    } else if (response && typeof response === 'object' && 'token' in response) {
      // 使用类型断言确保TypeScript知道response有token属性
      token = (response as { token: string }).token
    } else {
      throw new Error('登录返回格式异常')
    }
    
    // 保存token到localStorage
    localStorage.setItem('token', token)
    
    // 调试信息：验证token格式
    console.log('登录成功，存储的token:', {
      type: typeof token,
      length: token.length,
      prefix: token.substring(0, 20) + '...',
      isJWT: token.split('.').length === 3
    })
    
    // 显示登录成功提示
    ElMessage.success('登录成功')
    
    // 跳转到之前的页面（如有）
    const redirect = router.currentRoute.value.query.redirect as string
    router.push(redirect || '/')
  } catch (error: any) {
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



// 重置表单
const resetForm = () => {
  loginFormRef.value?.resetFields()
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}


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