package org.houxg.leamonax.database;

import android.util.Log;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.sql.language.Join;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Account_Table;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.NoteFile;
import org.houxg.leamonax.model.NoteFile_Table;
import org.houxg.leamonax.model.Note_Table;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.Notebook_Table;
import org.houxg.leamonax.model.RelationshipOfNoteTag;
import org.houxg.leamonax.model.RelationshipOfNoteTag_Table;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.Tag_Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//TODO:upgrade to Ver.2, handle tags
@Database(name = "leanote_db", version = 1)
public class AppDataBase {

    private static final String TAG = "AppDataBase";

    public static void updateNoteSettings(long localId, String notebookId, String tags, boolean isBlog) {
        Note note = getNoteByLocalId(localId);
        if (note == null) {
            Log.i(TAG, "updateNoteSettings(), note not found");
            return;
        }
        note.setNoteBookId(notebookId);
        note.setIsPublicBlog(isBlog);
        note.setTags(tags);
        note.save();
    }

    public static void updateNoteTitle(long localId, String title) {
        Note note = getNoteByLocalId(localId);
        if (note == null) {
            Log.i(TAG, "updateNote(), note not found");
            return;
        }
        note.setTitle(title);
        note.save();
    }

    public static void updateNoteContent(long localId, String content) {
        Note note = getNoteByLocalId(localId);
        if (note == null) {
            Log.i(TAG, "updateNote(), note not found");
            return;
        }
        note.setContent(content);
        note.save();
    }

    public static void deleteNoteByLocalId(long localId) {
        SQLite.delete().from(Note.class)
                .where(Note_Table.id.eq(localId))
                .async()
                .execute();
    }

    public static Note getNoteByServerId(String serverId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.noteId.eq(serverId))
                .querySingle();
    }

    public static Note getNoteByLocalId(long localId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.id.eq(localId))
                .querySingle();
    }

    public static List<Note> getNotesFromNotebook(String userId, long localNotebookId) {
        Notebook notebook = getNotebookByLocalId(localNotebookId);
        if (notebook == null) {
            return new ArrayList<>();
        }
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.notebookId.eq(notebook.getNotebookId()))
                .and(Note_Table.userId.eq(userId))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDeleted.eq(false))
                .queryList();
    }

    public static List<Note> getAllNotes(String userId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDeleted.eq(false))
                .queryList();
    }

    public static Notebook getNotebookByServerId(String serverId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.notebookId.eq(serverId))
                .querySingle();
    }

    public static Notebook getNotebookByLocalId(long localId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.id.eq(localId))
                .querySingle();
    }

    public static Notebook getRecentNoteBook(String userId) {
        //FIXME:get recent notebook
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .querySingle();
    }

    public static List<Notebook> getAllNotebook(String userId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .queryList();
    }

    public static List<Notebook> getRootNotebooks(String userId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.parentNotebookId.eq(""))
                .queryList();
    }

    public static List<Notebook> getChildNotebook(String notebookId, String userId) {
        Log.i(TAG, "getChildNotebook(), parentId=" + notebookId);
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.parentNotebookId.eq(notebookId))
                .queryList();
    }

    public static List<String> getAllNotebookTitles(String userId) {
        List<String> titles = new ArrayList<>();
        List<Notebook> notebooks = getAllNotebook(userId);
        for (Notebook notebook : notebooks) {
            titles.add(notebook.getTitle());
        }
        return titles;
    }

    public static List<Notebook> getNoteisBlogList(String userId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.isBlog.eq(true))
                .queryList();
    }

    public static List<NoteFile> getAllRelatedFile(long noteLocalId) {
        return SQLite.select()
                .from(NoteFile.class)
                .where(NoteFile_Table.noteLocalId.eq(noteLocalId))
                .queryList();
    }

    public static NoteFile getNoteFileByLocalId(String localId) {
        return SQLite.select()
                .from(NoteFile.class)
                .where(NoteFile_Table.localId.eq(localId))
                .querySingle();
    }

    public static NoteFile getNoteFileByServerId(String serverId) {
        return SQLite.select()
                .from(NoteFile.class)
                .where(NoteFile_Table.serverId.eq(serverId))
                .querySingle();
    }

    public static void deleteFileExcept(long noteLocalId, Collection<String> excepts) {
        SQLite.delete()
                .from(NoteFile.class)
                .where(NoteFile_Table.noteLocalId.eq(noteLocalId))
                .and(NoteFile_Table.localId.notIn(excepts))
                .async()
                .execute();
    }

    public static Account getAccount(String email, String host) {
        return SQLite.select()
                .from(Account.class)
                .where(Account_Table.email.eq(email))
                .and(Account_Table.host.eq(host))
                .querySingle();
    }

    public static Account getAccountWithToken() {
        return SQLite.select()
                .from(Account.class)
                .where(Account_Table.token.notEq(""))
                .querySingle();
    }

    public static List<Tag> getTagByNoteLocalId(long noteLocalId) {
        return SQLite.select()
                .from(Tag.class).as("T")
                .join(RelationshipOfNoteTag.class, Join.JoinType.INNER).as("R")
                .on(Tag_Table.id.withTable(NameAlias.builder("T").build())
                        .eq(RelationshipOfNoteTag_Table.tagLocalId.withTable(NameAlias.builder("R").build())))
                .where(RelationshipOfNoteTag_Table.noteLocalId.withTable(NameAlias.builder("R").build()).eq(noteLocalId))
                .queryList();
    }

    public static Tag getTagByText(String text) {
        return SQLite.select()
                .from(Tag.class)
                .where(Tag_Table.text.eq(text))
                .querySingle();
    }
}
