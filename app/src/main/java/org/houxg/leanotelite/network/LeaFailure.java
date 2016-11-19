package org.houxg.leanotelite.network;


import org.houxg.leanotelite.model.BaseResponse;

public class LeaFailure extends IllegalStateException {

    private BaseResponse mResponse;

    public LeaFailure(BaseResponse response) {
        mResponse = response;
    }

    public BaseResponse getResponse() {
        return mResponse;
    }

    @Override
    public String getMessage() {
        return mResponse.getMsg();
    }
}
