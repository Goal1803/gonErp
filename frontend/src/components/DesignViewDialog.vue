<template>
  <q-dialog v-model="show" maximized transition-show="fade" transition-hide="fade">
    <q-card dark class="design-view-dialog">
      <!-- Header -->
      <q-card-section class="row items-center no-wrap q-py-sm" style="border-bottom: 1px solid rgba(255,255,255,0.07)">
        <q-icon name="palette" color="teal-5" size="sm" class="q-mr-sm" />
        <div class="text-h6 text-white text-weight-medium ellipsis" style="max-width: 500px">
          {{ design?.cardName || design?.name || 'Untitled' }}
        </div>
        <q-chip v-if="design?.stage" dense size="sm" color="grey-8" text-color="grey-3" class="q-ml-sm">
          {{ design.stage }}
        </q-chip>
        <q-chip v-if="!design?.cardId" dense size="sm" color="teal-9" text-color="teal-2" class="q-ml-sm">
          Standalone
        </q-chip>
        <q-chip v-if="design?.cardId && !design?.boardActive" dense size="sm" color="orange-9" text-color="orange-3" class="q-ml-xs">
          Archived
        </q-chip>
        <q-btn v-if="design?.cardId" flat dense icon="open_in_new" color="teal-5" label="View Card" class="q-ml-sm" @click="openCard" />
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
              <div class="main-mockup-actions">
                <q-btn flat round dense icon="download" color="white" size="sm"
                  @click="downloadFile(mainMockup.url, 'mockup-' + mainMockup.id + '.png')">
                  <q-tooltip>Download</q-tooltip>
                </q-btn>
              </div>
            </div>
            <div v-else class="main-mockup-placeholder q-mb-md">
              <q-icon name="image" size="64px" color="grey-7" />
              <div class="text-grey-6 q-mt-sm">No mockups uploaded</div>
            </div>

            <!-- Mockup Thumbnails -->
            <div v-if="detail?.mockups?.length" class="mockup-thumbnails q-mb-md">
              <div v-for="m in detail.mockups" :key="m.id"
                class="mockup-thumb-item" :class="{ active: selectedMockup === m.id, 'mockup-main-border': m.mainMockup }"
                @click="selectedMockup = m.id">
                <img :src="m.url" />
                <div class="mockup-thumb-actions">
                  <q-btn flat round dense icon="download" color="white" size="8px"
                    @click.stop="downloadFile(m.url, 'mockup-' + m.id + '.png')" />
                  <q-btn v-if="!m.mainMockup" flat round dense icon="star_outline" color="amber" size="8px"
                    @click.stop="setMainMockup(m)" />
                  <q-btn flat round dense icon="close" color="red-4" size="8px"
                    @click.stop="deleteMockup(m)" />
                </div>
                <q-icon v-if="m.mainMockup" name="star" color="amber" size="12px" class="mockup-star-badge" />
              </div>
            </div>

            <!-- Upload Mockup -->
            <q-file v-model="mockupFile" outlined dark color="teal-5" dense label="Upload mockup"
              accept="image/*" class="q-mb-md" @update:model-value="uploadMockup">
              <template #prepend><q-icon name="add_photo_alternate" color="grey-5" /></template>
            </q-file>

            <!-- Design Files -->
            <div class="design-files-row row q-gutter-md">
              <!-- PNG Files -->
              <div class="col">
                <div class="info-label"><q-icon name="image" size="xs" /> PNG Files</div>
                <div v-if="detail?.pngFiles?.length" class="q-mb-sm">
                  <div v-for="f in detail.pngFiles" :key="f.id" class="row items-center q-gutter-sm q-mb-xs">
                    <span class="text-teal-4 ellipsis" style="font-size:0.82rem; max-width: 160px">{{ f.name || 'PNG file' }}</span>
                    <q-btn flat round dense icon="download" color="white" size="xs"
                      @click="downloadFile(f.url, f.name || 'design.png')" />
                    <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deletePngFile(f)" />
                  </div>
                </div>
                <q-file v-model="pngFile" outlined dark color="teal-5" dense label="Upload PNG"
                  accept=".png" @update:model-value="uploadPng">
                  <template #prepend><q-icon name="upload" color="grey-5" /></template>
                </q-file>
              </div>
              <!-- PSD Files -->
              <div class="col">
                <div class="info-label"><q-icon name="brush" size="xs" /> PSD Files</div>
                <div v-if="detail?.psdFiles?.length" class="q-mb-sm">
                  <div v-for="f in detail.psdFiles" :key="f.id" class="row items-center q-gutter-sm q-mb-xs">
                    <span class="text-purple-4 ellipsis" style="font-size:0.82rem; max-width: 160px">{{ f.name || 'PSD file' }}</span>
                    <q-btn flat round dense icon="download" color="white" size="xs"
                      @click="downloadFile(f.url, f.name || 'design.psd')" />
                    <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deletePsdFile(f)" />
                  </div>
                </div>
                <q-file v-model="psdFile" outlined dark color="teal-5" dense label="Upload PSD"
                  accept=".psd,.psb" @update:model-value="uploadPsd">
                  <template #prepend><q-icon name="upload" color="grey-5" /></template>
                </q-file>
              </div>
            </div>
          </div>

          <!-- RIGHT: Info -->
          <div class="col-12 col-md-5">
            <!-- Board (card-linked only) -->
            <div v-if="design?.cardId" class="info-block">
              <div class="info-label">Board</div>
              <div class="info-value">{{ design?.boardName }}</div>
            </div>

            <!-- Name (standalone editable) -->
            <div v-if="isStandalone" class="info-block">
              <div class="info-label"><q-icon name="label" size="xs" /> Name</div>
              <q-input v-model="editName" outlined dark dense color="teal-5"
                @blur="saveDetail" @keyup.enter="saveDetail" />
            </div>

            <!-- Design Status -->
            <div class="info-block">
              <div class="info-label"><q-icon name="flag" size="xs" /> Design Status</div>
              <q-chip dense size="sm"
                :color="designStatusColor"
                :text-color="designStatusTextColor"
                :label="detail?.designStatus || design?.designStatus || '-'" />
            </div>

            <!-- Idea Creator -->
            <div class="info-block">
              <div class="info-label"><q-icon name="lightbulb" size="xs" /> Idea Creator</div>
              <div v-if="detail?.ideaCreator" class="row items-center gap-2">
                <q-avatar size="28px" color="teal-9" text-color="white">
                  {{ detail.ideaCreator.firstName?.charAt(0) || detail.ideaCreator.userName?.charAt(0) || '?' }}
                </q-avatar>
                <span class="text-grey-3" style="font-size:0.85rem">
                  {{ [detail.ideaCreator.firstName, detail.ideaCreator.lastName].filter(Boolean).join(' ') || detail.ideaCreator.userName }}
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

            <!-- Product Types (standalone editable) -->
            <div class="info-block">
              <div class="info-label"><q-icon name="inventory_2" size="xs" /> Product Types</div>
              <q-select v-if="isStandalone" v-model="editProductTypeIds" :options="productTypeOptions"
                option-value="id" option-label="name" emit-value map-options multiple outlined dark dense
                color="teal-5" use-chips @update:model-value="saveDetail" />
              <template v-else>
                <div v-if="detail?.productTypes?.length" class="row items-center q-gutter-xs" style="flex-wrap:wrap">
                  <q-chip v-for="pt in detail.productTypes" :key="pt.id" dense size="sm"
                    color="deep-purple-9" text-color="purple-2" :label="pt.name" />
                </div>
                <div v-else class="text-grey-6" style="font-size:0.85rem">-</div>
              </template>
            </div>

            <!-- Niches (standalone editable) -->
            <div class="info-block">
              <div class="info-label"><q-icon name="category" size="xs" /> Niches</div>
              <q-select v-if="isStandalone" v-model="editNicheIds" :options="nicheOptions"
                option-value="id" option-label="name" emit-value map-options multiple outlined dark dense
                color="teal-5" use-chips @update:model-value="saveDetail" />
              <template v-else>
                <div v-if="detail?.niches?.length" class="row items-center q-gutter-xs" style="flex-wrap:wrap">
                  <q-chip v-for="n in detail.niches" :key="n.id" dense size="sm"
                    color="teal-9" text-color="teal-2" :label="n.name" />
                </div>
                <div v-else class="text-grey-6" style="font-size:0.85rem">-</div>
              </template>
            </div>

            <!-- Occasion (standalone editable) -->
            <div class="info-block">
              <div class="info-label"><q-icon name="celebration" size="xs" /> Occasion</div>
              <q-select v-if="isStandalone" v-model="editOccasionId" :options="occasionOptions"
                option-value="id" option-label="name" emit-value map-options outlined dark dense
                color="teal-5" clearable @update:model-value="saveDetail" />
              <template v-else>
                <div v-if="detail?.occasion" class="text-grey-3" style="font-size:0.85rem">{{ detail.occasion.name }}</div>
                <div v-else class="text-grey-6" style="font-size:0.85rem">-</div>
              </template>
            </div>

            <!-- Custom (standalone editable) -->
            <div class="info-block">
              <div class="info-label"><q-icon name="tune" size="xs" /> Custom</div>
              <q-toggle v-if="isStandalone" v-model="editCustom" dark color="teal-5" @update:model-value="saveDetail" />
              <q-chip v-else dense size="sm"
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
import { useQuasar } from 'quasar'
import { designApi, designsApi, lookupApi } from 'src/api/tasks'

const $q = useQuasar()

const props = defineProps({
  modelValue: Boolean,
  design: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'open-card', 'updated'])

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const detail = ref(null)
const selectedMockup = ref(null)
const mockupFile = ref(null)
const pngFile = ref(null)
const psdFile = ref(null)

// Editable fields for standalone designs
const editName = ref('')
const editProductTypeIds = ref([])
const editNicheIds = ref([])
const editOccasionId = ref(null)
const editCustom = ref(false)

// Lookup options for standalone editing
const productTypeOptions = ref([])
const nicheOptions = ref([])
const occasionOptions = ref([])

const isStandalone = computed(() => !props.design?.cardId)

// Computed API adapter: picks card-based or design-ID-based API
const api = computed(() => {
  if (isStandalone.value) {
    const id = props.design?.id
    return {
      getDetail: () => designsApi.getDetail(id),
      uploadMockup: (fd) => designsApi.uploadMockup(id, fd),
      deleteMockup: (mockupId) => designsApi.deleteMockup(id, mockupId),
      setMainMockup: (mockupId) => designsApi.setMainMockup(id, mockupId),
      uploadPng: (fd) => designsApi.uploadPng(id, fd),
      deletePngFile: (fileId) => designsApi.deletePngFile(id, fileId),
      uploadPsd: (fd) => designsApi.uploadPsd(id, fd),
      deletePsdFile: (fileId) => designsApi.deletePsdFile(id, fileId),
    }
  } else {
    const id = props.design?.cardId
    return {
      getDetail: () => designApi.getDetail(id),
      uploadMockup: (fd) => designApi.uploadMockup(id, fd),
      deleteMockup: (mockupId) => designApi.deleteMockup(id, mockupId),
      setMainMockup: (mockupId) => designApi.setMainMockup(id, mockupId),
      uploadPng: (fd) => designApi.uploadPng(id, fd),
      deletePngFile: (fileId) => designApi.deletePngFile(id, fileId),
      uploadPsd: (fd) => designApi.uploadPsd(id, fd),
      deletePsdFile: (fileId) => designApi.deletePsdFile(id, fileId),
    }
  }
})

const mainMockup = computed(() => {
  if (!detail.value?.mockups?.length) return null
  if (selectedMockup.value) {
    const found = detail.value.mockups.find(m => m.id === selectedMockup.value)
    if (found) return found
  }
  return detail.value.mockups.find(m => m.mainMockup) || detail.value.mockups[0]
})

const formatDate = (d) => d ? new Date(d).toLocaleString() : '-'

const downloadFile = async (url, filename) => {
  try {
    const res = await fetch(url)
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = filename || url.split('/').pop()
    a.click()
    URL.revokeObjectURL(a.href)
  } catch {
    $q.notify({ type: 'negative', message: 'Download failed' })
  }
}

const uploadMockup = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    await api.value.uploadMockup(fd)
    await loadDetail()
    mockupFile.value = null
    emit('updated')
  } catch {
    $q.notify({ type: 'negative', message: 'Mockup upload failed' })
  }
}

const deleteMockup = async (m) => {
  try {
    await api.value.deleteMockup(m.id)
    await loadDetail()
    emit('updated')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to delete mockup' })
  }
}

const setMainMockup = async (m) => {
  try {
    await api.value.setMainMockup(m.id)
    await loadDetail()
    emit('updated')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to set main mockup' })
  }
}

const uploadPng = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    await api.value.uploadPng(fd)
    await loadDetail()
    pngFile.value = null
  } catch {
    $q.notify({ type: 'negative', message: 'PNG upload failed' })
  }
}

const deletePngFile = async (f) => {
  try {
    await api.value.deletePngFile(f.id)
    await loadDetail()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove PNG' })
  }
}

const uploadPsd = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    await api.value.uploadPsd(fd)
    await loadDetail()
    psdFile.value = null
  } catch {
    $q.notify({ type: 'negative', message: 'PSD upload failed' })
  }
}

const deletePsdFile = async (f) => {
  try {
    await api.value.deletePsdFile(f.id)
    await loadDetail()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove PSD' })
  }
}

const saveDetail = async () => {
  if (!isStandalone.value || !props.design?.id) return
  try {
    const res = await designsApi.updateDetail(props.design.id, {
      name: editName.value,
      productTypeIds: editProductTypeIds.value,
      nicheIds: editNicheIds.value,
      occasionId: editOccasionId.value || 0,
      custom: editCustom.value
    })
    detail.value = res.data.data
    emit('updated')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to update design' })
  }
}

const openCard = () => {
  emit('open-card', { cardId: props.design?.cardId, boardId: props.design?.boardId })
  show.value = false
}

const designStatusColor = computed(() => {
  const s = detail.value?.designStatus || props.design?.designStatus
  if (s === 'APPROVED') return 'green-9'
  if (s === 'DELETED') return 'red-9'
  return 'orange-9'
})
const designStatusTextColor = computed(() => {
  const s = detail.value?.designStatus || props.design?.designStatus
  if (s === 'APPROVED') return 'green-2'
  if (s === 'DELETED') return 'red-2'
  return 'orange-2'
})

const loadLookups = async () => {
  try {
    const [pt, ni, oc] = await Promise.all([
      lookupApi.getProductTypes(),
      lookupApi.getNiches(),
      lookupApi.getOccasions()
    ])
    productTypeOptions.value = pt.data.data || []
    nicheOptions.value = ni.data.data || []
    occasionOptions.value = oc.data.data || []
  } catch { /* silent */ }
}

const syncEditFields = () => {
  if (!detail.value) return
  editName.value = detail.value.name || ''
  editProductTypeIds.value = detail.value.productTypes?.map(p => p.id) || []
  editNicheIds.value = detail.value.niches?.map(n => n.id) || []
  editOccasionId.value = detail.value.occasion?.id || null
  editCustom.value = detail.value.custom || false
}

const loadDetail = async () => {
  if (!props.design?.cardId && !props.design?.id) return
  loading.value = true
  try {
    const res = await api.value.getDetail()
    detail.value = res.data.data
    selectedMockup.value = null
    if (isStandalone.value) syncEditFields()
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

watch(() => props.design, (d) => {
  if (d) {
    loadDetail()
    if (!d.cardId) loadLookups()
  } else {
    detail.value = null
    selectedMockup.value = null
  }
}, { immediate: true })
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
  position: relative;
}
.main-mockup-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.main-mockup-actions .q-btn {
  background: rgba(0, 0, 0, 0.6);
}
.main-mockup-container:hover .main-mockup-actions {
  opacity: 1;
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
.mockup-thumb-item.mockup-main-border {
  border-color: #ffc107;
}
.mockup-thumb-actions {
  position: absolute;
  top: 1px;
  right: 1px;
  display: flex;
  gap: 1px;
  opacity: 0;
  transition: opacity 0.15s;
}
.mockup-thumb-actions .q-btn {
  background: rgba(0, 0, 0, 0.6);
}
.mockup-thumb-item:hover .mockup-thumb-actions {
  opacity: 1;
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
