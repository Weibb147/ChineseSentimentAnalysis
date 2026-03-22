<template>
  <div class="user-profile-container">
    <el-card class="main-card" :body-style="{ padding: '0px' }">
      <div class="profile-header">
        <div class="header-bg"></div>
        <div class="user-summary">
          <div class="avatar-wrapper" @click="triggerAvatarInput">
            <el-avatar :size="100" :src="userInfoForm.imageUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" class="user-avatar" />
            <div class="avatar-mask">
              <el-icon><Camera /></el-icon>
            </div>
            <input
              type="file"
              ref="avatarInput"
              style="display: none"
              accept="image/*"
              @change="handleAvatarChange"
            />
          </div>
          <div class="user-text">
            <h2 class="nickname">{{ username }}</h2>
            <div class="tags">
              <el-tag effect="dark" :type="userRole === 'ADMIN' ? 'danger' : 'success'" size="small" round>
                {{ userRole === 'ADMIN' ? '管理员' : '普通用户' }}
              </el-tag>
              <el-tag effect="plain" type="info" size="small" round>
                ID: {{ userId }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-content">
        <el-tabs v-model="activeTab" class="profile-tabs" @tab-click="handleTabClick">
          <!-- 个人资料 Tab -->
          <el-tab-pane label="个人资料" name="info">
            <template #label>
              <span class="custom-tabs-label">
                <el-icon><User /></el-icon>
                <span>基本信息</span>
              </span>
            </template>
            <div class="tab-content">
              <el-row :gutter="40">
                <el-col :xs="24" :lg="14">
                  <h3 class="section-title">编辑信息</h3>
                  <el-form
                    ref="formRef"
                    :model="userInfoForm"
                    :rules="rules"
                    label-width="80px"
                    label-position="top"
                    class="info-form"
                    size="large"
                  >
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="昵称" prop="nickname">
                          <el-input v-model="userInfoForm.nickname" placeholder="请输入昵称" :prefix-icon="User" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="手机号" prop="phone">
                          <el-input v-model="userInfoForm.phone" placeholder="请输入手机号" :prefix-icon="Iphone" />
                        </el-form-item>
                      </el-col>
                    </el-row>

                    <el-form-item label="邮箱" prop="email">
                      <el-input v-model="userInfoForm.email" placeholder="请输入邮箱" :prefix-icon="Message" />
                    </el-form-item>
                    
                    <el-form-item label="性别" prop="sex">
                      <el-radio-group v-model="userInfoForm.sex">
                        <el-radio label="MALE" border>男</el-radio>
                        <el-radio label="FEMALE" border>女</el-radio>
                        <el-radio label="UNKNOWN" border>保密</el-radio>
                      </el-radio-group>
                    </el-form-item>

                    <el-form-item class="form-actions">
                      <el-button type="primary" @click="updateUserInfo(formRef)" :loading="loading">
                        保存修改
                      </el-button>
                      <el-button @click="fetchUserInfo">重置</el-button>
                    </el-form-item>
                  </el-form>
                </el-col>
                
                <el-col :xs="24" :lg="10">
                   <h3 class="section-title">账户概览</h3>
                   <div class="overview-grid">
                      <div class="stat-card blue">
                        <div class="stat-icon"><el-icon><List /></el-icon></div>
                        <div class="stat-info">
                          <span class="stat-num">{{ taskOverview.total }}</span>
                          <span class="stat-label">总任务数</span>
                        </div>
                      </div>
                      <div class="stat-card green">
                         <div class="stat-icon"><el-icon><Timer /></el-icon></div>
                        <div class="stat-info">
                          <span class="stat-num">{{ taskOverview.recent }}</span>
                          <span class="stat-label">近期活跃</span>
                        </div>
                      </div>
                      <div class="stat-card orange">
                         <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
                        <div class="stat-info">
                          <span class="stat-label">注册时间</span>
                          <span class="stat-desc">{{ userInfoForm.createdAt?.split(' ')[0] || '未知' }}</span>
                        </div>
                      </div>
                   </div>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>

          <!-- 安全设置 Tab -->
          <el-tab-pane label="安全设置" name="security">
             <template #label>
              <span class="custom-tabs-label">
                <el-icon><Lock /></el-icon>
                <span>安全设置</span>
              </span>
            </template>
            <div class="tab-content narrow">
              <h3 class="section-title">修改密码</h3>
              <p class="section-desc">为了您的账户安全，建议定期更换密码。</p>
              
              <el-form 
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-position="top"
                size="large"
                class="password-form"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input 
                    v-model="passwordForm.oldPassword" 
                    type="password" 
                    show-password 
                    placeholder="请输入当前使用的密码"
                    :prefix-icon="Lock"
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input 
                    v-model="passwordForm.newPassword" 
                    type="password" 
                    show-password 
                    placeholder="请输入新密码（6-20位）"
                    :prefix-icon="Key"
                  />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input 
                    v-model="passwordForm.confirmPassword" 
                    type="password" 
                    show-password 
                    placeholder="请再次输入新密码"
                    :prefix-icon="Key"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button type="primary" @click="handleUpdatePassword" :loading="passwordLoading">
                    确认修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getTaskList } from '@/api/analysis.js'
import { uploadAvatar } from '@/api/file.js'
import { userInfoService, userInfoUpdateService, userUpdatePasswordService, userUpdateAvatarService } from '@/api/user.js'
import type { FormInstance, FormRules } from 'element-plus'
import { 
  User, Iphone, Message, Camera, List, Timer, Calendar, Lock, Key 
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 激活的Tab
const activeTab = ref('info')

// 监听路由参数变化，自动切换Tab
watch(() => route.query.tab, (newTab) => {
  if (newTab && (newTab === 'info' || newTab === 'security')) {
    activeTab.value = newTab as string
  }
}, { immediate: true })

const handleTabClick = (tab: any) => {
  // 更新路由query，但不刷新页面
  router.replace({ query: { ...route.query, tab: tab.props.name } })
}

const loading = ref(false)

// 获取用户信息
const username = computed(() => authStore.userInfo.username || authStore.userInfo.nickname || '未知用户')
const userId = computed(() => authStore.userInfo.id)
const userRole = computed(() => authStore.userInfo.role)

// 表单数据
const userInfoForm = ref({
  id: undefined,
  nickname: '',
  email: '',
  imageUrl: '',
  phone: '',
  sex: '',
  lastLoginTime: '',
  createdAt: ''
})

const formRef = ref<FormInstance>()
const avatarInput = ref<HTMLInputElement | null>(null)
const triggerAvatarInput = () => { avatarInput.value?.click() }

// 表单验证规则
const rules = ref<FormRules>({
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度应在2-20个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
})

// 密码表单
const passwordFormRef = ref<FormInstance>()
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePass2 = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const passwordRules = reactive<FormRules>({
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
})

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await userInfoService()
    if (res.data) {
      userInfoForm.value = {
        ...userInfoForm.value,
        id: res.data.id,
        nickname: res.data.nickname || res.data.username || '',
        email: res.data.email || '',
        imageUrl: res.data.imageUrl || '',
        phone: res.data.phone || '',
        sex: res.data.sex || '',
        lastLoginTime: res.data.lastLoginTime || '',
        createdAt: res.data.createdAt || ''
      }
      authStore.setUserInfo(res.data)
    }
  } catch (error) {
    console.error('获取用户信息失败', error)
    ElMessage.error('获取用户信息失败')
  }
}

const taskOverview = ref({ total: 0, recent: 0 })

const fetchTaskOverview = async () => {
  try {
    const res = await getTaskList({ pageNum: 1, pageSize: 10 })
    if (res.code === 0 && res.data) {
      taskOverview.value.total = res.data.total || 0
      taskOverview.value.recent = (res.data.records || []).length
    }
  } catch (e) {}
}

// 更新用户信息
const updateUserInfo = async (formEl: FormInstance | undefined) => {
  if (!formEl) return

  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const updateData = {
          nickname: userInfoForm.value.nickname,
          email: userInfoForm.value.email,
          phone: userInfoForm.value.phone,
          sex: userInfoForm.value.sex
        }
        
        await userInfoUpdateService(updateData)

        // 更新状态管理中的用户信息
        const res = await userInfoService()
        authStore.setUserInfo(res.data)

        ElMessage.success('用户信息更新成功')
      } catch (error: any) {
        ElMessage.error(error?.response?.data?.message || '更新用户信息失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 处理头像变更
const handleAvatarChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files[0]) {
    const file = input.files[0]
    
    // 验证文件大小和类型
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning('头像图片大小不能超过 2MB')
      return
    }
    
    try {
      const formData = new FormData()
      formData.append('file', file)
      
      const uploadRes = await uploadAvatar(formData)
      if (uploadRes.code === 0 && uploadRes.data) {
        // 后端上传接口已自动更新用户头像，无需再次调用更新接口
        // await userUpdateAvatarService(uploadRes.data.url)
        
        // 更新本地显示的头像URL
        userInfoForm.value.imageUrl = uploadRes.data.url
        
        // 更新store
        const userRes = await userInfoService()
        authStore.setUserInfo(userRes.data)
        
        ElMessage.success('头像更新成功')
      }
    } catch (error) {
      ElMessage.error('头像上传失败')
    } finally {
       // 清空input，允许重复选择同一文件
       input.value = ''
    }
  }
}

// 修改密码
const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      passwordLoading.value = true
      try {
        await userUpdatePasswordService({
          old_pwd: passwordForm.oldPassword,
          new_pwd: passwordForm.newPassword,
          re_pwd: passwordForm.confirmPassword
        })
        ElMessage.success('密码修改成功，请重新登录')
        authStore.removeAuth()
        router.push('/login')
      } catch (error: any) {
        ElMessage.error(error.message || '密码修改失败')
      } finally {
        passwordLoading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchUserInfo()
  fetchTaskOverview()
})
</script>

<style scoped>
.user-profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.main-card {
  border-radius: 16px;
  overflow: hidden;
  min-height: 80vh;
}

.profile-header {
  position: relative;
  background-color: #fff;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;
}

.header-bg {
  height: 160px;
  background: linear-gradient(135deg, #409EFF 0%, #36d1dc 100%);
}

.user-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: -50px;
  padding: 0 20px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border: 4px solid #fff;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: #fff;
  font-size: 24px;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.user-text {
  text-align: center;
  margin-top: 15px;
}

.nickname {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.tags {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.profile-content {
  padding: 20px;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #f0f2f5;
}

.custom-tabs-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
}

.tab-content {
  padding: 20px 0;
}

.tab-content.narrow {
  max-width: 600px;
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #409EFF;
  padding-left: 12px;
}

.section-desc {
  color: #909399;
  margin-bottom: 30px;
}

.info-form {
  margin-top: 10px;
}

.form-actions {
  margin-top: 40px;
}

/* 概览卡片 */
.overview-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: #f9fafc;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.stat-card.blue .stat-icon { background: #ecf5ff; color: #409EFF; }
.stat-card.green .stat-icon { background: #f0f9eb; color: #67C23A; }
.stat-card.orange .stat-icon { background: #fdf6ec; color: #E6A23C; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-desc {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

@media (max-width: 768px) {
  .profile-header {
    margin-bottom: 0;
  }
  
  .user-summary {
    margin-top: -40px;
  }
  
  .el-tabs__nav {
    justify-content: center;
  }
}
</style>