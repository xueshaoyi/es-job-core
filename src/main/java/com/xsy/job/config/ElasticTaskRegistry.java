package com.xsy.job.config;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.xsy.job.core.SpringJobSchedulerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import java.util.Map;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:13
 **/
@Slf4j
public class ElasticTaskRegistry implements SmartLifecycle {

	private Map<String, ElasticTaskContainer> elasticTaskEndpointTable = Maps.newConcurrentMap();

	private SpringJobSchedulerFactory springJobSchedulerFactory;

	public ElasticTaskRegistry(SpringJobSchedulerFactory springJobSchedulerFactory) {
		this.springJobSchedulerFactory = springJobSchedulerFactory;
	}

	public ElasticTaskRegistry() {

	}

	@Value("${elasticjob.addSchedule}")
	private boolean addSchedule;


	public void register(ElasticTaskContainer endpoint) {
		String jobName = endpoint.getElasticTask().jobName();
		if (elasticTaskEndpointTable.containsKey(jobName)) {
			log.error("job {} is exists.", jobName);
			throw new IllegalStateException("job " + jobName + " is exists.");
		}

		SpringJobScheduler springJobScheduler =
				springJobSchedulerFactory
						.createSpringjobScheduler(endpoint.getElasticJob(), endpoint.getElasticJob().getClass(), endpoint.getElasticTask());
		endpoint.setupSpringJobScheduler(springJobScheduler);
		elasticTaskEndpointTable.put(jobName, endpoint);
	}


	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		if (isRunning()) {
			return;
		}
		log.info("{} ElasticTask starting.. ", elasticTaskEndpointTable.values().size());
		if (addSchedule) {
			elasticTaskEndpointTable.values().parallelStream().forEach(elasticTaskEndpoint -> {
				log.info("ElasticTask {} start. class:{}, params:{}", elasticTaskEndpoint.getElasticTask().jobName(),
				         elasticTaskEndpoint.getElasticJob(),
				         elasticTaskEndpoint.getElasticJob().getClass(),
				         elasticTaskEndpoint.getElasticTask());
				try {
					elasticTaskEndpoint.getSpringJobScheduler().init();
				} catch (Exception e) {
					log.error("start " + elasticTaskEndpoint.getElasticTask().jobName() + " error. continue start other.", e);
				}
			});
		}

		log.info("{} ElasticTask started.. ", elasticTaskEndpointTable.values().size());
		running = true;
	}


	@Override
	public void stop() {
		if (isRunning()) {
			running = false;
		}
		log.info("{} ElasticTask will stop.. ", elasticTaskEndpointTable.values().size());
		if (addSchedule) {
			elasticTaskEndpointTable.values().parallelStream().forEach(elasticTaskEndpoint -> {
				log.info("ElasticTask {} stop. class:{}, params:{}",
				         elasticTaskEndpoint.getElasticTask().jobName(),
				         elasticTaskEndpoint.getElasticJob(),
				         elasticTaskEndpoint.getElasticJob().getClass(),
				         elasticTaskEndpoint.getElasticTask());
				try {
					elasticTaskEndpoint.getSpringJobScheduler().getSchedulerFacade().shutdownInstance();
					log.debug("ElasticTask stop. class:{} success ", elasticTaskEndpoint.getElasticTask().jobName());
				} catch (Exception e) {
					log.error("stop " + elasticTaskEndpoint.getElasticTask().jobName() + " error. continue stop other.", e);
				}
			});
		}

		log.info("{} ElasticTask will stop.. ", elasticTaskEndpointTable.values().size());
	}

	private volatile boolean running;

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return Ordered.LOWEST_PRECEDENCE - 98;
	}

	public ElasticTaskContainer getEndpoint(String jobName) {
		return elasticTaskEndpointTable.get(jobName);
	}
}
