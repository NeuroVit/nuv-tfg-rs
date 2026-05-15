# Contrato de Ventas (v1)

## VentaResponse
- `id: number`
- `clientId: number`
- `clientName: string`
- `soldAt: string (fecha-hora ISO)`
- `notas: string | null`
- `subtotalAmount?: number`
- `discountTotalAmount?: number`
- `globalDiscountPct?: number`
- `totalAmount: number`
- `createdAt: string (fecha-hora ISO)`
- `items: VentaItem[]`

## VentaItem
- `productId: number`
- `bundleId?: number | null`
- `bundleName?: string | null`
- `productoNombre: string`
- `sku: string`
- `quantity: number`
- `unitPriceSnapshot: number`
- `subtotal: number`
