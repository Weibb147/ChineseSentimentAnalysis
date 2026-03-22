<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getTaskList } from '@/api/analysis.js'
import { useRouter } from 'vue-router'
import { User, Timer, DataLine, Trophy } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const userInfo = computed(() => authStore.userInfo || {})

const recentTasks = ref<any[]>([])
const loading = ref(false)
const stats = ref({
  totalTasks: 0,
  completedTasks: 0,
  totalWords: 0
})

const fetchHistory = async () => {
  loading.value = true
  try {
    const res = await getTaskList({ pageNum: 1, pageSize: 10 })
    if (res.code === 0 && res.data) {
      recentTasks.value = res.data.records || []
      stats.value.totalTasks = res.data.total || 0
      // Mock stats for demo
      stats.value.completedTasks = res.data.total || 0
      stats.value.totalWords = recentTasks.value.reduce((acc, curr) => acc + (curr.taskName?.length || 0), 0) * 100 // Estimate
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goToAnalysis = () => {
  router.push('/predict')
}

onMounted(() => {
  if (authStore.token) {
    fetchHistory()
  }
})
</script>

<template>
  <div class="profile-container">
    <div class="profile-header">
      <div class="user-info">
        <el-avatar :size="80" :icon="User" class="user-avatar" />
        <div class="user-meta">
          <h1>{{ userInfo.username || 'User' }}</h1>
          <p class="user-role">{{ userInfo.role || '普通用户' }}</p>
        </div>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalTasks }}</div>
          <div class="stat-label">总任务数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.completedTasks }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <el-card class="action-card hover-effect">
        <template #header>
          <div class="card-header">
            <h3>快捷操作</h3>
          </div>
        </template>
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="goToAnalysis">发起新分析</el-button>
          <el-button size="large">修改密码</el-button>
        </div>
      </el-card>

      <el-card class="history-card hover-effect">
        <template #header>
          <div class="card-header">
            <h3>最近活动</h3>
            <el-button link type="primary" @click="fetchHistory">刷新</el-button>
          </div>
        </template>
        <el-table :data="recentTasks" style="width: 100%" v-loading="loading">
          <el-table-column prop="taskName" label="任务名称" min-width="180" />
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ new Date(row.createdAt).toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'FINISHED' || row.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
                {{ row.status === 'FINISHED' || row.status === 'COMPLETED' ? '完成' : '处理中' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.profile-header {
  background: white;
  border-radius: 16px;
  padding: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 32px;
}

.user-meta h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.user-role {
  margin: 0;
  color: #909399;
  background: #f5f7fa;
  padding: 4px 12px;
  border-radius: 12px;
  display: inline-block;
  font-size: 12px;
}

.header-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
}

.hover-effect {
  transition: all 0.3s;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 6px rgba(0,0,0,0.02);
}

.hover-effect:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.05);
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-buttons .el-button {
  width: 100%;
  margin: 0;
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .profile-header {
    flex-direction: column;
    text-align: center;
    gap: 24px;
  }
  
  .user-info {
    flex-direction: column;
  }
}
</style>
