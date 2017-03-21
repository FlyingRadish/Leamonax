package org.houxg.leamonax.ui;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction;

import org.bson.types.ObjectId;
import org.houxg.leamonax.BuildConfig;
import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.utils.OpenUtils;
import org.houxg.leamonax.utils.TestUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class AboutActivity extends BaseActivity {

    @BindView(R.id.tv_version)
    TextView mVersionTv;
    @BindView(R.id.ll_debug)
    View mDebugPanel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        ButterKnife.bind(this);

        mVersionTv.setText(BuildConfig.VERSION_NAME);
        mDebugPanel.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.ll_generate_random_note)
    void clickedVersion() {
        Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        String userId = Account.getCurrent().getUserId();
                        SecureRandom random = new SecureRandom();
                        String notebookId = new ObjectId().toString();
                        List<Note> notes = new ArrayList<>(8000);
                        for (int i = 0; i < 5000; i++) {
                            Note note = TestUtils.randomNote(random, notebookId, userId);
                            notes.add(note);
                        }
                        ProcessModelTransaction<Note> processModelTransaction = new ProcessModelTransaction.Builder<>(
                                new ProcessModelTransaction.ProcessModel<Note>() {
                                    @Override
                                    public void processModel(Note note) {
                                        note.save();
                                    }
                                })
                                .addAll(notes)
                                .build();
                        Transaction transaction = FlowManager.getDatabase(AppDataBase.class).beginTransactionAsync(processModelTransaction).build();
                        transaction.execute();
                    }
                }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    @OnClick(R.id.ll_github)
    void clickedGithub() {
        OpenUtils.openUrl(this, "https://github.com/leanote/leanote-android");
    }

    @OnClick(R.id.ll_feedback)
    void clickedFeedback() {
        OpenUtils.openUrl(this, "https://github.com/leanote/leanote-android/issues");
    }

    @OnClick(R.id.thanks)
    void clickedThanks() {
        OpenUtils.openUrl(this, "https://github.com/leanote/leanote-android/graphs/contributors");
    }

}
