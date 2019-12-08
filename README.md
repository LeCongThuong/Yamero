# Bài tập lớn môn lập trình mạng nhóm Yamero
Danh sách thành viên:
- Trương Hoàng Giang
- Lưu Quang Tùng
- Vũ Quốc Phong
- Lê Công Thương

Project được phát triển bằng ngôn ngữ Java với IDE Intellij
## Installation
Pull code ở master về các máy S1, C1, C2, C3
```sh
git clone https://github.com/LeCongThuong/Yamero.git
```
Chạy code đã được build trong thư mục `Yamero/out/production/Yamero`

Chạy server S1
```sh
cd Yamero/out/production/Yamero
java server.Controller
```
Chạy các client C1, C2, C3
```sh
cd Yamero/out/production/Yamero
java client.Controller
```
Lưu ý kiểm tra config trước khi chạy. Chương trình sẽ sử dụng giá trị default khi không tìm thấy file config theo đường dẫn hoặc trong file config không có giá trị tương ứng. Các config trong chương trình:
- Client - ```out/production/Yamero/client/config.properties```:
    - ```server-ip```: địa chỉ ip của server (default: ```127.0.0.1```)
- Helper - ```out/production/Yamero/helpers/config.properties```:
    - ```file-buffer-size```: buffer đọc ghi và gửi nhận file (default: ```1024```)

## Usage

Sau khi chạy server và chạy lần lượt các client, chương trình sẽ cho phép nhập tên file để tải về các client. Các file server muốn gửi cho client cần nằm trong thư mục `Yamero/out/production/Yamero` (Ví dụ như file 1MB.pdf). Các file client nhận được sẽ được lưu trong thư mục `Yamero/out/production/Yamero/received`.

Có thể ấn Ctrl + C ở server để ngắt chương trình.

## Test result

1MB.pdf : 174149 ms 
3MB.mp4 : 517284 ms

## Cấu trúc chương trình

* [client/](.\src\client)
  * [ClientAddress.java](.\src\client\ClientAddress.java)
  * [Controller.java](.\src\client\Controller.java)
* [helpers/](.\src\helpers)
  * [ConfigLoader.java](.\src\helpers\ConfigLoader.java)
  * [FileHelper.java](.\src\helpers\FileHelper.java)
  * [MessageControlHelper.java](.\src\helpers\MessageControlHelper.java)
  * [QueueThread.java](.\src\helpers\QueueThread.java)
* [server/](.\src\server)
  * [ClientConnection.java](.\src\server\ClientConnection.java)
  * [Controller.java](.\src\server\Controller.java)
  * [UIManager.java](.\src\server\UIManager.java)

Có 3 package server, client, helpers trong đó
- client:
	- Controller kiểm soát quá trình chạy ở client
	- ClientAddress giúp trích xuất các thông tin như ip và port của client socket
- helpers:
	- ConfigLoader giúp khởi tạo các config khi chạy chương trình
	- FileHelper xử lý các tác vụ với file như gửi, nhận file
	- MessageControlHelper giúp xử lý các lệnh điều khiển
	- QueueThread quản lý các thread của client
- server:
	- Controller kiểm soát quá trình chạy ở server
	- UIManager hỗ trợ hiển thị các thông báo ở server
	- ClientConnection kiểm soát thông tin của client, gửi nhận dữ liệu từ client