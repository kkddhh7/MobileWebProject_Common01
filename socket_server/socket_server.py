import socket

HOST = '0.0.0.0'   # 모든 인터페이스에서 연결 허용
PORT = 8000        # 앱에서 요청할 포트 (10.0.2.2:8000 과 동일하게)

print(f"[INFO] Socket server listening on {HOST}:{PORT}")

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((HOST, PORT))
server.listen(1)

conn, addr = server.accept()
print(f"[INFO] Connected by {addr}")

data = conn.recv(4096)
print("\n=== Request Received ===\n")
print(data.decode(errors='ignore'))

# 요청을 파일로 저장 (timestamp 파일명)
import datetime
fname = f"request/{datetime.datetime.now().strftime('%Y-%m-%d-%H-%M-%S')}.bin"
with open(fname, "wb") as f:
    f.write(data)
print(f"\n[+] Saved request to {fname}\n")

response = b"HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK"
conn.sendall(response)

conn.close()
server.close()