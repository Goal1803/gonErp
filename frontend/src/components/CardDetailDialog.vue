<template>
  <q-dialog
    v-model="show"
    persistent
    maximized
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card dark style="background: #111" class="column">
      <!-- Header -->
      <q-card-section
        class="row items-center q-py-sm"
        style="
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
          flex-shrink: 0;
        "
      >
        <q-icon name="task_alt" color="teal-5" class="q-mr-sm" />
        <div class="text-white text-weight-medium">Card Detail</div>
        <q-chip
          dense
          color="grey-8"
          text-color="grey-4"
          size="sm"
          class="q-ml-sm"
          >{{ detail?.stage }}</q-chip
        >
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <!-- Conflict banner: another user updated this card while it's open -->
      <div
        v-if="externalUpdate"
        class="conflict-banner row items-center q-px-lg q-py-xs"
      >
        <q-icon
          name="sync_problem"
          color="orange-4"
          size="18px"
          class="q-mr-sm"
        />
        <span style="font-size: 0.82rem">
          <b class="text-orange-3">{{ externalUpdate.actorName }}</b>
          <span class="text-grey-4">
            updated this card. Your edits may be outdated.</span
          >
        </span>
        <q-space />
        <q-btn
          flat
          dense
          label="Refresh"
          color="teal-4"
          size="sm"
          @click="refreshDetail"
        />
        <q-btn
          flat
          round
          dense
          icon="close"
          color="grey-6"
          size="xs"
          @click="$emit('dismiss-update')"
        />
      </div>

      <div class="col row no-wrap overflow-hidden" v-if="detail">
        <!-- LEFT: main content -->
        <div class="col q-pa-lg overflow-y-auto" style="max-width: 680px">
          <!-- Card name -->
          <q-input
            v-model="detail.name"
            outlined
            dark
            color="teal-5"
            dense
            class="q-mb-md"
            label="Card name"
            @blur="saveField('name', detail.name)"
          />

          <!-- Description -->
          <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
            <q-icon name="subject" /> Description
          </div>
          <q-editor
            v-model="detail.description"
            dark
            min-height="120px"
            class="q-mb-lg"
            style="
              background: #1a1a1a;
              border: 1px solid rgba(255, 255, 255, 0.1);
              border-radius: 6px;
            "
            :toolbar="[
              ['bold', 'italic', 'underline', 'strike'],
              ['unordered', 'ordered'],
              ['link', 'fullscreen'],
            ]"
            @blur="saveField('description', detail.description)"
          />

          <!-- Attachments -->
          <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
            <q-icon name="attach_file" /> Attachments
          </div>
          <q-list dense class="q-mb-md">
            <q-item v-for="att in detail.attachments" :key="att.id" dense>
              <q-item-section avatar>
                <q-icon name="insert_drive_file" color="teal-4" />
              </q-item-section>
              <q-item-section>
                <a
                  :href="att.url"
                  target="_blank"
                  class="text-teal-4"
                  style="text-decoration: none"
                  >{{ att.name }}</a
                >
                <q-item-label caption class="text-grey-6"
                  >{{ att.fileType }} · {{ att.createdBy }}</q-item-label
                >
              </q-item-section>
              <q-item-section side>
                <q-btn
                  flat
                  round
                  dense
                  icon="delete"
                  color="red-4"
                  size="xs"
                  @click="deleteAttachment(att)"
                />
              </q-item-section>
            </q-item>
          </q-list>
          <q-file
            v-model="attachFile"
            outlined
            dark
            color="teal-5"
            dense
            label="Attach a file"
            accept="*/*"
            class="q-mb-lg"
            @update:model-value="uploadAttachment"
          >
            <template #prepend
              ><q-icon name="upload" color="grey-5"
            /></template>
          </q-file>

          <!-- Comments -->
          <div class="text-caption text-grey-5 q-mb-sm row items-center gap-2">
            <q-icon name="chat" /> Comments
          </div>
          <div
            v-for="c in detail.comments"
            :key="c.id"
            class="comment-item q-mb-sm"
          >
            <div class="row items-center q-gutter-xs q-mb-xs">
              <q-avatar
                color="teal-8"
                text-color="white"
                size="24px"
                style="font-size: 0.65rem"
              >
                {{ (c.author.firstName || c.author.userName)[0].toUpperCase() }}
              </q-avatar>
              <span class="text-grey-4" style="font-size: 0.8rem">{{
                c.author.userName
              }}</span>
              <span class="text-grey-7" style="font-size: 0.72rem"
                >· {{ formatDate(c.createdAt) }}</span
              >
              <q-btn
                v-if="canDeleteComment(c)"
                flat
                round
                dense
                icon="close"
                color="grey-6"
                size="xs"
                @click="deleteComment(c)"
              />
            </div>
            <div
              class="text-grey-3"
              style="font-size: 0.85rem; margin-left: 32px"
              v-html="c.content"
            />
          </div>
          <div class="row q-gutter-sm q-mt-sm items-end">
            <q-input
              v-model="newComment"
              outlined
              dark
              color="teal-5"
              dense
              placeholder="Write a comment..."
              class="col"
              type="textarea"
              rows="2"
            />
            <q-btn
              icon="send"
              color="teal-6"
              unelevated
              dense
              :loading="sendingComment"
              :disable="!newComment.trim()"
              @click="addComment"
            />
          </div>

          <!-- Activity -->
          <div
            class="text-caption text-grey-5 q-mt-lg q-mb-sm row items-center gap-2"
          >
            <q-icon name="history" /> Activity
          </div>
          <div
            v-for="act in detail.activities"
            :key="act.id"
            class="row items-center q-gutter-xs q-mb-xs"
          >
            <q-icon name="circle" size="6px" color="teal-4" />
            <span class="text-grey-5" style="font-size: 0.78rem">{{
              act.action
            }}</span>
            <span class="text-grey-7" style="font-size: 0.72rem"
              >· {{ formatDate(act.createdAt) }}</span
            >
          </div>
        </div>

        <!-- RIGHT: sidebar -->
        <div
          class="q-pa-lg"
          style="
            width: 260px;
            flex-shrink: 0;
            border-left: 1px solid rgba(255, 255, 255, 0.07);
            overflow-y: auto;
          "
        >
          <!-- Status -->
          <div class="sidebar-section">
            <div class="sidebar-label">
              <q-icon name="flag" size="xs" /> Status
            </div>
            <q-select
              v-model="detail.status"
              :options="statusOptions"
              outlined
              dark
              dense
              color="teal-5"
              @update:model-value="saveField('status', detail.status)"
            />
          </div>

          <!-- Stage (read-only) -->
          <div class="sidebar-section">
            <div class="sidebar-label">
              <q-icon name="view_column" size="xs" /> Stage
            </div>
            <q-chip color="teal-9" text-color="teal-3" dense>{{
              detail.stage
            }}</q-chip>
          </div>

          <!-- Labels -->
          <div class="sidebar-section">
            <!-- Manage / create labels popup -->
            <q-btn flat dense color="grey-5" icon="label" label="Labels">
              <q-menu
                persistent
                style="
                  min-width: 270px;
                  background: #1e1e1e;
                  border: 1px solid rgba(255, 255, 255, 0.1);
                "
              >
                <div class="q-pa-sm">
                  <!-- Header row -->
                  <div class="row items-center q-mb-sm">
                    <span
                      class="text-grey-4"
                      style="
                        font-size: 0.72rem;
                        font-weight: 600;
                        letter-spacing: 0.05em;
                      "
                      >BOARD LABELS</span
                    >
                    <q-space />
                    <q-btn
                      flat
                      round
                      dense
                      icon="close"
                      color="grey-5"
                      size="xs"
                      v-close-popup
                    />
                  </div>

                  <!-- Board labels: click to toggle attach/detach -->
                  <div
                    v-if="!allBoardLabels.length"
                    class="text-grey-6 text-caption q-mb-sm"
                  >
                    No labels yet — create one below.
                  </div>
                  <div
                    v-for="l in allBoardLabels"
                    :key="l.id"
                    class="row items-center q-mb-xs label-row"
                    style="
                      cursor: pointer;
                      border-radius: 6px;
                      padding: 2px 4px;
                    "
                    @click="toggleLabel(l)"
                  >
                    <q-chip
                      dense
                      :label="l.name"
                      :style="{
                        background: l.color,
                        color: l.textColor || '#fff',
                        minWidth: '110px',
                        fontSize: '0.78rem',
                      }"
                    />
                    <q-icon
                      v-if="isAttached(l)"
                      name="check"
                      color="teal-4"
                      size="16px"
                      class="q-ml-xs"
                    />
                    <q-space />
                    <q-btn
                      flat
                      round
                      dense
                      icon="delete"
                      color="grey-7"
                      size="xs"
                      @click.stop="deleteBoardLabel(l)"
                    />
                  </div>

                  <q-separator dark class="q-my-sm" />

                  <!-- Create new label form -->
                  <div
                    class="text-grey-5"
                    style="
                      font-size: 0.72rem;
                      font-weight: 600;
                      letter-spacing: 0.05em;
                      margin-bottom: 6px;
                    "
                  >
                    CREATE LABEL
                  </div>
                  <q-input
                    v-model="newLabel.name"
                    outlined
                    dark
                    color="teal-5"
                    dense
                    placeholder="Label name"
                    class="q-mb-sm"
                  />

                  <!-- 20 color preset swatches -->
                  <div
                    class="text-grey-6"
                    style="font-size: 0.7rem; margin-bottom: 4px"
                  >
                    Color presets
                  </div>
                  <div class="label-preset-grid q-mb-xs">
                    <div
                      v-for="(p, i) in allPresets"
                      :key="i"
                      class="label-preset-swatch"
                      :style="{ background: p.bg }"
                      :class="{
                        'preset-selected':
                          newLabel.bg === p.bg && newLabel.text === p.text,
                      }"
                      @click="
                        newLabel.bg = p.bg;
                        newLabel.text = p.text;
                      "
                      :title="`BG: ${p.bg}  Text: ${p.text}`"
                    />
                  </div>

                  <!-- Add custom color set -->
                  <div v-if="!showCustomPicker" class="q-mb-sm">
                    <q-btn
                      flat
                      dense
                      icon="add"
                      label="Add custom preset"
                      color="grey-5"
                      size="xs"
                      @click="showCustomPicker = true"
                    />
                  </div>
                  <div v-else class="q-mb-sm">
                    <div class="row items-center q-gutter-sm">
                      <div>
                        <div
                          class="text-grey-6"
                          style="font-size: 0.68rem; margin-bottom: 2px"
                        >
                          Background
                        </div>
                        <input
                          type="color"
                          v-model="customPreset.bg"
                          style="
                            width: 38px;
                            height: 26px;
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            border-radius: 4px;
                            cursor: pointer;
                            padding: 2px;
                            background: #2a2a2a;
                          "
                        />
                      </div>
                      <div>
                        <div
                          class="text-grey-6"
                          style="font-size: 0.68rem; margin-bottom: 2px"
                        >
                          Text
                        </div>
                        <input
                          type="color"
                          v-model="customPreset.text"
                          style="
                            width: 38px;
                            height: 26px;
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            border-radius: 4px;
                            cursor: pointer;
                            padding: 2px;
                            background: #2a2a2a;
                          "
                        />
                      </div>
                      <q-btn
                        flat
                        dense
                        icon="check"
                        color="teal-4"
                        size="sm"
                        style="margin-top: 14px"
                        title="Save to presets"
                        @click="saveCustomPreset"
                      />
                      <q-btn
                        flat
                        dense
                        icon="close"
                        color="grey-5"
                        size="sm"
                        style="margin-top: 14px"
                        @click="showCustomPicker = false"
                      />
                    </div>
                  </div>

                  <!-- Preview + create -->
                  <div class="row items-center q-gutter-xs q-mb-sm">
                    <span class="text-grey-6" style="font-size: 0.7rem"
                      >Preview:</span
                    >
                    <q-chip
                      dense
                      :label="newLabel.name || 'Label preview'"
                      :style="{
                        background: newLabel.bg,
                        color: newLabel.text,
                        fontSize: '0.78rem',
                      }"
                    />
                  </div>
                  <q-btn
                    label="Create label"
                    color="teal-6"
                    unelevated
                    dense
                    size="sm"
                    class="full-width"
                    :disable="!newLabel.name.trim()"
                    :loading="creatingLabel"
                    @click="createLabel"
                  />
                </div>
              </q-menu>
            </q-btn>
            <!-- Attached label chips -->
            <div class="row q-gutter-xs q-mb-xs" style="flex-wrap: wrap">
              <q-chip
                v-for="l in detail.labels"
                :key="l.id"
                dense
                removable
                :style="{
                  background: l.color,
                  color: l.textColor || '#fff',
                  fontSize: '0.72rem',
                }"
                @remove="removeLabel(l)"
              >
                {{ l.name }}
              </q-chip>
            </div>
          </div>

          <!-- Types -->
          <div class="sidebar-section">
            <!-- Manage / create types popup -->
            <q-btn flat dense color="grey-5" icon="category" label="Types">
              <q-menu
                persistent
                style="
                  min-width: 270px;
                  background: #1e1e1e;
                  border: 1px solid rgba(255, 255, 255, 0.1);
                "
              >
                <div class="q-pa-sm">
                  <!-- Header row -->
                  <div class="row items-center q-mb-sm">
                    <span
                      class="text-grey-4"
                      style="
                        font-size: 0.72rem;
                        font-weight: 600;
                        letter-spacing: 0.05em;
                      "
                      >BOARD TYPES</span
                    >
                    <q-space />
                    <q-btn
                      flat
                      round
                      dense
                      icon="close"
                      color="grey-5"
                      size="xs"
                      v-close-popup
                    />
                  </div>

                  <!-- Board types: click to toggle attach/detach -->
                  <div
                    v-if="!allBoardTypes.length"
                    class="text-grey-6 text-caption q-mb-sm"
                  >
                    No types yet — create one below.
                  </div>
                  <div
                    v-for="t in allBoardTypes"
                    :key="t.id"
                    class="row items-center q-mb-xs label-row"
                    style="
                      cursor: pointer;
                      border-radius: 6px;
                      padding: 2px 4px;
                    "
                    @click="toggleType(t)"
                  >
                    <q-chip
                      dense
                      :label="t.name"
                      :style="{
                        background: t.color,
                        color: t.textColor || '#fff',
                        minWidth: '110px',
                        fontSize: '0.78rem',
                      }"
                    />
                    <q-icon
                      v-if="isTypeAttached(t)"
                      name="check"
                      color="teal-4"
                      size="16px"
                      class="q-ml-xs"
                    />
                    <q-space />
                    <q-btn
                      flat
                      round
                      dense
                      icon="delete"
                      color="grey-7"
                      size="xs"
                      @click.stop="deleteBoardType(t)"
                    />
                  </div>

                  <q-separator dark class="q-my-sm" />

                  <!-- Create new type form -->
                  <div
                    class="text-grey-5"
                    style="
                      font-size: 0.72rem;
                      font-weight: 600;
                      letter-spacing: 0.05em;
                      margin-bottom: 6px;
                    "
                  >
                    CREATE TYPE
                  </div>
                  <q-input
                    v-model="newType.name"
                    outlined
                    dark
                    color="teal-5"
                    dense
                    placeholder="Type name"
                    class="q-mb-sm"
                  />

                  <!-- 20 color preset swatches -->
                  <div
                    class="text-grey-6"
                    style="font-size: 0.7rem; margin-bottom: 4px"
                  >
                    Color presets
                  </div>
                  <div class="label-preset-grid q-mb-xs">
                    <div
                      v-for="(p, i) in allPresets"
                      :key="'type-' + i"
                      class="label-preset-swatch"
                      :style="{ background: p.bg }"
                      :class="{
                        'preset-selected':
                          newType.bg === p.bg && newType.text === p.text,
                      }"
                      @click="
                        newType.bg = p.bg;
                        newType.text = p.text;
                      "
                      :title="`BG: ${p.bg}  Text: ${p.text}`"
                    />
                  </div>

                  <!-- Preview + create -->
                  <div class="row items-center q-gutter-xs q-mb-sm">
                    <span class="text-grey-6" style="font-size: 0.7rem"
                      >Preview:</span
                    >
                    <q-chip
                      dense
                      :label="newType.name || 'Type preview'"
                      :style="{
                        background: newType.bg,
                        color: newType.text,
                        fontSize: '0.78rem',
                      }"
                    />
                  </div>
                  <q-btn
                    label="Create type"
                    color="teal-6"
                    unelevated
                    dense
                    size="sm"
                    class="full-width"
                    :disable="!newType.name.trim()"
                    :loading="creatingType"
                    @click="createType"
                  />
                </div>
              </q-menu>
            </q-btn>
            <!-- Attached type chips -->
            <div class="row q-gutter-xs q-mb-xs" style="flex-wrap: wrap">
              <q-chip
                v-for="t in detail.types"
                :key="t.id"
                dense
                removable
                :style="{
                  background: t.color,
                  color: t.textColor || '#fff',
                  fontSize: '0.72rem',
                }"
                @remove="removeType(t)"
              >
                {{ t.name }}
              </q-chip>
            </div>
          </div>

          <!-- Members -->
          <div class="sidebar-section">
            <div class="sidebar-label">
              <q-icon name="group" size="xs" /> Members
            </div>
            <div class="row q-gutter-xs q-mb-xs">
              <q-avatar
                v-for="m in detail.members"
                :key="m.id"
                color="teal-8"
                text-color="white"
                size="28px"
                style="font-size: 0.7rem; cursor: pointer"
                :title="m.userName"
                @click="removeMember(m)"
              >
                {{ (m.firstName || m.userName)[0].toUpperCase() }}
              </q-avatar>
            </div>
            <q-btn-dropdown
              flat
              dense
              color="grey-5"
              icon="person_add"
              label="Add member"
              size="xs"
            >
              <q-list dark dense style="min-width: 200px">
                <q-item
                  v-for="u in availableMembers"
                  :key="u.id"
                  clickable
                  v-close-popup
                  @click="addMember(u)"
                >
                  <q-item-section avatar>
                    <q-avatar
                      color="teal-8"
                      text-color="white"
                      size="24px"
                      style="font-size: 0.65rem"
                    >
                      {{ (u.firstName || u.userName)[0].toUpperCase() }}
                    </q-avatar>
                  </q-item-section>
                  <q-item-section
                    >{{ u.firstName }} {{ u.lastName }}</q-item-section
                  >
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </div>

          <!-- Main image -->
          <div class="sidebar-section">
            <div class="sidebar-label">
              <q-icon name="image" size="xs" /> Cover Image
            </div>
            <div v-if="detail.mainImageUrl" class="q-mb-xs">
              <img
                :src="detail.mainImageUrl"
                style="
                  width: 100%;
                  border-radius: 6px;
                  max-height: 100px;
                  object-fit: cover;
                "
              />
              <q-btn
                flat
                dense
                color="red-4"
                icon="clear"
                label="Remove"
                size="xs"
                @click="saveField('mainImageUrl', null)"
              />
            </div>
            <q-file
              v-model="coverFile"
              outlined
              dark
              color="teal-5"
              dense
              label="Upload cover"
              accept="image/*"
              @update:model-value="uploadCover"
            >
              <template #prepend
                ><q-icon name="upload" color="grey-5"
              /></template>
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
import { ref, computed, watch } from "vue";
import { useQuasar } from "quasar";
import { cardApi, boardApi } from "src/api/tasks";
import { useAuthStore } from "src/stores/authStore";

// ─── Label color presets ──────────────────────────────────────────────────────
const LABEL_PRESETS = [
  { bg: "#e53935", text: "#ffffff" },
  { bg: "#d81b60", text: "#ffffff" },
  { bg: "#8e24aa", text: "#ffffff" },
  { bg: "#5e35b1", text: "#ffffff" },
  { bg: "#3949ab", text: "#ffffff" },
  { bg: "#1e88e5", text: "#ffffff" },
  { bg: "#039be5", text: "#ffffff" },
  { bg: "#00acc1", text: "#ffffff" },
  { bg: "#00897b", text: "#ffffff" },
  { bg: "#43a047", text: "#ffffff" },
  { bg: "#7cb342", text: "#ffffff" },
  { bg: "#c0ca33", text: "#212121" },
  { bg: "#fdd835", text: "#212121" },
  { bg: "#ffb300", text: "#212121" },
  { bg: "#fb8c00", text: "#ffffff" },
  { bg: "#f4511e", text: "#ffffff" },
  { bg: "#6d4c41", text: "#ffffff" },
  { bg: "#546e7a", text: "#ffffff" },
  { bg: "#455a64", text: "#ffffff" },
  { bg: "#2e7d32", text: "#ffffff" },
];
const CUSTOM_PRESETS_KEY = "kanban_label_custom_presets";
const loadCustomPresets = () => {
  try {
    return JSON.parse(localStorage.getItem(CUSTOM_PRESETS_KEY) || "[]");
  } catch {
    return [];
  }
};

const props = defineProps({
  modelValue: Boolean,
  cardId: { type: Number, default: null },
  boardId: { type: Number, default: null },
  boardLabels: { type: Array, default: () => [] },
  boardTypes: { type: Array, default: () => [] },
  boardMembers: { type: Array, default: () => [] },
  externalUpdate: { type: Object, default: null }, // { actorName, type }
});
const emit = defineEmits(["update:modelValue", "updated", "dismiss-update"]);
const $q = useQuasar();
const authStore = useAuthStore();

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});
const detail = ref(null);
const newComment = ref("");
const sendingComment = ref(false);
const attachFile = ref(null);
const coverFile = ref(null);

const statusOptions = ["OPEN", "IN_PROGRESS", "DONE", "BLOCKED", "CANCELLED"];

// ─── Label management state ───────────────────────────────────────────────────
// Local copy of board labels so we can push newly created ones immediately
const allBoardLabels = ref([]);
watch(
  () => props.boardLabels,
  (v) => {
    allBoardLabels.value = [...v];
  },
  { immediate: true },
);

const allPresets = ref([...LABEL_PRESETS, ...loadCustomPresets()]);
const newLabel = ref({
  name: "",
  bg: LABEL_PRESETS[0].bg,
  text: LABEL_PRESETS[0].text,
});
const creatingLabel = ref(false);
const showCustomPicker = ref(false);
const customPreset = ref({ bg: "#3949ab", text: "#ffffff" });

const isAttached = (label) =>
  detail.value?.labels?.some((l) => l.id === label.id);

const toggleLabel = async (label) => {
  if (isAttached(label)) await removeLabel(label);
  else await addLabel(label);
};

const createLabel = async () => {
  if (!newLabel.value.name.trim() || !props.boardId) return;
  creatingLabel.value = true;
  try {
    const res = await boardApi.createLabel(props.boardId, {
      name: newLabel.value.name.trim(),
      color: newLabel.value.bg,
      textColor: newLabel.value.text,
    });
    allBoardLabels.value.push(res.data.data);
    newLabel.value.name = "";
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to create label" });
  } finally {
    creatingLabel.value = false;
  }
};

const deleteBoardLabel = async (label) => {
  try {
    await boardApi.deleteLabel(label.id);
    allBoardLabels.value = allBoardLabels.value.filter(
      (l) => l.id !== label.id,
    );
    if (detail.value)
      detail.value.labels = detail.value.labels.filter(
        (l) => l.id !== label.id,
      );
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to delete label" });
  }
};

const saveCustomPreset = () => {
  const preset = { bg: customPreset.value.bg, text: customPreset.value.text };
  allPresets.value.push(preset);
  const saved = loadCustomPresets();
  saved.push(preset);
  localStorage.setItem(CUSTOM_PRESETS_KEY, JSON.stringify(saved));
  newLabel.value.bg = preset.bg;
  newLabel.value.text = preset.text;
  showCustomPicker.value = false;
};
// ─────────────────────────────────────────────────────────────────────────────

// ─── Type management state ────────────────────────────────────────────────────
const allBoardTypes = ref([]);
watch(
  () => props.boardTypes,
  (v) => {
    allBoardTypes.value = [...v];
  },
  { immediate: true },
);

const newType = ref({
  name: "",
  bg: LABEL_PRESETS[4].bg,
  text: LABEL_PRESETS[4].text,
});
const creatingType = ref(false);

const isTypeAttached = (type) =>
  detail.value?.types?.some((t) => t.id === type.id);

const toggleType = async (type) => {
  if (isTypeAttached(type)) await removeType(type);
  else await addType(type);
};

const createType = async () => {
  if (!newType.value.name.trim() || !props.boardId) return;
  creatingType.value = true;
  try {
    const res = await boardApi.createType(props.boardId, {
      name: newType.value.name.trim(),
      color: newType.value.bg,
      textColor: newType.value.text,
    });
    allBoardTypes.value.push(res.data.data);
    newType.value.name = "";
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to create type" });
  } finally {
    creatingType.value = false;
  }
};

const deleteBoardType = async (type) => {
  try {
    await boardApi.deleteType(type.id);
    allBoardTypes.value = allBoardTypes.value.filter(
      (t) => t.id !== type.id,
    );
    if (detail.value)
      detail.value.types = detail.value.types.filter(
        (t) => t.id !== type.id,
      );
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to delete type" });
  }
};
// ─────────────────────────────────────────────────────────────────────────────

const availableMembers = computed(() =>
  props.boardMembers
    .map((m) => m.user)
    .filter((u) => !detail.value?.members?.some((m) => m.id === u.id)),
);

watch(
  () => [props.modelValue, props.cardId],
  async ([open, id]) => {
    if (open && id) {
      detail.value = null;
      try {
        const res = await cardApi.getById(id);
        detail.value = res.data.data;
      } catch {
        $q.notify({ type: "negative", message: "Failed to load card" });
      }
    }
  },
);

const refreshDetail = async () => {
  if (!detail.value) return;
  try {
    const res = await cardApi.getById(detail.value.id);
    detail.value = res.data.data;
    emit("dismiss-update");
  } catch {
    /* silent */
  }
};

const formatDate = (d) => (d ? new Date(d).toLocaleString() : "");

const saveField = async (field, value) => {
  if (!detail.value) return;
  try {
    await cardApi.update(detail.value.id, { [field]: value });
    if (field !== "description" && field !== "name") emit("updated");
  } catch {
    /* silent */
  }
};

const canDeleteComment = (c) =>
  authStore.isAdmin || c.author.userName === authStore.currentUser?.userName;

const addComment = async () => {
  if (!newComment.value.trim()) return;
  sendingComment.value = true;
  try {
    const res = await cardApi.addComment(
      detail.value.id,
      newComment.value.trim(),
    );
    detail.value.comments.push(res.data.data);
    newComment.value = "";
  } catch {
    $q.notify({ type: "negative", message: "Failed to add comment" });
  } finally {
    sendingComment.value = false;
  }
};

const deleteComment = async (c) => {
  try {
    await cardApi.deleteComment(detail.value.id, c.id);
    detail.value.comments = detail.value.comments.filter((x) => x.id !== c.id);
  } catch {
    $q.notify({ type: "negative", message: "Failed to delete comment" });
  }
};

const uploadAttachment = async (file) => {
  if (!file) return;
  const fd = new FormData();
  fd.append("file", file);
  try {
    const res = await cardApi.uploadAttachment(detail.value.id, fd);
    detail.value.attachments.push(res.data.data);
    attachFile.value = null;
  } catch {
    $q.notify({ type: "negative", message: "Upload failed" });
  }
};

const deleteAttachment = async (att) => {
  try {
    await cardApi.deleteAttachment(detail.value.id, att.id);
    detail.value.attachments = detail.value.attachments.filter(
      (a) => a.id !== att.id,
    );
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const uploadCover = async (file) => {
  if (!file) return;
  const fd = new FormData();
  fd.append("file", file);
  fd.append("name", file.name);
  try {
    const res = await cardApi.uploadAttachment(detail.value.id, fd);
    await cardApi.update(detail.value.id, { mainImageUrl: res.data.data.url });
    detail.value.mainImageUrl = res.data.data.url;
    detail.value.attachments.push(res.data.data);
    coverFile.value = null;
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Upload failed" });
  }
};

const addLabel = async (label) => {
  try {
    await cardApi.addLabel(detail.value.id, label.id);
    detail.value.labels.push(label);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const removeLabel = async (label) => {
  try {
    await cardApi.removeLabel(detail.value.id, label.id);
    detail.value.labels = detail.value.labels.filter((l) => l.id !== label.id);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const addType = async (type) => {
  try {
    await cardApi.addType(detail.value.id, type.id);
    detail.value.types.push(type);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const removeType = async (type) => {
  try {
    await cardApi.removeType(detail.value.id, type.id);
    detail.value.types = detail.value.types.filter((t) => t.id !== type.id);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const addMember = async (user) => {
  try {
    await cardApi.addMember(detail.value.id, user.id);
    detail.value.members.push(user);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const removeMember = async (user) => {
  try {
    await cardApi.removeMember(detail.value.id, user.id);
    detail.value.members = detail.value.members.filter((m) => m.id !== user.id);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};
</script>

<style scoped>
.sidebar-section {
  margin-bottom: 20px;
}
.sidebar-label {
  font-size: 0.75rem;
  color: #9e9e9e;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.comment-item {
  background: #1a1a1a;
  border-radius: 6px;
  padding: 10px;
}
.conflict-banner {
  background: rgba(255, 152, 0, 0.1);
  border-bottom: 1px solid rgba(255, 152, 0, 0.25);
  flex-shrink: 0;
}

/* Label color preset grid */
.label-preset-grid {
  display: grid;
  grid-template-columns: repeat(10, 20px);
  gap: 4px;
}
.label-preset-swatch {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition:
    transform 0.1s,
    border-color 0.1s;
}
.label-preset-swatch:hover {
  transform: scale(1.2);
}
.label-preset-swatch.preset-selected {
  border-color: #fff;
  transform: scale(1.1);
}

/* Label row hover */
.label-row:hover {
  background: rgba(255, 255, 255, 0.05);
}
</style>
