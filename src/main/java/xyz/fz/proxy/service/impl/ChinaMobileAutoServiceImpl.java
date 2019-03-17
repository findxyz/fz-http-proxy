package xyz.fz.proxy.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.fz.proxy.model.ChinaMobileRecord;
import xyz.fz.proxy.service.AutoService;
import xyz.fz.proxy.util.BaseUtil;
import xyz.fz.proxy.util.ThreadUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChinaMobileAutoServiceImpl implements AutoService, InitializingBean {

    private static Logger LOGGER = LoggerFactory.getLogger(ChinaMobileAutoServiceImpl.class);

    private static Pattern TEL_PATTERN = Pattern.compile("DeviceUDID=[0-9]+&(.*)&A;");

    private static File RECORD_FILE = new File("data/china_mobile_records.json");

    private static final String HEADER_SPLIT = "___###___";

    private List<ChinaMobileRecord> records;

    @Value("${china.mobile.seconds}")
    private int chinaMobileSeconds;

    @Override
    public String autoUrl() {
        return "http://h5.ha.chinamobile.com/hnmccClientWap/weiboSign7h/signDraw4h.do";
    }

    @Override
    public void recordRequest(DefaultHttpRequest defaultHttpRequest, DefaultLastHttpContent defaultLastHttpContent) {
        ChinaMobileRecord record = new ChinaMobileRecord();
        List<String> headers = new ArrayList<>();
        headers.add("Host" + HEADER_SPLIT + defaultHttpRequest.headers().get("Host"));
        headers.add("Proxy-Connection" + HEADER_SPLIT + defaultHttpRequest.headers().get("Proxy-Connection"));
        headers.add("Accept" + HEADER_SPLIT + defaultHttpRequest.headers().get("Accept"));
        headers.add("Origin" + HEADER_SPLIT + defaultHttpRequest.headers().get("Origin"));
        headers.add("X-Requested-With" + HEADER_SPLIT + defaultHttpRequest.headers().get("X-Requested-With"));
        headers.add("User-Agent" + HEADER_SPLIT + defaultHttpRequest.headers().get("User-Agent"));
        headers.add("Content-Type" + HEADER_SPLIT + defaultHttpRequest.headers().get("Content-Type"));
        headers.add("Referer" + HEADER_SPLIT + defaultHttpRequest.headers().get("Referer"));
        headers.add("Accept-Encoding" + HEADER_SPLIT + defaultHttpRequest.headers().get("Accept-Encoding"));
        headers.add("Accept-Language" + HEADER_SPLIT + defaultHttpRequest.headers().get("Accept-Language"));
        headers.add("Cookie" + HEADER_SPLIT + defaultHttpRequest.headers().get("Cookie"));
        record.setHeaders(headers);

        Matcher matcher = TEL_PATTERN.matcher(defaultHttpRequest.headers().get("Cookie"));
        if (matcher.find()) {
            String tel = matcher.group(1);
            record.setTel(tel);
        }

        String body = defaultLastHttpContent.content().toString(Charset.forName("utf-8"));
        record.setBody(body);

        recordStore(record);
    }

    private synchronized void recordStore(ChinaMobileRecord record) {
        if (records.size() > 0) {
            records.removeIf(r -> r.getTel().equals(record.getTel()));
        }
        records.add(record);
        try {
            FileUtils.writeStringToFile(RECORD_FILE, BaseUtil.toJson(records), "utf-8");
            LOGGER.warn("new record added: \n{}", BaseUtil.toJson(record));
        } catch (Exception ignore) {
        }
    }

    @Override
    public void autoRequest() {
        String autoUrl = autoUrl() + "?r=" + Math.random() + "&rn=&pn=&jf=&ts=&pd=";
        for (ChinaMobileRecord record : records) {
            Runnable runnable = () -> {
                try {
                    long randomSeconds = (long) (chinaMobileSeconds * 1000 * Math.random());
                    LOGGER.warn("tel: {}, random delay: {}", record.getTel(), randomSeconds);
                    Thread.sleep(randomSeconds);
                    OkHttpClient client = new OkHttpClient();
                    Request.Builder requestBuilder = new Request.Builder();
                    requestBuilder.url(autoUrl);
                    requestBuilder.method(
                            "POST",
                            RequestBody.create(MediaType.get("application/x-www-form-urlencoded"), record.getBody())
                    );
                    for (String header : record.getHeaders()) {
                        String[] headers = header.split(HEADER_SPLIT);
                        requestBuilder.addHeader(headers[0], headers[1]);
                    }
                    try (Response response = client.newCall(requestBuilder.build()).execute()) {
                        if (response.isSuccessful()) {
                            ResponseBody responseBody = response.body();
                            if (responseBody != null) {
                                LOGGER.warn("tel: {}, result: {}", record.getTel(), responseBody.string());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error(BaseUtil.getExceptionStackTrace(e));
                    }
                } catch (Exception e) {
                    LOGGER.error(BaseUtil.getExceptionStackTrace(e));
                }
            };
            ThreadUtil.executor().execute(runnable);
        }
    }

    public String recordsSnapshot() {
        try {
            return BaseUtil.toJson(records);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            records = objectMapper.readValue(
                    FileUtils.readFileToString(RECORD_FILE),
                    new TypeReference<List<ChinaMobileRecord>>() {
                    }
            );
        } catch (Exception e) {
            records = new ArrayList<>();
        }

        autoRequest();

        DateTime now = new DateTime();
        DateTime tomorrowMorning = now.withTimeAtStartOfDay().plusDays(1).plusHours(7).plusMinutes(30);
        ThreadUtil.executor().scheduleAtFixedRate(() -> {
            try {
                autoRequest();
            } catch (Exception e) {
                LOGGER.warn(BaseUtil.getExceptionStackTrace(e));
            }
        }, Seconds.secondsBetween(now, tomorrowMorning).getSeconds(), 24 * 60 * 60, TimeUnit.SECONDS);
    }
}
