package com.fvjapps.allowancecalculator.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.fvjapps.allowancecalculator.databinding.AddTransactionDialogBinding;

import java.util.Objects;

public class AddTransactionDialogFragment extends DialogFragment {
    public interface OnAddTransactionListener {
        void onTransactionAdded(String labeltxt, double amount, TransactionType type);
    }

    public enum TransactionType {
        ALLOWANCE,
        EXPENSE
    }

    private OnAddTransactionListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAddTransactionListener) {
            listener = (OnAddTransactionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement AddTransactionDialogFragment.OnAddTransactionListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.setCancelable(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        AddTransactionDialogBinding binding = AddTransactionDialogBinding.inflate(inflater);
        builder.setView(binding.getRoot())
                .setTitle("Add Transaction");
        Dialog d = builder.create();
        d.setCanceledOnTouchOutside(true);

        binding.btnAllowanceTransaction.setOnClickListener(v -> {
            String label = Objects.requireNonNull(binding.transactionLabel.getText()).toString();
            String amountstr = Objects.requireNonNull(binding.transactionAmount.getText()).toString();
            if (amountstr.isBlank()) {
                binding.transactionAmount.setError("This field cannot be empty.");
            } else {
                binding.transactionAmount.setError(null);
                double amount = Double.parseDouble(amountstr);
                if (label.isBlank()) {
                    binding.transactionLabel.setError("This field cannot be empty.");
                } else {
                    binding.transactionLabel.setError(null);
                    listener.onTransactionAdded(label, amount, TransactionType.ALLOWANCE);
                    d.dismiss();
                }
            }
        });

        binding.btnExpenseTransaction.setOnClickListener(v -> {
            String label = Objects.requireNonNull(binding.transactionLabel.getText()).toString();
            String amountstr = Objects.requireNonNull(binding.transactionAmount.getText()).toString();
            if (amountstr.isBlank()) {
                binding.transactionAmount.setError("This field cannot be empty.");
            } else {
                binding.transactionAmount.setError(null);
                double amount = Double.parseDouble(amountstr);
                if (label.isBlank()) {
                    binding.transactionLabel.setError("This field cannot be empty.");
                } else {
                    binding.transactionLabel.setError(null);
                    listener.onTransactionAdded(label, amount, TransactionType.EXPENSE);
                    d.dismiss();
                }
            }
        });
        return d;
    }
}
