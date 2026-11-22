QUICKMART POS - NETBEANS PROJECT

REQUIREMENTS
- JDK 17
- NetBeans with Maven support
- MySQL Server 8.x and MySQL Workbench
- Webcam for webcam barcode scanning (manual barcode entry also works)

DATABASE SETUP
1. Start MySQL Server.
2. Open MySQL Workbench as an administrator.
3. Open database.sql and execute the whole script.
4. It creates quickmart_pos, related tables, terminal T1, sample grocery products,
   and the restricted quickmart_app database account.
5. For an existing QuickMart database, run upgrade_product_discounts.sql before
   launching this version. Existing products start with a 0% discount and
   existing sales keep their original prices and totals.
6. On an existing database, also run upgrade_product_photos.sql. Both upgrade
   scripts are safe to rerun. For a fresh setup, database.sql includes both.

DATABASE DIAGRAM
- erd/POS_ERD.png shows the MySQL schema defined in database.sql.

OPEN IN NETBEANS
1. Extract the project ZIP.
2. In NetBeans choose File > Open Project.
3. Select the QuickMartPOS folder containing pom.xml.
4. Wait for Maven dependencies to download.
5. Right-click project > Clean and Build.
6. Right-click project > Run.

DEFAULT LOGIN
- cashier / cashier123
- admin / admin123
The Java application creates these users on first launch if users is empty.
Change these passwords before any real deployment.

DEFAULT DATABASE CONNECTION
URL: jdbc:mysql://localhost:3306/quickmart_pos
User: quickmart_app
Password: ChangeThis_QM_2026!
You can override with environment variables:
QUICKMART_DB_URL, QUICKMART_DB_USER, QUICKMART_DB_PASSWORD.

FEATURES IN THIS STARTER
- Cashier login and cashier session open/close
- Session history and cash reconciliation
- Product add, view, update, deactivate, and stock adjustment
- Barcode lookup by typing or webcam
- Cart, quantity update, remove item, cancel sale, refresh prices
- Product percentage discounts (set Discount (%) under Products)
- Optional product photos from an image file or webcam, with offline background removal
- Cash checkout, change calculation, inventory deduction
- Receipt preview, TXT save, and Java print dialog
- Sales history, date filter, totals, and sale details
- Audit logs, stock movement records, related MySQL tables

PRODUCT DISCOUNTS
Set Discount (%) when adding or updating a product. Use 0 for regular price.
In the cart, Refresh prices reloads current prices and discounts for existing
items. The POS rounds the discounted unit price to two decimal places before
multiplying by quantity. Receipts and sale details show the applied discount.

PRODUCT PHOTOS
Under Products, add or update a product, then click Upload Photo or Take Photo.
Choose Use White Background or Keep Original in the preview. The photo saves
when you confirm the product form. Remove clears a product photo.

One-time background removal setup (Windows):
1. Download u2netp.onnx from the rembg project:
   https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2netp.onnx
2. Create the folder %USERPROFILE%\.quickmart and put u2netp.onnx in it.
3. Add a photo. QuickMart runs the model locally on the CPU after capture or
   upload. Without the model, you can still save the original image.

Use a single product centered against a simple background. Inspect the result
before saving, especially for clear or reflective packaging.

WEBCAM
In either camera dialog, select a camera and resolution, then click Start.
The preview displays the webcam driver's approximate capture FPS. Try a lower
resolution and brighter light if the frame rate is low. The built-in driver
does not expose a requested FPS setting. The actual rate depends on the
camera mode and driver, even if the camera is advertised as 60 FPS.
Barcode decoding checks the latest frame around four times a second, on a
separate worker. If the camera cannot read a code, type it manually.

LIMITATIONS
This is an educational starter, not a certified commercial POS.
Cash payment is implemented; payment gateway integration is not.
The schema contains a sale_reversals table, but a refund workflow is not included.
Multi-laptop networking is intentionally not configured yet.
A printer must be installed/configured in the operating system for physical printing.
Receipts are educational receipts, not tax invoices.
