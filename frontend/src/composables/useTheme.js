import { computed } from 'vue'
import { useQuasar } from 'quasar'

const STORAGE_KEY = 'gonerp_dark_mode'

export function useTheme() {
  const $q = useQuasar()

  const isDark = computed(() => $q.dark.isActive)

  function toggle() {
    $q.dark.toggle()
    localStorage.setItem(STORAGE_KEY, JSON.stringify($q.dark.isActive))
  }

  function init() {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored !== null) {
      $q.dark.set(JSON.parse(stored))
    }
  }

  init()

  return { isDark, toggle }
}
