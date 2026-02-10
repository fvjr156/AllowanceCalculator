package com.fvjapps.allowancecalculator.applications;

import android.app.Application;

import com.fvjapps.allowancecalculator.managers.ExecutorManager;

public class AllowanceCalculatorApplication extends Application {
    private static AllowanceCalculatorApplication appInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        appInstance = this;
        ExecutorManager.getInstance();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ExecutorManager.getInstance().shutdown();
    }
}
