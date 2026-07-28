<script setup>
import { computed } from 'vue'
import { formatRecordTime } from '@/utils/time'

const props = defineProps({
  value: { type: String, default: '' },
  now: { type: [Number, String, Date], required: true }
})

const display = computed(() => formatRecordTime(props.value, props.now))
</script>

<template>
  <time :datetime="value" :title="display.full" :class="['record-time', { 'record-time--old': display.isOld }]">
    <span v-if="display.relative" class="record-time-relative">{{ display.relative }}</span>
    <span class="record-time-exact">{{ display.exact }}</span>
  </time>
</template>
