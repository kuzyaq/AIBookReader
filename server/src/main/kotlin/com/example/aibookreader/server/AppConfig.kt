package com.example.aibookreader.server

data class AppConfig(
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlSeconds: Long,
    val serverPort: Int
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            val jwtSecret = System.getenv("JWT_SECRET")
                ?: System.getProperty("JWT_SECRET")
                ?: "dev-only-change-me-minimum-32-characters-long!!"

            return AppConfig(
                jdbcUrl = System.getenv("JDBC_URL")
                    ?: "jdbc:postgresql://localhost:5432/aibookreader",
                dbUser = System.getenv("DB_USER") ?: "postgres",
                dbPassword = System.getenv("DB_PASSWORD") ?: "postgres",
                jwtSecret = jwtSecret,
                jwtIssuer = System.getenv("JWT_ISSUER") ?: "aibookreader",
                jwtAudience = System.getenv("JWT_AUDIENCE") ?: "aibookreader-app",
                accessTokenTtlSeconds = System.getenv("ACCESS_TOKEN_TTL_SEC")?.toLongOrNull() ?: 900L,
                refreshTokenTtlSeconds = System.getenv("REFRESH_TOKEN_TTL_SEC")?.toLongOrNull() ?: 1209600L,
                serverPort = System.getenv("PORT")?.toIntOrNull() ?: 8080
            )
        }
    }
}
