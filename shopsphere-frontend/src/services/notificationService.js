import { authApi } from './authService'
const base='/admin/notifications'
export async function getAdminNotifications(params={}){return (await authApi.get(base,{params})).data.data}
export async function getAdminNotificationStats(){return (await authApi.get(`${base}/stats`)).data.data}
export async function getAdminNotification(id){return (await authApi.get(`${base}/${id}`)).data.data}
export async function createAdminNotification(payload){return (await authApi.post(base,payload)).data.data}
