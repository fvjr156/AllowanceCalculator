package com.fvjapps.allowancecalculator.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fvjapps.allowancecalculator.entities.ColorSchemeEntity;

import java.util.List;

@Dao
public interface ColorSchemeDao {
    @Insert
    long insert(ColorSchemeEntity en);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIfAbsent(ColorSchemeEntity scheme);
    @Update
    void update(ColorSchemeEntity en);
    @Delete
    void delete(ColorSchemeEntity en);

    @Query("""
        SELECT *
        FROM color_schemes
        ORDER BY id ASC
    """)
    LiveData<List<ColorSchemeEntity>> getAll();

    @Query("""
        SELECT *
        FROM color_schemes
        WHERE id = :id
    """)
    LiveData<ColorSchemeEntity> getById(long id);
}
