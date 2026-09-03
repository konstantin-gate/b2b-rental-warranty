package cz.b2brental.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cz.b2brental.data.local.dao.CatalogDao
import cz.b2brental.data.local.entity.CatalogEntity

/**
 * Room databáze B2B Rental — uchovává offline cache katalogu.
 * Verze 1: catalog_items tabulka.
 */
@Database(
    version = 1,
    entities = [CatalogEntity::class],
    exportSchema = true,
)
@TypeConverters(Converters::class)
public abstract class B2bDatabase : RoomDatabase() {

    /** DAO pro přístup k položkám katalogu. */
    public abstract fun catalogDao(): CatalogDao

    public companion object {

        /**
         * Vytvoří instanci Room databáze.
         * @param context kontext aplikace
         * @return instance B2bDatabase
         */
        public fun create(context: Context): B2bDatabase {
            return Room.databaseBuilder(
                context,
                B2bDatabase::class.java,
                "b2b_rental.db",
            ).build()
        }
    }
}
