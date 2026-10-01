package com.fvjapps.allowancecalculator.repository;

import androidx.lifecycle.LiveData;

import com.fvjapps.allowancecalculator.dao.ColorSchemeDao;
import com.fvjapps.allowancecalculator.dao.LedgerDao;
import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.entities.ColorSchemeEntity;
import com.fvjapps.allowancecalculator.entities.LedgerEntity;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class LedgerRepository {
    private final AppDatabase database;
    private final LedgerDao ledgerDao;
    private final ColorSchemeDao colorSchemeDao;
    private final ExecutorService databaseExecutor;

    public LedgerRepository(AppDatabase database) {
        this(database, ExecutorManager.getInstance().getDbExec());
    }

    public LedgerRepository(AppDatabase database, ExecutorService databaseExecutor) {
        if (database == null || databaseExecutor == null) {
            throw new IllegalArgumentException("Database and executor are required");
        }
        this.database = database;
        this.ledgerDao = database.ledgerDao();
        this.colorSchemeDao = database.colorSchemeDao();
        this.databaseExecutor = databaseExecutor;
    }

    public LiveData<List<LedgerEntity>> observeLedgers() {
        return ledgerDao.getAll();
    }

    public LiveData<LedgerEntity> observeLedger(long ledgerId) {
        return ledgerDao.getById(ledgerId);
    }

    public LiveData<Double> observeRunningBalance(long ledgerId) {
        return ledgerDao.observeRunningBalance(ledgerId);
    }

    public LedgerEntity getLedgerSync(long ledgerId) {
        return requireLedger(ledgerId);
    }

    public Future<LedgerEntity> getLedgerAsync(long ledgerId) {
        return databaseExecutor.submit(() -> getLedgerSync(ledgerId));
    }

    public List<LedgerEntity> getLedgersSync() {
        return ledgerDao.getAllSync();
    }

    public Future<List<LedgerEntity>> getLedgersAsync() {
        return databaseExecutor.submit(this::getLedgersSync);
    }

    public LedgerEntity getFirstLedgerSync() {
        return ledgerDao.getFirstSync();
    }

    public Future<LedgerEntity> getFirstLedgerAsync() {
        return databaseExecutor.submit(this::getFirstLedgerSync);
    }

    public double getRunningBalanceSync(long ledgerId) {
        return requireLedger(ledgerId).getRunningBalance();
    }

    public Future<Double> getRunningBalanceAsync(long ledgerId) {
        return databaseExecutor.submit(() -> getRunningBalanceSync(ledgerId));
    }

    public Future<Long> createLedgerAsync(String name, String description, Long colorSchemeId) {
        return databaseExecutor.submit(() -> createLedger(name, description, colorSchemeId));
    }

    public long createLedger(String name, String description, Long colorSchemeId) {
        validateLedgerName(name);
        LedgerEntity ledger = new LedgerEntity(name.trim(), description, colorSchemeId);
        return database.runInTransaction(() -> {
            validateColorScheme(colorSchemeId);
            return ledgerDao.insert(ledger);
        });
    }

    public Future<?> updateLedgerAsync(LedgerEntity ledger) {
        return databaseExecutor.submit(() -> updateLedger(ledger));
    }

    public void updateLedger(LedgerEntity ledger) {
        if (ledger == null) {
            throw new IllegalArgumentException("Ledger must not be null");
        }
        validateLedgerName(ledger.getName());
        ledger.setName(ledger.getName().trim());
        database.runInTransaction(() -> {
            requireLedger(ledger.getId());
            validateColorScheme(ledger.getColorSchemeId());
            ledgerDao.update(ledger);
        });
    }

    public Future<?> deleteLedgerAsync(long ledgerId) {
        return databaseExecutor.submit(() -> deleteLedger(ledgerId));
    }

    public void deleteLedger(long ledgerId) {
        database.runInTransaction(() -> ledgerDao.delete(requireLedger(ledgerId)));
    }

    private LedgerEntity requireLedger(long ledgerId) {
        LedgerEntity ledger = ledgerDao.getByIdSync(ledgerId);
        if (ledger == null) {
            throw new IllegalArgumentException("Ledger does not exist: " + ledgerId);
        }
        return ledger;
    }

    private void validateColorScheme(Long colorSchemeId) {
        if (colorSchemeId != null && colorSchemeDao.getByIdSync(colorSchemeId) == null) {
            throw new IllegalArgumentException(
                    "Color scheme does not exist: " + colorSchemeId
            );
        }
    }

    private void validateLedgerName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ledger name must not be blank");
        }
    }
}
