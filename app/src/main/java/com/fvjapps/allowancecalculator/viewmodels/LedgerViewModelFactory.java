package com.fvjapps.allowancecalculator.viewmodels;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fvjapps.allowancecalculator.repository.LedgerRepository;

public class LedgerViewModelFactory implements ViewModelProvider.Factory {
    private final LedgerRepository ledgerRepository;
    private final SharedPreferences preferences;

    public LedgerViewModelFactory(
            @NonNull LedgerRepository ledgerRepository,
            @NonNull SharedPreferences preferences
    ) {
        this.ledgerRepository = ledgerRepository;
        this.preferences = preferences;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == LedgerViewModel.class) {
            return modelClass.cast(new LedgerViewModel(ledgerRepository, preferences));
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
