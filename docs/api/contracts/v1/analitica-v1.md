# Contrato de Analítica (v1)

## InventorySummaryRowResponse
- `productId: number`
- `sku: string`
- `productName: string`
- `categoryName: string`
- `currentQuantity: number`
- `minimumStockLevel: number`
- `unitPrice: number`
- `stockValue: number`
- `stockStatus: string`

## SalesVelocityRowResponse
- `productId: number`
- `sku: string`
- `productName: string`
- `categoryName: string`
- `movementDate: string (fecha ISO)`
- `unitsSold: number`
- `revenue: number`
