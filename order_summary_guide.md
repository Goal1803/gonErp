# Etsy Order Summary — Build Guide

Quy trình tổng hợp Etsy statement + supplier files thành 1 file `order_summary_YYYY-MM.xlsx` (3 sheets).

## Input files (per shop folder)

| File | Format | Mục đích |
|---|---|---|
| `etsy_statement_YYYY_M.csv` | CSV | Tất cả giao dịch tháng (Sale, Fee, VAT, Tax, Refund, Marketing, Deposit) |
| `EtsySoldOrders*.csv` | CSV | 1 row / order — customer, address, SKU |
| `EtsySoldOrderItems*.csv` | CSV | 1 row / item (chi tiết item) |
| Supplier file(s) | xls/xlsx/csv | Đơn fulfillment + chi phí (Interestprint, Merchize, …) |

**Currency:** mỗi shop khác nhau (VND / EUR / USD). Đọc từ cột `Currency` trong statement.

## Output: 3 sheets

1. **Order Summary** — 1 row / Etsy order với tài chính + customer + supplier
2. **Other Etsy Charges** — shop-level fees (Ads, Listing fee, VAT shop-level...) grouped by Category
3. **Unmatched Supplier Orders** — đơn supplier trong tháng chưa match với Etsy

---

## Statement aggregation rules

### Order ID extraction
Quét `Info` rồi `Title`, các pattern:
```python
re.search(r'[Oo]rder[: #]+(\d{8,})', txt)   # "Order #1234567890" hoặc "order: 1234567890"
re.search(r'\brefund (\d{8,})\b', txt)       # "Charge for refund 1234567890"
```

⚠️ **KHÔNG** match:
- `Listing #...` (listing ID, không phải order)
- `auto-renew sold: ...`, `listing: ...`, `renew sold auto credit: ...`, `listing credit: ...`

### 🐛 BUG cần tránh
```python
if not oid: continue   # SAI — không catch NaN
if pd.isna(oid) or not oid: continue   # ĐÚNG
```
Nếu sai, tất cả non-order rows (NaN OID) sẽ bị gộp thành 1 row "ma" với grand total = Sheet 2 grand total → **trùng lặp**.

### Per-order aggregation (Type → field)
| Type | Field | Lấy từ |
|---|---|---|
| Sale | Sale Amount | Amount |
| Refund / Payment | Refund Amount | Amount |
| VAT | VAT | Net |
| Fee | Etsy Fee | Net |
| Tax | Tax | Net |
| Buyer Fee | Buyer Fee | Net |
| (all) | Net Earning | sum(Net) |

`Total Platform Fee = VAT + Etsy Fee + Tax + Buyer Fee` (negative = chi phí)

### Money parsing
```python
def parse_money(s):
    if pd.isna(s) or s in ('--','',None): return 0.0
    s = str(s).replace('₫','').replace('$','').replace('€','').replace(',','').strip()
    if s in ('--',''): return 0.0
    try: return float(s)
    except: return 0.0
```

---

## Sold Orders merge
Merge by exact `Order ID`. Cột giữ lại:
```
Order ID, Sale Date, Status, Number of Items, SKU, Full Name,
Street 1, Street 2, Ship City, Ship State, Ship Zipcode, Ship Country, Date Shipped
```

---

## Supplier matching rules

### Match keys (theo thứ tự ưu tiên)

1. **Etsy Order ID found in supplier field** (mạnh nhất):
   - Merchize: `Item name` ≈ Etsy Order ID (10-digit, bắt đầu bằng 4)
   - Interestprint: trong `Message` column tìm 10-digit number ∈ etsy_oids
   - Special: supplier `Full Name` đôi khi chính là Etsy Order ID (10-digit)
2. **Name + ZIP** (chuẩn): với normalization
3. **Name + 5-digit ZIP prefix** (fallback): khi ZIP+4 khác

### 🔑 Name normalization (QUAN TRỌNG)
```python
def norm_loose(s):
    if s is None: return None
    n = re.sub(r'\s+', ' ', str(s).strip().lower())   # collapse spaces
    return n.replace("'",'').replace('’','').replace('‘','')  # drop apostrophes
```

**Tại sao bắt buộc:**
- Etsy có thể lưu `Alicia  Grant` (2 spaces) ↔ supplier `Alicia Grant` (1 space)
- Supplier có thể lưu `O Hara` (apostrophe bị strip) ↔ Etsy `O'Hara`
- Etsy `O'Connor` (Unicode `’` U+2019) ↔ supplier có khi chứa entity-mangled

### Case rare cần fill manual (đừng đổi rule):
- Emoji trong tên: `Adrien Lara 🩷`
- Ký tự `&`: supplier XML lưu `&amp;` → cần decode về `&`
- Supplier `Full Name = Etsy Order ID` (10-digit number)
- `c/o` addresses: supplier name = c/o name (`Shelley Lesley c/o Jennifer Seawell` ↔ supplier `Jennifer`)
- Same name nhưng khác ZIP/state: cảnh giác, có thể là 2 người khác nhau (ví dụ Deepika Kurup CA vs GA → KHÔNG match)
- Same name khác ZIP cùng metro area: có thể fill nhưng flag (Ladys Roman 77494 vs 77447, cùng Houston)

---

## Supplier file parsing

### Interestprint XML SpreadsheetML (`.xls` nhưng là XML)
```python
import xml.etree.ElementTree as ET, re
with open(path) as f: content = f.read()

# CRITICAL: decode HTML entities thủ công (XML không định nghĩa rsquo, nbsp, ...)
ENT = {'&rsquo;':'’','&lsquo;':'‘','&rdquo;':'”','&ldquo;':'“','&nbsp;':' ','&amp;':'&',
    '&aacute;':'á','&eacute;':'é','&iacute;':'í','&oacute;':'ó','&uacute;':'ú',
    '&ntilde;':'ñ','&uuml;':'ü','&ouml;':'ö','&auml;':'ä','&szlig;':'ß',
    '&ccedil;':'ç','&egrave;':'è','&agrave;':'à'}
for k,v in ENT.items(): content = content.replace(k,v)
# Strip remaining unknown entities (rare)
content = re.sub(r'&(?!(amp|lt|gt|quot|apos|#\d+);)([a-zA-Z]+);', r'', content)

root = ET.fromstring(content)
ns = {'ss':'urn:schemas-microsoft-com:office:spreadsheet'}
ws = root.find('ss:Worksheet', ns)
rows = ws.find('ss:Table', ns).findall('ss:Row', ns)

# Sparse cells: cell có thể skip columns, dùng ss:Index attribute
def cells_idx(r):
    out=[]
    for c in r.findall('ss:Cell', ns):
        idx = c.get('{urn:schemas-microsoft-com:office:spreadsheet}Index')
        d = c.find('ss:Data', ns)
        out.append((idx, d.text if d is not None else None))
    return out

headers = [v for _,v in cells_idx(rows[0])]
data=[]
for r in rows[1:]:
    row=[None]*len(headers); cur=0
    for idx,val in cells_idx(r):
        if idx is not None: cur=int(idx)-1   # explicit position
        if cur < len(row): row[cur]=val
        cur+=1
    data.append(row)
```

⚠️ **BUG cần tránh:** đừng strip `&rsquo;` thành chuỗi `rsquo` (làm `O'Connor` → `OrsquoConnor`). Phải decode về `’`.

### Binary XLS misnamed as `.xlsx` (e.g., sup1_interestprint2)
```python
import xlrd
wb = xlrd.open_workbook(path, ignore_workbook_corruption=True)
sh = wb.sheet_by_index(0)
hdr = [sh.cell_value(0,c) for c in range(sh.ncols)]
data = [dict(zip(hdr, [sh.cell_value(r,c) for c in range(sh.ncols)])) for r in range(1, sh.nrows)]
```

### Cột mapping điển hình
| Trường | Interestprint | Interestprint v2 | Merchize |
|---|---|---|---|
| Order # | `Order ID` | `Order No:(Store)` | `Number` |
| Date | `Date` | `Create At` | `Created at` |
| Customer | `Full Name` | `Name` | `Shipping name` |
| ZIP | `Postal Code` | `State` (đúng — cột tên ZIP nó đặt là State) | `Shipping Zip` |
| State (US) | `State/Region` | `state` (lowercase) | `Shipping Province` |
| City | `City` | `City` | `Shipping city` |
| Cost | `Amount` | `Total Amount` | `Fulfillment cost` |
| Status | `Status` | `Status` | `Fulfillment status` |
| Etsy OID hint | `Message` | `Memo` | `Item name` |

---

## Multi-order / ambiguity resolution

Khi 1 customer có **N đơn Etsy + M đơn supplier**:

- **N=1, M=1:** trivial
- **N=1, M>1:** sum, gộp tất cả vào 1 dòng
- **N>1, M>1, N=M:** redistribute 1:1 theo chronological order (sort cả 2 theo ngày, pair)
- **N>1, M=1 hoặc N≠M:** thêm 1 dòng "consolidation" ngay TRÊN dòng đầu của customer group đó (KHÔNG ở top of file). Các dòng còn lại note `Supplier = "đã add ở đơn khác"`.

Khi nghi ngờ → hỏi user trước khi auto-fill.

---

## Output structure

### Sheet 1: Order Summary — column order
```
Order ID, Sale Date, Status, Number of Items, SKU,
Full Name, Street 1, Street 2, Ship City, Ship State, Ship Zipcode, Ship Country,
Sale Amount, Refund Amount, VAT, Etsy Fee, Tax, Buyer Fee, Total Platform Fee, Net Earning,
Supplier, Total Fulfillment Cost (USD),
<sup1>_cost, <sup1>_orders, <sup1>_status,
<sup2>_cost, <sup2>_orders, <sup2>_status,
Date Shipped
```
Sort: `Full Name` ASC, NaN cuối.

### Sheet 2: Other Etsy Charges — categories điển hình
- `Etsy Ads` (Marketing)
- `Listing Fee`, `Listing Fee Credit`
- `VAT on Etsy Ads`, `VAT on Listing Fee`, `VAT on Reg Op Fee`, `VAT on Auto-renew`, `VAT on Shipping Transaction (shop-level)`
- `Bank Deposit (transfer, not a cost)`
- Plus 1 dòng `GRAND TOTAL`

### Sheet 3: Unmatched Supplier Orders
Filter rows trong tháng (`Date.startswith('2026-04')`) chưa match. Cột: Supplier, Order #, Date, Customer, Address, City, State, ZIP, Country, SKU, Qty, Cost, Status, Memo.

### Format tip
```python
from openpyxl.utils import get_column_letter
for i, col in enumerate(df.columns, 1):
    maxlen = max(len(str(col)), df[col].astype(str).str.len().max() if len(df) else 0)
    ws.column_dimensions[get_column_letter(i)].width = min(maxlen+2, 50)
```

---

## Workflow & rules với user

1. **Inspect** — chạy `head` trên các file CSV, xác định format supplier (XML / binary / csv)
2. **Build full pipeline** — chạy 1 lần tạo file
3. **Verify** — báo match rate, ask để xác nhận trước khi đụng nghi ngờ
4. **Iterate** — user phát hiện thiếu → quét fuzzy → fill manual targeted
5. **Preserve manual edits** — không re-generate sheet, dùng `openpyxl.cell().value =` cho surgical edit. Khi fix bug parser, **đừng đổi rule** auto-match — chỉ fill cells cụ thể user chỉ ra.
6. **Lock file warning** — nếu thấy `~$<file>.xlsx`, file đang mở trong Excel. **Đừng ghi**, hoặc ghi xong cảnh báo user "đóng Excel KHÔNG save" rồi mở lại.

---

## Cross-shop supplier files
2 shop có thể dùng chung 1 file supplier (ví dụ `Gondesignss` + `gonofficial april` cùng dùng `table20260507104402.xls`). Hệ quả:
- Số supplier orders chưa match (Sheet 3) ở mỗi shop sẽ rất cao — vì chứa cả đơn của shop kia
- Để cross-check chính xác, có thể loại trừ chéo giữa 2 file
