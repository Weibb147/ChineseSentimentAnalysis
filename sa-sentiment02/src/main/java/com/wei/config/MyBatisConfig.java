package com.wei.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author: admin
 * @date: 2024/12/9
 */
@Configuration
@MapperScan("com.wei.mapper")
public class MyBatisConfig {
}