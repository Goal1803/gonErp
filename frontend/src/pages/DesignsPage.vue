<template>
  <q-page class="designs-page">
    <!-- Header -->
    <div class="page-header row items-center no-wrap q-px-xl q-pt-lg q-pb-md">
      <div>
        <div class="text-h5 text-white text-weight-light row items-center gap-2">
          <q-icon name="palette" color="teal-5" />
          Designs
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">All POD Design cards across boards</div>
      </div>
      <q-space />
      <q-btn v-if="authStore.isAdmin" flat round icon="settings" color="teal-5"
        @click="showConfigDialog = true">
        <q-tooltip>Design Config</q-tooltip>
      </q-btn>
    </div>

    <div class="q-px-xl q-pb-xl">
      <!-- Filter bar -->
      <div class="row q-gutter-sm q-mb-md items-end" style="flex-wrap: wrap">
        <q-input v-model="filters.search" outlined dark color="teal-5" dense label="Search" clearable
          style="min-width:200px" debounce="400" @update:model-value="loadDesigns">
          <template #prepend><q-icon name="search" /></template>
        </q-input>
        <q-select v-model="filters.stage" :options="stageOptions" outlined dark dense color="teal-5"
          label="Stage" clearable style="min-width:140px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.status" :options="statusOptions" outlined dark dense color="teal-5"
          label="Status" clearable style="min-width:140px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.productTypeId" :options="productTypeOptions" option-value="id" option-label="name"
          emit-value map-options outlined dark dense color="teal-5" label="Product Type" clearable
          style="min-width:160px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.nicheId" :options="nicheOptions" option-value="id" option-label="name"
          emit-value map-options outlined dark dense color="teal-5" label="Niche" clearable
          style="min-width:140px" @update:model-value="loadDesigns" />
        <q-select v-model="filters.occasionId" :options="occasionOptions" option-value="id" option-label="name"
          emit-value map-options outlined dark dense color="teal-5" label="Occasion" clearable
          style="min-width:140px" @update:model-value="loadDesigns" />
        <q-toggle v-model="filters.custom" dark color="teal-5" label="Custom only"
          @update:model-value="loadDesigns" />
        <q-input v-model="filters.dateFrom" type="date" outlined dark dense color="teal-5" label="From"
          stack-label clearable style="min-width:140px" @update:model-value="loadDesigns" />
        <q-input v-model="filters.dateTo" type="date" outlined dark dense color="teal-5" label="To"
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
        <div v-for="d in designs" :key="d.cardId" class="design-card" @click="openDesign(d)">
          <div class="design-card-img">
            <img v-if="d.mainMockupUrl" :src="d.mainMockupUrl" />
            <div v-else class="design-card-no-img">
              <q-icon name="image" size="32px" color="grey-7" />
            </div>
          </div>
          <div class="design-card-body">
            <div class="design-card-name">{{ d.cardName }}</div>
            <div class="design-card-meta">
              <q-chip dense size="xs" color="grey-8" text-color="grey-3">{{ d.stage }}</q-chip>
              <q-chip v-if="!d.boardActive" dense size="xs" color="orange-9" text-color="orange-3">Archived</q-chip>
            </div>
            <div class="design-card-info text-grey-5">
              <span v-if="d.seller">{{ d.seller.firstName }} {{ d.seller.lastName }}</span>
            </div>
            <div v-if="d.productTypes?.length" class="design-card-chips">
              <q-chip v-for="pt in d.productTypes" :key="pt.id" dense size="xs" color="deep-purple-9" text-color="purple-2">
                {{ pt.name }}
              </q-chip>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex flex-center q-mt-lg">
        <q-pagination v-model="page" :max="totalPages" direction-links boundary-links
          color="teal-5" active-color="teal-6" dark @update:model-value="loadDesigns" />
      </div>
    </div>

    <!-- Design View Dialog -->
    <design-view-dialog v-model="showDesignDialog" :design="selectedDesign" />

    <!-- Design Config Dialog -->
    <design-config-dialog v-model="showConfigDialog" />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { designsApi, lookupApi } from 'src/api/tasks'
import DesignViewDialog from 'src/components/DesignViewDialog.vue'
import DesignConfigDialog from 'src/components/DesignConfigDialog.vue'

const $q = useQuasar()
const authStore = useAuthStore()

const loading = ref(false)
const designs = ref([])
const page = ref(1)
const totalPages = ref(0)
const showDesignDialog = ref(false)
const selectedDesign = ref(null)
const showConfigDialog = ref(false)

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

const openDesign = (d) => {
  selectedDesign.value = d
  showDesignDialog.value = true
}

onMounted(() => {
  loadDesigns()
  loadLookups()
})
</script>

<style scoped>
.designs-page {
  background: #0d0d0d;
  min-height: 100vh;
}
.design-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.design-card {
  background: #161616;
  border: 1px solid rgba(255,255,255,0.07);
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
  background: #111;
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
</style>
