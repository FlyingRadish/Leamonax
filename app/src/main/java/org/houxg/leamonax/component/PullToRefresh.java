package org.houxg.leamonax.component;


import android.support.v4.widget.SwipeRefreshLayout;

import com.elvishew.xlog.XLog;

public class PullToRefresh {

    private SwipeRefreshLayout refreshLayout;
    private SwipeRefreshLayout.OnRefreshListener listener;

    public PullToRefresh(SwipeRefreshLayout refreshLayout, SwipeRefreshLayout.OnRefreshListener listener) {
        this.listener = listener;
        this.refreshLayout = refreshLayout;
        refreshLayout.setOnRefreshListener(listener);
    }

    public void forceRefresh() {
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                XLog.i("fetching notes");
                refreshLayout.setRefreshing(true);
                listener.onRefresh();
            }
        }, 200);
    }

    public void stopRefreshing() {
        refreshLayout.setRefreshing(false);
    }
}
