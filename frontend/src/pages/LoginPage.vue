<template>
  <div class="login-page flex flex-center">
    <div class="login-container">
      <!-- Logo / Brand -->
      <div class="text-center q-mb-xl">
        <div class="logo-text" style="font-size: 2.5rem; letter-spacing: 4px">gonERP</div>
        <div class="text-grey-5 q-mt-xs" style="font-size: 0.85rem; letter-spacing: 2px">
          ENTERPRISE RESOURCE PLANNING
        </div>
        <div class="accent-divider q-mt-sm" />
      </div>

      <!-- Login Card -->
      <q-card class="login-card" flat>
        <q-card-section class="q-pa-xl">
          <div class="text-h6 text-white q-mb-lg" style="font-weight: 300; letter-spacing: 1px">
            Sign In to Your Account
          </div>

          <q-form @submit="handleLogin" class="q-gutter-y-md">
            <q-input
              v-model="form.userName"
              label="Username"
              outlined
              dark
              color="green-5"
              label-color="grey-5"
              bg-color="rgba(255,255,255,0.03)"
              :rules="[v => !!v || 'Username is required']"
              lazy-rules
            >
              <template #prepend>
                <q-icon name="person" color="green-5" />
              </template>
            </q-input>

            <q-input
              v-model="form.password"
              label="Password"
              outlined
              dark
              color="green-5"
              label-color="grey-5"
              bg-color="rgba(255,255,255,0.03)"
              :type="showPassword ? 'text' : 'password'"
              :rules="[v => !!v || 'Password is required']"
              lazy-rules
            >
              <template #prepend>
                <q-icon name="lock" color="green-5" />
              </template>
              <template #append>
                <q-icon
                  :name="showPassword ? 'visibility_off' : 'visibility'"
                  class="cursor-pointer text-grey-5"
                  @click="showPassword = !showPassword"
                />
              </template>
            </q-input>

            <div class="q-pt-md">
              <q-btn
                type="submit"
                label="Sign In"
                color="primary"
                class="full-width sign-in-btn"
                unelevated
                size="lg"
                :loading="loading"
              >
                <template #loading>
                  <q-spinner-dots color="white" />
                </template>
              </q-btn>
            </div>
          </q-form>

          <div v-if="error" class="q-mt-md text-center text-red-4 text-caption">
            <q-icon name="error_outline" size="sm" class="q-mr-xs" />
            {{ error }}
          </div>
        </q-card-section>
      </q-card>

      <!-- Footer -->
      <div class="text-center q-mt-lg text-grey-7 text-caption">
        Default: admin / admin123
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'
import { useQuasar } from 'quasar'

const $q = useQuasar()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({ userName: '', password: '' })
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  error.value = ''
  loading.value = true
  try {
    await authStore.login(form.value.userName, form.value.password)
    $q.notify({ type: 'positive', message: 'Welcome back!', icon: 'check_circle' })
    router.push('/dashboard')
  } catch (err) {
    error.value = err.response?.data?.message || 'Invalid username or password'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  background: radial-gradient(ellipse at center, #1a2a1a 0%, #0D0D0D 70%);
  min-height: 100vh;
  width: 100%;
}

.login-container {
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-card {
  background: linear-gradient(135deg, #161616 0%, #1a1a1a 100%);
  border: 1px solid rgba(46, 125, 50, 0.3);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5), 0 0 30px rgba(46, 125, 50, 0.05);
}

.accent-divider {
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, #2E7D32, #F57C00);
  margin: 0 auto;
  border-radius: 2px;
}

.sign-in-btn {
  background: linear-gradient(90deg, #2E7D32, #388E3C);
  font-weight: 600;
  letter-spacing: 1px;
  transition: all 0.3s;
}

.sign-in-btn:hover {
  box-shadow: 0 4px 20px rgba(46, 125, 50, 0.4);
}
</style>
