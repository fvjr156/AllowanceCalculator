package com.fvjapps.allowancecalculator.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.fvjapps.allowancecalculator.R;
import com.fvjapps.allowancecalculator.databinding.ItemTransactionBinding;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionEntity> transactionEntities = new ArrayList<>();
    private Context context;

    public TransactionAdapter(Context c) {
        this.context = c;
    }

    public void setEntities(List<TransactionEntity> tx) {
        this.transactionEntities = tx;
        notifyDataSetChanged();
    }

    public TransactionEntity getEntity(int pos) {
        return transactionEntities.get(pos);
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(inflater, parent, false);
        return new TransactionViewHolder(binding);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int pos) {
        TransactionEntity tx = transactionEntities.get(pos);
        holder.binding.itemLabel.setText(tx.label);
        holder.binding.itemAmount.setText(String.format("%.2f", tx.amount));
        String typey = "";
        Drawable icony = AppCompatResources.getDrawable(context, R.drawable.baseline_add_24);;
        switch(tx.type) {
            case "IN":
                typey = "ALLOWANCE";
                break;
            case "OUT":
                typey = "EXPENSE";
                icony = AppCompatResources.getDrawable(context, R.drawable.baseline_remove_24);
                break;
        }
        holder.binding.transactionType.setText(typey);
        holder.binding.transactionIcon.setImageDrawable(icony);
    }

    @Override
    public int getItemCount() {
        return transactionEntities.size();
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder {
        public ItemTransactionBinding binding;
        public TransactionViewHolder(@NonNull ItemTransactionBinding b) {
            super(b.getRoot());
            this.binding = b;
        }
    }
}
