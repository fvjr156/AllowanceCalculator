package com.fvjapps.allowancecalculator.activities;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fvjapps.allowancecalculator.dao.TransactionDao;
import com.fvjapps.allowancecalculator.database.AppDatabase;
import com.fvjapps.allowancecalculator.databinding.ActivityMainBinding;
import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.fragments.AddTransactionDialogFragment;
import com.fvjapps.allowancecalculator.recyclerview.TransactionAdapter;
import com.fvjapps.allowancecalculator.repository.TransactionRepository;
import com.fvjapps.allowancecalculator.viewmodels.CurrentBalanceViewModel;
import com.fvjapps.allowancecalculator.viewmodels.CurrentBalanceViewModelFactory;
import com.fvjapps.allowancecalculator.viewmodels.TransactionViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AddTransactionDialogFragment.OnAddTransactionListener {

    ActivityMainBinding binding;
    TransactionViewModel transactionViewModel;
    TransactionAdapter adapter;
    RecyclerView rview;

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
                        return Objects.requireNonNull(modelClass.cast(new TransactionViewModel(repository)));
                    }
                }
        ).get(TransactionViewModel.class);

        rview = binding.recyclerView;
        adapter = new TransactionAdapter(getApplicationContext());
        rview.setLayoutManager(new LinearLayoutManager(this));
        rview.setAdapter(adapter);

        CurrentBalanceViewModelFactory balanceViewModelFactory = new CurrentBalanceViewModelFactory(repository);
        CurrentBalanceViewModel balanceViewModel = new ViewModelProvider(this, balanceViewModelFactory).get(CurrentBalanceViewModel.class);

        balanceViewModel.getCurrentBalance().observe(this, balance -> {
            binding.txvCurrentBalance.setText(String.format("%.2f", balance));
        });

        transactionViewModel.getTransactions().observe(this, adapter::setEntities);

        binding.fabAddtransaction.setOnClickListener(v -> {
            AddTransactionDialogFragment dialog = new AddTransactionDialogFragment();
            dialog.show(getSupportFragmentManager(), "addtransaction");
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