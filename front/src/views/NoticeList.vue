<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, ChatDotRound, Plus, Edit, Delete, Position,
  ChatLineSquare, Refresh, User, Iphone, Message as IconMessage,
  Clock, View as IconView, UserFilled, EditPen
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { isTokenExpired, getUserInfoFromToken } from '@/utils/jwtUtils'
// 模拟导入 API，请根据实际路径调整
import { getNoticeList, createNotice, updateNotice, deleteNotice } from '@/api/notice.js'
import {
  submitFeedback, getMyFeedbacks, getAllFeedbacks,
  replyFeedback, deleteFeedback, updateFeedback
} from '@/api/feedback.js'
import { userInfoService } from '@/api/user.js'

// --- 权限与用户状态 ---
const authStore = useAuthStore()
const isAuthenticated = computed(() => !!authStore.token && !isTokenExpired(authStore.token))

const getRolesUpper = () => {
  const info: any = authStore.userInfo || {}
  let raw: any = info.role ?? info.authorities
  if (!raw && authStore.token) {
    const parsed = getUserInfoFromToken(authStore.token)
    raw = parsed.role ?? parsed.authorities
  }
  const arr = Array.isArray(raw) ? raw : (raw ? [raw] : [])
  return arr.filter(Boolean).map((x: any) => String(x).toUpperCase())
}

const isAdmin = computed(() => {
  const up = getRolesUpper()
  return up.includes('ADMIN') || up.includes('ROLE_ADMIN')
})

const ensureUserInfo = async () => {
  if (!isAuthenticated.value) return
  try {
    const res = await userInfoService()
    const data = res.data || res
    if (data) {
      authStore.setUserInfo(data)
    }
  } catch (e) {
    // 静默失败
  }
}

// 分页事件处理
const handleNoticePageChange = (p: number) => { noticePagination.pageNum = p; fetchNotices() }
const handleAdminPageChange = (p: number) => { adminFbPage.pageNum = p; fetchAllFeedbacks() }
const handleMyFbPageChange = (p: number) => { myFbPage.pageNum = p; fetchMyFeedbacks() }

// --- 通用工具 ---
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

// ================== 公告模块 (Notices) ==================
const notices = ref<any[]>([])
const noticeLoading = ref(false)
const noticePagination = reactive({ pageNum: 1, pageSize: 5, total: 0 })

const noticeEditDialogVisible = ref(false)
const noticeDetailDialogVisible = ref(false)
const currentNotice = reactive({ id: undefined as any, title: '', content: '', type: 'SYSTEM', status: 'VISIBLE' })
const activeNoticeDetail = ref<any>({})

const noticeTypes = [
  { label: '系统公告', value: 'SYSTEM', color: '#409EFF', bg: '#ecf5ff', icon: 'Bell', elType: 'primary' },
  { label: '更新公告', value: 'UPDATE', color: '#67C23A', bg: '#f0f9eb', icon: 'Refresh', elType: 'success' },
  { label: '活动公告', value: 'EVENT', color: '#E6A23C', bg: '#fdf6ec', icon: 'Trophy', elType: 'warning' },
  { label: '其他公告', value: 'OTHER', color: '#909399', bg: '#f4f4f5', icon: 'InfoFilled', elType: 'info' }
]

const getNoticeMeta = (type: string) => noticeTypes.find(t => t.value === type) || noticeTypes[0]

const fetchNotices = async () => {
  noticeLoading.value = true
  try {
    // 仅获取可见的公告
    const res = await getNoticeList({ 
      pageNum: noticePagination.pageNum, 
      pageSize: noticePagination.pageSize,
      status: 'VISIBLE'
    })
    const data = res.data || res
    notices.value = data.records || []
    noticePagination.total = data.total || 0
  } catch (e) { console.error(e) } finally { noticeLoading.value = false }
}

const openNoticeDetail = (row: any) => {
  activeNoticeDetail.value = { ...row }
  noticeDetailDialogVisible.value = true
}

const openNoticeEdit = (row?: any) => {
  if (row) {
    Object.assign(currentNotice, { id: row.id, title: row.title, content: row.content, type: row.type, status: row.status })
  } else {
    Object.assign(currentNotice, { id: undefined, title: '', content: '', type: 'SYSTEM', status: 'VISIBLE' })
  }
  noticeEditDialogVisible.value = true
}

const saveNotice = async () => {
  if (!currentNotice.title || !currentNotice.content) return ElMessage.warning('请填写完整信息')
  try {
    if (currentNotice.id) {
      await updateNotice(currentNotice.id, { ...currentNotice })
      ElMessage.success('公告更新成功')
    } else {
      await createNotice({ ...currentNotice })
      ElMessage.success('公告发布成功')
    }
    noticeEditDialogVisible.value = false
    fetchNotices()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

const handleDelNotice = (id: number) => {
  ElMessageBox.confirm('确认删除该公告？', '警告', { type: 'warning' }).then(async () => {
    try { await deleteNotice(id); ElMessage.success('已删除'); fetchNotices() } catch (e) { ElMessage.error('删除失败') }
  })
}

// ================== 反馈模块 (Feedback) ==================
const fbActiveTab = ref('write')
const fbCategories = [
  { label: '功能建议', value: 'SUGGESTION' },
  { label: '系统Bug', value: 'BUG' },
  { label: '使用咨询', value: 'CONSULT' },
  { label: '其他', value: 'OTHER' }
]

// 用户侧数据
const myFeedbacks = ref<any[]>([])
const myFbLoading = ref(false)
const myFbPage = reactive({ pageNum: 1, pageSize: 5, total: 0 })
const fbForm = reactive({ category: 'SUGGESTION', content: '' })
const fbSubmitting = ref(false)
const userEditDialogVisible = ref(false)
const currentUserFb = reactive({ id: undefined as any, category: '', content: '' })

// 管理员侧数据
const allFeedbacks = ref<any[]>([])
const adminFbLoading = ref(false)
const adminFbPage = reactive({ pageNum: 1, pageSize: 8, total: 0 })
const replyDialogVisible = ref(false)
const replyForm = reactive({ id: null as number | null, content: '' })
const filterUnreplied = ref(false)

// --- 用户操作 ---
const fetchMyFeedbacks = async () => {
  if (!isAuthenticated.value || isAdmin.value) return
  myFbLoading.value = true
  try {
    const res = await getMyFeedbacks({ pageNum: myFbPage.pageNum, pageSize: myFbPage.pageSize })
    const data = res.data || res
    myFeedbacks.value = data.records || []
    myFbPage.total = data.total || 0
  } finally { myFbLoading.value = false }
}

const submitFb = async () => {
  if (!fbForm.content.trim()) return ElMessage.warning('请输入内容')
  fbSubmitting.value = true
  try {
    await submitFeedback({ ...fbForm })
    ElMessage.success('反馈提交成功')
    fbForm.content = ''
    fbActiveTab.value = 'list'
    myFbPage.pageNum = 1
    fetchMyFeedbacks()
  } catch (e: any) { ElMessage.error(e.message || '提交失败') } finally { fbSubmitting.value = false }
}

const openUserEditFb = (row: any) => {
  Object.assign(currentUserFb, { id: row.id, category: row.category, content: row.content })
  userEditDialogVisible.value = true
}

const saveUserFb = async () => {
  try {
    await updateFeedback(currentUserFb.id, { category: currentUserFb.category, content: currentUserFb.content })
    ElMessage.success('修改成功')
    userEditDialogVisible.value = false
    fetchMyFeedbacks()
  } catch (e) { ElMessage.error('修改失败') }
}

const handleUserDelFb = (id: number) => {
  ElMessageBox.confirm('确认删除此反馈记录？', '提示', { type: 'warning' }).then(async () => {
    try { await deleteFeedback(id); ElMessage.success('已删除'); fetchMyFeedbacks() } catch (e) { ElMessage.error('删除失败') }
  })
}

// --- 管理员操作 ---
const fetchAllFeedbacks = async () => {
  if (!isAdmin.value) return
  adminFbLoading.value = true
  try {
    const params = { pageNum: adminFbPage.pageNum, pageSize: adminFbPage.pageSize }
    const res = await getAllFeedbacks(params)
    const data = res.data || res

    let records = data.records || []

    // **核心优化：数据结构同步**
    // 确保反馈列表中的用户数据字段与个人中心页的 userInfoForm 一致，方便统一显示
    records = records.map((record: any) => {
      // 提取或映射用户数据
      const user = record.user || {};
      const userInfo = {
        nickname: user.nickname || record.nickname || record.username || '匿名用户',
        phone: user.phone || record.phone || '',
        email: user.email || record.email || '',
        imageUrl: user.imageUrl || record.imageUrl || ''
      };

      return {
        ...record,
        ...userInfo, // 将映射后的用户信息平铺到记录上，方便模板访问
        // 保留原始 user 对象，以防万一
        user: userInfo
      };
    });

    if (filterUnreplied.value) {
      records = records.filter((r: any) => !r.adminReply)
    }
    allFeedbacks.value = records
    adminFbPage.total = data.total || 0
  } finally { adminFbLoading.value = false }
}

const openReplyDialog = (row: any) => {
  replyForm.id = row.id
  replyForm.content = row.adminReply || ''
  replyDialogVisible.value = true
}

const submitReply = async () => {
  if (!replyForm.id || !replyForm.content.trim()) return ElMessage.warning('请输入回复内容')
  try {
    await replyFeedback(replyForm.id, replyForm.content)
    ElMessage.success('回复已发送')
    replyDialogVisible.value = false
    fetchAllFeedbacks()
  } catch (e: any) { ElMessage.error(e.message || '回复失败') }
}

const handleAdminDelFb = (id: number) => {
  ElMessageBox.confirm('删除用户的反馈？操作不可逆。', '危险操作', { type: 'warning' }).then(async () => {
    try { await deleteFeedback(id); ElMessage.success('已删除'); fetchAllFeedbacks() } catch (e) { ElMessage.error('删除失败') }
  })
}

onMounted(async () => {
  await ensureUserInfo()
  fetchNotices()
  if (isAuthenticated.value) {
    if (isAdmin.value) await fetchAllFeedbacks()
    else await fetchMyFeedbacks()
  }
})
</script>

<template>
  <div class="notices-page-layout">

    <div class="stack-container">

      <el-card class="box-card notices-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="header-title">
              <el-icon class="header-icon warning"><Bell /></el-icon>
              <span>平台公告</span>
            </div>
            <el-button v-if="isAdmin" type="primary" plain size="small" :icon="Plus" @click="openNoticeEdit()">
              发布公告
            </el-button>
          </div>
        </template>

        <div v-loading="noticeLoading" class="notice-container">
          <el-empty v-if="notices.length === 0" description="暂无公告发布" :image-size="80" />

          <div v-else class="notice-timeline">
            <div v-for="item in notices" :key="item.id" class="timeline-item">
              <div class="t-line"></div>
              <div class="t-dot" :style="{ borderColor: getNoticeMeta(item.type).color }"></div>

              <div class="t-content clickable-card" @click="openNoticeDetail(item)">
                <div class="n-meta">
                  <span class="n-tag" :style="{ color: getNoticeMeta(item.type).color, background: getNoticeMeta(item.type).bg }">
                    {{ getNoticeMeta(item.type).label }}
                  </span>
                  <span class="n-info">
                      <el-icon><User /></el-icon> {{ item.publisher || '管理员' }}
                      <span class="divider">|</span>
                      <el-icon><Clock /></el-icon> {{ formatDate(item.createdAt || item.createTime) }}
                  </span>
                </div>
                <div class="n-main">
                  <h4 class="n-title">{{ item.title }}</h4>
                  <p class="n-desc text-ellipsis-2">{{ item.content }}</p>
                </div>
                <div class="n-actions" v-if="isAdmin" @click.stop>
                  <el-button type="primary" link :icon="Edit" size="small" @click="openNoticeEdit(item)">编辑</el-button>
                  <el-button type="danger" link size="small" :icon="Delete" @click="handleDelNotice(item.id)">删除</el-button>
                </div>
              </div>
            </div>
          </div>

          <div class="pagination-area" v-if="notices.length > 0">
            <el-pagination
                background small layout="prev, pager, next"
                :total="noticePagination.total"
                :page-size="noticePagination.pageSize"
                :current-page="noticePagination.pageNum"
                @current-change="handleNoticePageChange"
            />
          </div>
        </div>
      </el-card>

      <el-card class="box-card feedback-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="header-title">
              <el-icon class="header-icon primary"><EditPen /></el-icon>
              <span>用户反馈</span>
            </div>
            <div v-if="isAdmin" class="header-actions">
              <el-checkbox v-model="filterUnreplied" label="仅待回复" size="small" @change="fetchAllFeedbacks" border />
              <el-button :icon="Refresh" circle size="small" @click="fetchAllFeedbacks" />
            </div>
          </div>
        </template>

        <div v-if="isAdmin" class="admin-fb-list-wrapper">
          <div class="admin-fb-list" v-loading="adminFbLoading">
            <el-scrollbar max-height="60vh">
              <el-empty v-if="allFeedbacks.length === 0" description="暂无反馈数据" :image-size="60" />

              <div v-for="item in allFeedbacks" :key="item.id" class="fb-item-admin">
                <div class="fa-user-card">
                  <el-avatar :size="36" :src="item.imageUrl || ''" :icon="UserFilled" class="user-avatar" />
                  <div class="user-info-block">
                    <div class="ui-row-1">
                      <span class="username">{{ item.nickname }}</span>
                      <el-tag size="small" type="info" class="user-id-tag">ID:{{ item.userId }}</el-tag>
                    </div>
                    <div class="ui-row-2">
                      <span class="contact-item" v-if="item.phone">
                        <el-icon><Iphone /></el-icon> {{ item.phone }}
                      </span>
                      <span class="contact-item" v-else-if="item.email">
                         <el-icon><IconMessage /></el-icon> {{ item.email }}
                      </span>
                      <span class="contact-item" v-else>
                         <el-icon><IconMessage /></el-icon> 暂无联系方式
                      </span>
                    </div>
                  </div>
                  <div class="fa-time">{{ formatDate(item.createdAt) }}</div>
                </div>

                <div class="fa-body">
                  <div class="fb-category-tag">
                    <el-tag size="small" effect="plain">{{ item.category }}</el-tag>
                  </div>
                  <div class="fa-content">{{ item.content }}</div>
                </div>

                <div v-if="item.adminReply" class="fa-reply-preview">
                  <div class="reply-label"><el-icon><ChatLineSquare /></el-icon> 回复:</div>
                  <div class="reply-text">{{ item.adminReply }}</div>
                </div>

                <div class="fa-footer">
                  <el-button type="primary" link size="small" :icon="item.adminReply ? Edit : ChatDotRound" @click="openReplyDialog(item)">
                    {{ item.adminReply ? '修改回复' : '立即回复' }}
                  </el-button>
                  <el-button type="danger" link size="small" :icon="Delete" @click="handleAdminDelFb(item.id)">
                    删除
                  </el-button>
                </div>
              </div>
            </el-scrollbar>

            <div class="pagination-area">
              <el-pagination small layout="prev, next"
                             :total="adminFbPage.total"
                             :page-size="adminFbPage.pageSize"
                             :current-page="adminFbPage.pageNum"
                             @current-change="handleAdminPageChange"
              />
            </div>
          </div>
        </div>

        <div v-else class="user-fb-tabs-wrapper">
          <el-tabs v-model="fbActiveTab" stretch class="custom-tabs">
            <el-tab-pane label="我要反馈" name="write">
              <div class="fb-write-pane" v-if="isAuthenticated">
                <div class="tips-box">
                  <el-icon><IconMessage /></el-icon> 感谢您的反馈，我们会尽快处理！
                </div>
                <el-form :model="fbForm" label-position="top">
                  <el-form-item label="问题类型">
                    <el-radio-group v-model="fbForm.category" size="default">
                      <el-radio-button v-for="c in fbCategories" :key="c.value" :label="c.value">{{ c.label }}</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="详细描述">
                    <el-input v-model="fbForm.content" type="textarea" :rows="6" placeholder="请详细描述..." maxlength="300" show-word-limit />
                  </el-form-item>
                  <el-button type="primary" class="submit-btn" :loading="fbSubmitting" :icon="Position" @click="submitFb">提交</el-button>
                </el-form>
              </div>
              <div v-else class="auth-placeholder">
                <el-empty description="登录后即可提交反馈" />
                <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="我的记录" name="list">
              <div v-loading="myFbLoading" class="fb-list-pane">
                <div v-if="!isAuthenticated" class="auth-placeholder"><el-empty description="请先登录" /></div>
                <el-empty v-else-if="myFeedbacks.length === 0" description="暂无反馈记录" />
                <div v-else class="user-fb-list">
                  <div v-for="item in myFeedbacks" :key="item.id" class="user-fb-item">
                    <div class="uf-head">
                      <el-tag size="small" effect="plain">{{ item.category }}</el-tag>
                      <span class="uf-date">{{ formatDate(item.createdAt) }}</span>
                    </div>
                    <div class="uf-content">{{ item.content }}</div>
                    <div v-if="item.adminReply" class="admin-reply-bubble">
                      <div class="bubble-title"><el-icon><User /></el-icon> 管理员回复</div>
                      <div class="bubble-text">{{ item.adminReply }}</div>
                    </div>
                    <div class="uf-actions">
                      <el-button type="primary" link size="small" :icon="Edit" @click="openUserEditFb(item)">编辑</el-button>
                      <el-button type="danger" link size="small" :icon="Delete" @click="handleUserDelFb(item.id)">删除</el-button>
                    </div>
                  </div>
                </div>
                <div class="pagination-area" v-if="myFeedbacks.length > 0">
                  <el-pagination small layout="prev, next"
                                 :total="myFbPage.total" :page-size="myFbPage.pageSize" :current-page="myFbPage.pageNum"
                                 @current-change="handleMyFbPageChange"
                  />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-card>

    </div>

    <el-dialog v-model="noticeDetailDialogVisible" title="公告详情" width="600px" align-center class="notice-detail-dialog">
      <div class="detail-header">
        <h2>{{ activeNoticeDetail.title }}</h2>
        <div class="detail-meta">
          <el-tag :type="getNoticeMeta(activeNoticeDetail.type).elType" effect="light" size="large">
            <el-icon style="vertical-align: middle; margin-right: 4px"><component :is="getNoticeMeta(activeNoticeDetail.type).icon" /></el-icon>
            {{ getNoticeMeta(activeNoticeDetail.type).label }}
          </el-tag>
          <span class="meta-item"><el-icon><User /></el-icon> {{ activeNoticeDetail.publisher || '管理员' }}</span>
          <span class="meta-item"><el-icon><Clock /></el-icon> {{ formatDate(activeNoticeDetail.createdAt || activeNoticeDetail.createTime) }}</span>
        </div>
      </div>
      <el-divider />
      <div class="detail-content">
        {{ activeNoticeDetail.content }}
      </div>
      <template #footer>
        <el-button @click="noticeDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noticeEditDialogVisible" :title="currentNotice.id ? '编辑公告' : '发布新公告'" width="500px" align-center destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="currentNotice.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="currentNotice.type" style="width: 100%">
            <el-option v-for="t in noticeTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="currentNotice.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNotice">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userEditDialogVisible" title="修改我的反馈" width="450px" align-center>
      <el-form label-position="top">
        <el-form-item label="类型">
          <el-select v-model="currentUserFb.category" style="width: 100%">
            <el-option v-for="c in fbCategories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="currentUserFb.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUserFb">保存修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyDialogVisible" :title="replyForm.content ? '修改回复' : '回复反馈'" width="450px" align-center>
      <div class="dialog-tips" v-if="replyForm.content"><el-icon><Edit /></el-icon> 您正在修改之前的回复</div>
      <el-input v-model="replyForm.content" type="textarea" :rows="5" placeholder="请输入回复内容..." />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 容器布局优化 */
.notices-page-layout {
  max-width: 1200px; /* 设定最大宽度 */
  margin: 0 auto;
  padding: 20px;
}

.stack-container {
  display: flex;
  flex-direction: column;
  gap: 20px; /* 垂直间距 */
}

/* 卡片基础样式 */
.box-card {
  border-radius: 12px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  min-height: 450px; /* 确保卡片有基础高度 */
  display: flex;
  flex-direction: column;
}

.box-card :deep(.el-card__header) { padding: 16px 22px; border-bottom: 1px solid #f2f6fc; }
.box-card :deep(.el-card__body) { padding: 0; flex: 1; overflow: hidden; display: flex; flex-direction: column; }

.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-title { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 18px; color: #303133; }
.header-icon.warning { color: #E6A23C; }
.header-icon.primary { color: #409EFF; }
.header-actions { display: flex; align-items: center; gap: 10px; }


/* --- 公告列表样式 --- */
.notice-container {
  padding: 18px;
  overflow-y: auto;
  flex: 1;
  max-height: 50vh; /* 统一最大高度 */
}
.notice-timeline { padding-left: 10px; }
.timeline-item { position: relative; padding-left: 28px; padding-bottom: 25px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-item:last-child .t-line { display: none; }
.t-line { position: absolute; left: 6px; top: 6px; bottom: 0; width: 2px; background: #e4e7ed; }
.t-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 50%; border: 3px solid #ccc; background: #fff; z-index: 1; }

.clickable-card {
  background: #fcfcfc; border: 1px solid #f0f2f5; border-radius: 8px;
  padding: 14px 18px; transition: all 0.2s; cursor: pointer;
}
.clickable-card:hover { background: #fff; box-shadow: 0 6px 16px rgba(0,0,0,0.08); transform: translateY(-1px); border-color: #dcdfe6; }

.n-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; flex-wrap: wrap; gap: 8px;}
.n-info { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 5px; }
.n-title { margin: 0 0 6px; font-size: 16px; font-weight: 600; color: #303133; }
.text-ellipsis-2 { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.n-actions { margin-top: 8px; text-align: right; border-top: 1px dashed #ebeef5; padding-top: 4px; }


/* --- 管理员反馈列表样式 (增强) --- */
.admin-fb-list-wrapper { flex: 1; display: flex; flex-direction: column; }
.admin-fb-list { flex: 1; display: flex; flex-direction: column; }
.admin-fb-list :deep(.el-scrollbar__view) { min-height: 100%; display: flex; flex-direction: column; } /* 确保内容撑满 */

.fb-item-admin { padding: 15px 20px; border-bottom: 1px solid #f2f6fc; transition: background 0.2s;}
.fb-item-admin:hover { background-color: #fafafa; }

.fa-user-card { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.user-avatar { background: #c0c4cc; flex-shrink: 0; }
.user-info-block { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.ui-row-1 { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.username { font-size: 15px; font-weight: 600; color: #303133; }
.user-id-tag { font-size: 11px; height: 18px; padding: 0 4px; line-height: 16px; transform: scale(0.9); transform-origin: left center;}
.ui-row-2 { font-size: 12px; color: #909399; display: flex; align-items: center; }
.contact-item { display: flex; align-items: center; gap: 4px; }
.fa-time { font-size: 12px; color: #c0c4cc; align-self: flex-start; margin-top: 4px; }

.fa-body { margin-bottom: 10px; padding-left: 48px; }
.fa-content { font-size: 14px; color: #303133; line-height: 1.6; background: #f9fafe; padding: 10px; border-radius: 6px; }

.fa-reply-preview {
  margin-left: 48px; background: #f0f9eb; padding: 8px 12px; border-radius: 4px;
  font-size: 13px; color: #606266; margin-bottom: 8px; border-left: 3px solid #67c23a;
}
.reply-label { font-weight: bold; color: #67c23a; margin-bottom: 2px; display: flex; align-items: center; gap: 5px; }
.fa-footer { text-align: right; margin-left: 48px; }


/* --- 用户反馈 Tab 样式 --- */
.user-fb-tabs-wrapper { flex: 1; display: flex; flex-direction: column; }
.custom-tabs { flex: 1; display: flex; flex-direction: column; }
.custom-tabs :deep(.el-tabs__content) { flex: 1; overflow-y: auto; }

.fb-write-pane { padding: 20px; max-width: 600px; margin: 0 auto; width: 100%; }
.fb-list-pane { padding: 15px 20px; max-height: 60vh; overflow-y: auto; } /* 统一高度，避免溢出 */
.submit-btn { width: 100%; margin-top: 10px; }

.auth-placeholder { text-align: center; padding: 40px 0; }
.pagination-area { display: flex; justify-content: center; padding: 12px 15px; border-top: 1px solid #f2f6fc; }
</style>