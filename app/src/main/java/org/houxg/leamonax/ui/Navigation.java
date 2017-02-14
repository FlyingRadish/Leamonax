package org.houxg.leamonax.ui;


import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.elvishew.xlog.XLog;

import org.houxg.leamonax.R;
import org.houxg.leamonax.adapter.AccountAdapter;
import org.houxg.leamonax.adapter.NotebookAdapter;
import org.houxg.leamonax.adapter.TagAdapter;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.User;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.service.NotebookService;
import org.houxg.leamonax.utils.DisplayUtils;
import org.houxg.leamonax.widget.TriangleView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;

public class Navigation {
    public static final int REQ_ADD_ACCOUNT = 55;
    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.rl_info)
    View mInfoPanel;
    @BindView(R.id.tv_email)
    TextView mEmailTv;
    @BindView(R.id.iv_avatar)
    ImageView mAvatarIv;
    @BindView(R.id.tv_user_name)
    TextView mUserNameTv;
    @BindView(R.id.tr_account)
    TriangleView mAccountTr;
    @BindView(R.id.rv_account)
    RecyclerView mAccountRv;

    @BindView(R.id.rv_notebook)
    RecyclerView mNotebookRv;
    @BindView(R.id.tr_notebook)
    TriangleView mNotebookTr;

    @BindView(R.id.rv_tag)
    RecyclerView mTagRv;
    @BindView(R.id.tr_tag)
    TriangleView mTagTr;

    Drawable mAccountRipple;

    private Callback mCallback;
    private Activity mActivity;
    private NotebookAdapter mNotebookAdapter;
    private AccountAdapter mAccountAdapter;
    private TagAdapter mTagAdapter;

    private Mode mCurrentMode = Mode.RECENT_NOTES;

    public Navigation(Callback callback) {
        mCallback = callback;
    }

    public void init(Activity activity, View view) {
        ButterKnife.bind(this, view);
        mActivity = activity;
        initAccountPanel();
        initNotebookPanel();
        initTagPanel();
        fetchInfo();
    }

    private void fetchInfo() {
        AccountService.getInfo(AccountService.getCurrent().getUserId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<User>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(User user) {
                        AccountService.saveToAccount(user, AccountService.getCurrent().getHost());
                        refreshUserInfo(AccountService.getCurrent());
                    }
                });
    }

    private void initAccountPanel() {
        mAccountRipple = mInfoPanel.getBackground();
        mAccountRipple.mutate();
        mInfoPanel.setBackground(null);
        mAccountRv.setLayoutManager(new LinearLayoutManager(mActivity));
        mAccountAdapter = new AccountAdapter(new AccountAdapter.AccountAdapterListener() {
            @Override
            public void onClickAccount(View v, final Account account) {
                animateChangeAccount(v, account);
            }

            @Override
            public void onClickAddAccount() {
                Intent intent = new Intent(mActivity, SignInActivity.class);
                mActivity.startActivityForResult(intent, REQ_ADD_ACCOUNT);
            }
        }

        );
        mAccountRv.setAdapter(mAccountAdapter);
        mAccountTr.setOnToggleListener(
                new TriangleView.OnToggleListener() {
                    @Override
                    public void onToggle(boolean isChecked) {
                        mAccountRv.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    }
                }
        );
        mAccountAdapter.load(AccountService.getAccountList());
    }

    private void animateChangeAccount(View v, final Account account) {
        ImageView itemAvatar = (ImageView) v.findViewById(R.id.iv_avatar);
        final ViewGroup rootView = (ViewGroup) mActivity.getWindow().getDecorView().getRootView();

        float preSize = DisplayUtils.dp2px(30);
        float postSize = DisplayUtils.dp2px(40);
        int[] position = new int[2];
        itemAvatar.getLocationInWindow(position);
        int preLeft = position[0];
        int preTop = position[1];
        mAvatarIv.getLocationInWindow(position);
        int postLeft = position[0];
        int postTop = position[1];

        final ImageView animateView = new ImageView(mActivity);
        Drawable drawable = itemAvatar.getDrawable();
        drawable.mutate();
        animateView.setImageDrawable(drawable);
        animateView.setLayoutParams(new ViewGroup.LayoutParams((int) preSize, (int) preSize));
        animateView.setX(preLeft);
        animateView.setY(preTop);
        animateView.setPivotX(0);
        animateView.setPivotY(0);
        animateView.setAlpha(0.7f);
        rootView.addView(animateView);

        animateView.animate()
                .scaleX(postSize / preSize)
                .scaleY(postSize / preSize)
                .translationX(postLeft)
                .translationY(postTop)
                .setDuration(350)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                                 @Override
                                 public void onAnimationStart(Animator animation) {

                                 }

                                 @Override
                                 public void onAnimationEnd(Animator animation) {
                                     //trigger quick ripple effect
                                     mInfoPanel.setBackground(mAccountRipple);
                                     mAccountRipple.setHotspot(mAvatarIv.getLeft() + mAvatarIv.getWidth() / 2, mAvatarIv.getTop() + mAvatarIv.getHeight() / 2);
                                     mAccountRipple.setHotspotBounds(0, 0, mInfoPanel.getWidth(), mInfoPanel.getHeight());
                                     mInfoPanel.setPressed(true);
                                     mInfoPanel.setPressed(false);

                                     refreshUserInfo(account);
                                     rootView.removeView(animateView);
                                     mInfoPanel.postDelayed(new Runnable() {
                                         @Override
                                         public void run() {
                                             mInfoPanel.setBackground(null);
                                             changeAccount(account);
                                         }
                                     }, 200);
                                 }

                                 @Override
                                 public void onAnimationCancel(Animator animation) {

                                 }

                                 @Override
                                 public void onAnimationRepeat(Animator animation) {

                                 }
                             }

                )
                .start();
    }

    private void changeAccount(Account account) {
        if (mCallback != null) {
            if (mCallback.onChangeAccount(account)) {
                mAccountTr.setChecked(false);
                mTagTr.setChecked(false);
                mNotebookTr.setChecked(false);
                close();
            }
        }
    }

    private void initTagPanel() {
        mTagRv.setLayoutManager(new LinearLayoutManager(mActivity));
        mTagAdapter = new TagAdapter();
        mTagAdapter.setListener(new TagAdapter.TagAdapterListener() {
            @Override
            public void onClickedTag(Tag tag) {
                mCurrentMode = Mode.TAG;
                mCurrentMode.setTagText(tag.getText());
                if (mCallback != null) {
                    if (mCallback.onShowNotes(mCurrentMode)) {
                        close();
                    }
                }
            }
        });
        mTagRv.setAdapter(mTagAdapter);
        mTagTr.setOnToggleListener(new TriangleView.OnToggleListener() {
            @Override
            public void onToggle(boolean isChecked) {
                mTagRv.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void initNotebookPanel() {
        mNotebookRv.setLayoutManager(new LinearLayoutManager(mActivity));
        mNotebookAdapter = new NotebookAdapter();
        mNotebookAdapter.setListener(new NotebookAdapter.NotebookAdapterListener() {
            @Override
            public void onClickedNotebook(Notebook notebook) {
                mCurrentMode = Mode.NOTEBOOK;
                mCurrentMode.setNotebookId(notebook.getId());
                if (mCallback != null) {
                    if (mCallback.onShowNotes(mCurrentMode)) {
                        close();
                    }
                }
            }

            @Override
            public void onClickedAddNotebook(final String parentNotebookId) {
                View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_sigle_edittext, null);
                final EditText mEdit = (EditText) view.findViewById(R.id.edit);
                new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.add_notebook)
                        .setView(view)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                addNotebook(mEdit.getText().toString(), parentNotebookId);
                            }
                        })
                        .show();
            }
        });
        mNotebookRv.setAdapter(mNotebookAdapter);
        mNotebookAdapter.refresh();
        mNotebookTr.setOnToggleListener(new TriangleView.OnToggleListener() {
            @Override
            public void onToggle(boolean isChecked) {
                mNotebookRv.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void addNotebook(final String title, final String parentNotebookId) {
        Observable.create(
                new Observable.OnSubscribe<Notebook>() {
                    @Override
                    public void call(Subscriber<? super Notebook> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(NotebookService.addNotebook(title, parentNotebookId));
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Notebook>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Notebook isSucceed) {
                        mNotebookAdapter.refresh();
                    }
                });
    }

    private void refreshUserInfo(Account account) {
        mUserNameTv.setText(account.getUserName());
        mEmailTv.setText(account.getEmail());
        if (!TextUtils.isEmpty(account.getAvatar())) {
            Glide.with(mActivity)
                    .load(account.getAvatar())
                    .centerCrop()
                    .bitmapTransform(new CropCircleTransformation(mActivity))
                    .into(mAvatarIv);
        }
    }

    public void refresh() {
        refreshUserInfo(AccountService.getCurrent());
        mAccountAdapter.load(AccountService.getAccountList());
        mTagAdapter.refresh();
        mNotebookAdapter.refresh();
        if (mCurrentMode == Mode.NOTEBOOK && TextUtils.isEmpty(mNotebookAdapter.getCurrentParentId())) {
            mCurrentMode = Mode.RECENT_NOTES;
        }
        if (mCallback != null) {
            if (mCallback.onShowNotes(mCurrentMode)) {
                close();
            }
        }
    }

    public void toggle() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START, true);
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START, true);
        }
    }

    public boolean isOpen() {
        return mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void open() {
        if (!isOpen()) {
            mDrawerLayout.openDrawer(GravityCompat.START, true);
        }
    }

    public void close() {
        if (isOpen()) {
            mDrawerLayout.closeDrawer(GravityCompat.START, true);
        }
    }

    public void onResume() {
        refresh();
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ADD_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                Account account = AccountService.getAccountById(SignInActivity.getAccountIdFromData(data));
                if (account != null) {
                    changeAccount(account);
                }
            }
            return true;
        }
        return false;
    }

    @OnClick(R.id.rl_notebook)
    void clickedNotebook() {
        mNotebookTr.toggle();
    }

    @OnClick(R.id.rl_tag)
    void clickedTag() {
        mTagTr.toggle();
    }

    @OnClick(R.id.rl_settings)
    void clickedSettings() {
        if (mCallback != null) {
            mCallback.onClickSetting();
        }
    }

    @OnClick(R.id.iv_avatar)
    void clickedAvatar() {
        mAccountTr.toggle();
    }

    @OnClick(R.id.rl_about)
    void clickedAbout() {
        if (mCallback != null) {
            mCallback.onClickAbout();
        }
    }

    @OnClick(R.id.rl_recent_notes)
    void clickedRecent() {
        mCurrentMode = Mode.RECENT_NOTES;
        if (mCallback != null) {
            if (mCallback.onShowNotes(mCurrentMode)) {
                close();
            }
        }
    }

    public interface Callback {
        boolean onChangeAccount(Account account);

        /**
         * @param mode
         * @return true if processed
         */
        boolean onShowNotes(Mode mode);

        void onClickSetting();

        void onClickAbout();
    }

    public enum Mode {
        RECENT_NOTES,
        NOTEBOOK,
        TAG;

        long notebookId;
        String tagText;

        public void setNotebookId(long notebookId) {
            this.notebookId = notebookId;
        }

        public void setTagText(String tagText) {
            this.tagText = tagText;
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "notebookId=" + notebookId +
                    ", tagText='" + tagText + '\'' +
                    '}';
        }
    }
}
