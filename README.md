# BlueItems 2.0.0

Plugin cho Paper 1.21.11 (BlueTequila SMP):
- **Emoji và huy hiệu hình** (gõ `:ten:` trong chat, tên item, menu, bảng tab, sidebar)
- **Item custom** (texture riêng)
- **Menu GUI**
- **Bảng tab**: logo, huy hiệu rank trước tên, header/footer
- **Sidebar** bên phải màn hình với icon
- **Thông báo tự động** xoay vòng trong chat
- **Định dạng chat** có huy hiệu rank (tắt sẵn)

Không có: furniture, block custom, mob, armor custom, tự tạo/host resource pack.

## 1. Build ra file .jar bằng GitHub Actions (chỉ cần trình duyệt)
1. Giải nén `BlueItems-source.zip`.
2. Trên github.com, vào repo `BlueItems`, bấm **Add file** > **Upload files**, kéo thả TOÀN BỘ nội dung bên trong thư mục `BlueItems` (kể cả thư mục `.github`) đè lên file cũ, rồi **Commit changes**.
3. Mở tab **Actions**, đợi lần chạy "Build BlueItems" có dấu tick xanh (1 đến 2 phút), tải artifact `BlueItems`, giải nén ra `BlueItems.jar`.
   Nếu có dấu X đỏ: mở lần chạy đó, copy các dòng `ERROR` gửi người hỗ trợ.

## 2. Cập nhật từ bản cũ
Bản 2.0 có thêm file cấu hình mới (`tab.yml`, `sidebar.yml`, `announcements.yml`, `chat.yml`) và thêm emoji.
Plugin chỉ tạo file mặc định khi file chưa tồn tại, nên:
1. Tắt server, xóa thư mục `plugins/BlueItems/` cũ (copy ra chỗ khác nếu đã sửa gì).
2. Xóa `BlueItems.jar` cũ, bỏ jar mới vào `plugins/`, khởi động lại.
3. Nếu đang cài plugin **TAB** hoặc plugin sidebar/scoreboard khác thì tắt chúng đi, vì sẽ tranh nhau với BlueItems.

## 3. Lệnh và quyền
| Lệnh | Quyền | Việc làm |
|---|---|---|
| `/bi emojis` | ai cũng dùng | xem danh sách emoji |
| `/bi sidebar` | ai cũng dùng | ẩn/hiện bảng thông tin bên phải |
| `/bi menu <id>` | menu tự đặt quyền | mở menu |
| `/bi menu <id> <người chơi>` | blueitems.admin | mở menu cho người khác |
| `/bi give <người chơi> <id> [số lượng]` | blueitems.admin | tặng item custom |
| `/bi list` | blueitems.admin | liệt kê item, menu |
| `/bi reload` | blueitems.admin | tải lại toàn bộ cấu hình |

Quyền khác: `blueitems.emoji` (mặc định ai cũng có) dùng emoji trong chat; `blueitems.badge` (mặc định OP)
dùng huy hiệu rank và logo trong chat để người thường không giả mạo rank; `blueitems.commands` (mặc định OP)
cho phép gõ `:tên:` ngay trong lệnh.

## 4. Các file cấu hình (plugins/BlueItems/)
Mọi file đều dùng được: MiniMessage (`<red>`, `<gradient:red:blue>`), mã màu `&c`, emoji `:ten:`, PlaceholderAPI.
Placeholder có sẵn, không cần PlaceholderAPI: `%player%`, `%online%`, `%max%`, `%ping%`.

- `emojis.yml`: emoji và huy hiệu. Mỗi emoji có thể đặt quyền riêng cho chat.
- `items.yml`: item custom (tên, lore, enchant, glow, không vỡ, `model` cho texture).
- `menus.yml`: menu GUI, hành động `[player] [console] [message] [open] [close] [sound] [give]`.
- `tab.yml`: header/footer, logo, định dạng tên trong bảng tab. Đổi dòng `name_format` nếu muốn thêm/bớt prefix, suffix.
- `sidebar.yml`: tiêu đề và tối đa 15 dòng. Placeholder mặc định dùng Vault, PlayerPoints, Statistic.
  Muốn thêm dòng shards/team của UltimateDonutSmp, hãy dùng đúng placeholder của plugin đó (xem tài liệu của nó).
- `announcements.yml`: thông báo tự động, thời gian lặp, tiền tố có icon.
- `chat.yml`: định dạng chat có huy hiệu. TẮT sẵn vì có thể đánh nhau với plugin/script chat khác.
- `config.yml`: tiền tố tin nhắn của plugin.

### Gắn huy hiệu cho rank
Admin (có `blueitems.commands`) hoặc console gõ `:tên:` ngay trong lệnh, ví dụ với LuckPerms:

    /lp group default meta setprefix 100 ":badge_member: "
    /lp group admin meta setprefix 100 ":badge_admin: "

Prefix lưu trong LuckPerms; bảng tab (`name_format` ở `tab.yml`) và chat (`chat.yml`) đọc nó qua `%luckperms_prefix%`.

## 5. Resource pack (BlueTequila-ResourcePack.zip)
Glyph nằm trong **font mặc định**, nên ký tự `\uE0xx` hiện thành hình ở mọi nơi (chat, tab, scoreboard, prefix, lore, menu...).

| Emoji | Ký tự | Nội dung |
|---|---|---|
| smile | U+E000 | mặt cười |
| badge_owner / admin / mod / vip / member | U+E010 ... U+E014 | huy hiệu rank có icon (cao 9px) |
| icon_coin / gem / clock / shard / user / sword / key / team / discord / info | U+E020 ... U+E029 | icon 9x9 |
| logo | U+E030 | logo 3D BLUE TEQUILA SMP (316x29px) |

Logo cao hơn một dòng chữ, nên trong header của tab để **3 dòng trống** ngay dưới (đã sẵn trong `tab.yml`).

Đổi hình: ghi đè file PNG trong `assets/blueitems/textures/font/` (huy hiệu và icon giữ chiều cao 9px),
hoặc sửa `resourcepack-tools/build_pack.py` (đổi chữ/màu huy hiệu, thêm huy hiệu) rồi chạy `python3 build_pack.py`.
Khi thêm glyph mới, thêm dòng tương ứng vào `emojis.yml`. Logo vẽ lại bằng `make_logo3d.py` (cần Pillow và numpy).

### Font chữ riêng cho toàn bộ game
Cần một file `.ttf` có hỗ trợ tiếng Việt và bạn có quyền dùng. Chạy:

    python3 build_pack.py --font TenFont.ttf --font-size 10

Font áp dụng cho MỌI chữ trong game (chat, tab, menu, tên item). Kích thước và độ lệch dòng cần thử trong game
và chỉnh `--font-size` cho vừa.

### Cho người chơi tải pack
Upload file zip lên nơi có link tải trực tiếp, rồi sửa `server.properties`:

    resource-pack=<link trực tiếp tới file zip>
    resource-pack-sha1=<SHA1 của file zip>
    resource-pack-id=<một UUID bất kỳ>
    require-resource-pack=false

SHA1 trên Windows: `certutil -hashfile BlueTequila-ResourcePack.zip SHA1`. `pack_format` là 75 (Minecraft 1.21.11).
Ai chưa tải pack sẽ thấy ô vuông thay cho huy hiệu và logo.
