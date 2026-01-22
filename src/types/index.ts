// 全局通用类型
import type { ApiResponse } from './auth'
export type { ApiResponse } from './auth'
import type { AxiosRequestConfig } from 'axios'

// 首页模块类型
export interface FeatureItem {
  icon: any
  title: string
  description: string
  to: string
}

export interface AdvantageItem {
  icon: any
  title: string
  description: string
}

export interface CaseItem {
  title: string
  status: string
  description: string
  company: string
  time: string
}

// 扩展Axios请求配置，支持取消请求
export interface RequestConfig extends AxiosRequestConfig {
  AbortController ?: AbortController
  noCache?: boolean // 是否禁用缓存
  retryCount?: number // 重试次数
  noRetry?: boolean // 是否禁用重试
}

// 取消请求token类型
export type AxiosCancelToken = {
  Controller: AbortController
  url: string
}

// 通用响应类型复用
export type BaseApiResponse<T = any> = ApiResponse<T>