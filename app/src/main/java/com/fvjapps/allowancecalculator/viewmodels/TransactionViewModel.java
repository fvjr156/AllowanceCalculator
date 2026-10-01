package com.fvjapps.allowancecalculator.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;

import java.util.List;

public class TransactionViewModel extends ViewModel {
    private final TransactionRepository transactionRepository;
    private final LiveData<List<TransactionEntity>> transactions;
    private final long ledgerId;

    private TransactionEntity lastDeletedEntity = null;

    public TransactionViewModel(TransactionRepository transactionRepository, long ledgerId) {
        this.transactionRepository = transactionRepository;
        this.ledgerId = ledgerId;
        this.transactions = transactionRepository.observeActiveOrdered(ledgerId);
    }

    public LiveData<List<TransactionEntity>> getTransactions() {
        return transactions;
    }

    public void add(TransactionEntity entity) {
        transactionRepository.insertAsync(entity);
    }

    public void delete(TransactionEntity entity) {
        lastDeletedEntity = entity;
        transactionRepository.voidTransactionAsync(entity.getId());
    }

    public void undoDelete() {
        if (lastDeletedEntity != null) {
            long transactionId = lastDeletedEntity.getId();
            transactionRepository.restoreTransactionAsync(transactionId);
            lastDeletedEntity = null;
        }
    }

    public List<TransactionEntity> exportData() {
        return transactionRepository.exportAllData(ledgerId);
    }

    public List<TransactionEntity> exportAllActiveData() {
        return transactionRepository.exportAllActiveData(ledgerId);
    }
}
