<template>
  <div class="demand-publish-container">
    <div class="container">
      <el-card class="publish-card">
        <template #header>
          <div class="card-header">
            <span>发布技术需求</span>
          </div>
        </template>
        
        <el-form 
          :model="demandForm"
          :rules="demandRules"
          ref="demandFormRef"
          label-width="120px"
          class="demand-form"
        >
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="需求标题" prop="title">
                <el-input 
                  v-model="demandForm.title" 
                  placeholder="请输入需求标题"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="技术方向" prop="techDirection">
                <el-select 
                  v-model="demandForm.techDirection" 
                  placeholder="请选择技术方向" 
                  style="width: 100%"
                >
                  <el-option label="人工智能" value="人工智能" />
                  <el-option label="新能源" value="新能源" />
                  <el-option label="生物医药" value="生物医药" />
                  <el-option label="新材料" value="新材料" />
                  <el-option label="智能制造" value="智能制造" />
                  <el-option label="信息技术" value="信息技术" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="合作模式" prop="cooperationMode">
            <el-radio-group v-model="demandForm.cooperationMode">
              <el-radio label="技术转让">技术转让</el-radio>
              <el-radio label="合作开发">合作开发</el-radio>
              <el-radio label="技术服务">技术服务</el-radio>
              <el-radio label="技术咨询">技术咨询</el-radio>
              <el-radio label="其他">其他</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="关键词" prop="keywords">
            <el-input 
              v-model="demandForm.keywords" 
              type="textarea" 
              :rows="3" 
              placeholder="请输入关键词，多个关键词用逗号分隔，用于智能匹配专家和专利"
            />
          </el-form-item>
          
          <el-form-item label="需求详情" prop="description">
            <editor-content 
              :editor="editor" 
              class="editor-content border rounded p-3" 
              style="min-height: 200px; width: 100%; border: 1px solid #dcdfe6;" 
            />
          </el-form-item>
          
          <el-form-item label="预算范围">
            <el-slider
              v-model="demandForm.budgetRange"
              :min="0"
              :max="1000"
              :step="50"
              show-stops
              show-input
              input-size="small"
              style="width: 50%;"
            />
            <div class="budget-text">{{ getBudgetText(demandForm.budgetRange) }}</div>
          </el-form-item>
          
          <el-form-item label="期望完成时间">
            <el-date-picker
              v-model="demandForm.expectedCompletionTime"
              type="date"
              placeholder="选择期望完成时间"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 300px;"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitDemand" :loading="submitting">
              {{ submitting ? '发布中...' : '发布需求' }}
            </el-button>
            <el-button @click="resetForm">重置</el-button>
            <el-button @click="previewDemand">预览</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      
      <!-- 预览弹窗 -->
      <el-dialog v-model="previewVisible" title="需求预览" width="70%">
        <div class="preview-content">
          <h3>{{ demandForm.title }}</h3>
          <div class="preview-item">
            <label>技术方向:</label>
            <span>{{ demandForm.techDirection }}</span>
          </div>
          <div class="preview-item">
            <label>合作模式:</label>
            <span>{{ demandForm.cooperationMode }}</span>
          </div>
          <div class="preview-item">
            <label>关键词:</label>
            <span>{{ demandForm.keywords }}</span>
          </div>
          <div class="preview-item">
            <label>预算范围:</label>
            <span>{{ getBudgetText(demandForm.budgetRange) }}</span>
          </div>
          <div class="preview-item">
            <label>期望完成时间:</label>
            <span>{{ demandForm.expectedCompletionTime || '未设定' }}</span>
          </div>
          <div class="preview-item">
            <label>需求详情:</label>
            <div class="preview-detail" v-html="sanitizedDescription"></div>
          </div>
        </div>
        <template #footer>
          <el-button @click="previewVisible = false">关闭</el-button>
          <el-button type="primary" @click="confirmSubmit">确认发布</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { requirementApi } from '@/api'
import type { CreateRequirementParams } from '@/types'
import DOMPurify from 'dompurify'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'

// 需求表单
const demandForm = reactive<CreateRequirementParams & { budgetRange: number, expectedCompletionTime?: string }>({
  title: '',
  description: '',
  keywords: '',
  techDirection: '',
  cooperationMode: '技术转让',
  budgetRange: 0,
  expectedCompletionTime: ''
})

const demandFormRef = ref()
const submitting = ref(false)
const previewVisible = ref(false)
const editor = ref<Editor>()

// 安全渲染的计算属性
const sanitizedDescription = computed(() => {
  return DOMPurify.sanitize(demandForm.description || '')
})

// 初始化编辑器
onMounted(() => {
  editor.value = new Editor({
    content: demandForm.description || '',
    extensions: [
      StarterKit,
      Placeholder.configure({
        placeholder: '请详细描述您的技术需求...',
      }),
    ],
    onUpdate: ({ editor }) => {
      // 同步编辑器内容到表单数据
      demandForm.description = editor.getHTML()
    },
  })
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})

// 表单验证规则
const demandRules = {
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度应在5-100个字符之间', trigger: 'blur' }
  ],
  techDirection: [
    { required: true, message: '请选择技术方向', trigger: 'change' }
  ],
  cooperationMode: [
    { required: true, message: '请选择合作模式', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入需求详情', trigger: 'blur' }
  ]
}

// 预览表单
const previewForm = reactive<CreateRequirementParams & { budgetRange: number, expectedCompletionTime?: string }>({...demandForm})

// 提交需求
const submitDemand = async () => {
  if (!demandFormRef.value) return
  
  try {
    await demandFormRef.value.validate()
    
    submitting.value = true
    
    // 调用后端接口发布需求
    await requirementApi.createRequirement({
      title: demandForm.title,
      description: demandForm.description,
      keywords: demandForm.keywords,
      techDirection: demandForm.techDirection,
      cooperationMode: demandForm.cooperationMode
    })
    
    ElMessage.success('需求发布成功！')
    resetForm()
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error(error.message || '发布需求失败')
    }
  } finally {
    submitting.value = false
  }
}

// 预览需求
const previewDemand = async () => {
  if (!demandFormRef.value) return
  
  try {
    await demandFormRef.value.validate()
    
    // 复制当前表单数据到预览表单
    Object.assign(previewForm, demandForm)
    previewVisible.value = true
  } catch (error: any) {
    if (error.message === 'error fields') {
      // 验证错误，已在验证器中提示
    } else {
      ElMessage.error('请完善必填信息')
    }
  }
}

// 确认提交
const confirmSubmit = () => {
  previewVisible.value = false
  submitDemand()
}

// 重置表单
const resetForm = () => {
  demandForm.title = ''
  demandForm.description = ''
  demandForm.keywords = ''
  demandForm.techDirection = ''
  demandForm.cooperationMode = '技术转让'
  demandForm.budgetRange = 0
  demandForm.expectedCompletionTime = ''
  
  if (demandFormRef.value) {
    demandFormRef.value.clearValidate()
  }
}

// 获取预算文本
const getBudgetText = (range: number) => {
  if (range === 0) return '面议'
  if (range <= 100) return `${range}万元以下`
  if (range <= 500) return `${range}万元`
  return `${range}万元以上`
}

</script>

<style scoped>
.editor-content {
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  padding: 12px;
  min-height: 200px;
  background-color: var(--el-fill-color-blank);
}

.editor-content:focus {
  outline: none;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-3) inset;
}

.demand-publish-container {
  padding: 20px 0;
  background-color: var(--bg-secondary);
  min-height: calc(100vh - 64px);
}

.publish-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
}

.demand-form {
  max-width: 800px;
  margin: 0 auto;
}

.budget-text {
  margin-top: 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.editor-content {
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  padding: 12px;
  min-height: 200px;
  background-color: var(--el-fill-color-blank);
}

.editor-content:focus {
  outline: none;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-3) inset;
}

.preview-content {
  max-height: 500px;
  overflow-y: auto;
}

.preview-content h3 {
  margin-bottom: 20px;
  color: var(--text-primary);
}

.preview-item {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color-light);
}

.preview-item label {
  font-weight: 600;
  color: var(--text-secondary);
  margin-right: 10px;
}

.preview-detail {
  margin-top: 10px;
  line-height: 1.6;
}
</style>