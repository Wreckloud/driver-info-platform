<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Search, View } from '@element-plus/icons-vue'
import { deleteRecord, exportRecords, getRecords } from '@/api/admin'
import RecordTime from '@/components/RecordTime.vue'
import { LOCATION_STATUS_LABELS, locationStatusType } from '@/constants/location'

const router = useRouter()
const loading = ref(false)
const exporting = ref(false)
const records = ref([])
const total = ref(0)
const dateRange = ref([])
const query = reactive({ page: 1, pageSize: 20, keyword: '' })
const currentTime = ref(Date.now())
let serverTimeAtSync = null
let monotonicTimeAtSync = null
let relativeTimeTimer = null
let recordsRefreshTimer = null
let requestInFlight = false

function apiParams(withPage = true) {
  const params = {
    keyword: query.keyword.trim() || undefined,
    startDate: dateRange.value?.[0] || undefined,
    endDate: dateRange.value?.[1] || undefined
  }
  if (withPage) {
    params.page = query.page
    params.pageSize = query.pageSize
  }
  return params
}

function syncServerTime(value) {
  const parsed = Date.parse(value)
  if (!Number.isFinite(parsed)) return
  serverTimeAtSync = parsed
  monotonicTimeAtSync = performance.now()
  updateCurrentTime()
}

function updateCurrentTime() {
  currentTime.value = serverTimeAtSync === null
    ? Date.now()
    : serverTimeAtSync + performance.now() - monotonicTimeAtSync
}

async function loadRecords({ silent = false } = {}) {
  if (requestInFlight) return
  requestInFlight = true
  if (!silent) loading.value = true
  try {
    const result = await getRecords(apiParams())
    records.value = result.items
    total.value = result.total
    syncServerTime(result.serverTime)
  } catch (error) {
    if (!silent) ElMessage.error(error.message)
  } finally {
    requestInFlight = false
    if (!silent) loading.value = false
  }
}

function search() {
  query.page = 1
  loadRecords()
}

function reset() {
  query.keyword = ''
  dateRange.value = []
  query.page = 1
  loadRecords()
}

async function remove(record) {
  await ElMessageBox.confirm(`确定删除 ${record.driverName} / ${record.licensePlate} 的登记记录吗？数据将被软删除。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消'
  })
  try {
    await deleteRecord(record.id)
    ElMessage.success('记录已删除')
    if (records.value.length === 1 && query.page > 1) query.page--
    await loadRecords()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function exportExcel() {
  exporting.value = true
  try {
    const { blob, filename } = await exportRecords(apiParams(false))
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('Excel 已导出')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exporting.value = false
  }
}

function refreshWhenVisible() {
  if (document.visibilityState === 'visible') loadRecords({ silent: true })
}

onMounted(async () => {
  await loadRecords()
  relativeTimeTimer = window.setInterval(updateCurrentTime, 30000)
  recordsRefreshTimer = window.setInterval(refreshWhenVisible, 60000)
  document.addEventListener('visibilitychange', refreshWhenVisible)
})

onUnmounted(() => {
  window.clearInterval(relativeTimeTimer)
  window.clearInterval(recordsRefreshTimer)
  document.removeEventListener('visibilitychange', refreshWhenVisible)
})
</script>

<template>
  <main class="admin-main">
    <div class="page-heading">
      <div><p class="eyebrow">RECORDS</p><h1>出车登记记录</h1><p>共 {{ total }} 条有效记录</p></div>
      <el-button type="primary" :icon="Download" :loading="exporting" @click="exportExcel">导出当前结果</el-button>
    </div>

    <section class="filter-card">
      <el-input v-model="query.keyword" clearable placeholder="搜索项目、姓名、车牌、目的地或备注" :prefix-icon="Search" @keyup.enter="search" />
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </section>

    <section class="records-card" v-loading="loading">
      <el-table class="desktop-records" :data="records" empty-text="暂无登记记录">
        <el-table-column prop="project" label="项目" min-width="130" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.project || '—' }}</template>
        </el-table-column>
        <el-table-column prop="driverName" label="姓名" min-width="90" />
        <el-table-column prop="phone" label="手机号" min-width="125" />
        <el-table-column prop="licensePlate" label="车牌号" min-width="110" />
        <el-table-column prop="vehicleType" label="车型" min-width="110" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" min-width="130" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.quantity || '—' }}</template>
        </el-table-column>
        <el-table-column prop="destination" label="目的地" min-width="160" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column prop="locationAddress" label="起始位置" min-width="210" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.locationStatus === 'SUCCESS'">{{ scope.row.locationAddress || '坐标已获取，地址解析失败' }}</span>
            <span v-else class="muted">{{ LOCATION_STATUS_LABELS[scope.row.locationStatus] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="定位" width="100">
          <template #default="scope"><el-tag size="small" :type="locationStatusType(scope.row.locationStatus)">{{ LOCATION_STATUS_LABELS[scope.row.locationStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="发车时间" min-width="185">
          <template #default="scope">
            <RecordTime :value="scope.row.createdAt" :now="currentTime" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :icon="View" @click="router.push(`/admin/records/${scope.row.id}`)">详情</el-button>
            <el-button link type="danger" :icon="Delete" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mobile-records">
        <article v-for="record in records" :key="record.id" class="record-item">
          <div class="record-item-head">
            <div><strong>{{ record.driverName }}</strong><span>{{ record.licensePlate }}</span></div>
            <el-tag size="small" :type="locationStatusType(record.locationStatus)">{{ LOCATION_STATUS_LABELS[record.locationStatus] }}</el-tag>
          </div>
          <dl>
            <div><dt>项目</dt><dd>{{ record.project || '—' }}</dd></div>
            <div><dt>手机号</dt><dd>{{ record.phone }}</dd></div>
            <div><dt>车型</dt><dd>{{ record.vehicleType }}</dd></div>
            <div><dt>数量</dt><dd>{{ record.quantity || '—' }}</dd></div>
            <div><dt>目的地</dt><dd>{{ record.destination }}</dd></div>
            <div><dt>备注</dt><dd>{{ record.remark || '—' }}</dd></div>
            <div><dt>起始位置</dt><dd>{{ record.locationStatus === 'SUCCESS' ? (record.locationAddress || '坐标已获取，地址解析失败') : LOCATION_STATUS_LABELS[record.locationStatus] }}</dd></div>
            <div>
              <dt>发车时间</dt>
              <dd><RecordTime :value="record.createdAt" :now="currentTime" /></dd>
            </div>
          </dl>
          <div class="record-actions">
            <el-button type="primary" plain :icon="View" @click="router.push(`/admin/records/${record.id}`)">详情</el-button>
            <el-button type="danger" plain :icon="Delete" @click="remove(record)">删除</el-button>
          </div>
        </article>
        <el-empty v-if="!records.length && !loading" description="暂无登记记录" />
      </div>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        class="records-pagination"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="() => loadRecords()"
        @size-change="search"
      />
    </section>
  </main>
</template>
