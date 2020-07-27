package com.xsy.job.supprt;

import java.time.LocalDate;

/**
 * @author xueshaoyi
 * @Date 2019/12/30 上午11:15
 **/
public class CronUtils {

	public static String nextYearCron() {
		return "0 * * ? " + LocalDate.now().minusMonths(1).getMonthValue() + " *";
	}
}
