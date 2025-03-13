// Polyfill for global object needed by SockJS
if (typeof (window as any).global === 'undefined') {
  (window as any).global = window;
} 