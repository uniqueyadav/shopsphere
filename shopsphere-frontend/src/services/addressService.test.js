import { afterEach, describe, expect, it, vi } from 'vitest'
import { normalizeAddressPayload } from '../utils/addressPayload'
import { authApi } from './authService'
import {
  createAddress,
  deleteAddress,
  getAddress,
  getAddresses,
  setDefaultAddress,
  updateAddress,
} from './addressService'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('address API service', () => {
  it('normalizes address form values before saving', () => {
    const payload = normalizeAddressPayload({
      fullName: '  A Customer  ',
      mobileNumber: ' 98765 43210 ',
      houseFlatNumber: ' 12B ',
      streetArea: ' Market Road ',
      landmark: '  Near station ',
      city: ' Jaipur ',
      state: ' Rajasthan ',
      pinCode: ' 302 001 ',
      addressType: 'home',
      defaultAddress: true,
      deliveryInstructions: '  Call after 6 PM  ',
      leavePackageAtDoor: false,
      callBeforeDelivery: true,
    })

    expect(payload).toEqual({
      fullName: 'A Customer',
      mobileNumber: '9876543210',
      houseFlatNumber: '12B',
      streetArea: 'Market Road',
      landmark: 'Near station',
      city: 'Jaipur',
      state: 'Rajasthan',
      pinCode: '302001',
      addressType: 'HOME',
      defaultAddress: true,
      deliveryInstructions: 'Call after 6 PM',
      leavePackageAtDoor: false,
      callBeforeDelivery: true,
    })
  })

  it('uses the authenticated CRUD and default-address endpoints', async () => {
    const addresses = [{ id: 4, fullName: 'A Customer' }]
    const address = addresses[0]
    const payload = {
      fullName: 'A Customer',
      mobileNumber: '9876543210',
      houseFlatNumber: '12B',
      streetArea: 'Market Road',
      landmark: '',
      city: 'Jaipur',
      state: 'Rajasthan',
      pinCode: '302001',
      addressType: 'HOME',
      defaultAddress: true,
    }
    const get = vi.spyOn(authApi, 'get')
      .mockResolvedValueOnce({ data: { data: addresses } })
      .mockResolvedValueOnce({ data: { data: address } })
    const post = vi.spyOn(authApi, 'post').mockResolvedValue({ data: { data: address } })
    const put = vi.spyOn(authApi, 'put').mockResolvedValue({ data: { data: address } })
    const remove = vi.spyOn(authApi, 'delete').mockResolvedValue({ data: { data: null } })
    const patch = vi.spyOn(authApi, 'patch').mockResolvedValue({ data: { data: address } })

    await expect(getAddresses()).resolves.toEqual(addresses)
    await expect(getAddress(4)).resolves.toEqual(address)
    await expect(createAddress(payload, 'access-token')).resolves.toEqual(address)
    await expect(updateAddress(4, payload)).resolves.toEqual(address)
    await expect(deleteAddress(4)).resolves.toBeNull()
    await expect(setDefaultAddress(4)).resolves.toEqual(address)

    expect(get).toHaveBeenNthCalledWith(1, '/addresses')
    expect(get).toHaveBeenNthCalledWith(2, '/addresses/4')
    expect(post).toHaveBeenCalledWith('/addresses', payload, {
      _useExplicitAuth: true,
      headers: { Authorization: 'Bearer access-token' },
    })
    expect(put).toHaveBeenCalledWith('/addresses/4', payload)
    expect(remove).toHaveBeenCalledWith('/addresses/4')
    expect(patch).toHaveBeenCalledWith('/addresses/4/default')
  })

  it('keeps the explicit bearer token on the actual Axios request', async () => {
    const address = { id: 4, fullName: 'A Customer' }
    const adapter = vi.fn(async (config) => ({
      config,
      data: { data: address },
      headers: {},
      status: 201,
      statusText: 'Created',
    }))
    const originalAdapter = authApi.defaults.adapter
    authApi.defaults.adapter = adapter

    try {
      await expect(createAddress({ fullName: 'A Customer' }, 'access-token')).resolves.toEqual(address)
      expect(adapter.mock.calls[0][0].headers.get('Authorization')).toBe('Bearer access-token')
    } finally {
      authApi.defaults.adapter = originalAdapter
    }
  })
})
