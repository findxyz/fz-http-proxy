package xyz.fz.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import xyz.fz.proxy.service.AutoService;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        Collection<AutoService> collection = context.getBeansOfType(AutoService.class).values();
        Map<String, AutoService> autoServiceMap = new HashMap<>();
        for (AutoService service : collection) {
            autoServiceMap.put(service.autoUrl(), service);
        }
        DefaultHttpProxyServer.bootstrap()
                .withAddress(new InetSocketAddress("0.0.0.0", 8888))
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new HttpFiltersAdapter(originalRequest) {
                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                if (httpObject instanceof DefaultLastHttpContent &&
                                        !originalRequest.method().toString().equals("CONNECT")) {
                                    for (Map.Entry<String, AutoService> entry : autoServiceMap.entrySet()) {
                                        if (originalRequest.uri().contains(entry.getKey())) {
                                            entry.getValue().recordRequest((DefaultHttpRequest) originalRequest, (DefaultLastHttpContent) httpObject);
                                        }
                                    }
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
        LOGGER.warn("http server has been started at {}", 9999);
        LOGGER.warn("http proxy server has been started at {}", 8888);
    }
}
