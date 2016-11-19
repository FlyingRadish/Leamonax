package org.houxg.leanotelite.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.houxg.leanotelite.BuildConfig;
import org.houxg.leanotelite.R;
import org.houxg.leanotelite.model.Account;
import org.houxg.leanotelite.model.BaseResponse;
import org.houxg.leanotelite.model.Note;
import org.houxg.leanotelite.model.Note_Table;
import org.houxg.leanotelite.model.Notebook;
import org.houxg.leanotelite.model.Notebook_Table;
import org.houxg.leanotelite.service.AccountService;
import org.houxg.leanotelite.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class SettingsActivity extends BaseActivity {

    private final String[] mEditors = new String[]{"RichText", "Markdown"};

    @BindView(R.id.tv_editor)
    TextView mEditorTv;
    @BindView(R.id.tv_image_size)
    TextView mImageSizeTv;
    @BindView(R.id.iv_avatar)
    ImageView mAvatarIv;
    @BindView(R.id.tv_user_name)
    TextView mUserNameTv;
    @BindView(R.id.tv_email)
    TextView mEmailTv;
    @BindView(R.id.tv_host)
    TextView mHostTv;
    @BindView(R.id.ll_clear)
    View mClearDataView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        ButterKnife.bind(this);
        refresh();
        mClearDataView.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.ll_editor)
    void selectEditor() {
        new AlertDialog.Builder(this)
                .setTitle("Choose editor")
                .setSingleChoiceItems(mEditors, AccountService.getCurrent().getDefaultEditor(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Account account = AccountService.getCurrent();
                        account.setDefaultEditor(which);
                        account.update();
                        mEditorTv.setText(mEditors[which]);
                    }
                })
                .setCancelable(true)
                .show();
    }

    @OnClick(R.id.ll_log_out)
    void clickedLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure to log out?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        AccountService.logout();
                        Intent intent = new Intent(SettingsActivity.this, SignInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnClick(R.id.rl_avatar)
    void clickedAvatar() {

    }

    @OnClick(R.id.ll_user_name)
    void clickedUserName() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sigle_edittext, null);
        final EditText mUserNameEt = (EditText) view.findViewById(R.id.edit);
        mUserNameEt.setText(AccountService.getCurrent().getUserName());
        new AlertDialog.Builder(this)
                .setTitle("Change username")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String username = mUserNameEt.getText().toString();
                        mUserNameTv.setText(username);
                        changeUsername(username);
                    }
                })
                .show();
    }

    @OnClick(R.id.ll_change_password)
    void clickedPassword() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_passowrd, null);
        final EditText mOldPasswordEt = (EditText) view.findViewById(R.id.et_old_password);
        final EditText mNewPasswordEt = (EditText) view.findViewById(R.id.et_new_password);
        new AlertDialog.Builder(this)
                .setTitle("Change password")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        changePassword(mOldPasswordEt.getText().toString(), mNewPasswordEt.getText().toString());
                    }
                })
                .show();
    }

    @OnClick(R.id.ll_clear)
    void clickedClearData() {
        new AlertDialog.Builder(this)
                .setTitle("Clear data")
                .setMessage("Are you sure to delete all notes and notebooks in this account?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        clearData();
                    }
                })
                .show();
    }

    private void clearData() {
        Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            SQLite.delete()
                                    .from(Note.class)
                                    .where(Note_Table.userId.eq(AccountService.getCurrent().getUserId()))
                                    .execute();
                            SQLite.delete()
                                    .from(Notebook.class)
                                    .where(Notebook_Table.userId.eq(AccountService.getCurrent().getUserId()))
                                    .execute();
                            Account account = AccountService.getCurrent();
                            account.setLastUsn(0);
                            account.update();
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        ToastUtils.show(SettingsActivity.this, "finish");
                    }
                });
    }

    private void changeUsername(final String username) {
        AccountService.changeUserName(username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.show(SettingsActivity.this, "Network error");
                        mUserNameTv.setText(AccountService.getCurrent().getUserName());
                    }

                    @Override
                    public void onNext(BaseResponse baseResponse) {
                        if (baseResponse.isOk()) {
                            Account account = AccountService.getCurrent();
                            account.setUserName(username);
                            account.update();
                        } else {
                            mUserNameTv.setText(AccountService.getCurrent().getUserName());
                            ToastUtils.show(SettingsActivity.this, "Change username failed");
                        }
                    }
                });
    }

    private void changePassword(String oldPassword, String newPassword) {
        AccountService.changePassword(oldPassword, newPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.show(SettingsActivity.this, "Change password failed");
                    }

                    @Override
                    public void onNext(BaseResponse baseResponse) {
                        if (!baseResponse.isOk()) {
                            ToastUtils.show(SettingsActivity.this, "Change password failed");
                        }
                    }
                });
    }

    private void refresh() {
        Account current = AccountService.getCurrent();
        mEditorTv.setText(mEditors[current.getDefaultEditor()]);
        mUserNameTv.setText(current.getUserName());
        mEmailTv.setText(current.getEmail());
        mHostTv.setText(current.getHost());
        Glide.with(this)
                .load(current.getAvatar())
                .centerCrop()
                .into(mAvatarIv);
    }
}
