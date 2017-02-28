package org.houxg.leamonax.model;


import com.google.gson.annotations.SerializedName;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Join;
import com.raizlabs.android.dbflow.sql.language.NameAlias;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.utils.TimeUtils;

import java.util.List;

@Table(name = "Tag", database = AppDataBase.class)
public class Tag extends BaseModel{

    @SerializedName("TagId")
    @Column(name = "tagId")
    String tagId;
    @SerializedName("UserId")
    @Column(name = "userId")
    String UserId;
    @SerializedName("Tag")
    @Column(name = "text")
    String text;
    @SerializedName("IsDeleted")
    @Column(name = "isDeleted")
    boolean isDeleted;
    @SerializedName("Usn")
    @Column(name = "usn")
    int usn;
    @SerializedName("CreatedTime")
    String createTimeData;
    @SerializedName("UpdatedTime")
    String updatedTimeData;

    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    long id;
    @Column(name = "createdTime")
    long createdTime;
    @Column(name = "updatedTime")
    long updatedTime;

    Tag() {}

    public Tag(String userId, String text) {
        UserId = userId;
        this.text = text;
    }

    public String getTagId() {
        return tagId;
    }

    public String getUserId() {
        return UserId;
    }

    public String getText() {
        return text;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public int getUsn() {
        return usn;
    }

    public long getId() {
        return id;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void updateTime() {
        createdTime = TimeUtils.toTimestamp(createTimeData);
        updatedTime = TimeUtils.toTimestamp(updatedTimeData);
    }

    public static Tag getByText(String text, String userId) {
        return SQLite.select()
                .from(Tag.class)
                .where(Tag_Table.userId.eq(userId))
                .and(Tag_Table.text.eq(text))
                .querySingle();
    }

    public static List<Tag> getAllTags(String userId) {
        return SQLite.select()
                .from(Tag.class)
                .where(Tag_Table.userId.eq(userId))
                .queryList();
    }

    public static List<Tag> getByNoteLocalId(long noteLocalId) {
        return SQLite.select()
                .from(Tag.class).as("T")
                .join(RelationshipOfNoteTag.class, Join.JoinType.INNER).as("R")
                .on(Tag_Table.id.withTable(NameAlias.builder("T").build())
                        .eq(RelationshipOfNoteTag_Table.tagLocalId.withTable(NameAlias.builder("R").build())))
                .where(RelationshipOfNoteTag_Table.noteLocalId.withTable(NameAlias.builder("R").build()).eq(noteLocalId))
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

    public static void deleteAll(String userId) {
        SQLite.delete()
                .from(Tag.class)
                .where(Tag_Table.userId.eq(userId))
                .execute();
    }
}
