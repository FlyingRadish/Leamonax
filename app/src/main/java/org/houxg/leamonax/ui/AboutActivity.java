package org.houxg.leamonax.ui;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import org.houxg.leamonax.BuildConfig;
import org.houxg.leamonax.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AboutActivity extends BaseActivity {

    @BindView(R.id.tv_version)
    TextView mVersionTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        ButterKnife.bind(this);

        mVersionTv.setText(BuildConfig.VERSION_NAME);
    }
}
