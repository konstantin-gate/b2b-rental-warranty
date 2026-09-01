# B2B Rental & Warranty

Platforma pro pronájem a záruční servis komerčního chladicího zařízení pro restaurace, kavárny a obchody.

## Architektura

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   Android   │────▶│   Backend   │────▶│  PostgreSQL  │
│  (Compose)  │     │  (Ktor)     │     │     16       │
└─────────────┘     └──────┬──────┘     └──────────────┘
                           │
                    ┌──────▼──────┐
                    │  OpenAI API │
                    └─────────────┘
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

- **Model:** `gpt-4o-mini` (OpenAI)
- **Klíč:** nastavte proměnnou prostředí `OPENAI_API_KEY` v `.env`
- **Volání:** probíhá přes backend (`/ai/diagnose`, `/ai/warranty-check`, `/ai/assistant`)
- **Fallback:** pokud klíč není nastaven nebo API nedostupné, vrací se deterministická odpověď bez pádu serveru
- **Bezpečnost:** `OPENAI_API_KEY` je pouze na backendu, nikdy se nedostane do Android aplikace ani do gitu
