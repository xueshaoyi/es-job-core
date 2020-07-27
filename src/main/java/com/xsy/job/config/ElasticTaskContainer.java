package com.xsy.job.config;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.xsy.job.annotation.ElasticTask;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:12
 **/
public interface ElasticTaskContainer {

	ElasticTask getElasticTask();

	ElasticJob getElasticJob();

	void setupSpringJobScheduler(SpringJobScheduler scheduler);

	SpringJobScheduler getSpringJobScheduler();

}
