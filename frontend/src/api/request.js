import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
  // Spring 返回的是防 BREACH 的令牌，不能被 Axios 用 Cookie 原值覆盖。
  withXSRFToken: false
})

let csrfHeader = 'X-XSRF-TOKEN'
let csrfToken = ''

request.interceptors.request.use((config) => {
  if (csrfToken && !['get', 'head', 'options'].includes(config.method?.toLowerCase())) {
    config.headers[csrfHeader] = csrfToken
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.data?.code === 0) {
      return Promise.reject(new Error(response.data.msg || '操作失败'))
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.msg || error.message || '网络请求失败'
    const normalized = new Error(message)
    normalized.status = error.response?.status
    if (normalized.status === 401 && window.location.pathname.startsWith('/admin')
      && window.location.pathname !== '/admin/login') {
      window.location.assign('/admin/login?expired=1')
    }
    return Promise.reject(normalized)
  }
)

export async function refreshCsrfToken() {
  const response = await request.get('/admin/auth/csrf')
  csrfHeader = response.data.data.headerName
  csrfToken = response.data.data.token
  return csrfToken
}

export default request
