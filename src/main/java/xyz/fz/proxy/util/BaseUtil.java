package xyz.fz.proxy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class BaseUtil {
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    public static <T> T parseJson(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    public static String getExceptionStackTrace(Throwable e) {
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            return sw.toString();
        } finally {
            try {
                if (sw != null) {
                    sw.close();
                }
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        DateTime now = new DateTime();
        DateTime tomorrowMorning = now.withTimeAtStartOfDay().plusDays(1).plusHours(7).plusMinutes(30);
        System.out.println(now);
        System.out.println(tomorrowMorning);
        System.out.println(Seconds.secondsBetween(now, tomorrowMorning).getSeconds());
    }
}
