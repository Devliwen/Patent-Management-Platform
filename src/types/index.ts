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

// AI聊天相关类型
export interface AiChatRequest {
  question: string                    // 问题
  sessionId?: number                 // 会话ID（可选，用于继续对话）
}

export interface AiChatResponse {
  answer: string                     // AI回答
  model: string                      // 使用的模型
  requestId: string                  // 请求ID
  sessionId: number                  // 会话ID
  messageId: number                  // 消息ID
}

// 聊天会话类型（对应后端ChatSession实体）
export interface ChatSession {
  id: number                         // 会话ID
  title: string                      // 会话标题
  createdAt: string                  // 创建时间（ISO格式）
  updatedAt: string                  // 更新时间（ISO格式）
}

// 聊天消息类型（对应后端ChatMessage实体）
export interface ChatMessage {
  id: number                         // 消息ID
  sessionId: number                  // 会话ID
  userId: number                     // 用户ID
  role: 'user' | 'assistant' | 'system' // 消息角色
  content: string                     // 消息内容
  createdAt: string                  // 创建时间（ISO格式）
}

// 后端返回的聊天消息响应类型（对应后端ChatMessageResponse）
export interface ChatMessageResponse {
  id: number                         // 消息ID
  role: 'user' | 'assistant' | 'system' // 角色
  content: string                     // 消息内容
  createdAt: string                  // 创建时间（ISO格式）
}

// 前端使用的简化消息类型（用于界面显示）
export interface UIMessage {
  id: string                         // 前端生成的临时ID
  role: 'user' | 'assistant' | 'system' // 消息角色
  content: string                    // 消息内容
  timestamp: number                  // 时间戳
  backendId?: number                 // 后端消息ID（保存后更新）
}

// 前端使用的简化会话类型（用于界面显示）
export interface UISession {
  id: string                         // 前端生成的临时ID
  title: string                      // 会话标题
  messages: UIMessage[]              // 消息列表
  createdAt: number                 // 创建时间戳
  updatedAt: number                  // 更新时间戳
  backendId?: number                 // 后端会话ID（保存后更新）
}

// 创建会话请求类型（对应后端ChatCreateSessionRequest）
export interface ChatCreateSessionRequest {
  title?: string                     // 会话标题（可选）
}

// 创建会话响应类型（对应后端ChatSessionResponse）
export interface ChatSessionResponse {
  id: number                         // 会话ID
  title: string                      // 会话标题
  createdAt: string                  // 创建时间（ISO格式）
  updatedAt: string                  // 更新时间（ISO格式）
}

// 专利基础信息类型
export interface PatentBase {
  publicNum: string           // 公开号（主键）
  legalStatus?: string        // 法律状态
  latestLegalStatus?: string  // 最新法律状态
  status?: string             // 状态
  title: string               // 标题
  type?: string               // 类型
  abstractText: string        // 摘要
  appliNum?: string           // 申请号
  appliDate?: string          // 申请日期
  publicDate?: string         // 公开日期
  applicant: string           // 申请人
  applicantAddress?: string   // 申请人地址
  patentee?: string           // 专利权人
  patenteeAddress?: string    // 专利权人地址
  inventor: string            // 发明人
  agent?: string              // 代理人
  ipc: string                 // 国际专利分类
  cpc?: string                // 共同专利分类
  nec?: string                // 国家/地区代码
  patentDetails?: string      // 专利详情
}

// 用户个人专利信息类型（包含类别信息）
export interface UserPatent extends PatentBase {
  category?: string           // 专利类别
}

// 专利类别枚举
export type PatentCategory = 'wind' | 'solar' | 'biomass' | 'hydrogen' | 'lilon'

// 专利查询参数
export interface PatentQueryParams {
  category?: string  // 后端接口中category是可选参数
  query?: string
  page?: number
  size?: number
}

// 专利创建/更新参数
export interface PatentCreateUpdateParams extends PatentBase {
  category: PatentCategory
}

// 专家信息类型
export interface Expert {
  id: number          // 专家ID
  name: string        // 姓名
  field: string       // 领域
  expertise: string   // 专长
  achievements?: string // 成果
  contactInfo?: string  // 联系方式
}

// 专家查询参数
export interface ExpertQueryParams {
  query?: string
}

// 需求信息类型
export interface Requirement {
  id: number
  title: string
  description?: string
  keywords?: string
  techDirection?: string
  cooperationMode?: string
  status: string
  createdDate: string
}

// 创建需求参数
export interface CreateRequirementParams {
  title: string
  description?: string
  keywords?: string
  techDirection?: string
  cooperationMode?: string
}

// 匹配专利结果类型
export interface MatchedPatent {
  category: PatentCategory
  publicNum: string
  title: string
  applicant: string
  inventor: string
}

// 匹配专家结果类型
export interface MatchedExpert {
  id: number
  name: string
  field: string
}

// 专利匹配持久化参数
export interface PersistPatentMatchParams {
  items: Array<{
    patentCategory: PatentCategory
    patentPublicNum: string
    matchScore?: number
    matchReason?: string
  }>
}

// 分页对象类型
export interface Pageable<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
  pageable?: {
    pageNumber: number
    pageSize: number
    offset: number
    paged: boolean
    unpaged: boolean
  }
}

// 专家匹配持久化参数
export interface PersistExpertMatchParams {
  items: Array<{
    expertId: number
    matchScore?: number
    matchReason?: string
  }>
}

// 转化成果信息类型
export interface TransformationResult {
  id: number
  patentCategory: PatentCategory
  patentPublicNum: string
  expertId?: number
  requirementId?: number
  partnerOrgId?: number
  description?: string
  transformationDate?: string
  status: string
  benefitAmount?: number
}

// 创建转化成果参数
export interface CreateTransformationParams {
  patentCategory: PatentCategory
  patentPublicNum: string
  expertId?: number
  requirementId?: number
  partnerOrgId?: number
  description?: string
  transformationDate?: string
  status?: string
  benefitAmount?: number
}

// 专利价值评估报告类型
export interface ValuationReport {
  id: number
  patentCategory: PatentCategory
  patentPublicNum: string
  reportTitle: string
  valuationAmount: number
  currency: string
  valuationDate: string
  modelVersion: string
  scoreDetails?: {
    technologicalInnovation: number
    marketPotential: number
    legalStatus: number
    economicValue: number
  }
  evaluationMethod: string
  evaluator: string
  status: string
  reportContent: string
  createdAt: string
  updatedAt?: string
}

// 生成评估报告参数
export interface GenerateValuationParams {
  patentCategory: PatentCategory
  patentPublicNum: string
  modelVersion?: string
}

// 查询评估报告列表参数
export interface QueryValuationParams {
  patentCategory: PatentCategory
  patentPublicNum: string
}

// 更新评估模型参数
export interface UpdateValuationParam {
  key: string
  value: string | number | boolean
  description?: string
}

// 用户账户信息类型
export interface UserAccount {
  id: number
  username: string
  email?: string
  phone?: string
  role: string
  status: string
  createdAt: string
  updatedAt: string
}

// 用户资料类型
export interface UserProfile {
  id: number
  userId: number
  nickname?: string
  avatar?: string
  realName?: string
  gender?: 'male' | 'female' | 'other'
  birthDate?: string
  bio?: string
  address?: string
  website?: string
  socialLinks?: Record<string, string>
  preferences?: Record<string, any>
  updatedAt: string
}

// 专家资料类型
export interface ExpertProfile {
  id: number
  userId: number
  name: string
  field: string
  expertise: string
  achievements?: string
  contactInfo?: string
  status: string
  createdAt: string
  updatedAt: string
}

// 机构信息类型
export interface Organization {
  id: number
  name: string
  type: string
  description?: string
  address?: string
  contactInfo?: string
  status: string
  createdAt: string
  updatedAt: string
}

// 完整用户信息类型
export interface FullUserInfo {
  userAccount: UserAccount
  userProfile: UserProfile
  expertProfile?: ExpertProfile
  mainOrganization?: Organization
}

// 更新用户资料参数
export interface UpdateUserProfileParams {
  nickname?: string
  avatar?: string
  realName?: string
  gender?: 'male' | 'female' | 'other'
  birthDate?: string
  bio?: string
  address?: string
  website?: string
  socialLinks?: Record<string, string>
  preferences?: Record<string, any>
}