package org.houxg.leamonax.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.houxg.leamonax.Leamonax;
import org.houxg.leamonax.R;
import org.houxg.leamonax.model.Note;

import rx.Observable;
import rx.Subscriber;

public class NetworkUtils {

    private static NetworkInfo getActiveNetworkInfo(Context context) {
        if (context == null) {
            return null;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        // note that this may return null if no network is currently active
        return cm.getActiveNetworkInfo();
    }

    public static boolean isNetworkAvailable() {
        NetworkInfo info = getActiveNetworkInfo(Leamonax.getContext());
        return (info != null && info.isConnected());
    }

    public static void checkNetwork() throws NetworkUnavailableException {
        if (!isNetworkAvailable()) {
            throw new NetworkUnavailableException();
        }
    }

    public <T> Observable<T> checkNetwork(T data) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                if (!isNetworkAvailable()) {
                    throw new NetworkUnavailableException();
                }

            }
        });
    }

    public static class NetworkUnavailableException extends IllegalStateException {
        public NetworkUnavailableException() {
            super(Leamonax.getContext().getResources().getString(R.string.network_is_unavailable));
        }
    }
}
