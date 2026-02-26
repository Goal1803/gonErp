<template>
  <q-dialog v-model="show" maximized transition-show="fade" transition-hide="fade">
    <q-card style="background: rgba(10,10,10,0.97)" class="flex column">
      <!-- Header -->
      <q-card-section class="flex items-center gap-2 q-pa-md" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon name="image" color="green-5" />
        <span class="text-white text-weight-medium">{{ image?.name }}</span>
        <q-space />
        <q-btn flat round icon="close" color="grey-4" v-close-popup />
      </q-card-section>

      <!-- Image -->
      <q-card-section class="flex flex-center col-grow q-pa-md">
        <div class="image-view-container">
          <img
            v-if="image"
            :src="image.url"
            :alt="image.name"
            class="large-image"
            @error="onError"
          />
        </div>
      </q-card-section>

      <!-- Image Info -->
      <q-card-section class="q-pa-lg" style="border-top: 1px solid rgba(46,125,50,0.2); max-width: 700px; margin: 0 auto; width: 100%">
        <div class="row q-col-gutter-md">
          <div class="col-12">
            <div class="text-caption text-grey-5 q-mb-xs">Image Name</div>
            <div class="text-white text-weight-medium">{{ image?.name }}</div>
          </div>
          <div class="col-12">
            <div class="text-caption text-grey-5 q-mb-xs">URL</div>
            <div class="text-green-4 text-caption" style="word-break: break-all">{{ image?.url }}</div>
          </div>
          <div class="col-6">
            <div class="text-caption text-grey-5 q-mb-xs">Created By</div>
            <div class="text-white">{{ image?.createdBy || '-' }}</div>
          </div>
          <div class="col-6">
            <div class="text-caption text-grey-5 q-mb-xs">Created At</div>
            <div class="text-white">{{ image?.createdAt ? formatDateTime(image.createdAt) : '-' }}</div>
          </div>
          <div class="col-6">
            <div class="text-caption text-grey-5 q-mb-xs">Last Updated By</div>
            <div class="text-white">{{ image?.lastUpdatedBy || '-' }}</div>
          </div>
          <div class="col-6">
            <div class="text-caption text-grey-5 q-mb-xs">Last Updated At</div>
            <div class="text-white">{{ image?.lastUpdatedAt ? formatDateTime(image.lastUpdatedAt) : '-' }}</div>
          </div>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  image: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue'])

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const formatDateTime = (dt) => {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('en-GB')
}

const onError = (e) => {
  e.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNjAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iIzFhMWExYSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmaWxsPSIjNDQ0IiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+SW1hZ2UgTm90IEZvdW5kPC90ZXh0Pjwvc3ZnPg=='
}
</script>

<style scoped>
.image-view-container {
  max-width: 900px;
  max-height: calc(100vh - 300px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.large-image {
  max-width: 100%;
  max-height: calc(100vh - 300px);
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 0 40px rgba(0,0,0,0.5);
}
</style>
