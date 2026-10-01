package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fvjapps.allowancecalculator.repository.TransactionRepository;

public class CurrentBalanceViewModelFactory implements ViewModelProvider.Factory {
    private final TransactionRepository transactionRepository;
    private final long ledgerId;

    public CurrentBalanceViewModelFactory(TransactionRepository transactionRepository, long ledgerId) {
        this.transactionRepository = transactionRepository;
        this.ledgerId = ledgerId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CurrentBalanceViewModel.class)) {
            return (T) new CurrentBalanceViewModel(transactionRepository, ledgerId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class!");
    }
}
