package com.fvjapps.allowancecalculator.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.fvjapps.allowancecalculator.entities.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Query("""
        SELECT *
        FROM transactions
        WHERE ledger_id = :ledgerId
          AND is_void = 0
        ORDER BY created_at DESC, id DESC
    """)
    LiveData<List<TransactionEntity>> getActiveTransactions(
            long ledgerId
    );

    @Query("""
        SELECT *
        FROM transactions
        WHERE ledger_id = :ledgerId
          AND is_void = 0
        ORDER BY created_at ASC, id ASC
    """)
    List<TransactionEntity> getActiveTransactionsSync(
            long ledgerId
    );

    @Query("""
        SELECT *
        FROM transactions
        WHERE ledger_id = :ledgerId
        ORDER BY created_at ASC, id ASC
    """)
    List<TransactionEntity> getAllTransactionsSync(long ledgerId);

    @Query("""
        SELECT *
        FROM transactions
        WHERE ledger_id = :ledgerId
        ORDER BY created_at DESC, id DESC
    """)
    LiveData<List<TransactionEntity>> getAllTransactions(
            long ledgerId
    );

    @Query("""
        UPDATE transactions
        SET is_void = 1
        WHERE id = :transactionId
    """)
    void voidTransaction(long transactionId);

    @Query("""
        UPDATE transactions
        SET is_void = 0
        WHERE id = :transactionId
    """)
    void restoreTransaction(long transactionId);

    @Query("""
        SELECT *
        FROM transactions
        WHERE id = :transactionId
    """)
    TransactionEntity getByIdSync(long transactionId);

    @Query("""
        SELECT *
        FROM transactions
        WHERE ledger_id = :ledgerId
        ORDER BY created_at DESC, id DESC
    """)
    List<TransactionEntity> exportAllData(long ledgerId);

    @Query("""
        SELECT *
        FROM transactions
        WHERE ledger_id = :ledgerId
          AND is_void = 0
        ORDER BY created_at ASC, id ASC
    """)
    List<TransactionEntity> exportAllActiveData(long ledgerId);
}