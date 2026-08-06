# Cài đặt môi trường — Chuẩn bị cho tất cả Phase

---

## Tổng quan: Cần gì cho từng Phase?

| Công nghệ | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Phase 5 |
|-----------|---------|---------|---------|---------|---------|
| Java 17 | ✅ | ✅ | ✅ | ✅ | ✅ |
| Maven | ✅ | ✅ | ✅ | ✅ | ✅ |
| Node.js 18+ | ✅ | ✅ | ✅ | ✅ | ✅ |
| IntelliJ IDEA | ✅ | ✅ | ✅ | ✅ | ✅ |
| Camunda Account | ✅ | ✅ | ✅ | ✅ | ✅ |
| Docker | — | — | — | ✅ | ✅ |
| PostgreSQL | — | — | — | ✅ | ✅ |

> Cài hết 1 lần, dùng cho tất cả phase. Không cần cài thêm gì sau này.

---

## 1. Java 17

**Tại sao?** Spring Boot 3.x yêu cầu Java 17 tối thiểu. Zeebe Client cũng build trên Java 17.

### macOS
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Windows
```bash
# Dùng Chocolatey
choco install temurin17

# Hoặc tải từ: https://adoptium.net/temurin/releases/
# Sau khi cài, mở System Properties → Environment Variables
# Thêm JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.x.x
```

### Verify
```bash
java -version
# → openjdk version "17.x.x"
```

---

## 2. Maven

**Tại sao?** Build tool cho Spring Boot. Quản lý dependencies, compile, package.

### macOS
```bash
brew install maven
```

### Windows
```bash
choco install maven
```

### Verify
```bash
mvn -version
# → Apache Maven 3.9.x
# → Java version: 17.x.x (phải hiện đúng Java 17)
```

---

## 3. Node.js 18+ và npm

**Tại sao?** Chạy React frontend (Vite build tool cần Node 18+).

### macOS
```bash
# Cách 1: Homebrew
brew install node@18

# Cách 2: nvm (khuyến khích — dễ switch version)
brew install nvm
nvm install 18
nvm use 18
```

### Windows
```bash
choco install nodejs-lts
```

### Verify
```bash
node -v   # → v18.x.x hoặc v20.x.x
npm -v    # → 9.x.x hoặc 10.x.x
```

---

## 4. IntelliJ IDEA

**Tại sao?** IDE tốt nhất cho Java + hỗ trợ React/TypeScript (bản Ultimate). Bản Community cũng đủ cho backend.

### Cài đặt
- Tải từ: https://www.jetbrains.com/idea/download/
- **Ultimate**: hỗ trợ cả Java + JavaScript/TypeScript
- **Community**: chỉ Java (frontend dùng terminal hoặc VS Code)

### Plugins khuyến nghị (cài trong IntelliJ → Settings → Plugins)
| Plugin | Dùng cho |
|--------|----------|
| Lombok | Tránh boilerplate code Java |
| Spring Boot | Auto-complete cho Spring config |
| BPMN (nếu có) | Xem file .bpmn trong IDE |

---

## 5. Camunda Account (SaaS)

**Tại sao?** Zeebe Engine chạy trên cloud — không cần cài local. Đây là nơi process instance chạy thật.

### Đăng ký
1. Truy cập: https://signup.camunda.com/accounts
2. Đăng ký bằng email / Google / GitHub
3. Xác nhận email → đăng nhập Console

### Tạo Cluster
1. Camunda tự tạo 1 cluster miễn phí khi đăng ký
2. Đợi status = **Healthy** (1-2 phút)

### Tạo API Client (quan trọng)
1. Console → Clusters → chọn cluster
2. Tab **API** → **Create new client**
3. Tên: `dev-client`
4. Scopes: tick hết
5. Click **Create** → **TẢI FILE CREDENTIALS NGAY** (chỉ hiện 1 lần)

Bạn sẽ nhận được:
```
ZEEBE_ADDRESS=xxxxx.bru-2.zeebe.camunda.io:443
ZEEBE_CLIENT_ID=xxxx
ZEEBE_CLIENT_SECRET=xxxx
ZEEBE_AUTHORIZATION_SERVER_URL=https://login.cloud.camunda.io/oauth/token
```

> Lưu vào file riêng (ví dụ `credentials.txt`), KHÔNG commit lên git.

---

## 6. Docker (từ Phase 4)

**Tại sao?** Chạy PostgreSQL local mà không cần cài trực tiếp lên máy.

### macOS
```bash
brew install --cask docker
# Mở Docker Desktop, đợi icon cá voi xanh lá = sẵn sàng
```

### Windows
```bash
choco install docker-desktop
# Mở Docker Desktop, enable WSL2 nếu được hỏi
```

### Verify
```bash
docker --version    # → Docker version 24.x.x
docker run hello-world  # → "Hello from Docker!"
```

---

## 7. PostgreSQL via Docker (từ Phase 4)

**Tại sao?** Lưu dữ liệu nghiệp vụ (hồ sơ vay, thông tin khách hàng). Camunda lo trạng thái process, PostgreSQL lo dữ liệu business.

### Chạy container

**macOS:**
```bash
docker run -d \
  --name lpd-postgres \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin123 \
  -e POSTGRES_DB=lpd_tinchap \
  -p 5432:5432 \
  postgres:15
```

**Windows (CMD):**
```bash
docker run -d ^
  --name lpd-postgres ^
  -e POSTGRES_USER=admin ^
  -e POSTGRES_PASSWORD=admin123 ^
  -e POSTGRES_DB=lpd_tinchap ^
  -p 5432:5432 ^
  postgres:15
```

### Kết nối từ IntelliJ
1. View → Tool Windows → Database
2. New → Data Source → PostgreSQL
3. Host: `localhost`, Port: `5432`, User: `admin`, Password: `admin123`, Database: `lpd_tinchap`

---

## Tóm tắt: Lệnh verify tất cả

Chạy lần lượt — tất cả pass là sẵn sàng:

```bash
java -version          # Java 17+
mvn -version           # Maven 3.8+
node -v                # Node 18+
npm -v                 # npm 9+
docker --version       # Docker 20+ (optional cho Phase 1-3)
```

---

## Cấu trúc thư mục sau khi setup

```
Camunda/
├── docs/
│   ├── 01-kien-truc-tong-quan.md  ← đã có
│   └── 02-cai-dat-moi-truong.md   ← file này
├── phase1-hello-camunda/          ← sẽ tạo ở bước tiếp theo
│   ├── backend/
│   └── frontend/
└── .kiro/steering/                ← steering rules
```

---

## Tiếp theo

Sau khi verify xong → chuyển sang tạo project Phase 1.
