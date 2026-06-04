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
      <q-btn icon="image_search" label="Search by image" color="indigo-6" unelevated class="q-mr-sm"
        @click="showImageSearchDialog = true" />
      <q-btn v-if="authStore.isAdmin" flat round dense icon="refresh" color="grey-5" class="q-mr-sm"
        :loading="rehashing" @click="runRehash">
        <q-tooltip>Rebuild image hashes for all mockups</q-tooltip>
      </q-btn>
      <q-btn icon="add" label="New Design" color="teal-6" unelevated
        @click="openCreateDialog" />
    </div>

    <div class="q-px-xl q-pb-xl">
      <!-- Active image-search banner -->
      <div v-if="imageSearchActive" class="image-search-banner row items-center q-mb-md q-pa-sm">
        <q-icon name="image_search" color="indigo-4" size="20px" class="q-mr-sm" />
        <div>
          <div class="text-caption text-indigo-3">Showing designs visually similar to your query image</div>
          <div class="text-caption text-grey-5">
            {{ designs.length }} match{{ designs.length === 1 ? '' : 'es' }} · threshold {{ imageSearchThreshold }}
          </div>
        </div>
        <img v-if="imageSearchPreview" :src="imageSearchPreview" class="banner-preview q-mx-md" />
        <q-space />
        <q-btn flat color="grey-5" icon="close" label="Clear search" no-caps @click="clearImageSearch" />
      </div>

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
            <img v-if="d.mainMockupUrl" :src="thumbUrl(d.mainMockupUrl)" loading="lazy" />
            <div v-else class="design-card-no-img">
              <q-icon name="image" size="32px" color="grey-7" />
            </div>
            <div v-if="d._distance != null" class="match-chip"
                 :class="'match-' + matchQuality(d._distance)">
              {{ matchLabel(d._distance) }}
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
                  <img :src="thumbUrl(m.url)" style="width:100%; height:100%; object-fit:cover" loading="lazy" />
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

    <!-- Image Search Dialog -->
    <q-dialog v-model="showImageSearchDialog" persistent>
      <q-card style="min-width: 460px; max-width: 520px">
        <q-card-section class="row items-center no-wrap">
          <q-icon name="image_search" color="indigo-4" size="md" class="q-mr-sm" />
          <div>
            <div class="text-h6 text-white">Search by image</div>
            <div class="text-caption text-grey-5">Upload an image to find visually similar designs</div>
          </div>
          <q-space />
          <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
        </q-card-section>

        <q-card-section class="q-pt-none">
          <div class="image-drop q-mb-md" @click="$refs.imageInput.click()" @drop.prevent="onDropImage" @dragover.prevent>
            <input ref="imageInput" type="file" accept="image/*" style="display:none" @change="onPickImage" />
            <template v-if="searchFilePreview">
              <img :src="searchFilePreview" class="drop-preview" />
              <div class="text-caption text-grey-5 q-mt-xs ellipsis">{{ searchFile?.name }}</div>
            </template>
            <template v-else>
              <q-icon name="add_photo_alternate" size="42px" color="indigo-4" />
              <div class="text-white q-mt-sm">Click or drop an image</div>
              <div class="text-caption text-grey-5">PNG, JPG, WebP</div>
            </template>
          </div>

          <div class="text-caption text-grey-5 q-mb-xs">
            Match tolerance: <strong class="text-indigo-3">{{ imageSearchThreshold }}</strong>
            <span class="text-grey-6">(lower = stricter)</span>
          </div>
          <q-slider v-model="imageSearchThreshold" :min="2" :max="28" :step="1"
                    color="indigo-4" label markers marker-labels="(v) => v" />
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn unelevated color="indigo-6" icon="search" label="Search"
                 :loading="searching" :disable="!searchFile" @click="runImageSearch" />
        </q-card-actions>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { designsApi, lookupApi } from 'src/api/tasks'
import { thumbUrl } from 'src/utils/fileUrl'
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

const stageOptions = ['Draft', 'Idea', 'Doing', 'Checking', 'Need to Fix', 'Fixing', 'Fix-Checking', 'Done', 'Seller Gen- Done', 'Listed', 'Seller Gen- Listed', 'Canceled']
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
  // If an image-search result set is active, don't clobber it with the normal list.
  if (imageSearchActive.value) return
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

// ── Search by image ─────────────────────────────────────────────────
const showImageSearchDialog = ref(false)
const searchFile = ref(null)
const searchFilePreview = ref('')
const searching = ref(false)
const imageSearchThreshold = ref(18)
const imageSearchActive = ref(false)
const imageSearchPreview = ref('')
const rehashing = ref(false)

function onPickImage(ev) {
  const f = ev.target.files?.[0]
  if (f) setSearchFile(f)
}
function onDropImage(ev) {
  const f = ev.dataTransfer?.files?.[0]
  if (f) setSearchFile(f)
}
function setSearchFile(f) {
  searchFile.value = f
  if (searchFilePreview.value) URL.revokeObjectURL(searchFilePreview.value)
  searchFilePreview.value = URL.createObjectURL(f)
}

async function runImageSearch() {
  if (!searchFile.value) return
  searching.value = true
  try {
    const fd = new FormData()
    fd.append('file', searchFile.value)
    const res = await designsApi.searchByImage(fd, { threshold: imageSearchThreshold.value, limit: 60 })
    const results = res.data.data || []
    designs.value = results.map(r => ({ ...r.design, _distance: r.distance }))
    totalPages.value = 0
    imageSearchActive.value = true
    imageSearchPreview.value = searchFilePreview.value
    showImageSearchDialog.value = false
    if (!results.length) {
      $q.notify({ type: 'warning', message: 'No designs matched. Try raising the tolerance.' })
    }
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Search failed' })
  } finally {
    searching.value = false
  }
}

function clearImageSearch() {
  imageSearchActive.value = false
  if (searchFilePreview.value) { URL.revokeObjectURL(searchFilePreview.value); searchFilePreview.value = '' }
  imageSearchPreview.value = ''
  searchFile.value = null
  loadDesigns()
}

function matchQuality(d) {
  if (d <= 4) return 'exact'
  if (d <= 10) return 'very'
  return 'similar'
}
function matchLabel(d) {
  if (d <= 4) return 'Near-exact match'
  if (d <= 10) return 'Very similar'
  return 'Similar'
}

async function runRehash() {
  rehashing.value = true
  // Live progress notification that we'll update as batches complete.
  const notify = $q.notify({
    type: 'ongoing',
    message: 'Checking mockup hash status...',
    timeout: 0, spinner: true, group: false
  })
  let totalUnhashed = 0
  let cumulativeProcessed = 0
  let cumulativeFailed = 0
  try {
    const stats = await designsApi.hashStats()
    const { total = 0, unhashed = 0 } = stats.data.data || {}
    if (unhashed === 0) {
      notify({ type: 'info', message: `All ${total} mockups already hashed.`, timeout: 3000, spinner: false })
      return
    }
    totalUnhashed = unhashed

    // Loop batches until the server says there's nothing left.
    const BATCH = 25
    // Safety cap so a runaway loop can't spin forever.
    const MAX_ITERATIONS = Math.ceil(unhashed / BATCH) + 10
    let iteration = 0
    while (iteration++ < MAX_ITERATIONS) {
      const res = await designsApi.rehashMockups(BATCH)
      const d = res.data.data || {}
      cumulativeProcessed += (d.processed || 0)
      cumulativeFailed += (d.failed || 0)
      const remaining = d.remaining ?? 0
      notify({
        message: `Hashing mockups... ${cumulativeProcessed} done, ${remaining} left`
          + (cumulativeFailed ? `, ${cumulativeFailed} failed` : ''),
        timeout: 0, spinner: true
      })
      if (remaining === 0) break
      // If a batch made no progress and no failures, the remaining rows are unreachable — stop.
      if ((d.processed || 0) === 0 && (d.failed || 0) === 0) break
    }

    notify({
      type: cumulativeFailed ? 'warning' : 'positive',
      message: `Done. Hashed ${cumulativeProcessed}/${totalUnhashed}`
        + (cumulativeFailed ? `. ${cumulativeFailed} failed — check backend logs.` : '.'),
      timeout: 8000, spinner: false
    })
  } catch (e) {
    notify({
      type: 'negative',
      message: e.response?.data?.message || `Rehash failed after ${cumulativeProcessed} mockups. Click again to resume.`,
      timeout: 8000, spinner: false
    })
  } finally {
    rehashing.value = false
  }
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
  position: relative;
}
.match-chip {
  position: absolute;
  top: 6px;
  left: 6px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.35);
}
.match-exact { background: #2e7d32; }
.match-very { background: #00838f; }
.match-similar { background: #ef6c00; }

.image-search-banner {
  background: linear-gradient(90deg, rgba(92, 107, 192, 0.12), rgba(63, 81, 181, 0.06));
  border: 1px solid rgba(121, 134, 203, 0.35);
  border-radius: 8px;
}
.banner-preview {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--erp-border-subtle);
}

.image-drop {
  border: 2px dashed rgba(121, 134, 203, 0.45);
  border-radius: 10px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.image-drop:hover {
  border-color: rgba(121, 134, 203, 0.8);
  background: rgba(121, 134, 203, 0.06);
}
.drop-preview {
  max-width: 100%;
  max-height: 180px;
  border-radius: 6px;
  display: block;
  margin: 0 auto;
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
