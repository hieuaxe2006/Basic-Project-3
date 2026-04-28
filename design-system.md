📘 Design System & UI Layout Proposal (Final Version)
1. 🎨 Design System (Chuẩn cho ứng dụng chat Java – phong cách hiện đại)
1.1 Màu sắc (Color Palette)
Primary:        #1877F2  (Xanh dương hiện đại)
Primary Light:  #E7F3FF  
Primary Dark:   #0F5BD3

Secondary:      #42B72A  (Xanh lá – trạng thái online)
Danger:         #E41E3F
Warning:        #F7B928
Success:        #31A24C

Background:     #F0F2F5
Card:           #FFFFFF
Border:         #DADDE1
Text Primary:   #050505
Text Secondary: #606770
1.2 Typography
Font: Segoe UI / Roboto / San Francisco  
Message text: 14px  
Sidebar labels: 13px  
Title/App name: 18–20px, bold  
1.3 Component System
Button
Size: medium
Border radius: 8px
Hover: tăng sáng 10%
Types: primary / secondary / subtle
Input
Bo góc 10px
Border nhẹ (#DADDE1)
Background trắng
Focus: outline xanh #1877F2
Message bubble
Sent bubble:
Align: right
Background: #1877F2 (white text)
Radius: 16px
Received bubble:
Align: left
Background: #E4E6EB
Icon set
Dùng style đơn giản như Facebook:
Image upload
Record voice
Emoji
File
Hidden message lock
Status online
2. 🧱 System Layout (Ý tưởng từ Facebook Messenger)
2.1 Ứng dụng gồm 3 phần chính
+----------------------------------------------+
| SIDEBAR LEFT (Danh sách cuộc chat)           |
+----------------------------------------------+
| CONTENT (Khung chat + message + input)       |
+----------------------------------------------+
| SIDEBAR RIGHT (Thông tin user, extra info)   |
+----------------------------------------------+
2.2 Sidebar trái – danh sách cuộc chat

Gợi ý các chức năng quan trọng:

Search người dùng
Danh sách chat, hiển thị:
Avatar
Tên
Last message
Time
Status online (chấm xanh)
Nút New Chat
Phân nhóm (All / Online / Unread)
2.3 Khung chat chính

Nên có:

Header:
Avatar
Tên người nhận
Status
Nút Call / Info
Body (message list):
Bubble trái/phải
Time separator
Image preview
File preview
Voice message play inline
Hidden message (mã mở khóa)
Double-tick: sent, delivered, seen
Input area:
Emoji
Upload image
Upload file
Voice record
Send icon
2.4 Sidebar phải (tùy chọn)
Thông tin người đang chat
File media đã gửi
File đã gửi
Cài đặt từng cuộc hội thoại:
Đổi màu cuộc chat
Tắt thông báo
Xóa lịch sử
3. 🎤 Voice Recording (Thay thế gửi mp3 cũ)
Luồng xử lý:
Nhấn giữ nút 🟢 Record
UI hiển thị waveform + timer
Thả nút → gửi audio
Tin nhắn audio hiển thị có:
Nút play
Thanh thời gian
Tải về

Format đề xuất: .wav hoặc .m4a

4. 🔒 Hidden Message (Cần mã để xem)
Flow:
Người gửi nhập mã → gửi
Người nhận nhấn vào tin → yêu cầu nhập mã
Đúng mã → hiển thị nội dung
Sai mã → báo lỗi

UI đề xuất:

Bubble màu xám đậm
Icon khóa 🔐
5. 🖼 Media & Files
Image:
Thumbnail trong bubble
Click → mở full-view
Zoom
File:
Icon file
Tên file
Kích thước
Download
Audio:
Player mini
Seek bar
6. 📡 Kiến trúc tổng quan (Giữ nguyên project cũ)
Các module giữ lại:
System chat server
Client gửi/nhận
Message handler
Gửi file
Gửi ảnh
Hidden message logic
Voice recording module mới

Chỉ thay đổi: UI phía client.

7. 📑 Checklist chức năng còn thiếu (nên thêm)
Search tin nhắn trong cuộc chat
Thông báo tin nhắn mới
Message reactions (👍 ❤️ 😆 😢 😡)
Typing indicator (“A is typing…”)
Seen status theo người
Custom theme (light/dark mode)
Sticker pack
Gửi vị trí
Reply to message (quote message)
Forward message
8. 📁 File Structure đề xuất
/ui
  /components
    - Button.java
    - InputText.java
    - MessageBubble.java
    - SidebarChatItem.java
    - UserInfoPanel.java
  /layouts
    - MainWindow.java
    - LeftSidebar.java
    - RightSidebar.java
    - ChatPane.java
  /theme
    - Colors.java
    - Typography.java
    - Icons.java
    
/logic
  - MessageHandler.java
  - VoiceRecorder.java
  - HiddenMessage.java

/assets
  /icons
  /images