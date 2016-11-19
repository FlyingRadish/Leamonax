package org.houxg.leanotelite.model;


import com.google.gson.annotations.SerializedName;

public class SyncState extends BaseResponse {
    @SerializedName("LastSyncUsn")
    private int mLastSyncUsn;

    @SerializedName("LastSyncTime")
    private long mLastSyncTime;

    public int getLastSyncUsn() {
        return mLastSyncUsn;
    }

    public long getLastSyncTime() {
        return mLastSyncTime;
    }
}
