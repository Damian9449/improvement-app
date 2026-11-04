# Improvement App – Backend

## Spis treści

1. [Wstęp](#wstęp)
2. [Technologie](#technologie)
3. [Struktura katalogów](#struktura-katalogów)
4. [Opis architektury i domen](#opis-architektury-i-domen)
5. [Przegląd modułów](#przegląd-modułów)
6. [Bazy danych i migracje](#bazy-danych-i-migracje)
7. [Konfiguracja i profile](#konfiguracja-i-profile)
8. [Dokumentacja API](#dokumentacja-api)
9. [Obsługa wyjątków](#obsługa-wyjątków)
10. [Testy](#testy)
11. [Budowanie i uruchamianie](#budowanie-i-uruchamianie)

---

## Wstęp

Backend Improvement App to modularna aplikacja napisana w Javie 17 z użyciem Spring Boot 3.3.4. Każda domena funkcjonalna posiada własny pakiet i podział na warstwy, co ułatwia rozwój, testowanie i utrzymanie kodu. Moduł `food` został dodatkowo zorganizowany według architektury heksagonalnej (porty i adaptery).

---

## Technologie

- **Java 17 + Spring Boot 3.3.4** – Security, Web, Data JPA, Jersey, WebSocket, Mail, OAuth2 Client, Actuator
- **PostgreSQL** (JPA/Hibernate, pula HikariCP)
- **Liquibase** – migracje schematu bazy
- **Hibernate Envers** – audyt zmian encji
- **Springdoc OpenAPI** – Swagger UI (`/swagger-ui.html`)
- **JWT (jjwt)**, **BCrypt**, **OAuth2 (Google)** – uwierzytelnianie i autoryzacja
- **Apache POI** (Excel), **Jsoup** (HTML)
- **Log4j2**, **Micrometer / Prometheus** – logowanie i metryki
- **Lombok** – ograniczenie boilerplate
- **Maven** (wrapper `mvnw`) – budowanie
- **Testy** – JUnit, Spring Security Test, Testcontainers (PostgreSQL), REST Assured, Datafaker

---

## Struktura katalogów

Kod źródłowy znajduje się w pakiecie `com.improvement_app`, podzielonym na moduły domenowe:

```
com/improvement_app/
├── ImprovementAppBackendApplication.java   # punkt wejścia
├── food/          # Żywienie – produkty, posiłki, przepisy, diety (architektura heksagonalna)
├── workouts/      # Treningi – ćwiczenia, plany, szablony, statystyki
├── finance/       # Finanse i notowania kryptowalut (CoinMarketCap)
├── shopping/      # Listy zakupów
├── other/         # Zadania okresowe (dzienne / tygodniowe)
├── parser/        # Parsowanie i przechowywanie danych o technologiach
├── googledrive/   # Integracja z Google Drive
├── security/      # Uwierzytelnianie i autoryzacja (JWT, OAuth2, rate limiting)
├── audit/         # Audyt zmian encji (Hibernate Envers)
├── common/        # Współdzielone konfiguracje i kontrolery
├── exceptions/    # Globalna obsługa wyjątków i walidacji
└── util/          # Klasy pomocnicze (paginacja, odpowiedzi listowe)
```

> Drzewo celowo zatrzymano na poziomie modułów — wewnętrzny podział na warstwy
> (`controller`, `service`, `repository`, `entity`, …) opisano słownie poniżej, dzięki czemu
> dokumentacja nie dezaktualizuje się przy każdym refaktorze plików.

---

## Opis architektury i domen

Aplikacja oparta jest na architekturze modularnej, w której każda domena wydzielona jest w osobny pakiet. Domeny stosują typowy podział na warstwy:

- **controller / controllers** – obsługa żądań HTTP (REST API)
- **entity** – encje (mapowanie na bazę danych)
- **repository** – interfejsy repozytoriów (Spring Data JPA)
- **service / services** – logika biznesowa
- **request / response** – obiekty wejściowe i wyjściowe (DTO)
- **util / helpers / converters** – klasy pomocnicze i konwertery
- **config** – konfiguracje (serializacja, bezpieczeństwo, integracje)

Moduł `food` stosuje dodatkowo architekturę heksagonalną: `application` (przypadki użycia i porty), `domain` (logika domenowa), `infrastructure` (adaptery i persystencja), `ui` (warstwa REST).

---

## Przegląd modułów

### `audit/`
Audyt historii zmian encji oparty o Hibernate Envers. Udostępnia podgląd rewizji (`controller`, `service`, `envers`, `response`).

### `common/`
Współdzielone konfiguracje i kontrolery: `JacksonConfiguration` (serializacja JSON), `RootController` oraz `ratelimit/RateLimitFilter` – limitowanie żądań HTTP (Bucket4j, token bucket). Filtr działa w łańcuchu Spring Security za filtrem JWT i ma osobne limity per użytkownik/IP dla logowania (`/api/v1/auth/*`, per IP), Google Drive (`/api/v1/drive/**`) i reszty API, plus limit globalny. Po przekroczeniu zwraca `429` z `Retry-After`; limity w `app.security.rate-limit.http` (`application.yaml`).
`dashboard/` – `GET /api/v1/dashboard/summary` zwraca dane panelu głównego dla zalogowanego użytkownika: daty ostatnich wpisów (trening, dzień diety, weekly, daily), oraz dni z wpisem z ostatnich 35 dni.

### `exceptions/`
Globalna obsługa wyjątków i walidacji: `GlobalExceptionHandler` (centralny handler) oraz klasy formatujące odpowiedzi błędów (`ErrorResponse`, `ValidationErrorResponse`, `FieldValidationError`).

### `finance/`
Śledzenie finansów oraz notowań kryptowalut (integracja z CoinMarketCap). Podział: `controller`, `entity`, `repository`, `service`, `util`.

### `food/`
Najbardziej rozbudowany moduł (architektura heksagonalna): zarządzanie produktami, posiłkami, przepisami, dziennymi podsumowaniami diety i wyliczaniem makroskładników. Zawiera parsery plików z Google Drive.

### `googledrive/`
Integracja z Google Drive (dostęp do API przez konto serwisowe) – import/eksport plików.

### `other/`
Zadania okresowe z podziałem na `daily` i `weekly`.
- `daily` – dzienna checklista (zdrowie, ćwiczenia, książka, software) w PostgreSQL (`other.daily_record`), wpisy przypisane do zalogowanego użytkownika. Datę wpisu wybiera się w formularzu (domyślnie dzisiaj, można uzupełniać wstecz, bez dat w przyszłości). Wpisy można usuwać (tylko własne — cudzy wpis zwraca `404`).
- `weekly` – rekordy tygodniowe (np. waga) w PostgreSQL (`other.weekly_record`), wpisy przypisane do zalogowanego użytkownika; data ustawiana na serwerze.

### `parser/`
Przechowywanie i parsowanie danych o technologiach (`Technology`, `TechnologyList`).

### `security/`
Pełna obsługa uwierzytelniania: JWT (access + refresh token, `jwt`), logowanie przez Google (`oauth2`), kontrolery uwierzytelniania (`controllers`), konfiguracja zabezpieczeń i CORS (`config`), rate limiting prób logowania oraz weryfikacja e-mail. WebSocket (`/ws`, STOMP) wymaga zalogowania: handshake przechodzi przez łańcuch Spring Security, a `WebSocketAuthChannelInterceptor` pozwala klientom tylko subskrybować własną kolejkę `/user/queue/**` — wysyłanie wiadomości (SEND) jest zablokowane, kanał służy wyłącznie do powiadomień serwer → klient. Komunikaty postępu (np. importu z Google Drive) wysyła `common/websocket/UserLogNotifier` tylko do użytkownika, który uruchomił operację (`convertAndSendToUser`); nie ma tematów broadcast.

### `shopping/`
Listy zakupów w PostgreSQL (`shopping.shopping_item`) – dodawanie, filtrowanie po kategorii i usuwanie produktów zalogowanego użytkownika. Kategorie: `GROCERY`, `CLOTHES`, `ELECTRONICS`, `PHARMACY`, `OTHER` (etykiety tłumaczone we frontendzie); `ALL` służy tylko do filtrowania listy.

### `util/`
Wsparcie paginacji i uniwersalnych odpowiedzi REST (`ListResponse`, `Page`, `PaginationHelper`).

### `workouts/`
Treningi: ćwiczenia, plany i szablony, statystyki, rekordy. Zawiera konwertery oraz obiekty `request`/`response`.
Import treningów z Google Drive (`/api/v1/drive/initApplication`) podmienia tylko treningi zalogowanego użytkownika; nazwy ćwiczeń i szablony treningowe to wspólne słowniki odtwarzane przy każdym imporcie.

---

## Bazy danych i migracje

- **PostgreSQL** – główne dane relacyjne (JPA/Hibernate). Schemat zarządzany przez **Liquibase** – changelogi w `src/main/resources/db/changelog/` (master: `changelog.yaml`).
- **Audyt** – tabele Enversa w schemacie `audit` (sufiks `_AUD`).

Skrypty inicjalizujące bazy dla środowiska kontenerowego znajdują się w katalogu `StartupScripts/` w katalogu głównym repozytorium.

---

## Konfiguracja i profile

Konfiguracja znajduje się w `src/main/resources/`:

- `application.yaml` – konfiguracja wspólna (profil domyślny: `dev`)
- `application-dev.yaml` – środowisko lokalne
- `application-compose.yaml` – Docker Compose
- `application-smallhost.yaml` – produkcja

Profil wybiera się parametrem `--spring.profiles.active=<profil>`.

Sekrety i dane dostępowe wstrzykiwane są przez zmienne środowiskowe. Lokalnie skopiuj
[`.env.example`](.env.example) do `.env` i uzupełnij wartości (plik `.env` jest w `.gitignore`
i nie może trafić do repozytorium). Pełny opis zmiennych znajduje się w
[README głównym](../README.md#konfiguracja-i-zmienne-środowiskowe).

---

## Dokumentacja API

Pełna, zawsze aktualna specyfikacja REST API generowana jest automatycznie przez **Springdoc OpenAPI**
i dostępna po uruchomieniu aplikacji:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI (JSON):** `http://localhost:8080/v3/api-docs`

Endpointów nie opisujemy ręcznie w dokumentacji — Swagger UI jest jedynym źródłem prawdy,
dzięki czemu specyfikacja nie dezaktualizuje się względem kodu.

Na produkcji (profil `smallhost`) Swagger UI i `/v3/api-docs` są wyłączone (`springdoc.*.enabled: false`).

---

## Obsługa wyjątków

Obsługa błędów realizowana jest centralnie w pakiecie `exceptions/` (`GlobalExceptionHandler`). Dzięki temu wyjątki, błędy walidacji oraz niestandardowe błędy aplikacji zwracane są w ustandaryzowanym formacie zgodnym z dobrymi praktykami REST.

---

## Testy

Testy znajdują się w `src/test/java/com/improvement_app/` (moduły `food`, `workouts`, `security`). Wykorzystują JUnit, Spring Security Test, **Testcontainers** (PostgreSQL), **REST Assured** oraz **Datafaker** do generowania danych. Uruchomienie:

```bash
./mvnw test
```

---

## Budowanie i uruchamianie

```bash
# Build (z katalogu Backend/)
./mvnw package

# Uruchomienie
java -jar target/improvement-app-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8080
```

**Docker:**
```bash
docker build -t improvement-app-backend .
docker run -p 8080:8080 improvement-app-backend
```

Po starcie dostępne są: Swagger UI (`/swagger-ui.html`), healthcheck (`/actuator/health`) oraz metryki Prometheus (`/actuator/prometheus`).
