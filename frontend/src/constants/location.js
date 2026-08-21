export const LOCATION_STATUS_LABELS = {
  SUCCESS: '定位成功',
  DENIED: '拒绝授权',
  FAILED: '定位失败',
  TIMEOUT: '定位超时',
  NOT_REQUESTED: '未获取定位'
}

export function locationStatusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'NOT_REQUESTED') return 'info'
  return 'warning'
}

export function locationCanRetry(status) {
  return ['DENIED', 'FAILED', 'TIMEOUT'].includes(status)
}

export function locationDisplayText(status, address) {
  if (status === 'SUCCESS') return address || '已获取当前位置，文字地址解析失败'
  return LOCATION_STATUS_LABELS[status]
}
