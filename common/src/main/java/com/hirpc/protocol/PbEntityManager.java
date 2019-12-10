package com.hirpc.protocol;

import com.hirpc.config.ProtocolConfig;
import com.hirpc.protocol.util.PackageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mwq0106
 * @date 2019/10/8
 */
@Component
public class PbEntityManager {
    private static final Logger logger = LoggerFactory.getLogger(PbEntityManager.class);
    private Map<String,Class> pbEntityMap = new HashMap<>();
    @Resource
    ProtocolConfig protocolConfig;

    @PostConstruct
    public void init() {
        String[] packages = protocolConfig.getProtobufScanBasePackages().split(",");
        if(packages.length == 0){
            logger.debug("protobuf实体类扫描路径为空");
        }
        for (String packageName:packages){
            List<String> classNames = PackageUtil.getClassName(packageName);
            if(classNames == null || classNames.size() == 0){
                return;
            }
            for (String className : classNames) {
                Class clazz;
                try {
                    clazz = Class.forName(className);
                }catch (Exception e){
                    throw new RuntimeException("找不到类:" + className);
                }
                Class[] classes = clazz.getDeclaredClasses();
                for (int i = 0; i < classes.length; i++) {
                    int mod=classes[i].getModifiers();
                    if(Modifier.isPublic(mod) && Modifier.isStatic(mod) && Modifier.isFinal(mod)){
                        try {
                            Method method1=classes[i].getMethod("getDescriptor");
                            Object descriptor=method1.invoke(null);
                            Class descriptorClass=descriptor.getClass();
                            Method method2=descriptorClass.getMethod("getFullName");
                            Object fullName=method2.invoke(descriptor);
                            pbEntityMap.put((String) fullName,classes[i]);
                        }catch (Exception e){
                            throw new RuntimeException("该类不是pb生成的类:" + classes[i].getName());
                        }
                    }
                }
            }

        }
    }
    public Class getProtobufEntity(String fullName){
        return pbEntityMap.get(fullName);
    }
}
