import { describe, expect, it } from 'vitest'
import { LOCATION_STATUS_LABELS, locationCanRetry, locationDisplayText, locationStatusType } from './location'

describe('location status presentation', () => {
  it('contains all backend statuses', () => {
    expect(Object.keys(LOCATION_STATUS_LABELS).sort()).toEqual(
      ['DENIED', 'FAILED', 'NOT_REQUESTED', 'SUCCESS', 'TIMEOUT'].sort()
    )
  })

  it('uses distinct display tones', () => {
    expect(locationStatusType('SUCCESS')).toBe('success')
    expect(locationStatusType('NOT_REQUESTED')).toBe('info')
    expect(locationStatusType('FAILED')).toBe('warning')
  })

  it('only allows retry after a failed location attempt', () => {
    expect(locationCanRetry('DENIED')).toBe(true)
    expect(locationCanRetry('FAILED')).toBe(true)
    expect(locationCanRetry('TIMEOUT')).toBe(true)
    expect(locationCanRetry('SUCCESS')).toBe(false)
    expect(locationCanRetry('NOT_REQUESTED')).toBe(false)
  })

  it('prefers the resolved address and does not expose coordinates', () => {
    expect(locationDisplayText('SUCCESS', '江苏省苏州市张家港市')).toBe('江苏省苏州市张家港市')
    expect(locationDisplayText('SUCCESS', '')).toBe('已获取当前位置，文字地址解析失败')
    expect(locationDisplayText('DENIED', '')).toBe('拒绝授权')
  })
})
