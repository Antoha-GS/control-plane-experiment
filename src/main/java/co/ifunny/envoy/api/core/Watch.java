package co.ifunny.envoy.api.core;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Watch {

    final private BlockingQueue<Response> responsesQueue = new ArrayBlockingQueue<>(1);
    final private ResponseType type;
    final private List<String> names;
    private Runnable stop;

    public Watch(ResponseType type, List<String> names) {
        this.type = type;
        this.names = names;
    }

    public BlockingQueue<Response> getResponsesQueue() {
        return responsesQueue;
    }

    public ResponseType getType() {
        return type;
    }

    public List<String> getNames() {
        return names;
    }

    public void setStop(Runnable stop) {
        this.stop = stop;
    }

    final public void cancel() {
        if (stop != null) {
            stop.run();
            stop = null;
        }
    }
}
