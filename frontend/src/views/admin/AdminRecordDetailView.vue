<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getRecord, updateRecord } from '@/api/admin'
import { LOCATION_STATUS_LABELS, locationStatusType } from '@/constants/location'
import { formatTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const record = ref(null)
const form = reactive({ driverName: '', phone: '', licensePlate: '', vehicleType: '', destination: '' })
const rules = {
  driverName: [{ required: true, message: '请输入姓名', trigger: 'blur' }, { max: 50, message: '姓名不能超过 50 字', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  licensePlate: [{ required: true, message: '请输入车牌号', trigger: 'blur' }, { pattern: /^[\u4e00-\u9fa5A-Za-z0-9-]{5,12}$/, message: '车牌号格式不正确', trigger: 'blur' }],
  vehicleType: [{ required: true, message: '请输入车型', trigger: 'blur' }],
  destination: [{ required: true, message: '请输入目的地', trigger: 'blur' }]
}

function fillForm(value) {
  Object.assign(form, {
    driverName: value.driverName,
    phone: value.phone,
    licensePlate: value.licensePlate,
    vehicleType: value.vehicleType,
    destination: value.destination
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
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || saving.value) return
  saving.value = true
  try {
    record.value = await updateRecord(route.params.id, {
      driverName: form.driverName.trim(),
      phone: form.phone.trim(),
      licensePlate: form.licensePlate.trim().toUpperCase(),
      vehicleType: form.vehicleType.trim(),
      destination: form.destination.trim()
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
        <div class="section-heading"><div><p class="eyebrow">EDITABLE</p><h1>司机填写信息</h1></div><span class="record-id">#{{ record.id }}</span></div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="save">
          <div class="field-grid">
            <el-form-item label="姓名" prop="driverName"><el-input v-model.trim="form.driverName" maxlength="50" /></el-form-item>
            <el-form-item label="手机号" prop="phone"><el-input v-model.trim="form.phone" maxlength="11" /></el-form-item>
            <el-form-item label="车牌号" prop="licensePlate"><el-input v-model.trim="form.licensePlate" maxlength="12" /></el-form-item>
            <el-form-item label="车型" prop="vehicleType"><el-input v-model.trim="form.vehicleType" maxlength="50" /></el-form-item>
          </div>
          <el-form-item label="目的地" prop="destination"><el-input v-model.trim="form.destination" maxlength="200" show-word-limit /></el-form-item>
          <el-button type="primary" :loading="saving" native-type="submit">保存修改</el-button>
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
          <div><dt>提交时间</dt><dd>{{ formatTime(record.createdAt) }}</dd></div>
          <div><dt>最后修改时间</dt><dd>{{ formatTime(record.updatedAt) }}</dd></div>
          <div><dt>最后修改人</dt><dd>{{ record.updatedBy || '—' }}</dd></div>
        </dl>
      </section>
    </div>
  </main>
</template>
