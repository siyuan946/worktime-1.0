<template>
  <section class="section-card">
    <h2 class="h5">人员管理</h2>
    <div class="mb-2 text-end">
      <button class="btn btn-sm btn-primary" @click="openModal">新增人员</button>
    </div>
    <div class="row mb-2">
      <div class="col-sm-4">
        <input class="form-control form-control-sm" v-model="search" placeholder="搜索工号或姓名">
      </div>
    </div>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr>
          <th>工号</th><th>姓名</th><th>车间</th><th>班组</th><th>入厂</th><th>离厂</th><th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="w in workers" :key="w.id">
          <td><input class="form-control form-control-sm" v-model="w.code" /></td>
          <td><input class="form-control form-control-sm" v-model="w.name" /></td>
          <td><input class="form-control form-control-sm" v-model="w.workshop" /></td>
          <td><input class="form-control form-control-sm" v-model="w.team" /></td>
          <td><input class="form-control form-control-sm" v-model="w.entryDate" type="date" /></td>
          <td><input class="form-control form-control-sm" v-model="w.leaveDate" type="date" /></td>
          <td>
            <button class="btn btn-sm btn-outline-primary" @click="updateWorker(w)">保存</button>
            <button class="btn btn-sm btn-outline-danger" @click="deleteWorker(w.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <!-- 新增人员模态框 -->
    <div class="modal fade" tabindex="-1" ref="addModal">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">新增人员</h5>
            <button type="button" class="btn-close" @click="closeModal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newWorker.code" placeholder="工号" /></div>
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newWorker.name" placeholder="姓名" /></div>
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newWorker.workshop" placeholder="车间" /></div>
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newWorker.team" placeholder="班组" /></div>
            <div class="mb-2"><input class="form-control form-control-sm" v-model="newWorker.entryDate" type="date" placeholder="入厂日期" /></div>
            <div><input class="form-control form-control-sm" v-model="newWorker.leaveDate" type="date" placeholder="离厂日期" /></div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="closeModal">取消</button>
            <button class="btn btn-primary" @click="createWorker" :disabled="!canAdd">保存</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return {
      workers: [],
      search: '',
      newWorker: { code: '', name: '', workshop: '', team: '', entryDate: '', leaveDate: '' },
      modal: null
    }
  },
  computed: {
    canAdd() {
      return this.newWorker.code && this.newWorker.name
    }
  },
  watch: {
    search(val) {
      this.fetchWorkers(val)
    }
  },
  created() {
    this.fetchWorkers('')
  },
  mounted() {
    this.modal = new window.bootstrap.Modal(this.$refs.addModal)
  },
  methods: {
    openModal() {
      this.newWorker = { code: '', name: '', workshop: '', team: '', entryDate: '', leaveDate: '' }
      this.modal.show()
    },
    closeModal() {
      this.modal.hide()
    },
    async fetchWorkers(term) {
      const url = term
        ? `/api/workers/search?term=${encodeURIComponent(term)}`
        : '/api/workers'
      const res = await axios.get(url)
      this.workers = res.data
    },
    async createWorker() {
      await axios.post('`/api/workers', this.newWorker)
      this.closeModal()
      this.fetchWorkers(this.search)
    },
    async updateWorker(w) {
      await axios.put(`/api/workers/${w.id}`, w)
      this.fetchWorkers(this.search)
    },
    async deleteWorker(id) {
      await axios.delete(`/api/workers/${id}`)
      this.fetchWorkers(this.search)
    }
  }
}
</script>
