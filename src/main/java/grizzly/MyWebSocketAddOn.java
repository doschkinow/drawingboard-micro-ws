package grizzly;

import org.glassfish.tyrus.spi.ServerContainer;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;


/**
 * WebSockets {@link AddOn} for the {@link HttpServer}.
 *
 * @author Alexey Stashok
 */
// keep this public to allow other developers use this with their own GrizzlyServerContainer alternative
// see https://java.net/jira/browse/TYRUS-317
public class MyWebSocketAddOn implements AddOn {

    private final ServerContainer serverContainer;
    private final String contextPath;

    MyWebSocketAddOn(ServerContainer serverContainer, String contextPath) {
        this.serverContainer = serverContainer;
        this.contextPath = contextPath;
    }

    @Override
    public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
        // Get the index of HttpServerFilter in the HttpServer filter chain
        final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);

        if (httpServerFilterIdx >= 0) {
            // Insert the WebSocketFilter right before HttpServerFilter
            builder.add(httpServerFilterIdx, new MyGrizzlyServerFilter(serverContainer, contextPath));
        }
    }
}
