package com.fvjapps.allowancecalculator.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(
        tableName = "color_schemes",
        indices = @Index(value = "name", unique = true)
)
public class ColorSchemeEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(int secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getSurfaceColor() {
        return surfaceColor;
    }

    public void setSurfaceColor(int surfaceColor) {
        this.surfaceColor = surfaceColor;
    }

    public int getSurfaceElevatedColor() {
        return surfaceElevatedColor;
    }

    public void setSurfaceElevatedColor(int surfaceElevatedColor) {
        this.surfaceElevatedColor = surfaceElevatedColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    @ColumnInfo(name = "primary_color")
    private int primaryColor;

    @ColumnInfo(name = "secondary_color")
    private int secondaryColor;

    @ColumnInfo(name = "background_color")
    private int backgroundColor;

    @ColumnInfo(name = "surface_color")
    private int surfaceColor;

    @ColumnInfo(name = "surface_elevated_color")
    private int surfaceElevatedColor;

    @ColumnInfo(name = "text_color")
    private int textColor;

    @ColumnInfo(name = "border_color")
    private int borderColor;

    public ColorSchemeEntity(
            @NonNull String name,
            int primaryColor,
            int secondaryColor,
            int backgroundColor,
            int surfaceColor,
            int surfaceElevatedColor,
            int textColor,
            int borderColor
    ) {
        this.name = name;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.backgroundColor = backgroundColor;
        this.surfaceColor = surfaceColor;
        this.surfaceElevatedColor = surfaceElevatedColor;
        this.textColor = textColor;
        this.borderColor = borderColor;
    }


}
