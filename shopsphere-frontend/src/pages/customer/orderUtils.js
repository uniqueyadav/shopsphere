export function getOrderStatusLabel(status) {
  return String(status || '')
    .toLowerCase()
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export function canCancelOrder(order) {
  return Boolean(
    order?.sellerOrders?.length
    && order.sellerOrders.every((sellerOrder) => ['PLACED', 'CONFIRMED'].includes(sellerOrder.orderStatus)),
  )
}

export async function reorderOrder(order, addItem) {
  const failedItems = []
  let addedItems = 0

  for (const sellerOrder of order.sellerOrders || []) {
    for (const item of sellerOrder.items || []) {
      try {
        await addItem({ productId: item.productId, quantity: item.quantity })
        addedItems += 1
      } catch {
        failedItems.push(item.productName)
      }
    }
  }

  return { addedItems, failedItems }
}
