import Vue from 'vue'
import Router from 'vue-router'
import RecordUpload from './components/RecordUpload.vue'
import RecordScanner from './components/RecordScanner.vue'
import WorkerManager from './components/WorkerManager.vue'
import ProcessManager from './components/ProcessManager.vue'
import OperationLog from './components/OperationLog.vue'

Vue.use(Router)

export default new Router({
  routes: [
    { path: '/', redirect: '/upload' },
    { path: '/upload', component: RecordUpload },
    { path: '/records', component: RecordScanner },
    { path: '/workers', component: WorkerManager },
   { path: '/processes', component: ProcessManager },
    { path: '/logs', component: OperationLog }
  ]
})
