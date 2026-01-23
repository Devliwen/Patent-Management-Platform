import axios, {
  AxiosInstance,
  AxiosResponse,
  CancelTokenSource,
  isCancel
} from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '../router'
import type { ApiResponse, CreateRequirementParams, CreateTransformationParams, Expert, ExpertQueryParams, FullUserInfo, GenerateValuationParams, MatchedExpert, MatchedPatent, PatentBase, PatentCategory, PatentCreateUpdateParams, PatentQueryParams, PersistExpertMatchParams, PersistPatentMatchParams, QueryValuationParams, RequestConfig, Requirement, TransformationResult, UpdateUserProfileParams, UpdateValuationParam, UserProfile, ValuationReport } from '../types'

// 取消请求token缓存
const cancelTokenMap = new Map<string, CancelTokenSource>()
// 请求缓存
const requestCache = new Map<string, any>()
// 环境变量中的接口地址
const BASE_URL = import.meta.env.VITE_API_BASE_URL

// 响应数据类型已从 ../types 导入

class ApiService {
  private axiosInstance: AxiosInstance

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: BASE_URL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // 请求拦截器
    this.axiosInstance.interceptors.request.use(
      (config) => {
        const typedConfig = config as RequestConfig
        // 取消重复请求
        const requestKey = this.getRequestKey(typedConfig)
        if (cancelTokenMap.has(requestKey)) {
          cancelTokenMap.get(requestKey)?.cancel('重复请求已取消')
          cancelTokenMap.delete(requestKey)
        }
        // 创建取消token
        const source = axios.CancelToken.source()
        typedConfig.cancelToken = source.token
        cancelTokenMap.set(requestKey, source)

        // 添加token
        const token = localStorage.getItem('token')
        if (token) {
          if (!typedConfig.headers) {
            typedConfig.headers = {}
          }
          typedConfig.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // 响应拦截器
    this.axiosInstance.interceptors.response.use(
      (response: AxiosResponse<ApiResponse>) => {
        // 移除取消token
        const requestKey = this.getRequestKey(response.config as RequestConfig)
        cancelTokenMap.delete(requestKey)

        const res = response.data
        // 业务逻辑错误处理
        if (res.code !== 0) {
          // 401 token失效
          if (res.code === 401) {
            ElMessageBox.confirm(
              '登录状态已失效，请重新登录',
              '提示',
              {
                confirmButtonText: '重新登录',
                cancelButtonText: '取消',
                type: 'warning'
              }
            ).then(() => {
              localStorage.removeItem('token')
              router.push('/login')
            })
            return Promise.reject(new Error('登录状态失效'))
          }
          ElMessage.error(res.message || '请求失败')
          return Promise.reject(new Error(res.message || '请求失败'))
        }
        // 缓存请求结果（如果配置了缓存）
        if (!(response.config as RequestConfig).noCache) {
          requestCache.set(requestKey, res.data)
        }
        return res.data
      },
      async (error) => {
        // 移除取消token
        const requestKey = this.getRequestKey(error.config as RequestConfig)
        cancelTokenMap.delete(requestKey)

        // 处理取消请求
        if (isCancel(error)) {
          console.log('请求已取消:', error.message)
          return Promise.reject(error)
        }

        // 网络错误重试
        const config = error.config as RequestConfig
        config.retryCount = config.retryCount || 0
        const maxRetry = 3
        if (config.retryCount < maxRetry && !config.noRetry) {
          config.retryCount++
          const delay = this.getRetryDelay(config.retryCount)
          await this.sleep(delay)
          return this.axiosInstance(config)
        }

        // 通用网络错误提示
        console.error('网络错误:', error)
        ElMessage.error('网络异常，请检查网络连接')
        return Promise.reject(error)
      }
    )
  }

  // 生成请求唯一标识
  private getRequestKey(config: RequestConfig): string {
    const { method, url, params, data } = config
    return [method, url, JSON.stringify(params), JSON.stringify(data)].join('-')
  }

  // 重试延迟（指数退避）
  private getRetryDelay(retryCount: number): number {
    return Math.pow(2, retryCount) * 100 // 100ms, 200ms, 400ms...
  }

  // 休眠函数
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  // 清除请求缓存
  public clearCache(): void {
    requestCache.clear()
  }

  // GET请求
  get<T = any>(url: string, params?: any, config?: RequestConfig): Promise<T> {
    const requestKey = this.getRequestKey({ method: 'get', url, params })
    // 优先读取缓存
    if (requestCache.has(requestKey) && !(config?.noCache)) {
      return Promise.resolve(requestCache.get(requestKey))
    }
    return this.axiosInstance.get(url, { params, ...config })
  }

  // POST请求
  post<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T> {
    // POST请求默认不缓存
    config = { ...config, noCache: true }
    return this.axiosInstance.post(url, data, config)
  }

  // PUT请求
  put<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T> {
    config = { ...config, noCache: true }
    return this.axiosInstance.put(url, data, config)
  }

  // DELETE请求
  delete<T = any>(url: string, params?: any, config?: RequestConfig): Promise<T> {
    config = { ...config, noCache: true }
    return this.axiosInstance.delete(url, { params, ...config })
  }

  // 查询专利（按类别）
  getPatentsByCategory(category: PatentCategory, query?: string) {
    const params: { category: PatentCategory; query?: string } = { category }
    if (query) {
      params.query = query
    }
    return this.get<PatentBase[]>('/patents', params)
  }

  // 创建/更新专利
  createOrUpdatePatent(params: PatentCreateUpdateParams) {
    const { category, ...data } = params
    return this.post<PatentBase>('/patents', data, { params: { category } })
  }

  // 获取单条专利
  getPatentByCategoryAndNum(category: PatentCategory, publicNum: string) {
    return this.get<PatentBase>(`/patents/${category}/${publicNum}`)
  }

  // 查询专家
  getExperts(query?: string) {
    const params: { query?: string } = {}
    if (query) {
      params.query = query
    }
    return this.get<Expert[]>('/experts', params)
  }

  // 创建专家
  createExpert(expertData: Omit<Expert, 'id'>) {
    return this.post<Expert>('/experts', expertData)
  }

  // 创建需求
  createRequirement(requirementData: CreateRequirementParams) {
    return this.post<Requirement>('/requirements', requirementData)
  }

  // 需求匹配专利
  matchPatentsForRequirement(requirementId: number) {
    return this.get<MatchedPatent[]>(`/requirements/${requirementId}/match-patents`)
  }

  // 需求匹配专家
  matchExpertsForRequirement(requirementId: number) {
    return this.get<MatchedExpert[]>(`/requirements/${requirementId}/match-experts`)
  }

  // 保存专利匹配结果
  persistPatentMatches(requirementId: number, params: PersistPatentMatchParams) {
    return this.post(`/requirements/${requirementId}/match-patents/persist`, params)
  }

  // 保存专家匹配结果
  persistExpertMatches(requirementId: number, params: PersistExpertMatchParams) {
    return this.post(`/requirements/${requirementId}/match-experts/persist`, params)
  }

  // 查询全部转化成果
  getAllTransformations() {
    return this.get<TransformationResult[]>('/transformations')
  }

  // 创建转化成果记录
  createTransformation(transformationData: CreateTransformationParams) {
    return this.post<TransformationResult>('/transformations', transformationData)
  }

  // 生成评估报告
  generateValuationReport(valuationParams: GenerateValuationParams) {
    return this.post<ValuationReport>('/valuations', valuationParams)
  }

  // 查询评估报告列表
  getValuationReports(queryParams: QueryValuationParams) {
    return this.get<ValuationReport[]>('/valuations', queryParams)
  }

  // 更新评估模型参数
  updateValuationParam(key: string, paramData: UpdateValuationParam) {
    return this.put<ApiResponse>(`/valuation-params/${key}`, paramData)
  }

  // 获取当前用户信息
  getCurrentUserInfo() {
    return this.get<FullUserInfo>('/users/me')
  }

  // 更新用户资料
  updateUserProfile(profileData: UpdateUserProfileParams) {
    return this.put<UserProfile>('/users/me/profile', profileData)
  }
}

// 导出API实例
const api = new ApiService()

export default api

// 认证相关API
export const authApi = {
  // 注册
  register(data: { username: string; password: string; phone?: string; email?: string; nickname?: string }) {
    return api.post<{ userId: number }>('/auth/register', data)
  },
  // 登录
  login(data: { username: string; password: string }) {
    return api.post<string>('/auth/login', data)
  },
  // 获取验证码
  getCaptcha(phone: string) {
    return api.post<{ captcha: string }>('/auth/captcha', { phone })
  },
  // 获取当前用户信息
  getCurrentUser() {
    return api.get<any>('/users/me', {}, { noCache: true })
  }
}

// 专利相关方法
export const patentApi = {
  // 查询专利（按类别）
  getPatents: (params: PatentQueryParams) => {
    return api.getPatentsByCategory(params.category, params.query)
  },

  // 创建/更新专利
  createOrUpdatePatent: (params: PatentCreateUpdateParams) => {
    return api.createOrUpdatePatent(params)
  },

  // 获取单条专利
  getPatent: (category: PatentCategory, publicNum: string) => {
    return api.getPatentByCategoryAndNum(category, publicNum)
  }
}
// 专家相关方法
export const expertApi = {
  // 查询专家
  getExperts: (params: ExpertQueryParams) => {
    return api.getExperts(params.query)
  },

  // 创建专家
  createExpert: (expertData: Omit<Expert, 'id'>) => {
    return api.createExpert(expertData)
  }
}

// 需求相关方法
export const requirementApi = {
  // 创建需求
  createRequirement: (requirementData: CreateRequirementParams) => {
    return api.createRequirement(requirementData)
  },

  // 需求匹配专利
  matchPatentsForRequirement: (requirementId: number) => {
    return api.matchPatentsForRequirement(requirementId)
  },

  // 需求匹配专家
  matchExpertsForRequirement: (requirementId: number) => {
    return api.matchExpertsForRequirement(requirementId)
  },

  // 保存专利匹配结果
  persistPatentMatches: (requirementId: number, params: PersistPatentMatchParams) => {
    return api.persistPatentMatches(requirementId, params)
  },

  // 保存专家匹配结果
  persistExpertMatches: (requirementId: number, params: PersistExpertMatchParams) => {
    return api.persistExpertMatches(requirementId, params)
  }
}

// 转化成果相关方法
export const transformationApi = {
  // 查询全部转化成果
  getAllTransformations: () => {
    return api.getAllTransformations()
  },

  // 创建转化成果记录
  createTransformation: (transformationData: CreateTransformationParams) => {
    return api.createTransformation(transformationData)
  }
}

// 价值评估相关方法
export const valuationApi = {
  // 生成评估报告
  generateValuationReport: (valuationParams: GenerateValuationParams) => {
    return api.generateValuationReport(valuationParams)
  },

  // 查询评估报告列表
  getValuationReports: (queryParams: QueryValuationParams) => {
    return api.getValuationReports(queryParams)
  },

  // 更新评估模型参数
  updateValuationParam: (key: string, paramData: UpdateValuationParam) => {
    return api.updateValuationParam(key, paramData)
  }
}

// 用户资料相关方法
export const profileApi = {
  // 获取当前用户信息
  getCurrentUserInfo: () => {
    return api.getCurrentUserInfo()
  },

  // 更新用户资料
  updateUserProfile: (profileData: UpdateUserProfileParams) => {
    return api.updateUserProfile(profileData)
  }
}