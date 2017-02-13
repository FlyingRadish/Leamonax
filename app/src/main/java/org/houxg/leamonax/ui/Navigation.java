package org.houxg.leamonax.ui;


import android.app.Activity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.houxg.leamonax.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Navigation {

    @BindView(R.id.rv_notebook)
    RecyclerView mNotebookRv;
    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.tv_email)
    TextView mEmailTv;
    @BindView(R.id.iv_avatar)
    ImageView mAvatarIv;
    @BindView(R.id.tv_user_name)
    TextView mUserNameTv;
    @BindView(R.id.iv_notebook_triangle)
    View mNotebookTriangle;
    @BindView(R.id.rl_notebook_list)
    View mNotebookPanel;
    @BindView(R.id.rv_tag)
    RecyclerView mTagRv;
    @BindView(R.id.iv_tag_triangle)
    View mTagTriangle;
    @BindView(R.id.iv_other_account)
    ImageView mOtherAccountIv;

    public void init(Activity activity, View view) {
        ButterKnife.bind(this, view);
    }

    public void refresh() {

    }

    public void open() {

    }

    public void close() {

    }
}
