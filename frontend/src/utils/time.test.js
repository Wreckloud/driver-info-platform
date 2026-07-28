import { describe, expect, it } from 'vitest'
import { formatRecordTime, formatRelativeTime, formatTime } from './time'

describe('time utilities', () => {
  it('formats absolute time in Asia/Shanghai', () => {
    expect(formatTime('2026-07-27T04:06:49Z')).toBe('2026-07-27 12:06:49')
  })

  it('calculates relative time from the supplied server time', () => {
    expect(formatRelativeTime('2026-07-27T04:03:49Z', '2026-07-27T04:06:49Z')).toBe('3 分钟前')
  })

  it('treats a slightly future submission as just now', () => {
    expect(formatRelativeTime('2026-07-27T04:06:50Z', '2026-07-27T04:06:49Z')).toBe('几秒前')
  })

  it('uses a compact exact time for records newer than seven days', () => {
    expect(formatRecordTime('2026-07-27T04:03:49Z', '2026-07-27T04:06:49Z')).toEqual({
      relative: '3 分钟前', exact: '12:03:49', full: '2026-07-27 12:03:49', isOld: false
    })
    expect(formatRecordTime('2026-07-24T04:06:49Z', '2026-07-27T04:06:49Z')).toEqual({
      relative: '3 天前', exact: '07-24 12:06', full: '2026-07-24 12:06:49', isOld: false
    })
  })

  it('shows only full absolute time from seven days onward', () => {
    expect(formatRecordTime('2026-07-20T04:06:49Z', '2026-07-27T04:06:49Z')).toEqual({
      relative: '', exact: '2026-07-20 12:06:49', full: '2026-07-20 12:06:49', isOld: true
    })
  })
})
