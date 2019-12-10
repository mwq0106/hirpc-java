package com.hirpc.beanprocess;

import com.hirpc.client.RpcClient;
import com.hirpc.annotation.RpcReference;
import com.hirpc.exception.RpcReferenceException;
import com.hirpc.registry.ServerInRegistry;
import com.hirpc.registry.ServiceDiscovery;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @author mwq0106
 * @date 2019/10/23
 */
@Component
public class RpcReferenceAnnotationBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(RpcReferenceAnnotationBeanPostProcessor.class);
    private ApplicationContext applicationContext;
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    private Object refer(RpcReference rpcReference,Class clazz,String beanName) throws Exception{
        RpcClient rpcClient = applicationContext.getBean(RpcClient.class);
        ServiceDiscovery serviceDiscovery = applicationContext.getBean(ServiceDiscovery.class);
        ServerInRegistry condition = getServerMatchCondition(rpcReference);
        if(!rpcReference.interfaceClass().equals(void.class)){
            if(!rpcReference.interfaceClass().isInterface()){
                throw new RpcReferenceException("bean:"+beanName+"注解RpcReference所指定的类不是接口:"+rpcReference.interfaceClass().getName());
            }
            serviceDiscovery.discoverAndConnectServers(rpcReference.interfaceClass().getName(),condition);
            return rpcClient.create(rpcReference.interfaceClass(),rpcReference);
        }else {
            if(!clazz.isInterface()){
                throw new RpcReferenceException("bean:"+beanName+"注解RpcReference所指定的类不是接口:"+clazz.getName());
            }
            serviceDiscovery.discoverAndConnectServers(clazz.getName(),condition);
            return rpcClient.create(clazz,rpcReference);
        }
    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (! field.isAccessible()) {
                    field.setAccessible(true);
                }
                RpcReference rpcReference = field.getAnnotation(RpcReference.class);
                if (rpcReference != null) {
                    Object value;
                    try {
                        value = refer(rpcReference, field.getType(),beanName);
                    }catch (Exception e){
//                        throw new RpcReferenceException("Fail to connect to remote server"+", cause: " + e.getMessage(), e);
                        throw new RpcReferenceException("连接远程服务器失败, 原因: " + e.getMessage(), e);
                    }
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Throwable e) {
                //todo 异常丢出示例
//                logger.error("Failed to init remote service reference at filed:" + field.getName() + " in class " + bean.getClass().getName() + ", cause: " + e.getMessage(), e);
                logger.error("初始化RpcReference注解失败，在类：" + bean.getClass().getName() + " 的字段：" + field.getName() + "上, 原因: " + e.getMessage(), e);
            }
        }
        return bean;
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    private ServerInRegistry getServerMatchCondition(RpcReference rpcReference){
        //todo RpcReference注解修改需要更新
        ServerInRegistry serverInRegistry = new ServerInRegistry();

        if(StringUtils.isNotBlank(rpcReference.group())){
            serverInRegistry.setGroup(rpcReference.group());
        }
        if (StringUtils.isNotBlank(rpcReference.version())){
            serverInRegistry.setVersion(rpcReference.version());
        }
        return serverInRegistry;
    }
}
