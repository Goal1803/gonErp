import { ref, computed } from 'vue'
import { useWorktimeStore } from 'src/stores/worktimeStore'
import { worktimeUserConfigApi } from 'src/api/worktime'

// Shared state (module-level singleton)
const showBreakReminder = ref(false)
const showForceCheckout = ref(false)
const userTimezone = ref(null)
let breakReminderDismissedAt = 0
let forceCheckoutTriggered = false
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

  const forceCheckoutLabel = computed(() => {
    const raw = worktimeStore.settings?.forceCheckoutTime || '00:00'
    const t = raw.substring(0, 5)
    return t === '00:00' ? 'midnight' : t
  })

  // ── Break Reminder ──

  function checkBreakReminder() {
    if (showBreakReminder.value) return

    const breakReminderMinutes = worktimeStore.settings?.breakReminderMinutes ?? 240
    if (breakReminderMinutes <= 0) return

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

  function dismissBreakReminder() {
    showBreakReminder.value = false
    breakReminderDismissedAt = Date.now()
    if (breakChannel) breakChannel.postMessage({ type: 'BREAK_DISMISSED' })
  }

  async function takeBreakFromReminder() {
    showBreakReminder.value = false
    breakReminderDismissedAt = Date.now()
    await worktimeStore.pause()
    if (breakChannel) breakChannel.postMessage({ type: 'BREAK_STARTED' })
  }

  function resetBreakReminder() {
    breakReminderDismissedAt = 0
    showBreakReminder.value = false
    forceCheckoutTriggered = false
  }

  // ── Force Checkout ──

  function checkForceCheckout() {
    if (forceCheckoutTriggered) return

    const status = worktimeStore.clockStatus?.status
    if (status !== 'CHECKED_IN' && status !== 'ON_BREAK') {
      forceCheckoutTriggered = false
      return
    }

    const entry = worktimeStore.todayEntry
    if (!entry || !entry.workDate) return

    // Use user's own timezone, fallback to org setting
    const tz = userTimezone.value
      || worktimeStore.settings?.timezoneId
      || 'Asia/Ho_Chi_Minh'

    const now = new Date()
    const todayInTz = now.toLocaleDateString('en-CA', { timeZone: tz })
    const currentTime = now.toLocaleTimeString('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit', hour12: false })

    const rawForceTime = worktimeStore.settings?.forceCheckoutTime || '00:00'
    const forceTime = rawForceTime.substring(0, 5)

    let shouldForceCheckout = false
    if (forceTime === '00:00') {
      shouldForceCheckout = todayInTz !== entry.workDate
    } else {
      if (todayInTz === entry.workDate && currentTime >= forceTime) {
        shouldForceCheckout = true
      }
      if (todayInTz > entry.workDate) {
        shouldForceCheckout = true
      }
    }

    if (shouldForceCheckout) {
      forceCheckoutTriggered = true
      showForceCheckout.value = true
      worktimeStore.checkOut({ dailyNotes: '[Auto-checked out at ' + forceTime + ']' }).catch(() => {
        worktimeStore.fetchClockStatus()
        worktimeStore.fetchTodayEntry()
      })
      broadcastClockChange()
    }
  }

  // ── Refresh & Check ──

  async function refreshAndCheck() {
    try {
      await Promise.all([
        worktimeStore.fetchClockStatus(),
        worktimeStore.fetchTodayEntry(),
        worktimeStore.settings ? null : worktimeStore.fetchSettings(),
        userTimezone.value ? null : worktimeUserConfigApi.getMyConfig()
          .then(res => { userTimezone.value = res.data?.data?.timezoneId || null })
          .catch(() => {})
      ])
    } catch {
      // ignore
    }
    checkBreakReminder()
    checkForceCheckout()
  }

  // ── Lifecycle ──

  function startChecking() {
    if (checkInterval) clearInterval(checkInterval)
    if (breakChannel) { try { breakChannel.close() } catch {} }

    if (typeof BroadcastChannel !== 'undefined') {
      breakChannel = new BroadcastChannel('worktime-break-reminder')
      breakChannel.onmessage = (event) => {
        if (event.data?.type === 'BREAK_STARTED' || event.data?.type === 'BREAK_DISMISSED') {
          showBreakReminder.value = false
          if (event.data.type === 'BREAK_DISMISSED') breakReminderDismissedAt = Date.now()
        }
        if (event.data?.type === 'BREAK_STARTED' || event.data?.type === 'CLOCK_CHANGED' || event.data?.type === 'FORCE_CHECKOUT') {
          worktimeStore.fetchClockStatus()
          worktimeStore.fetchTodayEntry()
        }
      }
    }

    refreshAndCheck()
    checkInterval = setInterval(refreshAndCheck, 30000)
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

  function broadcastClockChange(type = 'CLOCK_CHANGED') {
    if (breakChannel) breakChannel.postMessage({ type })
  }

  return {
    showBreakReminder,
    showForceCheckout,
    breakReminderLabel,
    forceCheckoutLabel,
    checkBreakReminder,
    checkForceCheckout,
    dismissBreakReminder,
    takeBreakFromReminder,
    resetBreakReminder,
    broadcastClockChange,
    startChecking,
    stopChecking
  }
}
