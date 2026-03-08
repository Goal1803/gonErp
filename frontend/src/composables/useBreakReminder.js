import { ref, computed } from 'vue'
import { useWorktimeStore } from 'src/stores/worktimeStore'

// Shared state (module-level singleton)
const showBreakReminder = ref(false)
let breakReminderDismissedAt = 0
let breakChannel = null
let checkInterval = null

export function useBreakReminder() {
  const worktimeStore = useWorktimeStore()

  const breakReminderLabel = computed(() => {
    const mins = worktimeStore.settings?.breakReminderMinutes ?? 240
    const h = Math.floor(mins / 60)
    const m = mins % 60
    if (h > 0 && m > 0) return `${h}h ${m}min`
    if (h > 0) return `${h} hour${h > 1 ? 's' : ''}`
    return `${m} minutes`
  })

  function checkBreakReminder() {
    // Don't stack popups while one is already showing
    if (showBreakReminder.value) return

    const breakReminderMinutes = worktimeStore.settings?.breakReminderMinutes ?? 240
    if (breakReminderMinutes <= 0) return

    // After dismissing, wait one full reminder interval before showing again
    if (breakReminderDismissedAt && (Date.now() - breakReminderDismissedAt) < breakReminderMinutes * 60000) return

    const status = worktimeStore.clockStatus?.status
    if (status !== 'CHECKED_IN') return

    const entry = worktimeStore.todayEntry
    if (!entry || !entry.checkInTime) return

    const breaks = entry.breaks || []
    const completedBreaks = breaks.filter(b => b.endTime)
    let lastBreakEnd = null
    if (completedBreaks.length > 0) {
      lastBreakEnd = completedBreaks.reduce((latest, b) => {
        const t = new Date(b.endTime).getTime()
        return t > latest ? t : latest
      }, 0)
    }

    const sinceTime = lastBreakEnd ? new Date(lastBreakEnd) : new Date(entry.checkInTime)
    const continuousMinutes = (Date.now() - sinceTime.getTime()) / 60000

    if (continuousMinutes >= breakReminderMinutes) {
      showBreakReminder.value = true
    }
  }

  async function refreshAndCheck() {
    try {
      await Promise.all([
        worktimeStore.fetchClockStatus(),
        worktimeStore.fetchTodayEntry(),
        worktimeStore.settings ? null : worktimeStore.fetchSettings()
      ])
    } catch {
      // ignore
    }
    checkBreakReminder()
  }

  function dismissBreakReminder() {
    showBreakReminder.value = false
    breakReminderDismissedAt = Date.now()
    if (breakChannel) breakChannel.postMessage({ type: 'BREAK_DISMISSED' })
  }

  async function takeBreakFromReminder() {
    showBreakReminder.value = false
    breakReminderDismissedAt = Date.now()
    if (breakChannel) breakChannel.postMessage({ type: 'BREAK_STARTED' })
    await worktimeStore.pause()
  }

  function resetBreakReminder() {
    breakReminderDismissedAt = 0
    showBreakReminder.value = false
  }

  function startChecking() {
    // Clean up any existing interval/channel first (idempotent)
    if (checkInterval) clearInterval(checkInterval)
    if (breakChannel) { try { breakChannel.close() } catch {} }

    // BroadcastChannel for cross-tab sync
    if (typeof BroadcastChannel !== 'undefined') {
      breakChannel = new BroadcastChannel('worktime-break-reminder')
      breakChannel.onmessage = (event) => {
        if (event.data?.type === 'BREAK_STARTED' || event.data?.type === 'BREAK_DISMISSED') {
          showBreakReminder.value = false
          if (event.data.type === 'BREAK_DISMISSED') breakReminderDismissedAt = Date.now()
        }
        if (event.data?.type === 'BREAK_STARTED') {
          worktimeStore.fetchClockStatus()
          worktimeStore.fetchTodayEntry()
        }
        if (event.data?.type === 'FORCE_CHECKOUT') {
          worktimeStore.fetchClockStatus()
          worktimeStore.fetchTodayEntry()
        }
      }
    }

    // Initial fetch + check
    refreshAndCheck()

    // Refresh data and check every 60 seconds
    checkInterval = setInterval(refreshAndCheck, 60000)
  }

  function stopChecking() {
    if (checkInterval) {
      clearInterval(checkInterval)
      checkInterval = null
    }
    if (breakChannel) {
      try { breakChannel.close() } catch {}
      breakChannel = null
    }
  }

  return {
    showBreakReminder,
    breakReminderLabel,
    checkBreakReminder,
    dismissBreakReminder,
    takeBreakFromReminder,
    resetBreakReminder,
    startChecking,
    stopChecking
  }
}
