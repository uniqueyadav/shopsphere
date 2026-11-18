import { authApi } from './authService'

const adminBase = '/admin/delivery-partners'
export async function getAdminDeliveryPartners(params = {}) { return (await authApi.get(adminBase, { params })).data.data }
export async function getAdminDeliveryPartnerStats() { return (await authApi.get(`${adminBase}/stats`)).data.data }
export async function getAdminDeliveryPartner(id) { return (await authApi.get(`${adminBase}/${id}`)).data.data }
export async function createAdminDeliveryPartner(payload) { return (await authApi.post(adminBase, payload)).data.data }
export async function updateAdminDeliveryPartnerStatus(id, status, reason) { return (await authApi.patch(`${adminBase}/${id}/status`, { status, reason })).data.data }
export async function exportAdminDeliveryPartners(params = {}) { return (await authApi.get(`${adminBase}/export`, { params, responseType: 'blob' })).data }
