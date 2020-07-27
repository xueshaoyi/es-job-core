package com.xsy.job.annotation;

import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.JobEventListenerConfigurationException;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.xsy.job.config.ElasticTaskRegistry;
import com.xsy.job.core.JobEventCompositeConfiguration;
import com.xsy.job.core.MetricsJobEventListener;
import com.xsy.job.core.SpringJobSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:03
 **/
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "elasticjob.enable", matchIfMissing = false, havingValue = "true")
public class ElasticJobAutoConfiguration {

	@Bean(initMethod = "init")
	@ConditionalOnExpression("'${elasticjob.regCenter.serverList}'.length() > 0")
	public ZookeeperRegistryCenter regCenter(@Value("${elasticjob.regCenter.serverList}") final String serverList,
	                                         @Value("${elasticjob.regCenter.namespace}") final String namespace) {
		return new ZookeeperRegistryCenter(new ZookeeperConfiguration(serverList, namespace));
	}

	@Bean
	public SpringJobSchedulerFactory springJobSchedulerFactory(ZookeeperRegistryCenter zookeeperRegistryCenter,
	                                                           JobEventConfiguration jobEventConfiguration) {
		return new SpringJobSchedulerFactory(jobEventConfiguration, zookeeperRegistryCenter);
	}

	@Bean
	public JobEventConfiguration jobEventConfiguration(MetricsJobEventListener metricsJobEventListener)
			throws JobEventListenerConfigurationException {
		return new JobEventCompositeConfiguration(metricsJobEventListener);
	}

	@Bean
	public MetricsJobEventListener metricsJobEventListener() {
		return new MetricsJobEventListener();
	}


	@Bean
	public ElasticTaskRegistry elasticTaskRegistry(SpringJobSchedulerFactory springJobSchedulerFactory) {
		return new ElasticTaskRegistry(springJobSchedulerFactory);
	}

	@Bean
	public ElasticTaskBeanPostProcessor elasticTaskBeanPostProcessor() {
		return new ElasticTaskBeanPostProcessor();
	}

}
