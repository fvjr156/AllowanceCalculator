package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;

public class LedgerViewModel extends ViewModel {
    private final TransactionRepository transactionRepository;

    public LedgerViewModel(@NonNull AppDatabase database) {
        transactionRepository = new TransactionRepository(database);
    }

    public void createTransaction(
            long ledgerId,
            String name,
            double amount,
            int type
    ) {
        ExecutorManager.getInstance()
                .getDbExec()
                .execute(() ->
                        transactionRepository.createTransaction(
                                ledgerId,
                                name,
                                amount,
                                type
                        )
                );
    }

    public void voidTransaction(long transactionId) {
        ExecutorManager.getInstance()
                .getDbExec()
                .execute(() ->
                        transactionRepository
                                .voidTransaction(transactionId)
                );
    }

    public void recalculateBalance(long ledgerId) {
        ExecutorManager.getInstance()
                .getDbExec()
                .execute(() ->
                        transactionRepository
                                .recalculateRunningBalance(
                                        ledgerId
                                )
                );
    }
}
