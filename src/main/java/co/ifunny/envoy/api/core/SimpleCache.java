package co.ifunny.envoy.api.core;

import com.google.protobuf.Message;
import envoy.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

final public class SimpleCache implements Cache {

    final private static Logger logger = LoggerFactory.getLogger(SimpleCache.class);

    final private Map<Key, Snapshot> snapshots;
    final private Map<Key, Map<Long, Watch>> watches;
    final private KeyFactory<Base.Node> keyFactory;
    final private Consumer<Key> callback;
    private long watchCount = 0;

    public SimpleCache(Map<Key, Snapshot> snapshots, Map<Key, Map<Long, Watch>> watches, KeyFactory keyFactory, Consumer<Key> callback) {
        this.snapshots = snapshots;
        this.watches = watches;
        this.keyFactory = keyFactory;
        this.callback = callback;
    }

    @Override
    public void setSnapshot(Key key, Snapshot snapshot) {
        // update the existing entry
        snapshots.put(key, snapshot);

        Map<Long, Watch> watchMap = watches.get(key);

        if (watchMap == null) {
            return;
        }

        // trigger existing watches
        for (Watch watch : watchMap.values()) {
            respond(watch, snapshot, key);
        }

        // discard all watches; the client must request a new watch to receive updates and ACK/NACK
        watches.remove(key);
    }

    @Override
    public Watch watch(ResponseType responseType, Base.Node node, String version, List<String> names) {
        Watch watch = new Watch(responseType, names);
        Key key = keyFactory.create(node);

        // if the requested version is up-to-date or missing a response, leave an open watch
        Snapshot snapshot = snapshots.get(key);
        if (snapshot == null || Objects.equals(version, snapshot.getVersion())) {
            // invoke callback in a separate thread
            if (snapshot == null && callback != null) {
                logger.info("callback {} at {}", key, version);
                new Thread(() -> callback.accept(key)).start();
            }

            if (!watches.containsKey(key)) {
                watches.put(key, new HashMap<>());
            }

            logger.info("open watch for {}{} from key {} from version {}", responseType, names, key, version);
            watchCount++;
            watches.computeIfAbsent(key, it -> new HashMap<>()).put(watchCount, watch);
            watch.setStop(() -> watches.remove(key));

            return watch;
        }

        // otherwise, the watch may be responded immediately
        respond(watch, snapshot, key);

        return watch;
    }

    private void respond(Watch watch, Snapshot snapshot, Key key) {
        final ResponseType type = watch.getType();
        final List<Message> resources = snapshot.getResources(type);
        final String version = snapshot.getVersion();

        // remove clean-up as the watch is discarded immediately
        watch.setStop(null);

        // the request names must match the snapshot names
        // if they do not, then the watch is never responded, and it is expected that envoy makes another request
        if (watch.getNames().size() > 0) {
            Map<String, Boolean> names = new HashMap<>();
            for (String name : watch.getNames()) {
                names.put(name, true);
            }

            // check that every snapshot resource name is present in the request
            for (Message resource : resources) {
                String resourceName = getResourceName(resource);
                if (!names.containsKey(resourceName)) {
                    logger.info("not responding for {} from {} at {} since {} not requested {}",
                            type, key, version, resourceName, watch.getNames());
                    return;
                }
            }
        }

        try {
            watch.getResponsesQueue().put(new Response(version, resources));
        } catch (InterruptedException e) {
            logger.warn("Putting response interrupted", e);
        }
    }

    private String getResourceName(Message resource) {
        if (resource instanceof Eds.ClusterLoadAssignment) {
            return ((Eds.ClusterLoadAssignment) resource).getClusterName();
        }

        if (resource instanceof Cds.Cluster) {
            return ((Cds.Cluster) resource).getName();
        }

        if (resource instanceof Rds.RouteConfiguration) {
            return ((Rds.RouteConfiguration) resource).getName();
        }

        if (resource instanceof Lds.Listener) {
            return ((Lds.Listener) resource).getName();
        }

        return "";
    }
}
