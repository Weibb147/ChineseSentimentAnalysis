<template>
  <div class="login-container">
    <div class="login-card">
      <div class="card-header">
        <div class="logo">
          <img src="@/assets/logo.png" alt="Logo" v-if="false">
          <div class="logo-placeholder" v-else>
            <span class="logo-icon">🧠</span>
          </div>
        </div>
        <h1 class="app-title">情感分析平台</h1>
        <p class="app-subtitle">多类别中文情感识别系统</p>
      </div>

      <div class="mode-toggle">
        <h2 v-if="!isRegister" class="form-title text-center">用户登录</h2>
        <h2 v-else class="form-title text-center">用户注册</h2>
      </div>

      <div class="form-section">
        <!-- 登录表单 -->
        <el-form
          v-if="!isRegister"
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="loginForm.agreeTerms" class="agree-checkbox">
              我已阅读并同意
              <el-button type="text" @click="showTerms">《服务条款》</el-button>
              和
              <el-button type="text" @click="showPrivacy">《隐私政策》</el-button>
            </el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              size="large"
              class="submit-button"
              round
              :disabled="!loginForm.agreeTerms"
            >
              登录
            </el-button>
            <el-button @click="resetLoginForm" class="reset-button" size="large" round>
              重置
            </el-button>
          </el-form-item>
          <div class="form-footer">
            <span>还没有账号？</span>
            <el-button type="text" @click="switchToRegister">现在去注册！</el-button>
          </div>
        </el-form>

        <!-- 注册表单 -->
        <el-form
          v-else
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名（3-20个字符）"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱地址"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>

          <el-row :gutter="10">
            <el-col :span="16">
              <el-form-item prop="emailCode">
                <el-input
                  v-model="registerForm.emailCode"
                  placeholder="邮箱验证码"
                  size="large"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-button
                type="primary"
                @click="sendEmailCode"
                :disabled="emailCodeDisabled"
                size="large"
                class="code-button"
              >
                {{ emailCodeButtonText }}
              </el-button>
            </el-col>
          </el-row>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（6-20个字符）"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleRegister"
              :loading="loading"
              size="large"
              class="submit-button"
              round
            >
              注册
            </el-button>
            <el-button @click="resetRegisterForm" class="reset-button" size="large" round>
              重置
            </el-button>
          </el-form-item>
          <div class="form-footer">
            <span>已有账号？</span>
            <el-button type="text" @click="switchToLogin">现在去登录！</el-button>
          </div>
        </el-form>
      </div>

      <div class="features-section">
        <div class="feature-item">
          <span class="feature-icon">🎯</span>
          <span class="feature-text">高精度情感识别</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🚀</span>
          <span class="feature-text">实时分析处理</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">📊</span>
          <span class="feature-text">多维度情感分类</span>
        </div>
      </div>
    </div>

    <!-- 验证码弹窗 -->
    <el-dialog
      v-model="showCaptchaDialog"
      title="安全验证"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="true"
      @close="handleCaptchaDialogClose"
    >
      <div class="captcha-dialog-content">
        <p class="captcha-tip">为了您的账户安全，请输入验证码</p>
        <el-form ref="captchaFormRef" :model="captchaForm" :rules="captchaRules">
          <el-form-item prop="code">
            <div class="captcha-wrapper">
              <el-input
                v-model="captchaForm.code"
                placeholder="请输入验证码"
                class="captcha-input"
                @keyup.enter="handleCaptchaSubmit"
                size="large"
              />
              <div class="captcha-content">
                <img
                  :src="captchaUrl"
                  alt="验证码"
                  class="captcha-image"
                  @click="refreshCaptcha"
                />
                <div class="captcha-refresh" @click="refreshCaptcha">
                  看不清？换一张
                </div>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCaptchaDialogClose">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleCaptchaSubmit"
            :loading="captchaLoading"
          >
            确认登录
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 服务条款弹窗 -->
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
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onUnmounted } from 'vue'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { userRegisterService, userLoginService, userInfoService, sendEmailCodeService } from '@/api/user.js'
import { getCaptchaImageUrl } from '@/api/captcha.js'
import { generateRandomString, initCaptchaUuid, setCaptchaUuid } from '@/utils/captchaUtils.js'

const isRegister = ref(false)
const loading = ref(false)
const uuid = ref('')
const loginFormRef = ref()
const registerFormRef = ref()
const captchaUrl = ref('')
const router = useRouter()
const authStore = useAuthStore()

// 验证码弹窗相关
const showCaptchaDialog = ref(false)
const captchaLoading = ref(false)
const captchaFormRef = ref()

// 邮箱验证码相关
const emailCodeDisabled = ref(false)
const emailCodeButtonText = ref('发送验证码')
let emailCodeTimer = null
let emailCodeCountdown = 60

// 服务条款弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogContent = ref('')

// 登录表单
const loginForm = reactive({
  username: '',
  password: '',
  agreeTerms: false
})

// 注册表单
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  emailCode: ''
})

// 验证码表单
const captchaForm = reactive({
  code: ''
})

const loginRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ]
})

const registerRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6个字符', trigger: 'blur' }
  ]
})

// 验证码表单验证规则
const captchaRules = reactive({
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为4个字符', trigger: 'blur' }
  ]
})

// 切换到登录模式
const switchToLogin = () => {
  isRegister.value = false
}

// 切换到注册模式
const switchToRegister = () => {
  isRegister.value = true
}

// 显示服务条款
const showTerms = () => {
  dialogTitle.value = '服务条款'
  dialogContent.value = `
    <div class="terms-content">
      <h3>一、服务条款概述</h3>
      <p>欢迎使用基于BERT的中文文本情感分析平台。本服务条款旨在明确您与我们之间的权利和义务，保障双方合法权益。</p>
      
      <h3>二、服务内容</h3>
      <p>我们为您提供基于深度学习技术的情感分析服务，包括但不限于：</p>
      <ul>
        <li>文本情感识别（积极、消极、中性等）</li>
        <li>情感强度分析</li>
        <li>历史数据分析与可视化</li>
        <li>个性化情感趋势报告</li>
      </ul>
      
      <h3>三、用户责任</h3>
      <p>在使用本服务时，您需遵守以下规定：</p>
      <ul>
        <li>不得上传违法、有害或侵犯他人权益的内容</li>
        <li>不得恶意攻击或尝试破解系统安全防护</li>
        <li>不得滥用服务资源影响其他用户正常使用</li>
        <li>尊重知识产权，不得盗用他人成果</li>
      </ul>
      
      <h3>四、免责条款</h3>
      <p>在法律允许范围内，我们不对以下情况承担责任：</p>
      <ul>
        <li>因不可抗力导致的服务中断</li>
        <li>因系统维护造成的临时性服务暂停</li>
        <li>因用户自身操作不当引发的问题</li>
        <li>分析结果仅供参考，不承担决策责任</li>
      </ul>
      
      <h3>五、隐私保护</h3>
      <p>我们严格遵守相关法律法规，保护您的个人信息安全。详细内容请参阅《隐私政策》。</p>
      
      <h3>六、条款变更</h3>
      <p>我们有权根据业务发展需要适时修订本条款，变更将通过公告形式通知用户。</p>
    </div>
  `
  dialogVisible.value = true
}

// 显示隐私政策
const showPrivacy = () => {
  dialogTitle.value = '隐私政策'
  dialogContent.value = `
    <div class="privacy-content">
      <h3>一、信息收集</h3>
      <p>为提供更好的服务，我们可能会收集以下信息：</p>
      <ul>
        <li><strong>基本信息</strong>：注册时提供的用户名、邮箱等</li>
        <li><strong>使用数据</strong>：访问时间、IP地址、操作日志等</li>
        <li><strong>分析内容</strong>：您提交的情感分析文本（仅用于处理）</li>
        <li><strong>设备信息</strong>：浏览器类型、操作系统等环境信息</li>
      </ul>
      
      <h3>二、信息使用</h3>
      <p>收集的信息将用于以下目的：</p>
      <ul>
        <li>提供和优化情感分析服务</li>
        <li>个性化用户体验和功能推荐</li>
        <li>系统安全防护和异常监测</li>
        <li>服务改进和新产品研发</li>
      </ul>
      
      <h3>三、信息安全</h3>
      <p>我们采取多重措施保护您的信息安全：</p>
      <ul>
        <li>数据加密传输和存储</li>
        <li>严格的访问权限控制</li>
        <li>定期安全审计和漏洞扫描</li>
        <li>员工保密协议和培训</li>
      </ul>
      
      <h3>四、信息共享</h3>
      <p>未经您同意，我们不会向第三方出售或提供您的个人信息，以下情况除外：</p>
      <ul>
        <li>根据法律法规要求</li>
        <li>为维护平台安全和用户权益</li>
        <li>经您同意的合作伙伴服务</li>
      </ul>
      
      <h3>五、Cookie使用</h3>
      <p>为提升浏览体验，我们使用Cookie技术存储必要的会话信息。</p>
      
      <h3>六、权利保障</h3>
      <p>您享有以下权利：</p>
      <ul>
        <li>查询、更正或删除个人信息</li>
        <li>撤回授权同意</li>
        <li>注销账户</li>
        <li>获取个人信息副本</li>
      </ul>
    </div>
  `
  dialogVisible.value = true
}

// 显示使用帮助
const showHelp = () => {
  dialogTitle.value = '使用帮助'
  dialogContent.value = `
    <div class="help-content">
      <h3>一、平台简介</h3>
      <p>本平台基于先进的RoBERTa深度学习模型，提供专业的中文文本情感分析服务，支持6种情感类别识别：积极、愤怒、悲伤、恐惧、惊讶、无情绪。</p>
      
      <h3>二、功能使用指南</h3>
      <h4>1. 情感分析</h4>
      <ol>
        <li>进入"情感分析"页面</li>
        <li>选择合适的分析模型（推荐使用默认模型）</li>
        <li>输入待分析的中文文本（建议10-200字）</li>
        <li>点击"开始分析"按钮</li>
        <li>查看详细的分析结果和置信度</li>
      </ol>
      
      <h4>2. 历史记录</h4>
      <p>所有分析记录将自动保存至"历史记录"页面，您可以随时查看、搜索和导出历史数据。</p>
      
      <h4>3. 数据可视化</h4>
      <p>在"数据可视化"页面，您可以查看情感分布统计图表，了解情感趋势变化。</p>
      
      <h3>三、最佳实践建议</h3>
      <ul>
        <li>文本长度建议：10-200字，过短或过长可能影响准确性</li>
        <li>语言要求：目前仅支持中文文本分析</li>
        <li>语境重要性：保持文本语境完整有助于提高分析准确性</li>
        <li>结果解读：分析结果仅供参考，建议结合实际语境综合判断</li>
      </ul>
      
      <h3>四、常见问题</h3>
      <h4>Q: 分析结果不准确怎么办？</h4>
      <p>A: 可尝试调整文本表达方式或选择不同模型进行对比分析。</p>
      
      <h4>Q: 支持哪些情感类别？</h4>
      <p>A: 目前支持6类情感：积极(happy)、愤怒(angry)、悲伤(sad)、恐惧(fear)、惊讶(surprise)、无情绪(neutral)。</p>
      
      <h4>Q: 如何导出分析结果？</h4>
      <p>A: 在历史记录页面可以选择导出PDF或Excel格式报告。</p>
      
      <h3>五、技术支持</h3>
      <p>如有其他问题，请联系客服或通过"意见反馈"功能提交您的建议。</p>
    </div>
  `
  dialogVisible.value = true
}

// 刷新图片验证码
const refreshCaptcha = () => {
  uuid.value = generateRandomString()
  setCaptchaUuid(uuid.value)
  // 使用时间戳避免缓存问题
  captchaUrl.value = `${getCaptchaImageUrl(uuid.value)}&t=${Date.now()}`
  captchaForm.code = ''
}

// 发送邮箱验证码
const sendEmailCode = async () => {
  if (!registerForm.email) {
    ElMessage.error('请输入邮箱地址')
    return
  }

  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.error('请输入正确的邮箱地址')
    return
  }

  try {
    await sendEmailCodeService(registerForm.email)
    ElMessage.success('验证码已发送，请查收邮箱')
    
    // 启动倒计时
    emailCodeDisabled.value = true
    emailCodeCountdown = 60
    updateEmailCodeButton()
    emailCodeTimer = setInterval(() => {
      emailCodeCountdown--
      updateEmailCodeButton()
      if (emailCodeCountdown <= 0) {
        clearInterval(emailCodeTimer)
        emailCodeDisabled.value = false
        emailCodeButtonText.value = '发送验证码'
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败', error)
    ElMessage.error(error.response?.data?.message || '发送验证码失败')
  }
}

// 更新邮箱验证码按钮文字
const updateEmailCodeButton = () => {
  if (emailCodeCountdown > 0) {
    emailCodeButtonText.value = `${emailCodeCountdown}秒后重发`
  } else {
    emailCodeButtonText.value = '发送验证码'
  }
}

// 重置登录表单
const resetLoginForm = () => {
  loginFormRef.value?.resetFields()
}

// 重置注册表单
const resetRegisterForm = () => {
  registerFormRef.value?.resetFields()
  registerForm.emailCode = ''
}

// 处理登录
const handleLogin = async () => {
  try {
    // 先验证基本表单信息
    await loginFormRef.value.validate()

    // 检查是否同意服务条款
    if (!loginForm.agreeTerms) {
      ElMessage.error('请先阅读并同意服务条款和隐私政策')
      return
    }

    loading.value = true

    // 登录逻辑：先验证用户名密码，再显示验证码弹窗
    loading.value = false // 先关闭loading，因为要显示弹窗
    
    // 初始化UUID并生成验证码
    uuid.value = initCaptchaUuid()
    captchaUrl.value = `${getCaptchaImageUrl(uuid.value)}&t=${Date.now()}`
    showCaptchaDialog.value = true
  } catch (error) {
    console.error('表单验证失败', error)
    ElMessage.error('请检查输入信息')
  } finally {
    loading.value = false
  }
}

// 处理注册
const handleRegister = async () => {
  try {
    // 先验证基本表单信息
    await registerFormRef.value.validate()

    loading.value = true

    // 注册逻辑保持不变
    const registerData = {
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email,
      code: registerForm.emailCode
    }

    try {
      await userRegisterService(registerData)
      ElMessage.success('注册成功')
      // 注册成功后切换到登录模式
      try {
        resetRegisterForm()
      } catch (e) {
        console.error('Reset form error:', e)
      }
      isRegister.value = false
    } catch (registerError) {
      console.error('注册失败', registerError)
      ElMessage.error(registerError.response?.data?.message || '注册失败')
    }
  } catch (error) {
    console.error('表单验证失败', error)
    ElMessage.error('请检查输入信息')
  } finally {
    loading.value = false
  }
}

// 处理验证码弹窗关闭
const handleCaptchaDialogClose = () => {
  showCaptchaDialog.value = false
  captchaForm.code = ''
  captchaFormRef.value?.resetFields()
}

// 处理验证码提交
const handleCaptchaSubmit = async () => {
  try {
    // 验证验证码表单
    await captchaFormRef.value.validate()
    
    captchaLoading.value = true

    // 执行登录请求
    const loginData = {
      username: loginForm.username,
      password: loginForm.password,
      uuid: uuid.value,
      code: captchaForm.code
    }

    try {
      const res = await userLoginService(loginData)
      ElMessage.success('登录成功')
      
      // 保存 token 到 store
      if (res.data && res.data.token) {
        authStore.setToken(res.data.token)

        // 获取并存储用户详细信息
        try {
          const userRes = await userInfoService()
          if (userRes.data) {
            authStore.setUserInfo(userRes.data)
          }
        } catch (userError) {
          console.error('获取用户详细信息失败', userError)
        }

        // 等待store更新完成
        await nextTick()
      }
      
      // 关闭弹窗并跳转
      showCaptchaDialog.value = false
      router.push('/')
    } catch (loginError) {
      console.error('登录失败', loginError)
      ElMessage.error(loginError.response?.data?.message || '登录失败')
      // 登录失败时刷新验证码
      refreshCaptcha()
      captchaForm.code = ''
    }
  } catch (error) {
    console.error('验证码验证失败', error)
    ElMessage.error('请输入正确的验证码')
  } finally {
    captchaLoading.value = false
  }
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer)
  }
})
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: url('@/assets/Background.png') center/cover no-repeat;
  padding: 20px;
  position: relative;
  
  // 添加半透明遮罩层
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 1;
  }

  .login-card {
    width: 100%;
    max-width: 450px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 20px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    padding: 40px;
    position: relative;
    z-index: 2;

    .card-header {
      text-align: center;
      margin-bottom: 30px;

      .logo {
        display: flex;
        justify-content: center;
        margin-bottom: 20px;

        .logo-placeholder {
          width: 80px;
          height: 80px;
          border-radius: 50%;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          margin: 0 auto;

          .logo-icon {
            font-size: 40px;
          }
        }
      }

      .app-title {
        font-size: 28px;
        font-weight: 700;
        margin-bottom: 8px;
        color: #333;
      }

      .app-subtitle {
        font-size: 16px;
        color: #666;
        margin: 0;
      }
    }

    .mode-toggle {
      display: flex;
      gap: 15px;
      margin-bottom: 30px;

      :deep(.el-button) {
        flex: 1;
        height: 45px;
        border-radius: 10px;
        font-size: 16px;
        font-weight: 600;
      }
    }
    
    .text-center {
      text-align: center;
      width: 100%;
      color: #333;
      font-weight: 600;
      margin: 0;
      padding-bottom: 10px;
      border-bottom: 2px solid #409eff;
    }

    .form-section {
      .login-form,
      .register-form {
        :deep(.el-form-item) {
          margin-bottom: 20px;
        }

        :deep(.el-input__wrapper) {
          border-radius: 10px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .code-button {
          width: 100%;
          border-radius: 10px;
        }

        .submit-button {
          width: 60%;
          height: 45px;
          border-radius: 10px;
          font-size: 16px;
          font-weight: 600;
          margin-top: 10px;
        }
        
        .reset-button {
          width: 30%;
          height: 45px;
          border-radius: 10px;
          font-size: 16px;
          margin-top: 10px;
          margin-left: 10%;
        }
        
        .agree-checkbox {
          margin-bottom: 15px;
          color: #666;
          
          :deep(.el-checkbox__input.is-checked+.el-checkbox__label) {
            color: #666;
          }
        }
      }
    }

    .features-section {
      display: flex;
      justify-content: space-around;
      margin-top: 30px;
      padding-top: 20px;
      border-top: 1px solid #eee;

      .feature-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;

        .feature-icon {
          font-size: 20px;
        }

        .feature-text {
          font-size: 12px;
          color: #666;
        }
      }
    }

    .form-footer {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 5px;
      margin-top: 15px;
      
      span {
        color: #666;
        font-size: 14px;
      }
      
      :deep(.el-button) {
        padding: 0;
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .login-container {
    padding: 15px;

    .login-card {
      padding: 30px 20px;

      .card-header {
        .logo {
          .logo-placeholder {
            width: 60px;
            height: 60px;

            .logo-icon {
              font-size: 30px;
            }
          }
        }

        .app-title {
          font-size: 24px;
        }

        .app-subtitle {
          font-size: 14px;
        }
      }

      .form-section {
        .login-form,
        .register-form {
          :deep(.el-form-item__label) {
            font-size: 14px;
          }

          :deep(.el-input__inner) {
            font-size: 14px;
          }

          .submit-button {
            height: 40px;
            font-size: 14px;
          }
          
          .reset-button {
            height: 40px;
            font-size: 14px;
          }
        }
      }

      .features-section {
        .feature-item {
          .feature-icon {
            font-size: 16px;
          }

          .feature-text {
            font-size: 11px;
          }
        }
      }
      
      .form-footer {
        span {
          font-size: 12px;
        }
      }
    }
  }
}

// 验证码弹窗样式
.captcha-dialog-content {
  text-align: center;
  
  .captcha-tip {
    margin-bottom: 20px;
    color: #666;
    font-size: 14px;
  }
  
  .captcha-wrapper {
    display: flex;
    flex-direction: column;
    gap: 15px;
    align-items: center;

    .captcha-input {
      width: 100%;
    }

    .captcha-content {
      display: flex;
      flex-direction: column;
      align-items: center;

      .captcha-image {
        width: 150px;
        height: 50px;
        cursor: pointer;
        border: 1px solid #dcdfe6;
        border-radius: 8px;
        transition: all 0.3s ease;

        &:hover {
          border-color: #409eff;
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
        }
      }

      .captcha-refresh {
        font-size: 12px;
        color: #409eff;
        cursor: pointer;
        margin-top: 5px;
        transition: all 0.3s ease;

        &:hover {
          color: #66b1ff;
          text-decoration: underline;
        }
      }
    }
  }
}

.dialog-footer {
  text-align: center;
}

:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__header) {
  text-align: center;
  padding: 20px 20px 10px;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}

/* 内容对话框样式 */
.content-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.dialog-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 20px;
  line-height: 1.6;
}

.dialog-content h3 {
  margin: 20px 0 10px;
  color: #303133;
  font-size: 18px;
}

.dialog-content h4 {
  margin: 15px 0 8px;
  color: #606266;
  font-size: 16px;
}

.dialog-content p {
  margin: 8px 0;
  color: #606266;
}

.dialog-content ul {
  padding-left: 20px;
  margin: 10px 0;
}

.dialog-content li {
  margin: 5px 0;
  color: #606266;
}

.dialog-content strong {
  color: #303133;
}

:deep(.content-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 20px;
  }
  
  .el-dialog__title {
    color: white;
    font-weight: 600;
  }
  
  .el-dialog__headerbtn {
    top: 20px;
  }
  
  .el-dialog__headerbtn .el-dialog__close {
    color: white;
  }
  
  .el-dialog__body {
    padding: 0;
  }
  
  .el-dialog__footer {
    padding: 20px;
  }
}
</style>
