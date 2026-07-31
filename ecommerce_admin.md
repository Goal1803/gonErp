# E-commerce / POD Orders Management Module

## Overview

POD (Print on Demand) order management system integrated into gonERP. Manages stores, imports orders from sales platforms (Etsy, Amazon, etc.), tracks order fulfillment through a visual board, and provides role-based access control.

---

## Module Toggle

- `module_ecommerce` boolean on Organization (default false)
- Enabled by superadmin or org admin in Organization settings

---

## Store Management

### Store Entity (`ecom_stores`)
- Name, Sales Channel, Currency, Store URL, Active status
- Organization-scoped

### Sales Channels (enum)
`ETSY`, `AMAZON`, `EBAY`, `WALMART`, `SHOPIFY`, `OTHER`

### Store Member Roles (`ecom_store_members`)
Each store has members with assigned roles:

| Role | Description |
|------|-------------|
| `STORE_ADMIN` | Full access, manage store settings and members |
| `SELLER` | View all order data including financials |
| `SELLER_SUPPORT` | Same visibility as Seller |
| `FULFILLMENT_STAFF` | View customer address, tracking, fulfillment cost |
| `DESIGNER` | View product/SKU info and order status only |
| `MEMBER` | View product info and order status (read-only) |

### Role-Based Field Visibility

| Field | ADMIN | SELLER / SELLER_SUPPORT | FULFILLMENT | DESIGNER | MEMBER |
|-------|-------|------------------------|-------------|----------|--------|
| Customer address | Yes | Yes | Yes | No | No |
| Customer email/phone | Yes | Yes | Yes | No | No |
| Financials (price, fees, profit) | Yes | Yes | No | No | No |
| Fulfillment cost | Yes | Yes | Yes | No | No |
| Tracking | Yes | Yes | Yes | No | View |
| Product/SKU/Variations | Yes | Yes | Yes | Yes | Yes |
| Order status | Yes | Yes | Yes | Yes | View |

---

## Order Entity (`ecom_orders`)

Single entity for all sales channels. Platform-specific data stored in JSONB.

### Common Fields
- `platformOrderId` — Etsy Order ID / Amazon order-id
- `platformItemId` — Etsy Transaction ID / Amazon line item
- `orderDate` — when customer placed the order
- `salesChannel` — which platform
- `store` FK — which store

### Customer & Shipping
- `customerName`, `customerEmail`, `customerPhone`
- `shipName`, `shipStreet1`, `shipStreet2`, `shipCity`, `shipState`, `shipZipcode`, `shipCountry`

### Product
- `productName`, `sku`, `listingId`, `variations`, `quantity`

### Financials
- `currency`, `itemPrice`, `shippingPrice`, `tax`, `discount`
- `processingFees`, `orderTotal`, `orderNet`
- `fulfillmentCost` (manually entered)
- `otherCost` (manually entered)
- `grossProfit` (auto-calculated: orderNet - fulfillmentCost - otherCost)

### Fulfillment
- `status` (OrderStatus enum — matches board columns)
- `trackingNumber`, `trackingStatus`, `shippedDate`
- `refunded` (boolean)

### Data Preservation
- `rawData` (JSONB) — full original CSV row, never modified
- `platformData` (JSONB) — platform-specific structured fields

### Board Link
- `card` FK (nullable) — linked to a Card in a POD_ORDER board for visual tracking
- Moving the card automatically syncs the Order status

---

## Order Status (enum)

Matches the POD_ORDER board columns:

```
DRAFT → NEW_ORDER → CLARIFY_WITH_CUSTOMER → CONFIRMED →
DESIGNING → DESIGN_CHECKING → NEED_TO_FIX → FIX_CHECKING →
FIXING → CONFIRMING_DESIGN_WITH_CUSTOMER → DESIGN_APPROVED →
FULFILLED → TRACK_GENERATED → TRACK_ADDED_TO_STORE →
COMPLETED / CANCELLED
```

---

## Order Board (`POD_ORDER` board type)

- Auto-creates 14 columns matching the order workflow
- Cards can be linked to orders — moving a card updates the order status
- Supports all existing board features: members, comments, attachments, labels, types, drag-and-drop

### Board Columns
1. Draft
2. New Order
3. Clarify with Customer
4. Confirmed
5. Designing
6. Design Checking
7. Need to Fix
8. Fix Checking
9. Fixing
10. Confirming Design with Customer
11. Design Approved
12. Fulfilled
13. Track Generated
14. Track Added to Store

---

## Etsy CSV Import

### Supported Files
1. **EtsySoldOrderItems CSV** (required) — one row per item, contains product details, shipping address, variations
2. **EtsySoldOrders CSV** (optional) — one row per order, contains order-level financials

### Import Logic
- Each item row creates one `EcomOrder` record
- Multi-item orders: multiple records share the same `platformOrderId`
- Order-level data (financials, status) joined from the orders file by Order ID
- Duplicate detection: skips items already imported (by platformItemId + storeId)
- Raw CSV row preserved in `rawData` JSONB
- Platform-specific fields stored in `platformData` JSONB

### CSV Parsing
- RFC 4180 compliant parser (handles multi-line quoted fields)
- Etsy Variations field contains newlines within quotes — properly handled
- HTML entity decoding (`&quot;` → `"`)
- Date formats: MM/DD/YY (sale date), MM/DD/YYYY (paid/shipped dates)

---

## Frontend Pages

### E-commerce Landing (`/ecommerce`)
- Module home with cards: Stores, Orders (future: Products)

### Stores (`/ecommerce/stores`)
- Store list with CRUD
- Store detail page with member/role management

### Orders (`/ecommerce/orders`)
- Order list with filters: store, status, date range, search
- Import dialog: upload Etsy CSVs per store
- Pagination (20 per page)

### Order Detail (`/ecommerce/orders/:id`)
- Two-column layout:
  - Left: Product Info, Financials (with inline cost editing), Notes
  - Right: Customer & Shipping, Fulfillment (tracking, status), Platform Data, Raw Data
- Gross profit auto-calculated and color-coded (green/red)
- Status editable via dropdown

---

## API Endpoints

### Stores
- `GET /api/ecommerce/stores` — list stores
- `GET /api/ecommerce/stores/{id}` — get store
- `POST /api/ecommerce/stores` — create store
- `PUT /api/ecommerce/stores/{id}` — update store
- `DELETE /api/ecommerce/stores/{id}` — delete store

### Store Members
- `GET /api/ecommerce/stores/{storeId}/members` — list members
- `POST /api/ecommerce/stores/{storeId}/members` — assign member with role
- `DELETE /api/ecommerce/stores/{storeId}/members/{userId}` — remove member

### Orders
- `GET /api/ecommerce/orders` — list orders (params: storeId, status, startDate, endDate)
- `GET /api/ecommerce/orders/{id}` — get order detail
- `PUT /api/ecommerce/orders/{id}` — update order (status, tracking, costs, notes)
- `DELETE /api/ecommerce/orders/{id}` — delete order

### Import
- `POST /api/ecommerce/import/etsy/{storeId}` — import Etsy CSVs (multipart: itemsFile required, ordersFile optional)

---

## Implementation Status

- [x] Phase 1: Store & Member Management (entity, CRUD, roles, UI)
- [x] Phase 2: Order Entity & Etsy CSV Import (parser, importer, list page)
- [x] Phase 3: Order Detail Page (inline editing, financials, raw data)
- [x] Phase 4: Order Board Integration (POD_ORDER board type, status sync)
- [ ] Phase 5: Amazon CSV Import
- [ ] Phase 6: Order Dashboard (metrics, charts)
- [ ] Phase 7: Tracking Integration (auto-status from tracking number)
- [ ] Phase 8: Order ↔ Design linking (by SKU match)

---

## Future Enhancements

- Amazon order import (tab-separated format)
- eBay, Walmart, Shopify importers
- Automatic tracking status updates via carrier APIs
- Order dashboard with sales metrics, fulfillment rate, avg processing time
- SKU-based linking between orders and POD_DESIGN designs
- Bulk order operations (status update, export)
- Order notifications (new order, status change)
- Customer communication templates
