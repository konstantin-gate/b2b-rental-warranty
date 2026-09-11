package cz.b2brental.config

/**
 * Konfigurace aplikace - čtení proměnných prostředí.
 *
 * Parametry lokálního LLM (llama-server s OpenAI-kompatibilním API):
 * `AI_BASE_URL`, `AI_MODEL`, `AI_API_KEY`, `AI_REQUEST_TIMEOUT_MS`,
 * `AI_CONNECT_TIMEOUT_MS`, `AI_MAX_RETRIES`, `AI_MAX_OUTPUT_TOKENS`, `AI_ENABLED`.
 *
 * @property dbUrl JDBC URL databáze (env DB_URL)
 * @property dbUser uživatel databáze (env DB_USER)
 * @property dbPass heslo databáze (env DB_PASS)
 * @property jwtSecret tajný klíč pro podepisování JWT (env JWT_SECRET)
 * @property aiBaseUrl základní URL LLM serveru s OpenAI-kompatibilním API (env AI_BASE_URL)
 * @property aiModel název modelu LLM (env AI_MODEL)
 * @property aiApiKey API klíč LLM serveru nebo null (env AI_API_KEY)
 * @property aiRequestTimeoutMillis celkový timeout požadavku na LLM v ms (env AI_REQUEST_TIMEOUT_MS)
 * @property aiConnectTimeoutMillis timeout připojení k LLM serveru v ms (env AI_CONNECT_TIMEOUT_MS)
 * @property aiMaxRetries maximální počet opakování neúspěšného volání LLM (env AI_MAX_RETRIES)
 * @property aiMaxOutputTokens maximální počet výstupních tokenů LLM (env AI_MAX_OUTPUT_TOKENS)
 * @property aiEnabled příznak zapnuté AI funkcionality (env AI_ENABLED)
 * @property seedDemoData příznak naplnění demo daty (env SEED_DEMO_DATA)
 */
public data class Config(
    public val dbUrl: String,
    public val dbUser: String,
    public val dbPass: String,
    public val jwtSecret: String,
    public val aiBaseUrl: String,
    public val aiModel: String,
    public val aiApiKey: String?,
    public val aiRequestTimeoutMillis: Long,
    public val aiConnectTimeoutMillis: Long,
    public val aiMaxRetries: Int,
    public val aiMaxOutputTokens: Int,
    public val aiEnabled: Boolean,
    public val seedDemoData: Boolean,
) {
    public companion object {
        /**
         * Načte konfiguraci z proměnných prostředí a ověří její platnost.
         * @return naplněná a ověřená konfigurace aplikace
         * @throws IllegalStateException chybí povinná proměnná prostředí nebo má neplatnou hodnotu
         */
        @Suppress("HardCodedStringLiteral")
        public fun fromEnv(): Config {
            val dbUrl: String =
                System.getenv("DB_URL")
                    ?: throw IllegalStateException("Chybí povinná proměnná prostředí: DB_URL")
            val dbUser: String =
                System.getenv("DB_USER")
                    ?: throw IllegalStateException("Chybí povinná proměnná prostředí: DB_USER")
            val dbPass: String =
                System.getenv("DB_PASS")
                    ?: throw IllegalStateException("Chybí povinná proměnná prostředí: DB_PASS")
            val jwtSecret: String =
                System.getenv("JWT_SECRET")
                    ?: throw IllegalStateException("Chybí povinná proměnná prostředí: JWT_SECRET")
            if (jwtSecret.length < 32) {
                throw IllegalStateException("Proměnná prostředí JWT_SECRET musí mít alespoň 32 znaků")
            }

            val aiBaseUrl: String =
                (System.getenv("AI_BASE_URL") ?: "http://127.0.0.1:8080/v1").trim()
            val aiModel: String = (System.getenv("AI_MODEL") ?: "").trim()
            val aiApiKey: String? = System.getenv("AI_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
            val aiEnabled: Boolean =
                (System.getenv("AI_ENABLED") ?: "true").toBooleanStrictOrNull()
                    ?: throw IllegalStateException("Proměnná prostředí AI_ENABLED musí být true nebo false")
            val aiRequestTimeoutMillis: Long = parseLongEnv("AI_REQUEST_TIMEOUT_MS", 120_000L)
            val aiConnectTimeoutMillis: Long = parseLongEnv("AI_CONNECT_TIMEOUT_MS", 5_000L)
            val aiMaxRetries: Int = parseIntEnv("AI_MAX_RETRIES", 1)
            val aiMaxOutputTokens: Int = parseIntEnv("AI_MAX_OUTPUT_TOKENS", 700)

            if (aiEnabled && aiModel.isEmpty()) {
                throw IllegalStateException("Chybí povinná proměnná prostředí: AI_MODEL")
            }
            if (!aiBaseUrl.startsWith("http://") && !aiBaseUrl.startsWith("https://")) {
                throw IllegalStateException(
                    "Proměnná prostředí AI_BASE_URL musí začínat http:// nebo https://",
                )
            }
            if (aiRequestTimeoutMillis <= 0) {
                throw IllegalStateException("Proměnná prostředí AI_REQUEST_TIMEOUT_MS musí být kladné číslo")
            }
            if (aiConnectTimeoutMillis <= 0) {
                throw IllegalStateException("Proměnná prostředí AI_CONNECT_TIMEOUT_MS musí být kladné číslo")
            }
            if (aiMaxRetries !in 0..2) {
                throw IllegalStateException("Proměnná prostředí AI_MAX_RETRIES musí být v rozsahu 0..2")
            }
            if (aiMaxOutputTokens <= 0) {
                throw IllegalStateException("Proměnná prostředí AI_MAX_OUTPUT_TOKENS musí být kladné číslo")
            }

            val seedDemoData: Boolean =
                (System.getenv("SEED_DEMO_DATA") ?: "false").toBooleanStrictOrNull()
                    ?: throw IllegalStateException("Proměnná prostředí SEED_DEMO_DATA musí být true nebo false")

            return Config(
                dbUrl = dbUrl,
                dbUser = dbUser,
                dbPass = dbPass,
                jwtSecret = jwtSecret,
                aiBaseUrl = aiBaseUrl,
                aiModel = aiModel,
                aiApiKey = aiApiKey,
                aiRequestTimeoutMillis = aiRequestTimeoutMillis,
                aiConnectTimeoutMillis = aiConnectTimeoutMillis,
                aiMaxRetries = aiMaxRetries,
                aiMaxOutputTokens = aiMaxOutputTokens,
                aiEnabled = aiEnabled,
                seedDemoData = seedDemoData,
            )
        }

        /**
         * Načte celočíselnou proměnnou prostředí (Long) s výchozí hodnotou.
         * @param name název proměnné prostředí
         * @param default výchozí hodnota při chybějící nebo prázdné proměnné
         * @return hodnota proměnné jako Long nebo default
         * @throws IllegalStateException proměnná není celé číslo
         */
        private fun parseLongEnv(
            name: String,
            default: Long,
        ): Long {
            val raw: String? = System.getenv(name)
            if (raw.isNullOrBlank()) return default
            return raw.toLongOrNull()
                ?: throw IllegalStateException("Proměnná prostředí $name musí být celé číslo")
        }

        /**
         * Načte celočíselnou proměnnou prostředí (Int) s výchozí hodnotou.
         * @param name název proměnné prostředí
         * @param default výchozí hodnota při chybějící nebo prázdné proměnné
         * @return hodnota proměnné jako Int nebo default
         * @throws IllegalStateException proměnná není celé číslo
         */
        private fun parseIntEnv(
            name: String,
            default: Int,
        ): Int {
            val raw: String? = System.getenv(name)
            if (raw.isNullOrBlank()) return default
            return raw.toIntOrNull()
                ?: throw IllegalStateException("Proměnná prostředí $name musí být celé číslo")
        }
    }
}
