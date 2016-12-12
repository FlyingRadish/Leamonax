package org.houxg.leamonax.service;


import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.bson.types.ObjectId;
import org.houxg.leamonax.Leamonax;
import org.houxg.leamonax.model.Note;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

public class HtmlImporter {


    public Note from(File file) {
        String html;
        try {
            BufferedSource source = Okio.buffer(Okio.source(file));
            html = source.readUtf8();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Log.i("will", "html=" + html);
        if (TextUtils.isEmpty(html)) {
            return null;
        }

        Note note = new Note();
        String name = file.getName();
        note.setTitle(name.substring(0, name.lastIndexOf(".html")));
        note.setUserId(AccountService.getCurrent().getUserId());
        note.insert();

        Document document = Jsoup.parse(html);
        Elements imgs = document.select("img");
        File parentFile = file.getParentFile();
        for (Element imgNode : imgs) {
            String imgPath = imgNode.attr("src");
            Log.i("will", "img src=" + imgPath);
            File imageFile = new File(parentFile, imgPath);
            if (imageFile.isFile() && isImage(imageFile.getAbsolutePath())) {
                try {
                    String cacheName = new ObjectId().toString() + "." + getExtension(imageFile.getName());
                    File targetFile = new File(Leamonax.getContext().getCacheDir(), cacheName);
                    Source source = Okio.source(imageFile);
                    BufferedSink bufferedSink = Okio.buffer(Okio.sink(targetFile));
                    bufferedSink.writeAll(source);
                    bufferedSink.flush();
                    source.close();
                    bufferedSink.close();

                    Uri noteFile = NoteFileService.createImageFile(note.getId(), targetFile.getAbsolutePath());
                    imgNode.attr("src", noteFile.toString());
                } catch (FileNotFoundException e) {
                    Log.w("will", "image file not exist");
                    e.printStackTrace();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(true)
                .charset("UTF-8")
                .syntax(Document.OutputSettings.Syntax.html);
        document.outputSettings(settings);
        String output = document.body().outerHtml();
        Log.i("will", "output=" + output);
        Log.i("will", "output=" + document.body().children().outerHtml());
        note.setContent(output);
        long time = System.currentTimeMillis();
        note.setCreatedTimeVal(time);
        note.setUpdatedTimeVal(time);
        note.update();
        Log.i("will", "output note=" + note.getId());
        return note;
    }

    private boolean isLocalPath(String path) {
        return true;
    }

    private boolean isImage(String path) {
        switch (getExtension(path)) {
            case "png":
            case "jpg":
            case "jpeg":
            case "bmp":
                return true;
            default:
                return false;
        }
    }

    private String getExtension(String fileName) {
        String ext = "";
        int i = fileName.lastIndexOf('.');

        if (i > 0 && i < fileName.length() - 1) {
            ext = fileName.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
