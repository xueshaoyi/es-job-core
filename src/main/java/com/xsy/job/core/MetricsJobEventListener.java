package com.xsy.job.core;

import com.dangdang.ddframe.job.event.JobEventListener;
import com.dangdang.ddframe.job.event.type.JobExecutionEvent;
import com.dangdang.ddframe.job.event.type.JobStatusTraceEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:19
 **/
@Slf4j
public class MetricsJobEventListener implements JobEventListener, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private MeterRegistry meterRegistry;

	private static final String MEASUREMENT_JOB_STATUS_TRACE_LOG = "elastic_job_status_trace_log";

	private static final String TAGS_JOB_NAME = "name";
	private static final String TAGS_JOB_SHARDING_ITEM = "sharding_item";
	private static final String TAGS_JOB_EXECUTION_TYPE = "execution_type";
	private static final String TAGS_JOB_STATE = "state";


	/**
	 * influx 没有更新操作 做成多条和下面这张MEASUREMENT实际上是一样的 所以不处理
	 *
	 * @param jobExecutionEvent
	 */
	@Override
	public void listen(JobExecutionEvent jobExecutionEvent) {
	}

	@Override
	public void listen(JobStatusTraceEvent jobStatusTraceEvent) {
		insertJobStatusTraceEvent(jobStatusTraceEvent);
	}

	private void insertJobStatusTraceEvent(JobStatusTraceEvent jobStatusTraceEvent) {
		if (meterRegistry == null) {
			meterRegistry = applicationContext.getBean(MeterRegistry.class);
		}
		try {
			meterRegistry.counter(MEASUREMENT_JOB_STATUS_TRACE_LOG,
			                      TAGS_JOB_NAME, jobStatusTraceEvent.getJobName(),
			                      TAGS_JOB_SHARDING_ITEM, jobStatusTraceEvent.getShardingItems(),
			                      TAGS_JOB_EXECUTION_TYPE, jobStatusTraceEvent.getExecutionType().name(),
			                      TAGS_JOB_STATE, jobStatusTraceEvent.getState().name()
			).increment();
		} catch (Exception e) {
			log.error("insertJobStatusTraceEvent error", e);
		}
	}

	@Override
	public String getIdentity() {
		return "metrics";
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
