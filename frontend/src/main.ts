// Import polyfill to make browser's 'window' object available as 'global' for SockJS compatibility
// Needed because SockJS was originally designed for use in Node.js, not browsers
import './polyfills'

import { mount } from 'svelte'
import './app.css'
import App from './App.svelte'

const app = mount(App, {
  target: document.getElementById('app')!,
})

export default app
