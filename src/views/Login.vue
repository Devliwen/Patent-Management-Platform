<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">高校知识产权运营服务平台</h1>
        <p class="auth-subtitle">登录您的账号</p>
      </div>

      <a-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        layout="vertical"
        class="login-form"
        @submit="handleLogin"
      >
        <a-form-item field="username" label="用户名">
          <a-input
            v-model="loginForm.username"
            placeholder="请输入用户名(3-20个字符)"
            size="large"
            allow-clear
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <icon-user />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item field="password" label="密码">
          <a-input-password
            v-model="loginForm.password"
            placeholder="请输入密码(6-20个字符，建议包含字母+数字)"
            size="large"
            allow-clear
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-space fill style="width: 100%">
            <a-button
              type="primary"
              :loading="loading"
              html-type="submit"
              size="large"
              class="login-button"
              long
            >
              登录
            </a-button>
            <a-button
              size="large"
              class="reset-button"
              @click="resetForm"
            >
              重置
            </a-button>
          </a-space>
        </a-form-item>

        <div class="login-footer">
          <a-link @click="goToRegister">
            没有账号？立即注册
          </a-link>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { IconUser, IconLock } from '@arco-design/web-vue/es/icon'
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
const validatePasswordStrength = (value: string, callback: Function) => {
  if (value) {
    const hasLetter = /[a-zA-Z]/.test(value)
    const hasNumber = /\d/.test(value)
    if (!hasLetter || !hasNumber) {
      callback('密码需同时包含字母和数字')
    } else {
      callback()
    }
  } else {
    callback()
  }
}

// 登录表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { minLength: 3, maxLength: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { minLength: 6, maxLength: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  const errors = await loginFormRef.value?.validate()
  if (errors) return
  
  try {
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
    
    // 显示登录成功提示
    Message.success('登录成功')
    
    // 跳转到之前的页面（如有）
    const redirect = router.currentRoute.value.query.redirect as string
    router.push(redirect || '/')
  } catch (error: any) {
    // 处理错误
    if (error.message) {
      Message.error(error.message)
    } else {
      Message.error('登录失败，请检查用户名和密码')
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
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 480px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 40px;
}

.auth-header {
  text-align: center;
  margin-bottom: 40px;
}

.auth-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-1);
  margin-bottom: 8px;
}

.auth-subtitle {
  color: var(--color-text-3);
  font-size: 14px;
}

.login-form {
  width: 100%;
}

.login-button {
  flex: 2;
}

.reset-button {
  flex: 1;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
}
</style>