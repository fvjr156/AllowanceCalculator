package com.fvjapps.allowancecalculator.repository;

import androidx.lifecycle.LiveData;

import com.fvjapps.allowancecalculator.dao.TransactionDao;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;

import java.util.List;

public class TransactionRepository {
    private final TransactionDao transactionDao;

    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    public LiveData<List<TransactionEntity>> observeActiveOrdered() {
        return transactionDao.observeActiveOrdered();
    }

    public void insert(TransactionEntity entity) {
        ExecutorManager.getInstance().getDbExec().execute(() -> transactionDao.insert(entity));
    }

    public void softDelete(long id) {
        ExecutorManager.getInstance().getDbExec().execute(() -> transactionDao.softDeleteById(id));
    }

    public void restore(long id) {
        ExecutorManager.getInstance().getDbExec().execute(() -> transactionDao.restoreById(id));
    }
}
