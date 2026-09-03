# Kontrola prostředí

**Datum kontroly:** 2026-08-30 (čas bude doplněn při finálním zápisu)

## Verze nástrojů

| Nástroj | Verze | Stav |
|---|---|---|
| JDK (OpenJDK) | 17.0.20.1 | ✅ nainstalován |
| Gradle | 9.7.1 | ✅ nainstalován |
| Docker | 29.7.2 | ✅ nainstalován |
| Docker Compose | v5.4.0 | ✅ nainstalován |
| Git | main (čistá) | ✅ |

## Git status

```
On branch main
Your branch is up to date with 'origin/main'.
nothing added to commit but untracked files present (`.playwright-mcp/`)
```

## Kontrola před fází 2

- Gradle chybí — požadována instalace: `brew install gradle`
- Po instalaci bude pokračovat kroku A.2 (generování wrapperu)

## Kontrola Android prostředí (Fáze 3)

**Datum kontroly:** 2026-09-02

| Nástroj | Cesta / Verze | Stav |
|---|---|---|
| Android SDK | `~/Library/Android/sdk` | ✅ nainstalován |
| build-tools | 36.0.0 | ✅ |
| platforms | android-37.0 | ✅ |
| platform-tools (adb) | přítomny | ✅ |
| Emulátor / zařízení | — | ❌ nenalezen |

**Režim UI kontrol:** Emulátor ani připojené zařízení nebylo nalezeno. UI kontrola probíhá pouze přes `./gradlew assembleDebug` (sestavení APK) a `./gradlew test` (jednotkové testy ViewModel). Manuální ověření na emulátoru bude provedeno, pokud bude emulátor k dispozici v průběhu práce.
