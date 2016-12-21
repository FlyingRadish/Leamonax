package org.houxg.leamonax.ui;


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
import com.tencent.bugly.crashreport.CrashReport;

import org.houxg.leamonax.BuildConfig;
import org.houxg.leamonax.R;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.BaseResponse;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Note_Table;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.Notebook_Table;
import org.houxg.leamonax.model.RelationshipOfNoteTag;
import org.houxg.leamonax.model.RelationshipOfNoteTag_Table;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.Tag_Table;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.service.HtmlImporter;
import org.houxg.leamonax.utils.DialogDisplayer;
import org.houxg.leamonax.utils.DialogUtils;
import org.houxg.leamonax.utils.FileUtils;
import org.houxg.leamonax.utils.ToastUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.ExFilePickerParcelObject;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SettingsActivity extends BaseActivity {

    private final String[] mEditors = new String[]{"RichText", "Markdown"};

    private static final int REQ_CHOOSE_HTML = 15;

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

    private HtmlImporter mHtmlImporter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        ButterKnife.bind(this);
        refresh();
        mClearDataView.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        mHtmlImporter = new HtmlImporter();
        mHtmlImporter.setPureContent(true);
    }

    @OnClick(R.id.ll_editor)
    void selectEditor() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_editor)
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
                .setTitle(R.string.log_out)
                .setMessage(R.string.are_your_sure_to_log_out)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
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
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
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
                .setTitle(R.string.change_user_name)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
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
                .setTitle(R.string.change_password)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
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
                .setTitle(R.string.clear_data)
                .setMessage(R.string.are_you_sure_to_delete_all_data_in_this_account)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        clearData();
                    }
                })
                .show();
    }

    @OnClick(R.id.ll_import_html)
    void clickedImportHtml() {
        Intent intent = new Intent(getApplicationContext(), ru.bartwell.exfilepicker.ExFilePickerActivity.class);
        intent.putExtra(ExFilePicker.SET_CHOICE_TYPE, ExFilePicker.CHOICE_TYPE_ALL);

        startActivityForResult(intent, REQ_CHOOSE_HTML);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CHOOSE_HTML) {
            if (data != null) {
                ExFilePickerParcelObject object = data.getParcelableExtra(ExFilePickerParcelObject.class.getCanonicalName());
                if (object.count > 0) {
                    ImportConfig importConfig = new ImportConfig();
                    importConfig.names = object.names;
                    importConfig.path = object.path;
                    selectImportNotebook(importConfig)
                            .subscribe(new Action1<ImportConfig>() {
                                @Override
                                public void call(ImportConfig config) {
                                    mHtmlImporter.setNotebookId(config.notebookId);
                                    importHtml(config);
                                }
                            });
                }
            }
        }
    }

    private static class ImportConfig {
        String path;
        List<String> names;
        String notebookId;
    }

    private Observable<ImportConfig> selectImportNotebook(final ImportConfig config) {
        return Observable.create(new Observable.OnSubscribe<ImportConfig>() {
            @Override
            public void call(final Subscriber<? super ImportConfig> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    DialogUtils.selectNotebook(SettingsActivity.this, "Import to which notebook?", new DialogUtils.SelectNotebookListener() {
                        @Override
                        public void onNotebookSelected(Notebook notebook) {
                            config.notebookId = notebook.getNotebookId();
                            subscriber.onNext(config);
                            subscriber.onCompleted();
                        }
                    });
                }
            }
        });
    }

    private void importHtml(ImportConfig config) {
        getAllHtmlPath(config)
                .flatMapIterable(new Func1<List<String>, Iterable<String>>() {
                    @Override
                    public Iterable<String> call(List<String> strings) {
                        return strings;
                    }
                })
                .flatMap(new Func1<String, Observable<Note>>() {
                    @Override
                    public Observable<Note> call(final String filePath) {
                        return Observable.create(new Observable.OnSubscribe<Note>() {
                            @Override
                            public void call(Subscriber<? super Note> subscriber) {
                                if (!subscriber.isUnsubscribed()) {
                                    File file = new File(filePath);
                                    Note note = mHtmlImporter.from(file);
                                    subscriber.onNext(note);
                                    subscriber.onCompleted();
                                }
                            }
                        });
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DialogDisplayer.showProgress(SettingsActivity.this, getString(R.string.parsing_html));
                            }
                        });
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DialogDisplayer.dismissProgress();
                            }
                        });
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Note>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        CrashReport.postCatchedException(e);
                        ToastUtils.show(SettingsActivity.this, R.string.parse_error);
                    }

                    @Override
                    public void onNext(Note note) {

                    }
                });
    }

    private Observable<List<String>> getAllHtmlPath(final ImportConfig config) {
        return Observable.create(
                new Observable.OnSubscribe<List<String>>() {
                    @Override
                    public void call(Subscriber<? super List<String>> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            List<String> absPaths = new ArrayList<>();
                            for (String name : config.names) {
                                File file = new File(config.path + name);
                                scanHtmlFile(absPaths, file);
                            }
                            subscriber.onNext(absPaths);
                            subscriber.onCompleted();
                        }
                    }
                });
    }

    private void scanHtmlFile(List<String> absPaths, File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String extension = FileUtils.getExtension(pathname.getName()).toLowerCase();
                    return pathname.isDirectory() || (pathname.isFile() && "html".equals(extension));
                }
            });
            if (children != null && children.length > 0) {
                for (File child : children) {
                    scanHtmlFile(absPaths, child);
                }
            }
        } else if (file.isFile() && "html".equals(FileUtils.getExtension(file.getName()).toLowerCase())) {
            absPaths.add(file.getAbsolutePath());
        }
    }

    private void clearData() {
        Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            Account currentUser = AccountService.getCurrent();
                            SQLite.delete()
                                    .from(Note.class)
                                    .where(Note_Table.userId.eq(currentUser.getUserId()))
                                    .execute();
                            SQLite.delete()
                                    .from(Notebook.class)
                                    .where(Notebook_Table.userId.eq(currentUser.getUserId()))
                                    .execute();
                            SQLite.delete()
                                    .from(Tag.class)
                                    .where(Tag_Table.userId.eq(currentUser.getUserId()))
                                    .execute();
                            SQLite.delete()
                                    .from(RelationshipOfNoteTag.class)
                                    .where(RelationshipOfNoteTag_Table.userId.eq(currentUser.getUserId()))
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
                        ToastUtils.show(SettingsActivity.this, R.string.clear_data_successful);
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
                        ToastUtils.showNetworkError(SettingsActivity.this);
                        mUserNameTv.setText(AccountService.getCurrent().getUserName());
                    }

                    @Override
                    public void onNext(BaseResponse baseResponse) {
                        if (baseResponse.isOk()) {
                            Account account = AccountService.getCurrent();
                            account.setUserName(username);
                            account.update();
                            ToastUtils.show(SettingsActivity.this, R.string.change_user_name_successful);
                        } else {
                            mUserNameTv.setText(AccountService.getCurrent().getUserName());
                            ToastUtils.show(SettingsActivity.this, R.string.change_user_name_failed);
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
                        ToastUtils.showNetworkError(SettingsActivity.this);
                    }

                    @Override
                    public void onNext(BaseResponse baseResponse) {
                        if (!baseResponse.isOk()) {
                            ToastUtils.show(SettingsActivity.this, R.string.change_password_failed);
                        } else {
                            ToastUtils.show(SettingsActivity.this, R.string.change_password_successful);
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
