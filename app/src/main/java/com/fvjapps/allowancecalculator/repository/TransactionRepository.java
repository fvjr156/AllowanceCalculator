package com.fvjapps.allowancecalculator.repository;

import androidx.lifecycle.LiveData;

import com.fvjapps.allowancecalculator.dao.LedgerDao;
import com.fvjapps.allowancecalculator.dao.TransactionDao;
import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.entities.LedgerEntity;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TransactionRepository {

    private final AppDatabase database;
    private final TransactionDao transactionDao;
    private final LedgerDao ledgerDao;
    private final ExecutorService databaseExecutor;

    public TransactionRepository(AppDatabase database) {
        this(database, ExecutorManager.getInstance().getDbExec());
    }

    public TransactionRepository(
            AppDatabase database,
            ExecutorService databaseExecutor
    ) {
        if (database == null || databaseExecutor == null) {
            throw new IllegalArgumentException("Database and executor are required");
        }
        this.database = database;
        this.transactionDao = database.transactionDao();
        this.ledgerDao = database.ledgerDao();
        this.databaseExecutor = databaseExecutor;
    }

    private double getTransactionDelta(TransactionEntity transaction) {
        return switch (transaction.getType()) {
            case TransactionEntity.TYPE_ALLOWANCE -> transaction.getAmount();
            case TransactionEntity.TYPE_EXPENSE -> -transaction.getAmount();
            default -> throw new IllegalArgumentException(
                    "Unsupported transaction type: " + transaction.getType()
            );
        };
    }

    public void createTransaction(
            long ledgerId,
            String name,
            double amount,
            int type
    ) {
        insert(new TransactionEntity(ledgerId, name, amount, type));
    }

    public void insert(TransactionEntity transaction) {
        validateTransaction(transaction);
        database.runInTransaction(() -> {
            LedgerEntity ledger = requireLedger(transaction.getLedgerId());
            transactionDao.insert(transaction);
            ledgerDao.updateRunningBalance(
                    ledger.getId(),
                    ledger.getRunningBalance() + getTransactionDelta(transaction)
            );
        });
    }

    public Future<?> insertAsync(TransactionEntity transaction) {
        return databaseExecutor.submit(() -> insert(transaction));
    }

    public Future<?> createTransactionAsync(
            long ledgerId,
            String name,
            double amount,
            int type
    ) {
        return databaseExecutor.submit(
                () -> createTransaction(ledgerId, name, amount, type)
        );
    }

    public LiveData<List<TransactionEntity>> observeActiveOrdered(long ledgerId) {
        return transactionDao.getActiveTransactions(ledgerId);
    }

    public LiveData<List<TransactionEntity>> observeHistory(long ledgerId) {
        return transactionDao.getAllTransactions(ledgerId);
    }

    public List<TransactionEntity> getActiveTransactionsSync(long ledgerId) {
        requireLedger(ledgerId);
        return transactionDao.getActiveTransactionsSync(ledgerId);
    }

    public Future<List<TransactionEntity>> getActiveTransactionsAsync(long ledgerId) {
        return databaseExecutor.submit(() -> getActiveTransactionsSync(ledgerId));
    }

    public List<TransactionEntity> getAllTransactionsSync(long ledgerId) {
        requireLedger(ledgerId);
        return transactionDao.getAllTransactionsSync(ledgerId);
    }

    public Future<List<TransactionEntity>> getAllTransactionsAsync(long ledgerId) {
        return databaseExecutor.submit(() -> getAllTransactionsSync(ledgerId));
    }

    public void voidTransaction(long transactionId) {
        database.runInTransaction(() -> {
            TransactionEntity transaction =
                    transactionDao.getByIdSync(transactionId);

            if (transaction == null || transaction.isVoid()) {
                return;
            }
            LedgerEntity ledger = requireLedger(transaction.getLedgerId());
            transactionDao.voidTransaction(transactionId);
            ledgerDao.updateRunningBalance(
                    ledger.getId(),
                    ledger.getRunningBalance() - getTransactionDelta(transaction)
            );
        });
    }

    public Future<?> voidTransactionAsync(long transactionId) {
        return databaseExecutor.submit(() -> voidTransaction(transactionId));
    }

    public void restoreTransaction(long transactionId) {
        database.runInTransaction(() -> {
            TransactionEntity transaction =
                    transactionDao.getByIdSync(transactionId);

            if (transaction == null || !transaction.isVoid()) {
                return;
            }
            LedgerEntity ledger = requireLedger(transaction.getLedgerId());
            transactionDao.restoreTransaction(transactionId);
            ledgerDao.updateRunningBalance(
                    ledger.getId(),
                    ledger.getRunningBalance() + getTransactionDelta(transaction)
            );
        });
    }

    public Future<?> restoreTransactionAsync(long transactionId) {
        return databaseExecutor.submit(() -> restoreTransaction(transactionId));
    }

    public void recalculateRunningBalance(long ledgerId) {
        database.runInTransaction(() -> {
            requireLedger(ledgerId);
            double balance = 0.0;
            for (TransactionEntity transaction : transactionDao.getActiveTransactionsSync(ledgerId)) {
                balance += getTransactionDelta(transaction);
            }
            ledgerDao.updateRunningBalance(ledgerId, balance);
        });
    }

    public Future<?> recalculateRunningBalanceAsync(long ledgerId) {
        return databaseExecutor.submit(() -> recalculateRunningBalance(ledgerId));
    }

    public LiveData<Double> observeRunningBalance(long ledgerId) {
        return ledgerDao.observeRunningBalance(ledgerId);
    }

    public List<TransactionEntity> exportAllData(long ledgerId) {
        requireLedger(ledgerId);
        return transactionDao.exportAllData(ledgerId);
    }

    public List<TransactionEntity> exportAllActiveData(long ledgerId) {
        requireLedger(ledgerId);
        return transactionDao.exportAllActiveData(ledgerId);
    }

    public Future<List<TransactionEntity>> exportAllDataAsync(long ledgerId) {
        return databaseExecutor.submit(() -> exportAllData(ledgerId));
    }

    public Future<List<TransactionEntity>> exportAllActiveDataAsync(long ledgerId) {
        return databaseExecutor.submit(() -> exportAllActiveData(ledgerId));
    }

    private LedgerEntity requireLedger(long ledgerId) {
        LedgerEntity ledger = ledgerDao.getByIdSync(ledgerId);
        if (ledger == null) {
            throw new IllegalArgumentException("Ledger does not exist: " + ledgerId);
        }
        return ledger;
    }

    private void validateTransaction(TransactionEntity transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction must not be null");
        }
        if (transaction.getLedgerId() <= 0) {
            throw new IllegalArgumentException("Transaction must reference a valid ledger");
        }
        if (transaction.getName() == null || transaction.getName().isBlank()) {
            throw new IllegalArgumentException("Transaction name must not be blank");
        }
        if (!Double.isFinite(transaction.getAmount()) || transaction.getAmount() < 0) {
            throw new IllegalArgumentException("Transaction amount must be finite and non-negative");
        }
        getTransactionDelta(transaction);
    }
}