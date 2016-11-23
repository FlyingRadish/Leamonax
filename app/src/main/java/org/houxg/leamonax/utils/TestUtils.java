package org.houxg.leamonax.utils;


import org.houxg.leamonax.model.Note;

import java.security.SecureRandom;

public class TestUtils {

    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    public static Note randomNote(SecureRandom random, String notebookId, String userId) {
        Note note = new Note();
        note.setTitle(randomSentence(random));
        note.setContent(randomParagraph(random));
        note.setUserId(userId);
        note.setNoteBookId(notebookId);
        return note;
    }

    public static String randomWord(SecureRandom random) {
        int len = random.nextInt(10) + 1;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return builder.toString();
    }

    public static String randomSentence(SecureRandom random) {
        int len = random.nextInt(20) + 2;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(randomWord(random));
            builder.append(' ');
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public static String randomParagraph(SecureRandom random) {
        int len = random.nextInt(10) + 2;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(randomSentence(random));
            builder.append(random.nextBoolean() ? ',' : ".");
            builder.append(' ');
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
