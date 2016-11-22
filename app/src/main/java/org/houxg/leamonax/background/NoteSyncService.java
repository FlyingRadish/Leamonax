package org.houxg.leamonax.background;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.houxg.leamonax.model.SyncEvent;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.service.NoteService;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by binnchx on 10/19/15.
 */
public class NoteSyncService extends Service {

    private static final String TAG = "NoteSyncService";

    public static void startServiceForNote(Context context) {
        if (!AccountService.isSignedIn()) {
            Log.w(TAG, "Trying to sync but not login");
            return;
        }

        Intent intent = new Intent(context, NoteSyncService.class);
        context.startService(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(NoteService.fetchFromServer());
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                        stopSelf();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        EventBus.getDefault().post(new SyncEvent(false));
                        stopSelf();
                    }

                    @Override
                    public void onNext(Boolean isSucceed) {
                        EventBus.getDefault().post(new SyncEvent(true));
                    }
                });

        return START_NOT_STICKY;
    }
}
