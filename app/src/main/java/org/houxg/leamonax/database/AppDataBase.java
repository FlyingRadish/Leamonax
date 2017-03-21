package org.houxg.leamonax.database;

import android.database.Cursor;
import android.text.TextUtils;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Account_Table;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Note_Table;
import org.houxg.leamonax.model.RelationshipOfNoteTag;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.Tag_Table;

@Database(name = "leanote_db", version = 4)
public class AppDataBase {

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

    @Migration(version = 3, priority = 0, database = AppDataBase.class)
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
                    SQLite.update(Account.class)
                            .set(Account_Table.notebookUsn.eq(lastUsn), Account_Table.noteUsn.eq(lastUsn))
                            .where(Account_Table.id.eq(account.getLocalUserId()))
                            .execute(database);
                }
            }
            cursor.close();
        }
    }

    @Migration(version = 4, priority = 1, database = AppDataBase.class)
    public static class AddColLastUseTime extends AlterTableMigration<Account> {

        public AddColLastUseTime(Class<Account> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            super.onPreMigrate();
            addColumn(SQLiteType.INTEGER, "lastUseTime");
        }
    }

    @Migration(version = 4, priority = 0, database = AppDataBase.class)
    public static class UpdateLastUseTime extends BaseMigration {

        @Override
        public void migrate(DatabaseWrapper database) {
            Cursor cursor = SQLite.select()
                    .from(Account.class)
                    .where(Account_Table.token.notEq(""))
                    .query(database);
            if (cursor == null) {
                return;
            }
            int idIndex = cursor.getColumnIndex("id");
            while (cursor.moveToNext()) {
                int id = cursor.getInt(idIndex);
                Account account = SQLite.select()
                        .from(Account.class)
                        .where(Account_Table.id.eq(id))
                        .querySingle(database);
                if (account != null) {
                    account.updateLastUseTime();
                    SQLite.update(Account.class)
                            .set(Account_Table.lastUseTime.eq(account.getLastUseTime()))
                            .where(Account_Table.id.eq(account.getLocalUserId()))
                            .execute(database);
                }
            }
            cursor.close();
        }
    }
}
