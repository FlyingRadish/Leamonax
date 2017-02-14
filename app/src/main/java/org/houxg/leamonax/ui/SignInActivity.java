package org.houxg.leamonax.ui;


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

import org.houxg.leamonax.R;
import org.houxg.leamonax.model.Authentication;
import org.houxg.leamonax.model.BaseResponse;
import org.houxg.leamonax.network.ApiProvider;
import org.houxg.leamonax.network.LeaFailure;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.utils.OpenUtils;
import org.houxg.leamonax.utils.ToastUtils;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SignInActivity extends BaseActivity implements TextWatcher {

    public static final String ACTION_ADD_ACCOUNT = "action.addAccount:";
    private static final String EXTRA_ACCOUNT_LOCAL_ID = "extra.account.LocalId";
    private static final String TAG = "SignInActivity:";

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
    @BindView(R.id.tv_example)
    TextView mExampleTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        setResult(RESULT_CANCELED);
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
        mHostEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mExampleTv.setText(String.format(Locale.US, "For example, login api will be:\n%s/api/login", s.toString()));
            }
        });
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
        OpenUtils.openUrl(this, url);
    }

    @OnClick(R.id.tv_custom_host)
    void switchHost() {
        boolean isCustomHost = !(boolean) mCustomHostBtn.getTag();
        mCustomHostBtn.setTag(isCustomHost);
        mExampleTv.setVisibility(isCustomHost ? View.VISIBLE : View.GONE);
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
        final String email = mEmailEt.getText().toString();
        final String password = mPasswordEt.getText().toString();
        final String host = getHost();
        initHost()
                .flatMap(new Func1<String, Observable<Authentication>>() {
                    @Override
                    public Observable<Authentication> call(String s) {
                        return AccountService.login(email, password);
                    }
                })
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
                        if (e instanceof IllegalHostException) {
                            ToastUtils.show(SignInActivity.this, R.string.illegal_host);
                        } else {
                            ToastUtils.showNetworkError(SignInActivity.this);
                            animateFinish(mSignInBtn, mSignInProgress);
                        }
                    }

                    @Override
                    public void onNext(Authentication authentication) {
                        if (authentication.isOk()) {
                            long localId = AccountService.saveToAccount(authentication, host);
                            if (ACTION_ADD_ACCOUNT.equals(getIntent().getAction())) {
                                Intent intent = new Intent();
                                intent.putExtra(EXTRA_ACCOUNT_LOCAL_ID, localId);
                                setResult(RESULT_OK, intent);
                            } else {
                                Intent intent = MainActivity.getOpenIntent(SignInActivity.this, true);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            ToastUtils.show(SignInActivity.this, R.string.email_or_password_incorrect);
                        }
                    }
                });
    }

    @OnClick(R.id.tv_sign_up)
    void clickedSignUp() {
        final String email = mEmailEt.getText().toString();
        final String password = mPasswordEt.getText().toString();
        final String host = getHost();
        initHost()
                .flatMap(new Func1<String, Observable<BaseResponse>>() {
                    @Override
                    public Observable<BaseResponse> call(String s) {
                        return AccountService.register(email, password);
                    }
                })
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
                        if (e instanceof IllegalHostException) {
                            ToastUtils.show(SignInActivity.this, R.string.illegal_host);
                        } else {
                            ToastUtils.showNetworkError(SignInActivity.this);
                            animateFinish(mSignUpBtn, mSignUpProgress);
                        }
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

    /**
     *
     * @param data
     * @return account local id or -1
     */
    public static long getAccountIdFromData(Intent data) {
        if (data == null) {
            return -1;
        } else {
            return data.getLongExtra(EXTRA_ACCOUNT_LOCAL_ID, -1);
        }
    }

    private Observable<String> initHost() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    String host = getHost();
                    if (host.matches("^(http|https)://[^\\s]+")) {
                        ApiProvider.getInstance().init(host);
                        subscriber.onNext(host);
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(new IllegalHostException());
                    }
                }
            }
        });
    }

    private static class IllegalHostException extends Exception {
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
