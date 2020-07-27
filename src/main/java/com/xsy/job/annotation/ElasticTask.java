package com.xsy.job.annotation;


import java.lang.annotation.*;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:04
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ElasticTasks.class)
public @interface ElasticTask {

	/**
	 * jobName
	 *
	 * @return
	 */
	String jobName();

	/**
	 * cron表达式
	 *
	 * @return
	 */
	String cron();

	int shardingTotalCount() default 1;

	String shardingItemParameters() default "";

	/**
	 * job参数 通过后台动态指定
	 *
	 * @return
	 */
	String jobParameter() default "";

	/**
	 * propsFileName
	 *
	 * @return
	 */
	String propsFilename() default "";

	boolean failover() default false;

	boolean misfire() default true;

	String description() default "";

	String jobExceptionHandler() default
			"com.dangdang.ddframe.job.executor.handler.impl.DefaultJobExceptionHandler";

	String executorServiceHandler() default
			"com.dangdang.ddframe.job.executor.handler.impl.DefaultExecutorServiceHandler";

	boolean monitorExecution() default true;

	int maxTimeDiffSeconds() default -1;

	int monitorPort() default -1;

	String jobShardingStrategyClass() default "com.dangdang.ddframe.job.lite.api.strategy.impl.RotateServerByNameJobShardingStrategy";

	int reconcileIntervalMinutes() default 10;

	boolean disabled() default false;

	boolean overwrite() default false;

	boolean streamingProcess() default false;

	String[] successEmails() default {};

	String[] errorEmails() default {};

	/**
	 * 在测试环境不自动启动. 由于elastickjob 本身机制的问题，暂时只能通过设置cron的方式实现不自定启动，这个参数加上以后，cron会被设置成为下一年的今天。
	 *
	 * @return
	 */
	boolean autoTriggerForTest() default false;

	/**
	 * 同 @link #autoTriggerForTest. 这个可以在生产环境中触发
	 *
	 * @return
	 */
	boolean manualTrigger() default false;

}
