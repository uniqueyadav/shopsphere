export const ADDRESS_TYPES = [
  { value: 'HOME', label: 'Home' },
  { value: 'WORK', label: 'Work' },
  { value: 'OTHER', label: 'Other' },
]

export function normalizeAddressPayload(values) {
  const rawAddressType = String(values.addressType ?? '').trim().toUpperCase()
  const cleaned = {
    fullName: String(values.fullName ?? '').trim(),
    mobileNumber: String(values.mobileNumber ?? '').replace(/\s+/g, '').trim(),
    houseFlatNumber: String(values.houseFlatNumber ?? '').trim(),
    streetArea: String(values.streetArea ?? '').trim(),
    landmark: String(values.landmark ?? '').trim() || null,
    city: String(values.city ?? '').trim(),
    state: String(values.state ?? '').trim(),
    pinCode: String(values.pinCode ?? '').replace(/\s+/g, '').trim(),
    addressType: ADDRESS_TYPES.some((type) => type.value === rawAddressType) ? rawAddressType : 'HOME',
    defaultAddress: Boolean(values.defaultAddress),
    deliveryInstructions: String(values.deliveryInstructions ?? '').trim() || null,
    leavePackageAtDoor: Boolean(values.leavePackageAtDoor),
    callBeforeDelivery: Boolean(values.callBeforeDelivery),
  }

  return cleaned
}
