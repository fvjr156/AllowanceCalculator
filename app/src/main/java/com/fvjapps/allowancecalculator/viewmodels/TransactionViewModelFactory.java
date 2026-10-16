package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fvjapps.allowancecalculator.repository.TransactionRepository;

import java.util.concurrent.ExecutorService;

public class TransactionViewModelFactory implements ViewModelProvider.Factory {
    private final TransactionRepository transactionRepository;
    private final LiveData<Long> selectedLedgerId;
    private final ExecutorService databaseExecutor;

    public TransactionViewModelFactory(
            @NonNull TransactionRepository transactionRepository,
            @NonNull LiveData<Long> selectedLedgerId,
            @NonNull ExecutorService databaseExecutor
    ) {
        this.transactionRepository = transactionRepository;
        this.selectedLedgerId = selectedLedgerId;
        this.databaseExecutor = databaseExecutor;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == TransactionViewModel.class) {
            return modelClass.cast(
                    new TransactionViewModel(
                            transactionRepository,
                            selectedLedgerId,
                            databaseExecutor
                    )
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
