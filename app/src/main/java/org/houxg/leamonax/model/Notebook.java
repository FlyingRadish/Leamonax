package org.houxg.leamonax.model;

import com.google.gson.annotations.SerializedName;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.houxg.leamonax.database.AppDataBase;

import java.util.List;

/**
 * Created by binnchx on 11/1/15.
 */
@Table(name = "Notebook", database = AppDataBase.class)
public class Notebook extends BaseModel {

    @SerializedName("Ok")
    boolean isOk = true;
    @SerializedName("Msg")
    String msg;

    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    long id;
    @Column(name = "notebookId")
    @SerializedName("NotebookId")
    String notebookId;
    @Column(name = "parentNotebookId")
    @SerializedName("ParentNotebookId")
    String parentNotebookId;
    @Column(name = "userId")
    @SerializedName("UserId")
    String userId;
    @Column(name = "title")
    @SerializedName("Title")
    String title;
    String urlTitle;
    @Column(name = "seq")
    @SerializedName("Seq")
    int seq;
    @SerializedName("IsBlog")
    boolean isBlog;
    @Column(name = "createdTime")
    @SerializedName("CreatedTime")
    String createTime;
    @Column(name = "updatedTime")
    @SerializedName("UpdatedTime")
    String updateTime;
    @Column(name = "isDirty")
    boolean isDirty;
    @Column(name = "isDeletedOnServer")
    @SerializedName("IsDeleted")
    boolean isDeleted;
    @Column(name = "isTrash")
    boolean isTrash;
    @Column(name = "usn")
    @SerializedName("Usn")
    int usn;

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isBlog() {
        return isBlog;
    }

    public void setIsBlog(boolean isBlog) {
        this.isBlog = isBlog;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUrlTitle() {
        return urlTitle;
    }

    public void setUrlTitle(String urlTitle) {
        this.urlTitle = urlTitle;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getUsn() {
        return usn;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public String getParentNotebookId() {
        return parentNotebookId;
    }

    public void setParentNotebookId(String parentNotebookId) {
        this.parentNotebookId = parentNotebookId;
    }

    public boolean isTrash() {
        return isTrash;
    }

    public void setIsTrash(boolean isTrash) {
        this.isTrash = isTrash;
    }


    public boolean isOk() {
        return isOk;
    }

    public String getMsg() {
        return msg;
    }

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
