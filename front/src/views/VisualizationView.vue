<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, List, DataLine, Sunny, ChatDotRound, Delete, Sort } from '@element-plus/icons-vue'
import { getTaskList, getTaskResults, getUserVizData, getWordCloudData, deleteTask, deleteResult } from '@/api/analysis.js'
import { getAdminVizData, getAdminKeywordsTop } from '@/api/adminAnalysis.js'
import { useAuthStore } from '@/stores/auth'
import * as echarts from 'echarts'
import 'echarts-wordcloud'

const loading = ref(false)
const sentimentStats = ref<Record<string, number>>({
  happy: 0,
  angry: 0,
  sad: 0,
  fear: 0,
  surprise: 0,
  neutral: 0
})
const totalResults = ref(0)
const totalTasks = ref(0)
const recentTexts = ref<Array<{ content: string; label: string; prob: number }>>([])
const dateCounts = ref<Record<string, number>>({})
const keywordMap = ref<Record<string, number>>({})
const topKeywords = ref<Array<[string, number]>>([])
const coreHotWord = computed(() => {
  const entries = topKeywords.value.length > 0 ? topKeywords.value : Object.entries(keywordMap.value).map(([k, v]) => [k, Number(v)] as [string, number])
  for (const [rawWord] of entries) {
    const word = normalizeKeyword(rawWord)
    if (!word) continue
    if (isStopKeyword(word)) continue
    return word
  }
  return '-'
})
const historyList = ref<any[]>([])
const unifiedList = ref<any[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailResults = ref<any[]>([])
const currentTask = ref<any>(null)
const resultDetailVisible = ref(false)
const currentResult = ref<any>(null)
const wordCloudData = ref<Array<{ name: string; value: number }>>([])
const dateRange = ref<[string, string] | null>(null)
const topK = ref(100)
const taskNameFilter = ref('')

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

const normalizeKeyword = (val: any) => {
  if (val == null) return ''
  let s = String(val).trim().toLowerCase()
  if (!s) return ''
  s = s.replace(/[\s\p{P}\p{S}]+/gu, '')
  if (!s) return ''
  if (/^\d+$/.test(s)) return ''
  if (/^(https?|www)$/.test(s)) return ''
  if (/^(txt|csv|xlsx|xls|pdf|doc|docx|png|jpg|jpeg)$/.test(s)) return ''
  return s
}

const isStopKeyword = (word: string) => {
  if (!word) return true
  const s = word.toLowerCase()
  if (/^[a-z]+$/.test(s) && s.length < 3) return true
  if (/^[\u4e00-\u9fa5]+$/.test(s) && s.length < 2) return true
  if (/^[a-z0-9]+$/.test(s) && s.length < 3) return true
  const stop = new Set([
    'the','and','for','with','this','that','from','are','was','were','you','your','have','has','had','not','but','can','will','would','should','could','its','into','about','over','then','than','just','like','very','also','only','more','most','some','any','all','one','two','get','got','make','made','use','used',
    '一个','我们','你们','他们','她们','它们','因为','所以','但是','如果','就是','不是','可以','不会','应该','还是','然后','这个','那个','这里','那里','什么','怎么','为什么','已经','没有','真的','可能','感觉','非常','特别','比较','一下','一直','还有','以及','自己','目前','今天'
  ])
  return stop.has(s)
}

const labelOrder = ['happy', 'angry', 'sad', 'fear', 'surprise', 'neutral']

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.userInfo?.role === 'ADMIN')
const userId = computed(() => authStore.userInfo?.id)

const colors: Record<string, string> = {
  happy: '#22c55e',
  angry: '#f97316',
  sad: '#3b82f6',
  fear: '#a855f7',
  surprise: '#eab308',
  neutral: '#94a3b8'
}

const labels: Record<string, string> = {
  happy: '😊 积极 (Happy)',
  angry: '😡 愤怒 (Angry)',
  sad: '😢 悲伤 (Sad)',
  fear: '😱 恐惧 (Fear)',
  surprise: '😲 惊奇 (Surprise)',
  neutral: '😐 中性 (Neutral)'
}

const dominantSentiment = computed(() => {
  let maxKey = 'neutral'
  let maxVal = -1
  for (const [key, val] of Object.entries(sentimentStats.value)) {
    if (val > maxVal) {
      maxVal = val
      maxKey = key
    }
  }
  return maxKey
})

const barChartRef = ref<HTMLDivElement | null>(null)
const columnChartRef = ref<HTMLDivElement | null>(null)
const lineChartRef = ref<HTMLDivElement | null>(null)
const pieChartRef = ref<HTMLDivElement | null>(null)
const sentimentPieChartRef = ref<HTMLDivElement | null>(null)
let barChart: echarts.ECharts | null = null
let columnChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let sentimentPieChart: echarts.ECharts | null = null

const disposeCharts = () => {
  [barChart, columnChart, lineChart, pieChart, sentimentPieChart].forEach((c) => c?.dispose())
  barChart = columnChart = lineChart = pieChart = sentimentPieChart = null
}

const renderCharts = () => {
  disposeCharts()
  if (barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (columnChartRef.value) columnChart = echarts.init(columnChartRef.value)
  if (lineChartRef.value) lineChart = echarts.init(lineChartRef.value)
  if (pieChartRef.value) pieChart = echarts.init(pieChartRef.value)
  if (sentimentPieChartRef.value) sentimentPieChart = echarts.init(sentimentPieChartRef.value)

  const counts = labelOrder.map((k) => sentimentStats.value[k] || 0)

  barChart?.setOption({
    grid: { left: 80, right: 20, top: 20, bottom: 20 },
    xAxis: { type: 'value', axisLabel: { color: '#606266' } },
    yAxis: { type: 'category', data: labelOrder.map((k) => labels[k]), axisLabel: { color: '#303133' } },
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'bar',
        data: counts.map((v, idx) => ({ value: v, itemStyle: { color: colors[labelOrder[idx]] } })),
        barWidth: 20
      }
    ]
  })

  columnChart?.setOption({
    grid: { left: 40, right: 10, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: labelOrder.map((k) => labels[k]), axisLabel: { rotate: 20 } },
    yAxis: { type: 'value' },
    tooltip: { trigger: 'axis' },
    series: [
      {
        type: 'bar',
        data: counts,
        itemStyle: {
          color: (params: any) => colors[labelOrder[params.dataIndex]]
        }
      }
    ]
  })

  const dates = Object.keys(dateCounts.value).sort()
  lineChart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'line',
        smooth: true,
        data: dates.map((d) => dateCounts.value[d] || 0),
        areaStyle: { color: 'rgba(79, 70, 229, 0.08)' },
        lineStyle: { color: '#4f46e5' },
        itemStyle: { color: '#4f46e5' }
      }
    ]
  })

  const keywordEntries = Object.entries(keywordMap.value)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 100)

  const wcData = wordCloudData.value.length > 0
    ? wordCloudData.value
    : keywordEntries.map(([name, value]) => ({ name, value }))

  const wcEntries = wcData
    .map((d: any) => [normalizeKeyword(d.name), Number(d.value || 0)] as [string, number])
    .filter(([k]) => k && !isStopKeyword(k))
    .sort((a, b) => b[1] - a[1])
    .slice(0, 100)
  topKeywords.value = wcEntries.length > 0 ? wcEntries : keywordEntries

  pieChart?.setOption({
    tooltip: { show: true },
    series: [
      {
        type: 'wordCloud',
        shape: 'circle',
        left: 'center',
        top: 'center',
        width: '100%',
        height: '100%',
        sizeRange: [14, 60],
        rotationRange: [-30, 30],
        rotationStep: 30,
        gridSize: 6,
        drawOutOfBound: false,
        layoutAnimation: true,
        textStyle: {
          fontFamily: 'sans-serif',
          fontWeight: 'bold',
          color: function () {
            return 'rgb(' + [
              Math.round(Math.random() * 160),
              Math.round(Math.random() * 160),
              Math.round(Math.random() * 160)
            ].join(',') + ')';
          }
        },
        emphasis: {
          focus: 'self',
          textStyle: {
            textShadowBlur: 10,
            textShadowColor: '#333'
          }
        },
      data: wcEntries.map(([name, value]) => ({ name, value }))
    }
    ]
  })

  const sentimentPieData = labelOrder.map((k) => ({
    name: labels[k],
    value: sentimentStats.value[k] || 0
  }))
  sentimentPieChart?.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '情绪分布',
        type: 'pie',
        radius: ['35%', '60%'],
        data: sentimentPieData,
        label: { formatter: '{b}: {d}%' }
      }
    ]
  })
}

const aggregateDate = (dateStr?: string) => {
  if (!dateStr) return
  const d = dateStr.substring(0, 10)
  if (!dateCounts.value[d]) dateCounts.value[d] = 0
  dateCounts.value[d] += 1
}

const collectKeywords = (keywords?: any) => {
  if (!keywords) return
  const kwObj = parseJsonObject(keywords)
  Object.entries(kwObj).forEach(([k, v]) => {
    keywordMap.value[k] = (keywordMap.value[k] || 0) + (Number(v) || 0)
  })
}

const extractLocalKeywordsFromContents = (contents: string[]) => {
  const freq: Record<string, number> = {}
  for (const c of contents) {
    if (!c) continue
    const s = String(c).toLowerCase()
    const tokensCn = s.match(/[\u4e00-\u9fa5]{2,}/g) || []
    const tokensEn = s.match(/[a-zA-Z]{2,}/g) || []
    const tokens = [...tokensCn, ...tokensEn]
    for (const t of tokens) {
      const k = normalizeKeyword(t)
      if (!k || isStopKeyword(k)) continue
      freq[k] = (freq[k] || 0) + 1
    }
  }
  const entries = Object.entries(freq).sort((a,b)=>b[1]-a[1]).slice(0, 200)
  keywordMap.value = {}
  for (const [k,v] of entries) keywordMap.value[k] = v
  wordCloudData.value = entries.map(([name, value]) => ({ name, value }))
}

const fetchStats = async () => {
  loading.value = true
  sentimentStats.value = { happy: 0, angry: 0, sad: 0, fear: 0, surprise: 0, neutral: 0 }
  totalResults.value = 0
  totalTasks.value = 0
  recentTexts.value = []
  dateCounts.value = {}
  keywordMap.value = {}
  historyList.value = []
  wordCloudData.value = []

  try {
    const [startDate, endDate] = dateRange.value || []
    // 1. 获取可视化数据 (Unified Data Visualization Method)
    let vizData: any[] = []
    try {
      const params = isAdmin.value ? { startDate, endDate } : { user_id: userId.value, startDate, endDate }
      vizData = isAdmin.value ? await getAdminVizData(params) : await getUserVizData(params)

      // 客户端聚合计算
      vizData.forEach((item: any) => {
        // 统计情感分布
        const label = item.predictedLabel || 'neutral'
        if (sentimentStats.value[label] !== undefined) {
          sentimentStats.value[label]++
        }

        // 统计每日趋势
        if (item.createdAt) {
          const d = item.createdAt.substring(0, 10)
          dateCounts.value[d] = (dateCounts.value[d] || 0) + 1
        }

        // 统计关键词（优先使用模型抽取 keywords，再兜底从内容提取）
        const kw = parseJsonObject(item.keywordsJson || item.keywords)
        const kwEntries = kw && typeof kw === 'object' ? Object.entries(kw) : []
        if (kwEntries.length > 0) {
          kwEntries.forEach(([k, v]) => {
            const kk = normalizeKeyword(k)
            if (!kk || isStopKeyword(kk)) return
            keywordMap.value[kk] = (keywordMap.value[kk] || 0) + (Number(v) || 0)
          })
        } else if (item.content) {
          const s = String(item.content)
          const tokensCn = s.match(/[\u4e00-\u9fa5]{2,}/g) || []
          const tokensEn = s.match(/[a-zA-Z]{2,}/g) || []
          const tokens = [...tokensCn, ...tokensEn]
          tokens.forEach((t) => {
            const k = normalizeKeyword(t)
            if (!k || isStopKeyword(k)) return
            keywordMap.value[k] = (keywordMap.value[k] || 0) + 1
          })
        }
      })

      // 准备词云数据
      const keywordEntries = Object.entries(keywordMap.value)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 100)
      wordCloudData.value = keywordEntries.map(([name, value]) => ({ name, value }))

      // 如果关键词为空，进行兜底
      if (wordCloudData.value.length === 0) {
        if (isAdmin.value) {
          try {
            const kwList = await getAdminKeywordsTop({ top_k: topK.value })
            if (kwList.length > 0) {
              wordCloudData.value = kwList.map((item: any) => ({ name: item.name || item.keyword || '', value: Number(item.value || item.count || 0) }))
            }
          } catch (e) {}
        } else {
          try {
            const wcList = await getWordCloudData({ user_id: userId.value, top_k: topK.value })
            if (wcList.length > 0) {
              wordCloudData.value = wcList.map((d: any) => ({ name: d.name, value: Number(d.value || 0) }))
            }
          } catch (e) {}
        }
      }

      loading.value = false
      await nextTick()
      renderCharts()

    } catch (e) {
      console.warn('Failed to fetch visualization data', e)
      ElMessage.warning('可视化数据加载失败，正在尝试加载列表')
    }

      // 使用 FastAPI 可视化数据填充列表与最近样本
    totalResults.value = vizData.length
    try {
      const taskRes = await getTaskList({ pageNum: 1, pageSize: 1, startDate, endDate })
      totalTasks.value = taskRes.data?.total || 0
    } catch (e) {
      totalTasks.value = 0
    }
    
    // 统一列表逻辑：基于 vizData (最新样本详情)
    // 移除 .slice(0, 50) 限制，改由前端分页控制
      unifiedList.value = vizData.map((r: any) => {
          const probObj = parseJsonObject(r.probabilityJson || r.probability)
          
          const maxProb = Object.values(probObj).length > 0 
              ? Math.max(...Object.values(probObj).map(v => Number(v))) 
              : 0

          return {
              ...r,
              id: r.id,
              isResult: true,
              taskId: r.taskId,
              taskName: r.taskName,
              taskType: r.taskType,
              content: r.content || `记录 #${r.id}`,
              predictedLabel: r.predictedLabel,
              probability: probObj,
              probValue: maxProb,
              createdAt: r.createdAt,
              username: r.username,
              keywords: parseJsonObject(r.keywordsJson || r.keywords),
              // 兼容字段
              label: r.predictedLabel,
              prob: maxProb
          }
      })
      
      // 为了保持兼容性，historyList 可以保留给旧逻辑，或者如果不在模板中使用则忽略
      // 这里为了简单，不再单独请求 Task List，而是使用统一列表
      historyList.value = [] 
      recentTexts.value = []

  } catch (error: any) {
    console.error('Visualization error:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 分页与排序逻辑
const currentPage = ref(1)
const pageSize = ref(20)
const sortOrder = ref<'asc' | 'desc'>('desc')

const paginatedList = computed(() => {
  let list = [...unifiedList.value]
  
  // 1. 排序 (基于ID)
  list.sort((a, b) => {
    const idA = Number(a.id)
    const idB = Number(b.id)
    return sortOrder.value === 'asc' ? idA - idB : idB - idA
  })
  
  // 2. 分页
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return list.slice(start, end)
})

const toggleSort = () => {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

const indexMethod = (index: number) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

const applyFilters = () => { fetchStats() }

const handleResize = () => {
  barChart?.resize()
  columnChart?.resize()
  lineChart?.resize()
  pieChart?.resize()
  sentimentPieChart?.resize()
}

const statusType = (status?: string) => {
  const map: Record<string, string> = { FINISHED: 'success', RUNNING: 'warning', FAILED: 'danger', PENDING: 'info', COMPLETED: 'success' }
  return map[status || ''] || 'info'
}

const getSentimentLabel = (sentiment: string) => labels[sentiment] || sentiment
const getSentimentColor = (sentiment: string) => colors[sentiment] || '#909399'

const openDetail = async (task: any) => {
  if (task.isResult && task.rawResult) {
    openResultPopup(task.rawResult)
    return
  }

  // 如果是任务，且是单条分析任务，尝试直接显示结果详情
  if (!task.isResult && task.rawResult) {
      openResultPopup(task.rawResult)
      return
  }
  
  // 否则打开任务详情抽屉（通常用于批量任务）
  detailVisible.value = true
  currentTask.value = task

  detailLoading.value = true
  try {
    const res = await getTaskResults(task.id, { pageNum: 1, pageSize: 20 })
    detailResults.value = res.data?.records || []
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '获取详细结果失败')
  } finally {
    detailLoading.value = false
  }
}

const handleDeleteResult = async (item: any, event?: Event) => {
  event?.stopPropagation()
  try {
    const typeText = item.isResult ? '这条分析结果' : '这个分析任务'
    await ElMessageBox.confirm(`确定要删除${typeText}吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    if (item.isResult) {
        await deleteResult(item.id)
    } else {
        await deleteTask(item.id)
    }
    
    ElMessage.success('删除成功')
    
    // Close dialog if open
    resultDetailVisible.value = false
    detailVisible.value = false
    
    // Refresh
    fetchStats()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
      ElMessage.error('删除失败')
    }
  }
}

const openResultPopup = (result: any) => {
    currentResult.value = result
    resultDetailVisible.value = true
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<template>
  <div class="viz-container">
    <!-- Header -->
    <div class="header-bar">
      <div>
        <p class="sub-title">Data Insights</p>
        <h2 class="page-title">情感分析看板</h2>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/tasks')">
          <el-icon class="el-icon--left"><List /></el-icon>
          任务管理
        </el-button>
        <el-input v-model="taskNameFilter" placeholder="搜索任务/内容" style="width: 180px;" clearable />
        <el-tag v-if="isAdmin" type="danger" effect="dark" class="role-tag">管理员视图</el-tag>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-input-number v-model="topK" :min="10" :max="300" :step="10" label="关键词TopK" />
        <el-button type="primary" :icon="Refresh" circle @click="applyFilters" :loading="loading" />
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- Key Metrics Cards -->
      <div class="metrics-grid">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon task-icon"><el-icon><List /></el-icon></div>
          <div class="metric-info">
            <div class="metric-label">任务总数</div>
            <div class="metric-value">{{ totalTasks }}</div>
          </div>
        </el-card>
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon sample-icon"><el-icon><DataLine /></el-icon></div>
          <div class="metric-info">
            <div class="metric-label">样本总量</div>
            <div class="metric-value">{{ totalResults }}</div>
          </div>
        </el-card>
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon emotion-icon"><el-icon><Sunny /></el-icon></div>
          <div class="metric-info">
            <div class="metric-label">主导情绪</div>
            <div class="metric-value" :style="{ color: getSentimentColor(dominantSentiment) }">
              {{ getSentimentLabel(dominantSentiment).split(' ')[0] }}
            </div>
          </div>
        </el-card>
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon keyword-icon"><el-icon><ChatDotRound /></el-icon></div>
          <div class="metric-info">
            <div class="metric-label">核心热词</div>
            <div class="metric-value">{{ coreHotWord }}</div>
          </div>
        </el-card>
      </div>

      <!-- Main Charts Area -->
      <div class="charts-layout">
        <!-- Left Column: Trend & Distribution -->
        <div class="chart-col-main">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="card-header">
                <span class="header-title">📈 情感趋势分析</span>
              </div>
            </template>
            <div ref="lineChartRef" class="chart-box large"></div>
          </el-card>
          
          <div class="sub-charts-grid">
             <el-card shadow="never" class="chart-card">
              <template #header><span class="header-title">📊 情绪分布 (柱状)</span></template>
              <div ref="columnChartRef" class="chart-box"></div>
            </el-card>
            <el-card shadow="never" class="chart-card">
              <template #header><span class="header-title">📊 情绪分布 (条形)</span></template>
              <div ref="barChartRef" class="chart-box"></div>
            </el-card>
          </div>
        </div>

        <!-- Right Column: Pie & Keywords -->
        <div class="chart-col-side">
          <el-card shadow="never" class="chart-card">
            <template #header><span class="header-title">🍰 整体情绪占比</span></template>
            <div ref="sentimentPieChartRef" class="chart-box medium"></div>
          </el-card>
           <el-card shadow="never" class="chart-card">
            <template #header><span class="header-title">🔑 关键词分析</span></template>
            <div ref="pieChartRef" class="chart-box medium"></div>
             <div class="word-cloud-mini">
                <span
                  v-for="([k, v], idx) in topKeywords.slice(0, 10)"
                  :key="k"
                  class="wc-tag"
                  :style="{ fontSize: (12 + Math.min(v, 6)) + 'px', opacity: 1 - idx * 0.05 }"
                >
                  {{ k }}
                </span>
            </div>
          </el-card>
        </div>
      </div>

      <!-- Unified Records Section (Refactored to Table View) -->
      <el-card class="unified-section" shadow="never">
        <template #header>
           <div class="card-header">
              <div class="header-left">
                <span class="header-title">📋 最新样本分析详情</span>
                <el-tag type="info" size="small">共 {{ unifiedList.length }} 条记录</el-tag>
              </div>
              <div class="header-right">
                <el-button-group>
                   <el-button size="small" :icon="Sort" @click="toggleSort">
                     ID排序: {{ sortOrder === 'asc' ? '正序 (Oldest)' : '倒序 (Newest)' }}
                   </el-button>
                   <el-button size="small" :icon="Refresh" @click="fetchStats">刷新列表</el-button>
                </el-button-group>
              </div>
           </div>
        </template>
        
        <el-table 
          :data="paginatedList" 
          style="width: 100%" 
          @row-click="openResultPopup"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          highlight-current-row
          stripe
        >
          <el-table-column type="index" :index="indexMethod" label="序号" width="80" align="center" />
          
          <el-table-column prop="taskName" label="所属任务" min-width="120" show-overflow-tooltip>
             <template #default="{ row }">
                <el-tag v-if="row.taskType === 'BATCH' || (row.taskName && (row.taskName.endsWith('.xlsx') || row.taskName.endsWith('.csv') || row.taskName.endsWith('.txt')))" type="warning" effect="plain" size="small">
                    {{ row.taskName || '批量任务' }}
                </el-tag>
                <el-tag v-else-if="row.taskName" type="primary" effect="plain" size="small">
                    {{ row.taskName }}
                </el-tag>
                <span v-else style="color: #909399; font-size: 12px;">单条分析</span>
             </template>
          </el-table-column>

          <el-table-column prop="content" label="文本内容" min-width="300" show-overflow-tooltip />
          
          <el-table-column prop="predictedLabel" label="情感倾向" width="140">
            <template #default="{ row }">
               <el-tag :color="getSentimentColor(row.predictedLabel)" effect="dark" size="small">
                  {{ getSentimentLabel(row.predictedLabel).split(' ')[0] }} {{ getSentimentLabel(row.predictedLabel).split(' ')[1] || '' }}
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

          <el-table-column prop="username" label="分析用户" width="120" align="center">
             <template #default="{ row }">
                <el-tag type="info" effect="plain" size="small">{{ row.username || '未知' }}</el-tag>
             </template>
          </el-table-column>
          
          <el-table-column prop="createdAt" label="分析时间" width="180" align="center">
             <template #default="{ row }">
                <span style="font-size: 13px; color: #909399">{{ new Date(row.createdAt).toLocaleString() }}</span>
             </template>
          </el-table-column>
          
          <el-table-column label="操作" width="100" align="center" fixed="right">
             <template #default="{ row }">
                <el-button 
                  type="danger" 
                  link 
                  :icon="Delete" 
                  @click.stop="handleDeleteResult(row)"
                >
                  删除
                </el-button>
             </template>
          </el-table-column>
        </el-table>
        
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="unifiedList.length"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </template>
  </div>

  <!-- Detail Drawer -->
  <el-drawer v-model="detailVisible" size="40%" :title="`记录 #${currentTask?.id} 结果明细`" destroy-on-close>
    <div v-if="detailLoading" class="drawer-loading">
      <el-skeleton :rows="6" animated />
    </div>
    <div v-else class="drawer-content">
      <div v-if="currentTask?.isResult" class="single-result-view">
         <div class="detail-row">
            <span class="label">分析用户:</span> <strong>{{ currentTask.username }}</strong>
         </div>
          <div class="detail-row">
            <span class="label">分析时间:</span> {{ currentTask.createdAt }}
         </div>
         <div class="detail-actions" style="margin: 16px 0">
            <el-button type="danger" :icon="Delete" @click="handleDeleteResult(currentTask)">删除此记录</el-button>
         </div>
      </div>

      <el-empty v-if="detailResults.length === 0" description="暂无结果" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="r in detailResults"
          :key="r.id"
          :timestamp="r.createdAt"
          placement="top"
          :color="getSentimentColor(r.predictedLabel)"
        >
          <el-card shadow="hover" class="detail-item-card">
            <div class="detail-card-body">
              <div class="detail-head">
                <el-tag :color="getSentimentColor(r.predictedLabel)" effect="dark" size="small">
                  {{ getSentimentLabel(r.predictedLabel).split(' ')[0] }}
                </el-tag>
                <span class="prob-text">
                  置信度 {{ Math.round((r.probability?.[r.predictedLabel] || 0) * 100) }}%
                </span>
              </div>
              <p class="result-text">{{ r.content }}</p>
              <div v-if="r.keywords" class="keyword-list">
                <el-tag v-for="(freq, key) in r.keywords" :key="key" size="small" effect="plain" type="info">
                  {{ key }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-drawer>

  <!-- Result Dialog -->
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
                <span class="prob-tag" v-if="currentResult.probabilityJson || currentResult.probability">
                   置信度: {{ (() => {
                      try {
                        const p = currentResult.probability || JSON.parse(currentResult.probabilityJson || '{}');
                        const val = p[currentResult.predictedLabel] || currentResult.probValue || 0;
                        return Math.round(val * 100) + '%'
                      } catch(e) { return '-' }
                   })() }}
                </span>
            </div>
         </div>
         
         <!-- Probability Distribution -->
         <div class="detail-item full">
            <span class="d-label">概率分布</span>
            <div class="d-value probability-list" style="width: 100%">
               <div
                   v-for="(val, key) in (() => {
                      try {
                        const p = currentResult.probability || JSON.parse(currentResult.probabilityJson || '{}');
                        return Object.entries(p)
                          .sort((a, b) => Number(b[1]) - Number(a[1]))
                          .reduce((acc, [k, v]) => ({ ...acc, [k]: v }), {})
                      } catch(e) { return {} }
                   })()"
                   :key="key"
                   class="probability-item"
                   style="display: flex; align-items: center; margin-bottom: 8px;"
               >
                 <div class="prob-label" style="width: 100px; display: flex; align-items: center;">
                   <span :style="{ backgroundColor: getSentimentColor(String(key)), width: '8px', height: '8px', borderRadius: '50%', display: 'inline-block', marginRight: '6px' }"></span>
                   <span style="font-size: 13px;">{{ getSentimentLabel(String(key)).split(' ')[0] }}</span>
                 </div>
                 <el-progress
                     :color="getSentimentColor(String(key))"
                     :percentage="Math.round(Number(val) * 100)"
                     :stroke-width="10"
                     :text-inside="false"
                     style="flex: 1; margin: 0 12px;"
                 />
                 <span class="prob-value" style="width: 40px; text-align: right; font-size: 13px;">{{ Math.round(Number(val) * 100) }}%</span>
               </div>
            </div>
         </div>

         <div class="detail-item full">
            <span class="d-label">关键词</span>
            <div class="d-value keyword-tags">
               <template v-if="currentResult.keywordsJson || currentResult.keywords">
                  <el-tag v-for="(v, k) in (() => { 
                      try { 
                          const kw = currentResult.keywords || JSON.parse(currentResult.keywordsJson || '{}');
                          return kw;
                      } catch(e) { return {} } 
                  })()" :key="k" size="small" effect="plain">
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
</template>

<style scoped>
.viz-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
  font-weight: 600;
}

.sub-title {
  margin: 0 0 4px 0;
  font-size: 13px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.role-tag {
  margin-right: 8px;
}

/* Metrics Grid */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.metric-card {
  border: none;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.metric-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 20px;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.task-icon { background: #e0e7ff; color: #4f46e5; }
.sample-icon { background: #dcfce7; color: #16a34a; }
.emotion-icon { background: #fef3c7; color: #d97706; }
.keyword-icon { background: #f3e8ff; color: #9333ea; }

.metric-info {
  flex: 1;
}

.metric-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

/* Charts Layout */
.charts-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.chart-col-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-col-side {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.chart-box.large { height: 350px; }
.chart-box.medium { height: 250px; }

.sub-charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.word-cloud-mini {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  justify-content: center;
}

.wc-tag {
  color: #4b5563;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f3f4f6;
  font-size: 12px;
}

/* Unified Section */
.unified-section {
  border-radius: 12px;
  border: none;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 40px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}

/* Detail Dialog Styles */
.detail-container {
  padding: 20px;
}

.detail-header {
  text-align: center;
  margin-bottom: 30px;
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
}

.detail-header h3 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 20px;
}

.detail-meta {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 15px;
}

.detail-content-box {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.content-label {
  display: block;
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: bold;
}

.content-text {
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}

.detail-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
}

.info-section h4 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #606266;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}

.prob-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.prob-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prob-label {
  width: 60px;
  font-size: 13px;
}

.prob-bar-wrapper {
  flex: 1;
}

.prob-val {
  width: 45px;
  text-align: right;
  font-size: 13px;
  color: #606266;
}

.keywords-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.prob-tag {
  font-size: 14px;
  font-weight: bold;
  color: #67C23A;
  background: #f0f9eb;
  padding: 2px 8px;
  border-radius: 4px;
}

@media (max-width: 1200px) {
  .charts-layout {
    grid-template-columns: 1fr;
  }
  .sub-charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
