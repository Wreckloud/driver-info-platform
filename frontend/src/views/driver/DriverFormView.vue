<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, Close, Location, RefreshRight, Van } from '@element-plus/icons-vue'
import { createDriverRecord, resolveLocationAddress } from '@/api/driver'
import { LOCATION_STATUS_LABELS, locationCanRetry, locationDisplayText } from '@/constants/location'
import {
  clearSavedDriverInfo,
  createSubmissionToken,
  loadSavedDriverInfo,
  mapLocationError,
  saveDriverInfo
} from '@/utils/driver'
import { compressPhoto } from '@/utils/photo'

const SUCCESS_KEY = 'driver-submit-success'
const router = useRouter()
const formRef = ref()
const locating = ref(false)
const resolvingLocation = ref(false)
const locationAddress = ref('')
const submitting = ref(false)
const confirmVisible = ref(false)
const pendingPayload = ref(null)
const pendingLocationAddress = ref('')
const photoInput = ref()
const photoItems = ref([])
const processingPhotos = ref(false)
let locationRequestSequence = 0

const savedInfo = loadSavedDriverInfo()
const form = reactive({
  submissionToken: createSubmissionToken(),
  project: '',
  driverName: savedInfo.driverName || '',
  phone: savedInfo.phone || '',
  licensePlate: savedInfo.licensePlate || '',
  vehicleType: savedInfo.vehicleType || '',
  quantity: '',
  destination: '',
  remark: '',
  locationStatus: 'NOT_REQUESTED',
  latitude: null,
  longitude: null,
  locationAccuracy: null,
  locatedAt: null
})

const rules = {
  project: [
    { required: true, message: '请输入项目', trigger: 'blur' },
    { max: 100, message: '项目不能超过 100 字', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5A-Za-z0-9 ]+$/, message: '项目只能包含汉字、英文字母、数字和空格', trigger: 'blur' }
  ],
  driverName: [{ required: true, message: '请输入姓名', trigger: 'blur' }, { max: 50, message: '姓名不能超过 50 字', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }],
  licensePlate: [{ required: true, message: '请输入车牌号', trigger: 'blur' }, { pattern: /^[\u4e00-\u9fa5A-Za-z0-9-]{5,12}$/, message: '请输入正确的车牌号', trigger: 'blur' }],
  vehicleType: [{ required: true, message: '请输入车型', trigger: 'blur' }, { max: 50, message: '车型不能超过 50 字', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }, { max: 100, message: '数量不能超过 100 字', trigger: 'blur' }],
  destination: [{ required: true, message: '请输入目的地', trigger: 'blur' }, { max: 200, message: '目的地不能超过 200 字', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过 500 字', trigger: 'blur' }]
}

const locationTone = computed(() => form.locationStatus === 'SUCCESS' ? 'success' : form.locationStatus === 'NOT_REQUESTED' ? 'neutral' : 'warning')
const locationText = computed(() => {
  if (locating.value) return '正在获取位置…'
  if (resolvingLocation.value) return '正在解析位置…'
  return locationDisplayText(form.locationStatus, locationAddress.value)
})
const locationRetryable = computed(() => !locating.value && !resolvingLocation.value && locationCanRetry(form.locationStatus))
const photoPreviewUrls = computed(() => photoItems.value.map((item) => item.url))

function clearLocation() {
  form.latitude = null
  form.longitude = null
  form.locationAccuracy = null
  form.locatedAt = null
  locationAddress.value = ''
  resolvingLocation.value = false
}

function locate() {
  const requestSequence = ++locationRequestSequence
  clearLocation()
  if (!navigator.geolocation) {
    form.locationStatus = 'FAILED'
    ElMessage.warning('当前浏览器不支持定位，仍可继续提交')
    return
  }
  locating.value = true
  form.locationStatus = 'NOT_REQUESTED'
  navigator.geolocation.getCurrentPosition(
    async (position) => {
      if (requestSequence !== locationRequestSequence) return
      form.latitude = position.coords.latitude
      form.longitude = position.coords.longitude
      form.locationAccuracy = position.coords.accuracy
      form.locatedAt = new Date(position.timestamp).toISOString()
      form.locationStatus = 'SUCCESS'
      locating.value = false
      resolvingLocation.value = true
      try {
        const result = await resolveLocationAddress(form.latitude, form.longitude)
        if (requestSequence === locationRequestSequence) {
          locationAddress.value = result.address || ''
        }
      } catch (error) {
        if (requestSequence === locationRequestSequence) {
          ElMessage.warning(error.status === 429 ? error.message : '坐标已获取，但文字地址解析失败')
        }
      } finally {
        if (requestSequence === locationRequestSequence) {
          resolvingLocation.value = false
        }
      }
    },
    (error) => {
      if (requestSequence !== locationRequestSequence) return
      clearLocation()
      form.locationStatus = mapLocationError(error.code)
      locating.value = false
      ElMessage.warning(`${LOCATION_STATUS_LABELS[form.locationStatus]}，仍可继续提交`)
    },
    { enableHighAccuracy: true, maximumAge: 0, timeout: 10000 }
  )
}

function retryLocation() {
  if (locationRetryable.value) locate()
}

function openCamera() {
  if (photoItems.value.length >= 9 || processingPhotos.value) return
  photoInput.value?.click()
}

async function capturePhoto(event) {
  const source = event.target.files?.[0]
  event.target.value = ''
  if (!source || photoItems.value.length >= 9) return
  processingPhotos.value = true
  try {
    const file = await compressPhoto(source)
    photoItems.value.push({ file, url: URL.createObjectURL(file) })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    processingPhotos.value = false
  }
}

function removePhoto(index) {
  const [removed] = photoItems.value.splice(index, 1)
  if (removed) URL.revokeObjectURL(removed.url)
}

async function clearSavedInfo() {
  await ElMessageBox.confirm('将清除本机记住的姓名、电话、车牌和车型，是否继续？', '清除常用信息', { type: 'warning' })
  clearSavedDriverInfo()
  form.driverName = ''
  form.phone = ''
  form.licensePlate = ''
  form.vehicleType = ''
  ElMessage.success('常用信息已清除')
}

async function requestConfirmation() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || submitting.value) return
  if (!photoItems.value.length) {
    ElMessage.warning('请至少拍摄一张照片')
    return
  }
  if (processingPhotos.value) {
    ElMessage.info('照片正在压缩，请稍候')
    return
  }
  pendingPayload.value = {
    ...form,
    project: form.project.trim(),
    driverName: form.driverName.trim(),
    phone: form.phone.trim(),
    licensePlate: form.licensePlate.trim().toUpperCase(),
    vehicleType: form.vehicleType.trim(),
    quantity: form.quantity.trim(),
    destination: form.destination.trim(),
    remark: form.remark.trim() || null
  }
  pendingLocationAddress.value = locationAddress.value
  confirmVisible.value = true
}

async function confirmSubmit() {
  if (!pendingPayload.value || submitting.value) return
  submitting.value = true
  try {
    const payload = pendingPayload.value
    const result = await createDriverRecord(payload, photoItems.value.map((item) => item.file))
    saveDriverInfo(payload)
    sessionStorage.setItem(SUCCESS_KEY, JSON.stringify(result))
    confirmVisible.value = false
    await router.replace('/driver/success')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

onMounted(locate)

onUnmounted(() => {
  locationRequestSequence++
  photoItems.value.forEach((item) => URL.revokeObjectURL(item.url))
})
</script>

<template>
  <main class="driver-page">
    <section class="driver-card">
      <header class="driver-header">
        <div class="brand-mark"><el-icon><Van /></el-icon></div>
        <div>
          <p class="eyebrow">HAOYUAN HONGTU</p>
          <h1>信息登记</h1>
          <p>每次出车请重新提交一条记录</p>
        </div>
      </header>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="requestConfirmation">
        <el-form-item label="项目" prop="project">
          <el-input v-model.trim="form.project" maxlength="100" show-word-limit placeholder="请输入本次业务项目" />
        </el-form-item>
        <div class="field-grid">
          <el-form-item label="姓名" prop="driverName">
            <el-input v-model.trim="form.driverName" maxlength="50" placeholder="请输入司机姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model.trim="form.phone" maxlength="11" inputmode="numeric" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="车牌号" prop="licensePlate">
            <el-input v-model.trim="form.licensePlate" maxlength="12" placeholder="例如：京A12345" @blur="form.licensePlate = form.licensePlate.toUpperCase()" />
          </el-form-item>
          <el-form-item label="车型" prop="vehicleType">
            <el-input v-model.trim="form.vehicleType" maxlength="50" placeholder="例如：厢式货车" />
          </el-form-item>
        </div>
        <el-form-item label="数量" prop="quantity">
          <el-input v-model.trim="form.quantity" maxlength="100" show-word-limit placeholder="例如：20件（冻品）" />
        </el-form-item>
        <el-form-item label="目的地" prop="destination">
          <el-input v-model.trim="form.destination" maxlength="200" show-word-limit placeholder="请输入本次目的地" />
        </el-form-item>
        <el-form-item label="备注（选填）" prop="remark">
          <el-input v-model.trim="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="可填写本次出车的补充说明" />
        </el-form-item>

        <section :class="['evidence-panel', { 'is-retryable': locationRetryable }]">
          <div class="location-row">
            <div
              class="location-copy"
              :role="locationRetryable ? 'button' : undefined"
              :tabindex="locationRetryable ? 0 : undefined"
              :aria-label="locationRetryable ? '重新获取起始位置' : undefined"
              @click="retryLocation"
              @keydown.enter="retryLocation"
              @keydown.space.prevent="retryLocation"
            >
              <div class="location-title"><el-icon><Location /></el-icon><span>起始位置</span></div>
              <p :class="['location-state', locationTone]">{{ locationText }}</p>
              <span v-if="locationRetryable" class="location-retry-hint"><el-icon><RefreshRight /></el-icon>点击卡片重新获取</span>
              <small>定位失败或拒绝授权仍可提交。</small>
            </div>
            <input ref="photoInput" class="visually-hidden" type="file" accept="image/*" capture="environment" @change="capturePhoto" />
            <el-button :loading="processingPhotos" :disabled="photoItems.length >= 9" :icon="Camera" @click="openCamera">
              {{ photoItems.length ? `继续拍照 ${photoItems.length}/9` : '拍摄照片' }}
            </el-button>
          </div>

          <div class="photo-section">
            <div class="photo-panel-heading">
              <div><strong>出车照片</strong><span>至少 1 张，最多 9 张；拍摄后自动压缩</span></div>
              <small>点击照片可放大</small>
            </div>
            <div v-if="photoItems.length" class="photo-grid">
              <div v-for="(photo, index) in photoItems" :key="photo.url" class="photo-item">
                <el-image :src="photo.url" fit="cover" :preview-src-list="photoPreviewUrls" :initial-index="index" preview-teleported />
                <button type="button" aria-label="删除照片" @click="removePhoto(index)"><el-icon><Close /></el-icon></button>
              </div>
            </div>
            <p v-else class="photo-empty">尚未拍摄照片</p>
          </div>
        </section>

        <el-button class="submit-button" type="primary" size="large" native-type="submit" :loading="submitting">
          核对并提交登记
        </el-button>
      </el-form>

      <button class="text-action" type="button" @click="clearSavedInfo">清除本机保存的常用信息</button>
    </section>

    <el-dialog
      v-model="confirmVisible"
      title="请再次确认登记信息"
      width="520px"
      class="driver-confirm-dialog"
      :close-on-click-modal="!submitting"
      :close-on-press-escape="!submitting"
      :show-close="!submitting"
    >
      <p class="confirm-hint">提交后将生成一条新的出车记录，请确认以下信息无误。</p>
      <dl v-if="pendingPayload" class="confirm-list">
        <div><dt>项目</dt><dd>{{ pendingPayload.project }}</dd></div>
        <div><dt>姓名</dt><dd>{{ pendingPayload.driverName }}</dd></div>
        <div><dt>手机号</dt><dd>{{ pendingPayload.phone }}</dd></div>
        <div><dt>车牌号</dt><dd>{{ pendingPayload.licensePlate }}</dd></div>
        <div><dt>车型</dt><dd>{{ pendingPayload.vehicleType }}</dd></div>
        <div><dt>数量</dt><dd>{{ pendingPayload.quantity }}</dd></div>
        <div><dt>目的地</dt><dd>{{ pendingPayload.destination }}</dd></div>
        <div><dt>备注</dt><dd>{{ pendingPayload.remark || '—' }}</dd></div>
        <div><dt>定位状态</dt><dd>{{ LOCATION_STATUS_LABELS[pendingPayload.locationStatus] }}</dd></div>
        <div>
          <dt>起始位置</dt>
          <dd>{{ locationDisplayText(pendingPayload.locationStatus, pendingLocationAddress) }}</dd>
        </div>
        <div><dt>照片</dt><dd>共 {{ photoItems.length }} 张</dd></div>
      </dl>
      <div v-if="photoItems.length" class="confirm-photo-grid">
        <el-image
          v-for="(photo, index) in photoItems"
          :key="photo.url"
          :src="photo.url"
          fit="cover"
          :preview-src-list="photoPreviewUrls"
          :initial-index="index"
          preview-teleported
        />
      </div>
      <template #footer>
        <el-button :disabled="submitting" @click="confirmVisible = false">返回修改</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmSubmit">确认无误并提交</el-button>
      </template>
    </el-dialog>
  </main>
</template>
