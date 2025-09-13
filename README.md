# MoreEnchant

**MoreEnchant** — plugin Minecraft cung cấp hệ thống *enchantments* tùy biến, các level do người chơi đặt tên được, hook mở rộng (ví dụ: ExtraStorage), hiển thị bossbar, và nhiều tính năng tiện ích phục vụ server Paper/Spigot.

> ⚠️ Phiên bản này vẫn đang được phát triển liên tục.
>
> ⚠️ Hiện tại plugin này chỉ hỗ trợ hook duy nhất với ExtraStorageCustom của bên chúng tôi. Tạo issue nếu bạn muốn yêu cầu bổ sung plugin

---

## 📌 Tóm tắt

MoreEnchant hướng tới việc làm cho hệ thống enchant trên server trở nên linh hoạt và dễ mở rộng:

* Enchant tùy chỉnh gắn vào được mọi vật phẩm.
* Hỗ trợ level enchant custom.
* Các command quản lý, hiển thị help, và apply/remove enchant.
* Hook tích hợp với hệ thống lưu trữ ngoại vi (ví dụ: ExtraStorage).
* Bossbar/feedback real-time khi người chơi cầm item có enchant đặc biệt.
* Code được tối ưu để tránh gây tình trạng lag.

---

## ✨ Danh sách Enchant (mô tả tính năng)

> Phần này mô tả từng enchant hiện có trong repo và hướng dẫn nhanh cách cấu hình/ý nghĩa các tham số.
### 1) VirtualExplosion

* **Mô tả:** Tạo hiệu ứng "nổ ảo" khi enchant được kích hoạt — Là phiên bản thay thế của các plugin nổ hiện tại. Giúp fix các vấn đề về hiệu năng/tps máy chủ.
* **File cấu hình:** `plugins/MoreEnchant/enchantments/virtualexplosion.yml`
* **Tham số chính trong ví dụ:**

    * `width` — chiều ngang vùng ảnh hưởng (số block).
    * `height` — chiều cao vùng ảnh hưởng (số block).
    * `depth` — độ sâu vùng ảnh hưởng (số block).
    * `chance` — xác suất % để trigger nổ ảo.
    * `cooldown` — thời gian hồi giữa các lần trigger tính theo giây
* **Ghi chú:**

    * Giữ `width/height/depth` ở mức hợp lý (ví dụ <= 7) để tránh ảnh hưởng hiệu năng.
    * Trong bài test của tôi ở `300% CPU` và `32GiB RAM` và `1 người chơi` thì cho kết quả `20x20x20` ~ `8000 block` thì sẽ gây 1 chút delay nhẹ. Rơi vào tầm khoảng `0.2 - 0.5s` delay.
    * `chance` và `cooldown` là hai tham số chính để cân bằng trải nghiệm game.

**Ví dụ:**

```yaml
explosion_shapes: 
  I:
    width: 3
    height: 3
    depth: 1
    chance: 30.0
    cooldown: 1.0
  II:
    width: 3
    height: 3
    depth: 3
    chance: 50.0
    cooldown: 0.5
virtual_drops: # DropTable cho nổ ảo.
  default: # Khi người chơi không có quyền nào của oregen
    drops:
      STONE: 100.0 # 100% sẽ rơi ra đá
      LAPIS_LAZULI: 100.0 
      REDSTONE: 100.0
      GOLD_INGOT: 30.0 # 30% rơi ra thỏi vàng
      IRON_INGOT: 36.3 # 36,3% tỷ lệ rơi ra thỏi sắt
      DIAMOND: 100.0
  oregen_level1:
    permission: "oregen.level.1"
    drops:
      STONE: 50.0
      COAL_ORE: 20.0
      IRON_ORE: 5.0
  oregen_level2:
    permission: "oregen.level.2"
    drops:
      STONE: 30.0
      DIAMOND_ORE: 3.0
      EMERALD_ORE: 1.0

# Giới hạn số block tối đa 
max_blocks: 1000

# Cấu hình chống lag
anti_lag:
  max_nearby_items: 100 # Số lượng vật phẩm tối đa trước khi tạm dừng nổ ảo
  check_radius: 10 # Phạm vi kiểm tra
```
> ⚠️ Enchant nổ ảo có áp dụng gia tài và give kinh nghiệm nên bạn hãy tính toán thật kỹ trước khi dùng.

---

### 2) Trống
> Hiện tại chúng tôi chỉ có 1 enchant nên khu vực này tạm để trống. Sẽ được bổ sung sau.
---

## ✅ Tính năng chính

* Tạo/xóa enchant trên item.
* Hỗ trợ nhiều level enchant do người chơi định nghĩa bằng chuỗi (e.g. `no1`, `noao2`).
* Hiển thị bossbar cảnh báo/hiệu ứng khi người chơi giữ item có enchant.
* API hook cho các plugin khác.
* Tương thích với hệ sinh thái Paper/Spigot

---

## 🔧 Yêu cầu

* Java 17+ (khuyến nghị Java 17 hoặc 21 tuỳ mục tiêu build server)
* Paper hoặc Spigot (kiểm tra tương thích với API server target; phát triển hướng tới Paper 1.20+)
* Gradle (để build từ source)

---

## ⚙️ Cài đặt

### Cài đặt nhanh (từ file JAR)

1. Copy `MoreEnchant-version.jar` vào thư mục `plugins/` của server.
2. Khởi động server để plugin tạo cấu hình mặc định.
3. Điều chỉnh `config.yml` nếu cần và reload/restart server.

### Build từ source

**Linux / macOS**

```bash
./gradlew clean shadowJar
```

**Windows (PowerShell / CMD)**

```powershell
gradlew.bat clean shadowJar
# hoặc nếu dùng Git Bash: ./gradlew clean shadowJar
```

File JAR sẽ được sinh ra ở `build/libs/` (hoặc đường dẫn tương tự tuỳ cấu hình Gradle).

---

## 💬 Commands (ví dụ)

Plugin cung cấp các command quản lý (ví dụ dựa trên `Helper.showHelp` trong mã nguồn):

* `/moe help` — Hiển thị trang help.
* `/moe enchant <enchantName> [level]` — Thêm enchant và cấp độ
* `/moe remove  [enchantName]` — Gỡ enchantName hoặc gỡ toàn bộ enchant hiện có.
* `/moe reload` — Nạp lại file cấu hình

> Kiểm tra file này thường xuyên để biết những thay đổi về lệnh.

---

## 🔌 Hooks & Integrations

MoreEnchant đã có chỗ mở để tích hợp với các hệ thống khác:

* **ExtraStorage**: Rơi item có thể được gửi vào storages thay vì rơi xuống ground.
* Sắp tới sẽ hook thêm với các plugin `Economy` như `Vault`, `TokenManager`.

Nếu bạn phát triển hook mới, hãy mở issue trước.

---

## 🚀 Roadmap

1. Hoàn thiện hệ thống config để admin dễ cấu hình enchant/levels.
2. Thêm GUI để tạo/edit enchant trực tiếp trong-game.
3. Hỗ trợ NBT-based persistence cho enchant phức tạp.
4. Thêm unit/integration tests (mock Bukkit via MockBukkit hoặc tương tự).
5. Release bản 1.2 khi API ổn định và backward compatibility đảm bảo.
6. Thêm nhiều loại enchant mới.
7. Đảm bảo về mặt hiệu suất và tính linh hoạt.
8. Hỗ trợ hook với nhiều plugin khác nhau.

---

## 🤝 Contributing

Mình rất hoan nghênh Pull Request và Issue! Vui lòng:

1. Fork repository.
2. Tạo branch feature/your-feature.
3. Viết unit tests (nếu có thể) và giữ coding style thống nhất.
4. Mở Pull Request mô tả rõ thay đổi.
5. Nếu bạn contribute bằng code từ ChatGPT hoặc AI generated code, vui lòng ghi chú rõ phần nào là AI gen.

**Vui lòng mở issue để thảo luận trước khi bạn định làm 1 thay đổi lớn**

---

## 📝 Changelog

Ghi chú thay đổi được duy trì trong `CHANGELOG.md`.

---

## 📄 License

Đọc file `LICENSE` để biết thêm thông tin chi tiết.

---

## 📬 Liên hệ

* Tác giả: **Herzchen**
* Repo: `https://github.com/Herzchens/MoreEnchant`
* Discord: [itztli\_herzchen](https://discord.com/users/984085171408080897)
