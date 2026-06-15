# LinkZen — Backend

> Production-ready REST API for a URL shortener platform built with Spring Boot 3, MongoDB, Redis, and JWT authentication.

🌐 **Live API:** [linkzen-backend-2.onrender.com](https://linkzen-backend-2.onrender.com)  
🔗 **Frontend Repo:** [LinkZen Frontend](https://github.com)

---

## Features

- 🔐 JWT authentication with Spring Security (stateless)
- 🔗 URL shortening with random or custom alias
- ⏰ URL expiration with configurable days
- 📊 Click analytics — browser, OS, device, country, referrer
- 🌍 IP geolocation via ip-api.com
- ⚡ Redis caching for fast redirects (~1ms cache hits)
- 📷 QR code generation with ZXing
- 🗑️ URL delete with ownership validation
- ✏️ Expiry update with ownership validation
- 🛡️ Global exception handling with structured JSON responses
- ✅ Request validation with Jakarta Bean Validation

---

## Tech Stack

| Tech | Purpose |
|------|---------|
| Java 17 | Language |
| Spring Boot 3.5 | Framework |
| Spring Security | Auth & route protection |
| Spring Data MongoDB | Database ORM |
| Spring Data Redis | Caching layer |
| JWT (jjwt 0.11.5) | Token generation & validation |
| BCrypt | Password hashing |
| ZXing 3.5.3 | QR code generation |
| Lombok | Boilerplate reduction |
| Maven | Build tool |
| Docker | Containerization |

---

## Architecture

```
Controller → Service → Repository
```

```
src/main/java/com/pratik/urlshortener/
├── controller/
│   ├── AuthController.java        # /api/v1/auth
│   ├── UrlController.java         # /api/v1/url
│   └── RedirectController.java    # /{shortCode}
├── service/
│   ├── AuthService.java
│   ├── UrlService.java
│   └── JwtService.java
├── repository/
│   ├── UrlRepository.java
│   ├── UrlClickRepository.java
│   └── UserRepository.java
├── model/
│   ├── Url.java
│   ├── UrlClick.java
│   └── User.java
├── dto/
│   ├── ShortenUrlRequest.java
│   ├── ShortenUrlResponse.java
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── SignupRequest.java
│   ├── UrlStatsResponse.java
│   ├── UrlAnalyticsResponse.java
│   ├── AnalyticsResponse.java
│   └── UpdateExpiryRequest.java
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── WebConfig.java
│   └── RedisConfig.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── CustomAliasAlreadyExistsException.java
│   └── UrlExpiredException.java
└── UrlshortenerApplication.java
```

---

## REST API Reference

### Auth — `/api/v1/auth` (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/signup` | Register new user |
| POST | `/login` | Login, returns JWT token |

**Signup request:**
```json
{
  "name": "Pratik",
  "email": "pratik@gmail.com",
  "password": "password123"
}
```

**Login response:**
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### URL — `/api/v1/url` (🔒 JWT required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/shorten` | Create short URL |
| GET | `/all` | Get all URLs for user |
| GET | `/stats/:shortCode` | Get URL stats |
| PUT | `/:shortCode/expiry` | Update expiry days |
| DELETE | `/:shortCode` | Delete URL |
| GET | `/qr/:shortCode` | Generate QR code PNG (public) |
| GET | `/analytics` | Dashboard analytics |
| GET | `/analytics/:shortCode` | Per-URL analytics |

**Create short URL request:**
```json
{
  "originalUrl": "https://google.com",
  "customAlias": "google",
  "expiryInDays": 7
}
```

---

### Redirect — public

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/:shortCode` | Redirect + track click analytics |

---

## Data Models

### Url
```java
String id
String originalUrl
String shortCode
Long clickCount
LocalDateTime createdAt
LocalDateTime expiresAt
String userEmail
boolean expired  // computed — not stored in DB
```

### UrlClick
```java
String id
String shortCode
String ipAddress
String browser
String deviceType
String operatingSystem
String country
String referer
LocalDateTime clickedAt
String userEmail
```

### User
```java
String id
String name
String email
String password  // BCrypt hash
```

---

## Redirect Lifecycle

```
User clicks short URL
        ↓
RedirectController receives GET /{shortCode}
        ↓
Redis cache checked → hit? return in ~1ms
        ↓ miss
MongoDB queried → save result to Redis
        ↓
Expiry check → expired? redirect to /expired-link
        ↓
UrlClick record saved (browser, OS, device, country)
        ↓
clickCount++ in MongoDB + Redis
        ↓
HTTP 302 → original URL
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB Atlas account
- Redis Cloud account

### Environment Variables

Set these in your environment or Render dashboard:

```properties
SPRING_DATA_MONGODB_URI=mongodb+srv://...
SPRING_DATA_REDIS_HOST=redis-...
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your_password
```

### Run Locally

```bash
# Clone the repo
git clone https://github.com/your-username/linkzen-backend.git
cd linkzen-backend

# Build
mvn clean install

# Run
mvn spring-boot:run
```

### Docker

```bash
# Build image
docker build -t linkzen-backend .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI=your_uri \
  -e SPRING_DATA_REDIS_HOST=your_host \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e SPRING_DATA_REDIS_PASSWORD=your_password \
  linkzen-backend
```

---

## Deployment

Deployed on **Render** using the included `Dockerfile`.  
Environment variables configured via Render dashboard secrets.

---

## Security

- Passwords hashed with BCrypt
- JWT tokens signed with HMAC-SHA256
- Stateless authentication — no server-side sessions
- All URL operations validate ownership via email from JWT
- CORS configured via `WebConfig.java`
