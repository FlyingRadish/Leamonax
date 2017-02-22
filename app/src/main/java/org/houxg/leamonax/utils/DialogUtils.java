package org.houxg.leamonax.utils;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.houxg.leamonax.R;
import org.houxg.leamonax.adapter.NotebookAdapter;
import org.houxg.leamonax.model.Notebook;

public class DialogUtils {

    public static void editLink(Context context, String title, String link, @NonNull final ChangedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_link, null);
        final EditText urlEdit = (EditText) view.findViewById(R.id.linkURL);
        final EditText titleEdit = (EditText) view.findViewById(R.id.linkText);
        titleEdit.setText(title);
        urlEdit.setText(link);
        new AlertDialog.Builder(context)
                .setTitle(R.string.link)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        listener.onChanged(titleEdit.getText().toString(), urlEdit.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static void selectNotebook(Context context, String title, final SelectNotebookListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_notebook, null);
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rv_notebook);
        final NotebookAdapter adapter =
                new NotebookAdapter()
                        .setCanOpenEmpty(false)
                        .setHasAddButton(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        adapter.refresh();
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        adapter.setListener(new NotebookAdapter.NotebookAdapterListener() {
            @Override
            public void onClickedNotebook(Notebook notebook) {
                listener.onNotebookSelected(notebook);
                dialog.dismiss();
            }

            @Override
            public void onClickedAddNotebook(String parentNotebookId) {

            }

            @Override
            public void onEditNotebook(Notebook notebook) {

            }
        });
        dialog.show();

    }

    public interface ChangedListener {
        void onChanged(String title, String link);
    }

    public interface SelectNotebookListener {
        void onNotebookSelected(Notebook notebook);
    }
}
