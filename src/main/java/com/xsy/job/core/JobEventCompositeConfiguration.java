package com.xsy.job.core;

import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.JobEventListener;
import com.dangdang.ddframe.job.event.JobEventListenerConfigurationException;

import java.io.Serializable;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:17
 **/
public class JobEventCompositeConfiguration implements JobEventConfiguration, Serializable {

	private final JobEventListenerComposite listener;

	public JobEventCompositeConfiguration(JobEventListener... jobEventListeners) {
		this.listener = new JobEventListenerComposite(jobEventListeners);
	}

	@Override
	public JobEventListener createJobEventListener() throws JobEventListenerConfigurationException {
		return listener;
	}

	@Override
	public String getIdentity() {
		return listener.getIdentity();
	}
}
