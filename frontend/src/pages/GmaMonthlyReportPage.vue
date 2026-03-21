<template>
  <q-page padding>
    <!-- Header -->
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance/gma" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">
          {{ report ? `${monthName(report.month)} ${report.year}` : 'Monthly Report' }}
        </div>
        <div class="text-caption text-grey-5">
          <q-badge v-if="report" :color="statusColor(report.status)" :label="report.status" class="q-mr-sm" />
          Upload CSVs, categorize, and export
        </div>
      </div>
      <q-space />
      <q-btn flat icon="restart_alt" color="grey-5" label="Reset All" no-caps @click="resetAll" class="q-mr-sm">
        <q-tooltip>Clear all categories/notes and allow re-categorization</q-tooltip>
      </q-btn>
      <q-btn flat icon="sync" color="blue-4" label="Re-parse" no-caps @click="reExtract" class="q-mr-sm">
        <q-tooltip>Re-extract canonical fields from raw data (after parser updates)</q-tooltip>
      </q-btn>
      <q-btn flat icon="auto_fix_high" color="amber-5" label="Auto Categorize" no-caps @click="runCategorize" class="q-mr-sm" />
      <q-btn-dropdown flat icon="download" color="green-5" label="Export" no-caps>
        <q-list dense>
          <q-item clickable v-close-popup @click="downloadExcel(null)">
            <q-item-section>Excel (all accounts)</q-item-section>
          </q-item>
          <q-item clickable v-close-popup @click="downloadZip">
            <q-item-section>ZIP (per account Excel)</q-item-section>
          </q-item>
          <q-separator v-if="accounts.length" />
          <q-item v-for="acc in accounts" :key="acc.id" clickable v-close-popup @click="downloadExcel(acc.id)">
            <q-item-section>Excel: {{ acc.name }}</q-item-section>
          </q-item>
          <q-separator />
          <q-item clickable v-close-popup @click="generateZipPackage">
            <q-item-section avatar><q-icon name="archive" color="amber-5" /></q-item-section>
            <q-item-section>Generate ZIP Package</q-item-section>
          </q-item>
          <q-item clickable v-close-popup @click="loadExportHistory(); showExportHistory = true">
            <q-item-section avatar><q-icon name="history" color="blue-4" /></q-item-section>
            <q-item-section>Export History</q-item-section>
          </q-item>
        </q-list>
      </q-btn-dropdown>
    </div>

    <!-- Account Tabs -->
    <q-tabs v-model="activeTab" dense align="left" class="q-mb-md" active-color="green-5" indicator-color="green-5">
      <q-tab v-for="acc in accounts" :key="acc.id" :name="acc.id" :label="acc.name" no-caps />
    </q-tabs>

    <!-- Toolbar -->
    <div class="row items-center q-mb-md q-gutter-sm">
      <div class="text-caption text-grey-5">
        {{ filteredTransactions.length }}/{{ transactions.length }} transactions
      </div>
      <q-space />
      <q-btn-toggle
        v-model="completedFilter"
        no-caps dense unelevated
        text-color="white"
        toggle-color="green-7"
        color="grey-9"
        :options="[
          { label: 'All', value: 'all' },
          { label: 'Pending', value: 'pending' },
          { label: 'Done', value: 'done' }
        ]"
        class="q-mr-sm"
        style="border: 1px solid rgba(255,255,255,0.15); border-radius: 4px;"
      />
      <q-btn flat dense icon="view_column" color="grey-5" no-caps>
        <q-tooltip>Show/hide columns</q-tooltip>
        <q-menu anchor="bottom right" self="top right" style="min-width: 250px; max-height: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
          <q-list dense>
            <q-item-label header class="text-grey-5 text-caption">Visible Columns</q-item-label>
            <q-item v-for="col in allColumnOptions" :key="col.value" dense clickable @click="toggleColumn(col.value)">
              <q-item-section side>
                <q-checkbox :model-value="visibleColumns.includes(col.value)" @update:model-value="toggleColumn(col.value)" color="green-5" dense dark />
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-white" style="font-size: 0.8rem">{{ col.label }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-separator />
            <q-item dense clickable @click="showAllColumns">
              <q-item-section class="text-center">
                <q-item-label class="text-green-5 text-caption">Show All</q-item-label>
              </q-item-section>
            </q-item>
            <q-item dense clickable @click="showDefaultColumns">
              <q-item-section class="text-center">
                <q-item-label class="text-blue-4 text-caption">Reset to Default</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </q-btn>
    </div>

    <!-- File Manager -->
    <div class="fm q-mb-md">
      <div class="fm-header row items-center q-px-md q-py-sm" style="cursor: pointer;" @click="showFileManager = !showFileManager">
        <q-icon :name="showFileManager ? 'folder_open' : 'folder'" color="grey-5" size="sm" class="q-mr-sm" />
        <span class="text-grey-4 text-weight-medium">Files</span>
        <q-icon :name="showFileManager ? 'expand_less' : 'expand_more'" color="grey-6" class="q-ml-sm" />
        <q-space />
        <q-btn flat dense icon="upload_file" color="blue-4" no-caps @click.stop="$refs.rootUpload.pickFiles()">
          <q-tooltip>Upload attachment</q-tooltip>
        </q-btn>
        <q-file ref="rootUpload" v-model="rootAttachFiles" multiple class="hidden" @update:model-value="handleAttachUpload(null)" />
        <q-btn flat dense icon="create_new_folder" color="amber-5" @click.stop="showNewFolder = !showNewFolder">
          <q-tooltip>New folder</q-tooltip>
        </q-btn>
      </div>

      <div v-show="showFileManager">
      <!-- New folder input -->
      <div v-if="showNewFolder" class="row items-center q-px-md q-py-xs q-gutter-sm" style="background: rgba(255,255,255,0.03);">
        <q-icon name="folder" color="amber-5" />
        <q-input v-model="newFolderName" placeholder="Folder name..." dense borderless dark autofocus style="flex: 1; max-width: 250px;" class="text-white" />
        <q-btn flat dense icon="check" color="green-5" :disable="!newFolderName" @click="createEmptyFolder" />
        <q-btn flat dense icon="close" color="grey-5" @click="showNewFolder = false; newFolderName = ''" />
      </div>

      <!-- Transaction file -->
      <div v-if="!hasTransactionFile" class="fm-row fm-upload-row" style="background: rgba(76,175,80,0.04);">
        <q-icon name="table_chart" color="green-5" size="sm" class="q-mr-md" />
        <q-file v-model="txUploadFile" label="Upload transaction CSV..." dense borderless dark accept=".csv" style="flex: 1; max-width: 300px;" />
        <q-btn unelevated dense color="green-7" icon="upload" label="Upload" no-caps size="sm" :loading="uploading" @click="handleTxUpload" :disable="!txUploadFile" />
      </div>

      <!-- Root drop zone -->
      <div
        class="fm-drop-zone"
        :class="{ 'fm-drop-active': dragOverTarget === 'root' }"
        @dragover.prevent="onDragOver($event, 'root')"
        @dragleave.self="dragOverTarget = null"
        @drop.prevent="onDrop(null)"
      >
        <!-- Root files -->
        <div
          v-for="f in rootFiles" :key="f.id"
          class="fm-row"
          :class="{ 'fm-draggable': f.fileType !== 'TRANSACTION' }"
          :draggable="f.fileType !== 'TRANSACTION'"
          @dragstart="onDragStart($event, f)"
          @dragend="onDragEnd"
        >
          <q-icon v-if="f.fileType !== 'TRANSACTION'" name="drag_indicator" color="grey-7" size="xs" class="fm-grip q-mr-sm" />
          <q-icon :name="f.fileType === 'TRANSACTION' ? 'table_chart' : 'description'" :color="f.fileType === 'TRANSACTION' ? 'green-5' : 'blue-4'" size="sm" class="q-mr-md" />
          <div class="fm-name">
            {{ f.originalFilename }}
            <div v-if="f.fileType === 'TRANSACTION'" class="fm-meta">{{ f.rowCount }} rows &middot; {{ f.parseStatus }}</div>
          </div>
          <q-space />
          <div class="fm-actions">
            <q-btn v-if="f.storageUrl" flat round dense icon="download" size="sm" color="grey-5" :href="f.storageUrl" target="_blank" />
            <q-btn flat round dense icon="delete_outline" size="sm" color="grey-5" @click="deleteUploadedFile(f)" />
          </div>
        </div>
      </div>

      <!-- Folder tree (2 levels) -->
      <template v-for="folder in topLevelFolders" :key="folder">
        <div
          class="fm-folder-header fm-drop-zone"
          :class="{ 'fm-drop-active': dragOverTarget === folder }"
          @dragover.prevent="onDragOver($event, folder)"
          @dragleave.self="dragOverTarget = null"
          @drop.prevent="onDrop(folder)"
          @click="toggleFolder(folder)"
        >
          <q-icon :name="openFolders[folder] ? 'folder_open' : 'folder'" color="amber-5" size="sm" class="q-mr-sm" />
          <!-- Rename inline -->
          <template v-if="renamingFolder === folder">
            <q-input v-model="renameValue" dense borderless dark autofocus class="text-white" style="flex:1; max-width:250px;" @keyup.enter="submitRename(folder)" @blur="cancelRename" />
            <q-btn flat round dense icon="check" size="sm" color="green-5" @mousedown.prevent="submitRename(folder)" />
            <q-btn flat round dense icon="close" size="sm" color="grey-5" @mousedown.prevent="cancelRename" />
          </template>
          <template v-else>
            <span class="text-white text-weight-medium">{{ folder }}</span>
            <span class="text-grey-6 q-ml-sm text-caption">({{ filesInFolderTree(folder).length }})</span>
          </template>
          <q-space />
          <div class="fm-actions" style="opacity:1;">
            <q-btn flat round dense icon="edit" size="xs" color="grey-6" @click.stop="startRename(folder)"><q-tooltip>Rename</q-tooltip></q-btn>
            <q-btn flat round dense icon="create_new_folder" size="xs" color="grey-6" @click.stop="startNewSubfolder(folder)"><q-tooltip>New subfolder</q-tooltip></q-btn>
            <q-btn flat round dense icon="upload_file" size="xs" color="grey-6" @click.stop="triggerFolderUpload(folder)"><q-tooltip>Upload</q-tooltip></q-btn>
            <input type="file" multiple :ref="el => folderInputRefs[folder] = el" class="hidden" @change="onFolderFileChange($event, folder)" />
          </div>
          <q-icon :name="openFolders[folder] ? 'expand_less' : 'expand_more'" color="grey-6" />
        </div>

        <div v-show="openFolders[folder]" class="fm-folder-content">
          <!-- New subfolder input -->
          <div v-if="newSubfolderParent === folder" class="fm-row" style="background: rgba(255,255,255,0.03);">
            <q-icon name="folder" color="amber-4" size="xs" class="q-mr-sm" />
            <q-input v-model="newSubfolderName" placeholder="Subfolder name..." dense borderless dark autofocus style="flex:1; max-width:200px;" class="text-white" @keyup.enter="createSubfolder(folder)" />
            <q-btn flat dense icon="check" size="sm" color="green-5" :disable="!newSubfolderName" @click="createSubfolder(folder)" />
            <q-btn flat dense icon="close" size="sm" color="grey-5" @click="newSubfolderParent = null; newSubfolderName = ''" />
          </div>

          <!-- Direct files in this folder -->
          <div
            v-for="f in directFilesInFolder(folder)" :key="f.id"
            class="fm-row fm-draggable"
            draggable="true"
            @dragstart="onDragStart($event, f)"
            @dragend="onDragEnd"
          >
            <q-icon name="drag_indicator" color="grey-7" size="xs" class="fm-grip q-mr-sm" />
            <q-icon name="description" color="blue-4" size="sm" class="q-mr-sm" />
            <div class="fm-name">{{ f.originalFilename }}</div>
            <q-space />
            <div class="fm-actions">
              <q-btn v-if="f.storageUrl" flat round dense icon="download" size="sm" color="grey-5" :href="f.storageUrl" target="_blank" />
              <q-btn flat round dense icon="delete_outline" size="sm" color="grey-5" @click="deleteUploadedFile(f)" />
            </div>
          </div>

          <!-- Sub-subfolders -->
          <template v-for="sub in childFolders(folder)" :key="sub">
            <div
              class="fm-folder-header fm-drop-zone"
              :class="{ 'fm-drop-active': dragOverTarget === sub }"
              @dragover.prevent="onDragOver($event, sub)"
              @dragleave.self="dragOverTarget = null"
              @drop.prevent="onDrop(sub)"
              @click="toggleFolder(sub)"
            >
              <q-icon :name="openFolders[sub] ? 'folder_open' : 'folder'" color="amber-3" size="xs" class="q-mr-sm" />
              <template v-if="renamingFolder === sub">
                <q-input v-model="renameValue" dense borderless dark autofocus class="text-white" style="flex:1; max-width:200px;" @keyup.enter="submitRename(sub)" @blur="cancelRename" />
                <q-btn flat round dense icon="check" size="xs" color="green-5" @mousedown.prevent="submitRename(sub)" />
              </template>
              <template v-else>
                <span class="text-white text-caption">{{ subfolderDisplayName(sub) }}</span>
                <span class="text-grey-6 q-ml-xs text-caption">({{ directFilesInFolder(sub).length }})</span>
              </template>
              <q-space />
              <div class="fm-actions" style="opacity:1;">
                <q-btn flat round dense icon="edit" size="xs" color="grey-7" @click.stop="startRename(sub)"><q-tooltip>Rename</q-tooltip></q-btn>
                <q-btn flat round dense icon="upload_file" size="xs" color="grey-7" @click.stop="triggerFolderUpload(sub)"><q-tooltip>Upload</q-tooltip></q-btn>
                <input type="file" multiple :ref="el => folderInputRefs[sub] = el" class="hidden" @change="onFolderFileChange($event, sub)" />
              </div>
              <q-icon :name="openFolders[sub] ? 'expand_less' : 'expand_more'" color="grey-7" size="xs" />
            </div>
            <div v-show="openFolders[sub]" class="fm-folder-content">
              <div
                v-for="f in directFilesInFolder(sub)" :key="f.id"
                class="fm-row fm-draggable"
                draggable="true"
                @dragstart="onDragStart($event, f)"
                @dragend="onDragEnd"
              >
                <q-icon name="drag_indicator" color="grey-7" size="xs" class="fm-grip q-mr-sm" />
                <q-icon name="description" color="blue-4" size="sm" class="q-mr-sm" />
                <div class="fm-name">{{ f.originalFilename }}</div>
                <q-space />
                <div class="fm-actions">
                  <q-btn v-if="f.storageUrl" flat round dense icon="download" size="sm" color="grey-5" :href="f.storageUrl" target="_blank" />
                  <q-btn flat round dense icon="delete_outline" size="sm" color="grey-5" @click="deleteUploadedFile(f)" />
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>
      </div><!-- end v-show showFileManager -->
    </div>

    <!-- Transaction Table -->
    <q-table
      :rows="filteredTransactions"
      :columns="tableColumns"
      row-key="id"
      flat bordered dense
      :loading="loadingTx"
      class="erp-table"
      :pagination="{ rowsPerPage: 50 }"
      :visible-columns="visibleColumns"
    >
      <template v-slot:body-cell-completed="props">
        <q-td :props="props">
          <q-btn
            flat dense round
            :icon="props.row.completed ? 'check_circle' : 'radio_button_unchecked'"
            :color="props.row.completed ? 'green-5' : 'grey-6'"
            @click="toggleCompleted(props.row)"
          />
        </q-td>
      </template>
      <template v-slot:body-cell-note="props">
        <q-td :props="props">
          <q-input
            v-model="props.row.note"
            dense borderless
            @blur="saveTransaction(props.row)"
            placeholder="Add note..."
            class="text-white"
          />
        </q-td>
      </template>
      <template v-slot:body-cell-category="props">
        <q-td :props="props">
          <q-input
            v-model="props.row.category"
            dense borderless
            @blur="saveTransaction(props.row)"
            placeholder="Category..."
            class="text-white"
          />
        </q-td>
      </template>
      <template v-slot:body-cell-subcategory="props">
        <q-td :props="props">
          <q-input
            v-model="props.row.subcategory"
            dense borderless
            @blur="saveTransaction(props.row)"
            placeholder="Sub..."
            class="text-white"
          />
        </q-td>
      </template>
      <template v-slot:body-cell-invoice="props">
        <q-td :props="props">
          <q-btn flat dense icon="attach_file" color="blue-4" @click="openInvoiceUpload(props.row)">
            <q-badge v-if="invoiceCountMap[props.row.id]" floating color="green-7" :label="invoiceCountMap[props.row.id]" />
            <q-tooltip>Upload/View invoice ({{ invoiceCountMap[props.row.id] || 0 }})</q-tooltip>
          </q-btn>
          <q-btn v-if="props.row.manuallyReviewed || props.row.autoCategorized || props.row.category || props.row.note" flat dense icon="restart_alt" color="grey-5" @click="resetRow(props.row)">
            <q-tooltip>Reset category/note (allow re-categorization)</q-tooltip>
          </q-btn>
        </q-td>
      </template>
      <template v-slot:body-cell-amount="props">
        <q-td :props="props">
          <span :class="props.row.amount < 0 ? 'text-red-4' : 'text-green-4'">
            {{ props.row.amount != null ? Number(props.row.amount).toFixed(2) : '' }}
          </span>
        </q-td>
      </template>
    </q-table>

    <!-- Export History Dialog -->
    <q-dialog v-model="showExportHistory" @hide="stopExportPoll">
      <q-card style="min-width: 600px; max-width: 800px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Export History</div>
        </q-card-section>
        <q-card-section>
          <q-markup-table flat bordered dense class="erp-table" v-if="exportHistory.length">
            <thead>
              <tr>
                <th class="text-left text-grey-5">Generated At</th>
                <th class="text-left text-grey-5">Generated By</th>
                <th class="text-right text-grey-5">File Size</th>
                <th class="text-center text-grey-5">Status</th>
                <th class="text-center text-grey-5">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="exp in exportHistory" :key="exp.id">
                <td class="text-white">{{ new Date(exp.createdAt).toLocaleString() }}</td>
                <td class="text-white">{{ exp.generatedBy || '-' }}</td>
                <td class="text-right text-white">{{ exp.fileSize ? formatFileSize(exp.fileSize) : '-' }}</td>
                <td class="text-center">
                  <q-badge
                    :color="exp.status === 'GENERATING' ? 'blue' : exp.status === 'COMPLETED' ? 'green' : 'red'"
                    :label="exp.status"
                  />
                </td>
                <td class="text-center">
                  <q-btn
                    v-if="exp.status === 'COMPLETED' && exp.storageUrl"
                    flat dense icon="download" color="green-5"
                    :href="exp.storageUrl" target="_blank"
                  >
                    <q-tooltip>Download</q-tooltip>
                  </q-btn>
                  <q-btn flat dense icon="delete_outline" color="red-4" @click="deleteExport(exp)">
                    <q-tooltip>Delete</q-tooltip>
                  </q-btn>
                </td>
              </tr>
            </tbody>
          </q-markup-table>
          <div v-else class="text-grey-5 text-center q-pa-lg">No exports yet</div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Invoice Upload Dialog -->
    <q-dialog v-model="showInvoiceDialog">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Upload Invoice</div>
          <div v-if="invoiceTarget" class="text-caption text-grey-5 q-mt-xs">
            Transaction #{{ invoiceTarget.rowIndex + 1 }}: {{ invoiceTarget.counterparty || invoiceTarget.description }}
          </div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-file v-model="invoiceFile" label="Select PDF/image" dense outlined dark accept=".pdf,.jpg,.jpeg,.png" />
          <q-select v-model="invoiceType" :options="invoiceTypeOptions" emit-value map-options label="Type" dense outlined dark />
          <q-input v-model="invoiceDescription" label="Description (optional)" dense outlined dark />

          <div v-if="transactionInvoices.length" class="q-mt-md">
            <div class="text-subtitle2 text-grey-5 q-mb-xs">Existing Invoices</div>
            <q-list dense bordered class="rounded-borders">
              <q-item v-for="inv in transactionInvoices" :key="inv.id">
                <q-item-section avatar>
                  <q-icon name="receipt" color="blue-4" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-white">{{ inv.originalFilename }}</q-item-label>
                  <q-item-label caption>{{ inv.invoiceType }}</q-item-label>
                </q-item-section>
                <q-item-section side>
                  <div>
                    <q-btn flat dense icon="download" color="green-5" :href="inv.storageUrl" target="_blank" />
                    <q-btn flat dense icon="delete" color="red-4" @click="deleteInvoice(inv)" />
                  </div>
                </q-item-section>
              </q-item>
            </q-list>
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Upload" color="green-7" no-caps @click="handleInvoiceUpload" :loading="uploadingInvoice" :disable="!invoiceFile" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { financeReportApi, financeTransactionApi, financeInvoiceApi, financeRuleApi, financeExportApi } from 'src/api/finance'
import { useFinanceStore } from 'src/stores/financeStore'

const $q = useQuasar()
const route = useRoute()
const financeStore = useFinanceStore()

// Loading overlay helper
function showLoading(message = 'Processing...') {
  $q.loading.show({
    message,
    spinnerSize: 40,
    spinnerColor: 'green',
    messageColor: 'white',
    backgroundColor: 'dark',
    customClass: 'fm-loading'
  })
}
function hideLoading() {
  $q.loading.hide()
}
const reportId = computed(() => Number(route.params.id))

const report = ref(null)
const transactions = ref([])
const uploadedFiles = ref([])
const loadingTx = ref(false)
const uploading = ref(false)
const txUploadFile = ref(null)
const rootAttachFiles = ref(null)
const folderAttachFiles = ref({})
const newFolderName = ref('')
const newFolderFiles = ref(null)
const showFileManager = ref(true)
const showNewFolder = ref(false)
const openFolders = ref({})
const emptyFolders = ref([])
const renamingFolder = ref(null)
const renameValue = ref('')
const newSubfolderParent = ref(null)
const newSubfolderName = ref('')
const folderInputRefs = ref({})
const dragFile = ref(null)
const dragOverTarget = ref(null)
const activeTab = ref(null)
const completedFilter = ref('all')
const accounts = computed(() => financeStore.accounts)

const filteredTransactions = computed(() => {
  if (completedFilter.value === 'done') return transactions.value.filter(t => t.completed)
  if (completedFilter.value === 'pending') return transactions.value.filter(t => !t.completed)
  return transactions.value
})

const hasTransactionFile = computed(() =>
  currentAccountFiles.value.some(f => f.fileType === 'TRANSACTION')
)

const visibleColumns = ref([])

// Default important columns per account type (max ~10 most useful for accounting)
const DEFAULT_COLUMNS = {
  SPARKASSE: ['Buchungstag', 'Beguenstigter/Zahlungspflichtiger', 'Verwendungszweck', 'Betrag', 'Waehrung', 'amount', 'note', 'category', 'subcategory', 'invoice', 'completed'],
  PAYPAL: ['Date', 'Name', 'Type', 'Currency', 'Gross', 'Fee', 'Net', 'Balance Impact', 'amount', 'note', 'category', 'invoice', 'completed'],
  WISE_MAIN: ['Date', 'Amount', 'Currency', 'Description', 'Running Balance', 'Transaction Type', 'note', 'category', 'subcategory', 'invoice', 'completed'],
  WISE_SUB: ['Date', 'Amount', 'Currency', 'Description', 'Running Balance', 'Transaction Type', 'note', 'category', 'subcategory', 'invoice', 'completed'],
  VIVID: ['Completed date', 'Counterparty name', 'Reference', 'Payment amount', 'Payment currency', 'amount', 'note', 'category', 'subcategory', 'invoice', 'completed'],
  PAYONEER: ['Transaction date', 'Credit amount', 'Debit amount', 'Status', 'Running balance', 'Description', 'Currency', 'amount', 'note', 'category', 'invoice', 'completed'],
  AMAZON_SELLER: ['Date', 'Transaction type', 'Order ID', 'Product Details', 'Total product charges', 'Amazon fees', 'Other', 'amount', 'note', 'category', 'invoice', 'completed'],
  AMAZON_VAT: ['TAX_CALCULATION_DATE', 'MARKETPLACE', 'TRANSACTION_TYPE', 'ITEM_DESCRIPTION', 'TOTAL_ACTIVITY_VALUE_AMT_VAT_INCL', 'TRANSACTION_CURRENCY_CODE', 'amount', 'note', 'category', 'invoice', 'completed'],
  CASH: ['amount', 'note', 'category', 'subcategory', 'invoice', 'completed']
}

// Get the current account's type
const currentAccountType = computed(() => {
  if (!activeTab.value) return null
  const acc = accounts.value.find(a => a.id === activeTab.value)
  return acc?.accountType || null
})

function getDefaultColumnsForAccount() {
  const type = currentAccountType.value
  return DEFAULT_COLUMNS[type] || null
}

// Export history
const exportHistory = ref([])
const showExportHistory = ref(false)
let exportPollTimer = null

// Invoice dialog
const invoiceCountMap = ref({})
const showInvoiceDialog = ref(false)
const invoiceTarget = ref(null)
const invoiceFile = ref(null)
const invoiceType = ref('INVOICE')
const invoiceDescription = ref('')
const uploadingInvoice = ref(false)
const transactionInvoices = ref([])
const invoiceTypeOptions = [
  { label: 'Invoice', value: 'INVOICE' },
  { label: 'Receipt', value: 'RECEIPT' },
  { label: 'Credit Note', value: 'CREDIT_NOTE' },
  { label: 'Amazon Fee', value: 'AMAZON_FEE' }
]

const monthNames = ['', 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
function monthName(m) { return monthNames[m] || m }
function statusColor(s) {
  return { DRAFT: 'blue-grey-7', REVIEWED: 'blue-7', SHARED: 'green-7', ARCHIVED: 'grey-7' }[s] || 'grey-7'
}

const currentAccountFiles = computed(() =>
  uploadedFiles.value.filter(f => f.accountId === activeTab.value)
)

const rootFiles = computed(() =>
  currentAccountFiles.value.filter(f => !f.subfolder)
)

// All unique folder paths from files + empty folders
const allFolderPaths = computed(() => {
  const folders = new Set()
  for (const f of currentAccountFiles.value) {
    if (f.subfolder) {
      folders.add(f.subfolder)
      // Also add parent if it's a nested path
      const slash = f.subfolder.indexOf('/')
      if (slash > 0) folders.add(f.subfolder.substring(0, slash))
    }
  }
  for (const f of emptyFolders.value) folders.add(f)
  return Array.from(folders).sort()
})

// Top-level folders (no slash)
const topLevelFolders = computed(() =>
  allFolderPaths.value.filter(f => !f.includes('/'))
)

// Child folders of a parent
function childFolders(parent) {
  return allFolderPaths.value.filter(f => f.startsWith(parent + '/') && !f.substring(parent.length + 1).includes('/'))
}

// Files directly in a folder (not in sub-subfolders)
function directFilesInFolder(folder) {
  return currentAccountFiles.value.filter(f => f.subfolder === folder)
}

// All files in a folder tree (including sub-subfolders)
function filesInFolderTree(folder) {
  return currentAccountFiles.value.filter(f => f.subfolder === folder || (f.subfolder && f.subfolder.startsWith(folder + '/')))
}

// Display name for subfolder (last segment)
function subfolderDisplayName(path) {
  const slash = path.lastIndexOf('/')
  return slash >= 0 ? path.substring(slash + 1) : path
}

// Keep old name for compatibility
function filesByFolder(folder) {
  return directFilesInFolder(folder)
}

const tableColumns = computed(() => {
  if (!transactions.value.length) return []
  const rawKeySet = new Set()
  for (const tx of transactions.value) {
    if (tx.rawData && typeof tx.rawData === 'object') {
      for (const k of Object.keys(tx.rawData)) {
        rawKeySet.add(k)
      }
    }
  }
  const cols = Array.from(rawKeySet).map(k => ({
    name: k, label: k, field: row => row.rawData?.[k] ?? '', align: 'left', sortable: true
  }))
  cols.push(
    { name: 'amount', label: 'Amount', field: 'amount', align: 'right', sortable: true },
    { name: 'note', label: 'Note', field: 'note', align: 'left', style: 'min-width: 500px' },
    { name: 'category', label: 'Category', field: 'category', align: 'left' },
    { name: 'subcategory', label: 'Subcategory', field: 'subcategory', align: 'left' },
    { name: 'invoice', label: 'Actions', field: 'invoice', align: 'center' },
    { name: 'completed', label: 'Done', field: 'completed', align: 'center' }
  )
  return cols
})

const allColumnOptions = computed(() =>
  tableColumns.value.map(c => ({ label: c.label || c.name, value: c.name }))
)

function toggleColumn(colName) {
  const idx = visibleColumns.value.indexOf(colName)
  if (idx >= 0) {
    visibleColumns.value = visibleColumns.value.filter(c => c !== colName)
  } else {
    visibleColumns.value = [...visibleColumns.value, colName]
  }
}

function showAllColumns() {
  visibleColumns.value = tableColumns.value.map(c => c.name)
}

function showDefaultColumns() {
  applyDefaultColumns()
}

function applyDefaultColumns() {
  const defaults = getDefaultColumnsForAccount()
  const allNames = tableColumns.value.map(c => c.name)
  if (defaults) {
    // Show only default columns that exist in current data
    visibleColumns.value = defaults.filter(d => allNames.includes(d))
    // Always ensure at least note/category/invoice are visible
    for (const must of ['note', 'category', 'invoice', 'completed']) {
      if (allNames.includes(must) && !visibleColumns.value.includes(must)) {
        visibleColumns.value.push(must)
      }
    }
  } else {
    // Unknown type: show first 7 raw columns + note/category/subcategory/invoice
    const rawCols = allNames.filter(n => !['amount', 'note', 'category', 'subcategory', 'invoice', 'completed'].includes(n))
    visibleColumns.value = [
      ...rawCols.slice(0, 7),
      'amount', 'note', 'category', 'invoice'
    ].filter(n => allNames.includes(n))
  }
}

function ensureAlwaysVisible(cols) {
  // These columns should always be visible
  const must = ['completed', 'invoice']
  const allNames = tableColumns.value.map(c => c.name)
  for (const m of must) {
    if (allNames.includes(m) && !cols.includes(m)) {
      cols.push(m)
    }
  }
  return cols
}

function updateVisibleColumns() {
  const cols = tableColumns.value
  if (!cols.length) return

  const saved = localStorage.getItem(`fin_cols_${activeTab.value}`)
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      const colNames = cols.map(c => c.name)
      const validSaved = parsed.filter(n => colNames.includes(n))
      if (validSaved.length) {
        visibleColumns.value = ensureAlwaysVisible(validSaved)
        return
      }
    } catch { /* fall through */ }
  }
  // No saved or invalid: apply defaults
  applyDefaultColumns()
}

watch(tableColumns, () => updateVisibleColumns())

watch(visibleColumns, (v) => {
  if (activeTab.value && v.length) {
    localStorage.setItem(`fin_cols_${activeTab.value}`, JSON.stringify(v))
  }
})

async function loadReport() {
  try {
    const res = await financeReportApi.getById(reportId.value)
    report.value = res.data.data
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to load report' })
  }
}

async function loadFiles() {
  try {
    const res = await financeTransactionApi.getFilesByReport(reportId.value)
    uploadedFiles.value = res.data.data
    // Auto-expand all existing subfolders
    for (const f of uploadedFiles.value) {
      if (f.subfolder && !(f.subfolder in openFolders.value)) {
        openFolders.value[f.subfolder] = true
      }
    }
  } catch { /* ignore */ }
}

async function loadTransactions() {
  if (!activeTab.value) return
  loadingTx.value = true
  try {
    const res = await financeTransactionApi.getByReportAndAccount(reportId.value, activeTab.value)
    transactions.value = res.data.data
    loadInvoiceCounts()
  } finally {
    loadingTx.value = false
  }
}

async function loadInvoiceCounts() {
  try {
    const res = await financeInvoiceApi.getByReport(reportId.value)
    const counts = {}
    for (const inv of (res.data.data || [])) {
      if (inv.transactionId) {
        counts[inv.transactionId] = (counts[inv.transactionId] || 0) + 1
      }
    }
    invoiceCountMap.value = counts
  } catch { /* ignore */ }
}

watch(activeTab, (newVal) => {
  if (newVal) loadTransactions()
})

watch(accounts, (accs) => {
  if (accs.length && !activeTab.value) {
    activeTab.value = accs[0].id
  }
}, { immediate: false })

async function uploadFiles(fileType, subfolder, files) {
  uploading.value = true
  const fileList = Array.isArray(files) ? files : [files]
  showLoading(`Uploading ${fileList.length} file${fileList.length > 1 ? 's' : ''}...`)
  try {
    const acctId = activeTab.value
    for (let i = 0; i < fileList.length; i++) {
      if (fileList.length > 1) showLoading(`Uploading file ${i + 1} of ${fileList.length}...`)
      await financeTransactionApi.uploadFile(reportId.value, acctId, fileType, subfolder, fileList[i])
    }
    await loadFiles()
    if (fileType === 'TRANSACTION') await loadTransactions()
    $q.notify({ type: 'positive', message: fileList.length > 1 ? `${fileList.length} files uploaded` : 'File uploaded' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Upload failed' })
  } finally {
    uploading.value = false
    hideLoading()
  }
}

async function handleTxUpload() {
  if (!txUploadFile.value) return
  await uploadFiles('TRANSACTION', null, txUploadFile.value)
  txUploadFile.value = null
}

async function handleAttachUpload(folder) {
  const files = folder ? folderAttachFiles.value[folder] : rootAttachFiles.value
  if (!files?.length) return
  await uploadFiles('ATTACHMENT', folder, files)
  if (folder) { folderAttachFiles.value[folder] = null } else { rootAttachFiles.value = null }
}

async function handleNewFolderUpload() {
  if (!newFolderName.value || !newFolderFiles.value?.length) return
  await uploadFiles('ATTACHMENT', newFolderName.value.trim(), newFolderFiles.value)
  newFolderName.value = ''
  newFolderFiles.value = null
}

function toggleFolder(folder) {
  openFolders.value[folder] = !openFolders.value[folder]
}

function createEmptyFolder() {
  if (!newFolderName.value) return
  const name = newFolderName.value.trim()
  if (!emptyFolders.value.includes(name)) {
    emptyFolders.value.push(name)
  }
  openFolders.value[name] = true
  showNewFolder.value = false
  newFolderName.value = ''
}

function startRename(folder) {
  renamingFolder.value = folder
  renameValue.value = subfolderDisplayName(folder)
}

function cancelRename() {
  renamingFolder.value = null
  renameValue.value = ''
}

async function submitRename(oldPath) {
  if (!renameValue.value.trim()) { cancelRename(); return }
  const slash = oldPath.lastIndexOf('/')
  const newPath = slash >= 0 ? oldPath.substring(0, slash + 1) + renameValue.value.trim() : renameValue.value.trim()
  if (newPath === oldPath) { cancelRename(); return }
  try {
    await financeTransactionApi.renameFolder(reportId.value, activeTab.value, oldPath, newPath)
    // Update empty folders list
    const idx = emptyFolders.value.indexOf(oldPath)
    if (idx >= 0) emptyFolders.value[idx] = newPath
    // Transfer open state
    openFolders.value[newPath] = openFolders.value[oldPath]
    delete openFolders.value[oldPath]
    await loadFiles()
    $q.notify({ type: 'positive', message: 'Folder renamed' })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to rename' })
  }
  cancelRename()
}

function startNewSubfolder(parent) {
  newSubfolderParent.value = parent
  newSubfolderName.value = ''
  openFolders.value[parent] = true
}

function createSubfolder(parent) {
  if (!newSubfolderName.value) return
  const path = parent + '/' + newSubfolderName.value.trim()
  if (!emptyFolders.value.includes(path)) {
    emptyFolders.value.push(path)
  }
  openFolders.value[path] = true
  newSubfolderParent.value = null
  newSubfolderName.value = ''
}

function triggerFolderUpload(folder) {
  const input = folderInputRefs.value[folder]
  if (input) input.click()
}

async function onFolderFileChange(event, folder) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  await uploadFiles('ATTACHMENT', folder, files)
  event.target.value = ''
}

function onDragStart(event, file) {
  dragFile.value = file
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', file.id.toString())
}

function onDragOver(event, target) {
  if (!dragFile.value) return
  event.dataTransfer.dropEffect = 'move'
  dragOverTarget.value = target
}

function onDragEnd() {
  dragFile.value = null
  dragOverTarget.value = null
}

async function onDrop(targetFolder) {
  dragOverTarget.value = null
  if (!dragFile.value) return
  const file = dragFile.value
  dragFile.value = null

  // Don't move if already in the same folder
  const currentFolder = file.subfolder || null
  if (currentFolder === targetFolder) return

  try {
    await financeTransactionApi.moveFile(file.id, targetFolder)
    await loadFiles()
    $q.notify({ type: 'positive', message: `Moved to ${targetFolder || 'root'}` })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to move file' })
  }
}

async function deleteUploadedFile(f) {
  try {
    await financeTransactionApi.deleteFile(f.id)
    await loadFiles()
    await loadTransactions()
    $q.notify({ type: 'positive', message: 'File deleted' })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to delete file' })
  }
}

async function toggleCompleted(row) {
  try {
    const res = await financeTransactionApi.toggleCompleted(row.id)
    row.completed = res.data.data.completed
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to update' })
  }
}

async function saveTransaction(row) {
  try {
    await financeTransactionApi.update(row.id, {
      note: row.note, category: row.category, subcategory: row.subcategory
    })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to save' })
  }
}

async function reExtract() {
  showLoading('Re-parsing transactions...')
  try {
    const res = await financeTransactionApi.reExtract(reportId.value)
    $q.notify({ type: 'positive', message: `Re-extracted ${res.data.data} transactions` })
    await loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Re-extract failed' })
  } finally {
    hideLoading()
  }
}

async function runCategorize() {
  showLoading('Auto-categorizing transactions...')
  try {
    const res = await financeRuleApi.categorize(reportId.value)
    $q.notify({ type: 'positive', message: `Categorized ${res.data.data} transactions` })
    await loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Categorization failed' })
  } finally {
    hideLoading()
  }
}

async function resetRow(row) {
  try {
    await financeTransactionApi.resetOne(row.id)
    row.category = null
    row.subcategory = null
    row.note = null
    row.autoCategorized = false
    row.manuallyReviewed = false
    row.completed = false
    $q.notify({ type: 'positive', message: 'Transaction reset' })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to reset' })
  }
}

async function resetAll() {
  $q.dialog({
    title: 'Reset All Transactions',
    message: 'This will clear all categories, notes, and manual review flags for ALL transactions in this report. Are you sure?',
    cancel: true,
    color: 'orange'
  }).onOk(async () => {
    showLoading('Resetting all transactions...')
    try {
      const res = await financeTransactionApi.resetAll(reportId.value)
      $q.notify({ type: 'positive', message: `Reset ${res.data.data} transactions` })
      await loadTransactions()
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to reset' })
    } finally {
      hideLoading()
    }
  })
}

async function downloadExcel(accountId) {
  showLoading('Generating Excel file...')
  try {
    const res = await financeExportApi.exportExcel(reportId.value, accountId)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'transactions.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    $q.notify({ type: 'negative', message: 'Export failed' })
  } finally {
    hideLoading()
  }
}

async function downloadZip() {
  showLoading('Generating ZIP file...')
  try {
    const res = await financeExportApi.exportZip(reportId.value)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'monthly_report.zip'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    $q.notify({ type: 'negative', message: 'Export failed' })
  } finally {
    hideLoading()
  }
}

async function openInvoiceUpload(row) {
  invoiceTarget.value = row
  invoiceFile.value = null
  invoiceType.value = 'INVOICE'
  invoiceDescription.value = ''
  showInvoiceDialog.value = true
  try {
    const res = await financeInvoiceApi.getByTransaction(row.id)
    transactionInvoices.value = res.data.data
  } catch { transactionInvoices.value = [] }
}

async function handleInvoiceUpload() {
  if (!invoiceFile.value) return
  uploadingInvoice.value = true
  showLoading('Uploading invoice...')
  try {
    await financeInvoiceApi.upload(reportId.value, invoiceTarget.value?.id, invoiceType.value, invoiceDescription.value, invoiceFile.value)
    invoiceFile.value = null
    const res = await financeInvoiceApi.getByTransaction(invoiceTarget.value.id)
    transactionInvoices.value = res.data.data
    loadInvoiceCounts()
    $q.notify({ type: 'positive', message: 'Invoice uploaded' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Upload failed' })
  } finally {
    uploadingInvoice.value = false
    hideLoading()
  }
}

async function deleteInvoice(inv) {
  try {
    await financeInvoiceApi.delete(inv.id)
    transactionInvoices.value = transactionInvoices.value.filter(i => i.id !== inv.id)
    loadInvoiceCounts()
    $q.notify({ type: 'positive', message: 'Invoice deleted' })
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to delete' })
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(i > 0 ? 1 : 0)} ${units[i]}`
}

async function loadExportHistory() {
  try {
    const res = await financeExportApi.getHistory(reportId.value)
    exportHistory.value = res.data.data || []
    startExportPollIfNeeded()
  } catch {
    exportHistory.value = []
  }
}

function startExportPollIfNeeded() {
  stopExportPoll()
  const hasGenerating = exportHistory.value.some(e => e.status === 'GENERATING')
  if (hasGenerating && showExportHistory.value) {
    exportPollTimer = setInterval(async () => {
      await loadExportHistory()
    }, 3000)
  }
}

function stopExportPoll() {
  if (exportPollTimer) {
    clearInterval(exportPollTimer)
    exportPollTimer = null
  }
}

async function generateZipPackage() {
  showLoading('Generating ZIP package...')
  try {
    await financeExportApi.generateZip(reportId.value)
    $q.notify({ type: 'positive', message: 'ZIP package generated! Check Export History to download.' })
    loadExportHistory()
    showExportHistory.value = true
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to generate ZIP package' })
  } finally {
    hideLoading()
  }
}

async function deleteExport(exp) {
  $q.dialog({
    title: 'Delete Export',
    message: 'Are you sure you want to delete this export?',
    cancel: true,
    color: 'red'
  }).onOk(async () => {
    try {
      await financeExportApi.deleteExport(exp.id)
      exportHistory.value = exportHistory.value.filter(e => e.id !== exp.id)
      $q.notify({ type: 'positive', message: 'Export deleted' })
    } catch {
      $q.notify({ type: 'negative', message: 'Failed to delete export' })
    }
  })
}

onUnmounted(() => {
  stopExportPoll()
})

onMounted(async () => {
  await Promise.all([
    financeStore.fetchAccounts(),
    loadReport(),
    loadFiles()
  ])
  if (accounts.value.length && !activeTab.value) {
    activeTab.value = accounts.value[0].id
  }
})
</script>

<style scoped>
.fm {
  border: 1px solid var(--erp-border, rgba(255,255,255,0.1));
  border-radius: 8px;
  overflow: hidden;
  background: var(--erp-bg-tertiary, rgba(255,255,255,0.02));
}
.fm-header {
  border-bottom: 1px solid var(--erp-border, rgba(255,255,255,0.08));
  background: rgba(255,255,255,0.02);
}
.fm-row {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  transition: background 0.1s;
  min-height: 40px;
}
.fm-row:hover {
  background: rgba(255,255,255,0.04);
}
.fm-upload-row {
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.fm-draggable {
  cursor: grab;
}
.fm-draggable:active {
  cursor: grabbing;
  opacity: 0.4;
}
.fm-grip {
  opacity: 0.3;
  transition: opacity 0.15s;
}
.fm-draggable:hover .fm-grip {
  opacity: 1;
}
.fm-name {
  color: white;
  font-size: 0.85rem;
  line-height: 1.3;
}
.fm-meta {
  color: rgba(255,255,255,0.4);
  font-size: 0.7rem;
}
.fm-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}
.fm-row:hover .fm-actions,
.fm-folder-content .fm-row:hover .fm-actions {
  opacity: 1;
}
.fm-folder-header {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  transition: background 0.15s, box-shadow 0.15s;
}
.fm-folder-header:hover {
  background: rgba(255,255,255,0.04);
}
.fm-folder-content {
  background: rgba(0,0,0,0.1);
  padding-left: 24px;
  border-left: 1px solid rgba(255,255,255,0.06);
  margin-left: 16px;
}
.fm-drop-zone {
  transition: background 0.15s, box-shadow 0.15s;
  border: 2px solid transparent;
  border-radius: 4px;
  margin: 1px;
}
.fm-drop-active {
  background: rgba(76, 175, 80, 0.1) !important;
  border-color: rgba(76, 175, 80, 0.5);
  box-shadow: inset 0 0 12px rgba(76, 175, 80, 0.15);
}
.hidden {
  display: none;
}
</style>
