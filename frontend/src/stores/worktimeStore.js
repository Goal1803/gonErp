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
      const data = res.data.data
      // Ensure status field exists — derive from boolean fields if missing
      // (Jackson serializes boolean isClockedIn as "clockedIn", isOnBreak as "onBreak")
      if (data && !data.status) {
        if (data.clockedIn || data.isClockedIn) {
          data.status = (data.onBreak || data.isOnBreak) ? 'ON_BREAK' : 'CHECKED_IN'
        } else if (data.currentEntryId) {
          data.status = 'CHECKED_OUT'
        }
      }
      clockStatus.value = data
      return data
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

  // Update clockStatus from a TimeEntryResponse (returned by check-in/pause/resume/check-out)
  function updateStatusFromEntry(entry) {
    if (!entry) return
    clockStatus.value = {
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
      const entry = res.data.data
      updateStatusFromEntry(entry)
      todayEntry.value = entry
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
      const res = await worktimeClockApi.pause()
      const entry = res.data.data
      updateStatusFromEntry(entry)
      todayEntry.value = entry
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
      const res = await worktimeClockApi.resume()
      const entry = res.data.data
      updateStatusFromEntry(entry)
      todayEntry.value = entry
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
      const res = await worktimeClockApi.checkOut(data)
      const entry = res.data.data
      updateStatusFromEntry(entry)
      todayEntry.value = entry
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
