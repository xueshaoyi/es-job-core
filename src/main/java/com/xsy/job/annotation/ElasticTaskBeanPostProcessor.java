package com.xsy.job.annotation;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.google.common.collect.Lists;
import com.xsy.job.config.DefaultElasticTaskContainer;
import com.xsy.job.config.ElasticTaskContainer;
import com.xsy.job.config.ElasticTaskRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:07
 **/
@Slf4j
public class ElasticTaskBeanPostProcessor implements BeanPostProcessor, Ordered,
                                                     SmartInitializingSingleton, BeanFactoryAware {

	private BeanFactory beanFactory;
	private final Set<Class<?>> nonAnnotatedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(64));
	private BeanExpressionResolver resolver;
	private BeanExpressionContext expressionContext;

	private List<ElasticTaskContainer> endpoints = Lists.newArrayList();
	@Autowired
	private ElasticTaskRegistry registry;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.resolver = ((ConfigurableListableBeanFactory) beanFactory).getBeanExpressionResolver();
			this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory, null);
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		for (ElasticTaskContainer endpoint : endpoints) {
			registry.register(endpoint);
		}

	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (!this.nonAnnotatedClasses.contains(bean.getClass())) {
			ElasticTask elasticTask = findElasticTask(bean, beanName);
			if (elasticTask != null) {
				processElasticTask(bean, elasticTask);
			}
		}
		return bean;
	}

	private ElasticTask findElasticTask(Object bean, String beanName) {

		Class<?> targetClass = AopUtils.getTargetClass(bean);
		ElasticTasks annotation = AnnotationUtils.findAnnotation(targetClass, ElasticTasks.class);
		if (annotation != null && bean instanceof com.dangdang.ddframe.job.api.ElasticJob) {
			for (ElasticTask elasticTask : annotation.value()) {
				if (elasticTask.jobName().equals(beanName)) {
					return elasticTask;
				}
			}
		}
		ElasticTask elasticTask = AnnotationUtils.findAnnotation(targetClass, ElasticTask.class);
		return elasticTask;
	}

	private void processElasticTask(Object bean, ElasticTask annotation) {
		DefaultElasticTaskContainer endpoint = new DefaultElasticTaskContainer(annotation, (ElasticJob) bean);
		endpoints.add(endpoint);
	}


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 200;
	}

}
