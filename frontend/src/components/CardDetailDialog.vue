<template>
  <q-dialog v-model="show" persistent maximized transition-show="slide-up" transition-hide="slide-down">
    <q-card dark style="background:#111" class="column">
      <!-- Header -->
      <q-card-section class="row items-center q-py-sm" style="border-bottom:1px solid rgba(255,255,255,0.08); flex-shrink:0">
        <q-icon name="task_alt" color="teal-5" class="q-mr-sm" />
        <div class="text-white text-weight-medium">Card Detail</div>
        <q-chip dense color="grey-8" text-color="grey-4" size="sm" class="q-ml-sm">{{ detail?.stage }}</q-chip>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <!-- Conflict banner: another user updated this card while it's open -->
      <div v-if="externalUpdate" class="conflict-banner row items-center q-px-lg q-py-xs">
        <q-icon name="sync_problem" color="orange-4" size="18px" class="q-mr-sm" />
        <span style="font-size:0.82rem">
          <b class="text-orange-3">{{ externalUpdate.actorName }}</b>
          <span class="text-grey-4"> updated this card. Your edits may be outdated.</span>
        </span>
        <q-space />
        <q-btn flat dense label="Refresh" color="teal-4" size="sm" @click="refreshDetail" />
        <q-btn flat round dense icon="close" color="grey-6" size="xs"
          @click="$emit('dismiss-update')" />
      </div>

      <div class="col row no-wrap overflow-hidden" v-if="detail">
        <!-- LEFT: main content -->
        <div class="col q-pa-lg overflow-y-auto" style="max-width: 680px">
          <!-- Card name -->
          <q-input v-model="detail.name" outlined dark color="teal-5" dense class="q-mb-md"
            label="Card name" @blur="saveField('name', detail.name)" />

          <!-- Description -->
          <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
            <q-icon name="subject" />  Description
          </div>
          <q-editor v-model="detail.description" dark min-height="120px" class="q-mb-lg"
            style="background:#1a1a1a; border:1px solid rgba(255,255,255,0.1); border-radius:6px"
            :toolbar="[['bold','italic','underline','strike'],['unordered','ordered'],['link','fullscreen']]"
            @blur="saveField('description', detail.description)" />

          <!-- Attachments -->
          <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
            <q-icon name="attach_file" />  Attachments
          </div>
          <q-list dense class="q-mb-md">
            <q-item v-for="att in detail.attachments" :key="att.id" dense>
              <q-item-section avatar>
                <q-icon name="insert_drive_file" color="teal-4" />
              </q-item-section>
              <q-item-section>
                <a :href="att.url" target="_blank" class="text-teal-4" style="text-decoration:none">{{ att.name }}</a>
                <q-item-label caption class="text-grey-6">{{ att.fileType }} · {{ att.createdBy }}</q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-btn flat round dense icon="delete" color="red-4" size="xs" @click="deleteAttachment(att)" />
              </q-item-section>
            </q-item>
          </q-list>
          <q-file v-model="attachFile" outlined dark color="teal-5" dense label="Attach a file" accept="*/*"
            class="q-mb-lg" @update:model-value="uploadAttachment">
            <template #prepend><q-icon name="upload" color="grey-5" /></template>
          </q-file>

          <!-- Comments -->
          <div class="text-caption text-grey-5 q-mb-sm row items-center gap-2">
            <q-icon name="chat" />  Comments
          </div>
          <div v-for="c in detail.comments" :key="c.id" class="comment-item q-mb-sm">
            <div class="row items-center q-gutter-xs q-mb-xs">
              <q-avatar color="teal-8" text-color="white" size="24px" style="font-size:0.65rem">
                {{ (c.author.firstName || c.author.userName)[0].toUpperCase() }}
              </q-avatar>
              <span class="text-grey-4" style="font-size:0.8rem">{{ c.author.userName }}</span>
              <span class="text-grey-7" style="font-size:0.72rem">· {{ formatDate(c.createdAt) }}</span>
              <q-btn v-if="canDeleteComment(c)" flat round dense icon="close" color="grey-6" size="xs"
                @click="deleteComment(c)" />
            </div>
            <div class="text-grey-3" style="font-size:0.85rem; margin-left:32px" v-html="c.content" />
          </div>
          <div class="row q-gutter-sm q-mt-sm items-end">
            <q-input v-model="newComment" outlined dark color="teal-5" dense placeholder="Write a comment..."
              class="col" type="textarea" rows="2" />
            <q-btn icon="send" color="teal-6" unelevated dense :loading="sendingComment"
              :disable="!newComment.trim()" @click="addComment" />
          </div>

          <!-- Activity -->
          <div class="text-caption text-grey-5 q-mt-lg q-mb-sm row items-center gap-2">
            <q-icon name="history" />  Activity
          </div>
          <div v-for="act in detail.activities" :key="act.id" class="row items-center q-gutter-xs q-mb-xs">
            <q-icon name="circle" size="6px" color="teal-4" />
            <span class="text-grey-5" style="font-size:0.78rem">{{ act.action }}</span>
            <span class="text-grey-7" style="font-size:0.72rem">· {{ formatDate(act.createdAt) }}</span>
          </div>
        </div>

        <!-- RIGHT: sidebar -->
        <div class="q-pa-lg" style="width:260px; flex-shrink:0; border-left:1px solid rgba(255,255,255,0.07); overflow-y:auto">
          <!-- Status -->
          <div class="sidebar-section">
            <div class="sidebar-label"><q-icon name="flag" size="xs" /> Status</div>
            <q-select v-model="detail.status" :options="statusOptions" outlined dark dense color="teal-5"
              @update:model-value="saveField('status', detail.status)" />
          </div>

          <!-- Stage (read-only) -->
          <div class="sidebar-section">
            <div class="sidebar-label"><q-icon name="view_column" size="xs" /> Stage</div>
            <q-chip color="teal-9" text-color="teal-3" dense>{{ detail.stage }}</q-chip>
          </div>

          <!-- Labels -->
          <div class="sidebar-section">
            <div class="sidebar-label"><q-icon name="label" size="xs" /> Labels</div>
            <div class="row q-gutter-xs q-mb-xs">
              <q-chip v-for="l in detail.labels" :key="l.id" dense removable
                :style="{ background: l.color, color:'white', fontSize:'0.72rem' }"
                @remove="removeLabel(l)" />
            </div>
            <q-btn-dropdown flat dense color="grey-5" icon="add" label="Add label" size="xs">
              <q-list dark dense style="min-width:160px">
                <q-item v-for="l in availableLabels" :key="l.id" clickable v-close-popup @click="addLabel(l)">
                  <q-item-section avatar>
                    <div style="width:16px;height:16px;border-radius:3px" :style="{ background: l.color }" />
                  </q-item-section>
                  <q-item-section>{{ l.name }}</q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </div>

          <!-- Members -->
          <div class="sidebar-section">
            <div class="sidebar-label"><q-icon name="group" size="xs" /> Members</div>
            <div class="row q-gutter-xs q-mb-xs">
              <q-avatar v-for="m in detail.members" :key="m.id" color="teal-8" text-color="white" size="28px"
                style="font-size:0.7rem; cursor:pointer" :title="m.userName" @click="removeMember(m)">
                {{ (m.firstName || m.userName)[0].toUpperCase() }}
              </q-avatar>
            </div>
            <q-btn-dropdown flat dense color="grey-5" icon="person_add" label="Add member" size="xs">
              <q-list dark dense style="min-width:200px">
                <q-item v-for="u in availableMembers" :key="u.id" clickable v-close-popup @click="addMember(u)">
                  <q-item-section avatar>
                    <q-avatar color="teal-8" text-color="white" size="24px" style="font-size:0.65rem">
                      {{ (u.firstName || u.userName)[0].toUpperCase() }}
                    </q-avatar>
                  </q-item-section>
                  <q-item-section>{{ u.firstName }} {{ u.lastName }}</q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </div>

          <!-- Main image -->
          <div class="sidebar-section">
            <div class="sidebar-label"><q-icon name="image" size="xs" /> Cover Image</div>
            <div v-if="detail.mainImageUrl" class="q-mb-xs">
              <img :src="detail.mainImageUrl" style="width:100%;border-radius:6px;max-height:100px;object-fit:cover" />
              <q-btn flat dense color="red-4" icon="clear" label="Remove" size="xs" @click="saveField('mainImageUrl', null)" />
            </div>
            <q-file v-model="coverFile" outlined dark color="teal-5" dense label="Upload cover" accept="image/*"
              @update:model-value="uploadCover">
              <template #prepend><q-icon name="upload" color="grey-5" /></template>
            </q-file>
          </div>
        </div>
      </div>

      <div v-else class="col flex flex-center">
        <q-spinner color="teal-5" size="40px" />
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { cardApi } from 'src/api/tasks'
import { useAuthStore } from 'src/stores/authStore'

const props = defineProps({
  modelValue: Boolean,
  cardId: { type: Number, default: null },
  boardLabels: { type: Array, default: () => [] },
  boardMembers: { type: Array, default: () => [] },
  externalUpdate: { type: Object, default: null }  // { actorName, type }
})
const emit = defineEmits(['update:modelValue', 'updated', 'dismiss-update'])
const $q = useQuasar()
const authStore = useAuthStore()

const show = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const detail = ref(null)
const newComment = ref('')
const sendingComment = ref(false)
const attachFile = ref(null)
const coverFile = ref(null)

const statusOptions = ['OPEN', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'CANCELLED']

const availableLabels = computed(() =>
  props.boardLabels.filter(l => !detail.value?.labels?.some(dl => dl.id === l.id))
)
const availableMembers = computed(() =>
  props.boardMembers.map(m => m.user).filter(u => !detail.value?.members?.some(m => m.id === u.id))
)

watch(() => [props.modelValue, props.cardId], async ([open, id]) => {
  if (open && id) {
    detail.value = null
    try {
      const res = await cardApi.getById(id)
      detail.value = res.data.data
    } catch {
      $q.notify({ type: 'negative', message: 'Failed to load card' })
    }
  }
})

const refreshDetail = async () => {
  if (!detail.value) return
  try {
    const res = await cardApi.getById(detail.value.id)
    detail.value = res.data.data
    emit("dismiss-update")
  } catch { /* silent */ }
}

const formatDate = (d) => d ? new Date(d).toLocaleString() : ''

const saveField = async (field, value) => {
  if (!detail.value) return
  try {
    await cardApi.update(detail.value.id, { [field]: value })
    if (field !== 'description' && field !== 'name') emit('updated')
  } catch { /* silent */ }
}

const canDeleteComment = (c) =>
  authStore.isAdmin || c.author.userName === authStore.currentUser?.userName

const addComment = async () => {
  if (!newComment.value.trim()) return
  sendingComment.value = true
  try {
    const res = await cardApi.addComment(detail.value.id, newComment.value.trim())
    detail.value.comments.push(res.data.data)
    newComment.value = ''
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to add comment' })
  } finally {
    sendingComment.value = false
  }
}

const deleteComment = async (c) => {
  try {
    await cardApi.deleteComment(detail.value.id, c.id)
    detail.value.comments = detail.value.comments.filter(x => x.id !== c.id)
  } catch { $q.notify({ type: 'negative', message: 'Failed to delete comment' }) }
}

const uploadAttachment = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await cardApi.uploadAttachment(detail.value.id, fd)
    detail.value.attachments.push(res.data.data)
    attachFile.value = null
  } catch { $q.notify({ type: 'negative', message: 'Upload failed' }) }
}

const deleteAttachment = async (att) => {
  try {
    await cardApi.deleteAttachment(detail.value.id, att.id)
    detail.value.attachments = detail.value.attachments.filter(a => a.id !== att.id)
  } catch { $q.notify({ type: 'negative', message: 'Failed' }) }
}

const uploadCover = async (file) => {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  fd.append('name', file.name)
  try {
    const res = await cardApi.uploadAttachment(detail.value.id, fd)
    await cardApi.update(detail.value.id, { mainImageUrl: res.data.data.url })
    detail.value.mainImageUrl = res.data.data.url
    detail.value.attachments.push(res.data.data)
    coverFile.value = null
    emit('updated')
  } catch { $q.notify({ type: 'negative', message: 'Upload failed' }) }
}

const addLabel = async (label) => {
  try {
    await cardApi.addLabel(detail.value.id, label.id)
    detail.value.labels.push(label)
    emit('updated')
  } catch { $q.notify({ type: 'negative', message: 'Failed' }) }
}

const removeLabel = async (label) => {
  try {
    await cardApi.removeLabel(detail.value.id, label.id)
    detail.value.labels = detail.value.labels.filter(l => l.id !== label.id)
    emit('updated')
  } catch { $q.notify({ type: 'negative', message: 'Failed' }) }
}

const addMember = async (user) => {
  try {
    await cardApi.addMember(detail.value.id, user.id)
    detail.value.members.push(user)
    emit('updated')
  } catch { $q.notify({ type: 'negative', message: 'Failed' }) }
}

const removeMember = async (user) => {
  try {
    await cardApi.removeMember(detail.value.id, user.id)
    detail.value.members = detail.value.members.filter(m => m.id !== user.id)
    emit('updated')
  } catch { $q.notify({ type: 'negative', message: 'Failed' }) }
}
</script>

<style scoped>
.sidebar-section { margin-bottom: 20px; }
.sidebar-label { font-size: 0.75rem; color: #9e9e9e; margin-bottom: 6px; display: flex; align-items: center; gap: 4px; }
.comment-item { background: #1a1a1a; border-radius: 6px; padding: 10px; }
.conflict-banner { background: rgba(255,152,0,0.1); border-bottom: 1px solid rgba(255,152,0,0.25); flex-shrink: 0; }
</style>
