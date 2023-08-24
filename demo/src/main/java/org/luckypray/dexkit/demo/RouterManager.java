package org.luckypray.dexkit.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import org.luckypray.dexkit.demo.annotations.Router;

import java.util.HashMap;
import java.util.Map;

public class RouterManager {

    public static final String TAG = "RouterManager";

    private Map<String, String> map = new HashMap<>();

    private static Application sApplication;

    private static final class Host {
        private static final RouterManager INSTANCE = new RouterManager();
    }

    public void init(Application application) {
        sApplication = application;
        Host.INSTANCE.register(MainActivity.class);
        Host.INSTANCE.register(PlayActivity.class);
    }

    public static RouterManager getInstance() {
        return Host.INSTANCE;
    }

    public void register(Class<? extends Activity> clazz) {
        Router event = clazz.getAnnotation(Router.class);
        if (event != null) {
            Log.d(TAG, "register: " + clazz.getName() + " " + event.path());
            map.put(event.path(), clazz.getName());
        }
    }

    public void router(String path) {
        String className = map.get(path);
        Log.d(TAG, "router: " + className);
        if (className != null) {
            try {
                Class<?> clazz = Class.forName(className);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(sApplication, clazz);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sApplication.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "router error", e);
            }
        }
    }
}
