package org.houxg.leamonax.service;


import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.elvishew.xlog.XLog;

import org.bson.types.ObjectId;
import org.houxg.leamonax.R;
import org.houxg.leamonax.ReadableException;
import org.houxg.leamonax.database.NoteDataStore;
import org.houxg.leamonax.database.NoteFileDataStore;
import org.houxg.leamonax.database.NotebookDataStore;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.NoteFile;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.RelationshipOfNoteTag;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.UpdateRe;
import org.houxg.leamonax.network.ApiProvider;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.RetrofitUtils;
import org.houxg.leamonax.utils.StringUtils;
import org.houxg.leamonax.utils.TimeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import rx.Observable;

public class NoteService {

    private static final String TAG = "NoteService:";
    private static final String TRUE = "1";
    private static final String FALSE = "0";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String CONFLICT_SUFFIX = "--conflict";
    private static final int MAX_ENTRY = 20;

    public static void pushToServer() {
        List<Note> notes = NoteDataStore.getAllDirtyNotes(Account.getCurrent().getUserId());
        for (Note note : notes) {
            if (!note.getTitle().endsWith(CONFLICT_SUFFIX)) {
                saveNote(note.getId());
            }
        }
    }

    public static void fetchFromServer() {
        //sync notebook
        int notebookUsn = Account.getCurrent().getNotebookUsn();
        List<Notebook> notebooks;
        do {
            notebooks = RetrofitUtils.excuteWithException(ApiProvider.getInstance().getNotebookApi().getSyncNotebooks(notebookUsn, MAX_ENTRY));
            for (Notebook remoteNotebook : notebooks) {
                Notebook localNotebook = NotebookDataStore.getByServerId(remoteNotebook.getNotebookId());
                if (localNotebook == null) {
                    XLog.i(TAG + "notebook insert, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                    remoteNotebook.insert();
                } else {
                    XLog.i(TAG + "notebook update, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                    remoteNotebook.setId(localNotebook.getId());
                    remoteNotebook.setIsDirty(false);
                    remoteNotebook.update();
                }
                notebookUsn = remoteNotebook.getUsn();
                Account account = Account.getCurrent();
                account.setNotebookUsn(notebookUsn);
                account.save();
            }
        } while (notebooks.size() == MAX_ENTRY);


        //sync note
        int noteUsn = Account.getCurrent().getNoteUsn();
        List<Note> notes;
        do {
            notes = RetrofitUtils.excuteWithException(ApiProvider.getInstance().getNoteApi().getSyncNotes(noteUsn, MAX_ENTRY));
            for (Note noteMeta : notes) {
                Note remoteNote = RetrofitUtils.excuteWithException(ApiProvider.getInstance().getNoteApi().getNoteAndContent(noteMeta.getNoteId()));
                Note localNote = NoteDataStore.getByServerId(noteMeta.getNoteId());
                noteUsn = remoteNote.getUsn();
                long localId;
                if (localNote == null) {
                    localId = remoteNote.insert();
                    remoteNote.setId(localId);
                    XLog.i(TAG + "note insert, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId() + ", local=" + localId);
                } else {
                    long id = localNote.getId();
                    if (localNote.isDirty()) {
                        XLog.w(TAG + "note conflict, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                        //save local version as a local note
                        localNote.setId(null);
                        localNote.setTitle(localNote.getTitle() + CONFLICT_SUFFIX);
                        localNote.setNoteId("");
                        localNote.insert();
                    }
                    XLog.i(TAG + "note update, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                    remoteNote.setId(id);
                    localId = localNote.getId();
                }
                remoteNote.setIsDirty(false);
                String content;
                if (remoteNote.isMarkDown()) {
                    content = convertToLocalImageLinkForMD(localId, remoteNote.getContent());
                } else {
                    content = convertToLocalImageLinkForRichText(localId, remoteNote.getContent());
                }
                XLog.i(TAG + "content=" + remoteNote.getContent());
                remoteNote.setContent(content);
                remoteNote.setNoteAbstract(content.length() < 500 ? content : content.substring(0, 500));
                remoteNote.update();
                handleFile(localId, remoteNote.getNoteFiles());
                updateTagsToLocal(localId, remoteNote.getTagData());
                Account account = Account.getCurrent();
                account.setNoteUsn(noteUsn);
                account.save();
            }
        } while (notes.size() == MAX_ENTRY);
    }

    private static void handleFile(long noteLocalId, List<NoteFile> remoteFiles) {
        if (CollectionUtils.isEmpty(remoteFiles)) {
            return;
        }
        XLog.i(TAG + "file size=" + remoteFiles.size());
        List<String> excepts = new ArrayList<>();
        for (NoteFile remote : remoteFiles) {
            NoteFile local;
            if (TextUtils.isEmpty(remote.getLocalId())) {
                local = NoteFileDataStore.getByServerId(remote.getServerId());
            } else {
                local = NoteFileDataStore.getByLocalId(remote.getLocalId());
            }
            if (local != null) {
                XLog.i(TAG + "has local file, id=" + remote.getServerId());
                local.setServerId(remote.getServerId());
            } else {
                XLog.i(TAG + "need to insert, id=" + remote.getServerId());
                local = new NoteFile();
                local.setLocalId(new ObjectId().toString());
            }
            local.setServerId(remote.getServerId());
            local.setNoteId(noteLocalId);
            local.save();
            excepts.add(local.getLocalId());
        }
        NoteFileDataStore.deleteExcept(noteLocalId, excepts);
    }

    private static String convertToLocalImageLinkForRichText(long noteLocalId, String noteContent) {
        return StringUtils.replace(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                String.format(Locale.US, "\\ssrc\\s*=\\s*\"%s/api/file/getImage\\?fileId=.*?\"", Account.getCurrent().getHost()),
                new StringUtils.Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        XLog.i(TAG + "in=" + original);
                        Uri linkUri = Uri.parse(original.substring(6, original.length() - 1));
                        String serverId = linkUri.getQueryParameter("fileId");
                        NoteFile noteFile = NoteFileDataStore.getByServerId(serverId);
                        if (noteFile == null) {
                            noteFile = new NoteFile();
                            noteFile.setNoteId((Long) extraData[0]);
                            noteFile.setLocalId(new ObjectId().toString());
                            noteFile.setServerId(serverId);
                            noteFile.save();
                        }
                        String localId = noteFile.getLocalId();
                        String result = String.format(Locale.US, " src=\"%s\"", NoteFileService.getLocalImageUri(localId).toString());
                        XLog.i(TAG + "out=" + result);
                        return result;
                    }
                }, noteLocalId);
    }

    private static String convertToLocalImageLinkForMD(long noteLocalId, String noteContent) {
        return StringUtils.replace(noteContent,
                String.format(Locale.US, "!\\[.*?\\]\\(%s/api/file/getImage\\?fileId=.*?\\)", Account.getCurrent().getHost()),
                String.format(Locale.US, "\\(%s/api/file/getImage\\?fileId=.*?\\)", Account.getCurrent().getHost()),
                new StringUtils.Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(1, original.length() - 1));
                        String serverId = linkUri.getQueryParameter("fileId");
                        NoteFile noteFile = NoteFileDataStore.getByServerId(serverId);
                        if (noteFile == null) {
                            noteFile = new NoteFile();
                            noteFile.setNoteId((Long) extraData[0]);
                            noteFile.setLocalId(new ObjectId().toString());
                            noteFile.setServerId(serverId);
                            noteFile.save();
                        }
                        String localId = noteFile.getLocalId();
                        return String.format(Locale.US, "(%s)", NoteFileService.getLocalImageUri(localId).toString());
                    }
                }, noteLocalId);
    }

    public static void saveNote(final long noteLocalId) {
        Note modifiedNote = NoteDataStore.getByLocalId(noteLocalId);

        Map<String, RequestBody> requestBodyMap = generateCommonBodyMap(modifiedNote);
        List<MultipartBody.Part> fileBodies = handleFileBodies(modifiedNote, requestBodyMap);
        Call<Note> call;
        if (modifiedNote.isLocalNote()) {
            call = ApiProvider.getInstance().getNoteApi().add(requestBodyMap, fileBodies);
        } else {
            Note remoteNote = RetrofitUtils.excuteWithException(ApiProvider.getInstance().getNoteApi().getNoteAndContent(modifiedNote.getNoteId()));
            if (remoteNote.getUsn() != modifiedNote.getUsn()) {
                remoteNote.setId(modifiedNote.getId());
                remoteNote.update();
                modifiedNote.setId(null);
                modifiedNote.setTitle(modifiedNote.getTitle() + CONFLICT_SUFFIX);
                modifiedNote.setNoteId("");
                modifiedNote.setIsDirty(true);
                modifiedNote.insert();
                throw new ReadableException(ReadableException.Error.CONFLICT, R.string.conflict_occurs);
            } else {
                requestBodyMap.put("NoteId", createPartFromString(modifiedNote.getNoteId()));
                requestBodyMap.put("Usn", createPartFromString(String.valueOf(modifiedNote.getUsn())));
                call = ApiProvider.getInstance().getNoteApi().update(requestBodyMap, fileBodies);
            }
        }
        Note note = RetrofitUtils.excuteWithException(call);
        if (note.isOk()) {
            note.setId(modifiedNote.getId());
            note.setNoteBookId(modifiedNote.getNoteBookId());
            note.setIsDirty(false);
            note.setContent(modifiedNote.getContent());
            handleFile(modifiedNote.getId(), note.getNoteFiles());
            updateTagsToLocal(modifiedNote.getId(), note.getTagData());
            note.save();
            updateNoteUsnIfNeed(note.getUsn());
        } else {
            throw new IllegalStateException(note.getMsg());
        }
    }

    private static String convertToServerImageLinkForMD(String noteContent) {
        return StringUtils.replace(noteContent,
                "!\\[.*?\\]\\(file:/getImage\\?id=.*?\\)",
                "\\(file:/getImage\\?id=.*?\\)",
                new StringUtils.Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(1, original.length() - 1));
                        String localId = linkUri.getQueryParameter("id");
                        String serverId = NoteFileService.convertFromLocalIdToServerId(localId);
                        if (TextUtils.isEmpty(serverId)) {
                            serverId = localId;
                        }
                        return String.format(Locale.US, "(%s)", NoteFileService.getServerImageUri(serverId).toString());
                    }
                });
    }

    private static String convertToServerImageLinkForRichText(String noteContent) {
        return StringUtils.replace(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                "\\ssrc\\s*=\\s*\"file:/getImage\\?id=.*?\"",
                new StringUtils.Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(6, original.length() - 1));
                        String localId = linkUri.getQueryParameter("id");
                        String serverId = NoteFileService.convertFromLocalIdToServerId(localId);
                        if (TextUtils.isEmpty(serverId)) {
                            serverId = localId;
                        }
                        return String.format(Locale.US, " src=\"%s\"", NoteFileService.getServerImageUri(serverId).toString());
                    }
                });
    }

    public static boolean revertNote(String serverId) {
        Note serverNote = RetrofitUtils.excute(ApiProvider.getInstance().getNoteApi().getNoteAndContent(serverId));
        if (serverNote == null) {
            return false;
        }
        Note localNote = NoteDataStore.getByServerId(serverId);
        long localId;
        if (localNote == null) {
            localId = serverNote.insert();
        } else {
            localId = localNote.getId();
        }
        serverNote.setId(localId);
        if (serverNote.isMarkDown()) {
            serverNote.setContent(convertToLocalImageLinkForMD(localId, serverNote.getContent()));
        } else {
            serverNote.setContent(convertToLocalImageLinkForRichText(localId, serverNote.getContent()));
        }
        handleFile(localId, serverNote.getNoteFiles());
        updateTagsToLocal(localId, serverNote.getTagData());
        serverNote.save();
        return true;
    }

    @NonNull
    private static Map<String, RequestBody> generateCommonBodyMap(Note note) {
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        String content = note.getContent();
        if (note.isMarkDown()) {
            content = convertToServerImageLinkForMD(content);
        } else {
            content = convertToServerImageLinkForRichText(content);
        }
        requestBodyMap.put("NotebookId", createPartFromString(note.getNoteBookId()));
        requestBodyMap.put("Title", createPartFromString(note.getTitle()));
        requestBodyMap.put("Content", createPartFromString(content));
        requestBodyMap.put("IsMarkdown", createPartFromString(getBooleanString(note.isMarkDown())));
        requestBodyMap.put("IsBlog", createPartFromString(getBooleanString(note.isPublicBlog())));
        requestBodyMap.put("IsTrash", createPartFromString(getBooleanString(note.isTrash())));
        requestBodyMap.put("CreatedTime", createPartFromString(TimeUtils.toServerTime(note.getCreatedTimeVal())));
        requestBodyMap.put("UpdatedTime", createPartFromString(TimeUtils.toServerTime(note.getUpdatedTimeVal())));

        List<Tag> tags = Tag.getByNoteLocalId(note.getId());

        if (CollectionUtils.isNotEmpty(tags)) {
            int size = tags.size();
            for (int index = 0; index < size; index++) {
                Tag tag = tags.get(index);
                requestBodyMap.put(String.format("Tags[%s]", index), createPartFromString(tag.getText()));
            }
        }

        return requestBodyMap;
    }

    @NonNull
    private static List<MultipartBody.Part> handleFileBodies(Note note, Map<String, RequestBody> requestBodyMap) {
        List<MultipartBody.Part> fileBodies = new ArrayList<>();
        List<String> imageLocalIds;
        if (note.isMarkDown()) {
            imageLocalIds = getImagesFromContentForMD(note.getContent());
        } else {
            imageLocalIds = getImagesFromContentForRichText(note.getContent());
        }
        NoteFileDataStore.deleteExcept(note.getId(), imageLocalIds);
        List<NoteFile> files = NoteFileDataStore.getAllRelated(note.getId());
        if (CollectionUtils.isNotEmpty(files)) {
            int size = files.size();
            for (int index = 0; index < size; index++) {
                NoteFile noteFile = files.get(index);
                requestBodyMap.put(String.format("Files[%s][LocalFileId]", index), createPartFromString(noteFile.getLocalId()));
                requestBodyMap.put(String.format("Files[%s][IsAttach]", index), createPartFromString(getBooleanString(noteFile.isAttach())));
                requestBodyMap.put(String.format("Files[%s][FileId]", index), createPartFromString(StringUtils.notNullStr(noteFile.getServerId())));
                boolean shouldUploadFile = TextUtils.isEmpty(noteFile.getServerId());
                requestBodyMap.put(String.format("Files[%s][HasBody]", index), createPartFromString(getBooleanString(shouldUploadFile)));
                if (shouldUploadFile) {
                    fileBodies.add(createFilePart(noteFile));
                }
            }
        }
        return fileBodies;
    }

    private static List<String> getImagesFromContentForRichText(String noteContent) {
        final List<String> localIds = new ArrayList<>();
        StringUtils.find(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                "\\ssrc\\s*=\\s*\"file:/getImage\\?id=.*?\"",
                new StringUtils.Finder() {
                    @Override
                    public void onFound(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(6, original.length() - 1));
                        String localId = linkUri.getQueryParameter("id");
                        localIds.add(localId);
                    }
                });
        return localIds;
    }

    private static List<String> getImagesFromContentForMD(String noteContent) {
        final List<String> localIds = new ArrayList<>();
        StringUtils.find(noteContent,
                "!\\[.*?\\]\\(file:/getImage\\?id=.*?\\)",
                "\\(file:/getImage\\?id=.*?\\)",
                new StringUtils.Finder() {
                    @Override
                    public void onFound(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(1, original.length() - 1));
                        String localId = linkUri.getQueryParameter("id");
                        localIds.add(localId);
                    }
                });
        return localIds;
    }

    public static void trashNotesOnLocal(Note note) {
        note.setIsTrash(true);
        note.setIsDirty(true);
        note.update();
    }

    public static void trashNote(Note note) {
        if (!note.isLocalNote()) {
            saveNote(note.getId());
        }
    }

    public static void deleteNote(Note note) {
        if (note.isLocalNote()) {
            note.delete();
        } else {
            Call<UpdateRe> call = ApiProvider.getInstance().getNoteApi().delete(note.getNoteId(), note.getUsn());
            UpdateRe response = RetrofitUtils.excuteWithException(call);
            if (response.isOk()) {
                note.delete();
                updateNoteUsnIfNeed(response.getUsn());
            } else {
                throw new IllegalStateException(response.getMsg());
            }
        }
    }

    /**
     * if new usn equals to (current usn + 1), then just simply update usn without syncing.
     */
    private static void updateNoteUsnIfNeed(int newUsn) {
        Account account = Account.getCurrent();
        if (newUsn - account.getNoteUsn() == 1) {
            account.setNoteUsn(newUsn);
            account.update();
        }
    }

    public static void updateTagsToLocal(long noteLocalId, List<String> tags) {
        String currentUid = Account.getCurrent().getUserId();
        if (tags == null) {
            tags = new ArrayList<>();
        }

        List<Long> reservedIds = new ArrayList<>();
        for (String tagText : tags) {
            if (TextUtils.isEmpty(tagText)) {
                continue;
            }
            Tag tag = Tag.getByText(tagText, currentUid);
            long tagId;
            long relationShipId;
            RelationshipOfNoteTag relationShip;
            if (tag == null) {
                tag = new Tag(currentUid, tagText);
                tagId = tag.insert();
            } else {
                tagId = tag.getId();
            }

            relationShip = Tag.getRelationShip(noteLocalId, tagId, currentUid);
            if (relationShip == null) {
                relationShip = new RelationshipOfNoteTag(noteLocalId, tagId, currentUid);
                relationShipId = relationShip.insert();
            } else {
                relationShipId = relationShip.getId();
            }
            reservedIds.add(relationShipId);
        }
        if (CollectionUtils.isEmpty(reservedIds)) {
            Tag.deleteAllRelatedTags(noteLocalId, currentUid);
        } else {
            Tag.deleteRelatedTags(noteLocalId,
                    currentUid,
                    reservedIds.get(0),
                    CollectionUtils.toPrimitive(reservedIds.subList(1, reservedIds.size()))
            );
        }
    }

    private static RequestBody createPartFromString(String content) {
        return RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), content);
    }

    private static String getBooleanString(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    private static MultipartBody.Part createFilePart(NoteFile noteFile) {
        File tempFile;
        try {
            tempFile = new File(noteFile.getLocalPath());
            if (!tempFile.isFile()) {
                XLog.w(TAG + "not a file");
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        String extension = MimeTypeMap.getFileExtensionFromUrl(tempFile.toURI().toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), tempFile);
        return MultipartBody.Part.createFormData(String.format("FileDatas[%s]", noteFile.getLocalId()), tempFile.getName(), fileBody);
    }
}
