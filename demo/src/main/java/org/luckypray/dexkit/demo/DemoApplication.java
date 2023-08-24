package org.luckypray.dexkit.demo;

import android.app.Application;

public class DemoApplication extends Application {

    private static Application sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        RouterManager.getInstance().init(this);
        sApplication = this;
    }

    public static Application getApplication() {
        return sApplication;
    }
}
