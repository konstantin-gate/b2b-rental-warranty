package cz.b2brental.config

// Konfigurace aplikace - čtení proměnných prostředí
data class Config(
    val dbUrl: String,
    val dbUser: String,
    val dbPass: String,
    val jwtSecret: String,
    val openaiApiKey: String?
) {
    companion object {
        fun fromEnv(): Config {
            val dbUrl = System.getenv("DB_URL")
                ?: throw IllegalStateException("Chybí povinná proměnná prostředí: DB_URL")
            val dbUser = System.getenv("DB_USER")
                ?: throw IllegalStateException("Chybí povinná proměnná prostředí: DB_USER")
            val dbPass = System.getenv("DB_PASS")
                ?: throw IllegalStateException("Chybí povinná proměnná prostředí: DB_PASS")
            val jwtSecret = System.getenv("JWT_SECRET")
                ?: throw IllegalStateException("Chybí povinná proměnná prostředí: JWT_SECRET")
            val openaiApiKey = System.getenv("OPENAI_API_KEY")
            return Config(
                dbUrl = dbUrl,
                dbUser = dbUser,
                dbPass = dbPass,
                jwtSecret = jwtSecret,
                openaiApiKey = openaiApiKey
            )
        }
    }
}
