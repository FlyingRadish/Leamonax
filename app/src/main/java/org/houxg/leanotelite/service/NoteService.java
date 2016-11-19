package org.houxg.leanotelite.service;


import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.bson.types.ObjectId;
import org.houxg.leanotelite.database.AppDataBase;
import org.houxg.leanotelite.model.Account;
import org.houxg.leanotelite.model.Note;
import org.houxg.leanotelite.model.NoteFile;
import org.houxg.leanotelite.model.Notebook;
import org.houxg.leanotelite.model.UpdateRe;
import org.houxg.leanotelite.network.ApiProvider;
import org.houxg.leanotelite.utils.CollectionUtils;
import org.houxg.leanotelite.utils.RetrofitUtils;
import org.houxg.leanotelite.utils.StringUtils;
import org.houxg.leanotelite.utils.TimeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

public class NoteService {

    private static final String TAG = "NoteService";
    private static final String TRUE = "1";
    private static final String FALSE = "0";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final int MAX_ENTRY = 20;

    public static boolean fetchFromServer() {
        int noteUsn = AccountService.getCurrent().getLastSyncUsn();
        int notebookUsn = noteUsn;
        List<Note> notes;
        do {
            notes = RetrofitUtils.excute(getSyncNotes(noteUsn, MAX_ENTRY));
            if (notes != null) {
                for (Note noteMeta : notes) {
                    Note remoteNote = RetrofitUtils.excute(getNoteByServerId(noteMeta.getNoteId()));
                    if (remoteNote == null) {
                        return false;
                    }
                    Note localNote = AppDataBase.getNoteByServerId(noteMeta.getNoteId());
                    noteUsn = remoteNote.getUsn();
                    long localId;
                    if (localNote == null) {
                        localId = remoteNote.insert();
                        remoteNote.setId(localId);
                        Log.i(TAG, "note insert, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId() + ", local=" + localId);
                    } else {
                        if (localNote.isDirty()) {
                            Log.w(TAG, "note conflict, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                            continue;
                        } else {
                            Log.i(TAG, "note update, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                            remoteNote.setId(localNote.getId());
                            localId = localNote.getId();
                        }
                    }
                    remoteNote.setIsDirty(false);
                    if (remoteNote.isMarkDown()) {
                        remoteNote.setContent(convertToLocalImageLinkForMD(localId, remoteNote.getContent()));
                    } else {
                        remoteNote.setContent(convertToLocalImageLinkForRichText(localId, remoteNote.getContent()));
                    }
                    Log.i(TAG, "content=" + remoteNote.getContent());
                    remoteNote.update();
                    handleFile(localId, remoteNote.getNoteFiles());
                }
            } else {
                return false;
            }
        } while (notes.size() == MAX_ENTRY);

        List<Notebook> notebooks;
        do {
            notebooks = RetrofitUtils.excute(getSyncNotebooks(notebookUsn, MAX_ENTRY));
            if (notebooks != null) {
                for (Notebook remoteNotebook : notebooks) {
                    Notebook localNotebook = AppDataBase.getNotebookByServerId(remoteNotebook.getNotebookId());
                    if (localNotebook == null) {
                        Log.i(TAG, "notebook insert, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                        remoteNotebook.insert();
                    } else {
                        if (localNotebook.isDirty()) {
                            Log.w(TAG, "notebook conflict, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                        } else {
                            Log.i(TAG, "notebook update, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                            remoteNotebook.setId(localNotebook.getId());
                            remoteNotebook.setIsDirty(false);
                            remoteNotebook.update();
                        }
                    }
                    notebookUsn = remoteNotebook.getUsn();
                }
            } else {
                return false;
            }
        } while (notebooks.size() == MAX_ENTRY);

        Log.i(TAG, "noteUsn=" + noteUsn + ", notebookUsn=" + notebookUsn);
        int max = Math.max(notebookUsn, noteUsn);
        saveLastUsn(max);
        return true;
    }

    private static void saveLastUsn(int lastUsn) {
        Account account = AccountService.getCurrent();
        account.setLastUsn(lastUsn);
        account.save();
    }

    private static void handleFile(long noteLocalId, List<NoteFile> remoteFiles) {
        if (CollectionUtils.isEmpty(remoteFiles)) {
            return;
        }
        Log.i(TAG, "file size=" + remoteFiles.size());
        List<String> excepts = new ArrayList<>();
        for (NoteFile remote : remoteFiles) {
            NoteFile local;
            if (TextUtils.isEmpty(remote.getLocalId())) {
                local = AppDataBase.getNoteFileByServerId(remote.getServerId());
            } else {
                local = AppDataBase.getNoteFileByLocalId(remote.getLocalId());
            }
            if (local != null) {
                Log.i(TAG, "has local file, id=" + remote.getServerId());
                local.setServerId(remote.getServerId());
            } else {
                Log.i(TAG, "need to insert, id=" + remote.getServerId());
                local = new NoteFile();
                local.setLocalId(new ObjectId().toString());
            }
            local.setServerId(remote.getServerId());
            local.setNoteId(noteLocalId);
            local.save();
            excepts.add(local.getLocalId());
        }
        AppDataBase.deleteFileExcept(noteLocalId, excepts);
    }

    public static String replace(String content, String tagExp, String targetExp, Replacer replacer, Object... extraData) {
        Pattern tagPattern = Pattern.compile(tagExp);
        Pattern targetPattern = Pattern.compile(targetExp);
        Matcher tagMather = tagPattern.matcher(content);
        StringBuilder contentBuilder = new StringBuilder(content);
        int offset = 0;
        while (tagMather.find()) {
            String tag = tagMather.group();
            Matcher targetMatcher = targetPattern.matcher(tag);
            if (!targetMatcher.find()) {
                continue;
            }
            String original = targetMatcher.group();
            int originalLen = original.length();
            String modified = replacer.replaceWith(original, extraData);
            contentBuilder.replace(tagMather.start() + targetMatcher.start() + offset,
                    tagMather.end() - (tag.length() - targetMatcher.end()) + offset,
                    modified);
            offset += modified.length() - originalLen;
        }
        return contentBuilder.toString();
    }

    public static void find(String content, String tagExp, String targetExp, Finder finder, Object... extraData) {
        Pattern tagPattern = Pattern.compile(tagExp);
        Pattern targetPattern = Pattern.compile(targetExp);
        Matcher tagMather = tagPattern.matcher(content);
        while (tagMather.find()) {
            String tag = tagMather.group();
            Matcher targetMatcher = targetPattern.matcher(tag);
            if (!targetMatcher.find()) {
                continue;
            }
            String original = targetMatcher.group();
            finder.onFound(original);
        }
    }

    public interface Finder {
        void onFound(String original, Object... extraData);
    }

    public interface Replacer {
        String replaceWith(String original, Object... extraData);
    }

    private static String convertToLocalImageLinkForRichText(long noteLocalId, String noteContent) {
        return replace(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                String.format(Locale.US, "\\ssrc\\s*=\\s*\"%s/api/file/getImage\\?fileId=.*?\"", AccountService.getCurrent().getHost()),
                new Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Log.i(TAG, "in=" + original);
                        Uri linkUri = Uri.parse(original.substring(6, original.length() - 1));
                        String serverId = linkUri.getQueryParameter("fileId");
                        NoteFile noteFile = AppDataBase.getNoteFileByServerId(serverId);
                        if (noteFile == null) {
                            noteFile = new NoteFile();
                            noteFile.setNoteId((Long) extraData[0]);
                            noteFile.setLocalId(new ObjectId().toString());
                            noteFile.setServerId(serverId);
                            noteFile.save();
                        }
                        String localId = noteFile.getLocalId();
                        String result = String.format(Locale.US, " src=\"%s\"", NoteFileService.getLocalImageUri(localId).toString());
                        Log.i(TAG, "out=" + result);
                        return result;
                    }
                }, noteLocalId);
    }

    private static String convertToLocalImageLinkForMD(long noteLocalId, String noteContent) {
        return replace(noteContent,
                String.format(Locale.US, "!\\[.*?\\]\\(%s/api/file/getImage\\?fileId=.*?\\)", AccountService.getCurrent().getHost()),
                String.format(Locale.US, "\\(%s/api/file/getImage\\?fileId=.*?\\)", AccountService.getCurrent().getHost()),
                new Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(1, original.length() - 1));
                        String serverId = linkUri.getQueryParameter("fileId");
                        NoteFile noteFile = AppDataBase.getNoteFileByServerId(serverId);
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

    public static boolean updateNote(final Note modifiedNote) {
        Note note;
        if (modifiedNote.getUsn() == 0) {
            note = RetrofitUtils.excute(addNote(modifiedNote));
        } else {
            Note remoteNote = RetrofitUtils.excute(getNoteByServerId(modifiedNote.getNoteId()));
            if (remoteNote == null) {
                return false;
            }
            note = RetrofitUtils.excute(updateNote(remoteNote, modifiedNote));
        }
        if (note == null) {
            return false;
        }
        if (note.isOk()) {
            note.setId(modifiedNote.getId());
            note.setNoteBookId(modifiedNote.getNoteBookId());
            note.setIsDirty(false);
            note.setContent(modifiedNote.getContent());
            handleFile(modifiedNote.getId(), note.getNoteFiles());
            note.save();
            updateUsnIfNeed(note.getUsn());
        } else {
            throw new IllegalArgumentException(note.getMsg());
        }
        return true;
    }

    private static String convertToServerImageLinkForMD(String noteContent) {
        return replace(noteContent,
                "!\\[.*?\\]\\(file:/getImage\\?id=.*?\\)",
                "\\(file:/getImage\\?id=.*?\\)",
                new Replacer() {
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
        return replace(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                "\\ssrc\\s*=\\s*\"file:/getImage\\?id=.*?\"",
                new Replacer() {
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

    private static Call<List<Note>> getSyncNotes(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNoteApi().getSyncNotes(afterUsn, maxEntry);
    }

    private static Call<List<Notebook>> getSyncNotebooks(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNotebookApi().getSyncNotebooks(afterUsn, maxEntry);
    }

    public static Call<Note> getNoteByServerId(String serverId) {
        return ApiProvider.getInstance().getNoteApi().getNoteAndContent(serverId);
    }

    public static boolean revertNote(String serverId) {
        Note serverNote = RetrofitUtils.excute(NoteService.getNoteByServerId(serverId));
        if (serverNote == null) {
            return false;
        }
        Note localNote = AppDataBase.getNoteByServerId(serverId);
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
        serverNote.save();
        return true;
    }

    public static Call<Note> addNote(Note note) {
        List<MultipartBody.Part> fileBodies = new ArrayList<>();

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
        long current = System.currentTimeMillis();
        requestBodyMap.put("CreatedTime", createPartFromString(TimeUtils.toServerTime(current)));
        requestBodyMap.put("UpdatedTime", createPartFromString(TimeUtils.toServerTime(current)));

        List<String> imageLocalIds;
        if (note.isMarkDown()) {
            imageLocalIds = getImagesFromContentForMD(note.getContent());
        } else {
            imageLocalIds = getImagesFromContentForRichText(note.getContent());
        }
        AppDataBase.deleteFileExcept(note.getId(), imageLocalIds);
        List<NoteFile> files = AppDataBase.getAllRelatedFile(note.getId());
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
        return ApiProvider.getInstance().getNoteApi().add(requestBodyMap, fileBodies);
    }

    private static List<String> getImagesFromContentForRichText(String noteContent) {
        final List<String> localIds = new ArrayList<>();
        find(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                "\\ssrc\\s*=\\s*\"file:/getImage\\?id=.*?\"",
                new Finder() {
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
        find(noteContent,
                "!\\[.*?\\]\\(file:/getImage\\?id=.*?\\)",
                "\\(file:/getImage\\?id=.*?\\)",
                new Finder() {
                    @Override
                    public void onFound(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(1, original.length() - 1));
                        String localId = linkUri.getQueryParameter("id");
                        localIds.add(localId);
                    }
                });
        return localIds;
    }

    private static Call<Note> updateNote(Note original, Note modified) {
        List<MultipartBody.Part> fileBodies = new ArrayList<>();

        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        String noteId = original.getNoteId();
        String content = modified.getContent();
        if (modified.isMarkDown()) {
            content = convertToServerImageLinkForMD(content);
        } else {
            content = convertToServerImageLinkForRichText(content);
        }
        requestBodyMap.put("NoteId", createPartFromString(noteId));
        requestBodyMap.put("NotebookId", createPartFromString(modified.getNoteBookId()));
        requestBodyMap.put("Usn", createPartFromString(String.valueOf(original.getUsn())));
        requestBodyMap.put("Title", createPartFromString(modified.getTitle()));
        requestBodyMap.put("Content", createPartFromString(content));
        requestBodyMap.put("IsMarkdown", createPartFromString(getBooleanString(modified.isMarkDown())));
        requestBodyMap.put("IsBlog", createPartFromString(getBooleanString(modified.isPublicBlog())));
        requestBodyMap.put("UpdatedTime", createPartFromString(TimeUtils.toServerTime(System.currentTimeMillis())));

        List<String> imageLocalIds;
        if (modified.isMarkDown()) {
            imageLocalIds = getImagesFromContentForMD(modified.getContent());
        } else {
            imageLocalIds = getImagesFromContentForRichText(modified.getContent());
        }
        AppDataBase.deleteFileExcept(modified.getId(), imageLocalIds);
        List<NoteFile> files = AppDataBase.getAllRelatedFile(modified.getId());
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

        if (!original.getNoteBookId().equals(modified.getNoteBookId())) {
            requestBodyMap.put("NotebookId", createPartFromString(modified.getNoteBookId()));
        }

        if (original.isTrash() != modified.isTrash()) {
            requestBodyMap.put("IsTrash", createPartFromString(getBooleanString(modified.isTrash())));
        }
        return ApiProvider.getInstance().getNoteApi().update(requestBodyMap, fileBodies);
    }

    public static void deleteNote(Note note) {
        if (TextUtils.isEmpty(note.getNoteId())) {
            AppDataBase.deleteNoteByLocalId(note.getId());
        } else {
            UpdateRe response = RetrofitUtils.excuteWithException(deleteNote(note.getNoteId(), note.getUsn()));
            if (response.isOk()) {
                AppDataBase.deleteNoteByLocalId(note.getId());
                updateUsnIfNeed(response.getUsn());
            } else {
                throw new IllegalStateException(response.getMsg());
            }
        }
    }

    private static void updateUsnIfNeed(int newUsn) {
        Account account = AccountService.getCurrent();
        if (newUsn - account.getLastSyncUsn() == 1) {
            account.setLastUsn(newUsn);
            account.update();
        }
    }

    public static Call<UpdateRe> deleteNote(String noteId, int usn) {
        return ApiProvider.getInstance().getNoteApi().delete(noteId, usn);
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
                Log.w(TAG, "not a file");
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
