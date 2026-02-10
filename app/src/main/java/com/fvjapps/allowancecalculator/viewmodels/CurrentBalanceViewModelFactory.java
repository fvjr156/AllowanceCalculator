package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fvjapps.allowancecalculator.repository.TransactionRepository;

public class CurrentBalanceViewModelFactory implements ViewModelProvider.Factory {
    private final TransactionRepository transactionRepository;

    public CurrentBalanceViewModelFactory(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CurrentBalanceViewModel.class)) {
            return (T) new CurrentBalanceViewModel(transactionRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class!");
    }
}
