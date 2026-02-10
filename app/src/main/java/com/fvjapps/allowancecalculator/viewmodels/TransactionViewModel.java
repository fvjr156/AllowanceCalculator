package com.fvjapps.allowancecalculator.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;

import java.util.List;

public class TransactionViewModel extends ViewModel {
    private final TransactionRepository transactionRepository;
    private final LiveData<List<TransactionEntity>> transactions;

    private TransactionEntity lastDeletedEntity = null;

    public TransactionViewModel(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
        this.transactions = transactionRepository.observeActiveOrdered();
    }

    public LiveData<List<TransactionEntity>> getTransactions() {
        return transactions;
    }

    public void add(TransactionEntity entity) {
        transactionRepository.insert(entity);
    }

    public void delete(TransactionEntity entity) {
        lastDeletedEntity = entity;
        transactionRepository.softDelete(entity.transactionId);
    }

    public void undoDelete() {
        if (lastDeletedEntity != null) {
            transactionRepository.restore(lastDeletedEntity.transactionId);
            lastDeletedEntity = null;
        }
    }
}
