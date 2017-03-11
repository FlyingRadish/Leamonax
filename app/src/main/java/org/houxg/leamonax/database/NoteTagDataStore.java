package org.houxg.leamonax.database;


import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.houxg.leamonax.model.RelationshipOfNoteTag;
import org.houxg.leamonax.model.RelationshipOfNoteTag_Table;

public class NoteTagDataStore {
    public static void deleteAll(String userId) {
        SQLite.delete()
                .from(RelationshipOfNoteTag.class)
                .where(RelationshipOfNoteTag_Table.userId.eq(userId))
                .execute();
    }
}
