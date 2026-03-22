<template>
  <div class="task-history-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="title">分析任务历史</span>
            <el-tag type="info" size="small" style="margin-left: 10px">任务总数：{{ pagination.total }}</el-tag>
          </div>
          <div class="header-actions">
            <el-input 
              v-if="isAdmin"
              v-model="filters.userId" 
              placeholder="用户ID" 
              clearable 
              style="width: 120px" 
              @keyup.enter="fetchTasks(true)"
            />
            <el-input 
              v-model="filters.taskName" 
              placeholder="任务名称" 
              clearable 
              style="width: 200px" 
              @keyup.enter="fetchTasks(true)"
            />
            <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px">
              <el-option label="等待中" value="PENDING" />
              <el-option label="进行中" value="RUNNING" />
              <el-option label="完成" value="FINISHED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-select v-model="filters.taskType" placeholder="类型" clearable style="width: 120px">
              <el-option label="单条" value="SINGLE" />
              <el-option label="批量" value="BATCH" />
            </el-select>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
            />
            <el-button type="primary" @click="fetchTasks(true)">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </template>

      <!-- 任务列表表格 -->
      <el-table 
        v-loading="loading"
        :data="tasks" 
        style="width: 100%" 
        border
        stripe
      >
        <el-table-column type="index" :index="indexMethod" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户" width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="modelName" label="使用模型" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.modelName || '默认模型' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.taskType === 'BATCH' ? 'warning' : 'success'">
              {{ row.taskType === 'BATCH' ? '批量' : '单条' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度/结果" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.taskType === 'BATCH'">
               {{ row.successCount }}/{{ row.totalCount }}
            </span>
            <span v-else>
               -
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="finishedAt" label="完成时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.finishedAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewResults(row)">
              查看结果
            </el-button>
            <el-popconfirm 
              title="确定删除该任务及其所有结果吗？"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>


    <!-- 结果详情弹窗 -->
    <el-dialog
      v-model="resultsDialog.visible"
      :title="resultsDialog.title"
      width="75%"
      destroy-on-close
    >
      <el-table
        v-loading="resultsDialog.loading"
        :data="resultsDialog.data"
        style="width: 100%"
        border
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="content" label="文本内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="predictedLabel" label="情感标签" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getSentimentType(row.predictedLabel)">
              {{ row.predictedLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置信度" width="100" align="center">
          <template #default="{ row }">
             {{ getMaxProbability(row.probability) }}
          </template>
        </el-table-column>
        <el-table-column label="关键词" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="!getKeywordList(row.keywords).length">-</template>
            <template v-else>
              <el-tag
                v-for="item in getKeywordList(row.keywords)"
                :key="item.word"
                size="small"
                class="mx-1"
              >
                {{ item.word }}
              </el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="生成时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="resultsDialog.pagination.currentPage"
          v-model:page-size="resultsDialog.pagination.pageSize"
          layout="prev, pager, next"
          :total="resultsDialog.pagination.total"
          @current-change="fetchTaskResults"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getTaskList, getTaskResults, deleteTask } from '@/api/analysis'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isAdmin = computed(() => {
  const role = authStore.userInfo?.role || ''
  return String(role).toUpperCase() === 'ADMIN'
})

const loading = ref(false)
const tasks = ref([])
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  userId: '',
  taskName: '',
  status: '',
  taskType: ''
})

const dateRange = ref(null)

const resetFilters = () => {
  filters.userId = ''
  filters.taskName = ''
  filters.status = ''
  filters.taskType = ''
  dateRange.value = null
  fetchTasks(true)
}

const resultsDialog = reactive({
  visible: false,
  title: '任务结果详情',
  loading: false,
  data: [],
  currentTaskId: null,
  pagination: {
    currentPage: 1,
    pageSize: 10,
    total: 0
  }
})

// 初始化
onMounted(() => {
  fetchTasks()
})

// 获取任务列表
const fetchTasks = async (resetPage = false) => {
  if (resetPage) {
    pagination.currentPage = 1
  }
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value || []
    const userIdNum = Number(filters.userId)
    const res = await getTaskList({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      taskName: filters.taskName,
      status: filters.status,
      taskType: filters.taskType,
      userId: filters.userId && !Number.isNaN(userIdNum) ? userIdNum : undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    tasks.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error('获取任务列表失败')
  } finally {
    loading.value = false
  }
}

// 删除任务
const handleDelete = async (row) => {
  try {
    const res = await deleteTask(row.id)
    ElMessage.success(res.message || '删除成功')
    const shouldBackPage = tasks.value.length === 1 && pagination.currentPage > 1
    if (shouldBackPage) pagination.currentPage -= 1
    fetchTasks()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 查看结果
const viewResults = (row) => {
  resultsDialog.currentTaskId = row.id
  resultsDialog.title = `任务详情: ${row.taskName}`
  resultsDialog.pagination.currentPage = 1
  resultsDialog.pagination.total = 0
  resultsDialog.data = []
  resultsDialog.visible = true
  fetchTaskResults()
}

const parseJsonObject = (val) => {
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

// 获取任务具体结果
const fetchTaskResults = async () => {
  if (!resultsDialog.currentTaskId) return
  
  resultsDialog.loading = true
  try {
    const res = await getTaskResults(resultsDialog.currentTaskId, {
      pageNum: resultsDialog.pagination.currentPage,
      pageSize: resultsDialog.pagination.pageSize
    })
    resultsDialog.data = (res.data?.records || []).map(r => ({
      ...r,
      probability: parseJsonObject(r.probabilityJson || r.probability),
      keywords: parseJsonObject(r.keywordsJson || r.keywords)
    }))
    resultsDialog.pagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error('获取结果详情失败')
  } finally {
    resultsDialog.loading = false
  }
}

// 辅助函数
const indexMethod = (index) => {
  return (pagination.currentPage - 1) * pagination.pageSize + index + 1
}

const getStatusType = (status) => {
  const map = {
    'PENDING': 'info',
    'RUNNING': 'primary',
    'FINISHED': 'success',
    'FAILED': 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    'PENDING': '等待中',
    'RUNNING': '进行中',
    'FINISHED': '已完成',
    'FAILED': '失败'
  }
  return map[status] || status
}

const getSentimentType = (label) => {
  if (label === 'Positive' || label === 'positive') return 'success'
  if (label === 'Negative' || label === 'negative') return 'danger'
  return 'info'
}

const getMaxProbability = (probMap) => {
  if (!probMap) return '-'
  const vals = Object.values(probMap)
  if (vals.length === 0) return '-'
  return (Math.max(...vals) * 100).toFixed(2) + '%'
}

const formatDateTime = (val) => {
  if (!val) return '-'
  if (typeof val === 'string') {
    const s = val.trim()
    if (!s) return '-'
    return s.includes('T') ? s.replace('T', ' ').split('.')[0] : s
  }
  if (val instanceof Date && !Number.isNaN(val.getTime())) {
    const pad = (n) => String(n).padStart(2, '0')
    return `${val.getFullYear()}-${pad(val.getMonth() + 1)}-${pad(val.getDate())} ${pad(val.getHours())}:${pad(val.getMinutes())}:${pad(val.getSeconds())}`
  }
  return String(val)
}

const getKeywordList = (keywords) => {
  if (!keywords || typeof keywords !== 'object') return []
  const entries = Object.entries(keywords)
    .filter(([k]) => k && String(k).trim())
    .map(([word, score]) => ({ word: String(word), score: Number(score) }))
  entries.sort((a, b) => (Number.isFinite(b.score) ? b.score : 0) - (Number.isFinite(a.score) ? a.score : 0))
  return entries.slice(0, 12)
}

const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.currentPage = 1
  fetchTasks()
}

const handleCurrentChange = (val) => {
  pagination.currentPage = val
  fetchTasks()
}
</script>

<style scoped>
.task-history-container {
  padding: 20px;
}
.box-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left .title {
  font-size: 18px;
  font-weight: bold;
}
.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.mx-1 {
  margin-left: 0.25rem;
  margin-right: 0.25rem;
}
</style>
