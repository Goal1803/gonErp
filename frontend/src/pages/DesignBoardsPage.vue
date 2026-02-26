<template>
  <q-page class="design-boards-page">
    <!-- Header -->
    <div class="page-header row items-center no-wrap q-px-xl q-pt-lg q-pb-md">
      <q-btn flat round dense icon="arrow_back" color="grey-5" class="q-mr-sm" @click="router.push('/designs')" />
      <div>
        <div class="text-h5 text-white text-weight-light row items-center gap-2">
          <q-icon name="palette" color="teal-5" />
          Design Boards
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">Manage your POD Design boards</div>
      </div>
      <q-space />
      <q-btn v-if="authStore.isAdmin || authStore.isSuperAdmin" icon="add" label="New Board" color="teal-6" unelevated @click="openCreate" />
    </div>

    <div class="q-px-xl q-pb-xl">
      <!-- Loading -->
      <div v-if="loading" class="flex flex-center q-py-xl">
        <q-spinner color="teal-5" size="48px" />
      </div>

      <!-- Empty state -->
      <div v-else-if="boards.length === 0" class="flex flex-center column q-py-xl text-center">
        <q-icon name="dashboard_customize" size="64px" color="grey-7" />
        <div class="text-h6 text-grey-5 q-mt-md">No design boards yet</div>
        <div class="text-caption text-grey-6 q-mb-lg">Create your first POD Design board to get started</div>
        <q-btn v-if="authStore.isAdmin || authStore.isSuperAdmin" label="Create Board" color="teal-6" unelevated icon="add" @click="openCreate" />
      </div>

      <!-- Boards grid -->
      <div v-else class="row q-gutter-md">
        <div
          v-for="board in boards"
          :key="board.id"
          class="col-xs-12 col-sm-6 col-md-4 col-lg-3"
        >
          <router-link
            :to="{ name: 'designBoard', params: { boardId: board.id } }"
            class="board-link"
          >
            <q-card class="board-card" flat>
              <!-- Color accent bar -->
              <div class="board-accent" :style="{ background: board.coverColor || '#2E7D32' }" />

              <q-card-section class="q-pa-md">
                <div class="row items-start no-wrap q-mb-sm">
                  <div class="col">
                    <div class="text-white text-weight-medium" style="font-size:1rem; line-height:1.3">
                      {{ board.name }}
                    </div>
                    <div v-if="board.description" class="text-grey-5 q-mt-xs"
                      style="font-size:0.78rem; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden">
                      {{ board.description }}
                    </div>
                  </div>
                  <q-btn flat round dense icon="more_vert" color="grey-5" size="sm"
                    class="q-ml-xs" @click.prevent.stop>
                    <q-menu>
                      <q-list dense>
                        <q-item clickable v-close-popup @click="openEdit(board)">
                          <q-item-section avatar><q-icon name="edit" size="xs" /></q-item-section>
                          <q-item-section>Edit</q-item-section>
                        </q-item>
                        <q-item clickable v-close-popup @click="confirmDelete(board)" class="text-orange-4">
                          <q-item-section avatar>
                            <q-icon name="archive" size="xs" color="orange-4" />
                          </q-item-section>
                          <q-item-section>Deactivate</q-item-section>
                        </q-item>
                      </q-list>
                    </q-menu>
                  </q-btn>
                </div>

                <!-- Meta row -->
                <div class="row items-center q-gutter-sm q-mt-sm">
                  <q-chip dense color="grey-9" text-color="grey-4" size="sm" icon="view_column">
                    {{ board.columnCount || 0 }} columns
                  </q-chip>
                  <q-chip dense color="grey-9" text-color="grey-4" size="sm" icon="group">
                    {{ board.memberCount || 0 }} members
                  </q-chip>
                </div>

                <!-- Owner -->
                <div class="text-grey-6 q-mt-sm" style="font-size:0.72rem">
                  Owner: {{ board.ownerName }}
                </div>
              </q-card-section>
            </q-card>
          </router-link>
        </div>
      </div>
    </div>

    <!-- Board form dialog -->
    <board-form-dialog
      v-model="showForm"
      :board="editingBoard"
      force-board-type="POD_DESIGN"
      @saved="onSaved"
    />

    <!-- Design Config dialog -->
    <design-config-dialog v-model="showConfigDialog" />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { boardApi } from 'src/api/tasks'
import BoardFormDialog from 'src/components/BoardFormDialog.vue'
import DesignConfigDialog from 'src/components/DesignConfigDialog.vue'

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()

const boards = ref([])
const loading = ref(false)
const showForm = ref(false)
const editingBoard = ref(null)
const showConfigDialog = ref(false)

const loadBoards = async () => {
  loading.value = true
  try {
    const res = await boardApi.getAll()
    boards.value = (res.data.data || []).filter(b => b.boardType === 'POD_DESIGN')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load boards' })
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingBoard.value = null
  showForm.value = true
}

const openEdit = (board) => {
  editingBoard.value = board
  showForm.value = true
}

const onSaved = () => {
  showForm.value = false
  loadBoards()
}

const confirmDelete = (board) => {
  $q.dialog({
    title: 'Deactivate Board',
    message: `Deactivate board "${board.name}"? The board will be hidden but its design cards will remain accessible in the Designs page.`,
    cancel: true,
    persistent: true,
    color: 'orange-5'
  }).onOk(async () => {
    try {
      await boardApi.delete(board.id)
      $q.notify({ type: 'positive', message: 'Board deactivated' })
      loadBoards()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed' })
    }
  })
}

onMounted(loadBoards)
</script>

<style scoped>
.design-boards-page {
  background: var(--erp-bg);
  min-height: 100vh;
}
.board-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
  overflow: hidden;
  transition: box-shadow 0.15s, transform 0.1s;
}
.board-card:hover {
  box-shadow: 0 6px 24px rgba(0,0,0,0.5);
  transform: translateY(-2px);
}
.board-accent {
  height: 5px;
  width: 100%;
}
.board-link {
  display: block;
  text-decoration: none;
  color: inherit;
}
</style>
