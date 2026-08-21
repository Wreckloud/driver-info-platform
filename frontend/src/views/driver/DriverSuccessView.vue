<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { LOCATION_STATUS_LABELS, locationStatusType } from '@/constants/location'
import { formatTime } from '@/utils/time'

const SUCCESS_KEY = 'driver-submit-success'
const router = useRouter()
let result = null
try {
  result = JSON.parse(sessionStorage.getItem(SUCCESS_KEY))
} catch {
  result = null
}
if (!result) router.replace('/driver')

const locationLabel = computed(() => LOCATION_STATUS_LABELS[result?.locationStatus] || '未知')
const locationAddress = computed(() => {
  if (result?.locationStatus !== 'SUCCESS') return locationLabel.value
  return result.locationAddress || '位置已获取，文字地址解析失败'
})

function continueRegistering() {
  sessionStorage.removeItem(SUCCESS_KEY)
  router.replace('/driver')
}
</script>

<template>
  <main v-if="result" class="driver-page success-page">
    <section class="driver-card success-card">
      <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
      <p class="eyebrow">SUBMITTED</p>
      <h1>登记成功</h1>
      <p class="success-hint">本次出车信息已安全提交</p>
      <dl class="summary-list">
        <div><dt>项目</dt><dd>{{ result.project }}</dd></div>
        <div><dt>姓名</dt><dd>{{ result.driverName }}</dd></div>
        <div><dt>车牌</dt><dd>{{ result.licensePlate }}</dd></div>
        <div><dt>数量</dt><dd>{{ result.quantity }}</dd></div>
        <div><dt>目的地</dt><dd>{{ result.destination }}</dd></div>
        <div v-if="result.remark"><dt>备注</dt><dd>{{ result.remark }}</dd></div>
        <div><dt>定位状态</dt><dd><el-tag :type="locationStatusType(result.locationStatus)">{{ locationLabel }}</el-tag></dd></div>
        <div><dt>起始位置</dt><dd>{{ locationAddress }}</dd></div>
        <div><dt>发车时间</dt><dd>{{ formatTime(result.createdAt) }}</dd></div>
      </dl>
      <el-button class="submit-button" type="primary" size="large" @click="continueRegistering">继续登记</el-button>
    </section>
  </main>
</template>
