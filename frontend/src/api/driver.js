import request from './request'

export async function createDriverRecord(payload) {
  const response = await request.post('/driver/records', payload)
  return response.data.data
}

export async function resolveLocationAddress(latitude, longitude) {
  const response = await request.post('/driver/locations/address', { latitude, longitude })
  return response.data.data
}
