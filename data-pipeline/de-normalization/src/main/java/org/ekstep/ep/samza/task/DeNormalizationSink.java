package org.ekstep.ep.samza.task;

import org.apache.samza.task.MessageCollector;
import org.ekstep.ep.samza.core.BaseSink;
import org.ekstep.ep.samza.core.JobMetrics;
import org.ekstep.ep.samza.domain.Event;

public class DeNormalizationSink extends BaseSink {

	private JobMetrics metrics;
	private DeNormalizationConfig config;

	public DeNormalizationSink(MessageCollector collector, JobMetrics metrics, DeNormalizationConfig config) {
		super(collector);
		this.metrics = metrics;
		this.config = config;
	}

	public void toSuccessTopic(Event event) {
		toTopic(config.successTopic(), event.did(), event.getJson());
		metrics.incSuccessCounter();
		;
	}

	public void toMalformedTopic(String message) {
		toTopic(config.malformedTopic(), null, message);
		metrics.incErrorCounter();
	}

	public void toErrorTopic(Event event, String errorMessage) {
		event.markFailure(errorMessage, config);
		toTopic(config.failedTopic(), event.did(), event.getJson());
		metrics.incErrorCounter();
	}

}