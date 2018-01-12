package co.ifunny.envoy.api.metrics;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.timgroup.statsd.StatsDClient;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StatsDReporter {

    final private static Logger logger = LoggerFactory.getLogger(StatsDReporter.class);
    final private static long PUSH_INTERVAL_SEC = 60;

    final private ScheduledExecutorService executor;
    final private StatsDClient statsDClient;

    public StatsDReporter(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
        this.executor = Executors.newScheduledThreadPool(
                1, new ThreadFactoryBuilder().setNameFormat("StatsDReporter-%d").build());
    }

    private void push(CollectorRegistry registry) {
        for (Collector.MetricFamilySamples metricFamilySamples: Collections.list(registry.metricFamilySamples())) {
            for (Collector.MetricFamilySamples.Sample sample: metricFamilySamples.samples) {
                String aspect = sample.name;
                double value = sample.value;
                String[] tags = new String[sample.labelNames.size()];
                IntStream.range(0, sample.labelNames.size())
                        .mapToObj(i -> sample.labelNames.get(i) + ':' + sample.labelValues.get(i))
                        .collect(Collectors.toList())
                        .toArray(tags);
                statsDClient.gauge(aspect, value, tags);
            }
        }
    }

    public void start(CollectorRegistry registry) {
        executor.scheduleAtFixedRate(() -> push(registry), PUSH_INTERVAL_SEC, PUSH_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    public void stop() {
        logger.info("Shutting down StatsDReporter");
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("StatsDReporter shutdown was interrupted", e);
        }
        if (!executor.isTerminated()) {
            executor.shutdownNow();
        }
        logger.info("StatsDReporter shut down");
    }
}
