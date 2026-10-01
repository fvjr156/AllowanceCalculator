package com.fvjapps.allowancecalculator.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fvjapps.allowancecalculator.R;
import com.fvjapps.allowancecalculator.adapters.TransactionsPrintAdapter;
import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.databinding.ActivityMainBinding;
import com.fvjapps.allowancecalculator.entities.ColorSchemeEntity;
import com.fvjapps.allowancecalculator.entities.LedgerEntity;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.fragments.AddTransactionDialogFragment;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;
import com.fvjapps.allowancecalculator.adapters.TransactionAdapter;
import com.fvjapps.allowancecalculator.misc.MillisConv;
import com.fvjapps.allowancecalculator.misc.ExportFileName;
import com.fvjapps.allowancecalculator.repository.ColorSchemeRepository;
import com.fvjapps.allowancecalculator.repository.LedgerRepository;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;
import com.fvjapps.allowancecalculator.viewmodels.CurrentBalanceViewModel;
import com.fvjapps.allowancecalculator.viewmodels.CurrentBalanceViewModelFactory;
import com.fvjapps.allowancecalculator.viewmodels.LedgerViewModel;
import com.fvjapps.allowancecalculator.viewmodels.LedgerViewModelFactory;
import com.fvjapps.allowancecalculator.viewmodels.TransactionViewModel;
import com.fvjapps.allowancecalculator.viewmodels.TransactionViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.navigation.NavigationView;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AddTransactionDialogFragment.OnAddTransactionListener {

    private static final String STATE_SELECTED_LEDGER_ID = "selected_ledger_id";

    ActivityMainBinding binding;
    LedgerViewModel ledgerViewModel;
    TransactionViewModel transactionViewModel;
    TransactionAdapter adapter;
    RecyclerView rview;
    private Long selectedLedgerId;
    private String selectedLedgerName;
    private List<ColorSchemeEntity> availableColorSchemes = new ArrayList<>();
    private Long pendingCsvLedgerId;
    private String pendingCsvLedgerName;
    private final Map<Integer, Long> drawerLedgerIds = new HashMap<>();
    private static final int DRAWER_LEDGER_ITEM_BASE = 1000;

    private ActivityResultLauncher<Intent> createCsvLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            if (pendingCsvLedgerId != null && pendingCsvLedgerName != null) {
                                exportCsvToUri(
                                        uri,
                                        pendingCsvLedgerId,
                                        pendingCsvLedgerName
                                );
                            }
                        }
                    }
                }
            });

    private void exportAsPdf() {
        if (selectedLedgerId == null) {
            Snackbar.make(binding.main, "Wait for a ledger to load before exporting.", Snackbar.LENGTH_LONG).show();
            return;
        }
        PrintManager man = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        long exportLedgerId = selectedLedgerId;
        String exportLedgerName = selectedLedgerName;
        if (exportLedgerName == null) {
            Snackbar.make(binding.main, "Wait for the selected ledger to load before exporting.", Snackbar.LENGTH_LONG).show();
            return;
        }

        String jobName = getString(R.string.app_name) + " - " + exportLedgerName;
        ExecutorManager.getInstance().getDbExec().execute(() -> {
            try {
                List<TransactionEntity> entityList =
                        transactionViewModel.exportAllActiveData(exportLedgerId);
                if (entityList.stream().anyMatch(
                        transaction -> transaction.getLedgerId() != exportLedgerId
                )) {
                    throw new IllegalStateException(
                            "Export query returned a transaction from another ledger."
                    );
                }
                runOnUiThread(() -> man.print(
                        jobName,
                        new TransactionsPrintAdapter(
                                this,
                                entityList,
                                exportLedgerName,
                                new TransactionsPrintAdapter.TransactionPrintListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() ->
                                    Snackbar.make(binding.main, "Export success", Snackbar.LENGTH_LONG).show()
                            );
                        }

                        @Override
                        public void onFail(String error) {
                            runOnUiThread(() ->
                                    Snackbar.make(binding.main, "PDF export failed: " + error, Snackbar.LENGTH_LONG).show()
                            );
                        }
                    }),
                    null
                ));
            } catch (RuntimeException exception) {
                runOnUiThread(() ->
                        Snackbar.make(
                                binding.main,
                                "PDF export failed: " + exception.getMessage(),
                                Snackbar.LENGTH_LONG
                        ).show()
                );
            }
        });
    }

    private void exportCsvToUri(
            @NonNull Uri uri,
            long exportLedgerId,
            @NonNull String exportLedgerName
    ) {
        ExecutorManager.getInstance().getFileExec().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream == null) {
                        throw new java.io.IOException("Unable to open the selected export destination.");
                    }
                    try (BufferedWriter bw = new BufferedWriter(
                            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
                    )) {

                        List<TransactionEntity> entityList =
                                transactionViewModel.exportData(exportLedgerId);

                        bw.write("ledger,id,type,amount,creationdate,voided,label");
                        bw.newLine();

                        for (TransactionEntity e : entityList) {
                            if (e.getLedgerId() != exportLedgerId) {
                                throw new IllegalStateException(
                                        "Export query returned a transaction from another ledger."
                                );
                            }
                            bw.write(
                                    csvField(exportLedgerName) + "," +
                                            e.getId() + "," +
                                            e.getType() + "," +
                                            e.getAmount() + "," +
                                            csvField(MillisConv.toDate(
                                                    e.getCreatedAt(),
                                                    MillisConv.DateFormat.DATABASE_STANDARD
                                            )) + "," +
                                            e.isVoid() + "," +
                                            csvField(e.getName())
                            );
                            bw.newLine();
                        }

                        bw.flush();
                    }

                    runOnUiThread(() ->
                            Snackbar.make(binding.main, "Export success", Snackbar.LENGTH_LONG).show()
                    );
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Snackbar.make(binding.main, "CSV export failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.main);
//        Objects.requireNonNull(getSupportActionBar()).hide();

        AppDatabase database = AppDatabase.getInstance(this);
        LedgerRepository ledgerRepository = new LedgerRepository(database);
        ColorSchemeRepository colorSchemeRepository = new ColorSchemeRepository(database);
        TransactionRepository transactionRepository = new TransactionRepository(database);
        SharedPreferences preferences =
                getSharedPreferences("ledger_preferences", MODE_PRIVATE);
        ledgerViewModel = new ViewModelProvider(
                this,
                new LedgerViewModelFactory(
                        ledgerRepository,
                        colorSchemeRepository,
                        preferences
                )
        ).get(LedgerViewModel.class);

        transactionViewModel = new ViewModelProvider(
                this,
                new TransactionViewModelFactory(
                        transactionRepository,
                        ledgerViewModel.getSelectedLedgerId()
                )
        ).get(TransactionViewModel.class);

        rview = binding.recyclerView;
        adapter = new TransactionAdapter(getApplicationContext());
        rview.setLayoutManager(new LinearLayoutManager(this));
        rview.setAdapter(adapter);

        CurrentBalanceViewModelFactory balanceViewModelFactory =
                new CurrentBalanceViewModelFactory(
                        transactionRepository,
                        ledgerViewModel.getSelectedLedgerId()
                );
        CurrentBalanceViewModel balanceViewModel = new ViewModelProvider(this, balanceViewModelFactory).<CurrentBalanceViewModel>get(CurrentBalanceViewModel.class);

        balanceViewModel.getCurrentBalance().observe(this, balance -> {
            if (balance == null) {
                return;
            }
            binding.txvCurrentBalancePeso.setVisibility(View.VISIBLE);
            binding.txvCurrentBalance.setText(String.format("%.2f", balance));
        });

        transactionViewModel.getTransactions().observe(this, adapter::setEntities);
        ledgerViewModel.getSelectedLedgerId().observe(this, id -> {
            selectedLedgerId = id;
            selectedLedgerName = null;
            populateLedgerDrawer(ledgerViewModel.getLedgers().getValue());
        });
        ledgerViewModel.getSelectedLedger().observe(this, ledger -> {
            if (ledger != null) {
                selectedLedgerName = ledger.getName();
                binding.toolbar.setTitle(ledger.getName());
                binding.txvCurrentTransactionsCaption.setText(
                        getString(R.string.recent_transactions_for, ledger.getName())
                );
                populateLedgerDrawer(ledgerViewModel.getLedgers().getValue());
            }
        });
        ledgerViewModel.getLedgers().observe(this, this::populateLedgerDrawer);
        ledgerViewModel.getColorSchemes().observe(this, schemes -> {
            availableColorSchemes = schemes == null ? new ArrayList<>() : schemes;
        });
        ledgerViewModel.getSelectedColorScheme().observe(this, this::applyColorScheme);
        ledgerViewModel.getError().observe(this, message -> {
            if (message != null) {
                Snackbar.make(binding.main, message, Snackbar.LENGTH_LONG).show();
            }
        });
        transactionViewModel.getError().observe(this, message -> {
            if (message != null) {
                Snackbar.make(binding.main, message, Snackbar.LENGTH_LONG).show();
            }
        });
        transactionViewModel.getMessage().observe(this, message -> {
            if (message != null) {
                Snackbar.make(binding.main, message, Snackbar.LENGTH_SHORT).show();
            }
        });
        Long restoredLedgerId = savedInstanceState != null
                && savedInstanceState.containsKey(STATE_SELECTED_LEDGER_ID)
                ? savedInstanceState.getLong(STATE_SELECTED_LEDGER_ID)
                : null;
        ledgerViewModel.initialize(restoredLedgerId);

        binding.toolbar.setNavigationOnClickListener(
                view -> binding.main.openDrawer(binding.navigationView)
        );
        binding.navigationView.setNavigationItemSelectedListener(this::onDrawerItemSelected);

        binding.fabAddtransaction.setOnClickListener(v -> {
            AddTransactionDialogFragment dialog = new AddTransactionDialogFragment();
            dialog.show(getSupportFragmentManager(), "addtransaction");
        });

        binding.txvCurrentTransactionsCaption.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Export Data")
                        .setMessage("Want to export application data?")
                        .setPositiveButton("CSV", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("text/csv");
                                if (selectedLedgerId == null || selectedLedgerName == null) {
                                    Snackbar.make(binding.main, "Wait for a ledger to load before exporting.", Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                pendingCsvLedgerId = selectedLedgerId;
                                pendingCsvLedgerName = selectedLedgerName;
                                intent.putExtra(
                                        Intent.EXTRA_TITLE,
                                        ExportFileName.forLedger(selectedLedgerName, "csv")
                                );
                                createCsvLauncher.launch(intent);
                            }
                        })
                        .setNegativeButton("PDF", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                exportAsPdf();
                            }
                        })
                        .setNeutralButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
                return false;
            }
        });

        setupItemTouchHelper();
    }

    private void populateLedgerDrawer(List<LedgerEntity> ledgers) {
        Menu menu = binding.navigationView.getMenu();
        menu.removeGroup(R.id.ledger_group);
        drawerLedgerIds.clear();
        if (ledgers != null) {
            for (int i = 0; i < ledgers.size(); i++) {
                LedgerEntity ledger = ledgers.get(i);
                int itemId = DRAWER_LEDGER_ITEM_BASE + i;
                drawerLedgerIds.put(itemId, ledger.getId());
                MenuItem item = menu.add(
                        R.id.ledger_group,
                        itemId,
                        i,
                        ledger.getName()
                );
                item.setCheckable(true);
                item.setChecked(selectedLedgerId != null
                        && selectedLedgerId == ledger.getId());
            }
        }
        menu.setGroupCheckable(R.id.ledger_group, true, true);
        View header = binding.navigationView.getHeaderView(0);
        TextView subtitle = header.findViewById(R.id.navigationSubtitle);
        LedgerEntity selectedLedger = ledgerViewModel.getSelectedLedger().getValue();
        subtitle.setText(selectedLedger == null
                ? getString(R.string.select_a_ledger)
                : selectedLedger.getName());
    }

    private boolean onDrawerItemSelected(@NonNull MenuItem item) {
        Long ledgerId = drawerLedgerIds.get(item.getItemId());
        if (ledgerId != null) {
            ledgerViewModel.selectLedger(ledgerId);
            binding.main.closeDrawer(binding.navigationView);
            return true;
        }
        if (item.getItemId() == R.id.action_create_ledger) {
            binding.main.closeDrawer(binding.navigationView);
            showCreateLedgerDialog();
            return true;
        }
        return false;
    }

    private void showCreateLedgerDialog() {
        List<ColorSchemeEntity> schemes = availableColorSchemes;
        if (schemes == null || schemes.isEmpty()) {
            Snackbar.make(binding.main, "No predefined color schemes are available.", Snackbar.LENGTH_LONG).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.create_ledger_dialog, null);
        TextInputLayout nameLayout = dialogView.findViewById(R.id.ledgerNameLayout);
        TextInputEditText nameInput = dialogView.findViewById(R.id.ledgerName);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.ledgerDescription);
        android.widget.Spinner schemeSpinner = dialogView.findViewById(R.id.colorSchemeSpinner);
        List<String> schemeNames = new ArrayList<>();
        for (ColorSchemeEntity scheme : schemes) {
            schemeNames.add(scheme.getName());
        }
        schemeSpinner.setAdapter(new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                schemeNames
        ));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create_ledger)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.create_ledger, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = nameInput.getText() == null
                            ? ""
                            : nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        nameLayout.setError(getString(R.string.ledger_name_required));
                        return;
                    }
                    nameLayout.setError(null);
                    String description = descriptionInput.getText() == null
                            ? ""
                            : descriptionInput.getText().toString().trim();
                    ColorSchemeEntity selectedScheme = schemes.get(schemeSpinner.getSelectedItemPosition());
                    ledgerViewModel.createLedger(name, description, selectedScheme.getId());
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void applyColorScheme(ColorSchemeEntity scheme) {
        if (scheme == null) {
            return;
        }
        adapter.setColorScheme(
                scheme.getPrimaryColor(),
                scheme.getSurfaceColor(),
                scheme.getSurfaceElevatedColor(),
                scheme.getTextColor()
        );
        binding.toolbar.setBackgroundColor(scheme.getPrimaryColor());
        binding.balancecard.setBackgroundColor(scheme.getSecondaryColor());
        binding.transactionsContainer.setBackgroundColor(scheme.getSurfaceElevatedColor());
        binding.mainContent.setBackgroundColor(scheme.getBackgroundColor());
        binding.txvCurrentBalanceCaption.setTextColor(scheme.getTextColor());
        binding.txvCurrentBalancePeso.setTextColor(scheme.getTextColor());
        binding.txvCurrentBalance.setTextColor(scheme.getTextColor());
        binding.txvCurrentTransactionsCaption.setTextColor(scheme.getTextColor());
        binding.fabAddtransaction.setBackgroundTintList(
                ColorStateList.valueOf(scheme.getPrimaryColor())
        );
        binding.navigationView.setBackgroundColor(scheme.getSurfaceColor());
        binding.navigationView.setItemTextColor(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{scheme.getPrimaryColor(), scheme.getTextColor()}
        ));
        binding.navigationView.setItemIconTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{scheme.getPrimaryColor(), scheme.getTextColor()}
        ));
        getWindow().setStatusBarColor(scheme.getPrimaryColor());
        getWindow().setNavigationBarColor(scheme.getBackgroundColor());
    }

    @Override
    public void onTransactionAdded(String labeltxt, double amount, AddTransactionDialogFragment.TransactionType type) {
        int transactionType = switch (type) {
            case EXPENSE -> TransactionEntity.TYPE_EXPENSE;
            case ALLOWANCE -> TransactionEntity.TYPE_ALLOWANCE;
        };
        transactionViewModel.add(labeltxt, amount, transactionType);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (selectedLedgerId != null) {
            outState.putLong(STATE_SELECTED_LEDGER_ID, selectedLedgerId);
        }
        super.onSaveInstanceState(outState);
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback smpcb = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                TransactionEntity tx = adapter.getEntity(position);
                transactionViewModel.delete(tx);
                showUndoSnackbar(tx);
            }

            @Override
            public void onChildDraw(
                    @NonNull Canvas c,
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    float dX,
                    float dY,
                    int actionState,
                    boolean isCurrentlyActive
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    TransactionAdapter.TransactionViewHolder holder = (TransactionAdapter.TransactionViewHolder) viewHolder;

                    holder.binding.viewBackground.setVisibility(View.VISIBLE);
                    holder.binding.viewForeground.setTranslationX(dX);
                }

                super.onChildDraw(
                        c, recyclerView, viewHolder,
                        dX, dY, actionState, isCurrentlyActive
                );
            }

            @Override
            public void clearView(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder
            ) {
                TransactionAdapter.TransactionViewHolder holder = (TransactionAdapter.TransactionViewHolder) viewHolder;
                holder.binding.viewForeground.setTranslationX(0f);
                holder.binding.viewBackground.setVisibility(View.GONE);
                super.clearView(recyclerView, viewHolder);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        };

        new ItemTouchHelper(smpcb).attachToRecyclerView(rview);
    }

    private void showUndoSnackbar(TransactionEntity tx) {
        Snackbar.make(binding.main, "Transaction deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> transactionViewModel.undoDelete())
                .show();
    }

}