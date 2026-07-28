const SAVED_INFO_KEY = 'driver-common-info'

export function createSubmissionToken() {
  if (crypto.randomUUID) return crypto.randomUUID()
  const bytes = new Uint8Array(16)
  if (crypto.getRandomValues) {
    crypto.getRandomValues(bytes)
  } else {
    for (let index = 0; index < bytes.length; index++) bytes[index] = Math.floor(Math.random() * 256)
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = [...bytes].map((value) => value.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function mapLocationError(errorCode) {
  if (errorCode === 1) return 'DENIED'
  if (errorCode === 3) return 'TIMEOUT'
  return 'FAILED'
}

export function loadSavedDriverInfo(storage = localStorage) {
  try {
    return JSON.parse(storage.getItem(SAVED_INFO_KEY)) || {}
  } catch {
    return {}
  }
}

export function saveDriverInfo(form, storage = localStorage) {
  storage.setItem(SAVED_INFO_KEY, JSON.stringify({
    driverName: form.driverName,
    phone: form.phone,
    licensePlate: form.licensePlate,
    vehicleType: form.vehicleType
  }))
}

export function clearSavedDriverInfo(storage = localStorage) {
  storage.removeItem(SAVED_INFO_KEY)
}
