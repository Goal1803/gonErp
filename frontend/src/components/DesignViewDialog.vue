<template>
  <q-dialog v-model="show" maximized transition-show="fade" transition-hide="fade">
    <q-card dark class="design-view-dialog">
      <!-- Header -->
      <q-card-section class="row items-center no-wrap q-py-sm" style="border-bottom: 1px solid rgba(255,255,255,0.07)">
        <q-icon name="palette" color="teal-5" size="sm" class="q-mr-sm" />
        <div class="text-h6 text-white text-weight-medium ellipsis" style="max-width: 500px">
          {{ design?.cardName }}
        </div>
        <q-chip v-if="design?.stage" dense size="sm" color="grey-8" text-color="grey-3" class="q-ml-sm">
          {{ design.stage }}
        </q-chip>
        <q-chip v-if="!design?.boardActive" dense size="sm" color="orange-9" text-color="orange-3" class="q-ml-xs">
          Archived
        </q-chip>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section v-if="!detail && loading" class="flex flex-center q-py-xl">
        <q-spinner color="teal-5" size="48px" />
      </q-card-section>

      <q-scroll-area v-else class="design-view-scroll">
        <div class="row q-pa-lg q-col-gutter-lg">

          <!-- LEFT: Mockups -->
          <div class="col-12 col-md-7">
            <!-- Main Mockup -->
            <div v-if="mainMockup" class="main-mockup-container q-mb-md">
              <img :src="mainMockup.url" class="main-mockup-img" />
            </div>
            <div v-else class="main-mockup-placeholder q-mb-md">
              <q-icon name="image" size="64px" color="grey-7" />
              <div class="text-grey-6 q-mt-sm">No mockups uploaded</div>
            </div>

            <!-- Mockup Thumbnails -->
            <div v-if="detail?.mockups?.length > 1" class="mockup-thumbnails q-mb-md">
              <div v-for="m in detail.mockups" :key="m.id"
                class="mockup-thumb-item" :class="{ active: selectedMockup === m.id }"
                @click="selectedMockup = m.id">
                <img :src="m.url" />
                <q-icon v-if="m.mainMockup" name="star" color="amber" size="12px" class="mockup-star-badge" />
              </div>
            </div>

            <!-- Design Files -->
            <div class="design-files-row row q-gutter-md">
              <div v-if="detail?.pngFileUrl" class="design-file-chip">
                <a :href="detail.pngFileUrl" target="_blank" class="row items-center gap-2 text-teal-4">
                  <q-icon name="image" size="xs" />
                  <span>{{ detail.pngFileName || 'PNG file' }}</span>
                  <q-icon name="download" size="xs" />
                </a>
              </div>
              <div v-if="detail?.psdFileUrl" class="design-file-chip">
                <a :href="detail.psdFileUrl" target="_blank" class="row items-center gap-2 text-purple-4">
                  <q-icon name="brush" size="xs" />
                  <span>{{ detail.psdFileName || 'PSD file' }}</span>
                  <q-icon name="download" size="xs" />
                </a>
              </div>
            </div>
          </div>

          <!-- RIGHT: Info -->
          <div class="col-12 col-md-5">
            <!-- Board -->
            <div class="info-block">
              <div class="info-label">Board</div>
              <div class="info-value">{{ design?.boardName }}</div>
            </div>

            <!-- Stage & Status -->
            <div class="row q-gutter-md">
              <div class="info-block col">
                <div class="info-label">Stage</div>
                <q-chip dense color="grey-8" text-color="grey-3" :label="design?.stage || '-'" />
              </div>
              <div class="info-block col">
                <div class="info-label">Status</div>
                <q-chip dense color="blue-grey-8" text-color="blue-grey-3" :label="design?.status || '-'" />
              </div>
            </div>

            <!-- Seller -->
            <div class="info-block">
              <div class="info-label"><q-icon name="store" size="xs" /> Seller</div>
              <div v-if="detail?.seller" class="row items-center gap-2">
                <q-avatar size="28px" color="teal-9" text-color="white">
                  {{ detail.seller.firstName?.charAt(0) || detail.seller.userName?.charAt(0) || '?' }}
                </q-avatar>
                <span class="text-grey-3" style="font-size:0.85rem">
                  {{ [detail.seller.firstName, detail.seller.lastName].filter(Boolean).join(' ') || detail.seller.userName }}
                </span>
              </div>
              <div v-else class="text-grey-6" style="font-size:0.85rem">Not assigned</div>
            </div>

            <!-- Designers -->
            <div class="info-block">
              <div class="info-label"><q-icon name="design_services" size="xs" /> Designers</div>
              <div v-if="detail?.designers?.length" class="row items-center q-gutter-sm" style="flex-wrap:wrap">
                <q-chip v-for="d in detail.designers" :key="d.id" dense size="sm" color="grey-8" text-color="grey-3">
                  {{ [d.firstName, d.lastName].filter(Boolean).join(' ') || d.userName }}
                </q-chip>
              </div>
              <div v-else class="text-grey-6" style="font-size:0.85rem">None</div>
            </div>

            <!-- Product Types -->
            <div class="info-block">
              <div class="info-label"><q-icon name="inventory_2" size="xs" /> Product Types</div>
              <div v-if="detail?.productTypes?.length" class="row items-center q-gutter-xs" style="flex-wrap:wrap">
                <q-chip v-for="pt in detail.productTypes" :key="pt.id" dense size="sm"
                  color="deep-purple-9" text-color="purple-2" :label="pt.name" />
              </div>
              <div v-else class="text-grey-6" style="font-size:0.85rem">-</div>
            </div>

            <!-- Niches -->
            <div class="info-block">
              <div class="info-label"><q-icon name="category" size="xs" /> Niches</div>
              <div v-if="detail?.niches?.length" class="row items-center q-gutter-xs" style="flex-wrap:wrap">
                <q-chip v-for="n in detail.niches" :key="n.id" dense size="sm"
                  color="teal-9" text-color="teal-2" :label="n.name" />
              </div>
              <div v-else class="text-grey-6" style="font-size:0.85rem">-</div>
            </div>

            <!-- Occasion -->
            <div class="info-block">
              <div class="info-label"><q-icon name="celebration" size="xs" /> Occasion</div>
              <div v-if="detail?.occasion" class="text-grey-3" style="font-size:0.85rem">{{ detail.occasion.name }}</div>
              <div v-else class="text-grey-6" style="font-size:0.85rem">-</div>
            </div>

            <!-- Custom -->
            <div class="info-block">
              <div class="info-label"><q-icon name="tune" size="xs" /> Custom</div>
              <q-chip dense size="sm"
                :color="detail?.custom ? 'teal-9' : 'grey-9'"
                :text-color="detail?.custom ? 'teal-2' : 'grey-5'"
                :label="detail?.custom ? 'Yes' : 'No'" />
            </div>

            <!-- Approval Date -->
            <div v-if="detail?.approvalDate" class="info-block">
              <div class="info-label"><q-icon name="check_circle" size="xs" color="green-5" /> Approved</div>
              <div class="text-green-4" style="font-size:0.85rem">{{ formatDate(detail.approvalDate) }}</div>
            </div>

            <!-- Created -->
            <div class="info-block">
              <div class="info-label">Created</div>
              <div class="text-grey-5" style="font-size:0.82rem">{{ formatDate(design?.createdAt) }}</div>
            </div>
          </div>
        </div>
      </q-scroll-area>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { designApi } from 'src/api/tasks'

const props = defineProps({
  modelValue: Boolean,
  design: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue'])

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const detail = ref(null)
const selectedMockup = ref(null)

const mainMockup = computed(() => {
  if (!detail.value?.mockups?.length) return null
  if (selectedMockup.value) {
    const found = detail.value.mockups.find(m => m.id === selectedMockup.value)
    if (found) return found
  }
  return detail.value.mockups.find(m => m.mainMockup) || detail.value.mockups[0]
})

const formatDate = (d) => d ? new Date(d).toLocaleString() : '-'

const loadDetail = async () => {
  if (!props.design?.cardId) return
  loading.value = true
  try {
    const res = await designApi.getDetail(props.design.cardId)
    detail.value = res.data.data
    selectedMockup.value = null
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

watch(() => props.design, (d) => {
  if (d) {
    loadDetail()
  } else {
    detail.value = null
    selectedMockup.value = null
  }
})
</script>

<style scoped>
.design-view-dialog {
  background: #0d0d0d;
}
.design-view-scroll {
  height: calc(100vh - 56px);
}
.main-mockup-container {
  width: 100%;
  border-radius: 10px;
  overflow: hidden;
  background: #111;
}
.main-mockup-img {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
  display: block;
}
.main-mockup-placeholder {
  width: 100%;
  aspect-ratio: 1;
  max-height: 400px;
  background: #111;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.mockup-thumbnails {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.mockup-thumb-item {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  position: relative;
  transition: border-color 0.15s;
}
.mockup-thumb-item.active {
  border-color: #26a69a;
}
.mockup-thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.mockup-star-badge {
  position: absolute;
  bottom: 1px;
  left: 1px;
}
.design-files-row {
  margin-top: 8px;
}
.design-file-chip {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  padding: 6px 12px;
}
.design-file-chip a {
  text-decoration: none;
  font-size: 0.82rem;
}
.info-block {
  margin-bottom: 16px;
}
.info-label {
  font-size: 0.72rem;
  color: #9e9e9e;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.info-value {
  color: #ffffffde;
  font-size: 0.9rem;
}
</style>
