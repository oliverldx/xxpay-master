package org.xxpay.shop.config;

import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FluentValidatorConfiguration {

    /*@Bean("fluentValidateInterceptor")
    public FluentValidateInterceptor fluentValidateInterceptor(){
        FluentValidateInterceptor validateInterceptor = new FluentValidateInterceptor();
        validateInterceptor.setCallback(validateCarCallback);
        validateInterceptor.setLocale("zh_CN");
        validateInterceptor.setHibernateDefaultErrorCode(10000);
        return  validateInterceptor;
    }*/

    @Bean
    public BeanNameAutoProxyCreator beanNameAutoProxyCreator(){
        BeanNameAutoProxyCreator proxyCreator = new BeanNameAutoProxyCreator();
        proxyCreator.setBeanNames("*Controller"); // 配置拦截对象 如拦截service(*ServiceImpl)
//        proxyCreator.setInterceptorNames("fluentValidateInterceptor");
        return proxyCreator;
    }
}
