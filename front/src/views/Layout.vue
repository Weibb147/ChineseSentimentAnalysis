<template>
  <el-container class="layout-container">
    <el-aside v-if="!isMobile" width="220px" class="aside">
      <div class="logo" @click="router.push('/')">
        <el-icon class="logo-icon"><TrendCharts /></el-icon>
        <span class="logo-text">情感分析平台</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical-demo"
        :router="true"
        unique-opened
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        @select="handleSelect"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-sub-menu index="sentiment">
          <template #title>
            <el-icon><EditPen /></el-icon>
            <span>情感分析</span>
          </template>
          <el-menu-item index="/predict">
            <el-icon><Monitor /></el-icon>
            <span>在线预测</span>
          </el-menu-item>
          <el-menu-item index="/visualization">
            <el-icon><TrendCharts /></el-icon>
            <span>{{ userRole === 'ADMIN' ? '全站记录' : '我的记录' }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/notices">
          <el-icon><Bell /></el-icon>
          <span>公告与反馈</span>
        </el-menu-item>

        <el-sub-menu index="account" v-if="isAuthenticated">
          <template #title>
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/profile">基本资料</el-menu-item>
          <el-menu-item index="/profile?tab=security">修改密码</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="admin" v-if="userRole === 'ADMIN'">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin">
            <el-icon><Odometer /></el-icon>
            <span>管理员仪表盘</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-drawer
      v-model="mobileMenuVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="mobile-menu-drawer"
    >
      <div class="logo" @click="handleLogoClick">
        <el-icon class="logo-icon"><TrendCharts /></el-icon>
        <span class="logo-text">情感分析平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical-demo"
        :router="true"
        unique-opened
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        @select="handleSelect"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-sub-menu index="sentiment">
          <template #title>
            <el-icon><EditPen /></el-icon>
            <span>情感分析</span>
          </template>
          <el-menu-item index="/predict">
            <el-icon><Monitor /></el-icon>
            <span>在线预测</span>
          </el-menu-item>
          <el-menu-item index="/visualization">
            <el-icon><TrendCharts /></el-icon>
            <span>{{ userRole === 'ADMIN' ? '全站记录' : '我的记录' }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/notices">
          <el-icon><Bell /></el-icon>
          <span>公告与反馈</span>
        </el-menu-item>

        <el-sub-menu index="account" v-if="isAuthenticated">
          <template #title>
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/profile">基本资料</el-menu-item>
          <el-menu-item index="/profile?tab=security">修改密码</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="admin" v-if="userRole === 'ADMIN'">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/admin">
            <el-icon><Odometer /></el-icon>
            <span>管理员仪表盘</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-drawer>

    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-button v-if="isMobile" text class="menu-toggle-btn" @click="mobileMenuVisible = true">
            <el-icon><Fold /></el-icon>
          </el-button>
          <el-breadcrumb v-if="!isMobile" separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.path" :to="item.to">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 用户信息区域 -->
        <div class="header-right">
          <div v-if="isAuthenticated" class="user-section">
            <!-- 通知按钮 -->
<!--            <el-button class="notification-btn" circle size="small" @click="showNotifications">-->
<!--              <el-icon><Bell /></el-icon>-->
<!--              <el-badge v-if="unreadCount > 0" :value="unreadCount" class="notification-badge" />-->
<!--            </el-button>-->

            <!-- 用户下拉菜单 -->
            <el-dropdown class="user-dropdown" placement="bottom-end" @command="handleCommand">
              <div class="user-info">
                <el-avatar 
                  :src="userInfo?.imageUrl || '/src/assets/default.png'" 
                  :size="32"
                  class="user-avatar"
                />
                <div class="user-details">
                  <span class="user-name">{{ userInfo?.username || '用户' }}</span>
                  <span class="user-role">{{ getRoleText(userRole) }}</span>
                </div>
                <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="user-menu">
                  <el-dropdown-item command="profile" class="menu-item">
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided class="menu-item logout-item">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          
          <!-- 未登录状态 -->
          <div v-else class="auth-section">
            <el-button type="primary" plain @click="router.push('/login')" class="auth-btn">
              登录
            </el-button>
            <el-button type="primary" @click="router.push('/register')" class="auth-btn">
              注册
            </el-button>
          </div>
        </div>
      </el-header>

      <!-- 主要内容区域 -->
      <el-main class="main-content">
        <div class="content-wrapper">
          <router-view v-slot="{ Component }">
            <component :is="Component" :key="route.fullPath" />
          </router-view>
        </div>
      </el-main>
      
      <!-- 底部区域 -->
      <el-footer class="footer">
         <div class="footer-content">
          <p class="copyright">© 2025 基于BERT的中文文本情感分析平台</p>
        </div>
      </el-footer>
    </el-container>
    
    <!-- 弹出对话框 (保留原有逻辑) -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="60%"
      class="content-dialog"
      center
    >
      <div class="dialog-content" v-html="dialogContent"></div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { 
  TrendCharts, ArrowDown, EditPen, House, SwitchButton, 
  User, Setting, Bell, Monitor, Odometer, Fold
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 响应式数据
const unreadCount = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogContent = ref('')
const isMobile = ref(false)
const mobileMenuVisible = ref(false)

// 计算属性
const userInfo = computed(() => authStore.userInfo)
const isAuthenticated = computed(() => !!authStore.token)
const userRole = computed(() => userInfo.value?.role || '')

// 当前激活的菜单项
const activeMenu = computed(() => {
  if (route.path === '/profile' && route.query.tab === 'security') {
    return '/profile?tab=security'
  }
  return route.path
})

// 面包屑导航
const breadcrumbItems = computed(() => {
  const items = []
  const pathSegments = route.path.split('/').filter(Boolean)
  
  const routeMap = {
    'predict': '情感分析预测',
    'tasks': '分析任务管理',
    'visualization': '情感预测记录',
    'admin': '管理控制台',
    'profile': '个人中心',
    'login': '登录',
    'register': '注册',
    'notices': '公告与反馈'
  }
  
  pathSegments.forEach((segment, index) => {
    const title = routeMap[segment] || segment
    const path = '/' + pathSegments.slice(0, index + 1).join('/')
    items.push({
      title,
      path,
      to: { path }
    })
  })
  
  return items
})

// 获取角色文本
const getRoleText = (role) => {
  const roleMap = {
    'ADMIN': '管理员',
    'USER': '普通用户',
    'VIP': 'VIP用户'
  }
  return roleMap[role] || '用户'
}

const updateViewport = () => {
  isMobile.value = window.innerWidth <= 992
  if (!isMobile.value) {
    mobileMenuVisible.value = false
  }
}

const handleLogoClick = () => {
  router.push('/')
  mobileMenuVisible.value = false
}

// 处理菜单选择
const handleSelect = (index) => {
  router.push(index)
  if (isMobile.value) {
    mobileMenuVisible.value = false
  }
}

// 显示通知
const showNotifications = () => {
  ElMessage.info('暂无新通知')
  unreadCount.value = 0
}

// 处理下拉菜单命令
const handleCommand = async (command) => {
  switch (command) {
    case 'logout':
      await handleLogout()
      break
    case 'profile':
      router.push('/profile')
      break
  }
}

// 处理退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '您确认要退出登录吗？',
      '退出确认',
      {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 清除认证信息
    authStore.removeAuth()
    ElMessage.success('退出登录成功')
    
    // 跳转到登录页
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出失败')
    }
  }
}

// 监听路由变化
watch(() => route.path, () => {
  mobileMenuVisible.value = false
  if (isAuthenticated.value && Math.random() > 0.7) {
    unreadCount.value = Math.floor(Math.random() * 5)
  }
}, { immediate: true })

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})

</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.aside {
  background-color: #304156;
  color: #fff;
  transition: width 0.3s;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b2f3a;
  cursor: pointer;
  color: #fff;
}

.logo-icon {
  font-size: 24px;
  margin-right: 10px;
  color: #409EFF;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
}

.el-menu-vertical-demo {
  border-right: none;
  flex: 1;
}

.header {
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.menu-toggle-btn {
  margin-right: 8px;
  font-size: 18px;
}

.header-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  min-width: 0;
}

.user-details {
  margin-left: 8px;
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: 12px;
  color: #909399;
}

.main-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}

.content-wrapper {
  width: 100%;
  min-width: 0;
}

.footer {
  text-align: center;
  padding: 10px 0;
  color: #909399;
  font-size: 12px;
  background-color: #f0f2f5;
}

/* Transitions */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

:deep(.mobile-menu-drawer .el-drawer__body) {
  padding: 0;
  background-color: #304156;
}

@media (max-width: 992px) {
  .layout-container {
    display: block;
  }

  .header {
    padding: 0 12px;
  }

  .header-right {
    margin-left: 8px;
  }

  .user-details {
    display: none;
  }

  .auth-section {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .auth-btn {
    padding: 8px 10px;
    margin-left: 0 !important;
  }

  .main-content {
    padding: 12px;
  }

  .footer {
    padding: 8px 0;
  }
}
</style>
