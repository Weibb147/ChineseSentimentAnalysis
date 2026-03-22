<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {
  DataAnalysis,
  Delete,
  EditPen,
  InfoFilled,
  MagicStick,
  Monitor,
  Star,
  Sunny,
  Trophy,
  UploadFilled
} from '@element-plus/icons-vue';
import {useAuthStore} from '@/stores/auth';
import {analyzeSingleText, deleteTask, getTaskList, getTaskResults, uploadAndPredict} from '@/api/analysis.js';
import {getModelList} from '@/api/models.js';
import {useRouter} from 'vue-router';

const authStore = useAuthStore()
const router = useRouter()
const isAuthenticated = computed(() => !!authStore.token)

const text = ref('')
const selectedModel = ref('')
const models = ref<any[]>([])
const currentResults = ref<any[]>([])
const showDetailDialog = ref(false)
const selectedResult = ref<any>(null)
const selectedResultIndex = ref(-1)
const currentTask = ref<any>(null)

// Missing refs restored
const taskInfo = ref<any>(null)
const loading = ref(false)
const modelsLoading = ref(false)
const historyTasks = ref<any[]>([])
const historyLoading = ref(false)

// Batch Analysis State
const activeTab = ref('single')
const batchLoading = ref(false)
const batchTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  if (currentTask.value?.id) {
    fetchBatchResults(currentTask.value.id)
  }
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  if (currentTask.value?.id) {
    fetchBatchResults(currentTask.value.id)
  }
}

// Tech & Warm features
const timeGreeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 5) return '夜深了，注意休息'
  if (hour < 11) return '早上好，开启美好的一天'
  if (hour < 13) return '中午好，记得按时吃饭'
  if (hour < 18) return '下午好，愿你心情愉悦'
  return '晚上好，享受宁静时光'
})

const getSentimentAdvice = (label: string) => {
  if (!label) return ''
  const map: Record<string, string> = {
    happy: '保持这份好心情，将快乐传递给身边的人！✨',
    angry: '深呼吸，喝杯水，试着听听轻音乐放松一下。🌿',
    sad: '抱抱你，一切都会好起来的，去散散步吧。💪',
    fear: '别担心，不仅有我在，还有很多人支持你。🛡️',
    surprise: '生活总是充满了未知的惊喜！🎉',
    neutral: '平静也是一种力量，享受当下的宁静。☕'
  }
  return map[label.toLowerCase()] || '用心感受生活的每一个瞬间。'
}

const parseJsonObject = (val: any) => {
  if (!val) return {}
  if (typeof val === 'string') {
    try {
      const parsed = JSON.parse(val)
      return parsed && typeof parsed === 'object' ? parsed : {}
    } catch {
      return {}
    }
  }
  return typeof val === 'object' ? val : {}
}

const getMaxProbability = (probability: any) => {
  const values = Object.values(probability || {}) as Array<unknown>
  let max = 0
  for (const v of values) {
    const n = Number(v)
    if (Number.isFinite(n) && n > max) max = n
  }
  return max
}

const toFastApiModelName = (modelNameLike: any) => {
  if (!modelNameLike) return 'roberta_base'
  const raw = String(modelNameLike).trim()
  if (!raw) return 'roberta_base'

  const lower = raw.toLowerCase().trim()
  if (lower === 'roberta_base' || lower === 'roberta' || lower === 'base') return 'roberta_base'
  if (lower === 'roberta_bilstm_attention') return 'roberta_bilstm_attention'
  if (lower === 'roberta_gru_attention') return 'roberta_gru_attention'

  const compact = lower.replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '')
  const aliasMap: Record<string, string> = {
    roberta_gru: 'roberta_gru_attention',
    roberta_gru_att: 'roberta_gru_attention',
    roberta_bilstm: 'roberta_bilstm_attention',
    roberta_lstm: 'roberta_bilstm_attention',
    roberta_bilstm_att: 'roberta_bilstm_attention'
  }

  if (aliasMap[compact]) return aliasMap[compact]
  if (compact.includes('roberta') && compact.includes('bilstm')) return 'roberta_bilstm_attention'
  if (compact.includes('roberta') && compact.includes('gru')) return 'roberta_gru_attention'

  return raw
}

const setSelectedResult = (row: any) => {
  selectedResult.value = row
  const id = row?.id ?? row?.resultId
  let idx = -1
  if (id != null) {
    idx = currentResults.value.findIndex(r => (r?.id ?? r?.resultId) === id)
  }
  if (idx === -1) {
    idx = currentResults.value.indexOf(row)
  }
  selectedResultIndex.value = idx
}

const handleRowClick = (row: any) => {
  setSelectedResult(row)
  showDetailDialog.value = true
}

const canGoPrev = computed(() => selectedResultIndex.value > 0)
const canGoNext = computed(() => {
  if (selectedResultIndex.value < 0) return false
  return selectedResultIndex.value < currentResults.value.length - 1
})

const goPrev = () => {
  if (!canGoPrev.value) return
  const nextRow = currentResults.value[selectedResultIndex.value - 1]
  if (nextRow) setSelectedResult(nextRow)
}

const goNext = () => {
  if (!canGoNext.value) return
  const nextRow = currentResults.value[selectedResultIndex.value + 1]
  if (nextRow) setSelectedResult(nextRow)
}

const getStatusLabel = (status: string) => {
  if (!status) return '待处理'
  if (status === 'FINISHED' || status === 'COMPLETED') return '完成'
  if (status === 'FAILED') return '失败'
  return '处理中'
}

const getStatusType = (status: string) => {
  if (status === 'FINISHED' || status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const detailMeta = computed(() => {
  const task = currentTask.value || {}
  const result = selectedResult.value || {}

  const taskId = result.taskId ?? task.id ?? taskInfo.value?.id
  const resultId = result.id ?? result.resultId

  const modelDisplayName =
      task.modelDisplayName ||
      selectedModelInfo.value?.displayName ||
      selectedModelInfo.value?.modelName ||
      selectedModelInfo.value?.name ||
      selectedModel.value ||
      '-'

  const modelType =
      task.modelType ||
      task.modelName ||
      selectedModelInfo.value?.modelType ||
      selectedModelInfo.value?.modelName ||
      '-'

  const modelId = task.modelId ?? selectedModel.value

  const taskName = task.taskName || task.name || '-'
  const taskType = task.taskType || (batchTotal.value > 0 || currentResults.value.length > 1 ? 'BATCH' : 'SINGLE')
  const status = task.status || taskInfo.value?.status

  const username = result.username || task.username || authStore.userInfo?.username || '我'
  const createdAt = result.createdAt || task.createdAt || taskInfo.value?.createdAt

  return {
    taskId,
    resultId,
    taskName,
    taskType,
    modelId,
    modelType,
    modelDisplayName,
    status,
    username,
    createdAt
  }
})

const fetchBatchResults = async (taskId: number) => {
  try {
    const res = await getTaskResults(taskId, { pageNum: currentPage.value, pageSize: pageSize.value })
    if (res && res.code === 0 && res.data) {
      currentResults.value = res.data.records.map((r: any) => ({
        ...r,
        probability: parseJsonObject(r.probabilityJson ?? r.probability),
        keywords: parseJsonObject(r.keywordsJson ?? r.keywords)
      }))
      batchTotal.value = res.data.total
    }
  } catch (e) {
    console.error("Failed to fetch batch results", e)
  }
}

const handleBatchUpload = async (options: any) => {
  const { file } = options
  if (!file) return
  
  // Validate file type
  const isValidType = /\.(txt|csv|xlsx|xls)$/.test(file.name)
  if (!isValidType) {
    ElMessage.error('只支持 .txt, .csv, .xlsx, .xls 文件格式')
    return
  }
  
  // Validate file size (e.g., 10MB)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return
  }

  if (!selectedModel.value) {
    ElMessage.warning('请选择分析模型')
    return
  }

  if (!isAuthenticated.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  batchLoading.value = true
  currentResults.value = []
  currentPage.value = 1
  
  try {
    // 调用上传接口
    const formData = new FormData()
    formData.append('file', file)
    // Default to user_id 1 if not found, to avoid validation error
    formData.append('user_id', String(authStore.userInfo?.id || 1))
    
    const modelNameForFastapi = toFastApiModelName(
        selectedModelInfo.value?.modelType ||
        selectedModelInfo.value?.modelName ||
        String(selectedModel.value)
    )
    formData.append('model_name', modelNameForFastapi)
    
    formData.append('task_name', file.name)

    const res = await uploadAndPredict(formData)
    
    if (res.data?.success) {
       const taskId = res.data.taskId
       const now = new Date().toISOString()
       taskInfo.value = { id: taskId, createdAt: now, status: 'FINISHED' }
       currentTask.value = {
         id: taskId,
         taskName: file.name,
         taskType: 'BATCH',
         modelId: selectedModel.value,
         modelType: modelNameForFastapi,
         modelDisplayName: selectedModelInfo.value?.displayName || modelNameForFastapi,
         status: 'FINISHED',
         createdAt: now,
         username: authStore.userInfo?.username
       }
       ElMessage.success(`分析完成，共 ${res.data.total} 条数据`)
       
       await fetchBatchResults(taskId)
       fetchHistory() 
    } else {
       ElMessage.error(res.data?.detail || res.data?.message || '分析失败')
    }
  } catch (e: any) {
    console.error(e)
    ElMessage.error(e.response?.data?.detail || '上传分析失败')
  } finally {
    batchLoading.value = false
  }
}

// 加载模型列表
const loadModels = async () => {
  try {
    modelsLoading.value = true
    // Only fetch ACTIVE models for the user
    const res = await getModelList({ status: 'ACTIVE', pageSize: 100 })
    if (res.code === 0 && res.data) {
      // Backend returns a Page object, records are in data.records
      const records = res.data.records || []
      models.value = records.map((model: any) => {
        const speedLabel = getSpeedLabel(model.speed || model.speedLevel || 'medium')
        const accuracyLabel = getAccuracyLabel(model.accuracy || model.accuracyLevel || 'medium')
        return {
          ...model,
          displayName: model.modelName || model.name || model.id,
          speedLabel,
          accuracyLabel,
          speedColor: getSpeedColor(model.speed || model.speedLevel || 'medium'),
          accuracyColor: getAccuracyColor(model.accuracy || model.accuracyLevel || 'medium'),
          // 生成富文本描述
          richDescription: model.description || `该模型${speedLabel}，同时${accuracyLabel}。`
        }
      })

      // 不再自动选择推荐模型，让用户手动选择
      if (models.value.length > 0 && !selectedModel.value) {
        // 可以设置一个默认值，但不自动选择
      }
    }
  } catch (error) {
    console.error('加载模型列表失败', error)
    ElMessage.warning('加载模型列表失败，使用默认模型')
  } finally {
    modelsLoading.value = false
  }
}

const getSpeedLabel = (speed: string) => {
  const labels: Record<string, string> = {
    fast: '具备极速响应能力，适合实时交互与流式处理',
    medium: '在速度与精度之间保持最佳平衡，通用性强',
    slow: '采用深度分析模式，以时间换取更高精度，适合离线挖掘'
  }
  return labels[speed] || speed
}

const getAccuracyLabel = (accuracy: string) => {
  const labels: Record<string, string> = {
    very_high: '拥有专家级精度与极低误判率，适用于医疗/金融等严谨场景',
    high: '表现优异且稳定，能胜任绝大多数商业分析任务',
    medium: '满足日常分析需求，适合大规模数据快速初筛',
    low: '仅供参考，建议用于简单分类任务'
  }
  return labels[accuracy] || accuracy
}

const getSpeedColor = (speed: string) => {
  const colors: Record<string, string> = {
    fast: '#22c55e',
    medium: '#f59e0b',
    slow: '#ef4444'
  }
  return colors[speed] || '#909399'
}

const getAccuracyColor = (accuracy: string) => {
  const colors: Record<string, string> = {
    very_high: '#22c55e',
    high: '#3b82f6',
    medium: '#f59e0b',
    low: '#ef4444'
  }
  return colors[accuracy] || '#909399'
}

const selectedModelInfo = computed(() => {
  return models.value.find(m => m.id === selectedModel.value) || {}
})

const orderedModels = computed(() => {
  const list = (models.value || []).slice()
  list.sort((a: any, b: any) => {
    const ai = Number(a?.id)
    const bi = Number(b?.id)
    if (Number.isFinite(ai) && Number.isFinite(bi)) return ai - bi
    return String(a?.displayName ?? a?.modelName ?? a?.id ?? '').localeCompare(
        String(b?.displayName ?? b?.modelName ?? b?.id ?? '')
    )
  })
  return list
})

// 情感标签映射（后端返回的标签可能是中文或英文）
const sentimentLabelMap: Record<string, { label: string; color: string }> = {
  happy: {label: '😊 积极 (Happy)', color: '#22c55e'},
  angry: {label: '😡 愤怒 (Angry)', color: '#f97316'},
  sad: {label: '😢 悲伤 (Sad)', color: '#3b82f6'},
  fear: {label: '😱 恐惧 (Fear)', color: '#a855f7'},
  surprise: {label: '😲 惊奇 (Surprise)', color: '#eab308'},
  neutral: {label: '😐 中性 (Neutral)', color: '#94a3b8'}
}

const getSentimentLabel = (sentiment: string) => {
  if (!sentiment) return ''
  const key = sentiment.toLowerCase()
  return sentimentLabelMap[key]?.label || sentiment
}

const getSentimentColor = (sentiment: string) => {
  if (!sentiment) return '#909399'
  const key = sentiment.toLowerCase()
  return sentimentLabelMap[key]?.color || '#909399'
}

const probabilityList = computed(() => {
  if (!selectedResult.value?.probability) return []
  return Object.entries(selectedResult.value.probability)
      .map(([key, value]) => ({
        key,
        label: getSentimentLabel(key),
        value: Number(value) || 0
      }))
      .sort((a, b) => b.value - a.value)
})

const keywordList = computed(() => {
  if (!selectedResult.value?.keywords) return []
  const kwObj = parseJsonObject(selectedResult.value.keywords)
  return Object.entries(kwObj)
      .map(([key, value]) => ({
        text: key,
        value: Number(value) || 0
      }))
      .sort((a, b) => b.value - a.value)
})

const fetchResultByTask = async (task: any) => {
  currentTask.value = task
  const isBatch = task.taskType === 'BATCH' || (task.taskName && /\.(txt|csv|xlsx|xls)$/.test(task.taskName))

  if (isBatch) {
    currentPage.value = 1
    activeTab.value = 'batch'
    await fetchBatchResults(task.id)
  } else {
    activeTab.value = 'single'
    const res = await getTaskResults(task.id, {pageNum: 1, pageSize: 1})
    const record = res.data?.records?.[0]
    if (record) {
      const resultObj = {
        id: record.id,
        taskId: record.taskId || task.id,
        predictedLabel: record.predictedLabel,
        probability: parseJsonObject(record.probabilityJson ?? record.probability),
        content: record.content,
        createdAt: record.createdAt,
        keywords: parseJsonObject(record.keywordsJson ?? record.keywords),
        username: record.username
      }
      currentResults.value = [resultObj]
      setSelectedResult(resultObj)
      batchTotal.value = 1
      // Automatically open detail popup as requested
      showDetailDialog.value = true
    }
  }
}

const handleDeleteTask = async (task: any, event: Event) => {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm('确定要删除这条分析记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteTask(task.id)
    ElMessage.success('删除成功')
    fetchHistory()
    
    // If deleted task is currently displayed, clear it
    if (currentTask.value?.id === task.id) {
      clearText()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
      ElMessage.error('删除失败')
    }
  }
}

const fetchHistory = async () => {
  if (!isAuthenticated.value) return
  historyLoading.value = true
  try {
    const res = await getTaskList({ pageNum: 1, pageSize: 5 })
    if (res.code === 0 && res.data) {
      historyTasks.value = res.data.records || []
    }
  } catch (e) {
    console.error('获取历史记录失败', e)
  } finally {
    historyLoading.value = false
  }
}

// 调用后端API进行情感分析
const handlePredict = async () => {
  if (!text.value.trim()) {
    ElMessage.warning('请输入需要分析的文本')
    return
  }

  if (!selectedModel.value) {
    ElMessage.warning('请选择分析模型')
    return
  }

  if (!isAuthenticated.value) {
    ElMessage.warning('请先登录以使用情感分析功能')
    router.push('/login')
    return
  }

  loading.value = true
  try {
    const rawModelId: any = selectedModel.value
          let modelId = typeof rawModelId === 'number' ? rawModelId : Number(rawModelId)
          
          // Try to resolve model ID if it's invalid but we have a name match in loaded models
          if (!Number.isFinite(modelId) && typeof rawModelId === 'string') {
              const foundModel = models.value.find(m => 
                  m.modelName === rawModelId || 
                  m.name === rawModelId || 
                  m.displayName === rawModelId
              )
              if (foundModel) {
                  modelId = foundModel.id
              }
          }

          const modelName = toFastApiModelName(
              selectedModelInfo.value?.modelType ||
              selectedModelInfo.value?.modelName ||
              String(selectedModel.value)
          )
            
          const res = await analyzeSingleText({
            content: text.value,
            modelId: Number.isFinite(modelId) ? modelId : undefined,
            modelName,
            taskName: `${selectedModelInfo.value.displayName || selectedModel.value} - 情感分析`,
            userId: authStore.userInfo?.id || 1
          })

    if (res.success || (res.code === 0 && res.data)) {
      // Handle both pure object response (FastAPI) and wrapped response (Standard)
      const data = res.success ? res : res.data
      
      // Update result display directly
      const resultObj = {
        content: text.value,
        id: data.resultId || data.id,
        resultId: data.resultId,
        taskId: data.id,
        predictedLabel: data.predictedLabel || data.predicted_label || 'neutral',
        // Backend now returns JSON string for probability and keywords in SingleAnalysisResponse
        probability: parseJsonObject(data.probabilityJson || data.probability),
        keywords: parseJsonObject(data.keywordsJson || data.keywords),
        createdAt: data.createdAt || new Date().toISOString()
      }
      
      currentResults.value = [resultObj]
      setSelectedResult(resultObj)
      batchTotal.value = 1
      
      // If we have an ID, update task info and history
      if (data.id) {
          taskInfo.value = { id: data.id, createdAt: data.createdAt }
          currentTask.value = {
            id: data.id,
            taskName: `${selectedModelInfo.value.displayName || selectedModel.value} - 情感分析`,
            taskType: 'SINGLE',
            modelId: Number.isFinite(modelId) ? modelId : undefined,
            modelType: modelName,
            modelDisplayName: selectedModelInfo.value.displayName || modelName,
            status: 'FINISHED',
            createdAt: data.createdAt,
            username: authStore.userInfo?.username
          }
          fetchHistory()
      }
      
      ElMessage.success('情感分析完成')
    } else {
      ElMessage.error(res.message || res.errorMessage || '情感分析失败')
    }
  } catch (error: any) {
    console.error('情感分析失败', error)
    ElMessage.error(error.response?.data?.message || '情感分析失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const clearText = () => {
  text.value = ''
  currentResults.value = []
  selectedResult.value = null
  selectedResultIndex.value = -1
  taskInfo.value = null
  currentTask.value = null
}

// 组件加载时获取模型列表
onMounted(() => {
  loadModels()
  if (isAuthenticated.value) {
    fetchHistory()
  }
})
</script>

<template>
  <div class="predict-container">
    <div class="page-header">
      <div class="header-content">
        <div class="greeting-section">
          <el-icon class="greeting-icon"><Sunny /></el-icon>
          <span class="greeting-text">{{ timeGreeting }}，{{ authStore.userInfo?.username || '用户' }}</span>
        </div>
        <h1 class="page-title">智能情感分析平台</h1>
        <p class="page-subtitle">基于RoBERTa的中文文本情感分析，支持对多类别情感使用多种模型对比分析</p>
      </div>
    </div>

    <!-- 模型选择框 (位于上方) -->
    <div class="top-section">
      <el-card v-loading="modelsLoading" class="model-selector-card hover-effect">
        <template #header>
          <div class="card-header">
            <div class="header-title-group">
              <el-icon class="section-icon"><Monitor /></el-icon>
              <div>
                <p class="sub-title">Model Selection</p>
                <h3>选择分析模型</h3>
              </div>
            </div>
            <el-tag effect="plain" type="info" round>
              {{ models.length }} 个可用模型
            </el-tag>
          </div>
        </template>

        <div class="model-list">
          <div
              v-for="(model, idx) in orderedModels"
              :key="model.id"
              :class="{ 'selected': selectedModel === model.id, 'recommended': model.recommended }"
              class="model-item"
              @click="selectedModel = model.id"
          >
            <div class="model-header">
              <div class="model-name">

                <el-icon v-if="model.recommended" class="recommend-icon"><Star /></el-icon>
                <el-icon v-else class="model-icon"><Trophy /></el-icon>
                <h4>{{ model.displayName }}</h4>
              </div>
              <el-radio v-model="selectedModel" :label="model.id" class="model-radio"/>
            </div>

            <p class="model-description text-ellipsis-2" :title="model.description || model.richDescription">{{ model.description || model.richDescription || '基于SMP2020微博情绪数据集训练的情感分析模型' }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <div class="content-layout">
      <main class="center-panel">
        <div class="analysis-wrapper" :class="{ 'has-results': currentResults.length > 0 }">
          <!-- 文本输入框 (位于中间) -->
          <el-card class="input-card hover-effect">
          <template #header>
            <div class="card-header">
              <div class="header-title-group">
                <el-icon class="section-icon"><EditPen /></el-icon>
                <div>
                  <p class="sub-title">Intelligent Analysis</p>
                  <h2>情感分析任务</h2>
                </div>
              </div>
              <div class="header-actions">
                <el-tag v-if="selectedModelInfo.displayName" effect="plain" type="primary" round>
                  {{ selectedModelInfo.displayName }}
                </el-tag>
                <el-tag v-if="taskInfo?.status && activeTab === 'single'" effect="plain" type="success" round>
                  {{ taskInfo.status || '待处理' }}
                </el-tag>
              </div>
            </div>
          </template>

          <el-tabs v-model="activeTab" class="analysis-tabs">
            <el-tab-pane label="单文本分析" name="single">
              <el-input
                  v-model="text"
                  :rows="14"
                  class="text-input"
                  maxlength="500"
                  placeholder="请输入需要进行情感分析的中文文本... (例如：今天天气真好，心情非常愉快！)"
                  show-word-limit
                  type="textarea"
              />
              <div class="actions">
                <el-button :loading="loading" size="large" type="primary" class="analyze-btn" @click="handlePredict">
                  <el-icon class="el-icon--left"><MagicStick /></el-icon>
                  开始智能分析
                </el-button>
                <el-button :disabled="!text && currentResults.length === 0" size="large" @click="clearText" plain>
                  <el-icon class="el-icon--left"><Delete /></el-icon>
                  清空内容
                </el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="批量文件分析" name="batch">
              <div class="batch-upload-container">
                <el-upload
                    class="upload-demo"
                    drag
                    action="#"
                    :http-request="handleBatchUpload"
                    :show-file-list="false"
                    accept=".txt,.csv,.xlsx,.xls"
                    :disabled="batchLoading"
                >
                  <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                  <div class="el-upload__text">
                    拖拽文件到此处或 <em>点击上传</em>
                  </div>
                  <template #tip>
                    <div class="el-upload__tip">
                      支持 .txt, .csv, .xlsx 文件，第一列应为文本内容
                    </div>
                  </template>
                </el-upload>

                <div v-if="batchLoading" class="loading-state">
                  <el-skeleton :rows="3" animated />
                  <p>正在分析中，请稍候...</p>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 分析结果卡片 (位于文本输入框和使用说明之间) -->
        <el-card v-if="currentResults.length > 0" class="result-card hover-effect">
          <template #header>
            <div class="card-header">
              <div class="header-title-group">
                <el-icon class="section-icon"><DataAnalysis /></el-icon>
                <div>
                  <p class="sub-title">Analysis Results</p>
                  <h3>分析结果 (共 {{ batchTotal || currentResults.length }} 条)</h3>
                </div>
              </div>
            </div>
          </template>
          
          <!-- Warm Advice Section for Single Analysis -->
          <div v-if="activeTab === 'single' && currentResults.length > 0" class="warm-advice-section">
             <el-alert
               :title="getSentimentAdvice(currentResults[0].predictedLabel)"
               :type="currentResults[0].predictedLabel === 'neutral' ? 'info' : (['happy', 'surprise'].includes(currentResults[0].predictedLabel) ? 'success' : 'warning')"
               :closable="false"
               show-icon
               class="sentiment-advice"
             />
          </div>

          <!-- Result Table View (Unified) -->
          <div v-if="currentResults.length > 0" class="table-container">
            <el-table 
              :data="currentResults" 
              height="100%"
              style="width: 100%" 
              @row-click="handleRowClick"
              :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
              highlight-current-row
              stripe
            >
              <el-table-column type="index" label="#" width="60" align="center" />
              <el-table-column prop="content" label="文本内容" show-overflow-tooltip min-width="250" />
              <el-table-column prop="predictedLabel" label="情感倾向" width="150">
                <template #default="{ row }">
                  <el-tag :color="getSentimentColor(row.predictedLabel)" effect="dark" size="small">
                    {{ getSentimentLabel(row.predictedLabel) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="分析时间" width="170" align="center">
                <template #default="{ row }">
                  {{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '刚刚' }}
                </template>
              </el-table-column>
              <el-table-column label="置信度" width="120" align="center">
                <template #default="{ row }">
                   <span :style="{ color: getMaxProbability(row.probability) > 0.8 ? '#67C23A' : '#909399', fontWeight: 'bold' }">
                     {{ Math.round(getMaxProbability(row.probability) * 100) }}%
                   </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center" fixed="right">
                 <template #default="{ row }">
                    <el-button link type="primary" size="small" @click.stop="handleRowClick(row)">详情</el-button>
                 </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="currentResults.length > 0 && (batchTotal > pageSize || activeTab === 'batch')" class="pagination-container">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="batchTotal"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </div>
      </main>
    </div>

    <!-- 使用说明 (位于最下方) -->
    <div class="bottom-section">
      <el-card class="info-card hover-effect">
        <template #header>
          <div class="card-header">
            <div class="header-title-group">
              <el-icon class="section-icon"><InfoFilled /></el-icon>
              <div>
                <p class="sub-title">Guide</p>
                <h3>使用说明</h3>
              </div>
            </div>
          </div>
        </template>
        <div class="info-content">
          <div class="info-grid">
            <div class="info-item">
              <div class="info-title">数据来源：基于 SMP2020 微博情绪分类评测数据集进行预测。</div>

            </div>
            <div class="info-item">
              <div class="info-title">标签范围：支持6 类情感类别。如happy、angry、sad、fear、surprise、neutral。</div>
            </div>
            <div class="info-item">
              <div class="info-title">输入建议：建议 输入10~200 字的长句子，语义完整更稳定，模型分析的效果更佳。</div>
            </div>
            <div class="info-item">
              <div class="info-title">关键词说明：基于文本权重抽取，用于辅助理解模型关注点，亦能够用于绘制词云图。</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Dialog for Batch Detail & Single Result Detail -->
    <el-dialog v-model="showDetailDialog" title="分析详情" width="700px" append-to-body destroy-on-close align-center center>
       <div v-if="selectedResult" class="result-detail-box">
           <div class="detail-grid">
               <div class="detail-item">
                  <span class="d-label">分析用户</span>
                  <span class="d-value">{{ detailMeta.username }}</span>
               </div>
               <div class="detail-item">
                  <span class="d-label">分析时间</span>
                  <span class="d-value">{{ detailMeta.createdAt ? new Date(detailMeta.createdAt).toLocaleString() : '刚刚' }}</span>
               </div>
               <div class="detail-item">
                  <span class="d-label">模型名称</span>
                  <span class="d-value">{{ detailMeta.modelDisplayName }}</span>
               </div>
               <div class="detail-item">
                  <span class="d-label">模型类型</span>
                  <span class="d-value">{{ detailMeta.modelType || '-' }}</span>
               </div>
               <div class="detail-item">
                  <span class="d-label">任务ID</span>
                  <span class="d-value">{{ detailMeta.taskId ?? '-' }}</span>
               </div>
               <div class="detail-item">
                  <span class="d-label">结果ID</span>
                  <span class="d-value">{{ detailMeta.resultId ?? '-' }}</span>
               </div>
               <div class="detail-item">
                  <span class="d-label">任务名称</span>
                  <span class="d-value">{{ detailMeta.taskName }}</span>
               </div>
               <div class="detail-item">
                  <span class="d-label">任务类型</span>
                  <span class="d-value">{{ detailMeta.taskType }}</span>
               </div>
               <div class="detail-item full">
                  <span class="d-label">情感倾向</span>
                  <div class="d-value">
                      <el-tag :color="getSentimentColor(selectedResult.predictedLabel)" effect="dark" size="large">
                        {{ getSentimentLabel(selectedResult.predictedLabel) }}
                      </el-tag>
                      <span class="prob-tag" v-if="selectedResult.probability">
                         置信度: {{ (getMaxProbability(selectedResult.probability) * 100).toFixed(1) }}%
                      </span>
                  </div>
               </div>
               
               <div class="detail-item full" v-if="probabilityList.length > 0">
                  <span class="d-label">概率分布</span>
                  <div class="d-value probability-list" style="width: 100%">
                      <div
                          v-for="item in probabilityList"
                          :key="item.key"
                          class="probability-item"
                      >
                        <div class="prob-label">
                          <span :style="{ backgroundColor: getSentimentColor(item.key) }" class="dot"></span>
                          {{ item.label }}
                        </div>
                        <el-progress
                            :color="getSentimentColor(item.key)"
                            :percentage="Math.round(item.value * 100)"
                            :stroke-width="12"
                            :text-inside="false"
                            style="flex: 1"
                        />
                        <span class="prob-value">{{ Math.round(item.value * 100) }}%</span>
                      </div>
                  </div>
               </div>

               <div class="detail-item full">
                  <span class="d-label">文本内容</span>
                  <div class="d-value content-box">{{ selectedResult.content }}</div>
               </div>

               <div class="detail-item full" v-if="keywordList.length > 0">
                  <span class="d-label">关键词</span>
                  <div class="d-value keyword-tags">
                      <el-tag v-for="kw in keywordList" :key="kw.text" size="small" effect="plain">
                          {{ kw.text }} · {{ kw.value }}
                      </el-tag>
                  </div>
               </div>
           </div>
       </div>
       <template #footer>
          <div class="dialog-footer">
            <div v-if="currentResults.length > 1" class="dialog-nav">
              <el-button :disabled="!canGoPrev" @click="goPrev">上一条</el-button>
              <span class="nav-text">{{ (selectedResultIndex > -1 ? (selectedResultIndex + 1) : 1) }}/{{ currentResults.length }}</span>
              <el-button :disabled="!canGoNext" @click="goNext">下一条</el-button>
            </div>
            <div class="dialog-actions">
              <el-button type="danger" :icon="Delete" @click="handleDeleteTask(currentTask || taskInfo, $event); showDetailDialog = false">删除此记录</el-button>
              <el-button @click="showDetailDialog = false">关闭</el-button>
            </div>
          </div>
       </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.predict-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 8px 12px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.content-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

.left-panel,
.center-panel,
.right-panel {
  width: 100%;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 0 12px 12px;
  padding: 8px 8px 16px;
  margin: 0 -8px 8px;
  color: white;
  text-align: center;
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.2);
  position: relative;
}

.greeting-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.15);
  display: inline-flex;
  padding: 6px 16px;
  border-radius: 20px;
  backdrop-filter: blur(5px);
}

.greeting-icon {
  font-size: 18px;
  color: #ffd04b;
}

.greeting-text {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.header-content h1 {
  margin: 0 0 12px 0;
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.page-subtitle {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
  line-height: 1.6;
  max-width: 600px;
  margin: 0 auto;
}

.content-layout .model-selector-card,
.content-layout .input-card,
.content-layout .result-card,
.content-layout .info-card {
  border-radius: 12px;
  overflow: hidden;
}

.content-layout .model-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content-layout .model-item {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 6px;
  padding: 2px 2px 1px;
  background: #fff;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
  cursor: pointer;
}

.content-layout .model-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.content-layout .model-item.selected {
  border-color: rgba(102, 126, 234, 0.9);
  box-shadow: 0 10px 28px rgba(102, 126, 234, 0.18);
}

.content-layout .model-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.content-layout .model-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.content-layout .model-name h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  max-width: 190px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content-layout .model-index {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(102, 126, 234, 0.12);
  color: #4f46e5;
  font-weight: 800;
  font-size: 12px;
  flex: 0 0 auto;
}

.content-layout .model-description {
  margin: 0px 0 1px;
  color: #475569;
  font-size: 8px;
  line-height: 1.1;
}

.content-layout .model-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.content-layout .metric {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(2, 6, 23, 0.03);
  min-width: 0;
}

.content-layout .metric :deep(.el-tag) {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content-layout .metric-label {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.content-layout .analysis-tabs {
  margin-top: 2px;
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
}

.content-layout .analysis-tabs :deep(.el-tabs__content) {
  flex: 1 1 auto;
  min-height: 0;
}

.content-layout .analysis-tabs :deep(.el-tab-pane) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.content-layout .center-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.analysis-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

.analysis-wrapper.has-results {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: stretch;
  gap: 16px;
}

.content-layout .input-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content-layout .result-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 400px; /* Ensure minimum height */
}

.content-layout .result-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-bottom: 0;
  overflow: hidden;
}

.table-container {
  flex: 1;
  min-height: 0;
  height: 100%;
}

@media (max-width: 1200px) {
  .analysis-wrapper.has-results {
    grid-template-columns: 1fr;
  }
}

.content-layout .input-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.content-layout .info-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.content-layout .text-input :deep(textarea) {
  border-radius: 8px;
  padding: 4px 4px;
  line-height: 1.3;
  min-height: 80px;
  width: 100%;
}

.content-layout .text-input {
  margin: 0;
  flex: 1 1 auto;
  min-height: 0;
}

.content-layout .text-input :deep(.el-textarea__inner) {
  height: 100%;
  min-height: 80px;
  width: 100%;
}

.content-layout .actions {
  display: flex;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.content-layout .analyze-btn {
  min-width: 160px;
}

.content-layout .batch-upload-container {
  padding: 4px 0 2px;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.content-layout .upload-demo,
.content-layout .upload-demo :deep(.el-upload),
.content-layout .upload-demo :deep(.el-upload-dragger) {
  width: 100%;
}

.content-layout .upload-demo :deep(.el-upload-dragger) {
  min-height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4px 4px;
}

.content-layout .loading-state {
  margin-top: 14px;
}

.content-layout .info-content {
  padding: 4px 2px 2px;
}

.content-layout .info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.content-layout .info-item {
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  padding: 12px 12px;
}

.content-layout .info-title {
  font-weight: 800;
  color: #0f172a;
  font-size: 13px;
  margin-bottom: 6px;
}

.content-layout .info-desc {
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .content-layout {
    flex-direction: column;
  }
  .content-layout .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .content-layout {
    flex-direction: column;
  }
  .content-layout .info-grid {
    grid-template-columns: 1fr;
  }
}
.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 340px; /* Refactored Layout: Main | Sidebar */
  gap: 24px;
  align-items: start;
  margin-top: -40px;
}

/* Common Card Styles */
.hover-effect {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.hover-effect:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  font-size: 24px;
  color: #409eff;
  background: #ecf5ff;
  padding: 8px;
  border-radius: 12px;
}

.sub-title {
  margin: 0;
  color: #909399;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
}

.card-header h2,
.card-header h3 {
  margin: 4px 0 0 0;
  font-size: 18px;
  color: #303133;
}

/* Model Selector */
.model-selector-card {
  margin-bottom: 24px;
}

.model-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); /* Grid Layout for Models */
  gap: 16px;
}

.model-item {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  background: white;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
}

.model-item:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.model-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.model-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.model-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-icon {
  color: #909399;
  font-size: 18px;
}

.recommend-icon {
  color: #e6a23c;
  font-size: 18px;
}

.model-name h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.model-description {
  margin: 0 0 16px 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  flex-grow: 1;
}

.text-ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.model-metrics {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.metric {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

/* Main Content */
.main-content {
  display: flex;
  flex-direction: column;
}

.input-card {
  background: white;
  margin-bottom: 24px;
}

.text-input {
  margin: 20px 0;
}

.text-input :deep(.el-textarea__inner) {
  padding: 16px;
  font-size: 15px;
  line-height: 1.8;
  border-radius: 8px;
  box-shadow: none;
  background-color: #f8fafc;
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
}

.text-input :deep(.el-textarea__inner:focus) {
  background-color: white;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
}

.analyze-btn {
  padding: 12px 32px;
  font-weight: 600;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  transition: transform 0.1s;
}

.analyze-btn:active {
  transform: scale(0.98);
}

/* Batch Upload */
.batch-upload-container {
  padding: 20px 0;
}

.upload-demo :deep(.el-upload-dragger) {
  border-radius: 12px;
  border: 2px dashed #dcdfe6;
  background-color: #f8fafc;
  transition: all 0.3s;
}

.upload-demo :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background-color: #ecf5ff;
}

/* Result Card */
.warm-advice-section {
  margin-bottom: 24px;
}

.sentiment-advice {
  border-radius: 8px;
  padding: 12px 16px;
}

.sentiment-advice :deep(.el-alert__title) {
  font-size: 14px;
  font-weight: 500;
}

/* Right Sidebar */
.right-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.history-card, .info-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.history-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 8px;
  margin-bottom: 4px;
}

.history-item:hover {
  background-color: #f0f9ff;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.history-content {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 6px;
}

.history-result {
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Dialog Styles */
.result-detail-box {
  padding: 10px 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px 32px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item.full {
  grid-column: 1 / -1;
}

.d-label {
  font-size: 12px;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.d-value {
  font-size: 15px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.prob-tag {
  font-size: 12px;
  color: #606266;
  background: #f4f4f5;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.content-box {
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  line-height: 1.8;
  max-height: 300px;
  overflow-y: auto;
  width: 100%;
  font-size: 14px;
}

.probability-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.probability-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.prob-label {
  width: 120px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 13px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.prob-value {
  width: 50px;
  text-align: right;
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.dialog-nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-text {
  font-size: 13px;
  color: #606266;
  min-width: 70px;
  text-align: center;
}

.dialog-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Responsive */
@media (max-width: 1200px) {
  .content-wrapper {
    grid-template-columns: 1fr; /* Stack on smaller screens */
  }
  .right-sidebar {
    order: 2; /* Sidebar below main content */
  }
  .model-list {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .predict-container {
    padding: 0 16px 24px;
  }
  .page-header {
    margin: 0 -16px 24px;
    padding: 32px 20px;
    border-radius: 0 0 20px 20px;
  }
  .header-content h1 {
    font-size: 28px;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

.pagination-container {
  padding: 16px 0 0;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #ebeef5;
  margin-top: auto; /* Push to bottom if flex column */
}
</style>
