package com.fvjapps.allowancecalculator.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

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
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(
                                        @androidx.annotation.NonNull
                                        androidx.sqlite.db.SupportSQLiteDatabase db
                                ) {
                                    super.onCreate(db);
                                    seedColorSchemes(db);
                                }
                            })
                            .build();
                }
            }
        }
        return instance;
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
