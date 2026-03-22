<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>欢迎使用中文文本情感分析系统</h2>
        </div>
      </template>
      <div class="welcome-content">
        <p>您好，<strong>{{ isAuthenticated ? (userInfo?.username) : '访客' }}</strong>！</p>
        <p>本系统基于先进的RoBERTa模型，能够准确分析中文文本的情感倾向。</p>
        <div class="features">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card class="feature-card" @click="goToPredict">
                <el-icon :size="30" color="#409EFF"><EditPen /></el-icon>
                <h3>情感预测</h3>
                <p>输入中文文本，快速获得情感分析结果</p>
                <div v-if="!isAuthenticated" class="login-tag">
                  <el-tag type="warning" size="small">需登录</el-tag>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="feature-card" @click="goToHistory">
                <el-icon :size="30" color="#67C23A"><TrendCharts /></el-icon>
                <h3>情感预测记录</h3>
                <p>查看你的预测记录并实时可视化（柱状/折线/饼图、词云）</p>
                <div v-if="!isAuthenticated" class="login-tag">
                  <el-tag type="warning" size="small">需登录</el-tag>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="feature-card" @click="goToNotices">
                <el-icon :size="30" color="#E6A23C"><Bell /></el-icon>
                <h3>公告与反馈</h3>
                <p>查看平台公告，提交反馈并查看我的反馈记录</p>
              </el-card>
            </el-col>
          </el-row>
        </div>
        <div class="actions">
          <el-button type="primary" size="large" @click="goToPredict">
            {{ isAuthenticated ? '开始情感分析' : '体验情感分析' }}
          </el-button>
          <div v-if="!isAuthenticated" class="login-tip">
            <p>💡 登录后可享受完整功能：保存历史记录、个人数据统计等</p>
            <div class="login-buttons">
              <el-button type="primary" @click="router.push('/login')">立即登录</el-button>
              <span>或</span>
              <el-button type="success" @click="router.push('/register')">注册账号</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { TrendCharts, Bell, EditPen } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const isAuthenticated = computed(() => !!authStore.token)

// 统一的登录检查方法
const checkLogin = async (targetPath: string, featureName: string) => {
  if (!isAuthenticated.value) {
    // 对于预测与公告页，允许访客访问
    if (targetPath === '/predict' || targetPath === '/notices') {
      try {
        await ElMessageBox.confirm(
            targetPath === '/predict' ? '作为访客，您可以体验情感分析功能，但无法保存历史记录。建议登录后使用完整功能。' : '公告与反馈可在未登录状态访问，但提交反馈需登录。',
            '访客访问',
            {
              confirmButtonText: '继续体验',
              cancelButtonText: '立即登录',
              type: 'info',
              distinguishCancelAndClose: true
            }
        )
        // 用户选择继续体验
        router.push(targetPath)
      } catch (action) {
        if (action === 'cancel') {
          // 用户选择登录
          router.push('/login')
        }
        // 用户点击关闭按钮，不做操作
      }
    } else {
      // 对于其他功能，必须登录
      try {
        await ElMessageBox.confirm(
            `${featureName}功能需要登录后使用，是否立即登录？`,
            '登录提示',
            {
              confirmButtonText: '立即登录',
              cancelButtonText: '取消',
              type: 'warning'
            }
        )
        router.push('/login')
      } catch {
        // 用户取消，不做操作
      }
    }
  } else {
    router.push(targetPath)
  }
}

const goToPredict = () => {
  checkLogin('/predict', '情感预测')
}

const goToHistory = () => {
  checkLogin('/visualization', '情感预测记录')
}

const goToNotices = () => {
  checkLogin('/notices', '公告与反馈')
}
</script>

<style scoped>
.home-container {
  max-width: 100%;
  width: 100%;
  margin: 0;
  padding: 20px;
  min-height: calc(100vh - 80px);
}

.welcome-card {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;
}

.welcome-content {
  padding: 20px;
}

.welcome-content p {
  font-size: 1.3rem;
  color: #606266;
  line-height: 1.7;
  max-width: 800px;
  margin: 0 auto;
}

.features {
  margin: 40px 0;
}

.feature-card {
  text-align: center;
  padding: 40px 30px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  position: relative;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.feature-card h3 {
  font-size: 1.6rem;
  margin: 15px 0;
  color: #303133;
}

.feature-card p {
  color: #606266;
  line-height: 1.7;
}

.login-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.actions {
  text-align: center;
  margin-top: 40px;
}

.login-tip {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 8px;
  text-align: center;
  border: 1px solid #e4e7ed;
}

.login-tip p {
  margin: 0 0 15px 0;
  color: #606266;
  font-size: 1rem;
  line-height: 1.5;
}

.login-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.login-buttons span {
  color: #909399;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .home-container {
    padding: 10px;
  }

  .welcome-card {
    padding: 20px 10px;
    margin-bottom: 20px;
  }

  .welcome-card h2 {
    font-size: 2rem;
  }

  .welcome-content p {
    font-size: 1rem;
  }

  .feature-card {
    padding: 20px 15px;
    margin-bottom: 15px;
  }

  .feature-card h3 {
    font-size: 1.3rem;
  }

  .login-tip {
    padding: 15px;
  }

  .login-buttons {
    flex-direction: column;
    gap: 10px;
  }

  .login-buttons span {
    display: none;
  }
}
</style>
