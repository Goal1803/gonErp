# gonERP - Enterprise Resource Planning

A scalable ERP system built with Java Spring Boot + Vue Quasar.

## Tech Stack

| Layer    | Technology                           |
|----------|--------------------------------------|
| Backend  | Java 17, Spring Boot 3.2, PostgreSQL |
| Auth     | JWT (Bearer Token)                   |
| Frontend | Vue 3, Quasar Framework, Pinia       |
| Theme    | Dark Green + Yellow-Orange (Premium) |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ / npm
- PostgreSQL (user: `steve`, password: `dev12489$`)

---

## Database Setup

`steve` is already a PostgreSQL superuser. Just create the database:

```bash
./setup-db.sh
```

Or manually:
```bash
psql -U steve -d postgres -c "CREATE DATABASE gonerp;"
```

---

## Backend

```bash
cd backend
mvn spring-boot:run
```

Server runs on: `http://localhost:8080`

### Default Admin User
- **Username:** `admin`
- **Password:** `admin123`

### API Endpoints

| Module       | Method | Endpoint               | Auth Required | Role  |
|--------------|--------|------------------------|---------------|-------|
| Auth         | POST   | /api/auth/login        | No            | -     |
| Auth         | GET    | /api/auth/me           | Yes           | Any   |
| UserRole     | GET    | /api/user-roles        | Yes           | Any   |
| UserRole     | POST   | /api/user-roles        | Yes           | Admin |
| UserRole     | PUT    | /api/user-roles/{id}   | Yes           | Admin |
| UserRole     | DELETE | /api/user-roles/{id}   | Yes           | Admin |
| User         | GET    | /api/users             | Yes           | Admin |
| User         | GET    | /api/users/{id}        | Yes           | Admin |
| User         | POST   | /api/users             | Yes           | Admin |
| User         | PUT    | /api/users/{id}        | Yes           | Admin |
| User         | DELETE | /api/users/{id}        | Yes           | Admin |
| Image        | GET    | /api/images            | Yes           | Any   |
| Image        | GET    | /api/images/{id}       | Yes           | Any   |
| Image        | POST   | /api/images            | Yes           | Any   |
| Image        | PUT    | /api/images/{id}       | Yes           | Any   |
| Image        | DELETE | /api/images/{id}       | Yes           | Any   |

### Run Tests

```bash
cd backend
mvn test
```

---

## Frontend

```bash
cd frontend
npm install
npm run dev   # Quasar dev server on port 9000
```

Quasar dev server proxies `/api` requests to `http://localhost:8080`.

---

## Project Structure

```
gonErp/
├── backend/
│   └── src/main/java/com/gonerp/
│       ├── GonErpApplication.java
│       ├── common/              # BaseModel, ApiResponse
│       ├── config/              # Security, JWT, Audit, CORS
│       ├── auth/                # Login endpoint
│       ├── usermanager/         # UserRole + User CRUD
│       └── imagemanager/        # ImageInfo CRUD
└── frontend/
    └── src/
        ├── layouts/             # MainLayout (header + drawer)
        ├── pages/               # Dashboard, UserManager, ImageManager
        ├── components/          # Form dialogs, View dialog
        ├── stores/              # Pinia auth store
        ├── api/                 # Axios API modules
        └── css/                 # SCSS theme
```

---

## Features

### User Manager (Admin only)
- View all users with pagination
- Filter by status (Active / Pending / Deleted)
- Search by username, first name, or last name
- Add / Edit users
- Soft-delete (sets status to DELETED)

### Image Manager (All users)
- Grid view with 300x300 thumbnails
- Click thumbnail → full-size popup with metadata
- Add / Edit / Delete images
- Search by name
- URL preview when adding/editing

---

## Future Modules (Planned)
- Finance Management
- Sales & CRM
- Marketing Campaigns
- Inventory / Stock
- Task & Project Manager
