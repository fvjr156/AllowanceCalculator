package com.fvjapps.allowancecalculator.viewmodels;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fvjapps.allowancecalculator.repository.LedgerRepository;
import com.fvjapps.allowancecalculator.repository.ColorSchemeRepository;

public class LedgerViewModelFactory implements ViewModelProvider.Factory {
    private final LedgerRepository ledgerRepository;
    private final ColorSchemeRepository colorSchemeRepository;
    private final SharedPreferences preferences;

    public LedgerViewModelFactory(
            @NonNull LedgerRepository ledgerRepository,
            @NonNull ColorSchemeRepository colorSchemeRepository,
            @NonNull SharedPreferences preferences
    ) {
        this.ledgerRepository = ledgerRepository;
        this.colorSchemeRepository = colorSchemeRepository;
        this.preferences = preferences;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == LedgerViewModel.class) {
            return modelClass.cast(
                    new LedgerViewModel(ledgerRepository, colorSchemeRepository, preferences)
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
