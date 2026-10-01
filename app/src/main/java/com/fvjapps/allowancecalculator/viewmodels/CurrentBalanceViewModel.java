package com.fvjapps.allowancecalculator.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.repository.TransactionRepository;

public class CurrentBalanceViewModel extends ViewModel {
    private final LiveData<Double> currentBalance;

    public CurrentBalanceViewModel(
            @NonNull TransactionRepository repository,
            @NonNull LiveData<Long> selectedLedgerId
    ) {
        currentBalance = Transformations.switchMap(
                selectedLedgerId,
                repository::observeRunningBalance
        );
    }

    public LiveData<Double> getCurrentBalance() {
        return currentBalance;
    }
}
