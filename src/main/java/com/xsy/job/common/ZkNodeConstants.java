package com.xsy.job.common;

/**
 * @author xueshaoyi
 * @Date 2019/12/27 上午10:43
 **/
public class ZkNodeConstants {

	public static final String NAMESPACE = "xsy_job_space";
	public static final String INIT_JOB_Name = "init_job_node";
	public static final String INIT_JOB_NODE = "/init_job_node";
	public static final String JOBS_NODE = "/jobs_data";
	public static final String INSTANCES_NODE = "/instances";
	public static final String HOSTS_NODE = "/host";
	public static final String LEADER_NODE = "/leader";
	public static final String LEADER_LATCH_NODE = "/latch";

	public static final String JOB_CLASS = "/${jobClass}";
	public static final String JOB = "${jobClass}";
	public static final String IP = "${ipNode}";
	public static final String JOBS = INIT_JOB_NODE + JOB_CLASS + JOBS_NODE;
	public static final String INSTANCES = INIT_JOB_NODE + JOB_CLASS + INSTANCES_NODE;
	public static final String HOSTJOB = INIT_JOB_NODE + JOB_CLASS + HOSTS_NODE + "/${ipNode}";
	public static final String LEADER = INIT_JOB_NODE  + LEADER_NODE + INSTANCES_NODE;
	public static final String LEADER_LATCH = INIT_JOB_NODE  + LEADER_NODE + LEADER_LATCH_NODE;

	public static final String STATEGY_JOB_CLASS = "com.dangdang.ddframe.job.lite.api.strategy.impl.RotateServerByNameJobShardingStrategy";

	public static final String INIT_CRON = "0 0 0/1 * * ? *";
}
