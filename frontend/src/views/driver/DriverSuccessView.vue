<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { loadDriverRecordPhoto } from '@/api/driver'
import { LOCATION_STATUS_LABELS } from '@/constants/location'
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

const photoItems = ref([])
const photosLoading = ref(false)
const photosFailed = ref(false)
const locationLabel = computed(() => LOCATION_STATUS_LABELS[result?.locationStatus] || '未知')
const locationAddress = computed(() => {
  if (result?.locationStatus !== 'SUCCESS') return locationLabel.value
  return result.locationAddress || '位置已获取，文字地址解析失败'
})
const departureTime = computed(() => formatTime(result?.createdAt))
const photoPreviewUrls = computed(() => photoItems.value.map((photo) => photo.url))

async function loadPhotos() {
  if (!result?.photos?.length || !result.submissionToken) return
  photosLoading.value = true
  const loadedPhotos = await Promise.allSettled(result.photos.map(async (photo) => ({
    ...photo,
    url: await loadDriverRecordPhoto(photo.id, result.submissionToken)
  })))
  photoItems.value = loadedPhotos
    .filter((photo) => photo.status === 'fulfilled')
    .map((photo) => photo.value)
  photosFailed.value = photoItems.value.length !== result.photos.length
  photosLoading.value = false
}

function continueRegistering() {
  sessionStorage.removeItem(SUCCESS_KEY)
  router.replace('/driver')
}

onMounted(loadPhotos)

onUnmounted(() => {
  photoItems.value.forEach((photo) => URL.revokeObjectURL(photo.url))
})
</script>

<template>
  <main v-if="result" class="driver-page success-page">
    <section class="driver-card success-card">
      <header class="success-receipt">
        <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
        <div>
          <h1>登记成功</h1>
          <p><span>发车时间</span><time :datetime="result.createdAt">{{ departureTime }}</time></p>
        </div>
      </header>
      <dl class="summary-list summary-list--compact">
        <div class="summary-item--wide"><dt>项目</dt><dd>{{ result.project }}</dd></div>
        <div><dt>姓名</dt><dd>{{ result.driverName }}</dd></div>
        <div><dt>手机号</dt><dd>{{ result.phone || '—' }}</dd></div>
        <div><dt>车牌号</dt><dd>{{ result.licensePlate }}</dd></div>
        <div><dt>车型</dt><dd>{{ result.vehicleType || '—' }}</dd></div>
        <div class="summary-item--wide"><dt>数量</dt><dd>{{ result.quantity }}</dd></div>
        <div class="summary-item--wide"><dt>目的地</dt><dd>{{ result.destination }}</dd></div>
        <div v-if="result.remark" class="summary-item--wide"><dt>备注</dt><dd>{{ result.remark }}</dd></div>
        <div class="summary-item--wide"><dt>起始位置</dt><dd>{{ locationAddress }}</dd></div>
      </dl>
      <section class="success-photo-section" aria-label="出车照片">
        <div class="success-photo-heading"><span>出车照片</span><strong>{{ result.photoCount }} 张</strong></div>
        <div v-if="photoItems.length" class="confirm-photo-grid success-photo-grid">
          <el-image
            v-for="(photo, index) in photoItems"
            :key="photo.id"
            :src="photo.url"
            fit="cover"
            :preview-src-list="photoPreviewUrls"
            :initial-index="index"
            preview-teleported
          />
        </div>
        <p v-else-if="photosLoading" class="success-photo-state">照片加载中…</p>
        <p v-else class="success-photo-state">{{ photosFailed ? '部分照片暂时无法加载' : '照片暂时无法查看' }}</p>
      </section>
      <el-button class="submit-button" type="primary" size="large" @click="continueRegistering">继续登记</el-button>
    </section>
  </main>
</template>
