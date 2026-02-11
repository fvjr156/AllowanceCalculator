package com.fvjapps.allowancecalculator.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.fvjapps.allowancecalculator.entities.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("""
             SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY createdAt DESC, transactionId DESC
            """)
    List<TransactionEntity> getActiveOrdered();

    @Query("""
             SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY createdAt DESC, transactionId DESC
            """)
    LiveData<List<TransactionEntity>> observeActiveOrdered();

    @Query("""
            SELECT * FROM transactions
            WHERE createdAt <= :timestamp
            ORDER BY createdAt DESC, transactionId DESC
            """)
    List<TransactionEntity> getUpToTime(long timestamp);

    @Insert
    void insert(TransactionEntity transaction);

    @Query("DELETE FROM transactions")
    void hardDeleteAll();

    @Query("""
            DELETE FROM transactions WHERE transactionId = :id
            """)
    void hardDeleteById(long id);

    @Query("""
            UPDATE transactions
            SET isDeleted = 1
            WHERE transactionId = :id
            """)
    void softDeleteById(long id);

    @Query("""
            UPDATE transactions
            SET isDeleted = 0
            WHERE transactionId = :id
            """)
    void restoreById(long id);

    @Query("""
                SELECT transactionId, type, amount, createdAt, isDeleted, label
                FROM transactions
                ORDER BY createdAt DESC, transactionId DESC
            """)
    List<TransactionEntity> exportAllData();

}
