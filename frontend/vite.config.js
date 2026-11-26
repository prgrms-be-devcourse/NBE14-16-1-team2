import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 포트가 사용 중이면 다른 포트로 넘어가지 않고 실패하도록 고정 (README와 일치시키기 위함)
    strictPort: true,
  },
})
