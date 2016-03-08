package com.gmail.lionelg3.nxtraces.engine;

import com.gmail.lionelg3.nxtraces.rest.RSApplication;
import io.undertow.Undertow;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.proxy.SimpleProxyClientProvider;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import org.apache.commons.configuration.Configuration;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

import java.net.URI;
import java.net.URISyntaxException;

import static io.undertow.Handlers.*;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 21/12/15, Time: 14:25
 */
public class NXTraces {

    private static final Logger LOGGER = Logger.getLogger(NXTraces.class);

    public static Configuration CONFIGURATION = null;

    public void startup() throws URISyntaxException {
        // Config
        String internalHost = "127.0.0.1";
        String host = CONFIGURATION.getString("undertow.host");
        int ajpPort = CONFIGURATION.getInt("undertow.ajp_port");
        int httpPort = CONFIGURATION.getInt("undertow.http_port");
        String deploymentContext = CONFIGURATION.getString("deployment.context");

        // Rest server
        new UndertowJaxrsServer()
                .deploy(new RSApplication(), "/api")
                .start(
                        Undertow.builder().addHttpListener(httpPort + 1, internalHost)
                );
        LOGGER.debug("RestServer ready");

        // Proxy
        ProxyClient rsClient = new SimpleProxyClientProvider(
                new URI("http://" + internalHost + ":" + (httpPort + 1) + "/api")
        );

        // Web Server
        Undertow.builder()
                .addAjpListener(ajpPort, host)
                .addHttpListener(httpPort, host)
                .setHandler(
                        path()
                                .addPrefixPath("/",
                                        redirect("/" + deploymentContext + "/")
                                )
                                .addPrefixPath("/" + deploymentContext,
                                        resource(new ClassPathResourceManager(NXTraces.class.getClassLoader(), "web"))
                                )
                                .addPrefixPath("/api",
                                        new ProxyHandler(rsClient, 30000, ResponseCodeHandler.HANDLE_500)
                                )
                        //.addPrefixPath("/ws",
                        //		websocket(new InternalWebSocketHandler())
                        //)
                ).build().start();
        LOGGER.debug("WebServer ready");
    }
}
