import { authApi } from './authService'

// The current backend review package is empty; keep requests on the established API client.
const base = '/admin/reviews'
export async function getAdminReviews(params) { return (await authApi.get(base, { params })).data.data }
export async function getAdminReviewStats(params) { return (await authApi.get(`${base}/stats`, { params })).data.data }
export async function moderateAdminReview(id, payload) { return (await authApi.patch(`${base}/${id}/moderation`, payload)).data.data }
export async function deleteAdminReview(id) { return (await authApi.delete(`${base}/${id}`)).data.data }
export async function exportAdminReviews(params) { return (await authApi.get(`${base}/export`, { params, responseType: 'blob' })).data }
