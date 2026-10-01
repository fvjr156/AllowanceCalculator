package com.fvjapps.allowancecalculator.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        indices = {
                @Index("ledger_id"),
                @Index("created_at"),
                @Index("is_void")
        },
        foreignKeys = @ForeignKey(
                entity = LedgerEntity.class,
                parentColumns = "id",
                childColumns = "ledger_id",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
        )
)
public class TransactionEntity {

    public static final int TYPE_ALLOWANCE = 0;
    public static final int TYPE_EXPENSE = 1;

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "ledger_id")
    private long ledgerId;

    @ColumnInfo(name = "is_void")
    private boolean isVoid;

    private int type;

    @NonNull
    private String name;

    private double amount;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public TransactionEntity(
            long ledgerId,
            @NonNull String name,
            double amount,
            int type
    ) {
        this.ledgerId = ledgerId;
        this.name = name;
        this.amount = amount;
        this.type = type;
        this.isVoid = false;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public void setVoid(boolean aVoid) {
        isVoid = aVoid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
