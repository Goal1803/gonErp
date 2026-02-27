import { defineStore } from 'pinia'
import { ref } from 'vue'
import { worktimeClockApi, worktimeSettingsApi } from 'src/api/worktime'

export const useWorktimeStore = defineStore('worktime', () => {
  const clockStatus = ref(null)
  const todayEntry = ref(null)
  const settings = ref(null)
  const loading = ref(false)

  // Normalize status response — handles Jackson boolean naming (is-prefix stripped)
  function normalizeClockStatus(data) {
    if (!data) return data
    // Derive status from boolean fields if status is missing
    if (!data.status) {
      if (data.clockedIn || data.isClockedIn) {
        data.status = (data.onBreak || data.isOnBreak) ? 'ON_BREAK' : 'CHECKED_IN'
      } else if (data.currentEntryId) {
        data.status = 'CHECKED_OUT'
      }
    }
    return data
  }

  async function fetchClockStatus() {
    try {
      const res = await worktimeClockApi.getStatus()
      const data = normalizeClockStatus(res.data?.data)
      clockStatus.value = data
      return data
    } catch (e) {
      console.error('[worktime] Failed to fetch clock status', e)
    }
  }

  async function fetchTodayEntry() {
    try {
      const res = await worktimeClockApi.getToday()
      todayEntry.value = res.data?.data || null
      return todayEntry.value
    } catch (e) {
      console.error('[worktime] Failed to fetch today entry', e)
    }
  }

  async function fetchSettings() {
    loading.value = true
    try {
      const res = await worktimeSettingsApi.get()
      settings.value = res.data?.data || null
      return settings.value
    } catch (e) {
      console.error('[worktime] Failed to fetch settings', e)
    } finally {
      loading.value = false
    }
  }

  // Build clockStatus object from a TimeEntryResponse
  function buildClockStatusFromEntry(entry) {
    if (!entry) return null
    return {
      status: entry.status || null,
      currentEntryId: entry.id,
      checkInTime: entry.checkInTime,
      workLocation: entry.workLocation,
      elapsedWorkMinutes: entry.totalWorkMinutes || 0,
      elapsedBreakMinutes: entry.totalBreakMinutes || 0
    }
  }

  async function checkIn(data) {
    loading.value = true
    try {
      const res = await worktimeClockApi.checkIn(data)
      const entry = res.data?.data
      if (entry && entry.status) {
        clockStatus.value = buildClockStatusFromEntry(entry)
        todayEntry.value = entry
      } else {
        // Fallback: re-fetch from server if response was unexpected
        console.warn('[worktime] checkIn response missing status, re-fetching', entry)
        await Promise.all([fetchClockStatus(), fetchTodayEntry()])
      }
    } catch (e) {
      console.error('[worktime] Failed to check in', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function pause() {
    loading.value = true
    try {
      const res = await worktimeClockApi.pause()
      const entry = res.data?.data
      if (entry && entry.status) {
        clockStatus.value = buildClockStatusFromEntry(entry)
        todayEntry.value = entry
      } else {
        await Promise.all([fetchClockStatus(), fetchTodayEntry()])
      }
    } catch (e) {
      console.error('[worktime] Failed to pause', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function resume() {
    loading.value = true
    try {
      const res = await worktimeClockApi.resume()
      const entry = res.data?.data
      if (entry && entry.status) {
        clockStatus.value = buildClockStatusFromEntry(entry)
        todayEntry.value = entry
      } else {
        await Promise.all([fetchClockStatus(), fetchTodayEntry()])
      }
    } catch (e) {
      console.error('[worktime] Failed to resume', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function checkOut(data) {
    loading.value = true
    try {
      const res = await worktimeClockApi.checkOut(data)
      const entry = res.data?.data
      if (entry && entry.status) {
        clockStatus.value = buildClockStatusFromEntry(entry)
        todayEntry.value = entry
      } else {
        await Promise.all([fetchClockStatus(), fetchTodayEntry()])
      }
    } catch (e) {
      console.error('[worktime] Failed to check out', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateSettings(data) {
    loading.value = true
    try {
      const res = await worktimeSettingsApi.update(data)
      settings.value = res.data?.data || null
      return settings.value
    } catch (e) {
      console.error('[worktime] Failed to update settings', e)
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
