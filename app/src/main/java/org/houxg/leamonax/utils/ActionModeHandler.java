package org.houxg.leamonax.utils;


import android.app.Activity;
import android.support.annotation.MenuRes;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class ActionModeHandler<T> {
    private List<T> mPendingItems;
    private int mMenuId;
    private Callback<T> mCallback;
    private ActionMode mActionMode;
    private Activity mContext;
    private ActionMode.Callback mActionCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(mMenuId, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean isProceed = mCallback.onAction(item.getItemId(), mPendingItems);
            if (isProceed) {
                mPendingItems.clear();
            }
            return isProceed;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mCallback.onDestroy(mPendingItems);
        }
    };

    public ActionModeHandler(Activity activity, Callback<T> callback, @MenuRes int menuId) {
        mContext = activity;
        mMenuId = menuId;
        mCallback = callback;
    }

    /**
     *
     * @param item
     * @return true if the item is in pending list, false for others.
     */
    public boolean chooseItem(T item) {
        if (isActionMode()) {
            if (mPendingItems.contains(item)) {
                mPendingItems.remove(item);
                return false;
            } else {
                mPendingItems.add(item);
                return true;
            }
        } else {
            mActionMode = mContext.startActionMode(mActionCallback);
            mPendingItems = new ArrayList<>();
            mPendingItems.add(item);
            return true;
        }
    }

    public void dismiss() {
        mActionMode.finish();
    }

    public boolean isActionMode() {
        return mActionMode != null;
    }

    public interface Callback<T> {
        boolean onAction(int actionId, List<T> pendingItems);
        void onDestroy(List<T> pendingItems);
    }
}
