<template>
  <section class="section-card">
    <h2 class="h5">操作记录</h2>
    <table class="table table-bordered table-sm table-striped">
      <thead>
        <tr><th>时间</th><th>操作者</th><th>操作</th></tr>
      </thead>
      <tbody>
        <template v-for="log in logs">
          <tr :key="log.id" @click="log.show = !log.show" style="cursor:pointer">
            <td>{{ log.timestamp.replace('T',' ').slice(0,19) }}</td>
            <td>{{ log.username }}</td>
            <td class="wrap-text">{{ log.action }}</td>
          </tr>
          <tr v-if="log.show" :key="log.id + '-details'">
            <td colspan="3" class="pre-wrap">
              <div v-if="log.details">{{ log.details }}</div>
              <div v-for="s in log.sub" :key="s.id">{{ s.action }}</div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </section>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return { logs: [] }
  },
  created() { this.fetchLogs() },
  methods: {
    async fetchLogs() {
      const user = localStorage.getItem('username')
      const headers = user ? { 'X-User': user } : {}
      const res = await axios.get('/api/logs', { headers })
      this.logs = this.groupLogs(res.data)
    },
    groupLogs(raw) {
      const grouped = []
      let current = null
      for (const log of raw) {
        log.show = false
        log.sub = log.sub || []
        const isApi = /^(GET|POST|PUT|DELETE|PATCH) /.test(log.action) && !log.details
        if (isApi && current) {
          current.sub.push(log)
        } else {
          grouped.push(log)
          current = log
        }
      }
      return grouped
    }
  }
}
</script>

<style>
.pre-wrap { white-space: pre-wrap; }
</style>