package co.ifunny.envoy.api.api;

import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.KeyValueClient;
import envoy.api.v2.Discovery.DiscoveryRequest;
import envoy.api.v2.Discovery.DiscoveryResponse;
import envoy.api.v2.RouteDiscoveryServiceGrpc;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static envoy.api.v2.RouteDiscoveryServiceGrpc.METHOD_FETCH_ROUTES;
import static envoy.api.v2.RouteDiscoveryServiceGrpc.METHOD_STREAM_ROUTES;

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
                logRequest(request, METHOD_STREAM_ROUTES);

                responseObserver.onError(Status.UNIMPLEMENTED
                        .withDescription(String.format("Method %s is unimplemented", METHOD_STREAM_ROUTES.getFullMethodName()))
                        .asException());
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
        logRequest(request, METHOD_FETCH_ROUTES);
        super.fetchRoutes(request, responseObserver);
    }

    private void logRequest(DiscoveryRequest request, MethodDescriptor<DiscoveryRequest, DiscoveryResponse> methodDescriptor) {
        logger.info("[{}] {}", methodDescriptor.getFullMethodName(), request);
    }
}
