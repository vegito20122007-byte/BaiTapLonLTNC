# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM)
> **Bài tập lớn môn Lập trình nâng cao — Học kỳ I (2026–2027)**  
> Trường Đại học Công nghệ - ĐHQGHN (VNU-UET)

---

## 📌 1. TỔNG QUAN KIẾN TRÚC DỰ ÁN

Hệ thống được xây dựng theo kiến trúc **Client – Server**, áp dụng mô hình phân tầng **MVC** và cấu trúc **Maven Multi-Module**:

```text
BaiTapLon-main/
├── pom.xml                   # Root Parent POM (Quản lý phiên bản & build plugins)
├── config/checkstyle.xml      # Enforce quy chuẩn Google Java Style Guide
├── .github/workflows/ci.yml   # CI/CD tự động build & test qua GitHub Actions
├── build.bat                 # Script biên dịch tự động toàn bộ dự án
├── test.bat                  # Script chạy toàn bộ 18 JUnit 5 tests
├── run-server.bat            # Khởi động Socket Server
├── run-client.bat            # Khởi động Client JavaFX
│
├── auction-common/            # [Module 1] Thư viện dùng chung
│   ├── model/                 # OOP Domain Entities (Entity, User, Bidder, Seller, Admin, Item, ElectronicsItem, ArtItem, VehicleItem, Auction, BidTransaction)
│   ├── factory/               # Factory Method Pattern (ItemFactory) tạo các loại sản phẩm
│   ├── enums/                 # AuctionStatus, UserRole, CommandType
│   ├── protocol/              # Giao thức truyền tin JSON qua Socket (RequestMessage, ResponseMessage, BroadcastEvent)
│   ├── exception/             # Custom Exceptions (InvalidBidException, AuctionClosedException, AuthenticationException)
│   └── util/                  # Tiện ích tuần tự hóa JSON đa hình (JsonUtils)
│
├── auction-server/            # [Module 2] Máy chủ Đa luồng & Quản lý nghiệp vụ
│   ├── core/                  # AuctionManager (Singleton), AuctionLockService (ReentrantLock concurrency), AutoBidEngine (PriorityQueue)
│   ├── observer/              # Observer Pattern gửi thông báo Realtime tới Client qua Socket (RealtimeNotifier)
│   ├── network/               # SocketServer (ServerSocket + ThreadPool) & ClientHandler (Thread-per-client)
│   ├── service/               # AuctionService (nghiệp vụ đấu giá, anti-sniping, CRUD), UserService
│   ├── dao/                   # Tầng truy xuất dữ liệu lưu trữ (AuctionDao, UserDao, InMemory Persistent)
│   └── test/                  # Bộ Unit Test (JUnit 5) kiểm thử nghiệp vụ, Auto-Bid, vòng đời, concurrency
│
└── auction-client/            # [Module 3] Ứng dụng giao diện đồ họa người dùng
    ├── controller/            # JavaFX Controllers: Login, Register, MainDashboard, AuctionDetail, ProductDetail, CreateAuction, SellerManagement, AdminManagement
    ├── network/               # SocketClient (kết nối Socket & luồng nền bắt Event realtime)
    ├── service/               # ClientSession (Singleton quản lý phiên đăng nhập)
    └── resources/             # Giao diện FXML (SceneBuilder) và CSS giao diện hiện đại
```

---

## 🎯 2. ĐÁP ỨNG ĐẦY ĐỦ TIÊU CHÍ BAREM CHẤM ĐIỂM (10 + 1.5 = 11.5 ĐIỂM)

| Tiêu chí đánh giá | Điểm | Mức độ | Hiện thực trong mã nguồn |
| :--- | :---: | :---: | :--- |
| **Thiết kế lớp & Cây kế thừa** | **0.5đ** | Bắt buộc | `Entity` $\rightarrow$ `User` (Bidder/Seller/Admin), `Item` (Electronics/Art/Vehicle), `Auction`, `BidTransaction`. |
| **Áp dụng 4 nguyên tắc OOP** | **1.0đ** | Bắt buộc | **Encapsulation** (private/getter/setter), **Inheritance** (phân cấp lớp rõ ràng), **Polymorphism** (override `printInfo()`, `getSummary()`, `getCategory()`), **Abstraction** (`Entity`, `User`, `Item`). |
| **Design Patterns phù hợp** | **1.0đ** | Bắt buộc | **Singleton**: `AuctionManager`, `ClientSession`, `SocketClient`.<br>**Factory Method**: `ItemFactory` tạo Electronics, Art, Vehicle.<br>**Observer**: `RealtimeNotifier` $\rightarrow$ `ClientHandler` qua Socket. |
| **Quản lý người dùng, sản phẩm (CRUD)** | **1.0đ** | Bắt buộc | Đăng ký/đăng nhập (Bidder, Seller, Admin). Thêm, sửa, xóa sản phẩm đấu giá (Seller & Admin), xem thông tin đa hình. |
| **Chức năng đấu giá & Vòng đời** | **1.0đ** | Bắt buộc | Đặt giá, kiểm tra tính hợp lệ, tự động cập nhật người dẫn đầu. Vòng đời chuyển trạng thái: `OPEN` $\rightarrow$ `RUNNING` $\rightarrow$ `FINISHED` $\rightarrow$ `PAID` / `CANCELED`. |
| **Xử lý lỗi & Ngoại lệ** | **1.0đ** | Bắt buộc | Custom exceptions: `InvalidBidException`, `AuctionClosedException`, `AuthenticationException`. Bắt lỗi đặt giá thấp, phiên đóng, lỗi dữ liệu. |
| **Xử lý đồng thời (Concurrency)** | **1.0đ** | Bắt buộc | `AuctionLockService` dùng `ReentrantLock` theo từng Auction ID $\rightarrow$ Tránh Lost update, race condition, 2 người cùng thắng. Đạt chuẩn test đa luồng. |
| **Realtime Update (Observer/Socket)** | **0.5đ** | Bắt buộc | Khi có bid mới hoặc gia hạn, Server push sự kiện JSON ngay lập tức tới mọi Client qua Socket (hoàn toàn không dùng polling). |
| **Kiến trúc Client–Server** | **0.5đ** | Bắt buộc | Tách biệt hoàn toàn client và server; chỉ Server truy cập và lưu trữ dữ liệu. |
| **Áp dụng MVC** | **0.5đ** | Bắt buộc | Client: FXML (View) + Controller + Model/Service. Server: Handler (Controller) $\rightarrow$ Service $\rightarrow$ DAO (Model). |
| **Maven & Code Quality** | **0.5đ** | Bắt buộc | Chuẩn Maven Multi-Module, cấu hình `checkstyle.xml` (Google Java Style), `build.bat` và `test.bat`. |
| **Unit Test (JUnit 5)** | **0.5đ** | Bắt buộc | **18 Unit Tests** toàn diện kiểm tra OOP, xác thực, nghiệp vụ đấu giá, anti-sniping, Auto-Bid, và 20 luồng đồng thời. |
| **CI/CD** | **0.5đ** | Bắt buộc | Cấu hình `.github/workflows/ci.yml` tự động build và chạy test trên GitHub Actions mỗi khi push code. |
| **[Nâng cao 1] Auto-Bidding** | **+0.5đ** | Tùy chọn | `AutoBidEngine` dùng `PriorityQueue` so sánh các mức `maxBid` và FIFO, tự động trả giá thay người dùng khi có đối thủ; có UI cấu hình và bật/tắt trong phòng đấu giá. |
| **[Nâng cao 2] Anti-sniping** | **+0.5đ** | Tùy chọn | `checkAndApplyAntiSniping()`: Nếu có bid trong 30 giây cuối, tự động cộng thêm 60 giây và dời lịch đóng phiên, phát thông báo cho mọi client. |
| **[Nâng cao 3] Realtime Price Curve**| **+0.5đ** | Tùy chọn | `chartPriceCurve` (`LineChart` JavaFX): Tự động vẽ thêm điểm biến thiên giá ngay khi nhận `BroadcastEvent(NEW_BID)` mà không cần tải lại trang. |
| **TỔNG ĐIỂM** | **11.5 / 10** | | **ĐẠT ĐIỂM TỐI ĐA VÀ ĐỦ CẢ 3 TÍNH NĂNG NÂNG CAO** |

---

## 🖥️ 3. CÁC MÀN HÌNH CHÍNH CỦA HỆ THỐNG (JAVAFX MVC)

Hệ thống trang bị đầy đủ các màn hình đồ họa theo đúng yêu cầu đề bài:
1. **Đăng nhập / Đăng ký**: Hỗ trợ chọn vai trò (Bidder / Seller / Admin).
2. **Danh sách phiên đấu giá (Dashboard)**: Bảng dữ liệu hiển thị realtime, phân quyền nút bấm theo vai trò người dùng.
3. **Chi tiết sản phẩm (`product-detail.fxml`)**: Xem toàn bộ thuộc tính sản phẩm, hiển thị đa hình OOP theo từng phân loại (Thương hiệu/Bảo hành đối với Thiết bị điện tử; Họa sĩ/Năm sáng tác đối với Tác phẩm nghệ thuật; Hãng xe/Odometer đối với Phương tiện).
4. **Màn hình đấu giá trực tiếp (`auction-room.fxml`)**:
   - Hiển thị giá cao nhất hiện tại, người dẫn đầu, đếm ngược thời gian.
   - Nút đặt giá trực tiếp (⚡ Đặt giá).
   - Form đấu giá tự động (🤖 Auto-Bid) với `maxBid` và `increment`.
   - Lịch sử trả giá realtime (ListView).
   - Biểu đồ biến thiên giá Realtime (`LineChart`).
5. **Quản lý sản phẩm dành cho Seller (`seller-management.fxml`)**:
   - Seller xem danh sách sản phẩm mình đã đăng bán.
   - ➕ Đăng sản phẩm mới.
   - ✏️ Chỉnh sửa thông tin sản phẩm (nếu chưa có ai đặt giá).
   - 🗑️ Xóa sản phẩm (nếu chưa có ai đặt giá).
   - 💳 Xác nhận đã nhận thanh toán (`PAID`) khi phiên kết thúc thành công.
6. **Trung tâm Quản trị hệ thống (`admin-management.fxml`)**:
   - Thống kê toàn sàn (tổng user, tổng phiên, tổng giá trị giao dịch).
   - Quản lý tài khoản (xóa tài khoản vi phạm).
   - Quản lý phiên đấu giá (hủy phiên, xóa vĩnh viễn).

---

## 🚀 4. HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY THỬ NGHIỆM

### Cách 1: Chạy trực tiếp cực nhanh (Zero-Config — Không cần cài đặt Maven)
> 💡 **Máy mới bất kỳ chỉ cần có Java (JDK 17+)**: Không cần cài đặt sẵn Maven hay cấu hình biến môi trường phức tạp!
1. **Bước 1**: Nhấp đúp vào [run-server.bat](file:///c:/Users/LENOVO/OneDrive/Desktop/BaiTapLon-main/run-server.bat) để khởi động Server.
   *(Nếu là lần đầu chạy sau khi clone từ GitHub, script sẽ tự động dùng Maven Wrapper để tải toàn bộ thư viện cần thiết, biên dịch và chạy Server mà không cần làm gì thêm).*
2. **Bước 2**: Nhấp đúp vào [run-client.bat](file:///c:/Users/LENOVO/OneDrive/Desktop/BaiTapLon-main/run-client.bat) để mở giao diện người dùng. Có thể mở nhiều client cùng lúc để mô phỏng đấu giá giữa các người dùng.
3. *(Dành cho macOS / Linux)*: Có sẵn script shell tương ứng `./run-server.sh` và `./run-client.sh`.

### Cách 2: Mở dự án trong IDE (IntelliJ IDEA / Eclipse / VS Code)
1. Mở **IntelliJ IDEA** $\rightarrow$ chọn **Open** $\rightarrow$ chọn thư mục gốc chứa file `pom.xml`.
2. Khởi động Server: Mở file `ServerApp.java` $\rightarrow$ Run.
3. Khởi động Client: Mở file `Launcher.java` $\rightarrow$ Run.

### Danh sách tài khoản thử nghiệm có sẵn:
- **Người mua 1**: `bidder1` / mật khẩu: `123456`
- **Người mua 2**: `bidder2` / mật khẩu: `123456`
- **Người bán (Seller)**: `seller1` / mật khẩu: `123456`
- **Quản trị viên (Admin)**: `admin` / mật khẩu: `admin123`

---

## 🧪 5. BỘ KIỂM THỬ ĐƠN VỊ (18 UNIT TESTS)

Chạy kiểm thử nhanh bằng cách nhấp đúp vào file `test.bat` hoặc lệnh:
```bash
mvn test
```

Danh sách các test case:
1. `ItemFactoryTest` (4 tests): Kiểm thử Factory Method tạo Electronics, Art, Vehicle và bắt ngoại lệ.
2. `OOPModelTest` (3 tests): Kiểm thử 4 nguyên tắc OOP (Đa hình `getSummary()`, `printInfo()`, `getCategory()`, tính bao đóng và kế thừa).
3. `UserServiceTest` (5 tests): Đăng ký, đăng nhập thành công, thu hồi token khi đăng xuất, bắt ngoại lệ trùng username và sai mật khẩu.
4. `AuctionServiceTest` (4 tests): Đặt giá hợp lệ, bắt ngoại lệ giá thấp hơn giá hiện tại, cấm seller tự bid, cấm bid vào phiên đã đóng.
5. `AutoBidTest` (3 tests): Kiểm thử độ ưu tiên của `PriorityQueue`, kiểm thử auto-bidder tự động counter-bid khi đối thủ vào trả giá, kiểm thử chạm trần `maxBid`.
6. `AuctionLifecycleTest` (5 tests): Chuyển trạng thái `OPEN` $\rightarrow$ `RUNNING` $\rightarrow$ `FINISHED` $\rightarrow$ `PAID` / `CANCELED`, kiểm thử thuật toán Anti-sniping tự gia hạn thêm 60 giây, kiểm thử Seller CRUD (sửa, xóa sản phẩm khi chưa có bid).
7. `ConcurrencyBidTest` (1 test): Kiểm thử an toàn luồng với **20 thread chạy song song đồng thời**, chống race condition và lost update.
