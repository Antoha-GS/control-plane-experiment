package co.ifunny.envoy.api.api;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.KeyValueClient;
import envoy.api.v2.Discovery.DiscoveryRequest;
import envoy.api.v2.Discovery.DiscoveryResponse;
import envoy.api.v2.Rds;
import envoy.api.v2.Rds.Route;
import envoy.api.v2.Rds.RouteConfiguration;
import envoy.api.v2.RouteDiscoveryServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class RouteDiscoveryService extends RouteDiscoveryServiceGrpc.RouteDiscoveryServiceImplBase {

    final private static Logger logger = LoggerFactory.getLogger(RouteDiscoveryService.class);

    final private static AtomicLong streamNonce = new AtomicLong();

    final private CatalogClient consulCatalog;
    final private KeyValueClient consulKeyValue;

    public RouteDiscoveryService(CatalogClient consulCatalog, KeyValueClient consulKeyValue) {
        this.consulCatalog = consulCatalog;
        this.consulKeyValue = consulKeyValue;
    }

    @Override
    public StreamObserver<DiscoveryRequest> streamRoutes(StreamObserver<DiscoveryResponse> responseObserver) {
        return new StreamObserver<DiscoveryRequest>() {
            @Override
            public void onNext(DiscoveryRequest request) {
                responseObserver.onNext(buildRoutesResponse(request));
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("Encountered error in streamRoutes", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void fetchRoutes(DiscoveryRequest request, StreamObserver<DiscoveryResponse> responseObserver) {
        responseObserver.onNext(buildRoutesResponse(request));
        responseObserver.onCompleted();
        //responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Method is unimplemented").asException());
    }

    private DiscoveryResponse buildRoutesResponse(DiscoveryRequest request) {
        try {
            logger.info("Request: {}", JsonFormat.printer().print(request));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Request", e);
        }

        final String typeUrl = request.getTypeUrl();
        final String routeName = "route";
        final String virtualHostName = "subscriptions";
        final String virtualHost = "subscriptions";
        final String clusterName = "cluster";

        RouteConfiguration routeConfiguration = RouteConfiguration.newBuilder()
                .setName(routeName)
                .addVirtualHosts(Rds.VirtualHost.newBuilder()
                    .setName(virtualHostName)
                    .addDomains(virtualHost)
                    .addRoutes(Route.newBuilder()
                        .setMatch(Rds.RouteMatch.newBuilder()
                            .addHeaders(Rds.HeaderMatcher.newBuilder()
                                .setName("content-type")
                                .setValue("application/grpc")))
                        .setRoute(Rds.RouteAction.newBuilder()
                            .setCluster(clusterName))))
                .build();

        DiscoveryResponse response = DiscoveryResponse.newBuilder()
                .setTypeUrl(typeUrl)
                .setVersionInfo("0")
                .setCanary(false)
                .setNonce(String.valueOf(streamNonce.incrementAndGet()))
                .addResources(Any.pack(routeConfiguration))
                .build();

        try {
            logger.info("Response: {}", JsonFormat.printer()
                    .usingTypeRegistry(TypeRegistry.newBuilder().add(RouteConfiguration.getDescriptor()).build())
                    .print(response));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Response", e);
        }

        return response;
    }
}
