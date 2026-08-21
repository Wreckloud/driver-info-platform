<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getRecord, updateRecord } from '@/api/admin'
import { LOCATION_STATUS_LABELS, locationStatusType } from '@/constants/location'
import { formatTime } from '@/utils/time'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const record = ref(null)
const canManage = computed(() => Boolean(auth.admin?.canManage))
const photoUrls = computed(() => record.value?.photos?.map((photo) => photo.url) || [])
const form = reactive({ project: '', driverName: '', phone: '', licensePlate: '', vehicleType: '', quantity: '', destination: '', remark: '' })
const rules = {
  project: [
    { required: true, message: '请输入项目', trigger: 'blur' },
    { max: 100, message: '项目不能超过 100 字', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5A-Za-z0-9 ]+$/, message: '项目只能包含汉字、英文字母、数字和空格', trigger: 'blur' }
  ],
  driverName: [{ required: true, message: '请输入姓名', trigger: 'blur' }, { max: 50, message: '姓名不能超过 50 字', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  licensePlate: [{ required: true, message: '请输入车牌号', trigger: 'blur' }, { pattern: /^[\u4e00-\u9fa5A-Za-z0-9-]{5,12}$/, message: '车牌号格式不正确', trigger: 'blur' }],
  vehicleType: [{ required: true, message: '请输入车型', trigger: 'blur' }, { max: 50, message: '车型不能超过 50 字', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }, { max: 100, message: '数量不能超过 100 字', trigger: 'blur' }],
  destination: [{ required: true, message: '请输入目的地', trigger: 'blur' }, { max: 200, message: '目的地不能超过 200 字', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过 500 字', trigger: 'blur' }]
}

function fillForm(value) {
  Object.assign(form, {
    project: value.project || '',
    driverName: value.driverName,
    phone: value.phone,
    licensePlate: value.licensePlate,
    vehicleType: value.vehicleType,
    quantity: value.quantity || '',
    destination: value.destination,
    remark: value.remark || ''
  })
}

async function load() {
  loading.value = true
  try {
    record.value = await getRecord(route.params.id)
    fillForm(record.value)
  } catch (error) {
    ElMessage.error(error.message)
    if (error.status === 404) router.replace('/admin/records')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!canManage.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || saving.value) return
  saving.value = true
  try {
    record.value = await updateRecord(route.params.id, {
      project: form.project.trim(),
      driverName: form.driverName.trim(),
      phone: form.phone.trim(),
      licensePlate: form.licensePlate.trim().toUpperCase(),
      vehicleType: form.vehicleType.trim(),
      quantity: form.quantity.trim(),
      destination: form.destination.trim(),
      remark: form.remark.trim() || null
    })
    fillForm(record.value)
    ElMessage.success('登记信息已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="admin-main detail-main" v-loading="loading">
    <button class="back-link" type="button" @click="router.push('/admin/records')"><el-icon><ArrowLeft /></el-icon>返回记录列表</button>
    <div v-if="record" class="detail-grid">
      <section class="detail-card">
        <div class="section-heading"><div><p class="eyebrow">{{ canManage ? 'EDITABLE' : 'READ ONLY' }}</p><h1>司机填写信息</h1></div><span class="record-id">#{{ record.id }}</span></div>
        <el-alert v-if="!canManage" class="readonly-notice" title="当前账号仅可查看和导出，不能修改登记信息" type="info" :closable="false" />
        <el-form ref="formRef" :model="form" :rules="rules" :disabled="!canManage" label-position="top" size="large" @submit.prevent="save">
          <el-form-item label="项目" prop="project"><el-input v-model.trim="form.project" maxlength="100" show-word-limit /></el-form-item>
          <div class="field-grid">
            <el-form-item label="姓名" prop="driverName"><el-input v-model.trim="form.driverName" maxlength="50" /></el-form-item>
            <el-form-item label="手机号" prop="phone"><el-input v-model.trim="form.phone" maxlength="11" /></el-form-item>
            <el-form-item label="车牌号" prop="licensePlate"><el-input v-model.trim="form.licensePlate" maxlength="12" /></el-form-item>
            <el-form-item label="车型" prop="vehicleType"><el-input v-model.trim="form.vehicleType" maxlength="50" /></el-form-item>
          </div>
          <el-form-item label="数量" prop="quantity"><el-input v-model.trim="form.quantity" maxlength="100" show-word-limit /></el-form-item>
          <el-form-item label="目的地" prop="destination"><el-input v-model.trim="form.destination" maxlength="200" show-word-limit /></el-form-item>
          <el-form-item label="备注（选填）" prop="remark"><el-input v-model.trim="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
          <el-button v-if="canManage" type="primary" :loading="saving" native-type="submit">保存修改</el-button>
        </el-form>
      </section>

      <section class="detail-card readonly-card">
        <div class="section-heading"><div><p class="eyebrow">LOCATION & TIME</p><h2>定位与时间</h2></div><el-tag :type="locationStatusType(record.locationStatus)">{{ LOCATION_STATUS_LABELS[record.locationStatus] }}</el-tag></div>
        <dl class="detail-list">
          <div><dt>起始位置</dt><dd>{{ record.locationStatus === 'SUCCESS' ? (record.locationAddress || '坐标已获取，文字地址解析失败') : LOCATION_STATUS_LABELS[record.locationStatus] }}</dd></div>
          <div><dt>纬度</dt><dd>{{ record.latitude ?? '—' }}</dd></div>
          <div><dt>经度</dt><dd>{{ record.longitude ?? '—' }}</dd></div>
          <div><dt>定位精度</dt><dd>{{ record.locationAccuracy == null ? '—' : `${record.locationAccuracy} 米` }}</dd></div>
          <div><dt>定位获取时间</dt><dd>{{ formatTime(record.locatedAt) }}</dd></div>
          <div><dt>发车时间</dt><dd>{{ formatTime(record.createdAt) }}</dd></div>
          <div><dt>最后修改时间</dt><dd>{{ formatTime(record.updatedAt) }}</dd></div>
          <div><dt>最后修改人</dt><dd>{{ record.updatedBy || '—' }}</dd></div>
        </dl>
      </section>

      <section class="detail-card photo-detail-card">
        <div class="section-heading"><div><p class="eyebrow">PHOTOS</p><h2>出车照片</h2></div><span class="record-id">{{ record.photoCount }} 张</span></div>
        <div v-if="record.photos?.length" class="admin-photo-grid">
          <el-image
            v-for="(photo, index) in record.photos"
            :key="photo.id"
            :src="photo.url"
            fit="cover"
            :preview-src-list="photoUrls"
            :initial-index="index"
            preview-teleported
          />
        </div>
        <el-empty v-else :image-size="72" description="暂无照片" />
      </section>
    </div>
  </main>
</template>
