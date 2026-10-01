package com.fvjapps.allowancecalculator.repository;

import androidx.lifecycle.LiveData;

import com.fvjapps.allowancecalculator.dao.LedgerDao;
import com.fvjapps.allowancecalculator.dao.TransactionDao;
import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.entities.LedgerEntity;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;

import java.util.List;

public class TransactionRepository {

    private final AppDatabase database;

    private final TransactionDao transactionDao;
    private final LedgerDao ledgerDao;

    public TransactionRepository(AppDatabase database) {
        this.database = database;
        this.transactionDao = database.transactionDao();
        this.ledgerDao = database.ledgerDao();
    }

    private double getTransactionDelta(
            TransactionEntity transaction
    ) {
        if (transaction.getType()
                == TransactionEntity.TYPE_ALLOWANCE) {

            return transaction.getAmount();

        } else {

            return -transaction.getAmount();
        }
    }

    public void createTransaction(
            long ledgerId,
            String name,
            double amount,
            int type
    ) {
        database.runInTransaction(() -> {
            LedgerEntity ledger = requireLedger(ledgerId);

            TransactionEntity transaction =
                    new TransactionEntity(
                            ledgerId,
                            name,
                            amount,
                            type
                    );

            transactionDao.insert(transaction);

            double delta =
                    getTransactionDelta(transaction);

            double newBalance =
                    ledger.getRunningBalance() + delta;

            ledgerDao.updateRunningBalance(
                    ledgerId,
                    newBalance
            );
        });
    }

    public void insert(TransactionEntity transaction) {
        createTransaction(
                transaction.getLedgerId(),
                transaction.getName(),
                transaction.getAmount(),
                transaction.getType()
        );
    }

    public LiveData<List<TransactionEntity>> observeActiveOrdered(long ledgerId) {
        return transactionDao.getActiveTransactions(ledgerId);
    }

    public void voidTransaction(long transactionId) {

        database.runInTransaction(() -> {

            TransactionEntity transaction =
                    transactionDao.getByIdSync(transactionId);

            if (transaction == null || transaction.isVoid()) {
                return;
            }

            LedgerEntity ledger = requireLedger(transaction.getLedgerId());

            double delta =
                    getTransactionDelta(transaction);

            double newBalance =
                    ledger.getRunningBalance() - delta;

            transactionDao.voidTransaction(transactionId);

            ledgerDao.updateRunningBalance(
                    ledger.getId(),
                    newBalance
            );
        });
    }

    public void restoreTransaction(long transactionId) {

        database.runInTransaction(() -> {

            TransactionEntity transaction =
                    transactionDao.getByIdSync(transactionId);

            if (transaction == null || !transaction.isVoid()) {
                return;
            }

            LedgerEntity ledger = requireLedger(transaction.getLedgerId());

            double delta =
                    getTransactionDelta(transaction);

            double newBalance =
                    ledger.getRunningBalance() + delta;

            transactionDao.restoreTransaction(transactionId);

            ledgerDao.updateRunningBalance(
                    ledger.getId(),
                    newBalance
            );
        });
    }

    public void recalculateRunningBalance(long ledgerId) {

        database.runInTransaction(() -> {
            requireLedger(ledgerId);

            List<TransactionEntity> transactions =
                    transactionDao
                            .getActiveTransactionsSync(ledgerId);

            double balance = 0.0;

            for (TransactionEntity transaction : transactions) {

                balance +=
                        getTransactionDelta(transaction);
            }

            ledgerDao.updateRunningBalance(
                    ledgerId,
                    balance
            );
        });
    }

    public List<TransactionEntity> exportAllData(long ledgerId) {
        return transactionDao.exportAllData(ledgerId);
    }

    public List<TransactionEntity> exportAllActiveData(long ledgerId) {
        return transactionDao.exportAllActiveData(ledgerId);
    }

    private LedgerEntity requireLedger(long ledgerId) {
        LedgerEntity ledger = ledgerDao.getByIdSync(ledgerId);
        if (ledger == null) {
            throw new IllegalArgumentException("Ledger does not exist: " + ledgerId);
        }
        return ledger;
    }
}