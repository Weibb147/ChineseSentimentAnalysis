<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { 
  Search, Refresh, Plus, Delete, User, Bell, Connection, 
  DataLine, ChatDotRound, Odometer, Monitor, Edit, Sort, List, Sunny,
  Timer, Message, Reading, Male, Female, Iphone
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import TaskHistoryView from './TaskHistoryView.vue'
import AdminUserView from './AdminUserView.vue'

// API Imports
import { getUserListAPI } from '@/api/admin.js' // Still needed for metrics? Yes, getUserListAPI({ page: 1, size: 1 }) in fetchVisualizationData
import { getNoticeList, createNotice, updateNotice, deleteNotice } from '@/api/notice.js'
import { getModelList, createModel, activateModel, deactivateModel, deleteModel } from '@/api/models.js'
import { getAllFeedbacks, replyFeedback, deleteFeedback } from '@/api/feedback.js'
import { getAdminVizData, getAdminKeywordsTop } from '@/api/adminAnalysis.js'
import { deleteResult } from '@/api/analysis.js'

// --- State ---
const activeTab = ref('overview')
const visLoading = ref(false)
const usersLoading = ref(false) // Keeping it to avoid breaking refs if referenced elsewhere, though unused
const noticesLoading = ref(false)
const modelsLoading = ref(false)
const feedbacksLoading = ref(false)

// Metrics
const metrics = ref({ users: 0, notices: 0, models: 0, analysis: 0 })
const sentimentStats = ref<Record<string, number>>({ happy: 0, angry: 0, sad: 0, fear: 0, surprise: 0, neutral: 0 })
const totalResults = ref(0)
const dominantSentiment = computed(() => {
  let maxKey = 'neutral'; let maxVal = -1
  for (const [key, val] of Object.entries(sentimentStats.value)) {
    if (val > maxVal) { maxVal = val; maxKey = key }
  }
  return maxKey
})

// Chart Refs
const barRef = ref<HTMLDivElement | null>(null)
const lineRef = ref<HTMLDivElement | null>(null)
const pieRef = ref<HTMLDivElement | null>(null)
const cloudRef = ref<HTMLDivElement | null>(null)
let charts: echarts.ECharts[] = []

// Overview Data
const overviewData = ref<{ distribution: Record<string, any>; trend: Record<string, any>; keywords: { name: string; value: number }[] }>({ distribution: {}, trend: {}, keywords: [] })
const unifiedList = ref<any[]>([])
const topK = ref(100)
const dateRange = ref<[string, string] | null>(null)
const currentResult = ref<any>(null)
const resultDetailVisible = ref(false)

// Pagination & Search for Visualization
const vizCurrentPage = ref(1)
const vizPageSize = ref(10)
const vizSortOrder = ref<'asc' | 'desc'>('desc')

// CRUD Data
const notices = ref<any[]>([])
const models = ref<any[]>([])
const feedbacks = ref<any[]>([])

// Pagination & Search for CRUD
const noticePagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const modelPagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const feedbackPagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const feedbackFilters = reactive({ status: '', category: '' })

// Forms & Dialogs
const noticeDialogVisible = ref(false)
const modelDialogVisible = ref(false)
const replyDialogVisible = ref(false)
const saveLoading = ref(false)

const noticeFormRef = ref<FormInstance>()
const modelFormRef = ref<FormInstance>()

const currentNotice = reactive({ id: undefined, title: '', content: '', type: 'SYSTEM', status: 'VISIBLE' })
const currentModel = reactive({ id: undefined, modelName: '', modelType: '', version: '', description: '', modelFilePath: '' })
const currentFeedback = ref<any>(null)
const replyText = ref('')

// Constants
const colors: any = { happy: '#22c55e', angry: '#f97316', sad: '#3b82f6', fear: '#a855f7', surprise: '#eab308', neutral: '#94a3b8' }
const labels: any = { happy: '😊 积极', angry: '😡 愤怒', sad: '😢 悲伤', fear: '😱 恐惧', surprise: '😲 惊奇', neutral: '😐 中性' }
const labelOrder = ['happy', 'angry', 'sad', 'fear', 'surprise', 'neutral']
const noticeTypes = [{ label: '系统', value: 'SYSTEM' }, { label: '更新', value: 'UPDATE' }, { label: '提醒', value: 'WARNING' }]
const noticeStatuses = [{ label: '可见', value: 'VISIBLE' }, { label: '隐藏', value: 'HIDDEN' }]
const feedbackStatusOptions = [{ label: '全部', value: '' }, { label: '待处理', value: 'PENDING' }, { label: '处理中', value: 'IN_PROGRESS' }, { label: '已解决', value: 'RESOLVED' }]
const feedbackCategoryOptions = [{ label: '全部', value: '' }, { label: 'Bug反馈', value: 'BUG' }, { label: '功能建议', value: 'SUGGESTION' }, { label: '使用体验', value: 'EXPERIENCE' }, { label: '其他', value: 'OTHER' }]

const noticeRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}
const modelRules: FormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请输入模型类型', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  modelFilePath: [{ required: true, message: '请输入模型文件路径', trigger: 'blur' }]
}

// --- Helpers ---
const formatDate = (d: string) => d ? new Date(d).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-') : '-'
const getSentimentLabel = (s: string) => labels[s] || s
const getSentimentColor = (s: string) => colors[s] || '#909399'

const getNoticeStatusType = (status: string) => status === 'VISIBLE' ? 'success' : 'info'
const getNoticeTypeTag = (type: string) => {
  const map: any = { SYSTEM: 'primary', UPDATE: 'success', EVENT: 'warning', OTHER: 'info' }
  return map[type] || 'info'
}
const getNoticeTypeLabel = (type: string) => {
  const map: any = { SYSTEM: '系统公告', UPDATE: '更新公告', EVENT: '活动公告', OTHER: '其他公告' }
  return map[type] || '未知'
}

const getFeedbackStatusType = (status: string) => {
  const map: any = { PENDING: 'warning', IN_PROGRESS: 'primary', RESOLVED: 'success' }
  return map[status] || 'info'
}
const getFeedbackStatusLabel = (status: string) => {
  const map: any = { PENDING: '待处理', IN_PROGRESS: '处理中', RESOLVED: '已解决' }
  return map[status] || status
}
const getFeedbackCategoryLabel = (cat: string) => {
  const map: any = { BUG: 'Bug反馈', SUGGESTION: '功能建议', EXPERIENCE: '使用体验', OTHER: '其他' }
  return map[cat] || cat
}

// --- Charts ---
function initCharts() {
  charts.forEach(c => c.dispose())
  charts = []
  if (barRef.value) charts.push(echarts.init(barRef.value))
  if (lineRef.value) charts.push(echarts.init(lineRef.value))
  if (pieRef.value) charts.push(echarts.init(pieRef.value))
  if (cloudRef.value) charts.push(echarts.init(cloudRef.value))
  renderCharts()
}

function renderCharts() {
  if (charts.length < 4) return
  const [bar, line, pie, cloud] = charts
  const dist: any = overviewData.value.distribution
  const trend: any = overviewData.value.trend
  const keywords = overviewData.value.keywords

  bar.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 10, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: labelOrder.map(k => labels[k]), axisLabel: { rotate: 20 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: labelOrder.map(k => dist[k] || 0), itemStyle: { color: (p: any) => colors[labelOrder[p.dataIndex]] } }]
  })

  const lineData = Object.entries(trend).sort((a: any, b: any) => a[0] > b[0] ? 1 : -1)
  line.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: lineData.map(([d]) => d) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: lineData.map(([_, v]) => v), smooth: true, areaStyle: { color: 'rgba(59, 130, 246, 0.1)' }, itemStyle: { color: '#3b82f6' } }]
  })

  pie.setOption({
    tooltip: { trigger: 'item' },
    series: [{ 
      type: 'pie', 
      radius: ['40%', '70%'], 
      center: ['50%', '50%'], 
      data: labelOrder.map(k => ({ name: labels[k], value: dist[k] || 0 })),
      itemStyle: { color: (p: any) => colors[labelOrder[p.dataIndex]] || colors[Object.keys(colors).find(key => labels[key] === p.name) || 'neutral'] }
    }]
  })

  cloud.setOption({
    tooltip: { show: true },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      sizeRange: [12, 50],
      rotationRange: [-30, 30],
      gridSize: 6,
      data: keywords.slice(0, 100),
      textStyle: {
        fontFamily: 'sans-serif',
        fontWeight: 'bold',
        color: () => `rgb(${Math.round(Math.random() * 160)}, ${Math.round(Math.random() * 160)}, ${Math.round(Math.random() * 160)})`
      }
    }]
  })
}

// --- Fetch Data ---
async function fetchVisualizationData() {
  visLoading.value = true
  try {
    const [startDate, endDate] = dateRange.value || []
    const [u, n, m, vizData] = await Promise.all([
      getUserListAPI({ page: 1, size: 1 }),
      getNoticeList({ pageNum: 1, pageSize: 1 }),
      getModelList({ pageNum: 1, pageSize: 1 }),
      getAdminVizData({ startDate, endDate })
    ])
    
    metrics.value = { 
      users: u.data?.total || 0, 
      notices: n.data?.total || 0, 
      models: m.data?.total || 0,
      analysis: vizData.length
    }
    
    // Process Viz Data
    const dist: any = { happy: 0, angry: 0, sad: 0, fear: 0, surprise: 0, neutral: 0 }
    const trend: any = {}
    const freq: any = {}
    
    vizData.forEach((item: any) => {
      const l = item.predictedLabel || 'neutral'
      dist[l] = (dist[l] || 0) + 1
      if (item.createdAt) trend[String(item.createdAt).substring(0, 10)] = (trend[String(item.createdAt).substring(0, 10)] || 0) + 1
      
      // Keywords processing
      if (item.keywordsJson) {
        try { Object.entries(JSON.parse(item.keywordsJson)).forEach(([k, v]: any) => freq[k] = (freq[k] || 0) + (Number(v) || 0)) } catch {}
      } else if (item.keywords) {
        Object.entries(item.keywords).forEach(([k, v]: any) => freq[k] = (freq[k] || 0) + (Number(v) || 0))
      } else if (item.content) {
         // Fallback: simple tokenization
         const s = String(item.content)
         const tokens = [...(s.match(/[\u4e00-\u9fa5]{2,}/g) || []), ...(s.match(/[a-zA-Z]{2,}/g) || [])]
         tokens.forEach(t => freq[t] = (freq[t] || 0) + 1)
      }
    })
    
    sentimentStats.value = dist
    totalResults.value = vizData.length
    
    let keywords = Object.entries(freq).map(([name, value]) => ({ name, value: Number(value) })).sort((a,b) => b.value - a.value)
    if (keywords.length === 0) keywords = await getAdminKeywordsTop({ top_k: topK.value, start_date: startDate, end_date: endDate })
    
    overviewData.value = { distribution: dist, trend, keywords }
    
    // Unified List Processing
    unifiedList.value = vizData.map((r: any) => {
       const probObj = r.probability || (r.probabilityJson ? JSON.parse(r.probabilityJson) : {})
       const maxProb = Object.values(probObj).length > 0 ? Math.max(...Object.values(probObj).map((v: any) => Number(v))) : 0
       return {
         ...r,
         probValue: maxProb,
         probability: probObj
       }
    })

    await nextTick()
    if (activeTab.value === 'overview') initCharts()
  } catch (e) { console.error(e) } finally { visLoading.value = false }
}

const paginatedVizList = computed(() => {
  let list = [...unifiedList.value]
  list.sort((a, b) => {
    const idA = Number(a.id); const idB = Number(b.id)
    return vizSortOrder.value === 'asc' ? idA - idB : idB - idA
  })
  const start = (vizCurrentPage.value - 1) * vizPageSize.value
  return list.slice(start, start + vizPageSize.value)
})

const toggleVizSort = () => { vizSortOrder.value = vizSortOrder.value === 'asc' ? 'desc' : 'asc' }
const openResultPopup = (row: any) => { currentResult.value = row; resultDetailVisible.value = true }
const handleDeleteResult = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定删除这条分析记录吗?', '提示', { type: 'warning' })
    await deleteResult(row.id)
    ElMessage.success('删除成功')
    resultDetailVisible.value = false
    fetchVisualizationData()
  } catch (e) {}
}

const applyFilters = () => { vizCurrentPage.value = 1; fetchVisualizationData() }

// --- CRUD Operations (Simplified) ---
async function fetchData(type: 'notices'|'models'|'feedbacks') {
  const loadingMap = { notices: noticesLoading, models: modelsLoading, feedbacks: feedbacksLoading }
  loadingMap[type].value = true
  try {
    if (type === 'notices') {
      const res = await getNoticeList({ pageNum: noticePagination.currentPage, pageSize: noticePagination.pageSize })
      notices.value = res.data?.records || []; noticePagination.total = res.data?.total || 0
    } else if (type === 'models') {
      const res = await getModelList({ pageNum: modelPagination.currentPage, pageSize: modelPagination.pageSize })
      models.value = res.data?.records || []; modelPagination.total = res.data?.total || 0
    } else if (type === 'feedbacks') {
      const res = await getAllFeedbacks({ pageNum: feedbackPagination.currentPage, pageSize: feedbackPagination.pageSize, status: feedbackFilters.status, category: feedbackFilters.category })
      feedbacks.value = res.data?.records || []; feedbackPagination.total = res.data?.total || 0
    }
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '获取数据失败') }
  finally { loadingMap[type].value = false }
}

// Notice Actions
const showCreateNoticeDialog = () => { Object.assign(currentNotice, { id: undefined, title: '', content: '', type: 'SYSTEM', status: 'VISIBLE' }); noticeDialogVisible.value = true }
const editNotice = (row: any) => { Object.assign(currentNotice, row); noticeDialogVisible.value = true }
const saveNotice = async () => {
  if (!noticeFormRef.value) return
  await noticeFormRef.value.validate(async (valid) => {
    if (!valid) return
    try { currentNotice.id ? await updateNotice(currentNotice.id, currentNotice) : await createNotice(currentNotice); ElMessage.success('保存成功'); noticeDialogVisible.value = false; fetchData('notices') } catch (e) { ElMessage.error('保存失败') }
  })
}
const deleteNoticeRow = (row: any) => ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }).then(async () => { await deleteNotice(row.id); ElMessage.success('删除成功'); fetchData('notices') })

const toggleNoticeStatus = async (row: any) => {
  const originalStatus = row.status
  try {
    row.status = row.status === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE'
    await updateNotice(row.id, {
      title: row.title,
      content: row.content,
      type: row.type,
      status: row.status
    })
    ElMessage.success(row.status === 'VISIBLE' ? '公告已显示' : '公告已隐藏')
  } catch (e) {
    row.status = originalStatus
    ElMessage.error('操作失败')
  }
}

// Model Actions
const showCreateModelDialog = () => { Object.assign(currentModel, { id: undefined, modelName: '', modelType: '', version: '', description: '', modelFilePath: '' }); modelDialogVisible.value = true }
const editModel = (row: any) => { Object.assign(currentModel, row); modelDialogVisible.value = true }
const saveModel = async () => {
  if (!modelFormRef.value) return
  await modelFormRef.value.validate(async (valid) => {
    if (!valid) return
    try { await createModel(currentModel); ElMessage.success('保存成功'); modelDialogVisible.value = false; fetchData('models') } catch (e) { ElMessage.error('保存失败') }
  })
}
const activateModelRow = async (row: any) => { try { await activateModel(row.id); ElMessage.success('已激活'); fetchData('models') } catch (e) { ElMessage.error('激活失败') } }
const toggleModelStatus = async (row: any) => {
  const originalStatus = row.status
  try {
    const nextStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    row.status = nextStatus
    if (nextStatus === 'ACTIVE') {
      await activateModel(row.id)
      ElMessage.success('已激活')
    } else {
      await deactivateModel(row.id)
      ElMessage.success('已停用')
    }
    fetchData('models')
  } catch (e) {
    row.status = originalStatus
    ElMessage.error('操作失败')
  }
}
const deleteModelRow = (row: any) => ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }).then(async () => { await deleteModel(row.id); ElMessage.success('删除成功'); fetchData('models') })

// Feedback Actions
const openReplyDialog = (row: any) => { currentFeedback.value = row; replyText.value = row.adminReply || ''; replyDialogVisible.value = true }
const sendReply = async () => {
  try { await replyFeedback(currentFeedback.value.id, replyText.value); ElMessage.success('回复成功'); replyDialogVisible.value = false; fetchData('feedbacks') } catch (e) { ElMessage.error('回复失败') }
}
const deleteFeedbackRow = (row: any) => ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }).then(async () => { await deleteFeedback(row.id); ElMessage.success('删除成功'); fetchData('feedbacks') })

// Lifecycle
const onResize = () => charts.forEach(c => c.resize())
watch(activeTab, (val) => {
  if (val === 'overview') nextTick(() => { initCharts(); onResize() })
})

onMounted(() => {
  fetchVisualizationData()
  fetchData('notices')
  fetchData('models')
  fetchData('feedbacks')
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach(c => c.dispose())
})
</script>

<template>
  <div class="admin-container">
    <div class="header">
      <div class="header-left">
        <h2><el-icon class="header-icon"><Odometer /></el-icon> 管理员控制台</h2>
        <p class="subtitle">系统监控与资源管理中心</p>
      </div>
    </div>

    <el-card class="main-card" :body-style="{ padding: '0', height: '100%' }">
      <el-tabs v-model="activeTab" tab-position="left" class="admin-tabs">
        
        <!-- Overview Tab -->
        <el-tab-pane name="overview">
          <template #label><span class="tab-label"><el-icon><DataLine /></el-icon> 数据概览</span></template>
          <div class="tab-content" v-loading="visLoading">
            <div class="toolbar">
              <h3>系统状态监控</h3>
              <div class="actions">
                <el-date-picker 
                  v-model="dateRange" 
                  type="daterange" 
                  range-separator="至" 
                  start-placeholder="开始日期" 
                  end-placeholder="结束日期" 
                  value-format="YYYY-MM-DD" 
                  style="margin-right: 12px; width: 260px;" 
                />
                <el-button type="primary" plain @click="applyFilters" :icon="Refresh">刷新</el-button>
              </div>
            </div>

            <!-- Metrics -->
            <el-row :gutter="20" class="mb-4">
              <el-col :span="4"> <!-- Adjusted span to fit 5 items if needed, or row wrap -->
                 <div class="metric-card users">
                    <el-icon class="icon"><User /></el-icon>
                    <div class="info"><div class="label">注册用户</div><div class="value">{{ metrics.users }}</div></div>
                 </div>
              </el-col>
              <el-col :span="5">
                 <div class="metric-card notices">
                    <el-icon class="icon"><Bell /></el-icon>
                    <div class="info"><div class="label">发布公告</div><div class="value">{{ metrics.notices }}</div></div>
                 </div>
              </el-col>
               <el-col :span="5">
                 <div class="metric-card models">
                    <el-icon class="icon"><Connection /></el-icon>
                    <div class="info"><div class="label">模型版本</div><div class="value">{{ metrics.models }}</div></div>
                 </div>
              </el-col>
              <el-col :span="5">
                 <div class="metric-card analysis">
                    <el-icon class="icon"><Reading /></el-icon>
                    <div class="info"><div class="label">分析总数</div><div class="value">{{ metrics.analysis }}</div></div>
                 </div>
              </el-col>
              <el-col :span="5">
                 <div class="metric-card sentiment">
                    <el-icon class="icon"><Sunny /></el-icon>
                    <div class="info">
                        <div class="label">主导情绪</div>
                        <div class="value" style="font-size: 18px">{{ getSentimentLabel(dominantSentiment).split(' ')[0] }} {{ getSentimentLabel(dominantSentiment).split(' ')[1] }}</div>
                    </div>
                 </div>
              </el-col>
            </el-row>

            <!-- Charts -->
            <div class="chart-grid mb-4">
              <el-card shadow="never" class="chart-card">
                 <template #header><span>📈 情感分布 (柱状)</span></template>
                 <div ref="barRef" class="chart-box"></div>
              </el-card>
              <el-card shadow="never" class="chart-card">
                 <template #header><span>📊 趋势分析</span></template>
                 <div ref="lineRef" class="chart-box"></div>
              </el-card>
              <el-card shadow="never" class="chart-card">
                 <template #header><span>🍰 情绪占比</span></template>
                 <div ref="pieRef" class="chart-box"></div>
              </el-card>
              <el-card shadow="never" class="chart-card">
                 <template #header><span>🔑 热点关键词</span></template>
                 <div ref="cloudRef" class="chart-box"></div>
              </el-card>
            </div>

            <!-- Analysis History Table -->
            <el-card class="unified-section" shadow="never">
              <template #header>
                 <div class="card-header">
                    <div class="header-left-title">
                      <span class="header-title">📋 最新全站分析记录</span>
                      <el-tag type="info" size="small">共 {{ unifiedList.length }} 条</el-tag>
                    </div>
                    <div class="header-right-actions">
                       <el-button-group>
                          <el-button size="small" :icon="Sort" @click="toggleVizSort">
                            ID {{ vizSortOrder === 'asc' ? '正序' : '倒序' }}
                          </el-button>
                          <el-button size="small" :icon="Refresh" @click="fetchVisualizationData">刷新列表</el-button>
                       </el-button-group>
                    </div>
                 </div>
              </template>
              
              <el-table 
                :data="paginatedVizList" 
                style="width: 100%" 
                @row-click="openResultPopup"
                stripe
                highlight-current-row
              >
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column prop="content" label="文本内容" min-width="250" show-overflow-tooltip />
                <el-table-column prop="predictedLabel" label="情感倾向" width="120" align="center">
                  <template #default="{ row }">
                     <el-tag :color="getSentimentColor(row.predictedLabel)" effect="dark" size="small">
                        {{ getSentimentLabel(row.predictedLabel).split(' ')[0] }}
                     </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="probValue" label="置信度" width="100" align="center">
                  <template #default="{ row }">
                     <span :style="{ color: row.probValue > 0.8 ? '#67C23A' : '#909399', fontWeight: 'bold' }">
                       {{ Math.round(row.probValue * 100) }}%
                     </span>
                  </template>
                </el-table-column>
                <el-table-column prop="username" label="用户" width="120" align="center">
                   <template #default="{ row }"><el-tag type="info" effect="plain" size="small">{{ row.username || '未知' }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="createdAt" label="时间" width="170" align="center">
                   <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center" fixed="right">
                   <template #default="{ row }">
                      <el-button type="danger" link :icon="Delete" @click.stop="handleDeleteResult(row)">删除</el-button>
                   </template>
                </el-table-column>
              </el-table>
              
              <div class="pagination">
                <el-pagination 
                  v-model:current-page="vizCurrentPage" 
                  v-model:page-size="vizPageSize" 
                  :total="unifiedList.length" 
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next, jumper" 
                  background 
                />
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- User Management -->
        <el-tab-pane name="users">
          <template #label><span class="tab-label"><el-icon><User /></el-icon> 用户管理</span></template>
          <div class="tab-content">
            <AdminUserView />
          </div>
        </el-tab-pane>

        <!-- Notices -->
        <el-tab-pane name="notices">
          <template #label><span class="tab-label"><el-icon><Bell /></el-icon> 公告管理</span></template>
          <div class="tab-content">
            <div class="toolbar">
              <el-button type="primary" @click="showCreateNoticeDialog" :icon="Plus">发布公告</el-button>
              <el-button plain @click="fetchData('notices')" :icon="Refresh">刷新</el-button>
            </div>
            <el-table :data="notices" v-loading="noticesLoading" stripe border>
              <el-table-column prop="id" label="ID" width="70" align="center" />
              <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip>
                 <template #default="{row}">
                    <span style="font-weight: 600">{{ row.title }}</span>
                 </template>
              </el-table-column>
              <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
              <el-table-column prop="type" label="类型" width="100" align="center">
                 <template #default="{row}">
                   <el-tag :type="getNoticeTypeTag(row.type)" effect="light" size="small">
                     {{ getNoticeTypeLabel(row.type) }}
                   </el-tag>
                 </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100" align="center">
                 <template #default="{row}">
                    <el-switch
                      :model-value="row.status === 'VISIBLE'"
                      @change="toggleNoticeStatus(row)"
                      active-text="可见"
                      inactive-text="隐藏"
                      inline-prompt
                      style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                    />
                 </template>
              </el-table-column>
              <el-table-column prop="publisher" label="发布人" width="120" align="center">
                 <template #default="{row}">
                    {{ row.publisher || '管理员' }}
                 </template>
              </el-table-column>
              <el-table-column label="时间信息" width="180" align="center">
                 <template #default="{row}">
                    <div style="font-size: 12px; color: #606266">发: {{ formatDate(row.createdAt) }}</div>
                    <div style="font-size: 12px; color: #909399" v-if="row.updatedAt">更: {{ formatDate(row.updatedAt) }}</div>
                 </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{row}">
                  <el-button link type="primary" @click="editNotice(row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteNoticeRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination">
              <el-pagination v-model:current-page="noticePagination.currentPage" v-model:page-size="noticePagination.pageSize" :total="noticePagination.total" layout="total, prev, pager, next" @current-change="fetchData('notices')" background />
            </div>
          </div>
        </el-tab-pane>

        <!-- Models -->
        <el-tab-pane name="models">
          <template #label><span class="tab-label"><el-icon><Connection /></el-icon> 模型管理</span></template>
          <div class="tab-content">
            <div class="toolbar">
              <el-button type="primary" @click="showCreateModelDialog" :icon="Plus">注册模型</el-button>
            </div>
            <el-table :data="models" v-loading="modelsLoading" stripe border>
              <el-table-column prop="id" label="ID" width="80" align="center" />
              <el-table-column prop="modelName" label="名称" min-width="150" />
              <el-table-column prop="modelType" label="类型" width="150" />
              <el-table-column prop="version" label="版本" width="100" align="center" />
              <el-table-column prop="status" label="状态" width="100" align="center">
                 <template #default="{row}">
                    <el-switch
                      :model-value="row.status === 'ACTIVE'"
                      @change="toggleModelStatus(row)"
                      active-text="启用"
                      inactive-text="停用"
                      inline-prompt
                      style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                    />
                 </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center" fixed="right">
                <template #default="{row}">
                  <el-button link type="primary" @click="editModel(row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteModelRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination">
              <el-pagination v-model:current-page="modelPagination.currentPage" v-model:page-size="modelPagination.pageSize" :total="modelPagination.total" layout="total, prev, pager, next" @current-change="fetchData('models')" background />
            </div>
          </div>
        </el-tab-pane>

        <!-- Feedbacks -->
        <el-tab-pane name="feedbacks">
          <template #label><span class="tab-label"><el-icon><ChatDotRound /></el-icon> 反馈管理</span></template>
          <div class="tab-content">
            <div class="toolbar">
              <div class="actions">
                <el-select v-model="feedbackFilters.status" placeholder="状态" style="width: 120px" clearable><el-option v-for="s in feedbackStatusOptions" :key="s.value" :label="s.label" :value="s.value" /></el-select>
                <el-select v-model="feedbackFilters.category" placeholder="分类" style="width: 120px" clearable class="ml-2"><el-option v-for="c in feedbackCategoryOptions" :key="c.value" :label="c.label" :value="c.value" /></el-select>
                <el-button type="primary" @click="fetchData('feedbacks')" class="ml-2">筛选</el-button>
              </div>
            </div>
            <el-table :data="feedbacks" v-loading="feedbacksLoading" stripe border>
              <el-table-column prop="id" label="ID" width="70" align="center" />
              <el-table-column prop="category" label="分类" width="100" align="center">
                 <template #default="{row}"><el-tag type="info" effect="plain" size="small">{{ getFeedbackCategoryLabel(row.category) }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="content" label="反馈内容" min-width="200" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="100" align="center">
                 <template #default="{row}"><el-tag :type="getFeedbackStatusType(row.status)">{{ getFeedbackStatusLabel(row.status) }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="adminReply" label="管理员回复" min-width="150" show-overflow-tooltip>
                 <template #default="{row}">
                    <span v-if="row.adminReply" style="color: #67C23A">{{ row.adminReply }}</span>
                    <span v-else style="color: #909399">-</span>
                 </template>
              </el-table-column>
              <el-table-column label="时间信息" width="170" align="center">
                 <template #default="{row}">
                    <div style="font-size: 12px; color: #606266">提: {{ formatDate(row.createdAt) }}</div>
                    <div style="font-size: 12px; color: #909399" v-if="row.repliedAt">复: {{ formatDate(row.repliedAt) }}</div>
                 </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{row}">
                  <el-button link type="primary" @click="openReplyDialog(row)">回复</el-button>
                  <el-button link type="danger" @click="deleteFeedbackRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination">
              <el-pagination v-model:current-page="feedbackPagination.currentPage" v-model:page-size="feedbackPagination.pageSize" :total="feedbackPagination.total" layout="total, prev, pager, next" @current-change="fetchData('feedbacks')" background />
            </div>
          </div>
        </el-tab-pane>

        <!-- Task Management Tab -->
        <el-tab-pane name="tasks">
          <template #label><span class="tab-label"><el-icon><List /></el-icon> 任务管理</span></template>
          <TaskHistoryView />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Dialogs -->
    <el-dialog v-model="noticeDialogVisible" title="公告信息" width="500px">
      <el-form :model="currentNotice" :rules="noticeRules" ref="noticeFormRef" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="currentNotice.title" /></el-form-item>
        <el-form-item label="类型" prop="type"><el-select v-model="currentNotice.type" style="width:100%"><el-option v-for="t in noticeTypes" :key="t.value" :label="t.label" :value="t.value" /></el-select></el-form-item>
        <el-form-item label="状态" prop="status"><el-select v-model="currentNotice.status" style="width:100%"><el-option v-for="s in noticeStatuses" :key="s.value" :label="s.label" :value="s.value" /></el-select></el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="currentNotice.content" type="textarea" rows="6" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="noticeDialogVisible=false">取消</el-button><el-button type="primary" @click="saveNotice">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="modelDialogVisible" title="模型信息" width="500px">
      <el-form :model="currentModel" :rules="modelRules" ref="modelFormRef" label-width="80px">
        <el-form-item label="名称" prop="modelName"><el-input v-model="currentModel.modelName" /></el-form-item>
        <el-form-item label="类型" prop="modelType"><el-input v-model="currentModel.modelType" /></el-form-item>
        <el-form-item label="版本" prop="version"><el-input v-model="currentModel.version" /></el-form-item>
        <el-form-item label="路径" prop="modelFilePath"><el-input v-model="currentModel.modelFilePath" /></el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="currentModel.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="modelDialogVisible=false">取消</el-button><el-button type="primary" @click="saveModel">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="replyDialogVisible" title="回复反馈" width="500px">
      <el-input v-model="replyText" type="textarea" rows="4" placeholder="请输入回复内容" />
      <template #footer><el-button @click="replyDialogVisible=false">取消</el-button><el-button type="primary" @click="sendReply">发送</el-button></template>
    </el-dialog>

    <!-- Result Detail Dialog -->
    <el-dialog v-model="resultDetailVisible" title="分析结果详情" width="600px" destroy-on-close align-center center>
      <div v-if="currentResult" class="result-detail-box">
         <div class="detail-grid">
           <div class="detail-item">
              <span class="d-label">分析用户</span>
              <span class="d-value">
                <el-tag size="small" effect="plain">{{ currentResult.username || '未知' }}</el-tag>
              </span>
           </div>
           <div class="detail-item">
              <span class="d-label">分析时间</span>
              <span class="d-value">{{ new Date(currentResult.createdAt).toLocaleString() }}</span>
           </div>
           <div class="detail-item full">
              <span class="d-label">情感倾向</span>
              <div class="d-value">
                  <el-tag :color="getSentimentColor(currentResult.predictedLabel)" effect="dark" size="large">
                    {{ getSentimentLabel(currentResult.predictedLabel) }}
                  </el-tag>
                  <span class="prob-tag" v-if="currentResult.probability">
                     置信度: {{ Math.round((currentResult.probability[currentResult.predictedLabel] || currentResult.probValue || 0) * 100) }}%
                  </span>
              </div>
           </div>
           
           <div class="detail-item full">
              <span class="d-label">概率分布</span>
              <div class="d-value probability-list" style="width: 100%">
                 <div v-for="(val, key) in currentResult.probability" :key="key" class="probability-item" style="display: flex; align-items: center; margin-bottom: 8px;">
                   <div class="prob-label" style="width: 100px; display: flex; align-items: center;">
                     <span :style="{ backgroundColor: getSentimentColor(String(key)), width: '8px', height: '8px', borderRadius: '50%', display: 'inline-block', marginRight: '6px' }"></span>
                     <span style="font-size: 13px;">{{ getSentimentLabel(String(key)).split(' ')[0] }}</span>
                   </div>
                   <el-progress :color="getSentimentColor(String(key))" :percentage="Math.round(Number(val) * 100)" :stroke-width="10" style="flex: 1; margin: 0 12px;" />
                   <span class="prob-value" style="width: 40px; text-align: right; font-size: 13px;">{{ Math.round(Number(val) * 100) }}%</span>
                 </div>
              </div>
           </div>

           <div class="detail-item full">
              <span class="d-label">关键词</span>
              <div class="d-value keyword-tags">
                 <template v-if="currentResult.keywords || currentResult.keywordsJson">
                    <el-tag v-for="(v, k) in (currentResult.keywords || (currentResult.keywordsJson?JSON.parse(currentResult.keywordsJson):{}))" :key="k" size="small" effect="plain">
                      {{ k }}
                    </el-tag>
                 </template>
                 <span v-else>-</span>
              </div>
           </div>
           <div class="detail-item full">
              <span class="d-label">文本内容</span>
              <div class="d-value content-box">{{ currentResult.content }}</div>
           </div>
         </div>
      </div>
      <template #footer>
         <span class="dialog-footer">
            <el-button type="danger" :icon="Delete" @click="handleDeleteResult(currentResult)">删除记录</el-button>
            <el-button @click="resultDetailVisible = false">关闭</el-button>
         </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-container { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 60px); display: flex; flex-direction: column; }
.header { margin-bottom: 20px; }
.header h2 { margin: 0; font-size: 24px; display: flex; align-items: center; gap: 10px; color: #303133; }
.subtitle { color: #909399; font-size: 14px; margin-left: 34px; }
.header-icon { color: #409EFF; }

.main-card { flex: 1; display: flex; flex-direction: column; overflow: hidden; border-radius: 8px; }
.admin-tabs { height: 100%; border: none; }
:deep(.el-tabs__header.is-left) { background: #fff; border-right: 1px solid #f0f0f0; width: 200px; padding-top: 20px; }
:deep(.el-tabs__item) { height: 50px; justify-content: flex-start; padding-left: 24px !important; font-size: 15px; }
:deep(.el-tabs__item.is-active) { background: #ecf5ff; border-right: 3px solid #409EFF; color: #409EFF; }
.tab-label { display: flex; align-items: center; gap: 8px; }
.tab-content { padding: 24px; height: 100%; overflow-y: auto; box-sizing: border-box; }

.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px; border: 1px solid #ebeef5; }
.toolbar h3 { margin: 0; font-size: 16px; font-weight: 600; }
.actions { display: flex; gap: 12px; }

.metric-card { padding: 20px; border-radius: 12px; display: flex; align-items: center; gap: 20px; color: #fff; transition: transform 0.2s; }
.metric-card:hover { transform: translateY(-4px); }
.metric-card.users { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.metric-card.notices { background: linear-gradient(135deg, #10b981, #059669); }
.metric-card.models { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.metric-card.analysis { background: linear-gradient(135deg, #6366f1, #4338ca); }
.metric-card.sentiment { background: linear-gradient(135deg, #f59e0b, #d97706); }
.metric-card .icon { font-size: 32px; opacity: 0.8; }
.metric-card .label { font-size: 14px; opacity: 0.9; }
.metric-card .value { font-size: 28px; font-weight: 700; }

.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.chart-box { height: 300px; }
.mb-4 { margin-bottom: 20px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.user-cell { display: flex; align-items: center; gap: 8px; }
.ml-2 { margin-left: 8px; }

.unified-section { margin-top: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left-title { display: flex; align-items: center; gap: 10px; }
.header-title { font-weight: bold; font-size: 16px; }

.time-info-row .label { width: 36px; color: #909399; margin-right: 4px; text-align: right; display: inline-block; }
.text-truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* Detail Box */
.result-detail-box { padding: 10px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.detail-item.full { grid-column: span 2; }
.d-label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
.d-value { font-size: 14px; color: #303133; }
.content-box { background: #f8f9fa; padding: 12px; border-radius: 4px; line-height: 1.6; max-height: 200px; overflow-y: auto; }
.keyword-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.prob-tag { margin-left: 10px; font-weight: bold; color: #67C23A; }

.time-info-row { font-size: 12px; color: #606266; line-height: 1.4; display: flex; }

@media (max-width: 1200px) { .chart-grid { grid-template-columns: 1fr; } }
</style>
