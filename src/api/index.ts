import axios, {
  AxiosInstance,
  AxiosResponse,
  CancelTokenSource,
  isCancel
} from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '../router'
import type { ApiResponse, CreateRequirementParams, CreateTransformationParams, Expert, ExpertQueryParams, FullUserInfo, GenerateValuationParams, MatchedExpert, MatchedPatent, Pageable, PatentBase, PatentCategory, PatentQueryParams, PersistExpertMatchParams, PersistPatentMatchParams, QueryValuationParams, RequestConfig, Requirement, TransformationResult, UpdateUserProfileParams, UpdateValuationParam, UserPatent, UserProfile, ValuationReport, AiChatRequest, AiChatResponse, ChatSession, ChatMessage, ChatCreateSessionRequest, ChatSessionResponse, ChatMessageResponse } from '../types'

// 取消请求token缓存
const cancelTokenMap = new Map<string, CancelTokenSource>()
// 请求缓存
const requestCache = new Map<string, any>()
// 环境变量中的接口地址
const BASE_URL = import.meta.env.VITE_API_BASE_URL

// 响应数据类型已从 ../types 导入

// 免token接口白名单
const NO_TOKEN_WHITELIST = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/captcha'
]

// 检查token是否即将过期（在过期前5分钟提醒）
const isTokenExpiringSoon = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 转换为毫秒
    const now = Date.now()
    const fiveMinutes = 5 * 60 * 1000
    return (exp - now) < fiveMinutes
  } catch (error) {
    console.warn('无法解析token过期时间:', error)
    return false
  }
}

// 检查token是否已过期
const isTokenExpired = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 转换为毫秒
    return Date.now() > exp
  } catch (error) {
    console.warn('无法解析token过期时间:', error)
    return true
  }
}

// token状态缓存，避免频繁提示
let lastTokenState = {
  token: '',
  isExpiringSoon: false,
  isExpired: false,
  lastChecked: 0
}

// 检查token状态，只在状态变化时提示
const checkTokenState = (token: string): { isExpiringSoon: boolean; isExpired: boolean } => {
  const now = Date.now()
  const isExpiringSoon = isTokenExpiringSoon(token)
  const isExpired = isTokenExpired(token)
  
  // 如果token相同且5秒内检查过，直接返回缓存状态
  if (lastTokenState.token === token && 
      now - lastTokenState.lastChecked < 5000) {
    return {
      isExpiringSoon: lastTokenState.isExpiringSoon,
      isExpired: lastTokenState.isExpired
    }
  }
  
  // 更新缓存
  lastTokenState = {
    token,
    isExpiringSoon,
    isExpired,
    lastChecked: now
  }
  
  return { isExpiringSoon, isExpired }
}

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
        const requestKey = ApiService.getRequestKey(typedConfig)
        if (cancelTokenMap.has(requestKey)) {
          cancelTokenMap.get(requestKey)?.cancel('重复请求已取消')
          cancelTokenMap.delete(requestKey)
        }
        // 创建取消token
        const source = axios.CancelToken.source()
        typedConfig.cancelToken = source.token
        cancelTokenMap.set(requestKey, source)

        // 检查是否为免token接口
        const isNoTokenApi = NO_TOKEN_WHITELIST.some(api => config.url?.includes(api))
        
        // 如果是免token接口，直接返回配置，不进行任何token相关处理
        if (isNoTokenApi) {
          return config
        }
        
        // 为AI聊天接口设置更长的超时时间（60秒）
        if (config.url?.includes('/ai/chat')) {
          // 创建新的配置对象，避免被全局配置覆盖
          const newConfig = { ...config }
          newConfig.timeout = 60000 // 60秒
          console.log('AI聊天接口超时设置:', { 
            url: config.url, 
            timeout: newConfig.timeout,
            originalTimeout: config.timeout 
          })
          return newConfig
        }
        
        // 添加token（非免token接口才需要）
        if (!typedConfig.headers) {
          typedConfig.headers = {}
        }
        
        const token = localStorage.getItem('token')
        
        // 关键：只在token是字符串时才设置请求头
        if (token && typeof token === 'string') {
            // 检查token状态（使用缓存机制避免频繁提示）
            const tokenState = checkTokenState(token)
            
            if (tokenState.isExpired) {
              // token已过期，清除token并提示重新登录
              localStorage.removeItem('token')
              if (router.currentRoute.value.path !== '/login') {
                ElMessage.warning('登录已过期，请重新登录')
                router.push('/login')
              }
              return Promise.reject(new Error('Token已过期'))
            } else if (tokenState.isExpiringSoon && 
                      (lastTokenState.token !== token || !lastTokenState.isExpiringSoon)) {
              // token即将过期，且状态发生变化时才提示
              ElMessage.info('登录状态即将过期，建议保存当前工作后重新登录')
            }
            
            typedConfig.headers.Authorization = `Bearer ${token}`
            
            // 调试信息：打印请求头和token信息
            console.log('请求拦截器 - 添加Authorization头:', {
              url: config.url,
              method: config.method,
              authorizationHeader: `Bearer ${token}`,
              tokenLength: token.length,
              tokenPrefix: token.substring(0, 20) + '...',
              isJWT: token.split('.').length === 3
            })
          } else {
            // token无效时，清除并提示
            localStorage.removeItem('token')
            console.warn('token无效，已清除:', {
              tokenType: typeof token,
              tokenValue: token
            })
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
        const requestKey = ApiService.getRequestKey(response.config as RequestConfig)
        cancelTokenMap.delete(requestKey)

        const res = response.data
        // 业务逻辑错误处理
        if (res.code !== 0) {
          // 业务逻辑层面的401错误（后端返回的code为401）
          if (res.code === 401) {
            // 只在用户当前页面需要登录时才提示
            if (router.currentRoute.value.path !== '/login') {
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
            }
            return Promise.reject(new Error('登录状态失效'))
          }
          // 业务逻辑层面的403错误
          if (res.code === 403) {
            ElMessage.error('无权限执行此操作')
            return Promise.reject(new Error('无权限执行此操作'))
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
        const requestKey = ApiService.getRequestKey(error.config as RequestConfig)
        cancelTokenMap.delete(requestKey)

        // 处理取消请求
        if (isCancel(error)) {
          console.log('请求已取消:', error.message)
          return Promise.reject(error)
        }

        // HTTP状态码错误处理
        if (error.response?.status === 500) {
          // 500错误：服务器内部错误，直接拒绝，不重试
          console.error('服务器内部错误 (500):', error)
          ElMessage.error('服务器内部错误，请稍后重试')
          return Promise.reject(error)
        } else if (error.response?.status === 403) {
          // 403错误：无权限访问
          if (router.currentRoute.value.path !== '/login') {
            ElMessage.error('无权限访问，请确认登录状态')
            localStorage.removeItem('token')
            router.push('/login')
          }
          return Promise.reject(error)
        } else if (error.response?.status === 401) {
          // 401错误：登录已过期
          if (router.currentRoute.value.path !== '/login') {
            ElMessage.error('登录已过期，请重新登录')
            localStorage.removeItem('token')
            router.push('/login')
          }
          return Promise.reject(error)
        }

        // 通用网络错误提示（其他错误也不重试）
        console.error('网络错误:', error)
        ElMessage.error('网络异常，请检查网络连接')
        return Promise.reject(error)
      }
    )
  }

  // 生成请求唯一标识（静态方法，可在拦截器中使用）
  private static getRequestKey(config: RequestConfig): string {
    // 安全处理：检查config是否为undefined或null
    if (!config) {
      return 'unknown-request'
    }
    
    const { method, url, params, data } = config
    return [method, url, JSON.stringify(params), JSON.stringify(data)].join('-')
  }

  // 清除请求缓存
  public clearCache(): void {
    requestCache.clear()
  }

  // GET请求
  get<T = any>(url: string, params?: any, config?: RequestConfig): Promise<T> {
    const requestKey = ApiService.getRequestKey({ method: 'get', url, params })
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
  getPatentsByCategory(category?: string, query?: string, page?: number, size?: number) {
    const params: { category?: string; query?: string; page?: number; size?: number } = {}
    if (category) {
      params.category = category
    }
    if (query) {
      params.query = query
    }
    if (page !== undefined) {
      params.page = page
    }
    if (size !== undefined) {
      params.size = size
    }
    return this.get<Pageable<PatentBase>>('/patents', params)
  }

  // 创建专利
  createPatent(category: string, patentData: Omit<PatentBase, 'category'>) {
    return this.post<PatentBase>('/patents', patentData, { params: { category } })
  }

  // 更新专利（如果需要单独接口）
  updatePatent(category: string, publicNum: string, patentData: Omit<PatentBase, 'category' | 'publicNum'>) {
    return this.put<PatentBase>(`/patents/${category}/${publicNum}`, patentData)
  }

  // 获取单条专利
  getPatentByCategoryAndNum(category: PatentCategory, publicNum: string) {
    return this.get<PatentBase>(`/patents/${category}/${publicNum}`)
  }

  // 个人专利管理接口 - 上传个人专利
  createUserPatent(patentData: {
    category: string
    title: string
    publicNum?: string
    abstractText?: string
    ipc?: string
    cpc?: string
    applicant?: string
    inventor?: string
    visibility?: string
  }) {
    return this.post<any>('/api/user-patents', patentData)
  }

  // 个人专利管理接口 - 获取个人专利列表
  getUserPatents(params?: {
    owner?: string
    category?: string
    query?: string
    visibility?: string
    page?: number
    size?: number
  }) {
    return this.get<any>('/api/user-patents', params)
  }

  // 个人专利管理接口 - 获取个人专利详情
  getUserPatentDetail(id: number) {
    return this.get<any>(`/api/user-patents/${id}`)
  }

  // 个人专利管理接口 - 修改个人专利
  updateUserPatent(id: number, patentData: {
    category?: string
    title?: string
    publicNum?: string
    abstractText?: string
    ipc?: string
    cpc?: string
    applicant?: string
    inventor?: string
    visibility?: string
  }) {
    return this.put<any>(`/api/user-patents/${id}`, patentData)
  }

  // 个人专利管理接口 - 删除个人专利
  deleteUserPatent(id: number) {
    return this.delete<any>(`/api/user-patents/${id}`)
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
    console.log('注册请求参数：', data) // 打印参数 
    console.log('请求URL：', `${import.meta.env.VITE_API_BASE_URL}/auth/register`) // 打印完整URL
    return api.post<{ userId: number }>('/auth/register', data).catch(err => { 
      // 打印完整错误信息 
      console.error('注册接口错误详情：', { 
        url: err.config?.url, 
        data: err.config?.data, 
        status: err.response?.status, 
        responseData: err.response?.data, // 后端返回的错误提示（关键！） 
        message: err.message 
      }) 
      throw err // 继续抛出错误，不影响原有逻辑 
    })
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
    return api.getPatentsByCategory(params.category, params.query, params.page, params.size).then(response => {
      console.log('专利API返回数据:', {
        response: response,
        type: typeof response,
        isArray: Array.isArray(response),
        length: Array.isArray(response) ? response.length : 'N/A'
      })
      return response
    }).catch(error => {
      console.error('专利API错误:', error)
      throw error
    })
  },

  // 创建专利
  createPatent: (category: string, patentData: Omit<PatentBase, 'category'>) => {
    return api.createPatent(category, patentData)
  },

  // 更新专利
  updatePatent: (category: string, publicNum: string, patentData: Omit<PatentBase, 'category' | 'publicNum'>) => {
    return api.updatePatent(category, publicNum, patentData)
  },

  // 获取单条专利
  getPatent: (category: PatentCategory, publicNum: string) => {
    return api.getPatentByCategoryAndNum(category, publicNum)
  },

  // 获取用户个人专利列表
  getUserPatents: (params?: { page?: number; size?: number; query?: string }) => {
    return api.get<Pageable<UserPatent>>('/user/patents', params)
  },

  // 删除用户个人专利
  deleteUserPatent: (category: string, publicNum: string) => {
    return api.delete(`/user/patents/${category}/${publicNum}`)
  },

  // 个人专利管理接口 - 上传个人专利
  createUserPatent: (patentData: {
    category: string
    title: string
    publicNum?: string
    abstractText?: string
    ipc?: string
    cpc?: string
    applicant?: string
    inventor?: string
    visibility?: string
  }) => {
    return api.createUserPatent(patentData)
  },

  // 个人专利管理接口 - 获取个人专利列表
  getUserPatentsList: (params?: {
    owner?: string
    category?: string
    query?: string
    visibility?: string
    page?: number
    size?: number
  }) => {
    return api.getUserPatents(params)
  },

  // 个人专利管理接口 - 获取个人专利详情
  getUserPatentDetail: (id: number) => {
    return api.getUserPatentDetail(id)
  },

  // 个人专利管理接口 - 修改个人专利
  updateUserPatent: (id: number, patentData: {
    category?: string
    title?: string
    publicNum?: string
    abstractText?: string
    ipc?: string
    cpc?: string
    applicant?: string
    inventor?: string
    visibility?: string
  }) => {
    return api.updateUserPatent(id, patentData)
  },

  // 个人专利管理接口 - 删除个人专利
  deleteUserPatentById: (id: number) => {
    return api.deleteUserPatent(id)
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

// AI聊天相关方法
export const aiApi = {
  // AI聊天咨询
  chat: (requestData: AiChatRequest) => {
    return api.post<AiChatResponse>('/ai/chat', requestData)
  },
  
  // 获取用户的所有聊天会话（支持分页）
  getSessions: (page: number = 1, size: number = 10) => {
    return api.get<{success: boolean, data: ChatSession[]}>('/chat/sessions', {
      params: { page, size }
    })
  },
  
  // 创建新的聊天会话
  createSession: (title?: string) => {
    const requestData: ChatCreateSessionRequest = {
      title: title || '新对话'
    }
    return api.post<ChatSessionResponse>('/chat/sessions', requestData)
  },
  
  // 删除聊天会话
  deleteSession: (sessionId: number) => {
    return api.delete(`/chat/sessions/${sessionId}`)
  },
  
  // 更新会话标题
  updateSessionTitle: (sessionId: number, title: string) => {
    return api.put<ApiResponse<ChatSessionResponse>>(`/chat/sessions/${sessionId}/title`, { title })
  },
  
  // 获取会话的所有消息
  getMessages: (sessionId: number) => {
    return api.get<ChatMessageResponse[]>(`/chat/sessions/${sessionId}/messages`)
  }
}