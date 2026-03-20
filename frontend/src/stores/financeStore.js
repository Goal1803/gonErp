import { defineStore } from 'pinia'
import { ref } from 'vue'
import { financeAccountApi, financeReportApi } from 'src/api/finance'

export const useFinanceStore = defineStore('finance', () => {
  const accounts = ref([])
  const reports = ref([])
  const loading = ref(false)

  async function fetchAccounts() {
    loading.value = true
    try {
      const res = await financeAccountApi.getAll()
      accounts.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchReports() {
    loading.value = true
    try {
      const res = await financeReportApi.getAll()
      reports.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createAccount(data) {
    const res = await financeAccountApi.create(data)
    await fetchAccounts()
    return res.data.data
  }

  async function updateAccount(id, data) {
    const res = await financeAccountApi.update(id, data)
    await fetchAccounts()
    return res.data.data
  }

  async function deleteAccount(id) {
    await financeAccountApi.delete(id)
    await fetchAccounts()
  }

  async function createReport(data) {
    const res = await financeReportApi.create(data)
    await fetchReports()
    return res.data.data
  }

  async function updateReport(id, data) {
    const res = await financeReportApi.update(id, data)
    await fetchReports()
    return res.data.data
  }

  async function deleteReport(id) {
    await financeReportApi.delete(id)
    await fetchReports()
  }

  return {
    accounts, reports, loading,
    fetchAccounts, fetchReports,
    createAccount, updateAccount, deleteAccount,
    createReport, updateReport, deleteReport
  }
})
