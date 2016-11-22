package org.houxg.leamonax.utils;


import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;

public class RetrofitUtils {
    public static <T> Observable<T> create(final Call<T> call) {
        return Observable.create(new Observable.OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        Response<T> response = call.execute();
                        if (response.isSuccessful()) {
                            subscriber.onNext(response.body());
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new IllegalStateException("code=" + response.code()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new IllegalStateException(e);
                    }
                }
            }
        });
    }

    public static <T> T excute(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T excuteWithException(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                throw new IllegalStateException("response not successful");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
