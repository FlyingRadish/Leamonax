package org.houxg.leamonax;


import org.houxg.leamonax.utils.TimeUtils;
import org.junit.Assert;
import org.junit.Test;

public class TimeFormatTest {

    @Test
    public void testCompose() throws Exception {
        long time = 1480040991786l;
        String expectTime = "2016-11-25T10:29:51.786+08:00";
        Assert.assertEquals(expectTime, TimeUtils.toServerTime(time));
    }

    @Test
    public void testParse() throws Exception {
        String[] formatCases = new String[]{
                "2016-11-25T02:29:51.861+08:00",
                "2016-11-25T02:29:51.861+8:00",
                "2016-11-25T02:29:51.861+8:0",
                "2016-11-25T02:29:51.8612348761+08:00",
                "2016-11-25T02:29:51.8612348761+8:00",
                "2016-11-25T02:29:51.8612348761+8:0",
        };
        long expectTime = 1480012191861l;
        for (String format : formatCases) {
            Assert.assertEquals("test " + format, expectTime, TimeUtils.toTimestamp(format));
        }

        formatCases = new String[] {
                "2016-11-25T02:29:51+08:00",
                "2016-11-25T02:29:51+8:00",
                "2016-11-25T02:29:51+8:0"
        };
        expectTime = 1480012191000l;
        for (String format : formatCases) {
            Assert.assertEquals("test " + format, expectTime, TimeUtils.toTimestamp(format));
        }
    }
}
