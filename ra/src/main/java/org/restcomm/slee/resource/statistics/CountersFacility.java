package org.restcomm.slee.resource.statistics;

import java.util.concurrent.ConcurrentHashMap;

import org.restcomm.commons.statistics.reporter.RestcommStatsReporter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

public class CountersFacility {

	private MetricRegistry metrics = RestcommStatsReporter.getMetricRegistry();
	private ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<String, Counter>();

	public void updateCounter(String name, Long count) {
		Counter counter = counters.get(name);
		if (counter == null) {
			counter = initCounter(name);
		}
		counter.inc(count);
	}

	private Counter initCounter(String name) {
		Counter counter = metrics.counter(name);
		Counter oldCounter = counters.putIfAbsent(name, counter);
		if (oldCounter != null) {
			counter = oldCounter;
		}
		return counter;
	}

	public Long getCount(String name) {
		Long count = null;
		Counter counter = counters.get(name);
		if (counter != null)
			count = counter.getCount();
		return count;
	}
}
