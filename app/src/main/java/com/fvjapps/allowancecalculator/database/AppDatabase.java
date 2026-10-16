package com.fvjapps.allowancecalculator.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.fvjapps.allowancecalculator.dao.ColorSchemeDao;
import com.fvjapps.allowancecalculator.dao.LedgerDao;
import com.fvjapps.allowancecalculator.dao.TransactionDao;
import com.fvjapps.allowancecalculator.entities.ColorSchemeEntity;
import com.fvjapps.allowancecalculator.entities.LedgerEntity;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;

@Database(
        entities = {
                LedgerEntity.class,
                TransactionEntity.class,
                ColorSchemeEntity.class
        },
        version = 2,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;
    public abstract LedgerDao ledgerDao();

    public abstract TransactionDao transactionDao();

    public abstract ColorSchemeDao colorSchemeDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "allowancecalculatordb"
                    )
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(createCallback())
                            .build();
                }
            }
        }
        return instance;
    }

    static RoomDatabase.Callback createCallback() {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(
                    @androidx.annotation.NonNull
                    androidx.sqlite.db.SupportSQLiteDatabase db
            ) {
                super.onCreate(db);
                seedColorSchemes(db);
            }
        };
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@androidx.annotation.NonNull SupportSQLiteDatabase db) {
            String legacyTransactionTable = findLegacyTransactionTable(db);
            ensureLegacyTransactionTypesSupported(db, legacyTransactionTable);
            db.execSQL("""
                    CREATE TABLE IF NOT EXISTS color_schemes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        primary_color INTEGER NOT NULL,
                        secondary_color INTEGER NOT NULL,
                        background_color INTEGER NOT NULL,
                        surface_color INTEGER NOT NULL,
                        surface_elevated_color INTEGER NOT NULL,
                        text_color INTEGER NOT NULL,
                        border_color INTEGER NOT NULL
                    )
                    """);
            db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_color_schemes_name
                    ON color_schemes (name)
                    """);
            db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ledgers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        color_scheme_id INTEGER,
                        running_balance REAL NOT NULL,
                        FOREIGN KEY(color_scheme_id) REFERENCES color_schemes(id)
                            ON UPDATE CASCADE ON DELETE SET NULL
                    )
                    """);
            db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_ledgers_color_scheme_id
                    ON ledgers (color_scheme_id)
                    """);
            seedColorSchemes(db);
            long defaultLedgerId = getDefaultLedgerId(db);
            db.execSQL("ALTER TABLE `" + legacyTransactionTable
                    + "` RENAME TO legacy_transactions");
            db.execSQL("""
                    CREATE TABLE transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ledger_id INTEGER NOT NULL,
                        is_void INTEGER NOT NULL,
                        type INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(ledger_id) REFERENCES ledgers(id)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """);
            db.execSQL("""
                    CREATE INDEX index_transactions_ledger_id
                    ON transactions (ledger_id)
                    """);
            db.execSQL("""
                    CREATE INDEX index_transactions_created_at
                    ON transactions (created_at)
                    """);
            db.execSQL("""
                    CREATE INDEX index_transactions_is_void
                    ON transactions (is_void)
                    """);
            db.execSQL("""
                    INSERT INTO transactions (
                        id, ledger_id, is_void, type, name, amount, created_at
                    )
                    SELECT transactionId, ?, COALESCE(isDeleted, 0),
                           CASE type WHEN 'IN' THEN 0 WHEN 'OUT' THEN 1 END,
                           COALESCE(label, ''), amount, createdAt
                    FROM legacy_transactions
                    """, new Object[]{defaultLedgerId});
            db.execSQL("""
                    UPDATE ledgers
                    SET running_balance = (
                        SELECT COALESCE(SUM(
                            CASE
                                WHEN is_void = 0 AND type = 0 THEN amount
                                WHEN is_void = 0 AND type = 1 THEN -amount
                                ELSE 0
                            END
                        ), 0)
                        FROM transactions
                        WHERE ledger_id = ?
                    )
                    WHERE id = ?
                    """, new Object[]{defaultLedgerId, defaultLedgerId});
            db.execSQL("DROP TABLE legacy_transactions");
        }
    };

    private static String findLegacyTransactionTable(SupportSQLiteDatabase db) {
        if (tableExists(db, "transactions")) {
            return "transactions";
        }
        if (tableExists(db, "TransactionEntity")) {
            return "TransactionEntity";
        }
        throw new IllegalStateException(
                "Database migration could not find the legacy transactions table."
        );
    }

    private static boolean tableExists(SupportSQLiteDatabase db, String tableName) {
        try (android.database.Cursor cursor = db.query(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                new Object[]{tableName}
        )) {
            return cursor.moveToFirst();
        }
    }

    private static void ensureLegacyTransactionTypesSupported(
            SupportSQLiteDatabase db,
            String tableName
    ) {
        try (android.database.Cursor cursor = db.query("""
                SELECT COUNT(*)
                FROM `%s`
                WHERE type IS NULL OR type NOT IN ('IN', 'OUT')
                """.formatted(tableName))) {
            if (!cursor.moveToFirst()) {
                throw new IllegalStateException(
                        "Unable to validate legacy transaction types."
                );
            }
            if (cursor.getLong(0) > 0) {
                throw new IllegalStateException(
                        "Database migration found unsupported legacy transaction types."
                );
            }
        }
    }

    private static long getDefaultLedgerId(SupportSQLiteDatabase db) {
        try (android.database.Cursor cursor = db.query("""
                SELECT id
                FROM ledgers
                WHERE name = 'Allowance'
                ORDER BY id ASC
                LIMIT 1
                """)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalStateException(
                        "Database migration could not create the default ledger."
                );
            }
            return cursor.getLong(0);
        }
    }

    private static void seedColorSchemes(
            androidx.sqlite.db.SupportSQLiteDatabase db
    ) {
        db.execSQL("""
                INSERT OR IGNORE INTO color_schemes (
                    name, primary_color, secondary_color, background_color,
                    surface_color, surface_elevated_color, text_color, border_color
                ) VALUES
                    ('Classic Green', -16729964, -16741524, -1, -1, -460294,
                     -15066598, -2039584),
                    ('Ocean Blue', -16756020, -16761447, -1, -1, -460294,
                     -15066598, -2039584),
                    ('Warm Sunset', -38037, -18685, -1, -1, -460294,
                     -15066598, -2039584)
                """);
        db.execSQL("""
                INSERT OR IGNORE INTO ledgers (
                    name, description, color_scheme_id, running_balance
                )
                SELECT 'Allowance', '', id, 0
                FROM color_schemes
                WHERE name = 'Classic Green'
                  AND NOT EXISTS (SELECT 1 FROM ledgers)
                """);
    }

}
