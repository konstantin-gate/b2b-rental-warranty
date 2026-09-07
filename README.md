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

## Funkce aplikace

### Role client
- **Katalog vybavení** — prohlížení katalogu s filtrováním podle kategorie, karta vybavení (cena, měsíční sazba, popis, foto, stav).
- **Nájemní smlouvy** — vytvoření draftu smlouvy (výběr vybavení, doba nájmu 1–36 měsíců, adresa doručení), přehled smluv, PDF dokumenty (smlouva, předávací protokol, protokol o vrácení).
- **Platby** — přehled plateb smlouvy, označení zaplacení, přehled dlužných (overdue) plateb.
- **Servisní tikety** — „Nahlásit poruchu“: popis + fotografie (kamera/galerie), předbežná AI diagnostika a verdikt záruky přímo při tvorbě tiketu; přehled vlastních tiketů včetně historie.
- **Moje vybavení** — karty pronajatého vybavení se stavem.
- **AI asistent** — chat s backendovým AI (kontext z dat systému, RAG nad znalostní bází).

### Role technician
- **Moje úkoly** — seznam přiřazených servisních tiketů.
- **Karta tiketu** — popis a fotografie závady, AI diagnostika, verdikt záruky s vysvětlením.
- **Vyřízení tiketu** — výsledek `repaired` / `replaced` / `not_covered` + poznámky, AI pomáhá sestavit text zprávy; systém generuje servisní zprávu (PDF).

### Role manager / admin
- **Dashboard** — aktivní nájmy, otevřené tikety, zpožděné platby, vybavení podle stavů, statistika.
- **Schválení/zamítnutí nájmu** — draft → active/rejected, automatické vytvoření grafu plateb, přechod vybavení do stavu `rented`.
- **Přiřazení technika** k tiketu.
- **(Admin) Správa katalogu** — CRUD vybavení a kategorií.
- **Správa uživatelů** (admin).

### Notifikace (všechny role)
- **Serverové notifikace** — backend je jediným zdrojem; zapisuje notifikace při 6 obchodních událostech (nový/přiřazený/zahájený/vyřešený tiket, schválená/zamítnutá smlouva).
- **Obrazovka notifikací** — vstup ikonou zvonku v horní liště, počet nepřečtených, označení jedné/všech jako přečtené.
- **Systémová upozornění** — `SyncWorker` dotazuje `GET /notifications?unread=true` každých 15 minut a zobrazí systémovou notifikaci; poté automaticky `read-all`.

### AI (backend)
- **AI diagnostika** (`/ai/diagnose`) — popis + foto → pravděpodobná příčina, závažnost, doporučení.
- **Kontrola záruky** (`/ai/warranty-check`) — deterministická pravidla R1–R4 (WarrantyPrelude) rozhodují verdikt, AI dodává vysvětlení.
- **AI asistent** (`/ai/assistant`) — odpovídá na dotazy nad daty systému s RAG kontextem ze znalostní báze (filtrace vymyšlených citací).

### Zabezpečení
- JWT autorizace s rolemi `admin` / `manager` / `technician` / `client` (401 bez tokenu, 403 s cizí rolí).
- Hesla výhradně jako bcrypt hash; tajné klíče pouze v proměnných prostředí (`.env`).

## Snímky obrazovky

Snímky obrazovky aplikace se ukládají do adresáře `docs/screenshots/` a jsou připojeny níže.
Na snímky vyžadují spuštěné zařízení/emulátor — jsou přidávány ručně (viz plán, krok 6.3):

| Soubor | Obrazovka |
|---|---|
| `docs/screenshots/01_login.png` | Přihlášení (úvodní obrazovka) |
| `docs/screenshots/02_catalog.png` | Katalog vybavení (client) |
| `docs/screenshots/03_equipment_detail.png` | Karta vybavení |
| `docs/screenshots/04_contract_create.png` | Vytvoření nájemní smlouvy |
| `docs/screenshots/05_contracts.png` | Seznam smluv + PDF |
| `docs/screenshots/06_ticket_create.png` | „Nahlásit poruchu“ (AI diagnostika) |
| `docs/screenshots/07_tickets.png` | Seznam servisních tiketů |
| `docs/screenshots/08_payments.png` | Platby a dlužné částky |
| `docs/screenshots/09_technician_task.png` | Karta tiketu technika (výsledek, záruka) |
| `docs/screenshots/10_dashboard.png` | Dashboard manažera |
| `docs/screenshots/11_admin_catalog.png` | Správa katalogu (admin) |
| `docs/screenshots/12_notifications.png` | Obrazovka notifikací (zvonek) |
| `docs/screenshots/13_ai_assistant.png` | AI asistent (chat) |

> Poznámka: soubory zatím nejsou v repozitáři — tabulka slouží jako checklist pro ruční
> doplnění snímků z emulátoru/zařízení.

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

## Spuštění přes Docker Compose

1. Spusťte obě služby (PostgreSQL + backend) jedním příkazem:
   ```bash
   docker compose up -d --build
   ```

2. Ověřte dostupnost backendu:
   ```bash
   curl -s http://localhost:8090/health
   # Očekávaná odpověď: {"status":"ok"}
   ```

3. Úplné smazání dat (včetně databáze):
   ```bash
   docker compose down -v
   ```

V Docker režimu je AI vypnuté (`AI_ENABLED=false` v `docker-compose.yml`), protože
llama-server se v compose nespouští. Pro zapnutí AI je nutné před `docker compose up`
nastavit v shellu proměnné prostředí `AI_ENABLED=true`, `AI_MODEL=<název modelu>`,
`AI_BASE_URL=http://host.docker.internal:8080/v1` (llama-server běží na hostiteli, port
8080 zůstává modelu) a v `docker-compose.yml` u služby `backend` tytéž proměnné přidat
do bloku `environment` (`AI_ENABLED: ${AI_ENABLED}`, `AI_MODEL: ${AI_MODEL}`,
`AI_BASE_URL: ${AI_BASE_URL}`); bez těchto hodnot backend při `AI_ENABLED=true`
nenastartuje. Demo účty jsou stejné jako v sekci [Demo účty](#demo-účty).

## API – notifikace

Backend je jediným zdrojem notifikací. K dispozici jsou následující endpointy
(notifikace uživatele, všechny role):

| Metoda | Endpoint | Popis |
|---|---|---|
| GET | `/notifications` | seznam notifikací uživatele (parametr `unread=true` pro jen nepřečtené) |
| GET | `/notifications/unread-count` | počet nepřečtených notifikací |
| POST | `/notifications/{id}/read` | označení notifikace jako přečtené (pouze vlastník, jinak 404) |
| POST | `/notifications/read-all` | označení všech notifikací jako přečtené |

## AI integrace

- **Model:** lokální `llama-server` s OpenAI-kompatibilním API (jediný runtime poskytovatel LLM)
- **Konfigurace:** proměnné prostředí v `.env` — `AI_BASE_URL` (výchozí `http://127.0.0.1:8080/v1`), `AI_MODEL` (název modelu z `GET http://127.0.0.1:8080/v1/models`), `AI_API_KEY` (nepovinné), `AI_REQUEST_TIMEOUT_MS`, `AI_CONNECT_TIMEOUT_MS`, `AI_MAX_RETRIES`, `AI_MAX_OUTPUT_TOKENS`, `AI_ENABLED`
- **Volání:** probíhá přes backend (`/ai/diagnose`, `/ai/warranty-check`, `/ai/assistant`); HTTP komunikaci zajišťuje `OpenAiCompatibleLlmClient` s limitem délky vstupu, omezenými opakováními (timeout, chyba spojení, HTTP 429/5xx) a odstraněním bloků `<think>` z odpovědi
- **Fallback:** pokud je `AI_ENABLED=false` nebo llama-server nedostupný, vrací se deterministická odpověď bez pádu serveru
- **Bezpečnost:** přístupové údaje k LLM jsou pouze na backendu, nikdy se nedostanou do Android aplikace ani do gitu; obsah promptů a klíče se nelogují
