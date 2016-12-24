package org.houxg.leamonax.ui;


import android.content.Intent;
import android.net.Uri;
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

import org.houxg.leamonax.R;
import org.houxg.leamonax.model.Authentication;
import org.houxg.leamonax.model.BaseResponse;
import org.houxg.leamonax.network.ApiProvider;
import org.houxg.leamonax.network.LeaFailure;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SignInActivity extends BaseActivity implements TextWatcher {

    private static final String TAG = "SignInActivity";

    private static final String LEANOTE_HOST = "https://leanote.com";
    private static final String FIND_PASSWORD = "/findPassword";
    private static final String EXT_IS_CUSTOM_HOST = "ext_is_custom_host";
    private static final String EXT_HOST = "ext_host";
    private static final String EXT_ACTION_PANEL_OFFSET_Y = "ext_host_et_height";

    @BindView(R.id.et_email)
    EditText mEmailEt;
    @BindView(R.id.et_password)
    EditText mPasswordEt;
    @BindView(R.id.tv_sign_in)
    View mSignInBtn;
    @BindView(R.id.tv_sign_up)
    View mSignUpBtn;
    @BindView(R.id.tv_custom_host)
    TextView mCustomHostBtn;
    @BindView(R.id.et_custom_host)
    EditText mHostEt;
    @BindView(R.id.ll_action)
    View mActionPanel;
    @BindView(R.id.progress_sign_in)
    ProgressBar mSignInProgress;
    @BindView(R.id.progress_sign_up)
    ProgressBar mSignUpProgress;
    @BindView(R.id.rl_sign_up)
    View mSignUpPanel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        ButterKnife.bind(this);
        mEmailEt.addTextChangedListener(this);
        mPasswordEt.addTextChangedListener(this);
        mHostEt.setPivotY(0);

        int actionPanelOffsetY = 0;
        boolean isCustomHost = false;
        String host = "";
        if (savedInstanceState != null) {
            isCustomHost = savedInstanceState.getBoolean(EXT_IS_CUSTOM_HOST);
            actionPanelOffsetY = savedInstanceState.getInt(EXT_ACTION_PANEL_OFFSET_Y);
            host = savedInstanceState.getString(EXT_HOST);
        }
        mCustomHostBtn.setTag(isCustomHost);
        mActionPanel.setTag(actionPanelOffsetY);
        mActionPanel.setY(actionPanelOffsetY);
        mHostEt.setScaleY(isCustomHost ? 1 : 0);
        mHostEt.setText(host);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXT_IS_CUSTOM_HOST, (Boolean) mCustomHostBtn.getTag());
        outState.putString(EXT_HOST, mHostEt.getText().toString());
        outState.putInt(EXT_ACTION_PANEL_OFFSET_Y, (Integer) mActionPanel.getTag());
    }

    @OnClick(R.id.tv_forgot_password)
    void clickedForgotPassword() {
        String url = getHost() + FIND_PASSWORD;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        try {
            startActivity(i);
        } catch (Exception ex) {
            ToastUtils.show(this, R.string.host_address_is_incorrect);
        }
    }

    @OnClick(R.id.tv_custom_host)
    void switchHost() {
        boolean isCustomHost = !(boolean) mCustomHostBtn.getTag();
        mCustomHostBtn.setTag(isCustomHost);
        if (isCustomHost) {
            mCustomHostBtn.setText(R.string.use_leanote_host);
            mHostEt.animate()
                    .scaleY(1)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            mActionPanel.animate()
                    .yBy(mHostEt.getHeight())
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mActionPanel.setTag(mHostEt.getHeight());
                        }
                    })
                    .start();
        } else {
            mCustomHostBtn.setText(R.string.use_custom_host);
            mHostEt.animate()
                    .scaleY(0)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            mActionPanel.animate()
                    .yBy(-mHostEt.getHeight())
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mActionPanel.setTag(0);
                        }
                    })
                    .start();
        }
    }

    private String getHost() {
        return (boolean) mCustomHostBtn.getTag() ? mHostEt.getText().toString().trim() : LEANOTE_HOST;
    }

    @OnClick(R.id.tv_sign_in)
    void signIn() {
        String email = mEmailEt.getText().toString();
        String password = mPasswordEt.getText().toString();
        final String host = getHost();
        ApiProvider.getInstance().init(host);
        AccountService.login(email, password)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mSignInBtn.post(new Runnable() {
                            @Override
                            public void run() {
                                animateProgress(mSignInBtn, mSignInProgress);
                            }
                        });
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Authentication>() {
                    @Override
                    public void onCompleted() {
                        animateFinish(mSignInBtn, mSignInProgress);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        ToastUtils.showNetworkError(SignInActivity.this);
                        animateFinish(mSignInBtn, mSignInProgress);
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
                            ToastUtils.show(SignInActivity.this, R.string.email_or_password_incorrect);
                        }
                    }
                });
    }

    @OnClick(R.id.tv_sign_up)
    void clickedSignup() {
        final String email = mEmailEt.getText().toString();
        final String password = mPasswordEt.getText().toString();
        final String host = getHost();
        ApiProvider.getInstance().init(host);
        AccountService.register(email, password)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mSignUpBtn.post(new Runnable() {
                            @Override
                            public void run() {
                                animateProgress(mSignUpBtn, mSignUpProgress);
                            }
                        });
                    }
                })
                .flatMap(new Func1<BaseResponse, Observable<Authentication>>() {
                    @Override
                    public Observable<Authentication> call(BaseResponse baseResponse) {
                        if (baseResponse.isOk()) {
                            return AccountService.login(email, password);
                        } else {
                            throw new LeaFailure(baseResponse);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Authentication>() {
                    @Override
                    public void onCompleted() {
                        animateFinish(mSignUpBtn, mSignUpProgress);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        ToastUtils.showNetworkError(SignInActivity.this);
                        animateFinish(mSignUpBtn, mSignUpProgress);
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
                            ToastUtils.show(SignInActivity.this, R.string.email_or_password_incorrect);
                        }
                    }
                });
    }

    @OnClick(R.id.tv_sign_up)
    void clickedSignUp() {
        mSignUpPanel.animate()
                .scaleX(1)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void animateProgress(View button, final View progressBar) {
        button.animate()
                .scaleX(0)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.animate()
                                .alpha(1)
                                .setDuration(100)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();
                    }
                })
                .start();
    }

    private void animateFinish(final View button, final View progressBar) {
        progressBar.animate()
                .alpha(0)
                .setDuration(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        button.animate()
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
