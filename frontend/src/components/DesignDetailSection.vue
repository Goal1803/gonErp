<template>
  <div v-if="designDetail" class="design-detail-section q-mt-md">
    <div class="text-caption text-grey-5 q-mb-sm row items-center gap-2">
      <q-icon name="palette" /> Design Details
    </div>
    <div class="design-detail-card q-pa-md">

      <!-- Design Files Row -->
      <div class="row q-gutter-md q-mb-md">
        <!-- PNG Files -->
        <div class="col">
          <div class="sidebar-label"><q-icon name="image" size="xs" /> PNG Files</div>
          <div v-if="designDetail.pngFiles?.length" class="q-mb-sm">
            <div v-for="(f, i) in designDetail.pngFiles" :key="f.id" class="row items-center q-gutter-sm q-mb-xs">
              <span class="text-teal-4 ellipsis" style="font-size:0.82rem; max-width: 160px; cursor:pointer"
                @click="emit('view-images', { images: designDetail.pngFiles.map(x => x.url), index: i })">{{ f.name || 'PNG file' }}</span>
              <q-btn flat round dense icon="download" color="white" size="xs"
                @click="downloadFile(f.url, f.name || 'design.png')" />
              <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deletePngFile(f)" />
            </div>
          </div>
          <div v-if="uploadingPng" class="upload-indicator">
            <q-spinner-dots color="teal-5" size="20px" />
            <span class="text-grey-4" style="font-size:0.82rem">Uploading PNG files...</span>
            <q-linear-progress indeterminate color="teal-5" size="2px" class="q-mt-xs" />
          </div>
          <q-file v-else v-model="pngFile" outlined color="teal-5" dense label="Upload PNG files"
            accept=".png" multiple @update:model-value="uploadPng">
            <template #prepend><q-icon name="upload" color="grey-5" /></template>
          </q-file>
        </div>

        <!-- PSD Files -->
        <div class="col">
          <div class="sidebar-label"><q-icon name="brush" size="xs" /> PSD Files</div>
          <div v-if="designDetail.psdFiles?.length" class="q-mb-sm">
            <div v-for="f in designDetail.psdFiles" :key="f.id" class="row items-center q-gutter-sm q-mb-xs">
              <span class="text-teal-4 ellipsis" style="font-size:0.82rem; max-width: 160px">{{ f.name || 'PSD file' }}</span>
              <q-btn flat round dense icon="download" color="white" size="xs"
                @click="downloadFile(f.url, f.name || 'design.psd')" />
              <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deletePsdFile(f)" />
            </div>
          </div>
          <div v-if="uploadingPsd" class="upload-indicator">
            <q-spinner-dots color="teal-5" size="20px" />
            <span class="text-grey-4" style="font-size:0.82rem">Uploading PSD files...</span>
            <q-linear-progress indeterminate color="teal-5" size="2px" class="q-mt-xs" />
          </div>
          <q-file v-else v-model="psdFile" outlined color="teal-5" dense label="Upload PSD files"
            accept=".psd,.psb" multiple @update:model-value="uploadPsd">
            <template #prepend><q-icon name="upload" color="grey-5" /></template>
          </q-file>
        </div>
      </div>

      <!-- Mockups -->
      <div class="q-mb-md">
        <div class="sidebar-label"><q-icon name="photo_library" size="xs" /> Mockups</div>
        <div v-if="designDetail.mockups?.length" class="mockup-grid q-mb-sm">
          <div v-for="m in designDetail.mockups" :key="m.id" class="mockup-thumb-wrap"
            :class="{ 'mockup-main': m.mainMockup }">
            <img :src="thumbUrl(m.url)" class="mockup-thumb" style="cursor:pointer" loading="lazy"
              @click="emit('view-images', { images: designDetail.mockups.map(x => x.url), index: designDetail.mockups.indexOf(m) })" />
            <div class="mockup-actions">
              <q-btn flat round dense icon="download" color="white" size="xs"
                @click.stop="downloadFile(m.url, 'mockup-' + m.id + '.png')" />
              <q-btn v-if="!m.mainMockup" flat round dense icon="star_outline" color="amber" size="xs"
                @click.stop="setMainMockup(m)" title="Set as main" />
              <q-btn flat round dense icon="close" color="red-4" size="xs"
                @click.stop="deleteMockup(m)" />
            </div>
            <q-icon v-if="m.mainMockup" name="star" color="amber" size="14px" class="mockup-star" />
          </div>
        </div>
        <div v-if="uploadingMockup" class="upload-indicator">
          <q-spinner-dots color="teal-5" size="20px" />
          <span class="text-grey-4" style="font-size:0.82rem">Uploading mockups...</span>
          <q-linear-progress indeterminate color="teal-5" size="2px" class="q-mt-xs" />
        </div>
        <q-file v-else v-model="mockupFile" outlined color="teal-5" dense label="Upload mockups"
          accept="image/*" multiple @update:model-value="uploadMockup">
          <template #prepend><q-icon name="add_photo_alternate" color="grey-5" /></template>
        </q-file>
      </div>

      <!-- Idea Creator -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="lightbulb" size="xs" /> Idea Creator</div>
        <q-select v-model="ideaCreatorId" :options="memberOptions" option-value="id" option-label="displayName"
          emit-value map-options outlined dense color="teal-5" clearable
          @update:model-value="updateIdeaCreator" />
      </div>

      <!-- Designers -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="design_services" size="xs" /> Designers</div>
        <q-select v-model="designerIds" :options="memberOptions" option-value="id" option-label="displayName"
          emit-value map-options multiple outlined dense color="teal-5" use-chips
          @update:model-value="updateDesigners" />
      </div>

      <!-- Product Types -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="inventory_2" size="xs" /> Product Types</div>
        <q-select v-model="productTypeIds" :options="productTypeOptions" option-value="id" option-label="name"
          emit-value map-options multiple outlined dense color="teal-5" use-chips
          @update:model-value="updateDetail" />
      </div>

      <!-- Niches -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="category" size="xs" /> Niches</div>
        <q-select v-model="nicheIds" :options="nicheOptions" option-value="id" option-label="name"
          emit-value map-options multiple outlined dense color="teal-5" use-chips
          @update:model-value="updateDetail" />
      </div>

      <!-- Occasion -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="celebration" size="xs" /> Occasion</div>
        <q-select v-model="occasionId" :options="occasionOptions" option-value="id" option-label="name"
          emit-value map-options outlined dense color="teal-5" clearable
          @update:model-value="updateDetail" />
      </div>

      <!-- Custom Toggle -->
      <div class="q-mb-sm row items-center">
        <div class="sidebar-label q-mr-md"><q-icon name="tune" size="xs" /> Custom</div>
        <q-toggle v-model="isCustom" color="teal-5" @update:model-value="updateDetail" />
      </div>

      <!-- Design Status (read-only) -->
      <div v-if="designDetail.designStatus" class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="flag" size="xs" /> Design Status</div>
        <q-chip dense :color="designStatusColor" :text-color="designStatusTextColor" :label="designDetail.designStatus" />
      </div>

      <!-- Approval Date (read-only) -->
      <div v-if="designDetail.approvalDate" class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="check_circle" size="xs" /> Approved</div>
        <div class="text-grey-3" style="font-size:0.82rem">{{ formatDate(designDetail.approvalDate) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { designApi, lookupApi } from 'src/api/tasks'
import { thumbUrl } from 'src/utils/fileUrl'

const props = defineProps({
  cardId: { type: Number, required: true },
  boardMembers: { type: Array, default: () => [] }
})
const emit = defineEmits(['updated', 'view-images', 'members-changed'])
const $q = useQuasar()

const designDetail = ref(null)
const pngFile = ref(null)
const psdFile = ref(null)
const mockupFile = ref(null)
const uploadingPng = ref(false)
const uploadingPsd = ref(false)
const uploadingMockup = ref(false)

// Form state
const ideaCreatorId = ref(null)
const designerIds = ref([])
const productTypeIds = ref([])
const nicheIds = ref([])
const occasionId = ref(null)
const isCustom = ref(false)

// Lookup options
const productTypeOptions = ref([])
const nicheOptions = ref([])
const occasionOptions = ref([])

const memberOptions = ref([])

watch(() => props.boardMembers, (members) => {
  memberOptions.value = members.map(m => {
    const u = m.user || m
    return { id: u.id, displayName: [u.firstName, u.lastName].filter(Boolean).join(' ') || u.userName }
  })
}, { immediate: true })

const formatDate = (d) => d ? new Date(d).toLocaleString() : ''

const downloadFile = async (url, filename) => {
  try {
    if (url.startsWith('http') && !url.startsWith(window.location.origin)) {
      window.open(url, '_blank')
      return
    }
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

const designStatusColor = computed(() => {
  const s = designDetail.value?.designStatus
  if (s === 'APPROVED') return 'green-9'
  if (s === 'DELETED') return 'red-9'
  return 'orange-9'
})
const designStatusTextColor = computed(() => {
  const s = designDetail.value?.designStatus
  if (s === 'APPROVED') return 'green-2'
  if (s === 'DELETED') return 'red-2'
  return 'orange-2'
})

const loadDesignDetail = async () => {
  try {
    const res = await designApi.getDetail(props.cardId)
    designDetail.value = res.data.data
    // Sync form state
    ideaCreatorId.value = designDetail.value.ideaCreator?.id || null
    designerIds.value = designDetail.value.designers?.map(d => d.id) || []
    productTypeIds.value = designDetail.value.productTypes?.map(p => p.id) || []
    nicheIds.value = designDetail.value.niches?.map(n => n.id) || []
    occasionId.value = designDetail.value.occasion?.id || null
    isCustom.value = designDetail.value.custom || false
  } catch {
    // Design detail may not exist yet
  }
}

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

const updateDetail = async () => {
  try {
    const res = await designApi.updateDetail(props.cardId, {
      ideaCreatorId: ideaCreatorId.value,
      productTypeIds: productTypeIds.value,
      nicheIds: nicheIds.value,
      occasionId: occasionId.value || 0,
      custom: isCustom.value
    })
    designDetail.value = res.data.data
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to update design detail' })
  }
}

const updateIdeaCreator = () => updateDetail()

const updateDesigners = async (newIds) => {
  const current = designDetail.value.designers?.map(d => d.id) || []
  const toAdd = newIds.filter(id => !current.includes(id))
  const toRemove = current.filter(id => !newIds.includes(id))
  try {
    for (const id of toAdd) {
      const res = await designApi.addDesigner(props.cardId, id)
      designDetail.value = res.data.data
    }
    for (const id of toRemove) {
      const res = await designApi.removeDesigner(props.cardId, id)
      designDetail.value = res.data.data
    }
    if (toAdd.length || toRemove.length) {
      emit('members-changed')
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to update designers' })
  }
}

const uploadPng = async (files) => {
  if (!files?.length) return
  pngFile.value = null
  uploadingPng.value = true
  for (const file of files) {
    const fd = new FormData()
    fd.append('file', file)
    try {
      await designApi.uploadPng(props.cardId, fd)
    } catch {
      $q.notify({ type: 'negative', message: `Failed to upload ${file.name}` })
    }
  }
  await loadDesignDetail()
  uploadingPng.value = false
}

const deletePngFile = async (f) => {
  try {
    await designApi.deletePngFile(props.cardId, f.id)
    await loadDesignDetail()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove PNG' })
  }
}

const uploadPsd = async (files) => {
  if (!files?.length) return
  psdFile.value = null
  uploadingPsd.value = true
  for (const file of files) {
    const fd = new FormData()
    fd.append('file', file)
    try {
      await designApi.uploadPsd(props.cardId, fd)
    } catch {
      $q.notify({ type: 'negative', message: `Failed to upload ${file.name}` })
    }
  }
  await loadDesignDetail()
  uploadingPsd.value = false
}

const deletePsdFile = async (f) => {
  try {
    await designApi.deletePsdFile(props.cardId, f.id)
    await loadDesignDetail()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove PSD' })
  }
}

const uploadMockup = async (files) => {
  if (!files?.length) return
  mockupFile.value = null
  uploadingMockup.value = true
  for (const file of files) {
    const fd = new FormData()
    fd.append('file', file)
    try {
      await designApi.uploadMockup(props.cardId, fd)
    } catch {
      $q.notify({ type: 'negative', message: `Failed to upload ${file.name}` })
    }
  }
  await loadDesignDetail()
  uploadingMockup.value = false
  emit('updated')
}

const deleteMockup = async (m) => {
  try {
    await designApi.deleteMockup(props.cardId, m.id)
    await loadDesignDetail()
    emit('updated')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to delete mockup' })
  }
}

const setMainMockup = async (m) => {
  try {
    await designApi.setMainMockup(props.cardId, m.id)
    await loadDesignDetail()
    emit('updated')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to set main mockup' })
  }
}

watch(() => props.cardId, (id) => {
  if (id) {
    loadDesignDetail()
    loadLookups()
  }
}, { immediate: true })
</script>

<style scoped>
.design-detail-card {
  background: var(--erp-border-subtle);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
}
.sidebar-label {
  font-size: 0.75rem;
  color: #9e9e9e;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.mockup-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
}
.mockup-thumb-wrap {
  position: relative;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid transparent;
}
.mockup-thumb-wrap.mockup-main {
  border-color: #ffc107;
}
.mockup-thumb {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
}
.mockup-actions {
  position: absolute;
  top: 2px;
  right: 2px;
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}
.mockup-actions .q-btn { background: rgba(0,0,0,0.6); }
.mockup-thumb-wrap:hover .mockup-actions { opacity: 1; }
.mockup-star {
  position: absolute;
  bottom: 2px;
  left: 2px;
}
.upload-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(38, 166, 154, 0.08);
  border: 1px solid rgba(38, 166, 154, 0.2);
  border-radius: 4px;
  flex-wrap: wrap;
}
.upload-indicator .q-linear-progress {
  width: 100%;
}
</style>
