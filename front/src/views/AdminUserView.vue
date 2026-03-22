<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getUserListAPI, createUserAPI, updateUserAPI, deleteUserAPI, toggleUserStatusAPI } from '@/api/admin.js'

// State
const users = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchKeyword = ref('')
const dialogVisible = ref(false)
const saveLoading = ref(false)
const userFormRef = ref<FormInstance>()

const currentUser = reactive({ 
  id: undefined, 
  username: '', 
  email: '', 
  phone: '', 
  sex: '', 
  password: '', 
  role: 'USER', 
  status: 1, 
  nickname: '' 
})

// Rules
const userRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }, 
    { min: 3, max: 20, message: '长度3-20', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' }, 
    { type: 'email', message: '格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }, 
    { min: 6, max: 20, message: '长度6-20', trigger: 'blur' }
  ]
}

// Actions
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getUserListAPI({ 
      page: pagination.currentPage, 
      size: pagination.pageSize, 
      keyword: searchKeyword.value 
    })
    users.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const showCreateUserDialog = () => {
  Object.assign(currentUser, { 
    id: undefined, 
    username: '', 
    email: '', 
    phone: '', 
    sex: '', 
    password: '', 
    role: 'USER', 
    status: 1, 
    nickname: '' 
  })
  dialogVisible.value = true
}

const editUser = (row: any) => {
  Object.assign(currentUser, row)
  // Ensure password is reset so it's not sent if not changed (though backend should handle it)
  // But for editing, we usually don't pre-fill password.
  currentUser.password = '' 
  dialogVisible.value = true
}

const saveUser = async () => {
  if (!userFormRef.value) return
  await userFormRef.value.validate(async (valid) => {
    if (!valid) return
    saveLoading.value = true
    try {
      if (currentUser.id) {
        // Update
        const dataToSend = { ...currentUser }
        if (!dataToSend.password) delete (dataToSend as any).password // Don't update password if empty
        await updateUserAPI(currentUser.id, dataToSend)
      } else {
        // Create
        await createUserAPI(currentUser)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchUsers()
    } catch (e) {
      ElMessage.error('保存失败')
    } finally {
      saveLoading.value = false
    }
  })
}

const toggleUserStatus = async (row: any) => {
  try {
    await toggleUserStatusAPI(row.id, { ...row, status: row.status === 1 ? 0 : 1 })
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success('操作成功')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteUser = (row: any) => {
  ElMessageBox.confirm('确定删除该用户吗?', '提示', { type: 'warning' })
    .then(async () => {
      await deleteUserAPI(row.id)
      ElMessage.success('删除成功')
      fetchUsers()
    })
    .catch(() => {})
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="admin-user-view">
    <div class="toolbar">
      <el-button type="primary" @click="showCreateUserDialog" :icon="Plus">添加用户</el-button>
      <div class="actions">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索用户" 
          clearable 
          @keyup.enter="fetchUsers" 
          style="width: 200px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="fetchUsers">搜索</el-button>
      </div>
    </div>

    <el-table :data="users" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="username" label="用户名" width="150" show-overflow-tooltip>
        <template #default="{row}">
          <div class="user-cell">
            <el-avatar :size="24" :src="row.imageUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
            <span style="margin-left: 8px">{{ row.username }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" width="150" show-overflow-tooltip />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="sex" label="性别" width="80" align="center" />
      <el-table-column prop="role" label="角色" width="100" align="center">
        <template #default="{row}">
          <el-tag :type="row.role==='ADMIN'?'danger':'primary'" effect="plain">
            {{ row.role==='ADMIN'?'管理员':'用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{row}">
          <el-switch 
            :model-value="row.status === 1" 
            @change="toggleUserStatus(row)" 
            style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949" 
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" size="small" @click="editUser(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="deleteUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination 
        v-model:current-page="pagination.currentPage" 
        v-model:page-size="pagination.pageSize" 
        :total="pagination.total" 
        layout="total, prev, pager, next" 
        @current-change="fetchUsers" 
        background 
      />
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="currentUser.id?'编辑用户':'添加用户'" width="500px">
      <el-form :model="currentUser" :rules="userRules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="currentUser.username" :disabled="!!currentUser.id" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="currentUser.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="currentUser.email" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="currentUser.phone" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
           <el-radio-group v-model="currentUser.sex">
              <el-radio label="MALE">男</el-radio>
              <el-radio label="FEMALE">女</el-radio>
              <el-radio label="UNKNOWN">保密</el-radio>
           </el-radio-group>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!currentUser.id">
          <el-input v-model="currentUser.password" type="password" show-password placeholder="默认密码" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="currentUser.role" style="width:100%">
            <el-option label="用户" value="USER"/>
            <el-option label="管理员" value="ADMIN"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px; border: 1px solid #ebeef5; }
.actions { display: flex; gap: 12px; }
.user-cell { display: flex; align-items: center; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
