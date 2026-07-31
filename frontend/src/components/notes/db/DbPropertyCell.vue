<template>
  <!-- Checkbox toggles in place; everything else opens an editor on click. -->
  <div v-if="property.type === 'CHECKBOX'" class="db-cell db-cell-checkbox">
    <q-checkbox
      :model-value="value === 'true'"
      dense size="xs" color="green-6"
      :disable="readonly"
      @update:model-value="v => commit(String(v))"
    />
  </div>

  <div v-else class="db-cell" :class="{ 'is-readonly': readonly }" @click="open">
    <!-- ── read mode ── -->
    <template v-if="!editing">
      <template v-if="property.type === 'SELECT'">
        <span v-if="value" class="db-chip" :class="`db-chip-${optionColor(property, value)}`">
          {{ optionLabel(property, value) }}
        </span>
      </template>

      <template v-else-if="property.type === 'MULTI_SELECT'">
        <span v-for="id in splitList(value)" :key="id"
          class="db-chip" :class="`db-chip-${optionColor(property, id)}`">
          {{ optionLabel(property, id) }}
        </span>
      </template>

      <template v-else-if="property.type === 'PERSON'">
        <span v-for="uid in splitList(value)" :key="uid" class="db-chip db-chip-blue">
          {{ userName(uid) }}
        </span>
      </template>

      <template v-else-if="property.type === 'DATE'">
        <span v-if="value" class="db-cell-text">{{ formatDate(value) }}</span>
      </template>

      <template v-else-if="property.type === 'NUMBER'">
        <span v-if="value" class="db-cell-text db-cell-number">
          {{ formatNumber(value, property.numberFormat) }}
        </span>
      </template>

      <template v-else-if="property.type === 'URL'">
        <a v-if="value" :href="value" target="_blank" rel="noopener noreferrer"
          class="db-cell-text db-cell-link" @click.stop>{{ value }}</a>
      </template>

      <span v-else class="db-cell-text">{{ value }}</span>

      <span v-if="!hasValue" class="db-cell-empty">—</span>
    </template>

    <!-- ── edit mode ── -->
    <template v-else>
      <input
        v-if="['TEXT', 'URL', 'NUMBER'].includes(property.type)"
        ref="inputRef"
        v-model="draft"
        class="db-cell-input"
        :type="property.type === 'NUMBER' ? 'number' : 'text'"
        :step="property.type === 'NUMBER' ? 'any' : undefined"
        @blur="commit(draft)"
        @keydown.enter.prevent="commit(draft)"
        @keydown.esc="editing = false"
      />

      <input
        v-else-if="property.type === 'DATE'"
        ref="inputRef"
        v-model="draft"
        type="date"
        class="db-cell-input"
        @blur="commit(draft)"
        @keydown.enter.prevent="commit(draft)"
        @keydown.esc="editing = false"
      />
    </template>

    <!-- Option / person pickers live in a menu rather than an inline input. -->
    <q-menu
      v-if="editing && ['SELECT', 'MULTI_SELECT', 'PERSON'].includes(property.type)"
      v-model="editing" auto-close-disabled fit @hide="editing = false"
    >
      <q-list dense style="min-width: 190px; max-height: 280px" class="scroll">
        <q-item v-if="property.type === 'SELECT'" clickable v-close-popup @click="commit('')">
          <q-item-section class="text-grey-6">Xoá lựa chọn</q-item-section>
        </q-item>

        <template v-if="property.type === 'PERSON'">
          <q-item v-for="u in users" :key="u.id" clickable @click="toggleInList(String(u.id))">
            <q-item-section avatar style="min-width: 30px">
              <q-icon :name="splitList(value).includes(String(u.id)) ? 'check_box' : 'check_box_outline_blank'"
                size="17px" />
            </q-item-section>
            <q-item-section>{{ fullName(u) }}</q-item-section>
          </q-item>
        </template>

        <template v-else>
          <q-item v-for="opt in parseOptions(property.options)" :key="opt.id" clickable
            @click="property.type === 'SELECT' ? commit(opt.id) : toggleInList(opt.id)">
            <q-item-section avatar v-if="property.type === 'MULTI_SELECT'" style="min-width: 30px">
              <q-icon :name="splitList(value).includes(opt.id) ? 'check_box' : 'check_box_outline_blank'"
                size="17px" />
            </q-item-section>
            <q-item-section>
              <span class="db-chip" :class="`db-chip-${opt.color}`">{{ opt.label }}</span>
            </q-item-section>
          </q-item>
          <q-item v-if="!property.options?.length">
            <q-item-section class="text-grey-6 text-caption">
              Chưa có lựa chọn nào. Thêm trong cấu hình cột.
            </q-item-section>
          </q-item>
        </template>
      </q-list>
    </q-menu>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import {
  parseOptions, optionLabel, optionColor, splitList, formatNumber, formatDate
} from './dbTypes'

const props = defineProps({
  property: { type: Object, required: true },
  value: { type: String, default: null },
  users: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update'])

const editing = ref(false)
const draft = ref('')
const inputRef = ref(null)

const hasValue = computed(() => props.value !== null && props.value !== '')

function fullName (u) {
  return [u.firstName, u.lastName].filter(Boolean).join(' ') || u.userName
}

function userName (id) {
  const u = props.users.find(x => String(x.id) === String(id))
  return u ? fullName(u) : `#${id}`
}

function open () {
  if (props.readonly || editing.value) return
  draft.value = props.value || ''
  editing.value = true
  nextTick(() => inputRef.value?.focus())
}

function commit (next) {
  editing.value = false
  const value = next === null || next === undefined ? '' : String(next)
  if (value === (props.value || '')) return
  emit('update', value)
}

/** MULTI_SELECT / PERSON keep a comma-separated list; clicking toggles membership. */
function toggleInList (id) {
  const current = splitList(props.value)
  const next = current.includes(id) ? current.filter(x => x !== id) : [...current, id]
  emit('update', next.join(','))
}
</script>
