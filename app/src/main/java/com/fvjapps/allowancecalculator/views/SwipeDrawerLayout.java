package com.fvjapps.allowancecalculator.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.fvjapps.allowancecalculator.R;

public class SwipeDrawerLayout extends DrawerLayout {
    private final int touchSlop;
    private float startX;
    private float startY;
    private boolean gestureStartedOutsideTransactions;
    private boolean openingGesture;
    private boolean drawerOpenedForGesture;

    public SwipeDrawerLayout(@NonNull Context context) {
        this(context, null);
    }

    public SwipeDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeDrawerLayout(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
            gestureStartedOutsideTransactions = !isInsideTransactions(event);
            openingGesture = false;
            drawerOpenedForGesture = false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                && gestureStartedOutsideTransactions
                && !isDrawerOpen(GravityCompat.START)
                && event.getX() - startX > touchSlop
                && event.getX() - startX > Math.abs(event.getY() - startY)) {
            openingGesture = true;
        }

        if (openingGesture) {
            return true;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!openingGesture) {
            return super.onTouchEvent(event);
        }

        if (!drawerOpenedForGesture) {
            openDrawer(GravityCompat.START);
            drawerOpenedForGesture = true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            openingGesture = false;
            gestureStartedOutsideTransactions = false;
        }
        return true;
    }

    private boolean isInsideTransactions(MotionEvent event) {
        View transactions = findViewById(R.id.recyclerView);
        if (transactions == null || transactions.getVisibility() != View.VISIBLE) {
            return false;
        }

        int[] location = new int[2];
        transactions.getLocationOnScreen(location);
        Rect bounds = new Rect(
                location[0],
                location[1],
                location[0] + transactions.getWidth(),
                location[1] + transactions.getHeight()
        );
        int[] rootLocation = new int[2];
        getLocationOnScreen(rootLocation);
        return bounds.contains(
                Math.round(event.getX() + rootLocation[0]),
                Math.round(event.getY() + rootLocation[1])
        );
    }
}
