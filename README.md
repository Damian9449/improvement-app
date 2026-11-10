# Improvement App

Improvement App to aplikacja webowa wspierająca codzienne nawyki: zarządzanie dietą i produktami spożywczymi, planowanie treningów, śledzenie finansów (w tym kryptowalut), listy zakupów oraz zadania okresowe. Składa się z backendu (Spring Boot + Java 21) oraz frontendu (React 19 + Material UI).

## Szybki start

Najprostsza ścieżka uruchomienia całości (wymaga Dockera):

```bash
git clone https://github.com/damianPolchlopek/improvement-app.git
cd improvement-app
cp Backend/.env.example Backend/.env   # uzupełnij sekrety
docker-compose up --build
```

Po starcie: frontend → http://localhost:3000, backend → http://localhost:8080
(Swagger UI: http://localhost:8080/swagger-ui.html).

## Spis treści
- [Szybki start](#szybki-start)
- [Architektura](#architektura)
- [Backend](#backend)
  - [Opis funkcjonalny](#opis-funkcjonalny)
  - [Architektura i technologie](#architektura-i-technologie)
  - [Konfiguracja i zmienne środowiskowe](#konfiguracja-i-zmienne-środowiskowe)
  - [Budowanie i uruchamianie](#budowanie-i-uruchamianie)
- [Frontend](#frontend)
  - [Opis funkcjonalny](#opis-funkcjonalny-1)
  - [Architektura i technologie](#architektura-i-technologie-1)
  - [Konfiguracja i uruchomienie](#konfiguracja-i-uruchomienie)
- [Uruchomienie całości przez Docker Compose](#uruchomienie-całości-przez-docker-compose)
- [CI/CD](#cicd)

---

## Architektura

```
improvement-app/
├── Backend/            # Spring Boot 3.3.4, Java 21 (REST API, WebSocket)
├── frontend5/          # React 19 + Material UI (Vite)
├── StartupScripts/     # Skrypty inicjalizujące bazę (postgres/)
├── docker-compose.yml  # Uruchomienie całego stacku (backend, frontend, Postgres, pgAdmin)
└── .github/workflows/  # GitHub Actions – build i deploy
```

Backend przechowuje wszystkie dane w **PostgreSQL** (JPA/Hibernate, schemat zarządzany przez Liquibase). Szczegóły każdego modułu opisano w [Backend/README.md](Backend/README.md) i [frontend5/README.md](frontend5/README.md).

---

## Backend

### Opis funkcjonalny

Backend obsługuje całą logikę biznesową, autoryzację, dostęp do baz danych oraz komunikację w czasie rzeczywistym (WebSocket). Kluczowe funkcjonalności:

- **Żywienie** – produkty spożywcze, posiłki, przepisy, dzienne podsumowania diety i wyliczanie makroskładników (moduł `food`, architektura heksagonalna)
- **Treningi** – ćwiczenia, plany i szablony treningowe, statystyki, rekordy (moduł `workouts`)
- **Finanse** – śledzenie wydatków oraz notowań kryptowalut (integracja z CoinMarketCap, moduł `finance`)
- **Zakupy** – listy zakupowe (moduł `shopping`)
- **Zadania okresowe** – zadania dzienne i tygodniowe (moduł `other`)
- **Parser** – przechowywanie i parsowanie danych o technologiach (moduł `parser`)
- **Integracja z Google Drive** – import/eksport plików (moduł `googledrive`)
- **Audyt** – historia zmian encji oparta o Hibernate Envers (moduł `audit`)
- **Bezpieczeństwo** – uwierzytelnianie JWT (access + refresh token w cookie), logowanie przez Google (OAuth2), weryfikacja e-mail, role użytkowników, rate limiting prób logowania i żądań HTTP (Bucket4j)
- **Monitoring** – endpointy Actuatora oraz metryki Prometheus

### Architektura i technologie

- **Java 21 + Spring Boot 3.3.4** – Security, Web, Data JPA, Jersey, WebSocket, Mail, OAuth2 Client, Actuator
- **Baza danych** – PostgreSQL (JPA/Hibernate, pula połączeń HikariCP)
- **Liquibase** – migracje schematu bazy (`src/main/resources/db/changelog`)
- **Hibernate Envers** – audyt zmian encji
- **Springdoc OpenAPI** – dokumentacja REST (Swagger UI pod `/swagger-ui.html`)
- **Bezpieczeństwo** – JWT (jjwt), BCrypt, OAuth2 (Google)
- **Apache POI / Jsoup** – obsługa plików Excel oraz parsowanie HTML
- **Log4j2 + Micrometer / Prometheus** – logowanie i metryki
- **Maven** – budowanie i zarządzanie zależnościami (wrapper `mvnw`)
- **Testy** – JUnit, Spring Security Test, Testcontainers (PostgreSQL), REST Assured, Datafaker

### Konfiguracja i zmienne środowiskowe

Konfiguracja znajduje się w `Backend/src/main/resources/application.yaml` oraz w profilach: `dev` (domyślny), `compose` (Docker Compose) i `smallhost` (produkcja). Profil wybiera się parametrem `--spring.profiles.active=<profil>`.

Sekrety wstrzykiwane są przez zmienne środowiskowe. Lokalnie skopiuj [`Backend/.env.example`](Backend/.env.example) do `Backend/.env` i uzupełnij wartości (`.env` jest w `.gitignore`). Wymagane zmienne (poufne – nie commituj ich):

| Zmienna | Opis |
| --- | --- |
| `JWT_SECRET` | Sekret do podpisywania tokenów JWT |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Dane logowania OAuth2 (Google) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Konto SMTP (Gmail) do wysyłki e-maili |
| `COINMARKETCAP_API_KEY` | Klucz API do notowań kryptowalut |
| `DB_USERNAME`, `DB_PASSWORD` | Dane dostępowe do PostgreSQL |
| `SSL_KEYSTORE_PASSWORD` | Hasło keystore (HTTPS, profil produkcyjny) |

### Budowanie i uruchamianie

**Lokalnie** (z katalogu `Backend/`):
```bash
cd Backend
./mvnw package
java -jar target/improvement-app-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8080
```

**Docker** (z katalogu `Backend/`):
```bash
docker build -t improvement-app-backend .
docker run -p 8080:8080 improvement-app-backend
```

**Healthcheck**: endpoint zdrowia dostępny pod `/actuator/health` (eksponowane także `info`, `metrics`, `prometheus`).

---

## Frontend

### Opis funkcjonalny

Frontend to aplikacja React (Create React App). Pozwala użytkownikowi:
- Przeglądać, dodawać i filtrować produkty spożywcze oraz posiłki
- Generować i analizować dzienne podsumowania diety oraz statystyki
- Planować treningi i korzystać z szablonów treningowych
- Śledzić finanse i notowania kryptowalut
- Zarządzać listami zakupów oraz zadaniami okresowymi
- Logować się (JWT lub Google OAuth2) i zarządzać profilem
- Otrzymywać dane w czasie rzeczywistym przez WebSocket

### Architektura i technologie

- **React 19** + **Vite** (build i dev server) + **Vitest** (testy)
- **Material UI v7** (`@mui/material`, `@mui/icons-material`, `@mui/lab`, `@mui/x-data-grid`, `@mui/x-date-pickers`)
- **React Router** (`react-router-dom`) – routing
- **React Query** (`@tanstack/react-query`) – pobieranie danych i cache
- **Axios** (REST), **universal-cookie**, **i18next** / **react-i18next** (wielojęzyczność)
- **Recharts** – wykresy i statystyki
- **WebSocket** (`@stomp/stompjs`, `sockjs-client`, `stompjs`)
- **jwt-decode**, **dayjs**, **moment** – obsługa tokenów i dat
- **Prettier** – formatowanie kodu

Pełna lista zależności w [frontend5/package.json](frontend5/package.json).

### Konfiguracja i uruchomienie

**Zmienne środowiskowe:**
- `.env.development`
  ```
  VITE_API_URL=http://localhost:8080/
  ```
- `.env.production`
  ```
  VITE_API_URL=https://grindtrack.pl:37786/
  ```

**Instalacja i uruchomienie** (z katalogu `frontend5/`):
```bash
cd frontend5
npm install
npm start       # tryb developerski, Vite (http://localhost:3000)
npm run build   # build produkcyjny (do katalogu dist/)
```

---

## Uruchomienie całości przez Docker Compose

Z katalogu głównego repozytorium:
```bash
docker-compose up --build
```

Compose uruchamia: backend (port 8080, profil `compose`), frontend (port 3000), PostgreSQL (5432) oraz pgAdmin (5050). Skrypt inicjalizujący bazę znajdują się w `StartupScripts/`.

---

## CI/CD

Deploy realizowany jest przez **GitHub Actions** ([.github/workflows/main.yml](.github/workflows/main.yml)): po pushu na gałąź `main` budowany jest backend (Maven) i frontend (npm), a artefakty wgrywane są przez SSH na serwer produkcyjny. Sekrety (klucze API, dane SMTP, JWT itd.) przechowywane są w GitHub Secrets.

---

**Kod źródłowy:**
- [Backend w GitHub](https://github.com/damianPolchlopek/improvement-app/tree/main/Backend)
- [Frontend w GitHub](https://github.com/damianPolchlopek/improvement-app/tree/main/frontend5)
