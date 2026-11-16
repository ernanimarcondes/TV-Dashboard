package io.freemytv;

import android.app.Application;

/** Gives Prefs a context in every process. */
public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Prefs.init(this);
        Place.init(this);
        android.content.SharedPreferences settings=getSharedPreferences("settings",MODE_PRIVATE);
        if(!settings.getBoolean("racingDashboardV2",false)) {
            settings.edit().putBoolean("nowPlaying",true).putBoolean("racingDashboardV2",true).commit();
        }
    }
}
