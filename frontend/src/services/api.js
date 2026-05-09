import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.response.use(
  res => res.data,
  err => Promise.reject(err.response?.data?.message || '请求失败')
)

export function getDashboard(accountId = 1) {
  return api.get(`/dashboard?account_id=${accountId}`)
}

export function submitArticle(data) {
  return api.post('/analysis/articles/submit', data)
}

export function triggerAnalysis(articleId, forceUpdate = false) {
  return api.post('/analysis/articles/ai', { article_id: articleId, force_update: forceUpdate })
}

export function getAccountDiagnosis(accountId, days = 7) {
  return api.get(`/analysis/accounts/${accountId}/diagnosis?days=${days}`)
}

export function getArticleAnalyses(params) {
  return api.get('/analysis/articles', { params })
}

export function recommendTopics(accountId, hotKeywords) {
  return api.post(`/analysis/accounts/${accountId}/topics`, hotKeywords)
}

export default api
