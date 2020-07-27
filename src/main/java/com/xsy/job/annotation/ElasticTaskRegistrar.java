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
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:11
 **/
@Slf4j
public class ElasticTaskRegistrar implements BeanPostProcessor, ImportBeanDefinitionRegistrar, Ordered,
                                             SmartInitializingSingleton, BeanFactoryAware, ResourceLoaderAware,
                                             EnvironmentAware {

	private BeanFactory beanFactory;
	private final Set<Class<?>> nonAnnotatedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(64));
	private BeanExpressionResolver resolver;
	private BeanExpressionContext expressionContext;

	private List<ElasticTaskContainer> endpoints = Lists.newArrayList();
	@Autowired
	private ElasticTaskRegistry registry;
	private Environment environment;
	private ResourceLoader resourceLoader;

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
			Class<?> targetClass = AopUtils.getTargetClass(bean);
			ElasticTask annotation = AnnotationUtils.findAnnotation(targetClass, ElasticTask.class);
			if (annotation != null && bean instanceof com.dangdang.ddframe.job.api.ElasticJob) {
				processElasticTask(bean, targetClass, annotation);
			}
		}
		return bean;
	}

	private void processElasticTask(Object bean, Class<?> targetClass, ElasticTask annotation) {
		DefaultElasticTaskContainer endpoint = new DefaultElasticTaskContainer(annotation, (ElasticJob) bean);
		endpoints.add(endpoint);
	}


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 200;
	}


	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		registerElasticTasks(importingClassMetadata, registry);
	}

	private void registerElasticTasks(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		ClassPathScanningCandidateComponentProvider scanner = getScanner();



		Set<String> basePackages = getBasePackages(importingClassMetadata);
		for (String basePackage : basePackages) {
			Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
			for (BeanDefinition candidateComponent : beanDefinitions) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					Map<String, Object> attributes = annotationMetadata.getAnnotationAttributes(ElasticTasks.class.getCanonicalName());
					Class<?> targetClass = getTargetClass(candidateComponent);
					if (attributes == null) {
						AnnotationAttributes elasticTask =
								(AnnotationAttributes) annotationMetadata.getAnnotationAttributes(ElasticTask.class.getCanonicalName());
						registerElasticTask(elasticTask, targetClass, registry);
					} else {
						AnnotationAttributes[] elasticTasks = (AnnotationAttributes[]) attributes.get("value");
						for (AnnotationAttributes elasticTask : elasticTasks) {
							registerElasticTask(elasticTask, targetClass, registry);
						}
					}
				}
			}
		}
	}

	private Class<?> getTargetClass(BeanDefinition candidateComponent) {
		Class<?> targetClass = null;
		try {
			targetClass =
					ClassUtils.forName(candidateComponent.getBeanClassName(), candidateComponent.getClass().getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
		return targetClass;
	}

	private void registerElasticTask(AnnotationAttributes elasticTask, Class<?> targetClass, BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder genericBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(targetClass);
		AbstractBeanDefinition abstractBeanDefinition = genericBeanDefinition.getBeanDefinition();
		registry.registerBeanDefinition(elasticTask.getString("jobName"), abstractBeanDefinition);
	}

	protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
		Map<String, Object> attributes = importingClassMetadata
				.getAnnotationAttributes(EnableElasticJob.class.getCanonicalName());
		Set<String> basePackages = new HashSet<>();
		for (String pkg : (String[]) attributes.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		return basePackages;
	}

	private ClassPathScanningCandidateComponentProvider getScanner() {
		ClassPathScanningCandidateComponentProvider provider =
				new ClassPathScanningCandidateComponentProvider(false, environment);
		provider.setResourceLoader(provider.getResourceLoader());
		provider.addIncludeFilter(new AnnotationTypeFilter(ElasticTasks.class));
		provider.addIncludeFilter(new AnnotationTypeFilter(ElasticTask.class));
		return provider;
	}


	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {

		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {

		this.environment = environment;
	}
}
