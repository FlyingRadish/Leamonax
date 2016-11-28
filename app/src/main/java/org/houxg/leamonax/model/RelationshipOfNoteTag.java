package org.houxg.leamonax.model;


import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.houxg.leamonax.database.AppDataBase;

@Table(name = "RelationshipOfNoteTag", database = AppDataBase.class)
public class RelationshipOfNoteTag extends BaseModel {
    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    long id;

    @Column(name = "noteLocalId")
    long noteLocalId;

    @Column(name = "tagLocalId")
    long tagLocalId;

    @Column(name = "userId")
    String userId;

    RelationshipOfNoteTag() {
    }

    public RelationshipOfNoteTag(long noteLocalId, long tagLocalId, String userId) {
        this.noteLocalId = noteLocalId;
        this.tagLocalId = tagLocalId;
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public long getNoteLocalId() {
        return noteLocalId;
    }

    public long getTagLocalId() {
        return tagLocalId;
    }
}
