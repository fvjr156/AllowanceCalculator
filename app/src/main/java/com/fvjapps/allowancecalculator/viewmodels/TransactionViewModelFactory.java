package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fvjapps.allowancecalculator.repository.TransactionRepository;

public class TransactionViewModelFactory implements ViewModelProvider.Factory {
    private final TransactionRepository transactionRepository;
    private final LiveData<Long> selectedLedgerId;

    public TransactionViewModelFactory(
            @NonNull TransactionRepository transactionRepository,
            @NonNull LiveData<Long> selectedLedgerId
    ) {
        this.transactionRepository = transactionRepository;
        this.selectedLedgerId = selectedLedgerId;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == TransactionViewModel.class) {
            return modelClass.cast(
                    new TransactionViewModel(transactionRepository, selectedLedgerId)
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
