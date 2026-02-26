import { defineStore } from 'pinia'
import { ref } from 'vue'
import { worktimeClockApi, worktimeSettingsApi } from 'src/api/worktime'

export const useWorktimeStore = defineStore('worktime', () => {
  const clockStatus = ref(null)
  const todayEntry = ref(null)
  const settings = ref(null)
  const loading = ref(false)

  async function fetchClockStatus() {
    try {
      const res = await worktimeClockApi.getStatus()
      clockStatus.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to fetch clock status', e)
    }
  }

  async function fetchTodayEntry() {
    try {
      const res = await worktimeClockApi.getToday()
      todayEntry.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to fetch today entry', e)
    }
  }

  async function fetchSettings() {
    loading.value = true
    try {
      const res = await worktimeSettingsApi.get()
      settings.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to fetch settings', e)
    } finally {
      loading.value = false
    }
  }

  async function checkIn(data) {
    loading.value = true
    try {
      await worktimeClockApi.checkIn(data)
      await Promise.all([fetchClockStatus(), fetchTodayEntry()])
    } catch (e) {
      console.error('Failed to check in', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function pause() {
    loading.value = true
    try {
      await worktimeClockApi.pause()
      await Promise.all([fetchClockStatus(), fetchTodayEntry()])
    } catch (e) {
      console.error('Failed to pause', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function resume() {
    loading.value = true
    try {
      await worktimeClockApi.resume()
      await Promise.all([fetchClockStatus(), fetchTodayEntry()])
    } catch (e) {
      console.error('Failed to resume', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function checkOut(data) {
    loading.value = true
    try {
      await worktimeClockApi.checkOut(data)
      await Promise.all([fetchClockStatus(), fetchTodayEntry()])
    } catch (e) {
      console.error('Failed to check out', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateSettings(data) {
    loading.value = true
    try {
      const res = await worktimeSettingsApi.update(data)
      settings.value = res.data.data
      return res.data.data
    } catch (e) {
      console.error('Failed to update settings', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    clockStatus,
    todayEntry,
    settings,
    loading,
    fetchClockStatus,
    fetchTodayEntry,
    fetchSettings,
    checkIn,
    pause,
    resume,
    checkOut,
    updateSettings
  }
})
