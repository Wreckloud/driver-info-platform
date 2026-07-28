import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(utc)
dayjs.extend(timezone)
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const ZONE = 'Asia/Shanghai'
const ONE_DAY_MS = 24 * 60 * 60 * 1000
const RELATIVE_TIME_LIMIT_MS = 7 * ONE_DAY_MS

export function formatTime(value) {
  return value ? dayjs.utc(value).tz(ZONE).format('YYYY-MM-DD HH:mm:ss') : '—'
}

export function formatRelativeTime(value, now = Date.now()) {
  if (!value) return '—'
  const reference = dayjs.utc(now)
  const submittedAt = dayjs.utc(value)
  // 网络延迟或时间同步抖动不应让刚提交的记录显示成“几秒内”。
  return (submittedAt.isAfter(reference) ? reference : submittedAt).tz(ZONE).from(reference)
}

export function formatRecordTime(value, now = Date.now()) {
  if (!value) return { relative: '', exact: '—', full: '—', isOld: true }
  const reference = dayjs.utc(now)
  const submittedAt = dayjs.utc(value)
  const elapsed = Math.max(0, reference.diff(submittedAt))
  const localSubmittedAt = submittedAt.tz(ZONE)
  const isOld = elapsed >= RELATIVE_TIME_LIMIT_MS
  return {
    relative: isOld ? '' : formatRelativeTime(value, now),
    exact: isOld
      ? localSubmittedAt.format('YYYY-MM-DD HH:mm:ss')
      : localSubmittedAt.format(elapsed < ONE_DAY_MS ? 'HH:mm:ss' : 'MM-DD HH:mm'),
    full: localSubmittedAt.format('YYYY-MM-DD HH:mm:ss'),
    isOld
  }
}
