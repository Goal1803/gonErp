<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 550px; max-width: 650px" dark class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon :name="isEdit ? 'edit' : 'add_business'" color="green-5" size="md" />
        <div>
          <div class="text-h6 text-white text-weight-medium">{{ isEdit ? 'Edit Organization' : 'Create Organization' }}</div>
          <div class="text-caption text-grey-5">{{ isEdit ? `Editing: ${org?.name}` : 'Create a new tenant organization' }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <q-form @submit="handleSubmit" class="q-gutter-y-md">
          <div class="row q-col-gutter-md">
            <!-- Name -->
            <div class="col-12">
              <q-input
                v-model="form.name"
                label="Organization Name *"
                outlined dark color="green-5"
                :rules="[v => !!v || 'Name is required']"
                lazy-rules
                @update:model-value="autoSlug"
              />
            </div>

            <!-- Slug -->
            <div class="col-12">
              <q-input
                v-model="form.slug"
                label="Slug *"
                outlined dark color="green-5"
                :rules="[v => !!v || 'Slug is required']"
                lazy-rules
                hint="URL-friendly identifier"
              />
            </div>

            <!-- Org Type -->
            <div class="col-12">
              <q-select
                v-model="form.orgTypeId"
                :options="orgTypeOptions"
                label="Organization Type *"
                outlined dark color="green-5"
                emit-value map-options
                :rules="[v => !!v || 'Type is required']"
                lazy-rules
              />
            </div>

            <!-- Active -->
            <div class="col-12">
              <q-toggle v-model="form.active" label="Active" dark color="green-5" />
            </div>

            <!-- Feature Flags -->
            <div class="col-12">
              <div class="text-caption text-grey-5 q-mb-xs">Module Access</div>
              <q-toggle v-model="form.moduleTaskManager" label="Task Manager" dark color="teal-5" class="q-mr-md" />
              <q-toggle v-model="form.moduleImageManager" label="Image Manager" dark color="blue-5" class="q-mr-md" />
              <q-toggle v-model="form.moduleDesigns" label="Designs" dark color="purple-5" />
            </div>

            <!-- Admin User (create only) -->
            <template v-if="!isEdit">
              <div class="col-12">
                <q-separator dark class="q-my-sm" />
                <div class="text-subtitle2 text-grey-4">First Admin User</div>
              </div>
              <div class="col-6">
                <q-input v-model="form.adminUserName" label="Admin Username *" outlined dark color="green-5"
                  :rules="[v => !!v || 'Username is required']" lazy-rules />
              </div>
              <div class="col-6">
                <q-input v-model="form.adminPassword" label="Admin Password *" outlined dark color="green-5"
                  :type="showPwd ? 'text' : 'password'"
                  :rules="[v => !!v || 'Password is required']" lazy-rules>
                  <template #append>
                    <q-icon :name="showPwd ? 'visibility_off' : 'visibility'" class="cursor-pointer text-grey-5"
                      @click="showPwd = !showPwd" />
                  </template>
                </q-input>
              </div>
              <div class="col-6">
                <q-input v-model="form.adminFirstName" label="First Name *" outlined dark color="green-5"
                  :rules="[v => !!v || 'First name is required']" lazy-rules />
              </div>
              <div class="col-6">
                <q-input v-model="form.adminLastName" label="Last Name" outlined dark color="green-5" />
              </div>
            </template>
          </div>
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          :label="isEdit ? 'Save Changes' : 'Create Organization'"
          color="primary" unelevated :loading="saving"
          @click="handleSubmit"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { orgApi } from 'src/api/organizations'

const props = defineProps({
  modelValue: Boolean,
  org: { type: Object, default: null },
  orgTypes: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.org)
const saving = ref(false)
const showPwd = ref(false)

const defaultForm = {
  name: '', slug: '', orgTypeId: null, active: true,
  moduleTaskManager: true, moduleImageManager: true, moduleDesigns: true,
  adminUserName: '', adminPassword: '', adminFirstName: '', adminLastName: ''
}

const form = ref({ ...defaultForm })

const orgTypeOptions = computed(() =>
  props.orgTypes.map(t => ({ label: t.name, value: t.id }))
)

const autoSlug = (name) => {
  if (!isEdit.value && name) {
    form.value.slug = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')
  }
}

watch(() => props.org, (o) => {
  if (o) {
    form.value = {
      name: o.name || '', slug: o.slug || '', orgTypeId: o.orgTypeId || null,
      active: o.active ?? true,
      moduleTaskManager: o.moduleTaskManager ?? true,
      moduleImageManager: o.moduleImageManager ?? true,
      moduleDesigns: o.moduleDesigns ?? true,
      adminUserName: '', adminPassword: '', adminFirstName: '', adminLastName: ''
    }
  } else {
    form.value = { ...defaultForm }
  }
}, { immediate: true })

const handleSubmit = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await orgApi.update(props.org.id, form.value)
      $q.notify({ type: 'positive', message: 'Organization updated' })
    } else {
      await orgApi.create(form.value)
      $q.notify({ type: 'positive', message: 'Organization created' })
    }
    emit('saved')
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Operation failed' })
  } finally {
    saving.value = false
  }
}
</script>
