package com.fvjapps.allowancecalculator.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;

import java.util.List;

public class CurrentBalanceViewModel extends ViewModel {
    private final MediatorLiveData<Double> currentBalance = new MediatorLiveData<>();

    public CurrentBalanceViewModel(TransactionRepository repository) {
        currentBalance.addSource(
                repository.observeActiveOrdered(),
                this::recalculateBalance
        );
    }

    private void recalculateBalance(List<TransactionEntity> transactions) {
        double balance = 0.0;
        if (transactions != null) {
            for (TransactionEntity transaction : transactions) {
                switch(transaction.type) {
                    case "IN":
                        balance += transaction.amount;
                        break;
                    case "OUT":
                        balance -= transaction.amount;
                        break;
                }
            }
        }
        currentBalance.setValue(balance);
    }

    public LiveData<Double> getCurrentBalance() {
        return currentBalance;
    }
}
