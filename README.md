# ⚽ Sistema de Reservas de Canchas de Fútbol

Aplicación web full-stack para gestionar reservas de canchas de fútbol con pago de seña online (MercadoPago), pago presencial del saldo con código de retiro, envío de emails de confirmación y panel de administración con estadísticas.

---

## 🛠️ Stack tecnológico

### Backend
- **Java 21** + **Spring Boot 4.0**
- **Spring Security** (JWT + OAuth2 Google)
- **Spring Data JPA** + **Hibernate**
- **MySQL 8**
- **Spring Mail** (Gmail SMTP)
- **MercadoPago SDK 2.5.0** (Checkout Pro)
- **Maven**

### Frontend
- **React 18** + **Vite**
- **React Router v6**
- **Tailwind CSS**
- **Axios**
- **react-google-charts** (dashboard de estadísticas)

### Infraestructura
- **Docker** + **Docker Compose** (3 servicios: mysql, backend, frontend/nginx)
- **ngrok** para exponer webhooks de MercadoPago en desarrollo

---

## ✨ Funcionalidades

### Para usuarios
- Registro e inicio de sesión con email/contraseña o **Google OAuth2**
- Ver canchas disponibles con precio y tipo (Fútbol 5, 7, 11)
- Reservar turno en una fecha específica
- Pago de seña online con **MercadoPago** (tarjeta, efectivo, etc.)
- Código de retiro de 6 dígitos generado automáticamente al confirmar la seña
- Email automático con el código al pagar la seña
- Panel "Mis Reservas" con estado y código visible
- Política de cancelación con checkbox obligatorio (seña no reembolsable)

### Para el admin
- CRUD de **canchas** (nombre, tipo, precio, porcentaje de seña)
- CRUD de **turnos** (bulk: generar rangos horarios)
- Gestión de **días cerrados** (feriados, mantenimiento)
- Listado de **reservas** con búsqueda por email/nombre/código
- Cobrar el **saldo restante** generando un QR de MercadoPago en el momento
- Dashboard de **estadísticas** con métricas y 5 gráficos:
  - Reservas por estado
  - Reservas por cancha
  - Ingresos por cancha
  - Reservas por día (últimos 30 días)
  - Turnos más populares

### Sistema de pagos
- **Seña** online al reservar (% configurable por cancha)
- Expiración automática de reservas no pagadas (10 minutos)
- **Saldo** cobrable presencialmente en la cancha vía QR de MercadoPago
- Webhook de MercadoPago diferencia seña vs saldo por `externalReference`
- Validación de firma del webhook (opcional, configurable)

---

## 📋 Requisitos previos

- **Docker** y **Docker Compose** instalados
- Cuenta en **Google Cloud Console** para OAuth2 ([Instrucciones](https://console.cloud.google.com/))
- Cuenta en **MercadoPago Developers** para tokens de prueba o producción ([Panel](https://www.mercadopago.com.ar/developers/panel))
- (Opcional) Cuenta de **Gmail** con 2FA y App Password para envío de emails
- (Opcional) **ngrok** si querés recibir webhooks de MercadoPago en desarrollo local

---

## 🚀 Cómo levantarlo

### 1. Clonar el repositorio

```bash
git clone https://github.com/jara96/sistema-canchas-futbol.git
cd sistema-canchas-futbol
```

### 2. Configurar variables de entorno

Copiá el archivo de ejemplo y editalo con tus valores reales:

```bash
cp .env.example .env
```

Editá `.env` y completá:

| Variable | Descripción |
|---|---|
| `DB_PASSWORD` | Contraseña de MySQL |
| `JWT_SECRET` | Secreto aleatorio de al menos 32 bytes |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Credenciales OAuth2 de Google |
| `OAUTH2_REDIRECT_URI` | URL de redirect (ej: `http://localhost/oauth2/redirect`) |
| `CORS_ORIGINS` | Orígenes permitidos, separados por coma |
| `MP_ACCESS_TOKEN` / `MP_PUBLIC_KEY` | Tokens de MercadoPago (TEST o PROD) |
| `MP_WEBHOOK_URL` | URL pública donde llegan los webhooks (usar ngrok en dev) |
| `MAIL_ENABLED` | `true` para habilitar envío de emails |
| `MAIL_USER` / `MAIL_PASS` | Email y **App Password** de Gmail |

### 3. Levantar con Docker Compose

```bash
docker compose up -d --build
```

Esto levanta:
- **MySQL** en `localhost:3306`
- **Backend** en `http://localhost:8080`
- **Frontend** en `http://localhost` (puerto 80, servido por nginx)

### 4. Acceder

- App: http://localhost
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html

### 5. Usuario admin inicial

Se crea automáticamente al iniciar el backend:
- **Email:** `admin@tucancha.com`
- **Password:** `admin123`

> ⚠️ Cambiar esta contraseña antes de ir a producción.

---

## 📡 Webhooks de MercadoPago (desarrollo)

Para que MercadoPago pueda notificar los pagos a tu backend local:

```bash
ngrok http 8080
```

Copiá la URL HTTPS que te da ngrok y ponela en `.env`:

```env
MP_WEBHOOK_URL=https://xxxx-xx-xx-xx-xx.ngrok-free.app/api/pagos/webhook
OAUTH2_REDIRECT_URI=https://xxxx-xx-xx-xx-xx.ngrok-free.app/oauth2/redirect
CORS_ORIGINS=http://localhost,http://localhost:5173,https://xxxx-xx-xx-xx-xx.ngrok-free.app
```

Después reiniciá los containers:

```bash
docker compose up -d --build
```

En el panel de MercadoPago → **Webhooks**, configurá la misma URL y suscribite a eventos de `payment`.

---

## 📧 Email con Gmail

1. Activá la **verificación en 2 pasos** en tu cuenta de Google
2. Creá una **App Password** en https://myaccount.google.com/apppasswords
3. Ponela en `.env`:

```env
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=tucorreo@gmail.com
MAIL_PASS=xxxxxxxxxxxxxxxx
MAIL_FROM=tucorreo@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

Si `MAIL_ENABLED=false`, el sistema funciona igual pero los emails quedan desactivados (se loguea y sigue).

---

## 🗂️ Estructura del proyecto

```
.
├── backend/                  # Spring Boot API
│   ├── src/main/java/com/tucancha/backend/
│   │   ├── config/           # DataInitializer, MercadoPago, OpenAPI, Scheduler
│   │   ├── controller/       # REST endpoints
│   │   ├── dto/              # Request/Response DTOs
│   │   ├── entity/           # Entidades JPA
│   │   ├── enums/
│   │   ├── exception/        # Manejo global de errores
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── security/         # JWT, OAuth2, UserDetails
│   │   └── service/          # Lógica de negocio
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                 # React SPA
│   ├── src/
│   │   ├── api/              # cliente axios
│   │   ├── components/       # Navbar, PrivateRoute
│   │   ├── context/          # AuthContext
│   │   └── pages/            # Login, Register, Canchas, Reservar, MisReservas, Admin
│   ├── Dockerfile            # multi-stage: vite build + nginx
│   ├── nginx.conf
│   └── package.json
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## 🧪 Tests del backend

```bash
cd backend
./mvnw test
```

Incluye tests unitarios de:
- `AuthService`
- `ReservaService`
- `MercadoPagoSignatureValidator`

---

## 🔒 Seguridad

- Contraseñas hasheadas con **BCrypt**
- Autenticación **JWT** con expiración configurable
- **OAuth2 Google** para login social
- Endpoints admin protegidos con `@PreAuthorize("hasRole('ADMIN')")`
- **CORS** configurable por variables de entorno
- Validación opcional de **firma HMAC** en webhooks de MercadoPago
- Archivos con secretos (`.env`) excluidos del repo vía `.gitignore`

---

## 📝 Licencia

Uso educativo / personal. No incluye licencia explícita.

---

## 👤 Autor

Desarrollado por [@jara96](https://github.com/jara96)
