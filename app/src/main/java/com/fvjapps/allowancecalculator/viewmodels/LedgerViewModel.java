package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.database.AppDatabase;
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
                transactionRepository.createTransactionAsync(
                        ledgerId,
                        name,
                        amount,
                        type
                );
    }

    public void voidTransaction(long transactionId) {
        transactionRepository.voidTransactionAsync(transactionId);
    }

    public void recalculateBalance(long ledgerId) {
        transactionRepository.recalculateRunningBalanceAsync(ledgerId);
    }
}
