package org.houxg.leamonax.model;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.TimeUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * Created by binnchx on 10/18/15.
 */
@Table(name = "Note", database = AppDataBase.class)
public class Note extends BaseModel implements Serializable {

    @SerializedName("Ok")
    boolean isOk = true;
    @SerializedName("Msg")
    String msg;

    @Column(name = "noteId")
    @SerializedName("NoteId")
    String noteId = "";
    @Column(name = "notebookId")
    @SerializedName("NotebookId")
    String noteBookId = "";
    @Column(name = "userId")
    @SerializedName("UserId")
    String userId = "";
    @Column(name = "title")
    @SerializedName("Title")
    String title = "";
    @Column(name = "content")
    @SerializedName("Content")
    String content = "";
    @Column(name = "isMarkDown")
    @SerializedName("IsMarkdown")
    boolean isMarkDown;
    @Column(name = "isTrash")
    @SerializedName("IsTrash")
    boolean isTrash;
    @Column(name = "isDeleted")
    @SerializedName("IsDeleted")
    boolean isDeleted;
    @Column(name = "isBlog")
    @SerializedName("IsBlog")
    boolean isPublicBlog;
    @Column(name = "usn")
    @SerializedName("Usn")
    int usn;

    @SerializedName("Tags")
    List<String> tagData;
    @SerializedName("Files")
    List<NoteFile> noteFiles;
    @SerializedName("UpdatedTime")
    String updatedTimeData = "";
    @SerializedName("CreatedTime")
    String createdTimeData = "";
    @SerializedName("PublicTime")
    String publicTimeData = "";

    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    Long id;
    Long localNotebookId;
    @Column(name = "desc")
    String desc = "";
    @Column(name = "noteAbstract")
    String noteAbstract = "";
    String fileIds;
    @Column(name = "isDirty")
    boolean isDirty;
    @Column(name = "isUploading")
    boolean isUploading;
    @Column(name = "createdTime")
    long createdTime;
    @Column(name = "updatedTime")
    long updatedTime;
    @Column(name = "publicTime")
    long publicTime;
    @Column(name = "tags")
    String tags = "";
    boolean uploadSucc = true;

    public long getCreatedTimeVal() {
        return createdTime;
    }

    public void setCreatedTimeVal(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getUpdatedTimeVal() {
        return updatedTime;
    }

    public void setUpdatedTimeVal(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public long getPublicTimeVal() {
        return publicTime;
    }

    public void setPublicTimeVal(long publicTime) {
        this.publicTime = publicTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNoteBookId() {
        return noteBookId;
    }

    public void setNoteBookId(String noteBookId) {
        this.noteBookId = noteBookId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isMarkDown() {
        return isMarkDown;
    }

    public void setIsMarkDown(boolean isMarkDown) {
        this.isMarkDown = isMarkDown;
    }

    public boolean isTrash() {
        return isTrash;
    }

    public void setIsTrash(boolean isTrash) {
        this.isTrash = isTrash;
    }

    public int getUsn() {
        return usn;
    }

    public boolean isUploadSucc() {
        return uploadSucc;
    }

    public void setUploadSucc(boolean uploadSucc) {
        this.uploadSucc = uploadSucc;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public void updateTags() {
        if (CollectionUtils.isEmpty(tagData)) {
            tags = "";
            return;
        }
        StringBuilder tagBuilder = new StringBuilder();
        int size = tagData.size();
        int lastIndex = size - 1;
        for (int i = 0; i < size; i++) {
            tagBuilder.append(tagData.get(i));
            if (i < lastIndex) {
                tagBuilder.append(",");
            }
        }
        tags = tagBuilder.toString();
    }

    public void updateTime() {
        createdTime = TimeUtils.toTimestamp(createdTimeData);
        updatedTime = TimeUtils.toTimestamp(updatedTimeData);
        publicTime = TimeUtils.toTimestamp(publicTimeData);
    }

    public List<NoteFile> getNoteFiles() {
        return noteFiles;
    }

    //TODO:delete this
    public String getUpdatedTime() {
        return updatedTimeData;
    }

    //TODO:delete this
    public String getCreatedTime() {
        return updatedTimeData;
    }

    //TODO:delete this
    public String getPublicTime() {
        return publicTimeData;
    }

    //TODO:delete this
    public void setUpdatedTime(String v) {
    }

    //TODO:delete this
    public void setCreatedTime(String v) {
    }

    //TODO:delete this
    public void setPublicTime(String publicTime) {
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", noteId='" + noteId + '\'' +
                ", noteBookId='" + noteBookId + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", tags='" + tags + '\'' +
                ", noteAbstract='" + noteAbstract + '\'' +
                ", content='" + content + '\'' +
                ", fileIds='" + fileIds + '\'' +
                ", isMarkDown=" + isMarkDown +
                ", isTrash=" + isTrash +
                ", isDeleted=" + isDeleted +
                ", isDirty=" + isDirty +
                ", isPublicBlog=" + isPublicBlog +
                ", createdTime='" + createdTime + '\'' +
                ", updatedTime='" + updatedTime + '\'' +
                ", publicTime='" + publicTime + '\'' +
                ", usn=" + usn +
                '}';
    }

    public boolean hasChanges(Note otherNote) {
        return otherNote == null
                || isChanged("title", title, otherNote.title)
                || isChanged("content", content, otherNote.content)
                || isChanged("notebookId", noteBookId, otherNote.noteBookId)
                || isChanged("isMarkDown", isMarkDown, otherNote.isMarkDown)
                || isChanged("tags", tags, otherNote.tags)
                || isChanged("isBlog", isPublicBlog, otherNote.isPublicBlog);
    }

    private boolean isChanged(String message, Object l, Object r) {
        boolean isEqual = l.equals(r);
        if (!isEqual) {
            Log.i("Note", message + " changed, origin=" + l + ", modified=" + r);
        }
        return !isEqual;
    }

    public boolean isPublicBlog() {
        return isPublicBlog;
    }

    public void setIsPublicBlog(boolean isPublicBlog) {
        this.isPublicBlog = isPublicBlog;
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

    public String getNoteAbstract() {
        return noteAbstract;
    }

    public void setNoteAbstract(String noteAbstract) {
        this.noteAbstract = noteAbstract;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileIds() {
        return fileIds;
    }

    public void setFileIds(String fileIds) {
        this.fileIds = fileIds;
    }

    public boolean isUploading() {
        return isUploading;
    }

    public void setIsUploading(boolean isUploading) {
        this.isUploading = isUploading;
    }

    public Long getLocalNotebookId() {
        return localNotebookId;
    }

    public void setLocalNotebookId(Long localNotebookId) {
        this.localNotebookId = localNotebookId;
    }

    public boolean isOk() {
        return isOk;
    }

    public String getMsg() {
        return msg;
    }

    public static class UpdateTimeComparetor implements Comparator<Note> {
        @Override
        public int compare(Note lhs, Note rhs) {
            long lTime = lhs.getUpdatedTimeVal();
            long rTime = rhs.getUpdatedTimeVal();
            if (lTime > rTime) {
                return -1;
            } else if (lTime < rTime) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
