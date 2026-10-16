package com.fvjapps.allowancecalculator.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fvjapps.allowancecalculator.entities.ColorSchemeEntity;
import com.fvjapps.allowancecalculator.entities.LedgerEntity;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.repository.LedgerRepository;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class LedgerIsolationInstrumentedTest {
    private AppDatabase database;
    private ExecutorService databaseExecutor;
    private LedgerRepository ledgerRepository;
    private TransactionRepository transactionRepository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .addCallback(AppDatabase.createCallback())
                .build();
        databaseExecutor = Executors.newSingleThreadExecutor();
        ledgerRepository = new LedgerRepository(database, databaseExecutor);
        transactionRepository = new TransactionRepository(database, databaseExecutor);
    }

    @After
    public void tearDown() {
        database.close();
        databaseExecutor.shutdownNow();
    }

    @Test
    public void freshDatabaseSeedsSchemesAndDefaultLedger() {
        List<ColorSchemeEntity> schemes = database.colorSchemeDao().getAllSync();
        List<LedgerEntity> ledgers = ledgerRepository.getLedgersSync();

        assertEquals(3, schemes.size());
        assertEquals(1, ledgers.size());
        assertEquals("Allowance", ledgers.get(0).getName());
        assertNotNull(ledgers.get(0).getColorSchemeId());
    }

    @Test
    public void transactionsBalancesAndExportsRemainLedgerScoped() {
        LedgerEntity firstLedger = ledgerRepository.getFirstLedgerSync();
        long secondLedgerId = ledgerRepository.createLedger(
                "Savings",
                "Separate ledger",
                database.colorSchemeDao().getAllSync().get(1).getId()
        );

        TransactionEntity allowance =
                new TransactionEntity(firstLedger.getId(), "Allowance", 100, TransactionEntity.TYPE_ALLOWANCE);
        TransactionEntity expense =
                new TransactionEntity(firstLedger.getId(), "Expense", 30, TransactionEntity.TYPE_EXPENSE);
        TransactionEntity otherAllowance =
                new TransactionEntity(secondLedgerId, "Savings deposit", 400, TransactionEntity.TYPE_ALLOWANCE);
        transactionRepository.insert(allowance);
        transactionRepository.insert(expense);
        transactionRepository.insert(otherAllowance);

        assertEquals(70, ledgerRepository.getRunningBalanceSync(firstLedger.getId()), 0.001);
        assertEquals(400, ledgerRepository.getRunningBalanceSync(secondLedgerId), 0.001);

        long expenseId = findTransaction(
                transactionRepository.getAllTransactionsSync(firstLedger.getId()),
                "Expense"
        ).getId();
        transactionRepository.voidTransaction(expenseId);

        assertEquals(100, ledgerRepository.getRunningBalanceSync(firstLedger.getId()), 0.001);
        assertEquals(400, ledgerRepository.getRunningBalanceSync(secondLedgerId), 0.001);
        assertEquals(1, transactionRepository.exportAllActiveData(firstLedger.getId()).size());
        assertEquals(1, transactionRepository.exportAllActiveData(secondLedgerId).size());
        assertTrue(transactionRepository.exportAllData(firstLedger.getId()).stream()
                .anyMatch(transaction -> transaction.getId() == expenseId && transaction.isVoid()));

        transactionRepository.restoreTransaction(expenseId);
        assertEquals(70, ledgerRepository.getRunningBalanceSync(firstLedger.getId()), 0.001);
        assertEquals(400, ledgerRepository.getRunningBalanceSync(secondLedgerId), 0.001);

        List<TransactionEntity> firstLedgerExport =
                transactionRepository.exportAllData(firstLedger.getId());
        List<TransactionEntity> secondLedgerExport =
                transactionRepository.exportAllData(secondLedgerId);
        assertEquals(2, firstLedgerExport.size());
        assertEquals(1, secondLedgerExport.size());
        assertTrue(firstLedgerExport.stream().allMatch(
                transaction -> transaction.getLedgerId() == firstLedger.getId()
        ));
        assertTrue(secondLedgerExport.stream().allMatch(
                transaction -> transaction.getLedgerId() == secondLedgerId
        ));
    }

    @Test
    public void transactionForMissingLedgerIsRejectedWithoutChangingBalances() {
        try {
            transactionRepository.createTransaction(
                    Long.MAX_VALUE,
                    "Invalid",
                    10,
                    TransactionEntity.TYPE_ALLOWANCE
            );
            fail("Expected a missing-ledger failure");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Ledger does not exist"));
        }
        assertTrue(transactionRepository.getAllTransactionsSync(
                ledgerRepository.getFirstLedgerSync().getId()
        ).isEmpty());
    }

    @Test
    public void lastLedgerCannotBeDeleted() {
        LedgerEntity onlyLedger = ledgerRepository.getFirstLedgerSync();

        try {
            ledgerRepository.deleteLedger(onlyLedger.getId());
            fail("Expected deleting the last ledger to fail");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("last ledger"));
        }

        assertEquals(1, ledgerRepository.getLedgersSync().size());
        assertEquals(onlyLedger.getId(), ledgerRepository.getFirstLedgerSync().getId());
    }

    @Test
    public void versionOneMigrationPreservesLegacyTransactions() {
        assertLegacyTransactionsMigrate("allowance-migration-test.db", "transactions");
    }

    @Test
    public void versionOneMigrationSupportsOriginalEntityTableName() {
        assertLegacyTransactionsMigrate(
                "allowance-migration-entity-test.db",
                "TransactionEntity"
        );
    }

    private void assertLegacyTransactionsMigrate(
            String databaseName,
            String legacyTableName
    ) {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(databaseName);

        SQLiteDatabase legacyDatabase = context.openOrCreateDatabase(
                databaseName,
                Context.MODE_PRIVATE,
                null
        );
        legacyDatabase.execSQL("""
                CREATE TABLE `%s` (
                    transactionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    label TEXT,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    createdAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.formatted(legacyTableName));
        legacyDatabase.execSQL(
                "CREATE INDEX index_" + legacyTableName + "_createdAt " +
                        "ON `" + legacyTableName + "` (createdAt)"
        );
        legacyDatabase.execSQL(
                "CREATE INDEX index_" + legacyTableName + "_isDeleted " +
                        "ON `" + legacyTableName + "` (isDeleted)"
        );
        legacyDatabase.execSQL("""
                INSERT INTO `%s`
                    (transactionId, label, type, amount, createdAt, isDeleted)
                VALUES
                    (17, 'Allowance', 'IN', 100, 1000, 0),
                    (18, 'Groceries', 'OUT', 25, 2000, 0),
                    (19, 'Old expense', 'OUT', 10, 3000, 1)
                """.formatted(legacyTableName));
        legacyDatabase.setVersion(1);
        legacyDatabase.close();

        AppDatabase migratedDatabase = null;
        try {
            migratedDatabase = Room.databaseBuilder(
                            context,
                            AppDatabase.class,
                            databaseName
                    )
                    .addMigrations(AppDatabase.MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .build();
            List<LedgerEntity> ledgers = migratedDatabase.ledgerDao().getAllSync();
            assertEquals(1, ledgers.size());
            LedgerEntity migratedLedger = ledgers.get(0);
            assertNotNull(migratedLedger.getColorSchemeId());
            assertEquals(75, migratedLedger.getRunningBalance(), 0.001);

            List<TransactionEntity> transactions =
                    migratedDatabase.transactionDao().getAllTransactionsSync(
                            migratedLedger.getId()
                    );
            assertEquals(3, transactions.size());

            TransactionEntity allowance = transactions.get(0);
            assertEquals(17, allowance.getId());
            assertEquals("Allowance", allowance.getName());
            assertEquals(TransactionEntity.TYPE_ALLOWANCE, allowance.getType());
            assertEquals(100, allowance.getAmount(), 0.001);
            assertEquals(1000, allowance.getCreatedAt());
            assertFalse(allowance.isVoid());

            TransactionEntity expense = transactions.get(1);
            assertEquals(18, expense.getId());
            assertEquals("Groceries", expense.getName());
            assertEquals(TransactionEntity.TYPE_EXPENSE, expense.getType());
            assertEquals(2000, expense.getCreatedAt());
            assertFalse(expense.isVoid());

            TransactionEntity deletedExpense = transactions.get(2);
            assertEquals(19, deletedExpense.getId());
            assertEquals("Old expense", deletedExpense.getName());
            assertTrue(deletedExpense.isVoid());
        } finally {
            if (migratedDatabase != null) {
                migratedDatabase.close();
            }
            context.deleteDatabase(databaseName);
        }
    }

    private TransactionEntity findTransaction(
            List<TransactionEntity> transactions,
            String name
    ) {
        for (TransactionEntity transaction : transactions) {
            if (transaction.getName().equals(name)) {
                return transaction;
            }
        }
        throw new AssertionError("Transaction not found: " + name);
    }
}
