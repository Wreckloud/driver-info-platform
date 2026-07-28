import { describe, expect, it } from 'vitest'
import {
  clearSavedDriverInfo,
  createSubmissionToken,
  loadSavedDriverInfo,
  mapLocationError,
  saveDriverInfo
} from './driver'

function memoryStorage() {
  const values = new Map()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key)
  }
}

describe('driver utilities', () => {
  it('always creates a backend-compatible UUID', () => {
    expect(createSubmissionToken()).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i)
  })

  it('maps browser geolocation errors to business statuses', () => {
    expect(mapLocationError(1)).toBe('DENIED')
    expect(mapLocationError(2)).toBe('FAILED')
    expect(mapLocationError(3)).toBe('TIMEOUT')
  })

  it('stores only reusable driver fields', () => {
    const storage = memoryStorage()
    saveDriverInfo({
      driverName: '张三', phone: '13800138000', licensePlate: '京A12345',
      vehicleType: '厢式货车', destination: '天津', latitude: 39.9
    }, storage)
    expect(loadSavedDriverInfo(storage)).toEqual({
      driverName: '张三', phone: '13800138000', licensePlate: '京A12345', vehicleType: '厢式货车'
    })
    clearSavedDriverInfo(storage)
    expect(loadSavedDriverInfo(storage)).toEqual({})
  })

  it('ignores damaged local storage data', () => {
    const storage = { getItem: () => '{broken' }
    expect(loadSavedDriverInfo(storage)).toEqual({})
  })
})
