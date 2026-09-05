# B2B Rental & Warranty

Platforma pro pronájem a záruční servis komerčního chladicího zařízení pro restaurace, kavárny a obchody.

## Architektura

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   Android   │────▶│   Backend   │────▶│  PostgreSQL  │
│  (Compose)  │     │  (Ktor)     │     │     16       │
└─────────────┘     └──────┬──────┘     └──────────────┘
                           │
                    ┌──────▼──────────┐
                    │  llama-server   │
                    │ (lokální LLM,   │
                    │  OpenAI API)    │
                    └─────────────────┘
```

## Stack

- **Backend:** Kotlin 2.x | Ktor 3.x | Exposed | PostgreSQL 16
- **Android:** Kotlin 2.x | Jetpack Compose | Material 3
- **Infra:** Docker Compose

## Stav

🚧 Ve vývoji

## Demo účty

| Role | E-mail | Heslo |
|---|---|---|
| admin | admin@b2b.demo | admin123 |
| manager | manager@b2b.demo | manager123 |
| technician | tech@b2b.demo | tech123 |
| client | kitchen@b2b.demo | kitchen123 |

## Spuštění backendu lokálně

### Předpoklady

- Java 17 (JDK)
- Docker (pro PostgreSQL)

### Postup

1. Spusťte PostgreSQL přes Docker:
   ```bash
   docker compose up -d
   ```

2. Nakonfigurujte prostředí (zkopírujte `.env.example` do `.env` a vyplňte hodnoty):
   ```bash
   cp .env.example .env
   # Vygenerujte JWT_SECRET:
   openssl rand -hex 32
   # Vygenerujte DB_PASS:
   openssl rand -hex 16
   ```

3. Spusťte backend:
   ```bash
   cd backend
   ./gradlew run
   ```

4. Ověřte dostupnost:
   ```bash
   curl -s http://localhost:8090/health
   # Očekávaná odpověď: {"status":"ok"}
   ```

### Porty

| Služba | Port |
|---|---|
| Backend (Ktor) | 8090 |
| PostgreSQL | 5432 |

## AI integrace

- **Model:** lokální `llama-server` s OpenAI-kompatibilním API (jediný runtime poskytovatel LLM)
- **Konfigurace:** proměnné prostředí v `.env` — `AI_BASE_URL` (výchozí `http://127.0.0.1:8080/v1`), `AI_MODEL` (název modelu z `GET http://127.0.0.1:8080/v1/models`), `AI_API_KEY` (nepovinné), `AI_REQUEST_TIMEOUT_MS`, `AI_CONNECT_TIMEOUT_MS`, `AI_MAX_RETRIES`, `AI_MAX_OUTPUT_TOKENS`, `AI_ENABLED`
- **Volání:** probíhá přes backend (`/ai/diagnose`, `/ai/warranty-check`, `/ai/assistant`); HTTP komunikaci zajišťuje `OpenAiCompatibleLlmClient` s limitem délky vstupu, omezenými opakováními (timeout, chyba spojení, HTTP 429/5xx) a odstraněním bloků `<think>` z odpovědi
- **Fallback:** pokud je `AI_ENABLED=false` nebo llama-server nedostupný, vrací se deterministická odpověď bez pádu serveru
- **Bezpečnost:** přístupové údaje k LLM jsou pouze na backendu, nikdy se nedostanou do Android aplikace ani do gitu; obsah promptů a klíče se nelogují
