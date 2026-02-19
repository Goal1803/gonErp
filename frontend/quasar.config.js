/* eslint-env node */
const { configure } = require('quasar/wrappers')

module.exports = configure(function (/* ctx */) {
  return {
    boot: [
      'pinia',
      'axios'
    ],

    css: [
      'app.scss'
    ],

    extras: [
      'roboto-font',
      'material-icons',
      'mdi-v7'
    ],

    build: {
      target: {
        browser: ['es2019', 'edge88', 'firefox78', 'chrome87', 'safari13.1'],
        node: 'node20'
      },
      vueRouterMode: 'hash'
    },

    devServer: {
      open: true,
      port: 9000,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },

    framework: {
      config: {
        dark: true,
        brand: {
          primary: '#2E7D32',
          secondary: '#388E3C',
          accent: '#F57C00',
          dark: '#121212',
          'dark-page': '#0D0D0D',
          positive: '#21BA45',
          negative: '#C62828',
          info: '#1565C0',
          warning: '#F57F17'
        },
        notify: {
          position: 'top-right',
          timeout: 3000
        }
      },
      plugins: [
        'Notify',
        'Dialog',
        'Loading',
        'LocalStorage',
        'SessionStorage'
      ]
    },

    animations: 'all'
  }
})
