import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8091',
        changeOrigin: true
      },
      '/practice': {
        target: 'ws://127.0.0.1:8091',
        ws: true
      }
    }
  }
})