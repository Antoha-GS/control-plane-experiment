package co.ifunny.envoy.api.api;

import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.KeyValueClient;
import envoy.api.v2.Discovery.DiscoveryRequest;
import envoy.api.v2.Discovery.DiscoveryResponse;
import envoy.api.v2.Eds.LoadStatsRequest;
import envoy.api.v2.Eds.LoadStatsResponse;
import envoy.api.v2.EndpointDiscoveryServiceGrpc;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static envoy.api.v2.EndpointDiscoveryServiceGrpc.METHOD_FETCH_ENDPOINTS;
import static envoy.api.v2.EndpointDiscoveryServiceGrpc.METHOD_STREAM_ENDPOINTS;
import static envoy.api.v2.EndpointDiscoveryServiceGrpc.METHOD_STREAM_LOAD_STATS;

public class EndpointDiscoveryService extends EndpointDiscoveryServiceGrpc.EndpointDiscoveryServiceImplBase {

    final private static Logger logger = LoggerFactory.getLogger(EndpointDiscoveryService.class);

    final private static AtomicLong streamNonce = new AtomicLong();

    final private CatalogClient consulCatalog;
    final private KeyValueClient consulKeyValue;

    public EndpointDiscoveryService(CatalogClient consulCatalog, KeyValueClient consulKeyValue) {
        this.consulCatalog = consulCatalog;
        this.consulKeyValue = consulKeyValue;
    }

    @Override
    public StreamObserver<DiscoveryRequest> streamEndpoints(StreamObserver<DiscoveryResponse> responseObserver) {
        return new StreamObserver<DiscoveryRequest>() {
            @Override
            public void onNext(DiscoveryRequest request) {
                logRequest(request, METHOD_STREAM_ENDPOINTS);
                responseObserver.onError(Status.UNIMPLEMENTED
                        .withDescription(String.format("Method %s is unimplemented", METHOD_STREAM_ENDPOINTS.getFullMethodName()))
                        .asException());
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("Encountered error in streamEndpoints", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void fetchEndpoints(DiscoveryRequest request, StreamObserver<DiscoveryResponse> responseObserver) {
        logRequest(request, METHOD_FETCH_ENDPOINTS);
        super.fetchEndpoints(request, responseObserver);
    }

    @Override
    public StreamObserver<LoadStatsRequest> streamLoadStats(StreamObserver<LoadStatsResponse> responseObserver) {
        logger.info("[{}] WTF method?", METHOD_STREAM_LOAD_STATS);
        return super.streamLoadStats(responseObserver);
    }

    private void logRequest(DiscoveryRequest request, MethodDescriptor<DiscoveryRequest, DiscoveryResponse> methodDescriptor) {
        logger.info("[{}] {}", methodDescriptor.getFullMethodName(), request);
    }
}
