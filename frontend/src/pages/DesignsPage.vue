<template>
  <q-page class="designs-page">
    <!-- Header -->
    <div class="page-header row items-center no-wrap q-px-xl q-pt-lg q-pb-md">
      <q-btn flat round dense icon="arrow_back" color="grey-5" class="q-mr-sm" to="/designs" />
      <div>
        <div class="text-h5 text-white text-weight-light row items-center gap-2">
          <q-icon name="palette" color="teal-5" />
          Designs
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">All POD Design cards across boards</div>
      </div>
      <q-space />
      <q-btn icon="add" label="New Design" color="teal-6" unelevated
        @click="openCreateDialog" />
    </div>

    <div class="q-px-xl q-pb-xl">
      <!-- Filter bar -->
      <div class="row q-gutter-sm q-mb-md items-end" style="flex-wrap: wrap">
        <q-input v-model="filters.search" outlined color="teal-5" dense label="Search" clearable
          style="min-width:200px" debounce="400" @update:model-value="loadDesigns">
          <template #prepend><q-icon name="search" /></template>
        </q-input>
        <q-select v-model="filters.stage" :options="stageOptions" outlined dense color="teal-5"
          label="Stage" clearable style="min-width:140px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.status" :options="statusOptions" outlined dense color="teal-5"
          label="Status" clearable style="min-width:140px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.productTypeId" :options="productTypeOptions" option-value="id" option-label="name"
          emit-value map-options outlined dense color="teal-5" label="Product Type" clearable
          style="min-width:160px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.nicheId" :options="nicheOptions" option-value="id" option-label="name"
          emit-value map-options outlined dense color="teal-5" label="Niche" clearable
          style="min-width:140px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.occasionId" :options="occasionOptions" option-value="id" option-label="name"
          emit-value map-options outlined dense color="teal-5" label="Occasion" clearable
          style="min-width:140px" @update:model-value="loadDesigns" />
        <q-toggle v-model="filters.custom" color="teal-5" label="Custom only"
          @update:model-value="loadDesigns" />
        <q-input v-model="filters.dateFrom" type="date" outlined dense color="teal-5" label="From"
          stack-label clearable style="min-width:140px" @update:model-value="loadDesigns" />
        <q-input v-model="filters.dateTo" type="date" outlined dense color="teal-5" label="To"
          stack-label clearable style="min-width:140px" @update:model-value="loadDesigns" />
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex flex-center q-py-xl">
        <q-spinner color="teal-5" size="48px" />
      </div>

      <!-- Empty state -->
      <div v-else-if="designs.length === 0" class="flex flex-center column q-py-xl text-center">
        <q-icon name="palette" size="64px" color="grey-7" />
        <div class="text-h6 text-grey-5 q-mt-md">No designs found</div>
        <div class="text-caption text-grey-6">Try adjusting your filters</div>
      </div>

      <!-- Design grid -->
      <div v-else class="design-grid">
        <div v-for="d in designs" :key="d.id" class="design-card" @click="openDesign(d)">
          <div class="design-card-img">
            <img v-if="d.mainMockupUrl" :src="d.mainMockupUrl + '?thumb=true'" loading="lazy" />
            <div v-else class="design-card-no-img">
              <q-icon name="image" size="32px" color="grey-7" />
            </div>
          </div>
          <div class="design-card-body">
            <div class="design-card-name">{{ d.cardName || d.name || 'Untitled' }}</div>
            <div class="design-card-meta">
              <q-chip v-if="d.stage" dense size="xs" color="grey-8" text-color="grey-3">{{ d.stage }}</q-chip>
              <q-chip v-if="!d.cardId" dense size="xs" color="teal-9" text-color="teal-2">Standalone</q-chip>
              <q-chip v-if="d.boardActive === false && d.cardId" dense size="xs" color="orange-9" text-color="orange-3">Archived</q-chip>
            </div>
            <div class="design-card-info text-grey-5">
              <span v-if="d.ideaCreator">{{ d.ideaCreator.firstName }} {{ d.ideaCreator.lastName }}</span>
            </div>
            <div v-if="d.productTypes?.length" class="design-card-chips">
              <q-chip v-for="pt in d.productTypes" :key="pt.id" dense size="xs" color="deep-purple-9" text-color="purple-2">
                {{ pt.name }}
              </q-chip>
            </div>
            <q-btn v-if="authStore.isAdmin" flat round dense icon="delete" color="red-4" size="xs"
              class="design-card-delete" @click.stop="confirmDeleteDesign(d)">
              <q-tooltip>Delete design</q-tooltip>
            </q-btn>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex flex-center q-mt-lg">
        <q-pagination v-model="page" :max="totalPages" direction-links boundary-links
          color="teal-5" active-color="teal-6" @update:model-value="loadDesigns" />
      </div>
    </div>

    <!-- Design View Dialog -->
    <design-view-dialog v-model="showDesignDialog" :design="selectedDesign" @open-card="onOpenCard" @updated="loadDesigns" @deleted="loadDesigns" />

    <!-- Card Detail Dialog -->
    <card-detail-dialog
      v-model="showCardDialog"
      :card-id="selectedCardId"
      :board-id="selectedBoardId"
      board-type="POD_DESIGN"
      :board-labels="[]"
      :board-types="[]"
      :board-members="[]"
      :board-columns="[]"
      @updated="loadDesigns"
    />

    <!-- Design Config Dialog -->
    <design-config-dialog v-model="showConfigDialog" />

    <!-- Create Design Dialog -->
    <q-dialog v-model="showCreateDialog" persistent>
      <q-card style="min-width: 500px; max-width: 700px">
        <!-- Step 1: Basic Info -->
        <template v-if="!createdDesignId">
          <q-card-section>
            <div class="text-h6 text-white">New Design</div>
          </q-card-section>
          <q-card-section class="q-pt-none">
            <q-input v-model="newDesign.name" outlined dense color="teal-5" label="Design Name *" class="q-mb-md" />
            <q-select v-model="newDesign.productTypeIds" :options="productTypeOptions" option-value="id" option-label="name"
              emit-value map-options multiple outlined dense color="teal-5" label="Product Types" use-chips class="q-mb-md" />
            <q-select v-model="newDesign.nicheIds" :options="nicheOptions" option-value="id" option-label="name"
              emit-value map-options multiple outlined dense color="teal-5" label="Niches" use-chips class="q-mb-md" />
            <q-select v-model="newDesign.occasionId" :options="occasionOptions" option-value="id" option-label="name"
              emit-value map-options outlined dense color="teal-5" label="Occasion" clearable class="q-mb-md" />
            <q-toggle v-model="newDesign.custom" color="teal-5" label="Custom" />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancel" color="grey-5" v-close-popup />
            <q-btn unelevated label="Create" color="teal-6"
              :disable="!newDesign.name?.trim()"
              :loading="creating"
              @click="createDesign" />
          </q-card-actions>
        </template>

        <!-- Step 2: Upload Files -->
        <template v-else>
          <q-card-section>
            <div class="text-h6 text-white">Upload Files — {{ newDesign.name }}</div>
            <div class="text-caption text-grey-5">Design created. You can now upload files or click Done to finish.</div>
          </q-card-section>
          <q-card-section class="q-pt-none">
            <!-- PNG Files -->
            <div class="q-mb-md">
              <div class="text-caption text-grey-4 q-mb-xs"><q-icon name="image" size="xs" /> PNG Files</div>
              <div v-for="f in createdPngFiles" :key="f.id" class="row items-center q-gutter-sm q-mb-xs">
                <span class="text-teal-4 ellipsis" style="font-size:0.82rem; max-width:240px">{{ f.name }}</span>
                <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deleteCreatedFile(f, 'png')" />
              </div>
              <q-file v-model="createPngFile" outlined color="teal-5" dense label="Upload PNG"
                accept=".png" :loading="uploadingPng" @update:model-value="uploadCreatedPng">
                <template #prepend><q-icon name="upload" color="grey-5" /></template>
              </q-file>
            </div>

            <!-- PSD Files -->
            <div class="q-mb-md">
              <div class="text-caption text-grey-4 q-mb-xs"><q-icon name="brush" size="xs" /> PSD Files</div>
              <div v-for="f in createdPsdFiles" :key="f.id" class="row items-center q-gutter-sm q-mb-xs">
                <span class="text-purple-4 ellipsis" style="font-size:0.82rem; max-width:240px">{{ f.name }}</span>
                <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deleteCreatedFile(f, 'psd')" />
              </div>
              <q-file v-model="createPsdFile" outlined color="teal-5" dense label="Upload PSD"
                accept=".psd,.psb" :loading="uploadingPsd" @update:model-value="uploadCreatedPsd">
                <template #prepend><q-icon name="upload" color="grey-5" /></template>
              </q-file>
            </div>

            <!-- Mockups -->
            <div class="q-mb-md">
              <div class="text-caption text-grey-4 q-mb-xs"><q-icon name="photo_library" size="xs" /> Mockups</div>
              <div v-if="createdMockups.length" class="row q-gutter-sm q-mb-sm" style="flex-wrap:wrap">
                <div v-for="m in createdMockups" :key="m.id" style="position:relative; width:64px; height:64px; border-radius:6px; overflow:hidden; border:2px solid transparent"
                  :style="m.mainMockup ? 'border-color:#ffc107' : ''">
                  <img :src="m.url + '?thumb=true'" style="width:100%; height:100%; object-fit:cover" loading="lazy" />
                  <q-btn flat round dense icon="close" color="red-4" size="8px"
                    style="position:absolute; top:1px; right:1px; background:rgba(0,0,0,0.6)"
                    @click="deleteCreatedMockup(m)" />
                </div>
              </div>
              <q-file v-model="createMockupFile" outlined color="teal-5" dense label="Upload mockup"
                accept="image/*" :loading="uploadingMockup" @update:model-value="uploadCreatedMockup">
                <template #prepend><q-icon name="add_photo_alternate" color="grey-5" /></template>
              </q-file>
            </div>
          </q-card-section>
          <q-card-actions align="right">
            <q-btn unelevated label="Done" color="teal-6" @click="finishCreate" />
          </q-card-actions>
        </template>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { designsApi, lookupApi } from 'src/api/tasks'
import DesignViewDialog from 'src/components/DesignViewDialog.vue'
import DesignConfigDialog from 'src/components/DesignConfigDialog.vue'
import CardDetailDialog from 'src/components/CardDetailDialog.vue'
const $q = useQuasar()
const authStore = useAuthStore()

const loading = ref(false)
const designs = ref([])
const page = ref(1)
const totalPages = ref(0)
const showDesignDialog = ref(false)
const selectedDesign = ref(null)
const showConfigDialog = ref(false)
const showCardDialog = ref(false)
const selectedCardId = ref(null)
const selectedBoardId = ref(null)

const showCreateDialog = ref(false)
const creating = ref(false)
const newDesign = ref({ name: '', productTypeIds: [], nicheIds: [], occasionId: null, custom: false })

// Step 2: file uploads after creation
const createdDesignId = ref(null)
const createdPngFiles = ref([])
const createdPsdFiles = ref([])
const createdMockups = ref([])
const createPngFile = ref(null)
const createPsdFile = ref(null)
const createMockupFile = ref(null)
const uploadingPng = ref(false)
const uploadingPsd = ref(false)
const uploadingMockup = ref(false)

const stageOptions = ['Draft', 'Idea', 'Doing', 'Checking', 'Need to Fix', 'Fixing', 'Fix-Checking', 'Done', 'Listed', 'Canceled']
const statusOptions = ['OPEN', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'CANCELLED']

const productTypeOptions = ref([])
const nicheOptions = ref([])
const occasionOptions = ref([])

const filters = ref({
  search: '',
  stage: null,
  status: null,
  productTypeId: null,
  nicheId: null,
  occasionId: null,
  custom: false,
  dateFrom: null,
  dateTo: null
})

const loadDesigns = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: 20,
      sortBy: 'createdAt',
      sortDir: 'desc'
    }
    if (filters.value.search) params.search = filters.value.search
    if (filters.value.stage) params.stage = filters.value.stage
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.productTypeId) params.productTypeId = filters.value.productTypeId
    if (filters.value.nicheId) params.nicheId = filters.value.nicheId
    if (filters.value.occasionId) params.occasionId = filters.value.occasionId
    if (filters.value.custom) params.custom = true
    if (filters.value.dateFrom) params.dateFrom = filters.value.dateFrom
    if (filters.value.dateTo) params.dateTo = filters.value.dateTo

    const res = await designsApi.getAll(params)
    const data = res.data.data
    designs.value = data.content || []
    totalPages.value = data.totalPages || 0
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load designs' })
  } finally {
    loading.value = false
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

const confirmDeleteDesign = (d) => {
  $q.dialog({
    title: 'Delete Design',
    message: `Are you sure you want to delete "${d.cardName || d.name || 'Untitled'}"? This action cannot be undone.`,
    color: 'red',
    cancel: { flat: true, color: 'grey-5' },
    ok: { label: 'Delete', color: 'red', unelevated: true }
  }).onOk(async () => {
    try {
      await designsApi.delete(d.id)
      $q.notify({ type: 'positive', message: 'Design deleted' })
      loadDesigns()
    } catch {
      $q.notify({ type: 'negative', message: 'Failed to delete design' })
    }
  })
}

const openDesign = (d) => {
  selectedDesign.value = d
  showDesignDialog.value = true
}

const onOpenCard = ({ cardId, boardId }) => {
  showDesignDialog.value = false
  selectedCardId.value = cardId
  selectedBoardId.value = boardId
  showCardDialog.value = true
}

const openCreateDialog = () => {
  newDesign.value = { name: '', productTypeIds: [], nicheIds: [], occasionId: null, custom: false }
  createdDesignId.value = null
  createdPngFiles.value = []
  createdPsdFiles.value = []
  createdMockups.value = []
  showCreateDialog.value = true
}

const createDesign = async () => {
  if (!newDesign.value.name?.trim()) return
  creating.value = true
  try {
    const res = await designsApi.create({
      name: newDesign.value.name.trim(),
      productTypeIds: newDesign.value.productTypeIds,
      nicheIds: newDesign.value.nicheIds,
      occasionId: newDesign.value.occasionId || null,
      custom: newDesign.value.custom
    })
    createdDesignId.value = res.data.data.id
    $q.notify({ type: 'positive', message: 'Design created. You can now upload files.' })
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to create design' })
  } finally {
    creating.value = false
  }
}

const uploadCreatedPng = async (file) => {
  if (!file || !createdDesignId.value) return
  uploadingPng.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await designsApi.uploadPng(createdDesignId.value, fd)
    createdPngFiles.value.push(res.data.data)
    createPngFile.value = null
  } catch {
    $q.notify({ type: 'negative', message: 'PNG upload failed' })
  } finally {
    uploadingPng.value = false
  }
}

const uploadCreatedPsd = async (file) => {
  if (!file || !createdDesignId.value) return
  uploadingPsd.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await designsApi.uploadPsd(createdDesignId.value, fd)
    createdPsdFiles.value.push(res.data.data)
    createPsdFile.value = null
  } catch {
    $q.notify({ type: 'negative', message: 'PSD upload failed' })
  } finally {
    uploadingPsd.value = false
  }
}

const uploadCreatedMockup = async (file) => {
  if (!file || !createdDesignId.value) return
  uploadingMockup.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await designsApi.uploadMockup(createdDesignId.value, fd)
    createdMockups.value.push(res.data.data)
    createMockupFile.value = null
  } catch {
    $q.notify({ type: 'negative', message: 'Mockup upload failed' })
  } finally {
    uploadingMockup.value = false
  }
}

const deleteCreatedFile = async (f, type) => {
  try {
    if (type === 'png') {
      await designsApi.deletePngFile(createdDesignId.value, f.id)
      createdPngFiles.value = createdPngFiles.value.filter(x => x.id !== f.id)
    } else {
      await designsApi.deletePsdFile(createdDesignId.value, f.id)
      createdPsdFiles.value = createdPsdFiles.value.filter(x => x.id !== f.id)
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to delete file' })
  }
}

const deleteCreatedMockup = async (m) => {
  try {
    await designsApi.deleteMockup(createdDesignId.value, m.id)
    createdMockups.value = createdMockups.value.filter(x => x.id !== m.id)
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to delete mockup' })
  }
}

const finishCreate = () => {
  showCreateDialog.value = false
  createdDesignId.value = null
  loadDesigns()
}

onMounted(() => {
  loadDesigns()
  loadLookups()
})
</script>

<style scoped>
.designs-page {
  background: var(--erp-bg);
  min-height: 100vh;
}
.design-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.design-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.1s;
}
.design-card:hover {
  box-shadow: 0 6px 24px rgba(0,0,0,0.5);
  transform: translateY(-2px);
}
.design-card-img {
  width: 100%;
  aspect-ratio: 1;
  background: var(--erp-bg-secondary);
}
.design-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.design-card-no-img {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.design-card-body {
  padding: 10px 12px;
}
.design-card-name {
  color: #ffffffde;
  font-size: 0.85rem;
  font-weight: 500;
  line-height: 1.3;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.design-card-meta {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}
.design-card-info {
  font-size: 0.72rem;
  margin-bottom: 4px;
}
.design-card-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
}
.design-card-delete {
  position: absolute;
  bottom: 6px;
  right: 6px;
  opacity: 0;
  transition: opacity 0.15s;
}
.design-card:hover .design-card-delete {
  opacity: 1;
}
.design-card-body {
  position: relative;
}
</style>
