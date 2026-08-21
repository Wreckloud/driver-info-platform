import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import '@/styles/main.scss'
import App from './App.vue'
import router from './router'

createApp(App).use(createPinia()).use(router).mount('#app')
