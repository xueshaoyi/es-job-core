package com.xsy.job.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:11
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ElasticJobAutoConfiguration.class, ElasticTaskRegistrar.class})
public @interface EnableElasticJob {
	String[] basePackages() default {};
}
