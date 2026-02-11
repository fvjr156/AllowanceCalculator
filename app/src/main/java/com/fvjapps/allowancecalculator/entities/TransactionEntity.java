package com.fvjapps.allowancecalculator.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        indices = {
                @Index("createdAt"),
                @Index("isDeleted")
        }
)
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public long transactionId;
    public String label;
    @NonNull
    public String type;
    public double amount;
    public long createdAt;
    public boolean isDeleted = false;

    public TransactionEntity(String label, String type, double amount, long createdAt) {
        this.label = label;
        this.type = type;
        this.amount = amount;
        this.createdAt = createdAt;
    }
}
