# Project Structure

```
Camunda/
├── docs/                          # Tutorial & tài liệu học tập
│   ├── 01-gioi-thieu/            # Giới thiệu Camunda 8
│   ├── 02-tao-tai-khoan/         # Tạo tài khoản & cluster
│   ├── 03-model-process/         # Thiết kế BPMN process
│   ├── 04-human-tasks/           # Human Tasks & Forms
│   ├── 05-orchestrate-apis/      # Orchestrate APIs
│   ├── 06-automating-bpmn/       # Tự động hóa BPMN
│   └── 07-thuc-hanh/             # Bài thực hành tổng hợp
│
├── phase1-hello-camunda/         # Phase 1: CRUD đơn giản
│   ├── backend/                  # Spring Boot + Zeebe client
│   └── frontend/                 # React hiển thị process instances
│
├── phase2-human-task/            # Phase 2: User Task cơ bản
│   ├── backend/
│   └── frontend/
│
├── phase3-gateway/               # Phase 3: Gateway & nhiều bước
│   ├── backend/
│   └── frontend/
│
├── phase4-multi-role/            # Phase 4: Multi-role approval
│   ├── backend/
│   └── frontend/
│
├── phase5-lpd-tinchap/           # Phase 5: Demo LPD Tín Chấp đầy đủ
│   ├── backend/
│   │   └── src/main/java/com/demo/lpd/
│   ├── frontend/
│   └── docs/                     # Tài liệu thiết kế
│
├── .kiro/steering/               # Kiro configuration
└── README.md                     # Tổng quan dự án
```

## Quy ước chung
- Mỗi phase là 1 project độc lập, có thể chạy riêng
- Backend: Spring Boot, package `com.demo.lpd`
- Frontend: React + TypeScript
- API prefix: `/api/v1/`
- Mỗi phase build on top of phase trước (kiến thức tích lũy)
