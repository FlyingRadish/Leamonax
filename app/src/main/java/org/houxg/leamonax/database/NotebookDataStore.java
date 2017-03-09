package org.houxg.leamonax.database;


import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Note_Table;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.Notebook_Table;

import java.util.List;

public class NotebookDataStore {
    public static List<Notebook> getAllNotebooks(String userId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.isDeletedOnServer.eq(false))
                .queryList();
    }

    public static Notebook getByLocalId(long localId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.id.eq(localId))
                .querySingle();
    }

    public static Notebook getByServerId(String serverId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.notebookId.eq(serverId))
                .querySingle();
    }

    public static Notebook getRecentNoteBook(String userId) {
        Note recentNotes = SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.notebookId.notEq(""))
                .orderBy(Note_Table.updatedTime, false)
                .querySingle();
        if (recentNotes != null) {
            Notebook notebook = getByServerId(recentNotes.getNoteBookId());
            if (notebook != null && !notebook.isDeleted()) {
                return notebook;
            }
        }

        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.isDeletedOnServer.eq(false))
                .querySingle();
    }

    public static List<Notebook> getRootNotebooks(String userId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.parentNotebookId.eq(""))
                .and(Notebook_Table.isDeletedOnServer.eq(false))
                .queryList();
    }

    public static List<Notebook> getChildNotebook(String notebookId, String userId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.parentNotebookId.eq(notebookId))
                .and(Notebook_Table.isDeletedOnServer.eq(false))
                .queryList();
    }

    public static void deleteAll(String userId) {
        SQLite.delete()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .execute();
    }
}
