package pdm.application.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pdm.application.model.Converters
import pdm.application.model.TournamentEntity

@Database(entities = [TournamentEntity::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tournaments ADD COLUMN hasPendingChanges INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create temporary table with new schema
                database.execSQL("""
                    CREATE TABLE tournaments_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        startDateTime INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        participantsCount INTEGER NOT NULL,
                        prizePool REAL NOT NULL,
                        isRegistrationOpen INTEGER NOT NULL,
                        winner TEXT,
                        status TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        hasPendingChanges INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Copy data
                database.execSQL("""
                    INSERT INTO tournaments_new 
                    SELECT * FROM tournaments
                """)

                // Remove old table
                database.execSQL("DROP TABLE tournaments")

                // Rename new table
                database.execSQL("ALTER TABLE tournaments_new RENAME TO tournaments")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with updated schema
                database.execSQL("""
                    CREATE TABLE tournaments_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        participantsCount INTEGER NOT NULL,
                        prizePool REAL NOT NULL,
                        isRegistrationOpen INTEGER NOT NULL,
                        winner TEXT,
                        status TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        hasPendingChanges INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Copy data, renaming startDateTime to startDate
                database.execSQL("""
                    INSERT INTO tournaments_new 
                    SELECT 
                        id, 
                        name, 
                        description, 
                        startDateTime,
                        endDate, 
                        participantsCount, 
                        prizePool,
                        isRegistrationOpen, 
                        winner, 
                        status, 
                        userId,
                        version, 
                        lastUpdated, 
                        hasPendingChanges
                    FROM tournaments
                """)

                // Drop old table
                database.execSQL("DROP TABLE tournaments")

                // Rename new table to original name
                database.execSQL("ALTER TABLE tournaments_new RENAME TO tournaments")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add location columns to tournaments table
                database.execSQL("""
                    ALTER TABLE tournaments 
                    ADD COLUMN latitude REAL DEFAULT NULL
                """)
                database.execSQL("""
                    ALTER TABLE tournaments 
                    ADD COLUMN longitude REAL DEFAULT NULL
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tournament_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

}