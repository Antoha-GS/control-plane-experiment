package co.ifunny.envoy.api;

import co.ifunny.envoy.api.core.ConfigWatcher;
import co.ifunny.envoy.api.core.Watch;
import envoy.api.v2.Discovery;
import envoy.api.v2.Discovery.DiscoveryRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static co.ifunny.envoy.api.core.ResponseType.*;

public class XDSServer {

    final private static Logger logger = LoggerFactory.getLogger(XDSServer.class);

    final private static String TYPE_PREFIX = "type.googleapis.com/envoy.api.v2.";
    final private static String ENDPOINT_TYPE = TYPE_PREFIX + "ClusterLoadAssignment";
    final private static String CLUSTER_TYPE = TYPE_PREFIX + "Cluster";
    final private static String ROUTE_TYPE = TYPE_PREFIX + "RouteConfiguration";
    final private static String LISTENER_TYPE = TYPE_PREFIX + "Listener";
    final private static String ANY_TYPE = "";


    final private ConfigWatcher watcher;
    final private AtomicLong streamCount = new AtomicLong(0);

    public XDSServer(ConfigWatcher watcher) {
        this.watcher = watcher;
    }

    public void process(StreamObserver<Discovery.DiscoveryResponse> responseObserver, String defaultTypeURL) {
        long streamId = streamCount.incrementAndGet();
        long streamNonce = 0;
        Watches watches = new Watches();

        new StreamObserver<DiscoveryRequest>() {

            @Override
            public void onNext(DiscoveryRequest request) {
                // nonces can be reused across streams; we verify nonce only if nonce is not initialized
                String nonce = request.getResponseNonce();

                // type URL is required for ADS but is implicit for xDS
                String typeUrl = request.getTypeUrl();
                if ("".equals(typeUrl)) {
                    if (ANY_TYPE.equals(defaultTypeURL)) {
                        throw new IllegalArgumentException("type URL is required for ADS");
                    }
                    typeUrl = defaultTypeURL;
                }

                logger.info("[{}] request {}{} with nonce {} from version {}",
                        streamId, typeUrl, request.getResourceNamesList(), nonce, request.getVersionInfo());

                // cancel existing watches to (re-)request a newer version
                switch (typeUrl) {
                    case ENDPOINT_TYPE:
                        if ("".equals(watches.endpointNonce) || Objects.equals(watches.endpointNonce, nonce)) {
                            if (watches.endpoints != null) {
                                watches.endpoints.cancel();
                            }
                            watches.endpoints = watcher.watch(ENDPOINT, request.getNode(), request.getVersionInfo(), request.getResourceNamesList());
                        }
                        break;
                    case CLUSTER_TYPE:
                        if ("".equals(watches.clusterNonce) || Objects.equals(watches.clusterNonce, nonce)) {
                            if (watches.clusters != null) {
                                watches.clusters.cancel();
                            }
                            watches.clusters = watcher.watch(CLUSTER, request.getNode(), request.getVersionInfo(), request.getResourceNamesList());
                        }
                        break;
                    case ROUTE_TYPE:
                        if ("".equals(watches.routeNonce) || Objects.equals(watches.routeNonce, nonce)) {
                            if (watches.routes != null) {
                                watches.routes.cancel();
                            }
                            watches.routes = watcher.watch(ROUTE, request.getNode(), request.getVersionInfo(), request.getResourceNamesList());
                        }
                        break;
                    case LISTENER_TYPE:
                        if ("".equals(watches.listenerNonce) || Objects.equals(watches.listenerNonce, nonce)) {
                            if (watches.listeners != null) {
                                watches.listeners.cancel();
                            }
                            watches.listeners = watcher.watch(LISTENER, request.getNode(), request.getVersionInfo(), request.getResourceNamesList());
                        }
                        break;
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        };
    }

    private static class Watches {

        private Watch endpoints;
        private Watch clusters;
        private Watch routes;
        private Watch listeners;

        private String endpointNonce;
        private String clusterNonce;
        private String routeNonce;
        private String listenerNonce;

        private void cancel() {
            endpoints.cancel();
            clusters.cancel();
            routes.cancel();
            listeners.cancel();
        }
    }
}
