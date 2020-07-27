package com.xsy.job.core;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.xsy.job.annotation.ElasticTask;
import com.xsy.job.listener.HasElasticJobListener;
import com.xsy.job.supprt.CronUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:14
 **/
public class SpringJobSchedulerFactory implements EnvironmentAware {

	public static final String ENV_PRODUCTION = "production";

	public SpringJobSchedulerFactory(JobEventConfiguration jobEventConfiguration,
	                                 CoordinatorRegistryCenter zookeeperRegistryCenter) {
		this.jobEventConfiguration = jobEventConfiguration;
		this.zookeeperRegistryCenter = zookeeperRegistryCenter;
	}

	@Getter
	@Setter
	private Environment env;

	@Setter
	@Getter
	private JobEventConfiguration jobEventConfiguration;

	@Setter
	@Getter
	private CoordinatorRegistryCenter zookeeperRegistryCenter;


	public SpringJobScheduler createSpringjobScheduler(Object bean, Class<?> targetClass, ElasticTask annotation) {
		String jobName = annotation.jobName();

		int shardingTotalCount = annotation.shardingTotalCount();
		String shardingItemParameters = annotation.shardingItemParameters();
		String jobParameter = annotation.jobParameter();
		boolean failover = annotation.failover();
		boolean misfire = annotation.misfire();
		String description = annotation.description();

		boolean overwrite = annotation.overwrite();
		String cron = annotation.cron();

		if (annotation.manualTrigger()) {
			overwrite = true;
			cron = CronUtils.nextYearCron();
		} else if (!annotation.autoTriggerForTest() && !env.acceptsProfiles(ENV_PRODUCTION)) {
			overwrite = true;
			cron = CronUtils.nextYearCron();
		}

		JobCoreConfiguration coreConfiguration = JobCoreConfiguration
				.newBuilder(jobName, cron, shardingTotalCount)
				.shardingItemParameters(shardingItemParameters)
				.jobParameter(jobParameter)
				.failover(failover)
				.misfire(misfire)
				.description(description)
				.jobProperties(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(),
				               annotation.executorServiceHandler())
				.jobProperties(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(),
				               annotation.jobExceptionHandler())
				.build();


		JobTypeConfiguration typeConfig = bean instanceof SimpleJob ?
		                                  new SimpleJobConfiguration(coreConfiguration, targetClass.getName()) :
		                                  new DataflowJobConfiguration(coreConfiguration, targetClass.getName(),
		                                                               annotation.streamingProcess());

		boolean monitorExecution = annotation.monitorExecution();
		int maxTimeDiffSeconds = annotation.maxTimeDiffSeconds();
		int monitorPort = annotation.monitorPort();
		String jobShardingStrategyClass = annotation.jobShardingStrategyClass();
		int reconcileIntervalMinutes = annotation.reconcileIntervalMinutes();
		boolean disabled = annotation.disabled();


		LiteJobConfiguration liteJobConfiguration = LiteJobConfiguration
				.newBuilder(typeConfig)
				.monitorExecution(monitorExecution)
				.maxTimeDiffSeconds(maxTimeDiffSeconds)
				.monitorPort(monitorPort)
				.jobShardingStrategyClass(jobShardingStrategyClass)
				.reconcileIntervalMinutes(reconcileIntervalMinutes)
				.disabled(disabled)
				.overwrite(overwrite)
				.build();

		ElasticJobListener[] listeners = bean instanceof HasElasticJobListener ?
		                                 ((HasElasticJobListener) bean).getListeners() :
		                                 new ElasticJobListener[0];
		return new SpringJobScheduler((com.dangdang.ddframe.job.api.ElasticJob) bean, zookeeperRegistryCenter, liteJobConfiguration,
		                              jobEventConfiguration, listeners);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.env = environment;
	}
}

