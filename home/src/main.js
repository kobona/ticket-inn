import Vue from 'vue'
import App from '@/App.vue'
import router from '@/router'
import store from '@/store'

import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css'
Vue.use(Vuetify)

import axios from 'axios'
import VueAxios from 'vue-axios'
Vue.use(VueAxios, axios)

import lodash from 'lodash'
import moment from 'moment'
Object.defineProperty(Vue.prototype, '_', { value: lodash })
Object.defineProperty(Vue.prototype, '$moment', { value: moment })

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
