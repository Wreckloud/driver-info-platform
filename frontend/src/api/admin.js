import request, { refreshCsrfToken } from './request'

export async function login(payload) {
  await refreshCsrfToken()
  const response = await request.post('/admin/auth/login', payload)
  return response.data.data
}

export async function logout() {
  await refreshCsrfToken()
  await request.post('/admin/auth/logout')
}

export async function getCurrentAdmin() {
  const response = await request.get('/admin/auth/me')
  return response.data.data
}

export async function getRecords(params) {
  const response = await request.get('/admin/records', { params })
  return response.data.data
}

export async function getRecord(id) {
  const response = await request.get(`/admin/records/${id}`)
  return response.data.data
}

export async function updateRecord(id, payload) {
  await refreshCsrfToken()
  const response = await request.put(`/admin/records/${id}`, payload)
  return response.data.data
}

export async function deleteRecord(id) {
  await refreshCsrfToken()
  await request.delete(`/admin/records/${id}`)
}

export async function exportRecords(params) {
  const response = await request.get('/admin/records/export', {
    params,
    responseType: 'blob'
  })
  const disposition = response.headers['content-disposition'] || ''
  const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  return {
    blob: response.data,
    filename: encodedName ? decodeURIComponent(encodedName) : `司机出车登记_${Date.now()}.xlsx`
  }
}
