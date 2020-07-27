package com.xsy.job.listener;

import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;
import com.dangdang.ddframe.job.lite.internal.listener.AbstractJobListener;
import com.dangdang.ddframe.job.lite.internal.listener.AbstractListenerManager;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.xsy.job.common.ZkNodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xueshaoyi
 * @Date 2019/12/25 下午5:28
 **/
@Slf4j
public class JobLeaderListenerManager extends AbstractListenerManager {

	private String nodeName;
	private String jobInstanceId;

	public JobLeaderListenerManager(CoordinatorRegistryCenter regCenter, String nodeName) {
		super(regCenter, nodeName);
		this.nodeName = nodeName;
	}


	@Resource
	private ZookeeperRegistryCenter regCenter;
	private final static int NODE_SIZE = 5;

	@Override
	public void start() {
		log.info("########EsJobListener########");
		String listenerNode = "/" + nodeName;
		regCenter.addCacheData(listenerNode);
		addDataListener(new IpNodeListener());
		JobInstance jobInstance = new JobInstance();
		jobInstanceId = jobInstance.getJobInstanceId();
		if (isLeaderUntilBlock()) {
			log.info("this is leader");
		}
		log.info("########   started   ########");
	}

	class IpNodeListener extends AbstractJobListener {

		@Override
		protected void dataChanged(String path, TreeCacheEvent.Type eventType, String data) {
			log.info("listener data change path is {}, eventType is {}, data is {}", path, eventType, data);
			if (StringUtils.isEmpty(data)) {
				return;
			}
			if (!isLeaderUntilBlock()) {
				return;
			}
			log.info("this is leader run");
			if (path.contains(ZkNodeConstants.INSTANCES_NODE)) {
				instancesChange(path, eventType);
			} else if (path.contains(ZkNodeConstants.JOBS_NODE)) {
				jobsDataNodeChange(path, eventType, data);
			}

		}


		/**
		 * 任务基本数据发生变更
		 *
		 * @param path      zk节点地址
		 * @param eventType 操作类型
		 * @param data      操作数据
		 */
		private void jobsDataNodeChange(String path, TreeCacheEvent.Type eventType, String data) {
			String[] paths = path.split("/");
			if (paths.length != NODE_SIZE) {
				return;
			}
			String jobClass = paths[2];
			String jobName = paths[4];
			try {
				if (eventType == TreeCacheEvent.Type.NODE_ADDED) {
					jobsDataAdded(jobClass, jobName, data);
				} else if (eventType == TreeCacheEvent.Type.NODE_REMOVED) {
					jobsDataRemovedOrUpdated(jobClass, jobName, data, 1);
				} else if (eventType == TreeCacheEvent.Type.NODE_UPDATED) {
					jobsDataRemovedOrUpdated(jobClass, jobName, data, 2);
				}
			} catch (Exception e) {
				log.info("job listener jobsDataNodeChange has error {}", e);
			}
		}


		/**
		 * 删除或更新任务数据
		 * 删除数据 从注册的服务实例里把该任务数据删除处理
		 * 更新数据 从注册的服务实例里把该任务数据更新处理
		 *
		 * @param jobClass job类名
		 * @param jobName  zk注册任务名称
		 * @param data     操作数据
		 * @param type     1 删除 2 更新
		 */
		private void jobsDataRemovedOrUpdated(String jobClass, String jobName, String data, int type) {
			String instancs = ZkNodeConstants.INSTANCES.replace(ZkNodeConstants.JOB, jobClass);
			List<String> instancesList = regCenter.getChildrenKeys(instancs);
			boolean status = false;
			for (String instance : instancesList) {
				String instanceJob = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
				                                            .replace(ZkNodeConstants.IP, instance);
				List<String> jobNames = regCenter.getChildrenKeys(instanceJob);
				for (String job : jobNames) {
					if (job.equals(jobName)) {
						String jobNameNode = instanceJob + "/" + jobName;
						if (type == 1) {
							regCenter.remove(jobNameNode);
							checkJobBalance(instance, jobClass, instancesList);
						} else {
							regCenter.update(jobNameNode, data);
						}
						status = true;
						break;
					}
				}
				if (status) {
					break;
				}
			}
			if (!status && type != 1) {
				jobsDataAdded(jobClass, jobName, data);
			}
		}

		/**
		 * 平衡各节点任务数
		 * 计算每个节点平均任务数 如果该节点任务数小于平均数 则从任务最多的节点里拿出一个任务放到该节点
		 * @param addInstance
		 * @param jobClass
		 * @param instancesList
		 */
		private void checkJobBalance(String addInstance, String jobClass, List<String> instancesList) {
			//计算平均任务数
			String jobsNode = ZkNodeConstants.JOBS.replace(ZkNodeConstants.JOB, jobClass);
			int jobsCount = regCenter.getNumChildren(jobsNode);
			int minAvgCount = jobsCount / instancesList.size();
			String addInstanceJobNode = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
			                                                   .replace(ZkNodeConstants.IP, addInstance);
			int count = regCenter.getNumChildren(addInstanceJobNode);
			if (count >= minAvgCount) {
				return;
			}
			//查找任务数最多的节点
			int maxCount = Integer.MIN_VALUE;
			String maxInstance = instancesList.get(0);
			for (String instance : instancesList) {
				if (addInstance.equals(instance)) {
					continue;
				}
				String instanceJob = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
				                                            .replace(ZkNodeConstants.IP, instance);
				int jobCount = regCenter.getNumChildren(instanceJob);
				if (maxCount < jobCount) {
					maxCount = jobCount;
					maxInstance = instance;
				}
			}

			if (maxCount <= 1) {
				return;
			}
			//将最大节点任务中随机一个任务放到该节点中
			String instanceJob = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
			                                            .replace(ZkNodeConstants.IP, maxInstance);
			List<String> jobList = regCenter.getChildrenKeys(instanceJob);
			String jobName = jobList.get(0);
			String newInstanceJob = addInstanceJobNode + "/" + jobName;
			String delJobNode = instanceJob + "/" + jobName;
			String job = regCenter.get(instanceJob + "/" + jobName);
			regCenter.persist(newInstanceJob, job);
			regCenter.remove(delJobNode);

		}

		/**
		 * 新增job任务配置 分配到 执行任务数最少的机器上
		 *
		 * @param jobClass job类名
		 * @param jobName  zk注册任务名称
		 * @param data     操作数据
		 */
		private void jobsDataAdded(String jobClass, String jobName, String data) {
			String instancs = ZkNodeConstants.INSTANCES.replace(ZkNodeConstants.JOB, jobClass);
			List<String> instancesList = regCenter.getChildrenKeys(instancs);
			if (CollectionUtils.isEmpty(instancesList)) {
				return;
			}
			int minCount = Integer.MAX_VALUE;
			String minInstance = instancesList.get(0);
			for (String instance : instancesList) {
				String instanceJob = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
				                                            .replace(ZkNodeConstants.IP, instance);
				int jobCount = regCenter.getNumChildren(instanceJob);
				if (jobCount < minCount) {
					minCount = jobCount;
					minInstance = instance;
				}
			}
			String newInstanceJob = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
			                                               .replace(ZkNodeConstants.IP, minInstance) + "/" + jobName;
			regCenter.persist(newInstanceJob, data);
		}

		/**
		 * 服务实例发生变更
		 *
		 * @param path      zk节点地址
		 * @param eventType 操作类型
		 */
		private void instancesChange(String path, TreeCacheEvent.Type eventType) {
			String[] paths = path.split("/");
			if (paths.length != NODE_SIZE) {
				return;
			}
			String jobClass = paths[2];
			String ip = paths[4];
			try {
				String jobNode = ZkNodeConstants.JOBS.replace(ZkNodeConstants.JOB, jobClass);
				String instancs = ZkNodeConstants.INSTANCES.replace(ZkNodeConstants.JOB, jobClass);
				log.info(instancs);
				int jobCount = regCenter.getNumChildren(jobNode);
				List<String> instancesIp = regCenter.getChildrenKeys(instancs);
				if (eventType == TreeCacheEvent.Type.NODE_ADDED) {
					instanceNewNode(jobCount, jobClass, ip, instancesIp);
				} else if (eventType == TreeCacheEvent.Type.NODE_REMOVED) {
					instanceDelNode(jobClass, ip, instancesIp);
				}
			} catch (Exception e) {
				log.info("job listener instancesChange has error {}", e);
			}
		}

		/**
		 * 增加新服务实例 从其他服务中随机获取几个 放到该服务实例上 平均压力
		 *
		 * @param jobCount    任务数量
		 * @param jobClass    job类名称
		 * @param ip          服务实例ip地址
		 * @param instancesIp 所有的服务实例ip
		 */
		private void instanceNewNode(int jobCount, String jobClass, String ip, List<String> instancesIp) {
			int minCount = jobCount / instancesIp.size();
			int maxCount = Double.valueOf(Math.ceil(jobCount / instancesIp.size())).intValue();
			String newInstanceJob =
					ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass).replace(ZkNodeConstants.IP, ip);
			int newJobCount = regCenter.getNumChildren(newInstanceJob);
			if (newJobCount != 0) {
				return;
			}
			int addCount = 0;
			for (String instance : instancesIp) {
				if (ip.equals(instance)) {
					continue;
				}
				String oldInstanceJob = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
				                                               .replace(ZkNodeConstants.IP, instance);
				List<String> childrenKeys = regCenter.getChildrenKeys(oldInstanceJob);
				int delCount = childrenKeys.size() - minCount;
				for (int i = 0; i < delCount; i++) {
					if (addCount >= maxCount) {
						break;
					}
					String delJob = childrenKeys.get(i);
					String delJobNode = oldInstanceJob + "/" + delJob;
					String jobData = regCenter.get(delJobNode);
					regCenter.persist(newInstanceJob + "/" + delJob, jobData);
					regCenter.remove(delJobNode);
					addCount++;
				}
			}
		}

		/**
		 * 服务实例下线 将注册在本实例的任务轮循注册到其他实例上
		 *
		 * @param jobClass    job类名称
		 * @param ip          服务实例ip地址
		 * @param instancesIp 所有的服务实例ip
		 */
		private void instanceDelNode(String jobClass, String ip, List<String> instancesIp) {
			String removeInstanceJob =
					ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass).replace(ZkNodeConstants.IP, ip);
			List<String> removeJob = regCenter.getChildrenKeys(removeInstanceJob);
			for (int i = 0; i < removeJob.size(); i++) {
				String jobName = removeJob.get(i);
				int number = i % instancesIp.size();
				String instance = instancesIp.get(number);
				String addJobNode = ZkNodeConstants.HOSTJOB.replace(ZkNodeConstants.JOB, jobClass)
				                                           .replace(ZkNodeConstants.IP, instance) + "/" + jobName;
				String delJobNode = removeInstanceJob + "/" + jobName;
				String jobData = regCenter.get(delJobNode);
				regCenter.persist(addJobNode, jobData);
			}
			regCenter.remove(removeInstanceJob);
		}


	}

	/**
	 * 判断当前节点是否是主节点.
	 *
	 * <p>
	 * 如果主节点正在选举中而导致取不到主节点, 则阻塞至主节点选举完成再返回.
	 * </p>
	 *
	 * @return 当前节点是否是主节点
	 */
	private boolean isLeaderUntilBlock() {
		while (!hasLeader()) {
			try (LeaderLatch latch = new LeaderLatch(regCenter.getClient(), ZkNodeConstants.LEADER_LATCH)) {
				latch.start();
				latch.await();
				if (!hasLeader()) {
					regCenter.persistEphemeral(ZkNodeConstants.LEADER, jobInstanceId);
				}
			} catch (Exception ex) {

			}
		}
		return isLeader();
	}

	/**
	 * 判断当前节点是否是主节点.
	 *
	 * @return 当前节点是否是主节点
	 */
	private boolean isLeader() {
		return regCenter.get(ZkNodeConstants.LEADER).equals(jobInstanceId);

	}

	/**
	 * 判断是否已经有主节点.
	 *
	 * @return 是否已经有主节点
	 */
	private boolean hasLeader() {
		return regCenter.isExisted(ZkNodeConstants.LEADER);
	}
}
