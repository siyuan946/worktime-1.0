import Vue from 'vue'
import App from './App.vue'
import router from './router'
import './assets/style.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import axios from 'axios'

axios.interceptors.request.use(config => {
  const user = localStorage.getItem('username')
  if (user) config.headers['X-User'] = user
  return config
})

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
