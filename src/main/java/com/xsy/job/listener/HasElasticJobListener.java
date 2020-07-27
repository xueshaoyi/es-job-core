package com.xsy.job.listener;

import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:15
 **/
public interface HasElasticJobListener {

	ElasticJobListener[] getListeners();
}
