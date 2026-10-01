package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;

import java.util.List;

public class TransactionViewModel extends ViewModel {
    private final TransactionRepository transactionRepository;
    private final LiveData<Long> selectedLedgerId;
    private final LiveData<List<TransactionEntity>> transactions;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private TransactionEntity lastDeletedEntity;

    public TransactionViewModel(
            @NonNull TransactionRepository transactionRepository,
            @NonNull LiveData<Long> selectedLedgerId
    ) {
        this.transactionRepository = transactionRepository;
        this.selectedLedgerId = selectedLedgerId;
        this.transactions = Transformations.switchMap(
                selectedLedgerId,
                transactionRepository::observeActiveOrdered
        );
    }

    public LiveData<List<TransactionEntity>> getTransactions() {
        return transactions;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void add(String name, double amount, int type) {
        Long ledgerId = selectedLedgerId.getValue();
        if (ledgerId == null) {
            error.setValue("Wait for a ledger to load before adding transactions.");
            return;
        }
        executeDatabaseOperation(
                () -> transactionRepository.createTransaction(ledgerId, name, amount, type),
                "Unable to add transaction",
                "Transaction added."
        );
    }

    public void delete(TransactionEntity entity) {
        Long ledgerId = selectedLedgerId.getValue();
        if (entity == null || ledgerId == null || entity.getLedgerId() != ledgerId) {
            error.setValue("The transaction does not belong to the selected ledger.");
            return;
        }
        lastDeletedEntity = entity;
        executeDatabaseOperation(
                () -> transactionRepository.voidTransaction(entity.getId()),
                "Unable to delete transaction",
                null
        );
    }

    public void undoDelete() {
        TransactionEntity deletedEntity = lastDeletedEntity;
        Long ledgerId = selectedLedgerId.getValue();
        lastDeletedEntity = null;
        if (deletedEntity == null) {
            return;
        }
        if (ledgerId == null || deletedEntity.getLedgerId() != ledgerId) {
            error.setValue("The deleted transaction is not in the selected ledger.");
            return;
        }
        executeDatabaseOperation(
                () -> transactionRepository.restoreTransaction(deletedEntity.getId()),
                "Unable to restore transaction",
                "Transaction restored."
        );
    }

    public List<TransactionEntity> exportData() {
        Long ledgerId = selectedLedgerId.getValue();
        if (ledgerId == null) {
            throw new IllegalStateException("No ledger is selected");
        }
        return transactionRepository.exportAllData(ledgerId);
    }

    public List<TransactionEntity> exportData(long ledgerId) {
        return transactionRepository.exportAllData(ledgerId);
    }

    public List<TransactionEntity> exportAllActiveData() {
        Long ledgerId = selectedLedgerId.getValue();
        if (ledgerId == null) {
            throw new IllegalStateException("No ledger is selected");
        }
        return transactionRepository.exportAllActiveData(ledgerId);
    }

    public List<TransactionEntity> exportAllActiveData(long ledgerId) {
        return transactionRepository.exportAllActiveData(ledgerId);
    }

    private void executeDatabaseOperation(
            Runnable operation,
            String failureMessage,
            String successMessage
    ) {
        ExecutorManager.getInstance().getDbExec().execute(() -> {
            try {
                operation.run();
                error.postValue(null);
                if (successMessage != null) {
                    message.postValue(successMessage);
                }
            } catch (RuntimeException exception) {
                error.postValue(failureMessage + ": " + exception.getMessage());
            }
        });
    }
}
