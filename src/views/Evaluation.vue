<template>
  <div class="evaluation-container">
    <div class="container">
      <el-card class="evaluation-card">
        <template #header>
          <div class="card-header">
            <span>专利价值评估</span>
          </div>
        </template>
        
        <el-steps :active="activeStep" finish-status="success" align-center class="evaluation-steps">
          <el-step title="选择专利" />
          <el-step title="评估参数" />
          <el-step title="生成报告" />
        </el-steps>
        
        <!-- 步骤1: 选择专利 -->
        <div v-if="activeStep === 0" class="step-content">
          <el-form 
            :model="patentSelectionForm" 
            :rules="patentSelectionRules" 
            ref="patentSelectionFormRef" 
            label-width="120px"
          >
            <el-form-item label="专利类别" prop="patentCategory">
              <el-select 
                v-model="patentSelectionForm.patentCategory" 
                placeholder="请选择专利类别" 
                style="width: 100%"
              >
                <el-option label="风能" value="wind" />
                <el-option label="太阳能" value="solar" />
                <el-option label="生物质能" value="biomass" />
                <el-option label="氢能" value="hydrogen" />
                <el-option label="锂电池" value="lilon" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="专利公开号" prop="patentPublicNum">
              <el-input 
                v-model="patentSelectionForm.patentPublicNum" 
                placeholder="请输入专利公开号" 
                maxlength="50"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="nextStep">下一步</el-button>
              <el-button @click="searchPatent">查询专利信息</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 步骤2: 评估参数 -->
        <div v-if="activeStep === 1" class="step-content">
          <el-form 
            :model="evaluationParamsForm" 
            label-width="120px"
          >
            <el-form-item label="评估模型">
              <el-select 
                v-model="evaluationParamsForm.modelVersion" 
                placeholder="请选择评估模型" 
                style="width: 100%"
              >
                <el-option label="标准模型 v1.0" value="v1.0" />
                <el-option label="增强模型 v2.0" value="v2.0" />
                <el-option label="专业模型 v3.0" value="v3.0" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="评估维度权重">
              <el-row :gutter="20">
                <el-col :span="6">
                  <div class="weight-item">
                    <div class="weight-label">技术创新</div>
                    <el-slider 
                      v-model="evaluationParamsForm.weights.technologicalInnovation" 
                      vertical 
                      height="150px"
                      :format-tooltip="formatWeightTooltip"
                    />
                    <div class="weight-value">{{ evaluationParamsForm.weights.technologicalInnovation }}%</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="weight-item">
                    <div class="weight-label">市场潜力</div>
                    <el-slider 
                      v-model="evaluationParamsForm.weights.marketPotential" 
                      vertical 
                      height="150px"
                      :format-tooltip="formatWeightTooltip"
                    />
                    <div class="weight-value">{{ evaluationParamsForm.weights.marketPotential }}%</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="weight-item">
                    <div class="weight-label">法律状态</div>
                    <el-slider 
                      v-model="evaluationParamsForm.weights.legalStatus" 
                      vertical 
                      height="150px"
                      :format-tooltip="formatWeightTooltip"
                    />
                    <div class="weight-value">{{ evaluationParamsForm.weights.legalStatus }}%</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="weight-item">
                    <div class="weight-label">经济价值</div>
                    <el-slider 
                      v-model="evaluationParamsForm.weights.economicValue" 
                      vertical 
                      height="150px"
                      :format-tooltip="formatWeightTooltip"
                    />
                    <div class="weight-value">{{ evaluationParamsForm.weights.economicValue }}%</div>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
            
            <el-form-item>
              <el-button @click="prevStep">上一步</el-button>
              <el-button type="primary" @click="nextStep">下一步</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 步骤3: 生成报告 -->
        <div v-if="activeStep === 2" class="step-content">
          <div class="confirmation-section">
            <h3>确认评估信息</h3>
            <div class="confirmation-details">
              <p><strong>专利类别:</strong> {{ getCategoryText(patentSelectionForm.patentCategory) }}</p>
              <p><strong>专利公开号:</strong> {{ patentSelectionForm.patentPublicNum }}</p>
              <p><strong>评估模型:</strong> {{ evaluationParamsForm.modelVersion }}</p>
              <p><strong>评估维度权重:</strong></p>
              <ul>
                <li>技术创新: {{ evaluationParamsForm.weights.technologicalInnovation }}%</li>
                <li>市场潜力: {{ evaluationParamsForm.weights.marketPotential }}%</li>
                <li>法律状态: {{ evaluationParamsForm.weights.legalStatus }}%</li>
                <li>经济价值: {{ evaluationParamsForm.weights.economicValue }}%</li>
              </ul>
            </div>
            
            <div class="action-buttons">
              <el-button @click="prevStep">上一步</el-button>
              <el-button type="primary" @click="generateEvaluation" :loading="generating">
                {{ generating ? '评估中...' : '生成评估报告' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 评估结果展示 -->
      <el-card v-if="showResult" class="result-card">
        <template #header>
          <div class="card-header">
            <span>评估结果</span>
          </div>
        </template>
        
        <div class="result-content">
          <div class="result-summary">
            <div class="result-score">
              <div class="score-value">{{ evaluationResult.valuationAmount }}</div>
              <div class="score-label">评估价值 (元)</div>
            </div>
            <div class="result-details">
              <div class="detail-item">
                <div class="detail-label">技术创新得分</div>
                <el-progress 
                  :percentage="evaluationResult.scoreDetails?.technologicalInnovation" 
                  :stroke-width="10"
                />
              </div>
              <div class="detail-item">
                <div class="detail-label">市场潜力得分</div>
                <el-progress 
                  :percentage="evaluationResult.scoreDetails?.marketPotential" 
                  :stroke-width="10"
                />
              </div>
              <div class="detail-item">
                <div class="detail-label">法律状态得分</div>
                <el-progress 
                  :percentage="evaluationResult.scoreDetails?.legalStatus" 
                  :stroke-width="10"
                />
              </div>
              <div class="detail-item">
                <div class="detail-label">经济价值得分</div>
                <el-progress 
                  :percentage="evaluationResult.scoreDetails?.economicValue" 
                  :stroke-width="10"
                />
              </div>
            </div>
          </div>
          
          <div class="result-actions">
            <el-button type="primary" @click="viewFullReport">查看完整报告</el-button>
            <el-button @click="newEvaluation">重新评估</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { valuationApi, patentApi } from '@/api'
import type { GenerateValuationParams, ValuationReport, PatentCategory } from '@/types'

// 当前步骤
const activeStep = ref(0)

// 专利选择表单
const patentSelectionForm = reactive<{
  patentCategory: PatentCategory;
  patentPublicNum: string;
}>({
  patentCategory: '' as PatentCategory,
  patentPublicNum: ''
})

const patentSelectionFormRef = ref()

// 评估参数表单
const evaluationParamsForm = reactive({
  modelVersion: 'v2.0',
  weights: {
    technologicalInnovation: 30,
    marketPotential: 25,
    legalStatus: 20,
    economicValue: 25
  }
})

// 表单验证规则
const patentSelectionRules = {
  patentCategory: [
    { required: true, message: '请选择专利类别', trigger: 'change' }
  ],
  patentPublicNum: [
    { required: true, message: '请输入专利公开号', trigger: 'blur' },
    { min: 5, max: 50, message: '专利公开号长度应在5-50个字符之间', trigger: 'blur' }
  ]
}

// 评估结果
const showResult = ref(false)
const evaluationResult = ref<ValuationReport>({} as ValuationReport)
const generating = ref(false)

// 下一步
const nextStep = async () => {
  if (activeStep.value === 0) {
    // 验证专利选择表单
    if (!patentSelectionFormRef.value) return
    
    try {
      await patentSelectionFormRef.value.validate()
      activeStep.value++
    } catch (error) {
      ElMessage.error('请完善专利信息')
      return
    }
  } else if (activeStep.value === 1) {
    // 验证权重总和是否为100%
    const totalWeight = Object.values(evaluationParamsForm.weights).reduce((sum, w) => sum + w, 0)
    if (totalWeight !== 100) {
      ElMessage.error('权重总和必须为100%')
      return
    }
    activeStep.value++
  } else {
    activeStep.value++
  }
}

// 上一步
const prevStep = () => {
  if (activeStep.value > 0) {
    activeStep.value--
  }
}

// 生成评估
const generateEvaluation = async () => {
  generating.value = true
  try {
    // 确保专利类别不是空字符串
    if (!patentSelectionForm.patentCategory) {
      ElMessage.error('请选择专利类别')
      generating.value = false
      return
    }
    
    const params: GenerateValuationParams = {
      patentCategory: patentSelectionForm.patentCategory as PatentCategory,
      patentPublicNum: patentSelectionForm.patentPublicNum,
      modelVersion: evaluationParamsForm.modelVersion
    }
    
    // 调用后端接口生成评估报告
    const result = await valuationApi.generateValuationReport(params)
    evaluationResult.value = result
    showResult.value = true
    ElMessage.success('评估完成！')
  } catch (error: any) {
    ElMessage.error(error.message || '评估失败')
  } finally {
    generating.value = false
  }
}

// 查询专利信息
const searchPatent = async () => {
  if (!patentSelectionForm.patentCategory || !patentSelectionForm.patentPublicNum) {
    ElMessage.warning('请先选择专利类别和输入专利公开号')
    return
  }
  
  try {
    const patent = await patentApi.getPatent(patentSelectionForm.patentCategory, patentSelectionForm.patentPublicNum)
    ElMessage.success(`找到专利: ${patent.title}`)
  } catch (error: any) {
    ElMessage.error('未找到该专利，请检查专利类别和公开号')
  }
}

// 重新评估
const newEvaluation = () => {
  showResult.value = false
  activeStep.value = 0
}

// 查看完整报告
const viewFullReport = () => {
  ElMessage.info('跳转到完整报告页面')
  // 这里可以跳转到报告详情页面
}

// 格式化权重提示
const formatWeightTooltip = (value: number) => {
  return `${value}%`
}

// 获取专利类别文本
const getCategoryText = (category: '' | 'wind' | 'solar' | 'biomass' | 'hydrogen' | 'lilon') => {
  const categoryMap: Record<string, string> = {
    'wind': '风能',
    'solar': '太阳能',
    'biomass': '生物质能',
    'hydrogen': '氢能',
    'lilon': '锂电池'
  }
  return categoryMap[category] || category
}
</script>

<style scoped>
.evaluation-container {
  padding: 20px 0;
  background-color: var(--bg-secondary);
  min-height: calc(100vh - 64px);
}

.evaluation-card {
  max-width: 1000px;
  margin: 0 auto 20px;
}

.result-card {
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
}

.evaluation-steps {
  margin: 20px 0 40px;
}

.step-content {
  padding: 20px 0;
}

.weight-item {
  text-align: center;
}

.weight-label {
  font-size: 14px;
  margin-bottom: 10px;
  color: var(--text-secondary);
}

.weight-value {
  font-weight: 600;
  margin-top: 10px;
}

.confirmation-section {
  text-align: center;
}

.confirmation-details {
  text-align: left;
  background: var(--bg-primary);
  padding: 20px;
  border-radius: 8px;
  margin: 20px 0;
}

.action-buttons {
  margin-top: 30px;
}

.result-content {
  text-align: center;
}

.result-summary {
  margin-bottom: 30px;
}

.result-score {
  margin-bottom: 30px;
}

.score-value {
  font-size: 48px;
  font-weight: bold;
  color: var(--el-color-primary);
  margin-bottom: 10px;
}

.score-label {
  font-size: 16px;
  color: var(--text-secondary);
}

.result-details {
  text-align: left;
  max-width: 600px;
  margin: 0 auto;
}

.detail-item {
  margin-bottom: 20px;
}

.detail-label {
  margin-bottom: 8px;
  font-weight: 600;
}

.result-actions {
  margin-top: 30px;
}
</style>