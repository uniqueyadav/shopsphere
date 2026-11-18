import { authApi } from './authService'

function authConfig(accessToken) {
  return accessToken
    ? { _useExplicitAuth: true, headers: { Authorization: `Bearer ${accessToken}` } }
    : undefined
}

export async function getAddresses(accessToken) {
  const response = accessToken
    ? await authApi.get('/addresses', authConfig(accessToken))
    : await authApi.get('/addresses')
  return response.data.data
}

export async function getAddress(addressId, accessToken) {
  const response = accessToken
    ? await authApi.get(`/addresses/${addressId}`, authConfig(accessToken))
    : await authApi.get(`/addresses/${addressId}`)
  return response.data.data
}

export async function createAddress(address, accessToken) {
  const response = accessToken
    ? await authApi.post('/addresses', address, authConfig(accessToken))
    : await authApi.post('/addresses', address)
  return response.data.data
}

export async function updateAddress(addressId, address, accessToken) {
  const response = accessToken
    ? await authApi.put(`/addresses/${addressId}`, address, authConfig(accessToken))
    : await authApi.put(`/addresses/${addressId}`, address)
  return response.data.data
}

export async function deleteAddress(addressId, accessToken) {
  const response = accessToken
    ? await authApi.delete(`/addresses/${addressId}`, authConfig(accessToken))
    : await authApi.delete(`/addresses/${addressId}`)
  return response.data.data
}

export async function setDefaultAddress(addressId, accessToken) {
  const response = accessToken
    ? await authApi.patch(`/addresses/${addressId}/default`, undefined, authConfig(accessToken))
    : await authApi.patch(`/addresses/${addressId}/default`)
  return response.data.data
}
