\# 🔐 DocuVault — Secure Document Management REST API



A Spring Boot REST API for storing documents securely. Files are encrypted with \*\*AES-256-GCM\*\* before being written to disk, access is controlled with \*\*JWT authentication\*\* and \*\*per-document permissions\*\*, and every document keeps a \*\*version history\*\* that can be restored. Documents can also be shared through \*\*expiring, password-protected, one-time-use links\*\*.



\*\*Live API docs (Swagger):\*\* https://docuvault-q8o1.onrender.com/swagger-ui.html

> Hosted on Render's free plan: the first request after inactivity can take 30–60 seconds, and uploaded files are lost when the instance restarts or redeploys (see \[Deployment](#deployment)).



\---



\## Table of Contents

\- \[Key Features](#key-features)

\- \[Tech Stack](#tech-stack)

\- \[Architecture Overview](#architecture-overview)

\- \[Project Structure](#project-structure)

\- \[Database Design](#database-design)

\- \[Authentication \& Authorization](#authentication--authorization)

\- \[File Encryption Flow](#file-encryption-flow)

\- \[Versioning](#versioning)

\- \[Sharing \& Permissions](#sharing--permissions)

\- \[Audit Logging](#audit-logging)

\- \[Search](#search)

\- \[API Endpoints](#api-endpoints)

\- \[Swagger / OpenAPI](#swagger--openapi)

\- \[Local Setup](#local-setup)

\- \[Environment Variables](#environment-variables)

\- \[Docker](#docker)

\- \[Deployment](#deployment)

\- \[Testing](#testing)

\- \[Access-Control Matrix](#access-control-matrix)

\- \[Security Considerations](#security-considerations)

\- \[Future Improvements](#future-improvements)

\- \[Author](#author)



\---



\## Key Features



\- \*\*User registration \& login\*\* with BCrypt-hashed passwords

\- \*\*JWT access token (15 min) + refresh token (7 days)\*\*

\- \*\*Document CRUD\*\* with ownership

\- \*\*AES-256-GCM encryption\*\* of every uploaded file (random IV per file)

\- \*\*Version history\*\*: every upload creates a new version; any version can be downloaded or restored

\- \*\*Document-level permissions\*\*: `EDITOR` and `VIEWER`, assigned by the owner

\- \*\*Share links\*\* with optional expiry, optional password and one-time-use

\- \*\*Audit logging\*\* of key document actions (with IP address)

\- \*\*Title search\*\* across documents the user owns or has been given access to

\- \*\*Centralised JSON error handling\*\*

\- \*\*Swagger UI\*\* with JWT "Authorize" support

\- \*\*Dockerised\*\* and deployed on Render



\---



\## Tech Stack



| Layer | Technology |

|---|---|

| Language | Java 21 |

| Framework | Spring Boot 4.2.0-M1 (Spring MVC, Spring Security, Spring Data JPA, Bean Validation) |

| Database | PostgreSQL (Hibernate, `ddl-auto=update`) |

| Authentication | JWT via JJWT 0.12.6 (stateless sessions) |

| Password hashing | BCrypt |

| File encryption | AES-256-GCM (`javax.crypto`) |

| API docs | springdoc-openapi 3.0.0 (Swagger UI) |

| Utilities | Lombok |

| Build | Maven (Maven Wrapper) |

| Code coverage | JaCoCo 0.8.13 (configured in `pom.xml`) |

| Containers | Docker (`eclipse-temurin:21-jdk`) |

| Hosting | Render (Web Service + managed PostgreSQL) |



\---



\## Architecture Overview



Classic layered architecture:



```mermaid

flowchart LR

&#x20;   Client\[Client / Swagger / Postman] --> F\[JwtAuthFilter]

&#x20;   F --> C\[Controllers]

&#x20;   C --> S\[Services]

&#x20;   S --> R\[Repositories - Spring Data JPA]

&#x20;   R --> DB\[(PostgreSQL)]

&#x20;   S --> ENC\[EncryptionService]

&#x20;   S --> FS\[FileStorageService]

&#x20;   FS --> DISK\[(uploads/ folder)]

&#x20;   S --> AUD\[AuditLogService]

```



\- \*\*Controllers\*\* handle HTTP and validation (`@Valid`).

\- \*\*Services\*\* hold business rules (ownership/permission checks, versioning, encryption).

\- \*\*Repositories\*\* are Spring Data JPA interfaces.

\- \*\*GlobalExceptionHandler\*\* converts exceptions into JSON `{"error": "..."}` responses.



\---



\## Project Structure



```

docuvault/

├── Dockerfile

├── pom.xml

└── src/main/

&#x20;   ├── java/com/docuvault/docuvault/

&#x20;   │   ├── DocuvaultApplication.java

&#x20;   │   ├── config/        SecurityConfig, OpenAPIConfig

&#x20;   │   ├── controller/    AuthController, DocumentController,

&#x20;   │   │                  PermissionController, ShareLinkController

&#x20;   │   ├── dto/           request/response objects (Auth, Document, Permission, ShareLink, User)

&#x20;   │   ├── entity/        User, Document, Version, Permission, AuditLog, ShareLink

&#x20;   │   ├── exception/     GlobalExceptionHandler

&#x20;   │   ├── repository/    JPA repositories

&#x20;   │   ├── security/      JwtAuthFilter

&#x20;   │   └── service/       AuthService, JwtService, DocumentService, PermissionService,

&#x20;   │                      ShareLinkService, EncryptionService, FileStorageService, AuditLogService

&#x20;   └── resources/

&#x20;       └── application.properties

```



\---



\## Database Design



Six tables are generated from the JPA entities:



| Table | Purpose |

|---|---|

| `users` | Accounts (name, unique email, BCrypt password hash, role `USER`/`ADMIN`) |

| `documents` | Document metadata, owner, pointer to the current version |

| `document\_versions` | One row per uploaded/restored version (file name, path, size, MIME type) |

| `permissions` | Per-document access for another user (`VIEWER` / `EDITOR`); unique per (document, user) |

| `share\_links` | Share tokens with optional expiry, password hash, one-time-use flag |

| `audit\_logs` | Action history (user, document, action, IP, timestamp) |



\### ER Diagram



```mermaid

erDiagram

&#x20;   USERS ||--o{ DOCUMENTS : "owns"

&#x20;   USERS ||--o{ PERMISSIONS : "is granted"

&#x20;   DOCUMENTS ||--o{ PERMISSIONS : "is shared via"

&#x20;   DOCUMENTS ||--o{ DOCUMENT\_VERSIONS : "has versions"

&#x20;   DOCUMENTS }o--o| DOCUMENT\_VERSIONS : "current version"

&#x20;   DOCUMENTS ||--o{ SHARE\_LINKS : "has share links"

&#x20;   USERS |o--o{ AUDIT\_LOGS : "performs"

&#x20;   DOCUMENTS |o--o{ AUDIT\_LOGS : "logged for"



&#x20;   USERS {

&#x20;       bigint id PK

&#x20;       string name

&#x20;       string email UK

&#x20;       string password

&#x20;       string role

&#x20;       datetime created\_at

&#x20;       datetime updated\_at

&#x20;   }

&#x20;   DOCUMENTS {

&#x20;       bigint id PK

&#x20;       string title

&#x20;       string description

&#x20;       bigint owner\_id FK

&#x20;       bigint current\_version\_id FK

&#x20;       datetime created\_at

&#x20;       datetime updated\_at

&#x20;   }

&#x20;   DOCUMENT\_VERSIONS {

&#x20;       bigint id PK

&#x20;       int version\_number

&#x20;       string file\_name

&#x20;       string file\_path

&#x20;       bigint file\_size

&#x20;       string mime\_type

&#x20;       bigint document\_id FK

&#x20;       datetime created\_at

&#x20;   }

&#x20;   PERMISSIONS {

&#x20;       bigint id PK

&#x20;       bigint document\_id FK

&#x20;       bigint user\_id FK

&#x20;       string permission\_type

&#x20;       datetime created\_at

&#x20;   }

&#x20;   SHARE\_LINKS {

&#x20;       bigint id PK

&#x20;       bigint document\_id FK

&#x20;       string token UK

&#x20;       datetime expires\_at

&#x20;       string password\_hash

&#x20;       boolean one\_time\_use

&#x20;       boolean used

&#x20;       datetime created\_at

&#x20;   }

&#x20;   AUDIT\_LOGS {

&#x20;       bigint id PK

&#x20;       bigint user\_id FK

&#x20;       bigint document\_id FK

&#x20;       string action

&#x20;       string ip\_address

&#x20;       datetime timestamp

&#x20;   }

```



\### Relationships (from the JPA mappings)



\- `Document.owner` → `User` (many-to-one, required)

\- `Document.currentVersion` → `Version` (many-to-one, optional)

\- `Version.document` → `Document` (many-to-one, required); unique on (`document\_id`, `version\_number`)

\- `Permission.document` / `Permission.user` → many-to-one, both required; unique on (`document\_id`, `user\_id`) — this is how Document ↔ User many-to-many sharing is modelled

\- `ShareLink.document` → `Document` (many-to-one, required)

\- `AuditLog.user` / `AuditLog.document` → many-to-one, both nullable (the document reference is cleared when a document is deleted so history is kept)



\---



\## Authentication \& Authorization



\### Registration \& login

\- `POST /api/auth/register` stores the user with a \*\*BCrypt\*\* password hash (minimum 8 characters, validated).

\- `POST /api/auth/login` verifies the password and returns an \*\*access token\*\* and a \*\*refresh token\*\*.



\### JWT flow



```mermaid

sequenceDiagram

&#x20;   participant C as Client

&#x20;   participant API as DocuVault API

&#x20;   C->>API: POST /api/auth/login (email, password)

&#x20;   API-->>C: accessToken (15 min) + refreshToken (7 days)

&#x20;   C->>API: Request + Authorization: Bearer accessToken

&#x20;   API->>API: JwtAuthFilter validates token, sets authenticated user

&#x20;   API-->>C: Response

&#x20;   C->>API: POST /api/auth/refresh (refreshToken)

&#x20;   API-->>C: new accessToken (same refreshToken returned)

```



\- Tokens are signed with an HMAC key derived from `JWT\_SECRET`. The subject is the user's email.

\- Sessions are \*\*stateless\*\*; CSRF is disabled (token-based API).

\- `JwtAuthFilter` reads the `Authorization: Bearer <token>` header; missing or invalid tokens leave the request unauthenticated, so protected endpoints reject it.



\### Authorization model

Authorization is \*\*per document\*\*, enforced in the service layer:

\- \*\*Owner\*\* – full control

\- \*\*EDITOR\*\* – can update the document and upload files, plus everything a viewer can do

\- \*\*VIEWER\*\* – can download the current version and specific versions



See the \[Access-Control Matrix](#access-control-matrix).



> The `User.role` field (`USER` / `ADMIN`) exists, but no endpoint currently uses it for authorization.



\---



\## File Encryption Flow



\*\*Upload\*\* (`POST /api/documents/{id}/upload`)



```mermaid

flowchart LR

&#x20;   A\[Original file] --> B\[Permission check: owner or EDITOR]

&#x20;   B --> C\["AES-256-GCM encrypt<br/>(random 12-byte IV prepended)"]

&#x20;   C --> D\["Write to uploads/ as<br/>UUID\_originalName"]

&#x20;   D --> E\[Create Version row: number, name, path, size, MIME type]

&#x20;   E --> F\[Set as document's current version]

&#x20;   F --> G\[Audit log: FILE\_UPLOADED]

```



\*\*Download\*\* (`GET /api/documents/{id}/download`)



```mermaid

flowchart LR

&#x20;   A\[Permission check: owner, EDITOR or VIEWER] --> B\[Read encrypted file from disk]

&#x20;   B --> C\["Split IV and ciphertext,<br/>AES-GCM decrypt + authenticate"]

&#x20;   C --> D\[Return original file with original name and MIME type]

&#x20;   D --> E\[Audit log: DOCUMENT\_DOWNLOADED]

```



Details:

\- Cipher: `AES/GCM/NoPadding`, 128-bit authentication tag, 12-byte random IV per file (stored as the first 12 bytes of the stored file).

\- The key comes from `DOCUVAULT\_ENCRYPTION\_KEY` (Base64-encoded 32-byte key). It is never stored in the database.

\- Files on disk are only ever the encrypted bytes; the database stores metadata and the file path.

\- GCM authenticates the data, so a wrong key or tampered file fails with `Decryption failed`.



\---



\## Versioning



\- First upload creates \*\*version 1\*\*; every later upload creates \*\*version N+1\*\* and becomes the current version.

\- `GET /api/documents/{id}/versions` lists all versions, newest first (owner only).

\- `GET /api/documents/{documentId}/versions/{versionId}/download` downloads any specific version (owner, EDITOR or VIEWER).

\- \*\*Restore\*\* (`POST .../versions/{versionId}/restore`, owner only) does not overwrite history: it creates a \*\*new version\*\* that points to the old version's stored file and makes it current.



\---



\## Sharing \& Permissions



\### Document permissions

The owner grants access with `POST /api/documents/{documentId}/permissions` using a `userId` and a `permissionType` (`VIEWER` or `EDITOR`). Calling it again for the same user updates their permission. Shared documents show up in `GET /api/documents` and in search results.



\### Share links

The owner creates a link with `POST /api/share/{documentId}`; all options are optional:



| Field | Meaning |

|---|---|

| `expiresAt` | Link stops working after this date-time |

| `password` | If set, it is stored as a BCrypt hash and must be supplied as `?password=` |

| `oneTimeUse` | Link can be accessed once; later attempts are rejected |



`GET /api/share/{token}` is public (no JWT). It validates the link and returns its metadata:



| Situation | Response |

|---|---|

| Unknown token | `404` Share link not found |

| Wrong or missing password | `401` Invalid share password |

| Expired, or one-time link already used | `410` Gone |



> The response contains the token, expiry and one-time flag only. The password hash is never returned. Streaming the actual file through a share link is not implemented yet (see \[Future Improvements](#future-improvements)).



\---



\## Audit Logging



Actions are saved to the `audit\_logs` table with user, document, action, IP address (`request.getRemoteAddr()`) and timestamp.



| Action | Logged when |

|---|---|

| `DOCUMENT\_CREATED` | Document created |

| `DOCUMENT\_UPDATED` | Title/description updated |

| `DOCUMENT\_DELETED` | Document deleted (stored without a document reference) |

| `FILE\_UPLOADED` | File uploaded |

| `DOCUMENT\_DOWNLOADED` | Current version downloaded |

| `VERSION\_DOWNLOADED` | A specific version downloaded |

| `VERSION\_RESTORED` | A version restored |



Notes: there is currently no API endpoint to read audit logs (query the table directly), and the IP is not recorded for update and upload actions. Behind a proxy such as Render, `getRemoteAddr()` may show the proxy's address.



\---



\## Search



`GET /api/documents/search?title=<text>` performs a case-insensitive "contains" match on the document title and returns only documents the user \*\*owns\*\* or has been \*\*given a permission\*\* on.



\---



\## API Endpoints



Base URL (local): `http://localhost:8080`



\### Auth

| Method | Endpoint | Auth | Purpose | Request |

|---|---|---|---|---|

| POST | `/api/auth/register` | No | Register a user | JSON: `name`, `email`, `password` (min 8 chars) |

| POST | `/api/auth/login` | No | Get access + refresh tokens | JSON: `email`, `password` |

| POST | `/api/auth/refresh` | No | Get a new access token | JSON: `refreshToken` |



\### Documents

| Method | Endpoint | Auth | Purpose | Request |

|---|---|---|---|---|

| POST | `/api/documents` | JWT | Create a document | JSON: `title` (required), `description` |

| GET | `/api/documents` | JWT | List owned + shared documents | — |

| GET | `/api/documents/search` | JWT | Search accessible documents by title | Query: `title` |

| PUT | `/api/documents/{id}` | JWT | Update title/description (owner/EDITOR) | JSON: `title`, `description` |

| DELETE | `/api/documents/{id}` | JWT | Delete document (owner only) | — |

| POST | `/api/documents/{id}/upload` | JWT | Upload + encrypt a file (owner/EDITOR) | `multipart/form-data`, field `file` |

| GET | `/api/documents/{id}/download` | JWT | Download + decrypt current version (owner/EDITOR/VIEWER) | — |

| GET | `/api/documents/{id}/versions` | JWT | List version history (owner only) | — |

| GET | `/api/documents/{documentId}/versions/{versionId}/download` | JWT | Download a specific version | — |

| POST | `/api/documents/{documentId}/versions/{versionId}/restore` | JWT | Restore a version (owner only) | — |



\### Permissions

| Method | Endpoint | Auth | Purpose | Request |

|---|---|---|---|---|

| POST | `/api/documents/{documentId}/permissions` | JWT | Grant/update access (owner only) | JSON: `userId`, `permissionType` (`VIEWER` or `EDITOR`) |



\### Share links

| Method | Endpoint | Auth | Purpose | Request |

|---|---|---|---|---|

| POST | `/api/share/{documentId}` | JWT | Create a share link (owner only) | JSON: `expiresAt`, `password`, `oneTimeUse` (all optional) |

| GET | `/api/share/{token}` | No | Validate/access a share link | Query: `password` (if the link has one) |



\### Errors

Errors are returned as JSON, for example `{"error": "Share link not found"}`. Share-link errors use `404`, `401`, `410` and `403`; other business-rule errors (e.g. "Document not found", missing permission) return `400`.



\---



\## Swagger / OpenAPI



\- \*\*Local:\*\* http://localhost:8080/swagger-ui.html

\- \*\*Deployed:\*\* https://docuvault-q8o1.onrender.com/swagger-ui.html

\- \*\*OpenAPI JSON:\*\* `/v3/api-docs`



To call protected endpoints from Swagger:

1\. Call `POST /api/auth/register`, then `POST /api/auth/login` and copy the `accessToken`.

2\. Click \*\*Authorize\*\* (top right), paste the token (the `Bearer` scheme is already configured) and confirm.

3\. Use \*\*Try it out\*\* on any endpoint. The access token expires after 15 minutes; log in again or use `/api/auth/refresh`.



\---



\## Local Setup



\*\*Prerequisites:\*\* Java 21, PostgreSQL, Git (the Maven Wrapper is included).



```bash

git clone https://github.com/<your-username>/<your-repo>.git

cd <your-repo>

```



1\. \*\*Create the database\*\*

```sql

&#x20;  CREATE DATABASE docuvault;

```

2\. \*\*Set the environment variables\*\* (see the next section).

3\. \*\*Run the app\*\*

```bash

&#x20;  # Linux / macOS / Git Bash

&#x20;  ./mvnw spring-boot:run



&#x20;  # Windows PowerShell

&#x20;  .\\mvnw.cmd spring-boot:run

```

4\. Open http://localhost:8080/swagger-ui.html



Tables are created automatically (`spring.jpa.hibernate.ddl-auto=update`). Uploaded (encrypted) files are written to an `uploads/` folder in the working directory, so keep `uploads/` out of Git.



\---



\## Environment Variables



Secrets are \*\*never\*\* stored in the repository; `application.properties` reads them from the environment.



| Variable | Required | Description |

|---|---|---|

| `DB\_URL` | No | JDBC URL. Default: `jdbc:postgresql://localhost:5432/docuvault` |

| `DB\_USERNAME` | Yes | PostgreSQL username |

| `DB\_PASSWORD` | Yes | PostgreSQL password |

| `JWT\_SECRET` | Yes | Long random string used to sign JWTs (at least 32 characters; 64+ recommended) |

| `DOCUVAULT\_ENCRYPTION\_KEY` | Yes | \*\*Base64-encoded 32-byte\*\* AES-256 key |

| `PORT` | No | HTTP port. Default `8080` (Render sets this automatically) |



Generate secrets:



```bash

openssl rand -base64 32   # DOCUVAULT\_ENCRYPTION\_KEY

openssl rand -base64 64   # JWT\_SECRET

```



PowerShell example for setting variables for the current session:



```powershell

$env:DB\_USERNAME = "<db-username>"

$env:DB\_PASSWORD = "<db-password>"

$env:JWT\_SECRET = "<random-secret>"

$env:DOCUVAULT\_ENCRYPTION\_KEY = "<base64-32-byte-key>"

```



> ⚠️ Keep the encryption key safe. If it is lost or changed, previously uploaded files can no longer be decrypted.



\---



\## Docker



The `Dockerfile` builds the JAR inside the image (`eclipse-temurin:21-jdk`) with the Maven Wrapper and runs it on port 8080.



```bash

\# Build

docker build -t docuvault .



\# Run (replace the placeholders)

docker run -d --name docuvault -p 8080:8080 \\

&#x20; -e DB\_URL="jdbc:postgresql://host.docker.internal:5432/docuvault" \\

&#x20; -e DB\_USERNAME="<db-username>" \\

&#x20; -e DB\_PASSWORD="<db-password>" \\

&#x20; -e JWT\_SECRET="<random-secret>" \\

&#x20; -e DOCUVAULT\_ENCRYPTION\_KEY="<base64-32-byte-key>" \\

&#x20; docuvault



\# Optional: keep uploaded files across container restarts

\#   add:  -v docuvault-uploads:/app/uploads

```



On Windows PowerShell, put the `docker run` command on a single line (or use backticks instead of `\\`). `host.docker.internal` lets the container reach a PostgreSQL instance running on your host machine.



Check logs with `docker logs docuvault`, then open http://localhost:8080/swagger-ui.html.



\---



\## Deployment



The backend is deployed on \*\*Render\*\* as a Docker Web Service, connected to a Render PostgreSQL database in the same region.



\- Render builds the image from the repository's `Dockerfile`.

\- Configuration is supplied through Render \*\*Environment Variables\*\* (`DB\_URL`, `DB\_USERNAME`, `DB\_PASSWORD`, `JWT\_SECRET`, `DOCUVAULT\_ENCRYPTION\_KEY`); `PORT` is provided by Render.

\- Use the database's \*\*internal\*\* hostname in `DB\_URL`.



\*\*Free-plan limitations\*\*

\- The service sleeps after inactivity, so the first request can be slow.

\- The instance's disk is ephemeral: encrypted files in `uploads/` are lost on restart or redeploy, while database records remain. For production, store files on persistent disk or object storage.



\---



\## Testing



\- \*\*Manual API testing:\*\* done with Postman and Swagger UI (auth, documents, upload/download, versions, permissions, share links, error cases).

\- \*\*Automated tests:\*\* the project includes Spring Boot test dependencies and JaCoCo, but automated test coverage is currently minimal.

\- To run the Maven tests, the same environment variables must be set in the terminal first:



```bash

./mvnw test          # Windows: .\\mvnw.cmd test

```



A JaCoCo coverage report is generated at `target/site/jacoco/index.html` after the tests run.



\---



\## Access-Control Matrix



| Action | Owner | EDITOR | VIEWER | Other authenticated user |

|---|:---:|:---:|:---:|:---:|

| Create a document | ✅ | — | — | ✅ (becomes owner) |

| See document in list / search | ✅ | ✅ | ✅ | ❌ |

| Update title / description | ✅ | ✅ | ❌ | ❌ |

| Upload a file (new version) | ✅ | ✅ | ❌ | ❌ |

| Download current version | ✅ | ✅ | ✅ | ❌ |

| Download a specific version | ✅ | ✅ | ✅ | ❌ |

| View version history | ✅ | ❌ | ❌ | ❌ |

| Restore a version | ✅ | ❌ | ❌ | ❌ |

| Delete the document | ✅ | ❌ | ❌ | ❌ |

| Assign permissions | ✅ | ❌ | ❌ | ❌ |

| Create a share link | ✅ | ❌ | ❌ | ❌ |

| Access a share link | Anyone with the token (and password, if set) |



\---



\## Security Considerations



\*\*Implemented\*\*

\- BCrypt for user passwords and for share-link passwords

\- Stateless JWT authentication with short-lived access tokens

\- AES-256-GCM encryption at rest with a random IV per file and authenticated decryption

\- Ownership / permission checks in the service layer for every document operation

\- Secrets (database credentials, JWT secret, encryption key) loaded from environment variables

\- Share-link responses never expose the password hash

\- Bean Validation on request bodies; consistent JSON error responses



\*\*Known limitations\*\*

\- Refresh and access tokens are validated the same way (no token-type claim), and refresh tokens are not rotated or revocable.

\- Files are encrypted and decrypted fully in memory, so very large files are not supported efficiently.

\- Swagger UI and `/v3/api-docs` are public; consider disabling them in production (`springdoc.swagger-ui.enabled=false`, `springdoc.api-docs.enabled=false`).

\- Default Spring Boot upload size limits apply; no rate limiting is configured.



\---



\## Future Improvements



\- Serve the document through a valid share link (download via token)

\- Token-type claims, refresh-token rotation and revocation

\- Endpoint to view audit logs, plus audit entries for logins, permission changes and share links

\- Persistent / object storage (e.g. S3-compatible) for files

\- Streaming encryption for large files; pagination for list and search

\- Use the `ADMIN` role for admin-only operations

\- Unit and integration tests for services and security rules

\- Rate limiting and account lockout



\---



\## Author



\*\*Swastika\*\*

B.Tech Information Technology, KNIT Sultanpur



\- GitHub: https://github.com/swastika-gupta26

\- LinkedIn: https://www.linkedin.com/in/swastika-gupta-4ba45932b/

