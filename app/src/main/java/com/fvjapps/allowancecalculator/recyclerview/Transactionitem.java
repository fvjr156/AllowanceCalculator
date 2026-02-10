package com.fvjapps.allowancecalculator.recyclerview;

import com.fvjapps.allowancecalculator.fragments.AddTransactionDialogFragment;

public class Transactionitem {
    String label;
    AddTransactionDialogFragment.TransactionType type;
    double amount;

    public Transactionitem(String l, double d, AddTransactionDialogFragment.TransactionType t) {
        this.label = l;
        this.amount = d;
        this.type = t;
    }
}
