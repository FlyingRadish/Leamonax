package org.houxg.leamonax;


import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.flurry.android.FlurryAgent;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import net.danlew.android.joda.JodaTimeAndroid;

import org.greenrobot.eventbus.EventBus;

public class Leamonax extends Application {

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        new FlurryAgent.Builder()
                .withLogEnabled(true)
                .build(this, BuildConfig.FLURRY_KEY);
        EventBus.builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .throwSubscriberException(true)
                .installDefaultEventBus();
        FlowManager.init(new FlowConfig.Builder(this).build());
        Stetho.initializeWithDefaults(this);
        JodaTimeAndroid.init(this);
    }
}
