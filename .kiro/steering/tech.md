# Tech Stack

## Backend
- **Spring Boot 3.x** (Java 17+)
  - Spring Web (REST API)
  - Spring Security (xác thực/phân quyền)
  - Spring Data JPA (truy xuất dữ liệu)
  - Camunda Zeebe Client (kết nối Camunda 8)

## Frontend
- **React 18+** (TypeScript)
  - React Router (điều hướng)
  - Axios (gọi API)
  - Ant Design hoặc Material UI (UI components)
  - React Context / Zustand (state management)

## Process Engine
- **Camunda 8 SaaS**
  - Zeebe (process execution engine)
  - Operate (giám sát process)
  - Tasklist (quản lý user tasks)
  - Web Modeler (thiết kế BPMN)

## Database
- **PostgreSQL** (lưu trữ dữ liệu nghiệp vụ)

## Build & DevOps
- Maven (build backend)
- Vite (build frontend)
- Docker (containerization cho local dev)

## Giao tiếp giữa các thành phần
```
React (Frontend) <--REST API--> Spring Boot (Backend) <--gRPC--> Camunda Zeebe (Engine)
                                      |
                                      v
                                 PostgreSQL (DB)
```
