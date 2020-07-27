package com.xsy.job.annotation;


import java.lang.annotation.*;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:05
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ElasticTasks {

	ElasticTask[] value();

}
