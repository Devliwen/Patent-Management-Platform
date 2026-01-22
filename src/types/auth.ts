// 注册请求参数类型
export interface RegisterParams {
  username: string
  password: string
  phone?: string
  email?: string
  nickname?: string
}

// 注册响应数据类型
export interface RegisterResponse {
  userId: number
}

// 登录请求参数类型
export interface LoginParams {
  username: string
  password: string
}

// 登录响应数据类型
export type LoginResponse = string // JWT token

// 用户基本信息类型
export interface UserInfo {
  userId: number
  username: string
  phone?: string
  email?: string
  nickname?: string
  avatarUrl?: string
  realName?: string
  // 可以根据实际需求添加更多字段
}

// API响应通用类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}