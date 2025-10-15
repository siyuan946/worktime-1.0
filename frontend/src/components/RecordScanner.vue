<template>
  <section class="section-card">
    <h2 class="h5">扫码录入</h2>
    <div class="input-group mb-2" style="max-width:300px;">
      <input class="form-control form-control-sm" v-model="searchBarcode" placeholder="扫码条形码" @keyup.enter="searchByBarcode" />
      <button class="btn btn-outline-secondary btn-sm" @click="searchByBarcode">查询</button>
    </div>
    <div class="mb-2 d-flex flex-wrap align-items-center gap-2">
      <select
        class="form-select form-select-sm"
        style="min-width: 220px; flex: 0 1 auto;"
        v-model="selectedFileId"
      >
        <option value="" disabled>选择文件查看全部</option>
        <option v-for="f in files" :key="f.id" :value="f.id">{{ f.fileName }} ({{ f.uploadTime ? f.uploadTime.slice(0,10) : '' }})</option>
      </select>
      <button class="btn btn-outline-secondary btn-sm" @click="loadFile" :disabled="!selectedFileId">加载</button>
      <div class="d-flex flex-wrap align-items-center gap-2">
        <input
          type="month"
          class="form-control form-control-sm"
          style="min-width: 160px; flex: 0 1 auto;"
          v-model="exportMonth"
        >
        <button class="btn btn-outline-primary btn-sm" @click="exportByNaturalMonth" :disabled="!exportMonth">按自然月导出</button>
      </div>
      <div class="d-flex flex-wrap align-items-center gap-2">
        <input
          class="form-control form-control-sm"
          style="min-width: 180px; flex: 0 1 auto;"
          v-model="exportDrawing"
          placeholder="输入图号导出"
        >
        <button class="btn btn-outline-primary btn-sm" @click="exportByDrawing" :disabled="!exportDrawingReady">按图号导出</button>
      </div>
    </div>
    <div class="mb-2" v-if="records.length">
      计划数:
      <input type="number" class="form-control form-control-sm d-inline-block" style="width:80px" v-model.number="planQtyInput" @change="updatePlanQty" :disabled="viewOnly" />
      | 总合格数: {{ totalQualified }} | 已填写 {{ records.length }} 条
      <button v-if="!viewOnly" class="btn btn-sm btn-outline-secondary ms-2" @click="addRecord">新增记录</button>
      <button
        v-if="!viewOnly"
        class="btn btn-sm btn-primary ms-2"
        @click="saveAllRecords"
        :disabled="savingAll || !records.length"
      >保存全部</button>
    </div>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr>
          <th>ID</th>
          <th>通知单号</th>
          <th>产品名称</th>
          <th>图号</th>
          <th>工序代码</th>
          <th>单件工时</th>
          <th>计划数</th>
          <th>人员代码</th>
          <th>数量分配</th>
          <th>姓名</th>
          <th>车间</th>
          <th>班组</th>
          <th>起始日期</th>
          <th>结束日期</th>
          <th>合格数</th>
          <th>单件工时小计</th>
          <th>单件工时分配</th>
          <th v-if="!viewOnly"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="rec in records" :key="rec.id">
          <td>{{ rec.id }}</td>
          <td class="wrap-text">{{ rec.notificationNumber }}</td>
          <td class="wrap-text">{{ rec.productName }}</td>
          <td class="wrap-text">{{ rec.drawingNumber }}</td>
          <td>{{ rec.processCode }}</td>
          <td>{{ rec.hours }}</td>
          <td>
            <span v-if="viewOnly">{{ rec.planQty }}</span>
            <input
              v-else
              type="number"
              class="form-control form-control-sm edit-highlight"
              style="width:80px"
              v-model.number="rec.planQty"
              @blur="onPlanQtyCellChange(rec)"
            />
          </td>
          <td class="wrap-text">
            <span v-if="viewOnly">{{ rec.workerCodes }}</span>
            <textarea
              v-else
              class="form-control form-control-sm auto-grow edit-highlight"
              style="width:100%;"
              v-model="rec.workerCodes"
              @focus="autoGrow"
              @input="autoGrow"
              @blur="onWorkerCodesChange(rec)"
            ></textarea>
          </td>
          <td>
            <span v-if="viewOnly">{{ rec.workerQtys }}</span>
            <div v-else class="d-flex flex-wrap">
              <div
                v-for="(name, idx) in rec.workerNamesList"
                :key="idx"
                class="me-1 mb-1"
              >
                <input
                  type="number"
                  class="form-control form-control-sm alloc-input edit-highlight"
                  v-model.number="rec.workerQtyVals[idx]"
                  :placeholder="name"
                  @input="onQtyFieldsInput(rec)"
                  @blur="validateQtyAllocation(rec)"
                />
              </div>
            </div>
          </td>
          <td class="wrap-text">{{ rec.workerNames }}</td>
          <td class="wrap-text">{{ rec.workshop }}</td>
          <td class="wrap-text">{{ rec.team }}</td>
          <td>
            <span v-if="viewOnly">{{ rec.startDate }}</span>
            <input v-else type="text" class="form-control form-control-sm edit-highlight" v-model="rec.startDate" @blur="normalizeStart(rec)" style="width:130px" placeholder="MM/DD" />
          </td>
          <td>
            <span v-if="viewOnly">{{ rec.endDate }}</span>
            <input v-else type="text" class="form-control form-control-sm edit-highlight" v-model="rec.endDate" @blur="normalizeEnd(rec)" style="width:130px" placeholder="MM/DD" />
          </td>
          <td>
            <span v-if="viewOnly">{{ rec.qualifiedQty }}</span>
            <input v-else type="number" step="0.01" class="form-control form-control-sm edit-highlight" v-model.number="rec.qualifiedQty" @input="onQtyChange(rec)" @blur="validatePlanQty();validateQtyAllocation(rec);validateHourAllocation(rec)" style="width:80px" />
          </td>
          <td>{{ rec.hourSubtotal }}</td>
          <td class="wrap-text">
            <span v-if="viewOnly">{{ rec.workerHours }}</span>
            <div v-else class="d-flex flex-wrap">
              <div
                v-for="(name, idx) in rec.workerNamesList"
                :key="idx"
                class="me-1 mb-1"
              >
                <input
                  type="number"
                  class="form-control form-control-sm alloc-input edit-highlight"
                  v-model.number="rec.workerHourVals[idx]"
                  :placeholder="name"
                  @input="onHourFieldsInput(rec)"
                  @blur="validateHourAllocation(rec)"
                />
              </div>
            </div>
          </td>
          <td v-if="!viewOnly">
            <button class="btn btn-sm btn-primary me-1" @click="updateRecord(rec)">保存</button>
            <button class="btn btn-sm btn-outline-danger" @click="deleteRecord(rec)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return {
      records: [],
      searchBarcode: '',
      files: [],
      selectedFileId: '',
      exportMonth: '',
      exportDrawing: '',
      viewOnly: false,
      scanBuffer: '',
      planQtyInput: null,
      savingAll: false
    }
  },
  created() {
    this.fetchFiles()
  },
  mounted() {
    window.addEventListener('keydown', this.handleScanKey)
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleScanKey)
  },
  computed: {
    planQty() {
      return this.records.length ? this.records[0].planQty : null
    },
    totalQualified() {
      return this.records.reduce((sum, r) => sum + (r.qualifiedQty || 0), 0)
    },
    exportDrawingReady() {
      return !!(this.exportDrawing && this.exportDrawing.trim())
    }
  },
  methods: {
    async fetchFiles() {
      const res = await axios.get('/api/files')
      this.files = res.data
    },
    async loadFile() {
      if (!this.selectedFileId) return
      try {
        const res = await axios.get(
          `/api/workrecords/file/${this.selectedFileId}/filled`
        )
        if (!Array.isArray(res.data) || !res.data.length) {
          alert('该文件暂无填写记录')
          this.records = []
          this.planQtyInput = null
        } else {
          this.viewOnly = true
          await this.processRecords(res.data)
        }
      } catch (e) {
        console.error(e)
        alert('加载失败')
      }
    },
    async exportByNaturalMonth() {
      if (!this.exportMonth) return
      const [year, month] = this.exportMonth.split('-')
      const res = await axios.get(
        `/api/workrecords/natural-month/${year}/${month}/export`,
        { responseType: 'blob' }
      )
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = `records_${year}-${month}.xlsx`
      a.click()
      window.URL.revokeObjectURL(url)
    },
    async exportByDrawing() {
      if (!this.exportDrawingReady) return
      const drawing = this.exportDrawing.trim()
      const res = await axios.get(
        `/api/workrecords/drawing/${encodeURIComponent(drawing)}/export`,
        { responseType: 'blob' }
      )
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = `${drawing}.xlsx`
      a.click()
      window.URL.revokeObjectURL(url)
    },
    async searchByBarcode() {
      const code = this.searchBarcode.trim()
      if (!code) { this.records = []; this.planQtyInput = null; this.viewOnly = false; return }
      if (this.viewOnly) {
        this.records = []
        this.viewOnly = false
        this.planQtyInput = null
      }
      const url = `/api/workrecords/barcode/${encodeURIComponent(code)}`
      try {
        const res = await axios.get(url)
        this.viewOnly = false
        await this.processRecords(res.data, { replace: false })
        this.searchBarcode = ''
      } catch (e) {
        console.error(e)
        alert('查询失败')
      }
    },
    async updateRecord(rec) {
      const total = this.records.reduce((sum, r) => sum + (r === rec ? (rec.qualifiedQty || 0) : (r.qualifiedQty || 0)), 0)
      if (this.planQty != null && total > this.planQty) {
        alert('总合格数已超过计划数，请确认')
      }
      const qtyOk = this.validateQtyAllocation(rec)
      const hourOk = this.validateHourAllocation(rec)
      if (!qtyOk || !hourOk) return
      const missing = this.collectMissingFields(rec)
      if (missing.length) {
        const proceed = confirm(`以下信息未填写：${missing.join('、')}，是否继续保存？`)
        if (!proceed) return
      }
      const payload = this.buildPayload(rec)
      try {
        const res = await axios.put(`/api/workrecords/${rec.id}`, payload)
        await this.processRecords([res.data], { replace: false })
      } catch (e) {
        console.error(e)
        alert('保存失败')
      }
    },
    async saveAllRecords() {
      if (!this.records.length || this.viewOnly) return
      const planOk = this.validatePlanQty()
      if (!planOk) return
      let valid = true
      for (const rec of this.records) {
        const qtyOk = this.validateQtyAllocation(rec)
        const hourOk = this.validateHourAllocation(rec)
        if (!qtyOk || !hourOk) {
          valid = false
        }
      }
      if (!valid) return
      const missingRows = []
      const payloads = []
      this.records.forEach((rec, idx) => {
        const missing = this.collectMissingFields(rec)
        if (missing.length) {
          const identifier = rec.notificationNumber ? `${rec.notificationNumber}-${rec.id || idx + 1}` : `第${idx + 1}行`
          missingRows.push(`${identifier}：${missing.join('、')}`)
        }
        payloads.push(this.buildPayload(rec))
      })
      if (missingRows.length) {
        const proceed = confirm(`以下记录未填写完整：\n${missingRows.join('\n')}\n是否继续保存？`)
        if (!proceed) return
      }
      this.savingAll = true
      try {
        const res = await axios.put('/api/workrecords/bulk', payloads)
        await this.processRecords(res.data, { replace: false })
      } catch (e) {
        console.error(e)
        alert('保存失败')
      } finally {
        this.savingAll = false
      }
    },
    async addRecord() {
      if (!this.records.length) return
      const id = this.records[0].id
      const res = await axios.post(`/api/workrecords/duplicate/${id}`)
      await this.processRecords([res.data], { replace: false })
    },
    async deleteRecord(rec) {
      if (!confirm('确定删除这条记录?')) return
      try {
        await axios.delete(`/api/workrecords/${rec.id}`)
        const idx = this.records.indexOf(rec)
        if (idx !== -1) this.records.splice(idx, 1)
        if (!this.records.length) this.planQtyInput = null
      } catch (e) {
        console.error(e)
        alert('删除失败')
      }
    },
    async processRecords(list, options = {}) {
      const replace = options.replace === undefined ? true : options.replace
      const prepared = []
      for (const raw of list) {
        const rec = this.decorateRecord(raw)
        this.computeSubtotal(rec)
        if (rec.workerCodes) await this.lookupWorker(rec)
        this.computeWorkerHours(rec)
        prepared.push(rec)
      }
      if (replace) {
        this.records = prepared
        this.planQtyInput = this.planQty
      } else {
        const existingMap = new Map(this.records.map(r => [r.id, r]))
        for (const rec of prepared) {
          const existing = existingMap.get(rec.id)
          if (existing) {
            Object.assign(existing, rec)
          } else {
            this.records.push(rec)
          }
        }
        if (this.planQtyInput == null && this.planQty != null) {
          this.planQtyInput = this.planQty
        }
      }
    },
    decorateRecord(raw) {
      return {
        ...raw,
        workshop: '',
        team: '',
        workerQtys: raw.workerQtys || '',
        workerHours: raw.workerHours || '',
        workerQtyVals: this.parseAllocValues(raw.workerQtys),
        workerHourVals: [],
        workerNamesList: [],
        codeToName: {},
        startDate: raw.startTime ? raw.startTime.slice(0, 10) : '',
        endDate: raw.endTime ? raw.endTime.slice(0, 10) : '',
        hourSubtotal: raw.hourSubtotal != null ? raw.hourSubtotal : (raw.qualifiedQty != null && raw.hours != null ? raw.qualifiedQty * raw.hours : null),
        _hourOverflowShown: false
      }
    },
    computeSubtotal(row) {
      if (row.qualifiedQty != null && row.hours != null) row.hourSubtotal = row.qualifiedQty * row.hours
      else row.hourSubtotal = null
    },
    onQtyChange(rec) {
      this.computeSubtotal(rec)
      if (rec.workerHourVals.some(v => v != null && v !== '' && !isNaN(v))) {
        this.computeQtysFromHours(rec)
      } else {
        this.computeWorkerHours(rec)
      }
    },
    onWorkerCodesChange(rec) {
      this.lookupWorker(rec)
      if (rec.workerHourVals.some(v => v != null && v !== '' && !isNaN(v))) {
        this.computeQtysFromHours(rec)
      } else {
        this.computeWorkerHours(rec)
      }
    },
    onQtyFieldsInput(rec) {
      this.computeWorkerHours(rec)
    },
    onHourFieldsInput(rec) {
      this.computeQtysFromHours(rec)
    },
    validatePlanQty() {
      const total = this.records.reduce((sum, r) => sum + (r.qualifiedQty || 0), 0)
      if (this.planQty != null && total > this.planQty) {
        alert('总合格数已超过计划数，请确认')
        return false
      }
      return true
    },
    validateQtyAllocation(rec) {
      if (!rec || !Array.isArray(rec.workerQtyVals)) return true
      const sum = rec.workerQtyVals.reduce((a, b) => a + (parseFloat(b) || 0), 0)
      if (rec.qualifiedQty != null && sum > rec.qualifiedQty) {
        alert('分配数量超过合格数量')
        return false
      }
      return true
    },
    validateHourAllocation(rec) {
      if (!rec || !Array.isArray(rec.workerHourVals)) return true
      const sum = rec.workerHourVals.reduce((a,b) => a + (parseFloat(b) || 0), 0)
      const total = (rec.qualifiedQty || 0) * (rec.hours || 0)
      if (total && sum > total) {
        if (!rec._hourOverflowShown) alert('填写的单件工时超过总工时')
        rec._hourOverflowShown = true
        return false
      } else {
        rec._hourOverflowShown = false
      }
      return true
    },
    onPlanQtyCellChange(rec) {
      this.planQtyInput = rec.planQty
      this.updatePlanQty()
    },
    updatePlanQty() {
      this.records.forEach(r => { r.planQty = this.planQtyInput })
      this.validatePlanQty()
    },
    async lookupWorker(rec) {
      const codes = rec.workerCodes ? rec.workerCodes.split(/[,\u3001\s]+/) : []
      const names = []
      const workshops = new Set()
      const teams = new Set()
      const map = {}
      for (const c of codes) {
        if (!c) continue
        try {
          const res = await axios.get(`/api/workers/code/${encodeURIComponent(c)}`)
          const w = res.data
          if (w) {
            if (w.name) {
              names.push(w.name)
              map[c] = w.name
            }
            if (w.workshop) workshops.add(w.workshop)
            if (w.team) teams.add(w.team)
          } else {
            alert(`未找到人员 ${c}`)
          }
        } catch (e) {
          console.error(e)
          alert(`未找到人员 ${c}`)
        }
      }
      rec.codeToName = map
      rec.workerNames = names.join(',')
      rec.workshop = Array.from(workshops).join(',')
      rec.team = Array.from(teams).join(',')
      rec.workerNamesList = names
      const qtyVals = rec.workerQtyVals && rec.workerQtyVals.length
        ? rec.workerQtyVals
        : this.parseAllocValues(rec.workerQtys)
      const hourVals = rec.workerHourVals && rec.workerHourVals.length
        ? rec.workerHourVals
        : []
      while (qtyVals.length < names.length) qtyVals.push(null)
      while (hourVals.length < names.length) hourVals.push(null)
      rec.workerQtyVals = qtyVals
      rec.workerHourVals = hourVals
      rec.workerQtys = this.formatAllocString(names, qtyVals)
      rec.workerHours = this.formatAllocString(names, hourVals)
    },
    computeWorkerHours(rec) {
      const names = rec.workerNamesList || []
      if (!names.length || rec.hours == null || rec.qualifiedQty == null) {
        rec.workerHourVals = names.map(() => null)
        rec.workerHours = ''
        rec.workerQtys = this.formatAllocString(names)
        return
      }
      const qtyList = (rec.workerQtyVals || []).map(v => (v == null || isNaN(v)) ? 0 : parseFloat(v))
      if (qtyList.every(v => v === 0)) {
        rec.workerHourVals = names.map(() => null)
        rec.workerHours = ''
        rec.workerQtys = this.formatAllocString(names, qtyList)
        return
      }
      while (qtyList.length < names.length) qtyList.push(0)
      const hourList = qtyList.map(q => q * rec.hours)
      rec.workerHourVals = hourList
      rec.workerHours = names.map((n,i) => `${n}:${hourList[i].toFixed(2)}`).join(',')
      rec.workerQtys = this.formatAllocString(names, qtyList)
      const sum = qtyList.reduce((a,b) => a + (b || 0), 0)
      rec._qtyOverflow = rec.qualifiedQty != null && sum > rec.qualifiedQty
    },
    computeQtysFromHours(rec) {
      const names = rec.workerNamesList || []
      if (!names.length || rec.hours == null || rec.qualifiedQty == null) {
        rec.workerHours = ''
        rec.workerQtys = this.formatAllocString(names)
        rec.workerQtyVals = names.map(() => null)
        return
      }
      const hourList = (rec.workerHourVals || []).map(v => (v == null || isNaN(v)) ? 0 : parseFloat(v))
      if (hourList.every(v => v === 0)) {
        rec.workerQtyVals = names.map(() => null)
        rec.workerHours = ''
        rec.workerQtys = this.formatAllocString(names)
        return
      }
      while (hourList.length < names.length) hourList.push(0)
      const total = rec.qualifiedQty * rec.hours
      const sum = hourList.reduce((a,b) => a + (b || 0), 0)
      rec._hourOverflow = total != null && sum > total
      const qtyList = hourList.map(h => rec.hours ? h / rec.hours : 0)
      rec.workerQtyVals = qtyList
      rec.workerQtys = this.formatAllocString(names, qtyList)
      const qtySum = qtyList.reduce((a,b) => a + (b || 0), 0)
      rec._qtyOverflow = rec.qualifiedQty != null && qtySum > rec.qualifiedQty
      rec.workerHours = names.map((n,i) => `${n}:${hourList[i].toFixed(2)}`).join(',')
    },
    parseAllocValues(str) {
      if (!str) return []
      return str.trim().split(/[\s,]+/).map(seg => {
        if (!seg) return null
        let idx = seg.indexOf(':')
        if (idx < 0) idx = seg.indexOf('：')
        const numStr = idx >= 0 ? seg.slice(idx + 1) : seg
        if (numStr === '') return null
        const v = parseFloat(numStr)
        return isNaN(v) ? null : v
      })
    },
    formatAllocString(names, values) {
      return names.map((n, i) => {
        const val = values && values[i] != null && !isNaN(values[i]) ? values[i].toFixed(2) : ''
        return `${n}:${val}`
      }).join(' ')
    },
    autoGrow(event) {
      const el = event.target
      if (!el) return
      el.style.height = 'auto'
      el.style.height = el.scrollHeight + 'px'
    },
    handleScanKey(e) {
      const t = e.target.tagName
      if (t === 'INPUT' || t === 'TEXTAREA') return
      if (e.key === 'Enter') {
        if (!this.scanBuffer) return
        this.searchBarcode = this.scanBuffer
        this.scanBuffer = ''
        this.searchByBarcode()
      } else if (e.key.length === 1) {
        this.scanBuffer += e.key
      }
    },
    buildPayload(rec) {
      if (Array.isArray(rec.workerNamesList) && rec.workerNamesList.length) {
        rec.workerQtys = this.formatAllocString(rec.workerNamesList, rec.workerQtyVals)
        rec.workerHours = this.formatAllocString(rec.workerNamesList, rec.workerHourVals)
      }
      this.normalizeStart(rec)
      this.normalizeEnd(rec)
      const allowed = [
        'id',
        'notificationNumber',
        'productName',
        'drawingNumber',
        'productCode',
        'partName',
        'planQty',
        'processName',
        'processCode',
        'barcode',
        'barcodeImage',
        'batchNumber',
        'hours',
        'supplemental',
        'workerCodes',
        'workerNames',
        'workerQtys',
        'qualifiedQty',
        'hourSubtotal',
        'filled',
        'inspector',
        'remark1',
        'remark2'
      ]
      const payload = {}
      allowed.forEach(key => {
        if (rec[key] !== undefined) payload[key] = rec[key]
      })
      payload.startTime = rec.startDate ? rec.startDate + 'T00:00:00' : null
      payload.endTime = rec.endDate ? rec.endDate + 'T00:00:00' : null
      return payload
    },
    normalizeDate(str) {
      if (!str) return ''
      str = str.trim()
      if (/^\d{1,2}\/\d{1,2}$/.test(str)) {
        const [m, d] = str.split('/')
        const y = new Date().getFullYear()
        return `${y}-${m.padStart(2,'0')}-${d.padStart(2,'0')}`
      }
      if (/^\d{4}-\d{1,2}-\d{1,2}$/.test(str)) {
        const [y,m,d] = str.split('-')
        return `${y}-${m.padStart(2,'0')}-${d.padStart(2,'0')}`
      }
      return str
    },
    normalizeStart(rec) {
      rec.startDate = this.normalizeDate(rec.startDate)
      if (!rec.endDate) rec.endDate = rec.startDate
    },
    normalizeEnd(rec) {
      rec.endDate = this.normalizeDate(rec.endDate)
      if (!rec.endDate) rec.endDate = rec.startDate
    },
    collectMissingFields(rec) {
      const missing = []
      if (rec.planQty == null || rec.planQty === '') missing.push('计划数')
      if (!rec.workerCodes || !rec.workerCodes.trim()) missing.push('人员代码')
      if (rec.qualifiedQty == null || rec.qualifiedQty === '') missing.push('合格数')
      if (!rec.startDate || !rec.startDate.toString().trim()) missing.push('起始日期')
      if (!rec.endDate || !rec.endDate.toString().trim()) missing.push('结束日期')
      return missing
    }
  }
}
</script>