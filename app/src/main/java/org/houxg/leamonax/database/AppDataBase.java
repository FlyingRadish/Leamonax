package org.houxg.leamonax.database;

import android.database.Cursor;
import android.text.TextUtils;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.language.Join;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.property.IProperty;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

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
import org.houxg.leamonax.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Database(name = "leanote_db", version = 3)
public class AppDataBase {

    private static final String TAG = "AppDataBase:";

    @Migration(version = 2, database = AppDataBase.class)
    public static class UpdateTag extends BaseMigration {

        @Override
        public void migrate(DatabaseWrapper database) {
            Cursor cursor = SQLite.select()
                    .from(Note.class)
                    .where(Note_Table.tags.notEq(""))
                    .query(database);
            if (cursor == null) {
                return;
            }
            int idIndex = cursor.getColumnIndex("id");
            int tagIndex = cursor.getColumnIndex("tags");
            int uidIndex = cursor.getColumnIndex("userId");
            while (cursor.moveToNext()) {
                String originalTagText = cursor.getString(tagIndex);
                String uid = cursor.getString(uidIndex);
                long noteLocalId = cursor.getLong(idIndex);
                String[] tagTexts = originalTagText.split(",");
                for (String tagText : tagTexts) {
                    tagText = tagText.trim();
                    if (TextUtils.isEmpty(tagText)) {
                        continue;
                    }
                    Tag tagModel = SQLite.select()
                            .from(Tag.class)
                            .where(Tag_Table.userId.eq(uid))
                            .and(Tag_Table.text.eq(tagText))
                            .querySingle(database);
                    long tagId;
                    if (tagModel == null) {
                        tagModel = new Tag(uid, tagText);
                        tagModel.insert(database);
                    }
                    tagId = tagModel.getId();

                    RelationshipOfNoteTag relationship = new RelationshipOfNoteTag(noteLocalId, tagId, uid);
                    relationship.insert(database);
                }
            }
            cursor.close();
        }
    }

    @Migration(version = 3, priority = 1, database = AppDataBase.class)
    public static class AddUsnColumn extends AlterTableMigration<Account> {

        public AddUsnColumn(Class<Account> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            super.onPreMigrate();
            addColumn(SQLiteType.INTEGER, "noteUsn");
            addColumn(SQLiteType.INTEGER, "notebookUsn");
        }
    }

    @Migration(version = 3, priority =  0, database = AppDataBase.class)
    public static class UpdateUsn extends BaseMigration {

        @Override
        public void migrate(DatabaseWrapper database) {
            Cursor cursor = SQLite.select()
                    .from(Account.class)
                    .query(database);
            if (cursor == null) {
                return;
            }
            int idIndex = cursor.getColumnIndex("id");
            int usnIndex = cursor.getColumnIndex("lastUsn");
            while (cursor.moveToNext()) {
                int lastUsn = cursor.getInt(usnIndex);
                int id = cursor.getInt(idIndex);
                Account account = SQLite.select()
                        .from(Account.class)
                        .where(Account_Table.id.eq(id))
                        .querySingle(database);
                if (account != null) {
                    account.setNoteUsn(lastUsn);
                    account.setNotebookUsn(lastUsn);
                    account.update(database);
                }
            }
            cursor.close();
        }
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
                .and(Note_Table.isTrash.eq(false))
                .queryList();
    }

    public static List<Note> getNotesByTagText(String tagText, String userId) {
        Tag tag = getTagByText(tagText, userId);
        if (tag == null) {
            return new ArrayList<>();
        }
        return getNotesByTagId(tag.getId());
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
        List<Note> recentNotes = SQLite.select()
                .from(Note.class)
                .where(Note_Table.userId.eq(userId))
                .and(Note_Table.notebookId.notEq(""))
                .orderBy(Note_Table.updatedTime, false)
                .queryList();

        if (CollectionUtils.isNotEmpty(recentNotes)) {
            for (Note note : recentNotes) {
                Notebook notebook = getNotebookByServerId(note.getNoteBookId());
                if (!notebook.isDeleted()) {
                    return notebook;
                }
            }
        }
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.isDeletedOnServer.eq(false))
                .querySingle();
    }

    public static List<Notebook> getAllNotebook(String userId) {
        return SQLite.select()
                .from(Notebook.class)
                .where(Notebook_Table.userId.eq(userId))
                .and(Notebook_Table.isDeletedOnServer.eq(false))
                .queryList();
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

    public static List<Note> getNotesByTagId(long tagId) {
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

    public static RelationshipOfNoteTag getRelationShip(long noteLocalId, long tagId, String userId) {
        return SQLite.select()
                .from(RelationshipOfNoteTag.class)
                .where(RelationshipOfNoteTag_Table.userId.eq(userId))
                .and(RelationshipOfNoteTag_Table.tagLocalId.eq(tagId))
                .and(RelationshipOfNoteTag_Table.noteLocalId.eq(noteLocalId))
                .querySingle();
    }

    public static Tag getTagByText(String text, String userId) {
        return SQLite.select()
                .from(Tag.class)
                .where(Tag_Table.userId.eq(userId))
                .and(Tag_Table.text.eq(text))
                .querySingle();
    }

    public static void deleteAllRelatedTags(long noteLocalId, String userId) {
        SQLite.delete()
                .from(RelationshipOfNoteTag.class)
                .where(RelationshipOfNoteTag_Table.userId.eq(userId))
                .and(RelationshipOfNoteTag_Table.noteLocalId.eq(noteLocalId))
                .async()
                .execute();
    }

    public static void deleteRelatedTags(long noteLocalId, String userId, long firstReservedId, long... reservedIds) {
        SQLite.delete()
                .from(RelationshipOfNoteTag.class)
                .where(RelationshipOfNoteTag_Table.userId.eq(userId))
                .and(RelationshipOfNoteTag_Table.noteLocalId.eq(noteLocalId))
                .and(RelationshipOfNoteTag_Table.id.notIn(firstReservedId, reservedIds))
                .async()
                .execute();
    }

    public static List<Tag> getAllTags(String userId) {
        return SQLite.select()
                .from(Tag.class)
                .where(Tag_Table.userId.eq(userId))
                .queryList();
    }
}
