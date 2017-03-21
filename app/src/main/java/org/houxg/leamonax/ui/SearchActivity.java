package org.houxg.leamonax.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import org.houxg.leamonax.R;
import org.houxg.leamonax.utils.DisplayUtils;
import org.houxg.leamonax.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.search)
    SearchView mSearchView;

    private NoteFragment mNoteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        initToolBar(mToolbar, true);
        setTitle("");
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                NoteFragment.Mode mode = NoteFragment.Mode.SEARCH;
                mode.setKeywords(newText);
                mNoteFragment.setMode(mode);
                return true;
            }
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        mNoteFragment = NoteFragment.newInstance();
        mNoteFragment.setOnSearchFinishListener(new NoteFragment.OnSearchFinishListener() {
            @Override
            public void doSearchFinish() {
                ToastUtils.show(SearchActivity.this, R.string.activity_search_note_not_found);
                DisplayUtils.hideKeyboard(mSearchView);
            }
        });
        transaction.add(R.id.container, mNoteFragment);
        transaction.commit();

        ImageView searchCloseIcon = (ImageView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchCloseIcon.setImageResource(R.drawable.ic_clear);
        final ImageView searchIcon = (ImageView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        mSearchView.post(new Runnable() {
            @Override
            public void run() {
                searchIcon.setImageDrawable(null);
                searchIcon.setVisibility(View.GONE);
            }
        });
        SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setHintTextColor(getResources().getColor(R.color.menu_text));
        searchAutoComplete.setTextColor(getResources().getColor(R.color.menu_text));
        mSearchView.setIconified(false);
        mSearchView.setIconifiedByDefault(false);
    }
}
