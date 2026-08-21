import request from './request'

export async function createDriverRecord(payload, photos) {
  const formData = new FormData()
  formData.append('record', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  photos.forEach((photo) => formData.append('photos', photo, photo.name))
  const response = await request.post('/driver/records', formData, { timeout: 60000 })
  return response.data.data
}

export async function resolveLocationAddress(latitude, longitude) {
  const response = await request.post('/driver/locations/address', { latitude, longitude })
  return response.data.data
}

export async function loadDriverRecordPhoto(photoId, submissionToken) {
  const response = await request.get(`/driver/record-photos/${photoId}`, {
    headers: { 'X-Submission-Token': submissionToken },
    responseType: 'blob'
  })
  return URL.createObjectURL(response.data)
}
