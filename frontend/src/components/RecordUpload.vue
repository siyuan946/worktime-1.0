<template>
  <div class="record-upload">
    <section class="section-card">
      <h2 class="h5">Excel上传</h2>
      <div class="upload-toolbar screen-only">
        <div class="toolbar-row">
          <div class="toolbar-block flex-grow-1">
            <label class="form-label mb-1">选择Excel文件</label>
            <div class="d-flex flex-wrap gap-2 align-items-center">
              <input
                ref="fileInput"
                class="form-control"
                type="file"
                @change="onFileChange"
                :disabled="loading || printing"
              >
              <button class="btn btn-outline-primary" @click="parse" :disabled="!file || loading || printing">解析</button>
              <div class="spinner-border" v-if="loading"></div>
            </div>
          </div>
          <div class="toolbar-block">
            <label class="form-label mb-1">快速操作</label>
            <div class="d-flex flex-wrap gap-2">
              <button class="btn btn-outline-warning" @click="deleteZero" :disabled="!hasPreview || loading || printing">清除0工序</button>
              <button class="btn btn-primary" @click="save" :disabled="!hasPreview || loading || printing">保存</button>
              <button class="btn btn-secondary" @click="print" :disabled="!hasPreview || loading || printing">打印</button>
            </div>
          </div>
        </div>
        <div class="toolbar-row">
          <div class="toolbar-block flex-grow-1">
            <label class="form-label mb-1">历史文件</label>
            <div class="d-flex flex-wrap gap-2 align-items-center">
              <select class="form-select" v-model="selectedFileId" :disabled="loading || printing">
                <option value="" disabled>选择历史文件</option>
                <option v-for="f in files" :key="f.id" :value="f.id">
                  {{ f.fileName }} ({{ f.uploadTime ? f.uploadTime.slice(0, 10) : '' }})
                </option>
              </select>
              <div class="btn-group">
                <button class="btn btn-outline-secondary" @click="load" :disabled="!selectedFileId || loading || printing">加载</button>
                <button class="btn btn-outline-danger" @click="remove" :disabled="!selectedFileId || loading || printing">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="progress mb-2 screen-only" v-if="showProgress" style="height: 0.75rem;">
        <div class="progress-bar" role="progressbar" :style="{ width: parseProgress + '%' }">
          {{ parseProgress }}%
        </div>
      </div>
      <div class="screen-only text-muted small" v-if="statusMessage">
        {{ statusMessage }}
      </div>

      <div class="alert alert-info py-2 px-3 screen-only" v-if="summary">
        <span>共 {{ summary.total || 0 }} 条记录</span>
        <span class="ms-3">缺少工序码：{{ summary.codeMissing || 0 }}</span>
        <span class="ms-3">缺少工时：{{ summary.hoursMissing || 0 }}</span>
      </div>

      <div class="screen-only" v-if="printing">
        <div class="alert alert-info py-2 px-3 mb-3">
          正在准备打印数据，请稍候...
        </div>
      </div>

      <div v-if="hasPreview" id="preview-table" class="screen-only preview-container">
        <div class="navigation-toolbar mb-3">
          <div class="nav-section">
            <div class="nav-label">当前图号</div>
            <div class="nav-value">{{ currentPageInfo?.drawingNumber || '—' }}</div>
            <div class="nav-meta" v-if="drawings.length">（{{ drawingPositionText }}，共 {{ currentDrawingCount }} 条）</div>
            <div class="nav-meta" v-if="currentDrawingExcelRow">原始Excel第 {{ currentDrawingExcelRow }} 行</div>
            <div class="btn-group btn-group-sm">
              <button class="btn btn-outline-secondary" @click="prevDrawing" :disabled="!canPrevDrawing || loading || printing">上一图号</button>
              <button class="btn btn-outline-secondary" @click="nextDrawing" :disabled="!canNextDrawing || loading || printing">下一图号</button>
            </div>
          </div>
          <div class="nav-section">
            <div class="nav-label">分页</div>
            <div class="nav-value">第 {{ currentPageDisplay }} / {{ pageTotalPages || 1 }} 页</div>
            <div class="btn-group btn-group-sm">
              <button class="btn btn-outline-secondary" @click="prevPage" :disabled="!canPrevPage || loading || printing">上一页</button>
              <button class="btn btn-outline-secondary" @click="nextPage" :disabled="!canNextPage || loading || printing">下一页</button>
            </div>
          </div>
          <div class="nav-section flex-grow-1">
            <div class="nav-label">快速跳转</div>
            <div class="input-group input-group-sm">
              <input class="form-control" placeholder="输入图号关键词" v-model.trim="drawingSearch" @keyup.enter="jumpToDrawing" :disabled="loading || printing">
              <button class="btn btn-outline-secondary" @click="jumpToDrawing" :disabled="loading || printing">跳转</button>
            </div>
          </div>
        </div>

        <table class="table table-bordered table-sm table-striped mb-0">
          <thead>
            <tr>
              <th class="notification-col">通知单号</th>
              <th class="no-print">产品名称</th>
              <th class="drawing-col">图号</th>
              <th class="print-only plan-col">计划数</th>
              <th class="no-print">名称</th>
              <th class="plan-col no-print">计划数</th>
              <th class="hours-col">单件工时</th>
              <th class="no-print">工序代码</th>
              <th class="process-col">工序</th>
              <th class="print-only">人员代码</th>
              <th class="print-only">合格件数</th>
              <th class="print-only">起始时间</th>
              <th class="print-only">结束时间</th>
              <th class="print-only">检验员</th>
              <th class="barcode-cell">条形码</th>
              <th class="no-print"></th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="(record, index) in pageRecords"
              :key="record.id || record._localKey"
              :class="{ 'table-danger': record.codeMissing || record.hoursMissing }"
            >
              <td class="notification-col">{{ record.notificationNumber }}</td>
              <td class="no-print">{{ record.productName }}</td>
              <td class="drawing-col">{{ record.drawingNumber }}</td>
              <td class="print-only plan-col">{{ record.planQty }}</td>
              <td class="no-print">{{ record.partName }}</td>
              <td class="plan-col no-print">
                <input
                  type="number"
                  class="form-control form-control-sm"
                  v-model.number="record.planQty"
                  @change="markDirty(record)"
                />
                <span class="print-text">{{ record.planQty }}</span>
              </td>
              <td class="hours-col">
                <input
                  type="number"
                  class="form-control form-control-sm no-print"
                  style="width: 80px"
                  v-model.number="record.hours"
                  @blur="checkHours(record)"
                  @change="markDirty(record)"
                />
                <span class="print-text">{{ record.hours }}</span>
              </td>
              <td class="no-print">{{ record.processCode }}</td>
              <td class="process-col">
                <input
                  class="form-control form-control-sm no-print"
                  v-model="record.processName"
                  @blur="updateProcess(record)"
                />
                <span class="print-text">{{ record.processName }}</span>
              </td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="print-only"></td>
              <td class="barcode-cell">
                <div>{{ record.barcode }}</div>
                <img v-if="record.barcodeImage" :src="'data:image/png;base64,' + record.barcodeImage" />
              </td>
              <td class="no-print">
                <button class="btn btn-sm btn-outline-primary me-1" @click="addRow(index)">新增</button>
                <button class="btn btn-sm btn-outline-danger" @click="deleteRow(index)">删除</button>
              </td>
            </tr>

          </tbody>
        </table>
      </div>
    </section>

    <div v-if="printPages.length" id="print-area" class="print-area">
      <div v-for="page in printPages" :key="page.key" class="print-page">
        <div class="print-page-header">
          <div class="print-title">{{ currentFileName || '工时记录' }}</div>
          <div class="print-meta">
            图号：{{ page.drawingNumber || '—' }}（第 {{ page.pageNumber }} / {{ page.totalPages || 1 }} 页）
          </div>
          <div class="print-meta" v-if="page.sourceRowNumber">原始Excel第 {{ page.sourceRowNumber }} 行</div>
        </div>
        <table class="table table-bordered table-sm table-striped mb-4">
          <thead>
            <tr>
              <th class="notification-col">通知单号</th>
              <th>产品名称</th>
              <th class="drawing-col">图号</th>
              <th>名称</th>
              <th class="plan-col">计划数</th>
              <th class="hours-col">单件工时</th>
              <th>工序代码</th>
              <th class="process-col">工序</th>
              <th>人员代码</th>
              <th>合格件数</th>
              <th>起始时间</th>
              <th>结束时间</th>
              <th>检验员</th>
              <th class="barcode-cell">条形码</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in page.records" :key="record.id || record._localKey">
              <td class="notification-col">{{ record.notificationNumber }}</td>
              <td>{{ record.productName }}</td>
              <td class="drawing-col">{{ record.drawingNumber }}</td>
              <td>{{ record.partName }}</td>
              <td class="plan-col">{{ record.planQty }}</td>
              <td class="hours-col">{{ record.hours }}</td>
              <td>{{ record.processCode }}</td>
              <td class="process-col">{{ record.processName }}</td>
              <td>{{ record.workerCodes }}</td>
              <td>{{ record.qualifiedQty }}</td>
              <td>{{ record.startTime }}</td>
              <td>{{ record.endTime }}</td>
              <td>{{ record.inspector }}</td>
              <td class="barcode-cell">
                <div>{{ record.barcode }}</div>
                <img v-if="record.barcodeImage" :src="'data:image/png;base64,' + record.barcodeImage" />
              </td>
            </tr>
            <tr v-for="n in page.blankCount" :key="page.key + '-blank-' + n" class="blank-row">
              <td class="notification-col">&nbsp;</td>
              <td>&nbsp;</td>
              <td class="drawing-col">&nbsp;</td>
              <td>&nbsp;</td>
              <td class="plan-col">&nbsp;</td>
              <td class="hours-col">&nbsp;</td>
              <td>&nbsp;</td>
              <td class="process-col">&nbsp;</td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
              <td class="barcode-cell"><div>&nbsp;</div></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      file: null,
      loading: false,
      fileId: null,
      files: [],
      selectedFileId: '',
      drawings: [],
      currentDrawingIndex: 0,
      pageNo: 0,
      rowsPerPage: 20,
      pageSize: 20,
      pageTotalPages: 0,
      pageRecords: [],
      drawingSearch: '',
      processCache: {},
      processCacheLoaded: false,
      barcodeCache: {},
      showProgress: false,
      parseProgress: 0,
      summary: null,
      printing: false,
      printPages: [],
      // ==== 新增字段 ====
      jobId: null,
      _poll: null,
      pollIntervalMs: 1500,
      importing: false,
      statusMessage: '',
      pollErrorCount: 0,
      pollErrorLimit: 3,
      lastProcessedRows: 0
    }
  },
  created() {
    this.fetchFiles()
    this.ensureProcessCache()
  },
  beforeDestroy() {
    // ==== 新增：组件销毁前停止轮询 ====
    this.stopPolling()
  },
  computed: {
    currentFileName() {
      if (this.file) return this.file.name
      const id = this.fileId || this.selectedFileId
      const f = this.files.find(x => x.id === id)
      return f ? f.fileName : ''
    },
    currentDrawing() {
      return this.drawings[this.currentDrawingIndex] || null
    },
    currentPageInfo() {
      if (!this.currentDrawing) return null
      return {
        drawingNumber: this.currentDrawing.drawing,
        count: this.currentDrawing.count
      }
    },
    hasPreview() {
      return (this.pageRecords && this.pageRecords.length > 0) || ((this.summary && (this.summary.total || 0) > 0))
    },
    currentPageDisplay() {
      if (!this.pageTotalPages) {
        return this.pageRecords.length ? 1 : 0
      }
      return Math.min(this.pageNo + 1, this.pageTotalPages)
    },
    drawingPositionText() {
      if (!this.drawings.length) return '0 / 0'
      return `${this.currentDrawingIndex + 1} / ${this.drawings.length}`
    },
    currentDrawingCount() {
      const drawing = this.currentDrawing
      return drawing && typeof drawing.count === 'number' ? drawing.count : 0
    },
    currentDrawingExcelRow() {
      const drawing = this.currentDrawing
      if (!drawing) return null
      const value = drawing.startRow != null ? drawing.startRow : drawing.minRow
      return typeof value === 'number' && value > 0 ? value : null
    },
    canPrevDrawing() {
      return this.currentDrawingIndex > 0
    },
    canNextDrawing() {
      return this.drawings.length && this.currentDrawingIndex < this.drawings.length - 1
    },
    canPrevPage() {
      return this.pageNo > 0
    },
    canNextPage() {
      if (!this.pageTotalPages) return false
      return this.pageNo < this.pageTotalPages - 1
    }
  },
  methods: {
    handleRequestError(error, fallback) {
      if (error instanceof Error && error.message === 'missing-user') return
      console.error(error)
      const response = error && error.response
      let message = null
      if (response && response.data != null) {
        const data = response.data
        if (typeof data === 'string') {
          message = data
        } else if (typeof data === 'object') {
          message = data.message || data.error || data.detail || null
          if (!message && Array.isArray(data.errors) && data.errors.length) {
            const first = data.errors[0]
            message = typeof first === 'string' ? first : (first && first.message) || null
          }
        }
      }
      if (!message && error && typeof error.message === 'string') {
        message = error.message
      }
      if (typeof message === 'string') {
        const trimmed = message.trim()
        const looksHtml = /<!DOCTYPE|<html|<body|<[^>]+>/i.test(trimmed)
        const defaultPhrases = [
          /^request failed with status code \d+$/i,
          /^network error$/i,
          /^timeout of \d+\s*ms exceeded$/i
        ]
        if (!trimmed || looksHtml || defaultPhrases.some(pattern => pattern.test(trimmed))) {
          message = ''
        } else {
          message = trimmed
        }
      }
      alert(message || fallback || '操作失败')
    },
    sanitize(text) {
      return text ? text.replace(/[^\x00-\x7F]/g, '').replace(/\s+/g, '') : ''
    },
    requireUserHeaders() {
      const user = (localStorage.getItem('username') || '').trim()
      if (!user) {
        alert('请先登录后再进行此操作')
        throw new Error('missing-user')
      }
      return { 'X-User': user }
    },
    clearFileSelection() {
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = ''
      }
    },
    onFileChange(e) { this.file = e.target.files[0] || null },
    async fetchFiles() {
      try {
        const res = await axios.get('/api/files')
        this.files = Array.isArray(res.data) ? res.data : []
      } catch (e) {
        console.error('加载文件列表失败', e)
      }
    },
    decorateRecord(raw) {
      const record = { ...raw }
      record.barcode = this.sanitize(record.barcode)
      record.barcodeImage = ''
      record.codeMissing = !record.processCode
      record.hoursMissing = record.hours == null
      record._dirty = false
      record._localKey = record.id ? `id-${record.id}` : `new-${Date.now()}-${Math.random().toString(36).slice(2)}`
      return record
    },
    async fetchDrawings() {
      if (!this.fileId) {
        this.drawings = []
        this.pageRecords = []
        this.pageTotalPages = 0
        return
      }
      try {
        const res = await axios.get(`/api/workrecords/file/${this.fileId}/drawings`)
        const items = Array.isArray(res.data) ? res.data : []
        this.drawings = items.map(item => {
          const count = item && typeof item.count === 'number' ? item.count : Number(item && item.count)
          const startRow = item && item.startRow != null ? Number(item.startRow) : (item && item.minRow != null ? Number(item.minRow) : null)
          return {
            drawing: item ? item.drawing : undefined,
            count: Number.isFinite(count) ? count : 0,
            startRow: Number.isFinite(startRow) && startRow > 0 ? startRow : null
          }
        })
        if (!this.drawings.length) {
          this.pageRecords = []
          this.pageTotalPages = 0
        }
        if (this.currentDrawingIndex >= this.drawings.length) {
          this.currentDrawingIndex = 0
        }
      } catch (e) {
        console.error('加载图号列表失败', e)
        this.drawings = []
        this.pageRecords = []
        this.pageTotalPages = 0
      }
    },
    async fetchPageData(drawingNumber, pageIndex, size) {
      if (!this.fileId) {
        return { content: [], totalPages: 0 }
      }
      const res = await axios.get(`/api/workrecords/file/${this.fileId}/page`, {
        params: { drawing: drawingNumber, page: pageIndex, size }
      })
      return (res && res.data) || { content: [], totalPages: 0 }
    },
    async fetchPage() {
      const drawing = this.currentDrawing
      if (!drawing) {
        this.pageRecords = []
        this.pageTotalPages = 0
        return
      }
      try {
        const data = await this.fetchPageData(drawing.drawing, this.pageNo, this.pageSize)
        const list = Array.isArray(data.content) ? data.content : []
        const baseCount = (drawing && typeof drawing.count === 'number' && drawing.count > 0)
          ? drawing.count
          : list.length
        const fallbackPages = baseCount > 0 ? Math.max(1, Math.ceil(baseCount / this.pageSize)) : 0
        const totalPages = (data && typeof data.totalPages === 'number' && data.totalPages > 0)
          ? data.totalPages
          : fallbackPages
        if (totalPages && this.pageNo >= totalPages) {
          this.pageNo = Math.max(totalPages - 1, 0)
          await this.fetchPage()
          return
        }
        this.pageTotalPages = totalPages || (list.length ? 1 : 0)
        this.pageRecords = list.map(item => this.decorateRecord(item))
        this.$nextTick(() => this.loadBarcodesForCurrentPage())
      } catch (e) {
        this.handleRequestError(e, '加载分页数据失败')
      }
    },
    async load() {
      if (!this.selectedFileId) return
      this.statusMessage = ''
      this.loading = true
      this.barcodeCache = {}
      this.summary = null
      try {
        this.fileId = this.selectedFileId
        await this.fetchDrawings()
        this.currentDrawingIndex = 0
        this.pageNo = 0
        await this.fetchPage()
      } finally {
        this.loading = false
      }
    },
    async remove() {
      if (!this.selectedFileId) return
      if (!confirm('删除该文件及其所有记录，确定删除?')) return
      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }
      this.statusMessage = ''
      this.loading = true
      try {
        await axios.delete(`/api/files/${this.selectedFileId}`, { headers })
        this.selectedFileId = ''
        this.fileId = null
        this.drawings = []
        this.pageRecords = []
        this.pageTotalPages = 0
        this.summary = null
        await this.fetchFiles()
        alert('已删除')
      } catch (e) {
        console.error(e)
        alert('删除失败')
      }
      this.loading = false
    },

    // ====== 这里是“异步导入”的新 parse 实现（替换原 parse）======
    async parse() {
      if (!this.file) return
      if (this.importing || this.loading) return

      // 如果你保留了“同名文件提醒”的逻辑，下面两行沿用原来的
      if (this.file) {
        const dup = this.files.find(f => f.fileName === this.file.name)
        if (dup) {
          const cont = confirm('发现同名文件，若内容相同请勿重复上传。继续上传?')
          if (!cont) return
        }
      }

      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }

      this.stopPolling()
      this.loading = true
      this.importing = true
      this.showProgress = true
      this.parseProgress = 5
      this.barcodeCache = {}
      this.summary = null
      this.statusMessage = '正在上传文件，请稍候…'
      this.pollErrorCount = 0
      this.lastProcessedRows = 0

      try {
        const data = new FormData()
        data.append('file', this.file)
        // 注意：Nginx 去掉 /api 前缀，故这里使用 /api/*
        const res = await axios.post('/api/workrecords/import', data, { headers })
        const { jobId, fileId } = res.data || {}
        this.jobId = jobId || null
        this.fileId = fileId || null
        this.selectedFileId = fileId ? String(fileId) : ''
        this.statusMessage = '解析任务已启动，正在获取进度…'
        // 启动轮询
        this.startPolling(jobId)
      } catch (e) {
        this.handleRequestError(e, '解析失败')
        this.showProgress = false
        this.loading = false
        this.importing = false
        this.statusMessage = '解析失败，请稍后重试'
      }
    },

    // ===== 新增：轮询导入状态 =====
    startPolling(jobId) {
      if (!jobId) {
        this.handleRequestError(new Error('missing-job'), '导入任务启动失败')
        this.showProgress = false
        this.loading = false
        this.importing = false
        this.statusMessage = '导入任务启动失败'
        return
      }
      this.stopPolling()

      this.statusMessage = '正在解析，请稍候…'
      this.pollErrorCount = 0
      this.lastProcessedRows = 0

      const tick = async () => {
        try {
          const res = await axios.get(`/api/workrecords/import/${encodeURIComponent(jobId)}/status`)
          const s = (res && res.data) || {}
          const status = s.status
          const processed = Number(s.processedRows || 0)
          const output = Number(s.outputRows || 0)
          const total = Number(s.totalRows || s.total || s.expectedRows || 0)
          const effective = Math.max(processed, output, 0)

          // 简单进度估算（没有总数，只做“看起来动”的估计）
          if (Number.isFinite(total) && total > 0) {
            const percent = Math.round((Math.min(effective, total) / total) * 100)
            this.parseProgress = Math.max(20, Math.min(99, percent))
            if (effective !== this.lastProcessedRows) {
              this.statusMessage = `已处理 ${effective} / ${total} 行`
            }
          } else {
            const est = Math.max(20, Math.min(95, 20 + Math.floor(Math.log10(effective + 1) * 30)))
            this.parseProgress = Math.max(this.parseProgress, est)
            if (effective !== this.lastProcessedRows) {
              this.statusMessage = `已处理 ${effective} 行`
            }
          }
          this.lastProcessedRows = effective
          this.pollErrorCount = 0

          if (status === 'COMPLETED') {
            this.stopPolling()
            this.parseProgress = 100

            // 刷新数据
            await this.fetchFiles()
            await this.fetchDrawings()
            this.currentDrawingIndex = 0
            this.pageNo = 0
            await this.fetchPage()

            // 清理状态
            this.file = null
            this.clearFileSelection()
            this.loading = false
            this.importing = false
            setTimeout(() => { this.showProgress = false }, 600)
            const completed = output || processed
            this.statusMessage = completed ? `解析完成，共导入 ${completed} 条记录` : '解析完成'
            alert('导入完成')
          } else if (status === 'FAILED') {
            this.stopPolling()
            this.loading = false
            this.importing = false
            this.showProgress = false
            this.statusMessage = s.message ? String(s.message) : '导入失败'
            this.handleRequestError(new Error(s.message || '导入失败'), '导入失败')
          } else {
            // RUNNING：继续轮询
          }
        } catch (e) {
          this.pollErrorCount += 1
          if (this.pollErrorCount >= this.pollErrorLimit) {
            this.stopPolling()
            this.loading = false
            this.importing = false
            this.showProgress = false
            this.statusMessage = '获取导入进度失败，请稍后重试'
            this.handleRequestError(e, '获取导入进度失败')
            return
          }
          if (this.statusMessage) {
            this.statusMessage = '网络波动，正在重试获取进度…'
          }
          console.warn('poll error', e && e.message)
        }
      }

      tick()
      this._poll = setInterval(tick, this.pollIntervalMs)
    },
    stopPolling() {
      if (this._poll) {
        clearInterval(this._poll)
        this._poll = null
      }
      this.pollErrorCount = 0
      this.lastProcessedRows = 0
    },
    // ====== 原有方法保持不变（以下均与你提供的版本一致）======
    markDirty(record) {
      if (!record) return
      record._dirty = true
    },
    async ensureProcessCache(force = false) {
      if (this.processCacheLoaded && !force) return
      try {
        const res = await axios.get('/api/processcodes')
        const map = {}
        if (Array.isArray(res.data)) {
          for (const item of res.data) {
            if (!item || !item.name || !item.code) continue
            const name = String(item.name).trim()
            const code = String(item.code).trim()
            if (!name || !code) continue
            map[name] = code
          }
        }
        this.processCache = map
        this.processCacheLoaded = true
      } catch (e) {
        console.error('加载工序缓存失败', e)
      }
    },
    async updateProcess(record, cacheReady = false) {
      if (!record) return
      if (!cacheReady) await this.ensureProcessCache()
      const rawName = record.processName || ''
      const name = rawName.trim()
      if (!name) {
        record.processCode = ''
        record.codeMissing = true
        await this.updateBarcode(record)
        this.markDirty(record)
        return
      }
      let code = this.processCache[name]
      if (!code) {
        try {
          const res = await axios.get(`/api/processcodes/name/${encodeURIComponent(name)}`)
          if (res.data && res.data.code) {
            code = String(res.data.code).trim()
            if (code) this.$set(this.processCache, name, code)
          }
        } catch (e) { /* ignore */ }
      }
      if (code) {
        record.processCode = code
        record.codeMissing = false
      } else {
        record.processCode = rawName
        record.codeMissing = true
      }
      await this.updateBarcode(record)
      this.markDirty(record)
    },
    async updateBarcode(record) {
      if (record.drawingNumber && record.notificationNumber && record.processCode) {
        const bar = `${record.drawingNumber}-${record.notificationNumber}-${record.processCode}`
        const clean = this.sanitize(bar)
        record.barcode = clean
        if (!clean) {
          record.barcodeImage = ''
          return
        }
        if (this.barcodeCache[clean]) {
          record.barcodeImage = this.barcodeCache[clean]
          return
        }
        try {
          const res = await axios.get('/api/workrecords/generateBarcode', { params: { text: bar } })
          if (res && res.data) {
            this.$set(this.barcodeCache, clean, res.data)
            record.barcodeImage = res.data
          } else {
            record.barcodeImage = ''
          }
        } catch (e) {
          console.error('获取条码失败', e)
          record.barcodeImage = ''
        }
      } else {
        record.barcode = ''
        record.barcodeImage = ''
      }
    },
    async loadBarcodes(records, cacheKey = '') {
      if (!records || !records.length) return
      if (!this._barcodeLoading) this._barcodeLoading = new Set()
      const key = cacheKey || `page-${Date.now()}`
      if (this._barcodeLoading.has(key)) return
      const missing = []
      for (const record of records) {
        const code = this.sanitize(record.barcode)
        if (!code) continue
        if (this.barcodeCache[code]) {
          if (record.barcodeImage !== this.barcodeCache[code]) {
            this.$set(record, 'barcodeImage', this.barcodeCache[code])
          }
        } else if (!record.barcodeImage) {
          missing.push(code)
        }
      }
      if (!missing.length) return
      const unique = Array.from(new Set(missing))
      this._barcodeLoading.add(key)
      try {
        const res = await axios.post('/api/workrecords/generateBarcodes', unique)
        const data = res && res.data ? res.data : {}
        Object.keys(data || {}).forEach(code => {
          if (!code) return
          this.$set(this.barcodeCache, code, data[code])
        })
        for (const record of records) {
          const code = this.sanitize(record.barcode)
          if (code && this.barcodeCache[code]) {
            this.$set(record, 'barcodeImage', this.barcodeCache[code])
          }
        }
      } catch (e) {
        console.error('加载条码失败', e)
      } finally {
        this._barcodeLoading.delete(key)
      }
    },
    async loadBarcodesForCurrentPage() {
      if (!this.pageRecords.length) return
      const key = `${this.currentDrawingIndex}-${this.pageNo}`
      await this.loadBarcodes(this.pageRecords, key)
    },
    checkHours(record) {
      record.hoursMissing = record.hours == null || record.hours === ''
      this.markDirty(record)
    },
    addRow(index) {
      const base = this.pageRecords[index] || {}
      const blank = this.decorateRecord({
        notificationNumber: base.notificationNumber,
        productName: base.productName,
        drawingNumber: base.drawingNumber,
        partName: base.partName,
        planQty: null,
        processCode: '',
        processName: '',
        hours: null,
        workerCodes: '',
        qualifiedQty: null,
        startTime: '',
        endTime: '',
        inspector: '',
        barcode: '',
        barcodeImage: ''
      })
      blank.codeMissing = true
      blank.hoursMissing = true
      this.pageRecords.splice(index + 1, 0, blank)
    },
    async deleteRow(index) {
      const record = this.pageRecords[index]
      if (!record) return
      if (!confirm('确定删除该行? 删除后不可恢复')) return
      if (record.id) {
        let headers
        try { headers = this.requireUserHeaders() }
        catch (err) { return }
        try {
          await axios.delete(`/api/workrecords/${record.id}`, { headers })
        } catch (e) {
          this.handleRequestError(e, '删除失败')
          return
        }
      }
      this.pageRecords.splice(index, 1)
      await this.fetchDrawings()
      await this.fetchPage()
    },
    async deleteZero() {
      if (!this.hasPreview) return
      if (!confirm('确定删除所有工序为0的行?')) return
      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }
      const remaining = []
      for (const record of this.pageRecords) {
        const code = record.processCode != null ? String(record.processCode).trim() : ''
        if (code === '0') {
          if (record.id) {
            try {
              await axios.delete(`/api/workrecords/${record.id}`, { headers })
            } catch (e) {
              this.handleRequestError(e, '删除失败')
              return
            }
          }
        } else {
          remaining.push(record)
        }
      }
      this.pageRecords = remaining
      await this.fetchDrawings()
      await this.fetchPage()
      alert('已清除工序为0的记录')
    },
    async save() {
      if (!this.hasPreview) return
      if (!confirm('请再次核查数据后确认提交')) return
      let headers
      try { headers = this.requireUserHeaders() }
      catch (err) { return }
      await this.ensureProcessCache()
      const modified = this.pageRecords.filter(r => r._dirty || !r.id)
      if (!modified.length) {
        alert('当前页没有需要保存的修改')
        return
      }
      const toUpdate = modified.filter(r => r.id)
      const toCreate = modified.filter(r => !r.id).filter(r => r.processCode && r.barcode)
      if (toCreate.length < modified.filter(r => !r.id).length) {
        alert('部分新建记录缺少工序代码或条形码，已忽略')
      }
      try {
        if (toCreate.length) {
          await this.saveNewRecords(toCreate, headers)
        }
        if (toUpdate.length) {
          await this.saveExistingRecords(toUpdate, headers)
        }
        alert('保存成功')
        await this.fetchDrawings()
        await this.fetchPage()
      } catch (e) {
        this.handleRequestError(e, '保存失败')
      }
    },
    async saveNewRecords(records, headers) {
      const chunkSize = 300
      for (let offset = 0; offset < records.length; offset += chunkSize) {
        const chunk = records.slice(offset, offset + chunkSize).map(r => ({
          ...r,
          file: undefined,
          id: undefined,
          barcodeImage: undefined,
          _dirty: undefined,
          _localKey: undefined
        }))
        await axios.post(`/api/workrecords?fileId=${this.fileId}`, chunk, { headers })
      }
    },
    async saveExistingRecords(records, headers) {
      const chunkSize = 300
      for (let offset = 0; offset < records.length; offset += chunkSize) {
        const chunk = records.slice(offset, offset + chunkSize).map(r => ({
          ...r,
          file: undefined,
          barcodeImage: undefined,
          _dirty: undefined,
          _localKey: undefined
        }))
        await axios.put('/api/workrecords/bulk', chunk, { headers })
      }
    },
    async prevDrawing() {
      if (!this.canPrevDrawing) return
      this.currentDrawingIndex -= 1
      this.pageNo = 0
      await this.fetchPage()
    },
    async nextDrawing() {
      if (!this.canNextDrawing) return
      this.currentDrawingIndex += 1
      this.pageNo = 0
      await this.fetchPage()
    },
    async prevPage() {
      if (!this.canPrevPage) return
      this.pageNo -= 1
      await this.fetchPage()
    },
    async nextPage() {
      if (!this.canNextPage) return
      this.pageNo += 1
      await this.fetchPage()
    },
    async jumpToDrawing() {
      const term = (this.drawingSearch || '').trim().toLowerCase()
      if (!term || !this.drawings.length) return
      const index = this.drawings.findIndex(d => (d.drawing || '').toLowerCase().includes(term))
      if (index >= 0) {
        this.currentDrawingIndex = index
        this.pageNo = 0
        await this.fetchPage()
      } else {
        alert('未找到对应图号')
      }
    },
    async collectPrintPages() {
      const pages = []
      if (!this.fileId || !this.drawings.length) return pages
      for (let i = 0; i < this.drawings.length; i += 1) {
        const drawing = this.drawings[i]
        if (!drawing) continue
        const drawingNumber = drawing.drawing
        let pageIndex = 0
        while (true) {
          const data = await this.fetchPageData(drawingNumber, pageIndex, this.rowsPerPage)
          const list = data && Array.isArray(data.content) ? data.content : []
          if (!list.length) break
          const decorated = list.map(item => this.decorateRecord(item))
          const baseCount = (drawing && typeof drawing.count === 'number' && drawing.count > 0)
            ? drawing.count
            : decorated.length
          const totalPages = (data && typeof data.totalPages === 'number' && data.totalPages > 0)
            ? data.totalPages
            : Math.max(1, Math.ceil(baseCount / this.rowsPerPage))
          const isLastPage = (data && data.last === true)
            || pageIndex >= totalPages - 1
            || decorated.length < this.rowsPerPage
          const blankCount = isLastPage ? Math.max(0, this.rowsPerPage - decorated.length) : 0
          const minRow = decorated.reduce((acc, rec) => {
            if (rec && typeof rec.sourceRowNumber === 'number' && rec.sourceRowNumber > 0) {
              return Math.min(acc, rec.sourceRowNumber)
            }
            if (rec && rec.sourceRowNumber != null) {
              const num = Number(rec.sourceRowNumber)
              if (Number.isFinite(num) && num > 0) {
                return Math.min(acc, num)
              }
            }
            return acc
          }, Number.POSITIVE_INFINITY)
          const pageInfo = {
            key: `${(drawingNumber || 'drawing')}-${pageIndex}`,
            drawingNumber,
            pageNumber: pageIndex + 1,
            totalPages,
            records: decorated,
            blankCount,
            sourceRowNumber: Number.isFinite(minRow) && minRow !== Number.POSITIVE_INFINITY ? minRow : (drawing && drawing.startRow) || null
          }
          await this.loadBarcodes(decorated, pageInfo.key)
          pages.push(pageInfo)
          pageIndex += 1
          if (pageIndex >= totalPages) break
        }
      }
      return pages
    },
    async print() {
      if (!this.hasPreview || !this.fileId) return
      const originalTitle = document.title
      this.printing = true
      try {
        if (!this.drawings.length) {
          await this.fetchDrawings()
        }
        const pages = await this.collectPrintPages()
        if (!pages.length) {
          alert('没有可打印的数据')
          return
        }
        this.printPages = pages
        await this.$nextTick()
        if (this.currentFileName) {
          document.title = this.currentFileName
        }
        window.print()
      } catch (e) {
        this.handleRequestError(e, '打印失败')
      } finally {
        document.title = originalTitle
        this.printPages = []
        this.printing = false
      }
    }
  }
}
</script>
