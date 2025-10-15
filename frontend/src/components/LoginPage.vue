<template>
  <div class="login-page d-flex align-items-center justify-content-center min-vh-100">
    <div class="card shadow-lg border-0 p-4" style="max-width:420px;width:100%;border-radius:1rem;">
      <div class="text-center mb-4">
        <h2 class="h4 fw-bold text-primary mb-0">工时管理系统登录</h2>
      </div>
      <form @submit.prevent="login">
        <div class="mb-3">
          <label class="form-label visually-hidden" for="username">用户名</label>
          <input id="username" class="form-control form-control-lg" v-model.trim="username" placeholder="用户名" autocomplete="username" />
        </div>
        <div class="mb-3">
          <label class="form-label visually-hidden" for="password">密码</label>
          <input id="password" type="password" class="form-control form-control-lg" v-model.trim="password" placeholder="密码" autocomplete="current-password" />
        </div>
        <button class="btn btn-primary btn-lg w-100" :disabled="loading || !canSubmit">
          <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
          登录
        </button>
      </form>
      <div class="text-danger mt-3 text-center" v-if="error">{{ error }}</div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
export default {
  name: 'LoginPage',
  data() {
    return {
      username: '',
      password: '',
      loading: false,
      error: ''
    };
  },
  computed: {
    canSubmit() {
      return this.username && this.password;
    }
  },
  methods: {
    async login() {
      if (!this.canSubmit) return;
      this.loading = true;
      this.error = '';
      try {
        await axios.post(
          '/api/auth/login',
          {
            username: this.username,
            password: this.password
          },
          { headers: { 'X-User': this.username } }
        );
        localStorage.setItem('loggedIn', 'true');
        localStorage.setItem('username', this.username);
        this.$emit('logged-in');
      } catch (e) {
        this.error = e.response?.data?.message || '登录失败，请稍后再试';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.login-page {
  background: linear-gradient(135deg, var(--bs-primary) 0%, #003580 100%);
}

.card {
  backdrop-filter: saturate(180%) blur(30px);
  background: rgba(255, 255, 255, 0.85);
}
</style>
