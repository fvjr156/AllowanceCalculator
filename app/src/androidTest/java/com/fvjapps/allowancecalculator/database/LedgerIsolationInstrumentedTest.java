package com.fvjapps.allowancecalculator.database;

import android.content.Context;

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
