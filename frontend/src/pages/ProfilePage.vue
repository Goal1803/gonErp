<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-icon name="person" size="sm" color="green-5" class="q-mr-sm" />
      <div class="text-h6 text-weight-bold text-white">My Profile</div>
    </div>

    <div class="row q-col-gutter-md">
      <!-- LEFT: Avatar Card -->
      <div class="col-12 col-md-4">
        <q-card dark bordered class="q-pa-md">
          <q-card-section class="text-center">
            <UserAvatar :user="profile" size="120px" />
            <div class="text-subtitle1 text-weight-bold text-white q-mt-md">
              {{ profile.firstName }} {{ profile.lastName }}
            </div>
            <div class="text-caption text-grey-5">{{ profile.userName }}</div>
          </q-card-section>
          <q-card-actions align="center">
            <q-btn
              flat
              color="green-5"
              icon="upload"
              label="Upload"
              @click="triggerFileInput"
              :loading="avatarLoading"
            />
            <q-btn
              flat
              color="red-4"
              icon="delete"
              label="Remove"
              @click="removeAvatar"
              :loading="avatarLoading"
              :disable="!profile.avatarUrl"
            />
          </q-card-actions>
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="uploadAvatar"
          />
        </q-card>
      </div>

      <!-- RIGHT Column -->
      <div class="col-12 col-md-8">
        <!-- Profile Form -->
        <q-card dark bordered class="q-mb-md">
          <q-card-section>
            <div class="text-subtitle1 text-weight-bold text-white q-mb-md">Profile Information</div>
            <div class="row q-col-gutter-sm">
              <div class="col-12 col-sm-6">
                <q-input
                  v-model="form.firstName"
                  label="First Name"
                  outlined
                  dense
                  dark
                  :rules="[val => !!val || 'First name is required']"
                />
              </div>
              <div class="col-12 col-sm-6">
                <q-input
                  v-model="form.lastName"
                  label="Last Name"
                  outlined
                  dense
                  dark
                />
              </div>
              <div class="col-12 col-sm-6">
                <q-input
                  v-model="form.dateOfBirth"
                  label="Date of Birth"
                  outlined
                  dense
                  dark
                  type="date"
                />
              </div>
              <div class="col-12 col-sm-6">
                <q-input
                  :model-value="profile.userName"
                  label="Username"
                  outlined
                  dense
                  dark
                  readonly
                />
              </div>
              <div class="col-12 col-sm-6">
                <q-input
                  :model-value="profile.role?.name"
                  label="Role"
                  outlined
                  dense
                  dark
                  readonly
                />
              </div>
            </div>
          </q-card-section>
          <q-card-actions align="right" class="q-px-md q-pb-md">
            <q-btn
              color="green-7"
              label="Save"
              icon="save"
              @click="saveProfile"
              :loading="profileLoading"
            />
          </q-card-actions>
        </q-card>

        <!-- Password Card -->
        <q-card dark bordered>
          <q-card-section>
            <div class="text-subtitle1 text-weight-bold text-white q-mb-md">Change Password</div>
            <div class="row q-col-gutter-sm">
              <div class="col-12">
                <q-input
                  v-model="passwordForm.currentPassword"
                  label="Current Password"
                  outlined
                  dense
                  dark
                  :type="showCurrentPwd ? 'text' : 'password'"
                  :rules="[val => !!val || 'Current password is required']"
                >
                  <template #append>
                    <q-icon
                      :name="showCurrentPwd ? 'visibility_off' : 'visibility'"
                      class="cursor-pointer"
                      @click="showCurrentPwd = !showCurrentPwd"
                    />
                  </template>
                </q-input>
              </div>
              <div class="col-12 col-sm-6">
                <q-input
                  v-model="passwordForm.newPassword"
                  label="New Password"
                  outlined
                  dense
                  dark
                  :type="showNewPwd ? 'text' : 'password'"
                  :rules="[val => !!val || 'Required', val => val.length >= 6 || 'Min 6 characters']"
                >
                  <template #append>
                    <q-icon
                      :name="showNewPwd ? 'visibility_off' : 'visibility'"
                      class="cursor-pointer"
                      @click="showNewPwd = !showNewPwd"
                    />
                  </template>
                </q-input>
              </div>
              <div class="col-12 col-sm-6">
                <q-input
                  v-model="passwordForm.confirmPassword"
                  label="Confirm Password"
                  outlined
                  dense
                  dark
                  :type="showConfirmPwd ? 'text' : 'password'"
                  :rules="[val => !!val || 'Required', val => val === passwordForm.newPassword || 'Passwords do not match']"
                >
                  <template #append>
                    <q-icon
                      :name="showConfirmPwd ? 'visibility_off' : 'visibility'"
                      class="cursor-pointer"
                      @click="showConfirmPwd = !showConfirmPwd"
                    />
                  </template>
                </q-input>
              </div>
            </div>
          </q-card-section>
          <q-card-actions align="right" class="q-px-md q-pb-md">
            <q-btn
              color="orange-7"
              label="Change Password"
              icon="lock"
              @click="changePassword"
              :loading="passwordLoading"
            />
          </q-card-actions>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { profileApi } from 'src/api/profile'
import UserAvatar from 'src/components/UserAvatar.vue'

const $q = useQuasar()
const authStore = useAuthStore()

const profile = ref({})
const form = reactive({ firstName: '', lastName: '', dateOfBirth: null })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const profileLoading = ref(false)
const avatarLoading = ref(false)
const passwordLoading = ref(false)
const showCurrentPwd = ref(false)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)
const fileInput = ref(null)

async function loadProfile() {
  try {
    const { data } = await profileApi.get()
    profile.value = data.data
    form.firstName = data.data.firstName || ''
    form.lastName = data.data.lastName || ''
    form.dateOfBirth = data.data.dateOfBirth || null
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load profile' })
  }
}

async function saveProfile() {
  if (!form.firstName) return
  profileLoading.value = true
  try {
    const { data } = await profileApi.update(form)
    profile.value = data.data
    await authStore.fetchCurrentUser()
    $q.notify({ type: 'positive', message: 'Profile updated' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to update profile' })
  } finally {
    profileLoading.value = false
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

async function uploadAvatar(event) {
  const file = event.target.files[0]
  if (!file) return
  avatarLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await profileApi.uploadAvatar(formData)
    profile.value = data.data
    await authStore.fetchCurrentUser()
    $q.notify({ type: 'positive', message: 'Avatar uploaded' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to upload avatar' })
  } finally {
    avatarLoading.value = false
    event.target.value = ''
  }
}

async function removeAvatar() {
  avatarLoading.value = true
  try {
    const { data } = await profileApi.deleteAvatar()
    profile.value = data.data
    await authStore.fetchCurrentUser()
    $q.notify({ type: 'positive', message: 'Avatar removed' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to remove avatar' })
  } finally {
    avatarLoading.value = false
  }
}

async function changePassword() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) return
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    $q.notify({ type: 'negative', message: 'Passwords do not match' })
    return
  }
  passwordLoading.value = true
  try {
    await profileApi.changePassword(passwordForm)
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    $q.notify({ type: 'positive', message: 'Password changed successfully' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to change password' })
  } finally {
    passwordLoading.value = false
  }
}

onMounted(loadProfile)
</script>
