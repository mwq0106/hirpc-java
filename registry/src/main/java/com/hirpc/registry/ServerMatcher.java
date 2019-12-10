package com.hirpc.registry;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * @author mwq0106
 * @date 2019/11/16
 */
public class ServerMatcher {
    public static boolean isMatch(ServerInRegistry serverInRegistry, ServerInRegistry condition){
        Field[] fields = ServerInRegistry.class.getDeclaredFields();
        for (Field field:fields){
            field.setAccessible(true);
            try {
                if(field.get(condition) == null){
                    continue;
                }
                if(!field.get(condition).equals(field.get(serverInRegistry))){
                    return false;
                }
            }catch (Exception e){
                throw new RuntimeException("Fail to match ServerInRegistry's value:"+e);
            }
        }
        return true;
    }
}
