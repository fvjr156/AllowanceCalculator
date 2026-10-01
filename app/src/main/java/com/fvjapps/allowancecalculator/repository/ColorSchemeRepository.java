package com.fvjapps.allowancecalculator.repository;

import androidx.lifecycle.LiveData;

import com.fvjapps.allowancecalculator.dao.ColorSchemeDao;
import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.entities.ColorSchemeEntity;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ColorSchemeRepository {
    private final ColorSchemeDao colorSchemeDao;
    private final ExecutorService databaseExecutor;

    public ColorSchemeRepository(AppDatabase database) {
        this(database, ExecutorManager.getInstance().getDbExec());
    }

    public ColorSchemeRepository(AppDatabase database, ExecutorService databaseExecutor) {
        if (database == null || databaseExecutor == null) {
            throw new IllegalArgumentException("Database and executor are required");
        }
        this.colorSchemeDao = database.colorSchemeDao();
        this.databaseExecutor = databaseExecutor;
    }

    public LiveData<List<ColorSchemeEntity>> observeColorSchemes() {
        return colorSchemeDao.getAll();
    }

    public LiveData<ColorSchemeEntity> observeColorScheme(long colorSchemeId) {
        return colorSchemeDao.getById(colorSchemeId);
    }

    public List<ColorSchemeEntity> getColorSchemesSync() {
        return colorSchemeDao.getAllSync();
    }

    public ColorSchemeEntity getColorSchemeSync(long colorSchemeId) {
        ColorSchemeEntity colorScheme = colorSchemeDao.getByIdSync(colorSchemeId);
        if (colorScheme == null) {
            throw new IllegalArgumentException(
                    "Color scheme does not exist: " + colorSchemeId
            );
        }
        return colorScheme;
    }

    public Future<List<ColorSchemeEntity>> getColorSchemesAsync() {
        return databaseExecutor.submit(this::getColorSchemesSync);
    }

    public Future<ColorSchemeEntity> getColorSchemeAsync(long colorSchemeId) {
        return databaseExecutor.submit(() -> getColorSchemeSync(colorSchemeId));
    }
}
