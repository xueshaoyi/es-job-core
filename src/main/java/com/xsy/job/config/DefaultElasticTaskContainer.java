package com.xsy.job.config;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.xsy.job.annotation.ElasticTask;
import lombok.Setter;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:12
 **/
public class DefaultElasticTaskContainer implements ElasticTaskContainer {

	@Setter
	private ElasticTask elasticTask;
	@Setter
	private ElasticJob elasticJob;
	private SpringJobScheduler scheduler;

	public DefaultElasticTaskContainer(ElasticTask elasticTask, ElasticJob elasticJob) {
		this.elasticTask = elasticTask;
		this.elasticJob = elasticJob;
	}

	public DefaultElasticTaskContainer() {
	}

	@Override
	public ElasticTask getElasticTask() {
		return elasticTask;
	}

	@Override
	public ElasticJob getElasticJob() {
		return elasticJob;
	}

	@Override
	public void setupSpringJobScheduler(SpringJobScheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public SpringJobScheduler getSpringJobScheduler() {
		return scheduler;
	}
}
