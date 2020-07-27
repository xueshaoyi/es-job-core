package com.xsy.job.core;

import com.dangdang.ddframe.job.event.JobEventListener;
import com.dangdang.ddframe.job.event.type.JobExecutionEvent;
import com.dangdang.ddframe.job.event.type.JobStatusTraceEvent;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:18
 **/
@Slf4j
public class JobEventListenerComposite implements JobEventListener {

	private final List<JobEventListener> jobEventListener;
	private final String identity;

	public JobEventListenerComposite(JobEventListener... jobEventListeners) {
		jobEventListener = Lists.newArrayList(jobEventListeners);
		identity = Joiner.on(",").join(jobEventListeners);
	}

	@Override
	public void listen(JobExecutionEvent jobExecutionEvent) {
		for (JobEventListener eventListener : jobEventListener) {
			eventListener.listen(jobExecutionEvent);
		}
	}

	@Override
	public void listen(JobStatusTraceEvent jobStatusTraceEvent) {
		for (JobEventListener eventListener : jobEventListener) {
			try {
				eventListener.listen(jobStatusTraceEvent);
			} catch (Exception e) {
				log.error("listen error... ", e);
			}
		}
	}

	@Override
	public String getIdentity() {
		return identity;
	}
}
