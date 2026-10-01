package com.fvjapps.allowancecalculator.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ledgers",
        foreignKeys = @ForeignKey(
                entity = ColorSchemeEntity.class,
                parentColumns = "id",
                childColumns = "color_scheme_id",
                onDelete = ForeignKey.SET_NULL,
                onUpdate = ForeignKey.CASCADE
        ),
        indices = {
                @Index("color_scheme_id")
        }
)
public class LedgerEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    private String description;

    @ColumnInfo(name = "color_scheme_id")
    private Long colorSchemeId;

    @ColumnInfo(name = "running_balance")
    private double runningBalance;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getColorSchemeId() {
        return colorSchemeId;
    }

    public void setColorSchemeId(Long colorSchemeId) {
        this.colorSchemeId = colorSchemeId;
    }

    public double getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(double runningBalance) {
        this.runningBalance = runningBalance;
    }

    public LedgerEntity(
            @NonNull String name,
            String description,
            Long colorSchemeId
    ) {
        this.name = name;
        this.description = description;
        this.colorSchemeId = colorSchemeId;
        this.runningBalance = 0.0;
    }


}
