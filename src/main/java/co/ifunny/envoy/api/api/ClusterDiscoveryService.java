package co.ifunny.envoy.api.api;

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.KeyValueClient;
import envoy.api.v2.Cds.Cluster;
import envoy.api.v2.Cds.Cluster.DiscoveryType;
import envoy.api.v2.Cds.Cluster.EdsClusterConfig;
import envoy.api.v2.Cds.Cluster.LbPolicy;
import envoy.api.v2.ClusterDiscoveryServiceGrpc;
import envoy.api.v2.ConfigSourceOuterClass.ApiConfigSource;
import envoy.api.v2.ConfigSourceOuterClass.ApiConfigSource.ApiType;
import envoy.api.v2.ConfigSourceOuterClass.ConfigSource;
import envoy.api.v2.Discovery.DiscoveryRequest;
import envoy.api.v2.Discovery.DiscoveryResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class ClusterDiscoveryService extends ClusterDiscoveryServiceGrpc.ClusterDiscoveryServiceImplBase {

    final private static Logger logger = LoggerFactory.getLogger(ClusterDiscoveryService.class);

    final private static String XDS_CLUSTER_NAME = "xds_cluster";
    final private static AtomicLong streamNonce = new AtomicLong();

    final private CatalogClient consulCatalog;
    final private KeyValueClient consulKeyValue;

    public ClusterDiscoveryService(CatalogClient consulCatalog, KeyValueClient consulKeyValue) {
        this.consulCatalog = consulCatalog;
        this.consulKeyValue = consulKeyValue;
    }

    @Override
    public StreamObserver<DiscoveryRequest> streamClusters(StreamObserver<DiscoveryResponse> responseObserver) {
        return new StreamObserver<DiscoveryRequest>() {
            @Override
            public void onNext(DiscoveryRequest request) {
                responseObserver.onNext(buildClustersResponse(request));
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("Encountered error in streamClusters", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void fetchClusters(DiscoveryRequest request, StreamObserver<DiscoveryResponse> responseObserver) {
        responseObserver.onNext(buildClustersResponse(request));
        responseObserver.onCompleted();
        //responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Method is unimplemented").asException());
    }

    private DiscoveryResponse buildClustersResponse(DiscoveryRequest request) {
        try {
            logger.info("Request: {}", JsonFormat.printer().print(request));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Request", e);
        }

        final String typeUrl = request.getTypeUrl();
        final String clusterName = "cluster";
        final Duration connectTimeout = Duration.newBuilder().setSeconds(5).build();

        ConfigSource edsConfig = ConfigSource.newBuilder()
                .setApiConfigSource(ApiConfigSource.newBuilder()
                    .setApiType(ApiType.GRPC)
                    .addClusterName(XDS_CLUSTER_NAME))
                .build();

        Cluster cluster = Cluster.newBuilder()
                .setName(clusterName)
                .setConnectTimeout(connectTimeout)
                .setType(DiscoveryType.EDS)
                .setLbPolicy(LbPolicy.ROUND_ROBIN)
                .setEdsClusterConfig(EdsClusterConfig.newBuilder()
                    .setEdsConfig(edsConfig)
                    .setServiceName(clusterName))
                .build();
        DiscoveryResponse response = DiscoveryResponse.newBuilder()
                .setTypeUrl(typeUrl)
                .setVersionInfo("0")
                .setCanary(false)
                .setNonce(String.valueOf(streamNonce.incrementAndGet()))
                .addResources(Any.pack(cluster))
                .build();

        try {
            logger.info("Response: {}", JsonFormat.printer()
                    .usingTypeRegistry(TypeRegistry.newBuilder().add(Cluster.getDescriptor()).build())
                    .print(response));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Response", e);
        }

        return response;
    }
}
