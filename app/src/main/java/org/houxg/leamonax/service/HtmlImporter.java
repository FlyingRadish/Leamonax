package org.houxg.leamonax.service;


import android.net.Uri;
import android.util.Log;

import org.bson.types.ObjectId;
import org.houxg.leamonax.Leamonax;
import org.houxg.leamonax.model.Note;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class HtmlImporter {

    private boolean mShouldRemoveAttributes = false;
    private static final String NORMAL_TAGS = "a, img, ul, ol, div, p, br, h1, h2, h3, h4, h5, h6, i, b, del, ins, sub, sup, blockquote, code, pre";
    private Document.OutputSettings mOutPutSettings = new Document.OutputSettings()
            .prettyPrint(true)
            .charset("UTF-8")
            .syntax(Document.OutputSettings.Syntax.html);


    public void setPureContent(boolean isPureContent) {
        mShouldRemoveAttributes = isPureContent;
    }

    public Note from(File file) {
        Note note = new Note();
        String name = file.getName();
        note.setTitle(name.substring(0, name.lastIndexOf(".html")));
        note.setUserId(AccountService.getCurrent().getUserId());
        note.insert();

        Document document;
        try {
            document = Jsoup.parse(file, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Elements imgs = document.select("img");
        File parentFile = file.getParentFile();
        for (Element imgNode : imgs) {
            String imgPath = imgNode.attr("src");
            Log.i("will", "img src=" + imgPath);
            File imageFile = new File(parentFile, imgPath);
            if (imageFile.isFile()) {
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (mShouldRemoveAttributes) {
            for (Element element : document.body().select(NORMAL_TAGS)) {
                removeElementsAttributes(element);
            }
        }

        for (Element preElement : document.body().select("pre")) {
            Elements children = preElement.children();
            if (children.size() == 1 && "code".equals(children.first().nodeName())) {
                Element codeElement = children.first();
                String codeHtml = codeElement.html();
                codeElement.remove();
                preElement.html(codeHtml);

                preElement.addClass("ace-tomorrow");
                removeClassLike(preElement, "brush:\\w+");
                //to be compatible with desktop app, https://github.com/leanote/desktop-app/issues/192
                if (Pattern.compile("&lt;.+&gt;").matcher(codeHtml).find()) {
                    preElement.addClass("brush:html");
                } else {
                    preElement.addClass("brush:convert");
                }
            }
        }

        document.outputSettings(mOutPutSettings);
        String output = document.body().children().outerHtml();
        note.setContent(output);
        long time = System.currentTimeMillis();
        note.setCreatedTimeVal(time);
        note.setUpdatedTimeVal(time);
        note.setIsDirty(true);
        note.update();
        return note;
    }

    private void removeClassLike(Element element, String regex) {
        for (String cls : element.classNames()) {
            if (cls.matches(regex)) {
                element.removeClass(cls);
            }
        }
    }

    private void removeElementsAttributes(Element element) {
        List<String> removeAttributes = new ArrayList<>();
        Attributes attributes = element.attributes();
        for (Attribute attr : attributes) {
            if (shouldRemoveAttr(attr.getKey(), element.nodeName())) {
                removeAttributes.add(attr.getKey());
            }
        }

        for (String key : removeAttributes) {
            attributes.remove(key);
        }
    }

    private boolean shouldRemoveAttr(String attr, String nodeName) {
        switch (nodeName) {
            case "img":
                return !("src".equals(attr)
                        || "alt".equals(attr));
            case "a":
                return !("href".equals(attr));
            case "style":
            case "script":
                return !("type".equals(attr));
            default:
                return true;
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
