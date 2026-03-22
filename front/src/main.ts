import './assets/main.css'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// @ts-ignore
import locale from 'element-plus/dist/locale/zh-cn.mjs'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createPersistedState } from 'pinia-plugin-persistedstate'

// @ts-ignore
import App from './App.vue'
import router from './router'

// 创建 Pinia 实例，并启用持久化插件
const pinia = createPinia()
pinia.use(createPersistedState())

const app = createApp(App)

app.use(ElementPlus, { locale })
app.use(pinia)
app.use(router)

app.mount('#app')
