package org.houxg.leamonax.service;


import com.elvishew.xlog.XLog;

import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.network.ApiProvider;
import org.houxg.leamonax.utils.RetrofitUtils;

public class NotebookService {

    private static final String TAG = "NotebookService:";

    public static Notebook addNotebook(String title, String parentNotebookId) {
        Notebook notebook = RetrofitUtils.excute(ApiProvider.getInstance().getNotebookApi().addNotebook(title, parentNotebookId));
        if (notebook == null) {
            throw new IllegalStateException("Network error");
        }
        if (notebook.isOk()) {
            Account account = AccountService.getCurrent();
            if (notebook.getUsn() - account.getNotebookUsn() == 1) {
                XLog.d(TAG + "update usn=" + notebook.getUsn());
                account.setNotebookUsn(notebook.getUsn());
                account.save();
            }
            notebook.insert();
            return notebook;
        } else {
            throw new IllegalStateException(notebook.getMsg());
        }
    }

    public static String getTitle(long notebookLocalId) {
        Notebook notebook = Notebook.getByLocalId(notebookLocalId);
        return notebook != null ? notebook.getTitle() : "";
    }
}
