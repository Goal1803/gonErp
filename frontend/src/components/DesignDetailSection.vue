<template>
  <div v-if="designDetail" class="design-detail-section q-mt-md">
    <div class="text-caption text-grey-5 q-mb-sm row items-center gap-2">
      <q-icon name="palette" /> Design Details
    </div>
    <div class="design-detail-card q-pa-md">

      <!-- Design Files Row -->
      <div class="row q-gutter-md q-mb-md">
        <!-- PNG File -->
        <div class="col">
          <div class="sidebar-label"><q-icon name="image" size="xs" /> PNG File</div>
          <div v-if="designDetail.pngFileUrl" class="row items-center q-gutter-sm">
            <a :href="designDetail.pngFileUrl" target="_blank" class="text-teal-4" style="font-size:0.82rem; text-decoration:none">
              {{ designDetail.pngFileName || 'PNG file' }}
            </a>
            <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deletePng" :loading="deletingPng" />
          </div>
          <q-file v-else v-model="pngFile" outlined dark color="teal-5" dense label="Upload PNG"
            accept=".png" @update:model-value="uploadPng">
            <template #prepend><q-icon name="upload" color="grey-5" /></template>
          </q-file>
        </div>

        <!-- PSD File -->
        <div class="col">
          <div class="sidebar-label"><q-icon name="brush" size="xs" /> PSD File</div>
          <div v-if="designDetail.psdFileUrl" class="row items-center q-gutter-sm">
            <a :href="designDetail.psdFileUrl" target="_blank" class="text-teal-4" style="font-size:0.82rem; text-decoration:none">
              {{ designDetail.psdFileName || 'PSD file' }}
            </a>
            <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deletePsd" :loading="deletingPsd" />
          </div>
          <q-file v-else v-model="psdFile" outlined dark color="teal-5" dense label="Upload PSD"
            accept=".psd,.psb" @update:model-value="uploadPsd">
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
            <img :src="m.url" class="mockup-thumb" />
            <div class="mockup-actions">
              <q-btn v-if="!m.mainMockup" flat round dense icon="star_outline" color="amber" size="xs"
                @click.stop="setMainMockup(m)" title="Set as main" />
              <q-btn flat round dense icon="close" color="red-4" size="xs"
                @click.stop="deleteMockup(m)" />
            </div>
            <q-icon v-if="m.mainMockup" name="star" color="amber" size="14px" class="mockup-star" />
          </div>
        </div>
        <q-file v-model="mockupFile" outlined dark color="teal-5" dense label="Upload mockup"
          accept="image/*" @update:model-value="uploadMockup">
          <template #prepend><q-icon name="add_photo_alternate" color="grey-5" /></template>
        </q-file>
      </div>

      <!-- Idea Creator -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="lightbulb" size="xs" /> Idea Creator</div>
        <q-select v-model="ideaCreatorId" :options="memberOptions" option-value="id" option-label="displayName"
          emit-value map-options outlined dark dense color="teal-5" clearable
          @update:model-value="updateIdeaCreator" />
      </div>

      <!-- Designers -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="design_services" size="xs" /> Designers</div>
        <q-select v-model="designerIds" :options="memberOptions" option-value="id" option-label="displayName"
          emit-value map-options multiple outlined dark dense color="teal-5" use-chips
          @update:model-value="updateDesigners" />
      </div>

      <!-- Product Types -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="inventory_2" size="xs" /> Product Types</div>
        <q-select v-model="productTypeIds" :options="productTypeOptions" option-value="id" option-label="name"
          emit-value map-options multiple outlined dark dense color="teal-5" use-chips
          @update:model-value="updateDetail" />
      </div>

      <!-- Niches -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="category" size="xs" /> Niches</div>
        <q-select v-model="nicheIds" :options="nicheOptions" option-value="id" option-label="name"
          emit-value map-options multiple outlined dark dense color="teal-5" use-chips
          @update:model-value="updateDetail" />
      </div>

      <!-- Occasion -->
      <div class="q-mb-sm">
        <div class="sidebar-label"><q-icon name="celebration" size="xs" /> Occasion</div>
        <q-select v-model="occasionId" :options="occasionOptions" option-value="id" option-label="name"
          emit-value map-options outlined dark dense color="teal-5" clearable
          @update:model-value="updateDetail" />
      </div>

      <!-- Custom Toggle -->
      <div class="q-mb-sm row items-center">
        <div class="sidebar-label q-mr-md"><q-icon name="tune" size="xs" /> Custom</div>
        <q-toggle v-model="isCustom" color="teal-5" dark @update:model-value="updateDetail" />
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

const props = defineProps({
  cardId: { type: Number, required: true },
  boardMembers: { type: Array, default: () => [] }
})
const emit = defineEmits(['updated'])
const $q = useQuasar()

const designDetail = ref(null)
const pngFile = ref(null)
const psdFile = ref(null)
const mockupFile = ref(null)
const deletingPng = ref(false)
const deletingPsd = ref(false)

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
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to update designers' })
  }
}

const uploadPng = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await designApi.uploadPng(props.cardId, fd)
    designDetail.value = res.data.data
    pngFile.value = null
  } catch {
    $q.notify({ type: 'negative', message: 'PNG upload failed' })
  }
}

const deletePng = async () => {
  deletingPng.value = true
  try {
    const res = await designApi.deletePng(props.cardId)
    designDetail.value = res.data.data
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove PNG' })
  } finally {
    deletingPng.value = false
  }
}

const uploadPsd = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await designApi.uploadPsd(props.cardId, fd)
    designDetail.value = res.data.data
    psdFile.value = null
  } catch {
    $q.notify({ type: 'negative', message: 'PSD upload failed' })
  }
}

const deletePsd = async () => {
  deletingPsd.value = true
  try {
    const res = await designApi.deletePsd(props.cardId)
    designDetail.value = res.data.data
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove PSD' })
  } finally {
    deletingPsd.value = false
  }
}

const uploadMockup = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    await designApi.uploadMockup(props.cardId, fd)
    await loadDesignDetail()
    mockupFile.value = null
    emit('updated')
  } catch {
    $q.notify({ type: 'negative', message: 'Mockup upload failed' })
  }
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
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
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
</style>
