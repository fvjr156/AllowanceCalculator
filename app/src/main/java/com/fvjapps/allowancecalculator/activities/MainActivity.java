package com.fvjapps.allowancecalculator.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fvjapps.allowancecalculator.R;
import com.fvjapps.allowancecalculator.adapters.TransactionsPrintAdapter;
import com.fvjapps.allowancecalculator.dao.TransactionDao;
import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.databinding.ActivityMainBinding;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.fragments.AddTransactionDialogFragment;
import com.fvjapps.allowancecalculator.managers.ExecutorManager;
import com.fvjapps.allowancecalculator.adapters.TransactionAdapter;
import com.fvjapps.allowancecalculator.misc.MillisConv;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;
import com.fvjapps.allowancecalculator.viewmodels.CurrentBalanceViewModel;
import com.fvjapps.allowancecalculator.viewmodels.CurrentBalanceViewModelFactory;
import com.fvjapps.allowancecalculator.viewmodels.TransactionViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity implements AddTransactionDialogFragment.OnAddTransactionListener {

    ActivityMainBinding binding;
    TransactionViewModel transactionViewModel;
    TransactionAdapter adapter;
    RecyclerView rview;

    private ActivityResultLauncher<Intent> createCsvLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            exportCsvToUri(uri);
                        }
                    }
                }
            });

    private void exportAsPdf() {
        PrintManager man = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        AtomicReference<List<TransactionEntity>> entityList = new AtomicReference<>(new ArrayList<>());

        String jobName = getString(R.string.app_name) + " Transactions Export";
        ExecutorManager.getInstance().getDbExec().execute(() -> {
            entityList.set(transactionViewModel.exportAllActiveData());
            man.print(
                    jobName,
                    new TransactionsPrintAdapter(this,
                            entityList.get(), new TransactionsPrintAdapter.TransactionPrintListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() ->
                                    Snackbar.make(binding.main, "Export success", Snackbar.LENGTH_LONG).show()
                            );
                        }

                        @Override
                        public void onFail(String error) {
                            runOnUiThread(() ->
                                    Snackbar.make(binding.main, "Export failed!", Snackbar.LENGTH_LONG).show()
                            );
                        }
                    }),
                    null
            );
        });
    }

    private void exportCsvToUri(@NonNull Uri uri) {
        ExecutorManager.getInstance().getFileExec().execute(new Runnable() {
            @Override
            public void run() {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(getContentResolver().openOutputStream(uri)))) {

                    // transactionId, type, amount, createdAt, isDeleted, label
                    List<TransactionEntity> entityList = transactionViewModel.exportData();

                    bw.write("id,type,amount,creationdate,deleted,label");
                    bw.newLine();

                    for (TransactionEntity e : entityList) {
                        bw.write(
                                e.transactionId + "," +
                                        e.type + "," +
                                        e.amount + "," +
                                        MillisConv.toDate(e.createdAt, MillisConv.DateFormat.DATABASE_STANDARD) + "," +
                                        e.isDeleted + "," +
                                        ((Function<String, String>) (x) -> {
                                            if (x == null) return "";
                                            if (x.contains(",") || x.contains("\"") || x.contains("\n"))
                                                return "\"" + x.replace("\"", "\"\"") + "\"";
                                            return x;
                                        }).apply(e.label)
                        );
                        bw.newLine();
                    }

                    bw.flush();

                    runOnUiThread(() ->
                            Snackbar.make(binding.main, "Export success", Snackbar.LENGTH_LONG).show()
                    );
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Snackbar.make(binding.main, "Export failed", Snackbar.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.main);
//        Objects.requireNonNull(getSupportActionBar()).hide();

        TransactionDao transactionDao = AppDatabase.getInstance(this).transactionDao();
        TransactionRepository repository = new TransactionRepository(transactionDao);
        transactionViewModel = new ViewModelProvider(
                this,
                new ViewModelProvider.Factory() {
                    @NonNull
                    @Override
                    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                        return Objects.<T>requireNonNull(modelClass.cast(new TransactionViewModel(repository)));
                    }
                }
        ).<TransactionViewModel>get(TransactionViewModel.class);

        rview = binding.recyclerView;
        adapter = new TransactionAdapter(getApplicationContext());
        rview.setLayoutManager(new LinearLayoutManager(this));
        rview.setAdapter(adapter);

        CurrentBalanceViewModelFactory balanceViewModelFactory = new CurrentBalanceViewModelFactory(repository);
        CurrentBalanceViewModel balanceViewModel = new ViewModelProvider(this, balanceViewModelFactory).<CurrentBalanceViewModel>get(CurrentBalanceViewModel.class);

        balanceViewModel.getCurrentBalance().observe(this, balance -> {
            binding.txvCurrentBalancePeso.setVisibility(View.VISIBLE);
            binding.txvCurrentBalance.setText(String.format("%.2f", balance));
        });

        transactionViewModel.getTransactions().observe(this, adapter::setEntities);

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
                                intent.putExtra(Intent.EXTRA_TITLE, MillisConv.toDate(System.currentTimeMillis(), MillisConv.DateFormat.FILE_BACKUP) + "_transactions.csv");
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

    @Override
    public void onTransactionAdded(String labeltxt, double amount, AddTransactionDialogFragment.TransactionType type) {
        String t = switch (type) {
            case EXPENSE -> "OUT";
            case ALLOWANCE -> "IN";
        };
        long epoch = System.currentTimeMillis();
        TransactionEntity tx = new TransactionEntity(labeltxt, t, amount, epoch);
        transactionViewModel.add(tx);
        Snackbar.make(binding.main, "Successful creation.", Snackbar.LENGTH_SHORT).show();
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