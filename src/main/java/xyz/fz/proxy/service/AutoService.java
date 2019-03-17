package xyz.fz.proxy.service;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;

public interface AutoService {
    String autoUrl();

    void recordRequest(DefaultHttpRequest defaultHttpRequest, DefaultLastHttpContent defaultLastHttpContent);

    void autoRequest();
}
