import { authApi } from './authService'

const adminBase = '/admin/delivery-partners'
export async function getDeliveryPartnerProfile() { return (await authApi.get('/delivery/me')).data.data }
export async function getDeliveryDashboard() { return (await authApi.get('/delivery/dashboard')).data.data }
export async function getDeliveryAssignmentDetails(id) { return (await authApi.get(`/delivery/assignments/${id}`)).data.data }
export async function setDeliveryAvailability(online) { return (await authApi.patch('/delivery/availability', { online })).data.data }
export async function updateDeliveryAssignmentStatus(id, status) {
  const location = await new Promise(resolve => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) return resolve({})
    navigator.geolocation.getCurrentPosition(({ coords }) => resolve({ latitude: coords.latitude, longitude: coords.longitude }), () => resolve({}), { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 })
  })
  return (await authApi.patch(`/delivery/assignments/${id}/status`, { status, ...location })).data.data
}
export async function acceptDeliveryAssignment(id) { return (await authApi.patch(`/delivery/assignments/${id}/accept`)).data.data }
export async function updateDeliveryLocation(latitude, longitude) { return (await authApi.patch('/delivery/location', { latitude, longitude })).data.data }
export async function getAdminCourierDashboard(days = 30) { return (await authApi.get(`${adminBase}/dashboard`, { params: { days } })).data.data }
export async function getAdminDeliveryPartners(params = {}) { return (await authApi.get(adminBase, { params })).data.data }
export async function getAdminDeliveryPartnerStats() { return (await authApi.get(`${adminBase}/stats`)).data.data }
export async function getAdminDeliveryPartner(id) { return (await authApi.get(`${adminBase}/${id}`)).data.data }
export async function createAdminDeliveryPartner(payload) { return (await authApi.post(adminBase, payload)).data.data }
export async function updateAdminDeliveryPartnerStatus(id, status, reason) { return (await authApi.patch(`${adminBase}/${id}/status`, { status, reason })).data.data }
export async function updateAdminDeliveryPartnerServiceCity(id, city) { return (await authApi.patch(`${adminBase}/${id}/service-city`, { city })).data.data }
export async function exportAdminDeliveryPartners(params = {}) { return (await authApi.get(`${adminBase}/export`, { params, responseType: 'blob' })).data }
