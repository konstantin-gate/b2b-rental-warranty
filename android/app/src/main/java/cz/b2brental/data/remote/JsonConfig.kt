package cz.b2brental.data.remote

import kotlinx.serialization.json.Json

/** Volba formátu JSON pro klienta a parsování chyb. */
public val B2bJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
}
