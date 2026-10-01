package com.fvjapps.allowancecalculator.viewmodels;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.fvjapps.allowancecalculator.entities.LedgerEntity;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;
import com.fvjapps.allowancecalculator.repository.ColorSchemeRepository;
import com.fvjapps.allowancecalculator.repository.LedgerRepository;
import com.fvjapps.allowancecalculator.entities.ColorSchemeEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class LedgerViewModel extends ViewModel {
    private static final String PREF_SELECTED_LEDGER_ID = "selected_ledger_id";

    private final LedgerRepository ledgerRepository;
    private final ColorSchemeRepository colorSchemeRepository;
    private final SharedPreferences preferences;
    private final ExecutorService databaseExecutor;
    private final MutableLiveData<Long> selectedLedgerId = new MutableLiveData<>();
    private final LiveData<List<LedgerEntity>> ledgers;
    private final LiveData<LedgerEntity> selectedLedger;
    private final LiveData<List<ColorSchemeEntity>> colorSchemes;
    private final LiveData<ColorSchemeEntity> selectedColorScheme;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private boolean initializationStarted;

    public LedgerViewModel(
            @NonNull LedgerRepository ledgerRepository,
            @NonNull ColorSchemeRepository colorSchemeRepository,
            @NonNull SharedPreferences preferences
    ) {
        this.ledgerRepository = ledgerRepository;
        this.colorSchemeRepository = colorSchemeRepository;
        this.preferences = preferences;
        this.databaseExecutor = ExecutorManager.getInstance().getDbExec();
        this.ledgers = ledgerRepository.observeLedgers();
        this.colorSchemes = colorSchemeRepository.observeColorSchemes();
        this.selectedLedger = Transformations.switchMap(
                selectedLedgerId,
                ledgerRepository::observeLedger
        );
        this.selectedColorScheme = Transformations.switchMap(
                selectedLedger,
                ledger -> ledger == null || ledger.getColorSchemeId() == null
                        ? new MutableLiveData<>(null)
                        : colorSchemeRepository.observeColorScheme(ledger.getColorSchemeId())
        );
    }

    public LiveData<List<LedgerEntity>> getLedgers() {
        return ledgers;
    }

    public LiveData<Long> getSelectedLedgerId() {
        return selectedLedgerId;
    }

    public LiveData<LedgerEntity> getSelectedLedger() {
        return selectedLedger;
    }

    public LiveData<List<ColorSchemeEntity>> getColorSchemes() {
        return colorSchemes;
    }

    public LiveData<ColorSchemeEntity> getSelectedColorScheme() {
        return selectedColorScheme;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void initialize(Long restoredLedgerId) {
        if (initializationStarted) {
            return;
        }
        initializationStarted = true;
        databaseExecutor.execute(() -> {
            try {
                List<LedgerEntity> allLedgers = ledgerRepository.getLedgersSync();
                long preferredId = restoredLedgerId != null
                        ? restoredLedgerId
                        : preferences.getLong(PREF_SELECTED_LEDGER_ID, -1L);
                LedgerEntity selected = findLedger(allLedgers, preferredId);
                if (selected == null && !allLedgers.isEmpty()) {
                    selected = allLedgers.get(0);
                }
                if (selected == null) {
                    long createdId = ledgerRepository.createLedger("Allowance", "", null);
                    selected = ledgerRepository.getLedgerSync(createdId);
                }
                setSelectedLedger(selected.getId());
            } catch (RuntimeException exception) {
                error.postValue("Unable to load ledgers: " + exception.getMessage());
            }
        });
    }

    public Long getSelectedLedgerIdValue() {
        return selectedLedgerId.getValue();
    }

    public void selectLedger(long ledgerId) {
        if (ledgerId <= 0) {
            error.setValue("Unable to select ledger: invalid ledger ID");
            return;
        }
        databaseExecutor.execute(() -> {
            try {
                LedgerEntity ledger = ledgerRepository.getLedgerSync(ledgerId);
                setSelectedLedger(ledger.getId());
            } catch (RuntimeException exception) {
                error.postValue("Unable to select ledger: " + exception.getMessage());
            }
        });
    }

    public void createLedger(String name, String description, Long colorSchemeId) {
        databaseExecutor.execute(() -> {
            try {
                long newLedgerId = ledgerRepository.createLedger(
                        name,
                        description,
                        colorSchemeId
                );
                LedgerEntity createdLedger = ledgerRepository.getLedgerSync(newLedgerId);
                setSelectedLedger(createdLedger.getId());
            } catch (RuntimeException exception) {
                error.postValue("Unable to create ledger: " + exception.getMessage());
            }
        });
    }

    private LedgerEntity findLedger(List<LedgerEntity> ledgers, long ledgerId) {
        for (LedgerEntity ledger : ledgers) {
            if (ledger.getId() == ledgerId) {
                return ledger;
            }
        }
        return null;
    }

    private void setSelectedLedger(long ledgerId) {
        preferences.edit().putLong(PREF_SELECTED_LEDGER_ID, ledgerId).apply();
        selectedLedgerId.postValue(ledgerId);
        error.postValue(null);
    }

}
