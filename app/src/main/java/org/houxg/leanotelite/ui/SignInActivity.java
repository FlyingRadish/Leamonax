package org.houxg.leanotelite.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.houxg.leanotelite.R;
import org.houxg.leanotelite.model.Authentication;
import org.houxg.leanotelite.network.ApiProvider;
import org.houxg.leanotelite.service.AccountService;
import org.houxg.leanotelite.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class SignInActivity extends BaseActivity implements TextWatcher {

    private static final String TAG = "SignInActivity";

    private static final String LEANOTE_HOST = "https://leanote.com";
    private static final String EXT_IS_CUSTOM_HOST = "ext_is_custom_host";
    private static final String EXT_HOST = "ext_host";

    @BindView(R.id.et_email)
    EditText mEmailEt;
    @BindView(R.id.et_password)
    EditText mPasswordEt;
    @BindView(R.id.tv_sign_in)
    View mSignInBtn;
    @BindView(R.id.tv_custom_host)
    TextView mCustomHostBtn;
    @BindView(R.id.et_custom_host)
    EditText mHostEt;
    @BindView(R.id.ll_action)
    View mActionPanel;
    @BindView(R.id.progress)
    ProgressBar mProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        ButterKnife.bind(this);
        mEmailEt.addTextChangedListener(this);
        mPasswordEt.addTextChangedListener(this);
        mCustomHostBtn.setTag(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXT_IS_CUSTOM_HOST, (Boolean) mCustomHostBtn.getTag());
        outState.putString(EXT_HOST, mHostEt.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        boolean isCustomHost = savedInstanceState.getBoolean(EXT_IS_CUSTOM_HOST);
        refreshHostSetting(isCustomHost);
        mHostEt.setText(savedInstanceState.getString(EXT_HOST));
    }

    @OnClick(R.id.tv_custom_host)
    void switchHost() {
        refreshHostSetting(!(boolean) mCustomHostBtn.getTag());
    }

    private void refreshHostSetting(boolean isCustomHost) {
        if (isCustomHost) {
            mCustomHostBtn.setText(R.string.use_leanote_host);
            mHostEt.setPivotY(0);
            mHostEt.animate()
                    .scaleY(1)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            mActionPanel.animate()
                    .yBy(mHostEt.getHeight())
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            mCustomHostBtn.setText(R.string.use_custom_host);
            mHostEt.setPivotY(0);
            mHostEt.animate()
                    .scaleY(0)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            mActionPanel.animate()
                    .yBy(-mHostEt.getHeight())
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        mCustomHostBtn.setTag(isCustomHost);
    }


    @OnClick(R.id.tv_sign_in)
    void signIn() {
        String email = mEmailEt.getText().toString();
        String password = mPasswordEt.getText().toString();
        boolean isCustomHost = (boolean) mCustomHostBtn.getTag();
        final String host = isCustomHost ? mHostEt.getText().toString().trim() : LEANOTE_HOST;
        ApiProvider.getInstance().init(host);
        AccountService.login(email, password)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mSignInBtn.post(new Runnable() {
                            @Override
                            public void run() {
                                animateSignInProgress();
                            }
                        });
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Authentication>() {
                    @Override
                    public void onCompleted() {
                        animateSignFinish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        ToastUtils.showNetworkError(SignInActivity.this);
                        animateSignFinish();
                    }

                    @Override
                    public void onNext(Authentication authentication) {
                        if (authentication.isOk()) {
                            AccountService.saveToAccount(authentication, host);
                            Intent intent = MainActivity.getOpenIntent(SignInActivity.this, true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            ToastUtils.show(SignInActivity.this, R.string.wron_email_or_password);
                        }
                    }
                });
    }

    private void animateSignInProgress() {
        mSignInBtn.animate()
                .scaleX(0)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mProgress.animate()
                                .alpha(1)
                                .setDuration(100)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();
                    }
                })
                .start();
    }

    private void animateSignFinish() {
        mProgress.animate()
                .alpha(0)
                .setDuration(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mSignInBtn.animate()
                                .scaleX(1)
                                .setDuration(200)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();
                    }
                })
                .start();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String email = mEmailEt.getText().toString();
        String password = mPasswordEt.getText().toString();
        mSignInBtn.setEnabled(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password));
    }
}
