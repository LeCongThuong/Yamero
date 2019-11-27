# Server
- UIManager: hiển thị ở console
- ClientConnection: chứa
	- socket: Socket đến client đó
	- id: 1 2 3
	- port: port đến client đó
	- ip: ip đến client
	- dataInputStream
	- dataOutputStream
	- send(byte[]) gửi thông tin cho client
- Controller: chứa
	- main tạo 3 ClientConnection c1, c2, c3 (mặc định c1 forwarder)
	- đợi kết nối
	- gửi lại thông tin các client cho cả c1, c2, c3
	- đợi thông báo sẵn sàng từ 3 client
	- cho phép nhập tên file
	- gửi file name + file size
	- gửi file cho c1
	- đợi các client gửi lại thời gian hoàn thành nhận file => in thời gian
	- cho phép gửi file tiếp

# Client
- UIManager
- Controller:
	- Server ip + port hard code 
	- kết nối đến Server
	- đợi xác nhận iden của mình ( mình là c1, c2 hay c3)
	- c1 kết nối c2, c3; c2, c3 đợi kết nối; c1, c2, c3 gửi lại thông báo sẵn sàng cho server
	- Lặp lại : (forward dữ liệu cho c2, c3 nếu là c1), đợi thông báo file size + file name từ server (từ c1 nếu là c2, c3), đợi file; nếu đủ số byte => continue, bắt đầu lần lặp mới