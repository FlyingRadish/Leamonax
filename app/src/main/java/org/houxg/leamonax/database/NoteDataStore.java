package org.houxg.leamonax.database;


import com.raizlabs.android.dbflow.sql.language.Join;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.property.IProperty;

import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Note_Table;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.RelationshipOfNoteTag;
import org.houxg.leamonax.model.RelationshipOfNoteTag_Table;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.Tag_Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoteDataStore {
    public static List<Note> searchByTitle(String keyword) {
        keyword = String.format(Locale.US, "%%%s%%", keyword);
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(Account.getCurrent().getUserId()))
                .and(Note_Table.title.like(keyword))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDeleted.eq(false))
                .queryList();
    }

    public static Note getByServerId(String serverId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.noteId.eq(serverId))
                .querySingle();
    }

    public static Note getByLocalId(long localId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.id.eq(localId))
                .querySingle();
    }

    public static List<Note> getAllNotes(String userId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDeleted.eq(false))
                .and(Note_Table.isTrash.eq(false))
                .queryList();
    }

    public static List<Note> getAllDirtyNotes(String userId) {
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDeleted.eq(false))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDirty.eq(true))
                .queryList();
    }

    public static List<Note> getNotesFromNotebook(String userId, long localNotebookId) {
        Notebook notebook = NotebookDataStore.getByLocalId(localNotebookId);
        if (notebook == null) {
            return new ArrayList<>();
        }
        return SQLite.select()
                .from(Note.class)
                .where(Note_Table.notebookId.eq(notebook.getNotebookId()))
                .and(Note_Table.userId.eq(userId))
                .and(Note_Table.isTrash.eq(false))
                .and(Note_Table.isDeleted.eq(false))
                .and(Note_Table.isTrash.eq(false))
                .queryList();
    }

    public static List<Note> getByTagText(String tagText, String userId) {
        Tag tag = Tag.getByText(tagText, userId);
        if (tag == null) {
            return new ArrayList<>();
        }
        return getNotesByTagId(tag.getId());
    }

    private static List<Note> getNotesByTagId(long tagId) {
        IProperty[] properties = Note_Table.ALL_COLUMN_PROPERTIES;
        NameAlias nameAlias = NameAlias.builder("N").build();
        for (int i = 0; i < properties.length; i++) {
            properties[i] = properties[i].withTable(nameAlias);
        }
        return SQLite.select(properties)
                .from(Note.class).as("N")
                .join(RelationshipOfNoteTag.class, Join.JoinType.INNER).as("R")
                .on(Tag_Table.id.withTable(NameAlias.builder("N").build())
                        .eq(RelationshipOfNoteTag_Table.noteLocalId.withTable(NameAlias.builder("R").build())))
                .where(RelationshipOfNoteTag_Table.tagLocalId.withTable(NameAlias.builder("R").build()).eq(tagId))
                .queryList();
    }

    public static void deleteAll(String userId) {
        SQLite.delete()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .execute();
    }
}
