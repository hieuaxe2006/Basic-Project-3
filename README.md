# 📱 Social Network App (Android - Kotlin)

## 🚀 Giới thiệu

Ứng dụng mạng xã hội hiện đại được xây dựng bằng Jetpack Compose và Firebase, cho phép người dùng kết nối và chia sẻ nội dung thời gian thực.

## 🛠 Công nghệ sử dụng

* **UI**: Kotlin + Jetpack Compose
* **Backend**: Firebase (Auth, Firestore, Storage, Messaging)
* **Image Loading**: Coil
* **Navigation**: Compose Navigation
* **Thanh toán**: VNPAY Sandbox
* **AI**: OpenAI API integration

## 📱 Tính năng chính

### User:

* **Đăng ký / Đăng nhập**: Bảo mật qua Firebase Auth.
* **Đăng bài**: Hỗ trợ văn bản và hình ảnh.
* **Tương tác**: Like, comment, lưu bài viết.
* **Theo dõi**: Hệ thống Follow/Unfollow giữa người dùng.
* **Tin (Story)**: 
    * Tạo tin dạng ảnh hoặc văn bản màu sắc.
    * Tự động biến mất sau 24 giờ.
    * Xem tin với thanh tiến trình (progress bar) tự động chuyển.
* **Thông báo**: Nhận thông báo thời gian thực khi có người Like, Comment, Follow hoặc kết bạn.
* **Chat realtime**: Nhắn tin trực tiếp giữa các người dùng.
* **Profile**: Quản lý thông tin cá nhân và xem bài viết đã đăng.
* **Premium**: Nâng cấp tài khoản để sử dụng tính năng không giới hạn.

### Admin:

* Quản lý danh sách người dùng.
* Duyệt và quản lý nội dung bài viết.
* Thống kê hoạt động hệ thống.

## ⚙️ Cài đặt

1. Clone project: `git clone [url]`
2. Mở project bằng Android Studio (Ladybug hoặc mới hơn).
3. Thêm file `google-services.json` vào thư mục `app/`.
4. Cấu hình API Key cho ImgBB và OpenAI trong file cấu hình (nếu có).
5. Build và Run app.

## 📦 Build config

* **minSdk**: 24
* **targetSdk**: 35
* **Kotlin**: 2.1.0
* **Compose Compiler**: 1.5.3
