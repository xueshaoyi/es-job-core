package com.xsy.job.listener;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.config.LiteJobConfigurationGsonFactory;
import com.dangdang.ddframe.job.lite.internal.listener.AbstractJobListener;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.dangdang.ddframe.job.util.env.IpUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.xsy.job.common.ZkNodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

/**
 * @author xueshaoyi
 * @Date 2019/12/26 下午5:26
 **/
@Slf4j
public class JobInitListenerManager {

	private String jobClassName;

	public JobInitListenerManager(String jobClassName) {
		this.jobClassName = jobClassName;
	}

	@Resource
	private ZookeeperRegistryCenter regCenter;
	@Resource
	private JobEventConfiguration jobEventConfiguration;
	@Resource
	private ApplicationContext applicationContext;
    @Value("${elasticjob.regCenter.serverList}")
	private String serverLists;

	public void start() {
        log.info("########EsJobListener########");

        ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(serverLists,ZkNodeConstants.NAMESPACE);
        log.info("serverLists {}", serverLists);
        ZookeeperRegistryCenter devRgeCenter = new ZookeeperRegistryCenter(zkConfig);
        devRgeCenter.init();
		String ip = IpUtils.getIp();
		String heard = ZkNodeConstants.INIT_JOB_NODE + "/" + jobClassName;

		//进行监控

		String listenerNode = heard + ZkNodeConstants.HOSTS_NODE + "/" + ip;
        devRgeCenter.addCacheData(listenerNode);
        TreeCache cache = (TreeCache) devRgeCenter.getRawCache(listenerNode);
        cache.getListenable().addListener(new JobListener());


		//创建ip的临时节点 用于监控查活
		String ephemeralHost = heard + ZkNodeConstants.INSTANCES_NODE + "/" + ip;
		devRgeCenter.persistEphemeral(ephemeralHost, ip);
		log.info("add ephemera host {}", ephemeralHost);
        log.info("########   started   ########");
	}

	class JobListener extends AbstractJobListener {

		@Override
		protected void dataChanged(String path, TreeCacheEvent.Type eventType, String data) {
			if (StringUtils.isEmpty(data) || !path.contains(ZkNodeConstants.HOSTS_NODE)) {
				return;
			}
			log.info("listener data change path is {}, eventType is {}, data is {}", path, eventType, data);
			try {
				LiteJobConfiguration liteJobConfiguration = LiteJobConfigurationGsonFactory.fromJson(data);
				if (eventType == TreeCacheEvent.Type.NODE_ADDED) {
					addInitNewJob(liteJobConfiguration);
				} else if (eventType == TreeCacheEvent.Type.NODE_REMOVED) {
					removeJob(liteJobConfiguration);
				} else if (eventType == TreeCacheEvent.Type.NODE_UPDATED) {
					nodeUpdate(liteJobConfiguration);
				}
			} catch (Exception e) {
				log.info("job listener has error {}", e);
			}

		}

        /**
         * 更新Job
         * @param liteJobConfiguration
         */
		public void nodeUpdate(LiteJobConfiguration liteJobConfiguration) {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(liteJobConfiguration.getJobName()),
			                            "jobName can not be empty.");
			Preconditions.checkArgument(
					!Strings.isNullOrEmpty(liteJobConfiguration.getTypeConfig().getCoreConfig().getCron()),
					"cron can not be empty.");
			Preconditions
					.checkArgument(liteJobConfiguration.getTypeConfig().getCoreConfig().getShardingTotalCount() > 0,
					               "shardingTotalCount should larger than zero.");
			JobNodePath jobNodePath = new JobNodePath(liteJobConfiguration.getJobName());
			if (regCenter.isExisted("/" + liteJobConfiguration.getJobName())) {
				regCenter.update(jobNodePath.getConfigNodePath(),
				                 LiteJobConfigurationGsonFactory.toJsonForObject(liteJobConfiguration));
			} else {
				addInitNewJob(liteJobConfiguration);
			}
		}

		/**
		 * 删除Job
		 *
		 * @param liteJobConfiguration
		 */
		private void removeJob(LiteJobConfiguration liteJobConfiguration) {
			String jobNode = "/" + liteJobConfiguration.getJobName();
			JobRegistry instance = JobRegistry.getInstance();
			try {
				instance.shutdown(liteJobConfiguration.getJobName());
			} catch (Exception e) {
				log.info("job remove has error {}", e);
			}
			regCenter.remove(jobNode);
		}

		/**
		 * 增加Job
		 *
		 * @param liteJobConfiguration
		 */
		private void addInitNewJob(LiteJobConfiguration liteJobConfiguration) {

			String jobClass = liteJobConfiguration.getTypeConfig().getJobClass();
			String[] s = jobClass.split("\\.");
			String job = s[s.length - 1];

			log.info("init simpleJob from zk Node {}", liteJobConfiguration.getJobName());
			SimpleJob simpleJob = applicationContext.getBean(job, SimpleJob.class);

			new SpringJobScheduler(simpleJob, regCenter, liteJobConfiguration, jobEventConfiguration).init();

			log.info("Job init Ok , job running!!");
		}

	}

}