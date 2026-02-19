/* eslint-env node */

module.exports = {
  root: true,

  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },

  env: {
    browser: true,
    es2021: true
  },

  extends: [
    'plugin:vue/vue3-essential',
    'eslint:recommended'
  ],

  plugins: ['vue'],

  rules: {
    'vue/multi-word-component-names': 'off',
    'no-unused-vars': 'warn'
  }
}
