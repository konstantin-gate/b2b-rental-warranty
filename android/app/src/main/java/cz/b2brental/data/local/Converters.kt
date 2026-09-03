package cz.b2brental.data.local

import androidx.room.TypeConverter
import cz.b2brental.data.remote.B2bJson
import cz.b2brental.domain.model.EquipmentStatus

/**
 * Konvertéry pro uložiště Room — převádí enum EquipmentStatus na/from wire-řetězec.
 */
public class Converters {

    /**
     * Převod EquipmentStatus na wire-řetězec pro uložení v DB.
     * @param status stav vybavení
     * @return wire-řetězec (např. "available")
     */
    @TypeConverter
    public fun fromEquipmentStatus(status: EquipmentStatus?): String? {
        return status?.let { B2bJson.encodeToString(EquipmentStatus.serializer(), it) }
    }

    /**
     * Převod wire-řetězce z DB na EquipmentStatus.
     * @param value wire-řetězec
     * @return EquipmentStatus nebo null při chybě
     */
    @TypeConverter
    public fun toEquipmentStatus(value: String?): EquipmentStatus? {
        if (value == null) return null
        return try {
            B2bJson.decodeFromString(EquipmentStatus.serializer(), value)
        } catch (_: Exception) {
            null
        }
    }
}
