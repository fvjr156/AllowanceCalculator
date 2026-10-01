package com.fvjapps.allowancecalculator.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.fvjapps.allowancecalculator.entities.LedgerEntity;

import java.util.List;

@Dao
public interface LedgerDao {

    @Insert
    long insert(LedgerEntity ledger);

    @Update
    void update(LedgerEntity ledger);

    @Delete
    void delete(LedgerEntity ledger);

    @Query("""
        SELECT *
        FROM ledgers
        ORDER BY name ASC
    """)
    LiveData<List<LedgerEntity>> getAll();

    @Query("""
        SELECT *
        FROM ledgers
        WHERE id = :id
    """)
    LiveData<LedgerEntity> getById(long id);

    @Query("""
        SELECT *
        FROM ledgers
        WHERE id = :id
    """)
    LedgerEntity getByIdSync(long id);

    @Query("""
        SELECT *
        FROM ledgers
        ORDER BY id ASC
        LIMIT 1
    """)
    LedgerEntity getFirstSync();

    @Query("""
        UPDATE ledgers
        SET running_balance = :balance
        WHERE id = :ledgerId
    """)
    void updateRunningBalance(long ledgerId, double balance);
}
