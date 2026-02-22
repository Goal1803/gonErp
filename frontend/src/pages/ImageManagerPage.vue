<template>
  <q-page padding>
    <!-- Page Header -->
    <div class="page-header flex items-center justify-between">
      <div>
        <div class="text-h5 text-white text-weight-light">
          <q-icon name="image" color="green-5" class="q-mr-sm" />
          Image Manager
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">Browse and manage your media library</div>
      </div>
      <q-btn
        icon="add_photo_alternate"
        label="Add Image"
        color="primary"
        unelevated
        @click="openAddDialog"
      />
    </div>

    <!-- Search & Info Bar -->
    <div class="flex items-center gap-3 q-mb-lg">
      <q-input
        v-model="search"
        placeholder="Search images by name..."
        outlined
        dense
        dark
        color="green-5"
        clearable
        debounce="400"
        style="min-width: 280px"
        @update:model-value="loadImages"
      >
        <template #prepend>
          <q-icon name="search" color="grey-5" />
        </template>
      </q-input>
      <q-btn flat icon="refresh" color="green-5" @click="loadImages" round dense>
        <q-tooltip>Refresh</q-tooltip>
      </q-btn>
      <span class="text-grey-5 text-caption q-ml-auto">
        {{ pagination.rowsNumber }} image(s) total
      </span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex flex-center q-pa-xl">
      <q-spinner-orbit color="green-5" size="3rem" />
    </div>

    <!-- Empty State -->
    <div v-else-if="images.length === 0" class="flex flex-center q-pa-xl">
      <div class="text-center text-grey-5">
        <q-icon name="image_not_supported" size="4rem" class="q-mb-md" />
        <div class="text-h6">No images found</div>
        <div class="text-caption q-mt-xs">Add your first image to get started</div>
      </div>
    </div>

    <!-- Image Grid -->
    <div v-else class="image-grid">
      <div
        v-for="image in images"
        :key="image.id"
        class="image-item"
      >
        <!-- Thumbnail -->
        <div class="thumbnail-wrapper" @click="openViewDialog(image)">
          <img
            :src="image.url + '?thumb=true'"
            :alt="image.name"
            class="image-thumbnail"
            loading="lazy"
            @error="onImageError"
          />
          <div class="thumbnail-overlay">
            <q-icon name="zoom_in" color="white" size="2rem" />
          </div>
        </div>

        <!-- Image Info & Actions -->
        <div class="image-meta">
          <div class="text-white text-caption text-weight-medium text-ellipsis q-mb-xs" :title="image.name">
            {{ image.name }}
          </div>
          <div class="flex items-center justify-between">
            <span class="text-grey-6" style="font-size: 0.65rem">
              {{ image.createdBy || 'unknown' }}
            </span>
            <div class="flex gap-1">
              <q-btn
                flat round dense icon="edit" color="green-5" size="xs"
                @click.stop="openEditDialog(image)"
              >
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn
                flat round dense icon="delete" color="red-5" size="xs"
                @click.stop="confirmDelete(image)"
              >
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="pagination.rowsNumber > pagination.rowsPerPage" class="flex flex-center q-mt-lg">
      <q-pagination
        v-model="currentPage"
        :max="Math.ceil(pagination.rowsNumber / pagination.rowsPerPage)"
        color="primary"
        active-color="orange-8"
        dark
        boundary-links
        @update:model-value="loadImages"
      />
    </div>

    <!-- Image View Dialog -->
    <ImageViewDialog
      v-model="viewDialog.show"
      :image="viewDialog.image"
    />

    <!-- Add/Edit Dialog -->
    <ImageFormDialog
      v-model="formDialog.show"
      :image="formDialog.image"
      @saved="onImageSaved"
    />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { imageApi } from 'src/api/images'
import ImageViewDialog from 'src/components/ImageViewDialog.vue'
import ImageFormDialog from 'src/components/ImageFormDialog.vue'

const $q = useQuasar()

const images = ref([])
const loading = ref(false)
const search = ref('')
const currentPage = ref(1)

const pagination = ref({
  rowsNumber: 0,
  rowsPerPage: 24
})

const viewDialog = ref({ show: false, image: null })
const formDialog = ref({ show: false, image: null })

const loadImages = async () => {
  loading.value = true
  try {
    const res = await imageApi.getAll({
      search: search.value || undefined,
      page: currentPage.value - 1,
      size: pagination.value.rowsPerPage,
      sortBy: 'id',
      sortDir: 'desc'
    })
    const data = res.data.data
    images.value = data.content
    pagination.value.rowsNumber = data.totalElements
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load images' })
  } finally {
    loading.value = false
  }
}

const openViewDialog = (image) => {
  viewDialog.value = { show: true, image }
}

const openAddDialog = () => {
  formDialog.value = { show: true, image: null }
}

const openEditDialog = (image) => {
  formDialog.value = { show: true, image: { ...image } }
}

const confirmDelete = (image) => {
  $q.dialog({
    title: 'Delete Image',
    message: `Delete "<strong>${image.name}</strong>"? This cannot be undone.`,
    html: true,
    cancel: true,
    color: 'negative',
    dark: true
  }).onOk(async () => {
    try {
      await imageApi.delete(image.id)
      $q.notify({ type: 'positive', message: 'Image deleted' })
      loadImages()
    } catch {
      $q.notify({ type: 'negative', message: 'Failed to delete image' })
    }
  })
}

const onImageSaved = () => {
  formDialog.value.show = false
  loadImages()
}

const onImageError = (e) => {
  e.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMzAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iIzFhMWExYSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmaWxsPSIjNDQ0IiBmb250LXNpemU9IjE0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+SW1hZ2UgTm90IEZvdW5kPC90ZXh0Pjwvc3ZnPg=='
}

onMounted(loadImages)
</script>

<style scoped>
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.image-item {
  background: linear-gradient(135deg, #1a1a1a, #1e1e1e);
  border: 1px solid rgba(46, 125, 50, 0.2);
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.image-item:hover {
  border-color: rgba(245, 124, 0, 0.4);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.thumbnail-wrapper {
  position: relative;
  width: 100%;
  height: 300px;
  cursor: pointer;
  overflow: hidden;
}

.thumbnail-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.thumbnail-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  opacity: 0;
}

.thumbnail-wrapper:hover img {
  transform: scale(1.05);
}

.thumbnail-wrapper:hover .thumbnail-overlay {
  background: rgba(0, 0, 0, 0.4);
  opacity: 1;
}

.image-meta {
  padding: 10px 12px;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
