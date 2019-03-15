package xyz.fz.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        DefaultHttpProxyServer.bootstrap()
                .withAddress(new InetSocketAddress("0.0.0.0", 8080))
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new HttpFiltersAdapter(originalRequest) {
                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                if (httpObject instanceof DefaultHttpRequest &&
                                        !originalRequest.method().toString().equals("CONNECT")) {
                                    System.out.println(originalRequest.uri());
                                    System.out.println(originalRequest.method());
                                    System.out.println(originalRequest.headers());
                                }
                                return null;
                            }

                            @Override
                            public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                return httpObject;
                            }
                        };
                    }
                })
                .start();
    }
}
