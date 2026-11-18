import { authApi } from './authService'

export async function getAdminCoupons(params = {}) { const r = await authApi.get('/admin/coupons', { params }); return r.data.data }
export async function getAdminCouponStats() { const r = await authApi.get('/admin/coupons/stats'); return r.data.data }
export async function getAdminCoupon(id) { const r = await authApi.get(`/admin/coupons/${id}`); return r.data.data }
export async function createAdminCoupon(payload) { const r = await authApi.post('/admin/coupons', payload); return r.data.data }
export async function updateAdminCoupon(id, payload) { const r = await authApi.put(`/admin/coupons/${id}`, payload); return r.data.data }
export async function setAdminCouponActive(id, active) { const r = await authApi.patch(`/admin/coupons/${id}/active`, null, { params: { value: active } }); return r.data.data }
export async function archiveAdminCoupon(id) { const r = await authApi.delete(`/admin/coupons/${id}`); return r.data }
export async function getAdminCouponUsage(id, params = {}) { const r = await authApi.get(`/admin/coupons/${id}/usage`, { params }); return r.data.data }
export async function getAdminCouponAnalytics(id) { const r = await authApi.get(`/admin/coupons/${id}/analytics`); return r.data.data }
export async function checkAdminCouponCode(code, excludeId) { const r = await authApi.get('/admin/coupons/code-availability', { params: { code, ...(excludeId ? { excludeId } : {}) } }); return r.data.data }
export async function exportAdminCoupons(params = {}) { const r = await authApi.get('/admin/coupons/export', { params, responseType: 'blob' }); return r.data }
