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
