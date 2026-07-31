# Task Manager Module - Portable Specification Document

> This document provides a complete specification for reimplementing the Task Manager module in a new project with the same tech stack: **Spring Boot 3 + Vue 3/Quasar 2 + PostgreSQL**.

---

## Table of Contents

1. [Tech Stack](#1-tech-stack)
2. [Architecture Overview](#2-architecture-overview)
3. [External Dependencies (Interfaces to Adapt)](#3-external-dependencies)
4. [Database Schema](#4-database-schema)
5. [Enums](#5-enums)
6. [Backend Models (JPA Entities)](#6-backend-models)
7. [DTOs (Data Transfer Objects)](#7-dtos)
8. [Repository Layer](#8-repository-layer)
9. [Service Layer (Business Logic)](#9-service-layer)
10. [REST API Endpoints](#10-rest-api-endpoints)
11. [WebSocket (Real-time Updates)](#11-websocket)
12. [Notifications System](#12-notifications-system)
13. [Frontend Architecture](#13-frontend-architecture)
14. [Frontend API Layer](#14-frontend-api-layer)
15. [Frontend State Management (Pinia)](#15-frontend-state-management)
16. [Frontend WebSocket Composable](#16-frontend-websocket-composable)
17. [Frontend Pages](#17-frontend-pages)
18. [Frontend Components](#18-frontend-components)
19. [Frontend Routes](#19-frontend-routes)
20. [Implementation Order](#20-implementation-order)

---

## 1. Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.3** (Web, JPA, Security, WebSocket)
- **Spring Data JPA** with Hibernate
- **PostgreSQL**
- **Lombok** (getter/setter/builder generation)
- **JWT** (JJWT 0.12.5) for authentication
- **WebSocket/STOMP** for real-time events
- **AWS S3 SDK** (Cloudflare R2 compatible) for file storage

### Frontend
- **Vue 3** (Composition API with `<script setup>`)
- **Quasar 2.16.9** (UI framework)
- **Pinia 2.1.7** (state management)
- **Axios 1.7.7** (HTTP client)
- **Vue Router 4** (hash mode)
- **vuedraggable 4.1.0** (drag & drop)
- **STOMP.js 7.3.0 + SockJS 1.6.1** (WebSocket client)

---

## 2. Architecture Overview

```
Backend (Layered Architecture)
├── controller/     → REST endpoints, request validation
├── service/        → Business logic, access control, transactions
├── repository/     → Spring Data JPA interfaces
├── model/          → JPA entities
│   └── enums/      → Enum types
├── dto/            → Request/Response data transfer objects
├── event/          → Application events (notifications)
└── websocket/      → WebSocket event publisher

Frontend (Component-Based)
├── api/            → Axios API client modules
├── stores/         → Pinia state stores
├── composables/    → Reusable composition functions (WebSocket)
├── pages/          → Top-level route views
├── components/     → Reusable UI components
└── router/         → Route definitions
```

### Key Patterns
- **Controller → Service → Repository** layered pattern
- **DTO mapping** with static `from()` factory methods on response DTOs
- **BaseModel** provides audit fields (createdAt, createdBy, lastUpdatedAt, lastUpdatedBy)
- **Organization context** for multi-tenant board visibility
- **WebSocket event broadcasting** for real-time board updates
- **Async event listeners** for notification creation

---

## 3. External Dependencies

These are modules/classes the Task Manager depends on. You must provide equivalents in the target project.

### 3.1 BaseModel (Audit Fields)

```java
package com.gonerp.common;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseModel {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @LastModifiedBy
    @Column(name = "last_updated_by")
    private String lastUpdatedBy;
}
```

> Requires `@EnableJpaAuditing` and an `AuditorAware<String>` bean.

### 3.2 User Entity

The task manager references a `User` entity with these fields:
- `Long id`
- `String userName`
- `String firstName`
- `String lastName`
- `String avatarUrl`
- `String role` (ADMIN, SUPER_ADMIN, USER)

### 3.3 Organization Entity

Referenced for multi-tenant board visibility:
- `Long id`
- `String name`

### 3.4 OrgContext Utility

Provides the current user's organization context:
```java
OrgContext.getCurrentOrgId()  // Returns Long or null
```

### 3.5 FileStorageService

For uploading files to cloud storage (R2/S3):
```java
FileStorageService.uploadFile(MultipartFile file, String folder) → String url
FileStorageService.deleteFile(String url) → void
```

### 3.6 Security / Auth

- JWT-based authentication with `SecurityContextHolder`
- `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` for admin-only endpoints
- Current user retrieved from security context

### 3.7 OrgStructureRequest / OrgStructureResponse

Simple DTOs used by TaskConfig and DesignConfig:
```java
// Request
@Data
public class OrgStructureRequest {
    private String name;
    private String description;
}

// Response
@Data @Builder
public class OrgStructureResponse {
    private Long id;
    private String name;
    private String description;
}
```

---

## 4. Database Schema

All tables use the `tm_` prefix. All tables include audit columns from BaseModel unless noted otherwise.

### 4.1 Core Tables

```sql
-- Boards
CREATE TABLE tm_boards (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    cover_color     VARCHAR(20) DEFAULT '#2E7D32',
    board_type      VARCHAR(30) DEFAULT 'GENERAL',  -- GENERAL, POD_DESIGN
    active          BOOLEAN DEFAULT TRUE,
    owner_id        BIGINT NOT NULL REFERENCES users(id),
    organization_id BIGINT REFERENCES organizations(id),
    created_at      TIMESTAMP NOT NULL,
    created_by      VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Board Members
CREATE TABLE tm_board_members (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT NOT NULL REFERENCES tm_boards(id),
    user_id    BIGINT NOT NULL REFERENCES users(id),
    role       VARCHAR(20) NOT NULL,  -- OWNER, ADMIN, MEMBER
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255),
    UNIQUE(board_id, user_id)
);

-- Columns
CREATE TABLE tm_columns (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT NOT NULL REFERENCES tm_boards(id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    position   INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Cards
CREATE TABLE tm_cards (
    id             BIGSERIAL PRIMARY KEY,
    column_id      BIGINT NOT NULL REFERENCES tm_columns(id) ON DELETE CASCADE,
    name           VARCHAR(500) NOT NULL,
    description    TEXT,
    position       INT NOT NULL,
    status         VARCHAR(20),  -- OPEN, IN_PROGRESS, DONE, BLOCKED, CANCELLED
    stage          VARCHAR(200),
    main_image_url TEXT,
    created_at     TIMESTAMP NOT NULL,
    created_by     VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);
```

### 4.2 Card Feature Tables

```sql
-- Labels (board-level)
CREATE TABLE tm_labels (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT NOT NULL REFERENCES tm_boards(id),
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20) DEFAULT '#2E7D32',
    text_color VARCHAR(20) DEFAULT '#ffffff',
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Card-Label mapping (many-to-many)
CREATE TABLE tm_card_label_map (
    card_id  BIGINT NOT NULL REFERENCES tm_cards(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES tm_labels(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, label_id)
);

-- Types (board-level)
CREATE TABLE tm_card_types (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT NOT NULL REFERENCES tm_boards(id),
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20),
    text_color VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Card-Type mapping (many-to-many)
CREATE TABLE tm_card_type_map (
    card_id BIGINT NOT NULL REFERENCES tm_cards(id) ON DELETE CASCADE,
    type_id BIGINT NOT NULL REFERENCES tm_card_types(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, type_id)
);

-- Card Members (many-to-many)
CREATE TABLE tm_card_members (
    id         BIGSERIAL PRIMARY KEY,
    card_id    BIGINT NOT NULL REFERENCES tm_cards(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255),
    UNIQUE(card_id, user_id)
);

-- Comments (with threading via parent_id)
CREATE TABLE tm_card_comments (
    id         BIGSERIAL PRIMARY KEY,
    card_id    BIGINT NOT NULL REFERENCES tm_cards(id) ON DELETE CASCADE,
    author_id  BIGINT NOT NULL REFERENCES users(id),
    content    TEXT,
    parent_id  BIGINT REFERENCES tm_card_comments(id),
    image_urls TEXT,  -- JSON array stored as text via JPA converter
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Comment Reactions
CREATE TABLE tm_comment_reactions (
    id            BIGSERIAL PRIMARY KEY,
    comment_id    BIGINT NOT NULL REFERENCES tm_card_comments(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    reaction_type VARCHAR(50) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    created_by    VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Attachments
CREATE TABLE tm_card_attachments (
    id         BIGSERIAL PRIMARY KEY,
    card_id    BIGINT NOT NULL REFERENCES tm_cards(id) ON DELETE CASCADE,
    name       VARCHAR(500),
    url        TEXT NOT NULL,
    file_type  VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Links
CREATE TABLE tm_card_links (
    id         BIGSERIAL PRIMARY KEY,
    card_id    BIGINT NOT NULL REFERENCES tm_cards(id) ON DELETE CASCADE,
    url        TEXT NOT NULL,
    title      VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Activity Log
CREATE TABLE tm_card_activities (
    id         BIGSERIAL PRIMARY KEY,
    card_id    BIGINT NOT NULL REFERENCES tm_cards(id) ON DELETE CASCADE,
    actor_id   BIGINT REFERENCES users(id),
    action     TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);
```

### 4.3 Design Tables (for POD_DESIGN boards)

```sql
-- Design Details (linked to cards or standalone)
CREATE TABLE tm_design_details (
    id              BIGSERIAL PRIMARY KEY,
    card_id         BIGINT UNIQUE REFERENCES tm_cards(id),
    name            VARCHAR(500),
    idea_creator_id BIGINT REFERENCES users(id),
    design_status   VARCHAR(20),  -- PENDING, APPROVED, DELETED
    approval_date   TIMESTAMP,
    custom          BOOLEAN DEFAULT FALSE,
    occasion_id     BIGINT REFERENCES tm_occasions(id),
    created_at      TIMESTAMP NOT NULL,
    created_by      VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

-- Design Files
CREATE TABLE tm_design_files (
    id               BIGSERIAL PRIMARY KEY,
    design_detail_id BIGINT NOT NULL REFERENCES tm_design_details(id) ON DELETE CASCADE,
    name             VARCHAR(500),
    url              TEXT NOT NULL,
    file_type        VARCHAR(100),
    file_category    VARCHAR(20) NOT NULL,  -- PNG, PSD
    created_at       TIMESTAMP NOT NULL,
    created_by       VARCHAR(255),
    last_updated_at  TIMESTAMP,
    last_updated_by  VARCHAR(255)
);

-- Design Mockups
CREATE TABLE tm_design_mockups (
    id               BIGSERIAL PRIMARY KEY,
    design_detail_id BIGINT NOT NULL REFERENCES tm_design_details(id) ON DELETE CASCADE,
    name             VARCHAR(500),
    url              TEXT NOT NULL,
    file_type        VARCHAR(100),
    is_main          BOOLEAN DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL,
    created_by       VARCHAR(255),
    last_updated_at  TIMESTAMP,
    last_updated_by  VARCHAR(255)
);

-- Design-Designer mapping (many-to-many)
CREATE TABLE tm_design_designers (
    design_detail_id BIGINT NOT NULL REFERENCES tm_design_details(id) ON DELETE CASCADE,
    user_id          BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (design_detail_id, user_id)
);

-- Design-ProductType mapping (many-to-many)
CREATE TABLE tm_design_product_types (
    design_detail_id BIGINT NOT NULL REFERENCES tm_design_details(id) ON DELETE CASCADE,
    product_type_id  BIGINT NOT NULL REFERENCES tm_product_types(id),
    PRIMARY KEY (design_detail_id, product_type_id)
);

-- Design-Niche mapping (many-to-many)
CREATE TABLE tm_design_niches (
    design_detail_id BIGINT NOT NULL REFERENCES tm_design_details(id) ON DELETE CASCADE,
    niche_id         BIGINT NOT NULL REFERENCES tm_niches(id),
    PRIMARY KEY (design_detail_id, niche_id)
);
```

### 4.4 Lookup Tables

```sql
CREATE TABLE tm_product_types (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL,
    created_by  VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

CREATE TABLE tm_niches (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL,
    created_by  VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);

CREATE TABLE tm_occasions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL,
    created_by  VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);
```

### 4.5 Notification Table

```sql
CREATE TABLE tm_notifications (
    id           BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    actor_id     BIGINT REFERENCES users(id),
    type         VARCHAR(50) NOT NULL,
    message      TEXT,
    board_id     BIGINT,
    card_id      BIGINT,
    card_name    VARCHAR(500),
    read         BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL,
    created_by   VARCHAR(255),
    last_updated_at TIMESTAMP,
    last_updated_by VARCHAR(255)
);
```

### 4.6 Config Tables (Staff Roles & User Groups)

```sql
-- Task Staff Roles
CREATE TABLE tm_task_staff_roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP, created_by VARCHAR(255),
    last_updated_at TIMESTAMP, last_updated_by VARCHAR(255)
);

-- Task User Groups
CREATE TABLE tm_task_user_groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP, created_by VARCHAR(255),
    last_updated_at TIMESTAMP, last_updated_by VARCHAR(255)
);

-- User <-> Task Staff Role assignments
CREATE TABLE tm_user_task_staff_roles (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES tm_task_staff_roles(id),
    created_at TIMESTAMP, created_by VARCHAR(255),
    last_updated_at TIMESTAMP, last_updated_by VARCHAR(255)
);

-- User <-> Task User Group assignments
CREATE TABLE tm_user_task_user_groups (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT NOT NULL REFERENCES users(id),
    group_id BIGINT NOT NULL REFERENCES tm_task_user_groups(id),
    created_at TIMESTAMP, created_by VARCHAR(255),
    last_updated_at TIMESTAMP, last_updated_by VARCHAR(255)
);

-- Same structure for Design config:
-- tm_design_staff_roles, tm_design_user_groups
-- tm_user_design_staff_roles, tm_user_design_user_groups
```

---

## 5. Enums

```java
public enum BoardType { GENERAL, POD_DESIGN }

public enum BoardMemberRole { OWNER, ADMIN, MEMBER }

public enum CardStatus { OPEN, IN_PROGRESS, DONE, BLOCKED, CANCELLED }

public enum DesignFileCategory { PNG, PSD }

public enum DesignStatus { PENDING, APPROVED, DELETED }

public enum NotificationType {
    CARD_MEMBER_ADDED,
    CARD_STAGE_MOVED,
    COMMENT_MENTION,
    DAY_OFF_REQUESTED,
    DAY_OFF_APPROVED,
    DAY_OFF_DENIED,
    AUTO_CHECKOUT_REMINDER;

    public boolean isImportant() {
        return this == COMMENT_MENTION
            || this == DAY_OFF_REQUESTED
            || this == DAY_OFF_APPROVED
            || this == DAY_OFF_DENIED;
    }
}
```

---

## 6. Backend Models

### 6.1 Board

```java
@Entity @Table(name = "tm_boards")
public class Board extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "cover_color", length = 20) @Builder.Default private String coverColor = "#2E7D32";
    @Enumerated(EnumType.STRING) @Builder.Default private BoardType boardType = BoardType.GENERAL;
    @Builder.Default private boolean active = true;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @OneToMany(mappedBy = "board", cascade = ALL, orphanRemoval = true) @OrderBy("position ASC")
    private List<BoardColumn> columns = new ArrayList<>();
    @OneToMany(mappedBy = "board", cascade = ALL, orphanRemoval = true)
    private List<BoardMember> members = new ArrayList<>();
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "organization_id") private Organization organization;
}
```

### 6.2 BoardColumn

```java
@Entity @Table(name = "tm_columns")
public class BoardColumn extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "board_id", nullable = false) private Board board;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false) private int position;
    @OneToMany(mappedBy = "column", cascade = ALL, orphanRemoval = true) @OrderBy("position ASC")
    private List<Card> cards = new ArrayList<>();
}
```

### 6.3 Card

```java
@Entity @Table(name = "tm_cards")
public class Card extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "column_id", nullable = false) private BoardColumn column;
    @Column(nullable = false, length = 500) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private int position;
    @Enumerated(EnumType.STRING) private CardStatus status;
    @Column(length = 200) private String stage;
    @Column(name = "main_image_url", columnDefinition = "TEXT") private String mainImageUrl;

    // Relationships
    @ManyToMany @JoinTable(name = "tm_card_label_map",
        joinColumns = @JoinColumn(name = "card_id"),
        inverseJoinColumns = @JoinColumn(name = "label_id"))
    private List<CardLabel> labels = new ArrayList<>();

    @ManyToMany @JoinTable(name = "tm_card_type_map",
        joinColumns = @JoinColumn(name = "card_id"),
        inverseJoinColumns = @JoinColumn(name = "type_id"))
    private List<CardType> types = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = ALL, orphanRemoval = true)
    private List<CardMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = ALL, orphanRemoval = true) @OrderBy("createdAt DESC")
    private List<CardComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = ALL, orphanRemoval = true) @OrderBy("createdAt DESC")
    private List<CardAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = ALL, orphanRemoval = true) @OrderBy("createdAt DESC")
    private List<CardLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = ALL, orphanRemoval = true) @OrderBy("createdAt DESC")
    private List<CardActivity> activities = new ArrayList<>();
}
```

### 6.4 BoardMember

```java
@Entity @Table(name = "tm_board_members", uniqueConstraints = @UniqueConstraint(columns = {"board_id", "user_id"}))
public class BoardMember extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "board_id", nullable = false) private Board board;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BoardMemberRole role;
}
```

### 6.5 CardMember

```java
@Entity @Table(name = "tm_card_members", uniqueConstraints = @UniqueConstraint(columns = {"card_id", "user_id"}))
public class CardMember extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "card_id", nullable = false) private Card card;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
}
```

### 6.6 CardLabel

```java
@Entity @Table(name = "tm_labels")
public class CardLabel extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "board_id", nullable = false) private Board board;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 20) @Builder.Default private String color = "#2E7D32";
    @Column(name = "text_color", length = 20) @Builder.Default private String textColor = "#ffffff";
}
```

### 6.7 CardType

```java
@Entity @Table(name = "tm_card_types")
public class CardType extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "board_id", nullable = false) private Board board;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 20) private String color;
    @Column(name = "text_color", length = 20) private String textColor;
}
```

### 6.8 CardComment (Threaded)

```java
@Entity @Table(name = "tm_card_comments")
public class CardComment extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "card_id", nullable = false) private Card card;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "author_id", nullable = false) private User author;
    @Column(columnDefinition = "TEXT") private String content;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "parent_id") private CardComment parent;
    @OneToMany(mappedBy = "parent", cascade = ALL, orphanRemoval = true) @OrderBy("createdAt ASC")
    private List<CardComment> replies = new ArrayList<>();
    @Convert(converter = StringListConverter.class) @Column(name = "image_urls", columnDefinition = "TEXT")
    private List<String> imageUrls;
    @OneToMany(mappedBy = "comment", cascade = ALL, orphanRemoval = true)
    private List<CommentReaction> reactions = new ArrayList<>();
    @ManyToMany @JoinTable(name = "tm_comment_mentions",
        joinColumns = @JoinColumn(name = "comment_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> mentionedUsers = new ArrayList<>();
}
```

### 6.9 CommentReaction

```java
@Entity @Table(name = "tm_comment_reactions")
public class CommentReaction extends BaseModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "comment_id", nullable = false) private CardComment comment;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "reaction_type", length = 50, nullable = false) private String reactionType;
}
```

### 6.10 CardAttachment, CardLink, CardActivity

```java
// CardAttachment
@Entity @Table(name = "tm_card_attachments")
public class CardAttachment extends BaseModel {
    @Id @GeneratedValue private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "card_id", nullable = false) private Card card;
    private String name;
    @Column(nullable = false) private String url;
    private String fileType;
}

// CardLink
@Entity @Table(name = "tm_card_links")
public class CardLink extends BaseModel {
    @Id @GeneratedValue private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "card_id", nullable = false) private Card card;
    @Column(nullable = false) private String url;
    private String title;
}

// CardActivity
@Entity @Table(name = "tm_card_activities")
public class CardActivity extends BaseModel {
    @Id @GeneratedValue private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "card_id", nullable = false) private Card card;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "actor_id") private User actor;
    @Column(nullable = false, columnDefinition = "TEXT") private String action;
}
```

### 6.11 StringListConverter (JPA Converter)

```java
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        try { return list != null ? mapper.writeValueAsString(list) : null; }
        catch (Exception e) { return null; }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        try { return json != null ? mapper.readValue(json, new TypeReference<>() {}) : new ArrayList<>(); }
        catch (Exception e) { return new ArrayList<>(); }
    }
}
```

### 6.12 Notification

```java
@Entity @Table(name = "tm_notifications")
public class Notification extends BaseModel {
    @Id @GeneratedValue private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "recipient_id", nullable = false) private User recipient;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "actor_id") private User actor;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Column(columnDefinition = "TEXT") private String message;
    private Long boardId;
    private Long cardId;
    private String cardName;
    @Column(nullable = false) @Builder.Default private boolean read = false;
}
```

### 6.13 Design Models

```java
// DesignDetail
@Entity @Table(name = "tm_design_details")
public class DesignDetail extends BaseModel {
    @Id @GeneratedValue private Long id;
    @OneToOne(fetch = LAZY) @JoinColumn(name = "card_id", unique = true) private Card card;
    private String name;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "idea_creator_id") private User ideaCreator;
    @Enumerated(EnumType.STRING) private DesignStatus designStatus;
    private LocalDateTime approvalDate;
    @Builder.Default private boolean custom = false;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "occasion_id") private Occasion occasion;

    @ManyToMany @JoinTable(name = "tm_design_designers") private List<User> designers = new ArrayList<>();
    @ManyToMany @JoinTable(name = "tm_design_product_types") private List<ProductType> productTypes = new ArrayList<>();
    @ManyToMany @JoinTable(name = "tm_design_niches") private List<Niche> niches = new ArrayList<>();
    @OneToMany(mappedBy = "designDetail", cascade = ALL, orphanRemoval = true) private List<DesignFile> designFiles = new ArrayList<>();
    @OneToMany(mappedBy = "designDetail", cascade = ALL, orphanRemoval = true) private List<DesignMockup> mockups = new ArrayList<>();
}

// DesignFile
@Entity @Table(name = "tm_design_files")
public class DesignFile extends BaseModel {
    @Id @GeneratedValue private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "design_detail_id", nullable = false) private DesignDetail designDetail;
    private String name;
    @Column(nullable = false) private String url;
    private String fileType;
    @Enumerated(EnumType.STRING) @Column(name = "file_category", nullable = false) private DesignFileCategory fileCategory;
}

// DesignMockup
@Entity @Table(name = "tm_design_mockups")
public class DesignMockup extends BaseModel {
    @Id @GeneratedValue private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "design_detail_id", nullable = false) private DesignDetail designDetail;
    private String name;
    @Column(nullable = false) private String url;
    private String fileType;
    @Column(name = "is_main") @Builder.Default private boolean mainMockup = false;
}

// Lookup entities (ProductType, Niche, Occasion) - same structure:
@Entity @Table(name = "tm_product_types")
public class ProductType extends BaseModel {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false) private String name;
    private String description;
}
```

### 6.14 Config Models (TaskStaffRole, TaskUserGroup, etc.)

```java
// TaskStaffRole
@Entity @Table(name = "tm_task_staff_roles")
public class TaskStaffRole extends BaseModel {
    @Id @GeneratedValue private Long id;
    private String name;
    private String description;
}

// UserTaskStaffRole (junction table)
@Entity @Table(name = "tm_user_task_staff_roles")
public class UserTaskStaffRole extends BaseModel {
    @Id @GeneratedValue private Long id;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "role_id", nullable = false) private TaskStaffRole taskStaffRole;
}

// Same pattern for: TaskUserGroup, UserTaskUserGroup,
// DesignStaffRole, UserDesignStaffRole, DesignUserGroup, UserDesignUserGroup
```

---

## 7. DTOs

### 7.1 Board DTOs

```java
// BoardRequest
@Data
public class BoardRequest {
    @NotBlank private String name;
    private String description;
    private String coverColor;
    private String boardType;
}

// BoardResponse (full board with columns, members, labels, types)
@Data @Builder
public class BoardResponse {
    private Long id;
    private String name, description, coverColor;
    private BoardType boardType;
    private UserSummaryResponse owner;
    private List<BoardMemberResponse> members;
    private List<ColumnResponse> columns;
    private List<LabelResponse> labels;
    private List<TypeResponse> types;
    // static from(Board board, List<LabelResponse> labels, List<TypeResponse> types)
}

// BoardSummaryResponse (for board list)
@Data @Builder
public class BoardSummaryResponse {
    private Long id;
    private String name, description, coverColor;
    private BoardType boardType;
    private String ownerName;
    private Long ownerId;
    private int memberCount, columnCount;
    private LocalDateTime createdAt;
    // static from(Board board)
}
```

### 7.2 Column DTOs

```java
@Data public class ColumnRequest { @NotBlank private String title; }

@Data @Builder
public class ColumnResponse {
    private Long id;
    private String title;
    private int position;
    private Long boardId;
    private List<CardSummaryResponse> cards;
    // static from(BoardColumn column)
}
```

### 7.3 Card DTOs

```java
@Data
public class CardRequest {
    private String name, description;
    private CardStatus status;
    private String mainImageUrl;
}

@Data public class CardMoveRequest { private Long targetColumnId; private int position; }

@Data @Builder
public class CardSummaryResponse {
    private Long id;
    private String name, stage, mainImageUrl;
    private CardStatus status;
    private int position;
    private List<LabelResponse> labels;
    private List<TypeResponse> types;
    private List<UserSummaryResponse> members;
    private int commentCount, attachmentCount;
    private LocalDateTime createdAt;
    // static from(Card card)
}

@Data @Builder
public class CardDetailResponse {
    private Long id;
    private String name, description, stage, mainImageUrl;
    private CardStatus status;
    private int position;
    private Long columnId, boardId;
    private List<LabelResponse> labels;
    private List<TypeResponse> types;
    private List<UserSummaryResponse> members;
    private List<AttachmentResponse> attachments;
    private List<LinkResponse> links;
    private List<CommentResponse> comments;  // top-level only (no parent)
    private List<ActivityResponse> activities;
    private LocalDateTime createdAt, lastUpdatedAt;
    private String createdBy, lastUpdatedBy;
    private DesignDetailResponse designDetail;
    // static from(Card card, Long currentUserId)
}
```

### 7.4 Comment DTOs

```java
@Data
public class CommentRequest {
    private String content;
    private Long parentCommentId;
    private List<String> imageUrls;
    private List<Long> mentionedUserIds;
}

@Data @Builder
public class CommentResponse {
    private Long id;
    private String content;
    private UserSummaryResponse author;
    private LocalDateTime createdAt;
    private Long parentId;
    private List<CommentResponse> replies;
    private List<String> imageUrls;
    private Map<String, ReactionInfo> reactions;  // key = emoji, value = {count, reacted, users}
    private List<UserSummaryResponse> mentionedUsers;
    // static from(CardComment comment, Long currentUserId)
}

@Data public class ReactionRequest { @NotBlank private String reactionType; }
@Data @Builder public class ReactionInfo { private int count; private boolean reacted; private List<UserSummaryResponse> users; }
```

### 7.5 Other DTOs

```java
@Data @Builder public class AttachmentResponse { Long id; String name, url, fileType; LocalDateTime createdAt; String createdBy; }
@Data public class LinkRequest { @NotBlank String url; String title; }
@Data @Builder public class LinkResponse { Long id; String url, title; LocalDateTime createdAt; String createdBy; }
@Data @Builder public class LabelResponse { Long id; String name, color, textColor; }
@Data public class LabelRequest { String name, color, textColor; }
@Data @Builder public class TypeResponse { Long id; String name, color, textColor; }
@Data public class TypeRequest { String name, color, textColor; }
@Data @Builder public class ActivityResponse { Long id; String action, actorName; LocalDateTime createdAt; }
@Data public class ReorderRequest { List<Long> orderedIds; }

@Data public class BoardMemberRequest { Long userId; BoardMemberRole role; }
@Data @Builder public class BoardMemberResponse { Long id; UserSummaryResponse user; BoardMemberRole role; }

@Data
public class UserSummaryResponse {
    private Long id;
    private String userName, firstName, lastName, avatarUrl;
    // static from(User user)
}

@Data @Builder
public class NotificationResponse {
    Long id; String type; boolean important; String message;
    Long boardId, cardId; String cardName; boolean read;
    UserSummaryResponse actor; LocalDateTime createdAt;
}
```

### 7.6 Design DTOs

```java
@Data public class DesignDetailRequest { String name; Long ideaCreatorId; List<Long> productTypeIds, nicheIds; Long occasionId; Boolean custom; }
@Data public class CreateDesignRequest { String name; Long ideaCreatorId; List<Long> productTypeIds, nicheIds; Long occasionId; Boolean custom; }
@Data @Builder public class DesignDetailResponse { Long id; String name; List<DesignFileResponse> pngFiles, psdFiles; UserSummaryResponse ideaCreator; List<UserSummaryResponse> designers; LocalDateTime approvalDate; List<LookupResponse> productTypes, niches; LookupResponse occasion; boolean custom; String designStatus; List<DesignMockupResponse> mockups; String mainMockupUrl; }
@Data @Builder public class DesignSummaryResponse { Long id, cardId, boardId; String cardName, boardName, stage, status, name, designStatus, mainMockupUrl; boolean boardActive, custom; UserSummaryResponse ideaCreator; List<UserSummaryResponse> designers; List<LookupResponse> productTypes, niches; LookupResponse occasion; LocalDateTime approvalDate, createdAt; }
@Data @Builder public class DesignFileResponse { Long id; String name, url, fileType, fileCategory; LocalDateTime createdAt; }
@Data @Builder public class DesignMockupResponse { Long id; String name, url, fileType; boolean mainMockup; LocalDateTime createdAt; }

@Data public class LookupRequest { @NotBlank String name; String description; }
@Data @Builder public class LookupResponse { Long id; String name, description; }
```

---

## 8. Repository Layer

All repositories extend `JpaRepository<Entity, Long>`. Key custom queries:

```java
// BoardRepository
@Query("SELECT DISTINCT b FROM Board b JOIN FETCH b.owner WHERE b.active = true AND b.organization.id = :orgId AND (b.owner.id = :userId OR EXISTS (SELECT m FROM BoardMember m WHERE m.board = b AND m.user.id = :userId))")
List<Board> findAllVisibleToUserInOrg(@Param("userId") Long userId, @Param("orgId") Long orgId);

@Query("SELECT DISTINCT b FROM Board b JOIN FETCH b.owner WHERE b.active = true AND b.organization.id = :orgId")
List<Board> findAllByOrganizationId(@Param("orgId") Long orgId);

// Also: findAllVisibleToUser(userId), findAllWithOwner()

// BoardColumnRepository
List<BoardColumn> findByBoardIdOrderByPositionAsc(Long boardId);
int countByBoardId(Long boardId);

// CardRepository
List<Card> findByColumnIdOrderByPositionAsc(Long columnId);
int countByColumnId(Long columnId);

// BoardMemberRepository
Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);
boolean existsByBoardIdAndUserId(Long boardId, Long userId);

// CardMemberRepository
boolean existsByCardIdAndUserId(Long cardId, Long userId);
Optional<CardMember> findByCardIdAndUserId(Long cardId, Long userId);

// CardLabelRepository
List<CardLabel> findByBoardId(Long boardId);
@Modifying @Query(nativeQuery) void deleteLabelMappings(@Param("labelId") Long labelId);

// CardTypeRepository - same pattern as CardLabelRepository

// CommentReactionRepository
List<CommentReaction> findByCommentId(Long commentId);
Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

// NotificationRepository
Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
Page<Notification> findByRecipientIdAndTypeInOrderByCreatedAtDesc(Long recipientId, List<NotificationType> types, Pageable pageable);
long countByRecipientIdAndReadFalse(Long recipientId);
@Modifying @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
void markAllAsRead(@Param("recipientId") Long recipientId);

// DesignDetailRepository extends JpaSpecificationExecutor<DesignDetail>
Optional<DesignDetail> findByCardId(Long cardId);

// DesignMockupRepository
@Modifying @Query("UPDATE DesignMockup m SET m.mainMockup = false WHERE m.designDetail.id = :designDetailId")
void clearMainMockup(@Param("designDetailId") Long designDetailId);
```

---

## 9. Service Layer

### 9.1 BoardService

Key business rules:
- **Board visibility**: Admin/SuperAdmin sees all boards in org; regular users see only boards they own or are members of
- **POD_DESIGN boards**: Auto-creates fixed columns on creation: `Draft, Idea, Doing, Checking, Need to Fix, Fixing, Fix-Checking, Done, Listed, Canceled`
- **POD_DESIGN boards**: Cannot be deleted, only deactivated (set `active = false`)
- **Board owner**: Automatically added as OWNER member on creation
- **Access control**: Only board owner or admin members can manage the board

Methods:
```
findAll() → List<BoardSummaryResponse>         // Filtered by org context + user visibility
findById(Long id) → BoardResponse              // Full board with columns and cards
create(BoardRequest) → BoardSummaryResponse     // Creates board + auto columns for POD_DESIGN
update(Long id, BoardRequest) → BoardSummaryResponse
delete(Long id) → void                         // Delete or deactivate
addMember(boardId, BoardMemberRequest) → void
updateMemberRole(boardId, userId, BoardMemberRequest) → void
removeMember(boardId, userId) → void
getLabels(boardId) → List<LabelResponse>
createLabel(boardId, LabelRequest) → LabelResponse
deleteLabel(labelId) → void
getTypes(boardId) → List<TypeResponse>
createType(boardId, TypeRequest) → TypeResponse
deleteType(typeId) → void
```

### 9.2 CardService

Key business rules:
- **Card creation**: Creator auto-added as card member
- **Card move**: Updates column and position; for POD_DESIGN boards, updates `stage` from column title
- **Comment mentions**: Publishes NotificationEvent for mentioned users
- **Activity logging**: Records card actions (created, updated, moved, etc.)
- **WebSocket**: Publishes board events on card changes

Methods:
```
findById(Long id) → CardDetailResponse
create(columnId, CardRequest) → CardDetailResponse
update(id, CardRequest) → CardDetailResponse
delete(id) → void
moveCard(cardId, CardMoveRequest) → void
reorderCards(columnId, ReorderRequest) → void
addComment(cardId, CommentRequest) → CommentResponse
updateComment(cardId, commentId, CommentRequest) → CommentResponse
deleteComment(cardId, commentId) → void
toggleReaction(cardId, commentId, ReactionRequest) → Map<String, ReactionInfo>
uploadCommentImage(cardId, MultipartFile) → String (URL)
uploadAttachment(cardId, MultipartFile, name) → AttachmentResponse
deleteAttachment(cardId, attachmentId) → void
addLink(cardId, LinkRequest) → LinkResponse
deleteLink(cardId, linkId) → void
addLabel(cardId, labelId) → void
removeLabel(cardId, labelId) → void
addType(cardId, typeId) → void
removeType(cardId, typeId) → void
addCardMember(cardId, userId) → void
removeCardMember(cardId, userId) → void
```

### 9.3 ColumnService

Key business rules:
- **POD_DESIGN boards**: Column create/update/delete throws exception (fixed columns)
- **Reorder**: Updates position field based on ordered IDs list

### 9.4 NotificationService

- Uses `@Async` + `@EventListener` to handle `NotificationEvent` asynchronously
- Creates a `Notification` record for each recipient (excluding the actor)
- Sends WebSocket notification to `/topic/notifications/{userId}`

### 9.5 DesignDetailService, DesignsService, DesignConfigService, TaskConfigService, LookupService

Standard CRUD with access control. DesignsService uses JPA Specifications for complex filtering with pagination.

---

## 10. REST API Endpoints

### 10.1 Board Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/tasks/boards` | List visible boards | User |
| GET | `/api/tasks/boards/{id}` | Get board detail | User |
| POST | `/api/tasks/boards` | Create board | User |
| PUT | `/api/tasks/boards/{id}` | Update board | Board admin |
| DELETE | `/api/tasks/boards/{id}` | Delete/deactivate board | Board owner |
| POST | `/api/tasks/boards/{id}/members` | Add member | Board admin |
| PATCH | `/api/tasks/boards/{id}/members/{userId}` | Update member role | Board admin |
| DELETE | `/api/tasks/boards/{id}/members/{userId}` | Remove member | Board admin |
| GET | `/api/tasks/boards/{id}/labels` | Get labels | User |
| POST | `/api/tasks/boards/{id}/labels` | Create label | Board member |
| DELETE | `/api/tasks/boards/labels/{labelId}` | Delete label | Board member |
| GET | `/api/tasks/boards/{id}/types` | Get types | User |
| POST | `/api/tasks/boards/{id}/types` | Create type | Board member |
| DELETE | `/api/tasks/boards/types/{typeId}` | Delete type | Board member |

### 10.2 Column Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tasks/boards/{boardId}/columns` | Create column |
| PUT | `/api/tasks/columns/{id}` | Update column |
| DELETE | `/api/tasks/columns/{id}` | Delete column |
| PATCH | `/api/tasks/boards/{boardId}/columns/reorder` | Reorder columns |

### 10.3 Card Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tasks/columns/{columnId}/cards` | Create card |
| GET | `/api/tasks/cards/{id}` | Get card detail |
| PUT | `/api/tasks/cards/{id}` | Update card |
| DELETE | `/api/tasks/cards/{id}` | Delete card |
| PATCH | `/api/tasks/cards/{id}/move` | Move card to column |
| PATCH | `/api/tasks/columns/{columnId}/cards/reorder` | Reorder cards |
| POST | `/api/tasks/cards/{id}/comments` | Add comment |
| PUT | `/api/tasks/cards/{id}/comments/{commentId}` | Update comment |
| DELETE | `/api/tasks/cards/{id}/comments/{commentId}` | Delete comment |
| POST | `/api/tasks/cards/{id}/comments/{commentId}/reactions` | Toggle reaction |
| POST | `/api/tasks/cards/{id}/comments/image` | Upload comment image |
| POST | `/api/tasks/cards/{id}/attachments` | Upload attachment |
| DELETE | `/api/tasks/cards/{id}/attachments/{attachmentId}` | Delete attachment |
| POST | `/api/tasks/cards/{id}/links` | Add link |
| DELETE | `/api/tasks/cards/{id}/links/{linkId}` | Delete link |
| POST | `/api/tasks/cards/{id}/labels/{labelId}` | Add label to card |
| DELETE | `/api/tasks/cards/{id}/labels/{labelId}` | Remove label |
| POST | `/api/tasks/cards/{id}/types/{typeId}` | Add type to card |
| DELETE | `/api/tasks/cards/{id}/types/{typeId}` | Remove type |
| POST | `/api/tasks/cards/{id}/members/{userId}` | Add member |
| DELETE | `/api/tasks/cards/{id}/members/{userId}` | Remove member |
| GET | `/api/tasks/files/{filename}` | Serve file (query: `thumb=true`) |

### 10.4 Design Endpoints (Card-based)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tasks/cards/{cardId}/design` | Get design detail |
| PUT | `/api/tasks/cards/{cardId}/design` | Update design detail |
| POST | `/api/tasks/cards/{cardId}/design/png-files` | Upload PNG |
| DELETE | `/api/tasks/cards/{cardId}/design/png-files/{fileId}` | Delete PNG |
| POST | `/api/tasks/cards/{cardId}/design/psd-files` | Upload PSD |
| DELETE | `/api/tasks/cards/{cardId}/design/psd-files/{fileId}` | Delete PSD |
| POST | `/api/tasks/cards/{cardId}/design/mockups` | Upload mockup |
| DELETE | `/api/tasks/cards/{cardId}/design/mockups/{mockupId}` | Delete mockup |
| PATCH | `/api/tasks/cards/{cardId}/design/mockups/{mockupId}/main` | Set main mockup |
| POST | `/api/tasks/cards/{cardId}/design/designers/{userId}` | Add designer |
| DELETE | `/api/tasks/cards/{cardId}/design/designers/{userId}` | Remove designer |

### 10.5 Designs Endpoints (Standalone)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tasks/designs` | List designs (paginated, filtered) |
| POST | `/api/tasks/designs` | Create design |
| GET | `/api/tasks/designs/{designId}/detail` | Get design detail |
| PUT | `/api/tasks/designs/{designId}` | Update design |
| DELETE | `/api/tasks/designs/{designId}` | Delete design (ADMIN) |
| POST/DELETE | `/api/tasks/designs/{designId}/png-files`, `/psd-files`, `/mockups`, `/designers` | Same as card-based |

**Designs list filter params:** `page, size, boardId, ideaCreatorId, designerId, productTypeId, nicheId, occasionId, stage, status, custom, dateFrom, dateTo, search, sortBy, sortDir`

### 10.6 Notification Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notifications` | Get paginated notifications |
| GET | `/api/notifications/important` | Get important notifications |
| GET | `/api/notifications/unread-count` | Get unread count |
| PATCH | `/api/notifications/{id}/read` | Mark as read |
| PATCH | `/api/notifications/read-all` | Mark all as read |

### 10.7 Lookup Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET/POST/PUT/DELETE | `/api/tasks/product-types[/{id}]` | Product types CRUD | ADMIN |
| GET/POST/PUT/DELETE | `/api/tasks/niches[/{id}]` | Niches CRUD | ADMIN |
| GET/POST/PUT/DELETE | `/api/tasks/occasions[/{id}]` | Occasions CRUD | ADMIN |

### 10.8 Config Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET/POST/PUT/DELETE | `/api/tasks/config/staff-roles[/{id}]` | Task staff roles |
| GET/POST/PUT/DELETE | `/api/tasks/config/user-groups[/{id}]` | Task user groups |
| GET | `/api/tasks/config/users` | Users with task assignments |
| POST/DELETE | `/api/tasks/config/users/{userId}/staff-roles/{roleId}` | Assign/remove role |
| POST/DELETE | `/api/tasks/config/users/{userId}/user-groups/{groupId}` | Assign/remove group |
| *Same pattern* | `/api/tasks/designs/config/...` | Design config endpoints |

---

## 11. WebSocket

### 11.1 Server Configuration

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);  // JWT auth on CONNECT
    }
}
```

### 11.2 Auth Interceptor

Validates JWT token from `Authorization: Bearer <token>` STOMP header on CONNECT.

### 11.3 BoardEventPublisher

```java
@Component
public class BoardEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Long boardId, String type, Long cardId, Long columnId, String actorName, Object payload) {
        Map<String, Object> event = Map.of(
            "type", type,           // e.g., "CARD_CREATED", "CARD_MOVED", "CARD_UPDATED"
            "boardId", boardId,
            "cardId", cardId,
            "columnId", columnId,
            "actorName", actorName,
            "tabId", getTabId(),    // X-Tab-Id header from HTTP request (sender skip)
            "payload", payload,
            "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/board/" + boardId, event);
    }
}
```

### 11.4 Event Types (used in WebSocket messages)

- `CARD_CREATED` - New card added
- `CARD_UPDATED` - Card properties changed
- `CARD_MOVED` - Card moved to different column
- `CARD_DELETED` - Card removed
- `COLUMN_CREATED`, `COLUMN_UPDATED`, `COLUMN_DELETED`, `COLUMNS_REORDERED`
- `MEMBER_ADDED`, `MEMBER_REMOVED`, `MEMBER_ROLE_CHANGED`
- `COMMENT_ADDED`, `COMMENT_UPDATED`, `COMMENT_DELETED`

---

## 12. Notifications System

### 12.1 NotificationEvent (Application Event)

```java
@Data @Builder
public class NotificationEvent {
    private NotificationType type;
    private Long actorId;
    private Set<Long> recipientIds;
    private String message;
    private Long boardId;
    private Long cardId;
    private String cardName;
}
```

### 12.2 Publishing Notifications

Notifications are published from services using Spring's `ApplicationEventPublisher`:
```java
eventPublisher.publishEvent(NotificationEvent.builder()
    .type(NotificationType.CARD_MEMBER_ADDED)
    .actorId(currentUserId)
    .recipientIds(Set.of(targetUserId))
    .message("You were added to card: " + card.getName())
    .boardId(boardId)
    .cardId(card.getId())
    .cardName(card.getName())
    .build());
```

### 12.3 Handling (Async)

```java
@Async
@EventListener
public void handleNotificationEvent(NotificationEvent event) {
    // Creates Notification entity for each recipient (excluding actor)
    // Sends WebSocket message to /topic/notifications/{userId}
}
```

---

## 13. Frontend Architecture

```
frontend/src/
├── api/
│   └── tasks.js              # All task API endpoints
├── stores/
│   └── boardStore.js          # Pinia store for board state
├── composables/
│   └── useBoardSocket.js      # WebSocket composable
├── pages/
│   ├── TaskManagerPage.vue    # Board listing (GENERAL type)
│   ├── BoardPage.vue          # Kanban board view
│   └── DesignBoardsPage.vue   # Board listing (POD_DESIGN type)
├── components/
│   ├── BoardFormDialog.vue    # Create/edit board
│   ├── BoardMembersDialog.vue # Manage board members
│   ├── CardDetailDialog.vue   # Full card detail (largest component ~2200 lines)
│   ├── KanbanCard.vue         # Card display in column
│   └── TaskConfigDialog.vue   # Admin config for staff roles/groups
└── router/
    └── index.js               # Route definitions
```

---

## 14. Frontend API Layer

```javascript
// api/tasks.js
import api from './index'  // Axios instance with JWT interceptor

export const boardApi = {
  getAll:          ()           => api.get('/api/tasks/boards'),
  getById:         (id)         => api.get(`/api/tasks/boards/${id}`),
  create:          (data)       => api.post('/api/tasks/boards', data),
  update:          (id, data)   => api.put(`/api/tasks/boards/${id}`, data),
  delete:          (id)         => api.delete(`/api/tasks/boards/${id}`),
  addMember:       (id, data)   => api.post(`/api/tasks/boards/${id}/members`, data),
  updateMember:    (id, userId, data) => api.patch(`/api/tasks/boards/${id}/members/${userId}`, data),
  removeMember:    (id, userId) => api.delete(`/api/tasks/boards/${id}/members/${userId}`),
  getLabels:       (id)         => api.get(`/api/tasks/boards/${id}/labels`),
  createLabel:     (id, data)   => api.post(`/api/tasks/boards/${id}/labels`, data),
  deleteLabel:     (labelId)    => api.delete(`/api/tasks/boards/labels/${labelId}`),
  getTypes:        (id)         => api.get(`/api/tasks/boards/${id}/types`),
  createType:      (id, data)   => api.post(`/api/tasks/boards/${id}/types`, data),
  deleteType:      (typeId)     => api.delete(`/api/tasks/boards/types/${typeId}`)
}

export const columnApi = {
  create:  (boardId, data) => api.post(`/api/tasks/boards/${boardId}/columns`, data),
  update:  (id, data)      => api.put(`/api/tasks/columns/${id}`, data),
  delete:  (id)            => api.delete(`/api/tasks/columns/${id}`),
  reorder: (boardId, data) => api.patch(`/api/tasks/boards/${boardId}/columns/reorder`, data)
}

export const cardApi = {
  create:          (columnId, data) => api.post(`/api/tasks/columns/${columnId}/cards`, data),
  getById:         (id)             => api.get(`/api/tasks/cards/${id}`),
  update:          (id, data)       => api.put(`/api/tasks/cards/${id}`, data),
  delete:          (id)             => api.delete(`/api/tasks/cards/${id}`),
  move:            (id, data)       => api.patch(`/api/tasks/cards/${id}/move`, data),
  reorder:         (columnId, data) => api.patch(`/api/tasks/columns/${columnId}/cards/reorder`, data),
  addComment:      (id, data)       => api.post(`/api/tasks/cards/${id}/comments`, data),
  updateComment:   (id, commentId, data) => api.put(`/api/tasks/cards/${id}/comments/${commentId}`, data),
  deleteComment:   (id, commentId)  => api.delete(`/api/tasks/cards/${id}/comments/${commentId}`),
  toggleReaction:  (id, commentId, data) => api.post(`/api/tasks/cards/${id}/comments/${commentId}/reactions`, data),
  uploadCommentImage: (id, formData) => api.post(`/api/tasks/cards/${id}/comments/image`, formData),
  uploadAttachment:(id, formData)   => api.post(`/api/tasks/cards/${id}/attachments`, formData),
  deleteAttachment:(id, attId)      => api.delete(`/api/tasks/cards/${id}/attachments/${attId}`),
  addLink:         (id, data)       => api.post(`/api/tasks/cards/${id}/links`, data),
  deleteLink:      (id, linkId)     => api.delete(`/api/tasks/cards/${id}/links/${linkId}`),
  addLabel:        (id, labelId)    => api.post(`/api/tasks/cards/${id}/labels/${labelId}`),
  removeLabel:     (id, labelId)    => api.delete(`/api/tasks/cards/${id}/labels/${labelId}`),
  addType:         (id, typeId)     => api.post(`/api/tasks/cards/${id}/types/${typeId}`),
  removeType:      (id, typeId)     => api.delete(`/api/tasks/cards/${id}/types/${typeId}`),
  addMember:       (id, userId)     => api.post(`/api/tasks/cards/${id}/members/${userId}`),
  removeMember:    (id, userId)     => api.delete(`/api/tasks/cards/${id}/members/${userId}`)
}

export const designApi = {
  getDetail:       (cardId)               => api.get(`/api/tasks/cards/${cardId}/design`),
  update:          (cardId, data)         => api.put(`/api/tasks/cards/${cardId}/design`, data),
  uploadPng:       (cardId, formData)     => api.post(`/api/tasks/cards/${cardId}/design/png-files`, formData),
  deletePng:       (cardId, fileId)       => api.delete(`/api/tasks/cards/${cardId}/design/png-files/${fileId}`),
  uploadPsd:       (cardId, formData)     => api.post(`/api/tasks/cards/${cardId}/design/psd-files`, formData),
  deletePsd:       (cardId, fileId)       => api.delete(`/api/tasks/cards/${cardId}/design/psd-files/${fileId}`),
  uploadMockup:    (cardId, formData)     => api.post(`/api/tasks/cards/${cardId}/design/mockups`, formData),
  deleteMockup:    (cardId, mockupId)     => api.delete(`/api/tasks/cards/${cardId}/design/mockups/${mockupId}`),
  setMainMockup:   (cardId, mockupId)     => api.patch(`/api/tasks/cards/${cardId}/design/mockups/${mockupId}/main`),
  addDesigner:     (cardId, userId)       => api.post(`/api/tasks/cards/${cardId}/design/designers/${userId}`),
  removeDesigner:  (cardId, userId)       => api.delete(`/api/tasks/cards/${cardId}/design/designers/${userId}`)
}

export const lookupApi = {
  // getAll/create/update/delete for productTypes, niches, occasions
}

export const taskConfigApi = {
  // CRUD for staffRoles, userGroups, user assignments
}

export const designConfigApi = {
  // Same as taskConfigApi but for design context
}
```

---

## 15. Frontend State Management

```javascript
// stores/boardStore.js
import { defineStore } from 'pinia'
import { boardApi, columnApi, cardApi } from 'src/api/tasks'

export const useBoardStore = defineStore('board', {
  state: () => ({
    board: null,      // Full BoardResponse
    loading: false,
    error: null
  }),

  actions: {
    async loadBoard(boardId) {
      this.loading = true
      const { data } = await boardApi.getById(boardId)
      this.board = data.data  // ApiResponse wrapper
      this.loading = false
    },

    async reorderColumns(boardId, orderedIds) {
      // Optimistic update: reorder locally first
      const backup = [...this.board.columns]
      this.board.columns = orderedIds.map(id => this.board.columns.find(c => c.id === id))
      try {
        await columnApi.reorder(boardId, { orderedIds })
      } catch {
        this.board.columns = backup  // Rollback on failure
      }
    },

    async reorderCards(columnId, orderedIds) {
      await cardApi.reorder(columnId, { orderedIds })
    },

    async moveCard(cardId, targetColumnId, position) {
      await cardApi.move(cardId, { targetColumnId, position })
    }
  }
})
```

---

## 16. Frontend WebSocket Composable

```javascript
// composables/useBoardSocket.js
import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { useAuthStore } from 'src/stores/authStore'

export function useBoardSocket(boardId, onEvent) {
  const connected = ref(false)
  const tabId = crypto.randomUUID()  // Unique tab identifier
  let client = null

  function connect() {
    const authStore = useAuthStore()
    client = new Client({
      webSocketFactory: () => new SockJS('/api/ws'),
      connectHeaders: { Authorization: `Bearer ${authStore.token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        client.subscribe(`/topic/board/${boardId}`, (message) => {
          const event = JSON.parse(message.body)
          // Skip events from same tab (prevents echo)
          if (event.tabId !== tabId) {
            onEvent(event)
          }
        })
      },
      onDisconnect: () => { connected.value = false },
      onStompError: () => { connected.value = false }
    })
    client.activate()
  }

  function disconnect() {
    if (client) client.deactivate()
  }

  onUnmounted(disconnect)

  return { connected, tabId, connect, disconnect }
}
```

**Usage pattern in BoardPage.vue:**
```javascript
const { connected, tabId, connect } = useBoardSocket(boardId, handleBoardEvent)

function handleBoardEvent(event) {
  switch (event.type) {
    case 'CARD_CREATED':
    case 'CARD_UPDATED':
    case 'CARD_MOVED':
    case 'CARD_DELETED':
      boardStore.loadBoard(boardId)  // Refresh board data
      break
  }
}

// Send tabId in HTTP headers so server can tag WebSocket events
api.defaults.headers.common['X-Tab-Id'] = tabId
```

---

## 17. Frontend Pages

### 17.1 TaskManagerPage.vue (~211 lines)

- Displays grid of boards (filtered by `boardType === 'GENERAL'`)
- Board cards with cover color, name, description, member/column counts
- Create board button → opens `BoardFormDialog`
- Click board → navigates to `/tasks/:boardId`
- Settings button → opens `TaskConfigDialog` (admin only)

### 17.2 BoardPage.vue (~751 lines)

The main Kanban board view. Key features:

- **Column layout**: Horizontal scrollable columns with `vuedraggable`
- **Drag & drop**: Cards draggable between columns with auto-scroll at edges
- **WebSocket**: Real-time board sync via `useBoardSocket` composable
- **Card filtering**: Filter by members, labels, types, status, date range
- **Search**: Autocomplete search for cards across all columns
- **Deep linking**: `?cardId=123` opens card detail dialog on load
- **Column management**: Add/rename/delete columns (disabled for POD_DESIGN)
- **Live indicator**: Green dot when WebSocket connected
- **Member management**: Opens `BoardMembersDialog`

### 17.3 DesignBoardsPage.vue (~203 lines)

Same as TaskManagerPage but filters `boardType === 'POD_DESIGN'`. Uses deactivation instead of deletion.

---

## 18. Frontend Components

### 18.1 BoardFormDialog.vue (~105 lines)

- Form: name, description, cover color (8 presets), board type (GENERAL/POD_DESIGN)
- Warning for POD_DESIGN: "Columns will be automatically created"
- Emits: `created`, `updated`

### 18.2 BoardMembersDialog.vue (~196 lines)

- Lists current members with role badges
- Search users to add (filters out existing members)
- Role dropdown: MEMBER, ADMIN
- Remove member button
- Owner shown but not editable

### 18.3 KanbanCard.vue (~160 lines)

Card component displayed in Kanban columns:
- Cover image (square ratio, object-fit: cover)
- Label/type pills (colored chips)
- Card name
- Status badge
- Meta row: comment count, attachment count, member avatars (max 3)
- Hover: delete button appears
- Click: emits `open-detail`

### 18.4 CardDetailDialog.vue (~2265 lines)

The most complex component. Maximized dialog with:

**Header:**
- Stage display (for POD_DESIGN boards)
- Card name (editable inline)
- Delete button

**Left column:**
- Status & stage dropdowns with save button
- Labels section (add/remove, create new with 20 color presets)
- Types section (same as labels)
- Members section (add/remove from board members list)
- Description (Quasar rich text editor with image upload)
- Links section (add title + URL)
- Images section (grid view with lightbox/slideshow)
- Files section (attachment list)
- Activity log
- Design detail section (for POD_DESIGN boards)

**Right column:**
- Comments section
- Threaded replies
- Image attachments on comments
- Emoji reactions (toggle)
- @mention users
- Edit/delete comments

**Conflict handling:**
- Detects concurrent edits via `lastUpdatedAt` comparison
- Shows conflict banner with refresh option

### 18.5 TaskConfigDialog.vue (~343 lines)

Admin configuration with 3 tabs:
- **Staff Roles**: CRUD table for task staff roles
- **User Groups**: CRUD table for task user groups
- **User Assignments**: Table showing users with their assigned roles/groups, with add/remove buttons

---

## 19. Frontend Routes

```javascript
{ path: 'tasks',            name: 'tasks',       component: TaskManagerPage,   meta: { requiresModule: 'hasTaskManager' } }
{ path: 'tasks/:boardId',   name: 'board',       component: BoardPage,         meta: { requiresModule: 'hasTaskManager' } }
{ path: 'designs/boards',   name: 'designBoards', component: DesignBoardsPage, meta: { requiresModule: 'hasDesigns' } }
{ path: 'designs/boards/:boardId', name: 'designBoard', component: BoardPage,  meta: { requiresModule: 'hasDesigns' } }
```

Route guard checks `authStore.hasTaskManager` / `authStore.hasDesigns` feature flags.

---

## 20. Implementation Order

Follow this order for the most efficient implementation:

### Phase 1: Foundation
1. Set up external dependencies (BaseModel, User entity, Auth, FileStorage)
2. Create all enums
3. Create all JPA entities (models)
4. Create all repositories
5. Run application to verify Hibernate auto-creates tables

### Phase 2: Core Backend
6. Create DTOs (request + response classes)
7. Implement BoardService + BoardController
8. Implement ColumnService + ColumnController
9. Implement CardService + CardController (basic CRUD + move + reorder)
10. Test with Postman/curl

### Phase 3: Card Features
11. Add comments (with threading, reactions, mentions)
12. Add attachments and links
13. Add labels and types (board-level + card association)
14. Add card members
15. Add activity logging

### Phase 4: Real-time
16. Configure WebSocket (WebSocketConfig + AuthInterceptor)
17. Implement BoardEventPublisher
18. Add WebSocket publish calls in services
19. Implement NotificationService + NotificationController

### Phase 5: Frontend Core
20. Create API layer (`tasks.js`)
21. Create Pinia store (`boardStore.js`)
22. Create WebSocket composable (`useBoardSocket.js`)
23. Add routes

### Phase 6: Frontend Pages
24. Implement TaskManagerPage (board listing)
25. Implement BoardFormDialog
26. Implement KanbanCard component
27. Implement BoardPage (Kanban view with drag & drop)
28. Implement BoardMembersDialog

### Phase 7: Frontend Card Detail
29. Implement CardDetailDialog (start with basic fields)
30. Add comments section with threading
31. Add labels/types/members management
32. Add attachments and links
33. Add conflict detection

### Phase 8: Design Features (Optional - only for POD_DESIGN)
34. Design entities and DTOs
35. DesignDetailService + controllers
36. DesignsService (standalone designs with filtering)
37. Design config (staff roles, user groups)
38. Frontend design components

### Phase 9: Admin Config
39. TaskConfigService + controller
40. TaskConfigDialog component
41. Lookup CRUD (product types, niches, occasions)

---

## Tips for Claude-Assisted Implementation

When using Claude to implement this module in a new project:

1. **Share this document** as context in your Claude conversation
2. **Start with Phase 1-2** and share one controller+service pair as a pattern example
3. **Let Claude generate the rest** following the established patterns
4. **Test each phase** before moving to the next
5. **Adapt external dependencies** first - everything else follows the same patterns
6. **The CardDetailDialog.vue** is the most complex component - implement it incrementally, not all at once
