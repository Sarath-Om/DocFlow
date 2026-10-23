import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  base: './',
  plugins: [react(), VitePWA({ registerType: 'autoUpdate', includeAssets: ['icon.svg'], manifest: {
    name: 'DocFlow — Secure Document Workspace', short_name: 'DocFlow', description: 'Open, organize and securely sync your documents.',
    theme_color: '#f4f5f7', background_color: '#f4f5f7', display: 'standalone', start_url: './', scope: './',
    icons: [{ src: './icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any maskable' }]
  } })]
});
