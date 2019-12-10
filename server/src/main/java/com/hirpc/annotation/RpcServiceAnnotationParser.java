package com.hirpc.annotation;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mwq0106
 * @date 2019/11/5
 */
public class RpcServiceAnnotationParser {
    public static String parseAnnotation(RpcService rpcService){
        //todo RpcService注解需要修改
        List<String> list = new LinkedList<>();
        if(StringUtils.isNoneBlank(rpcService.interfaceName())){
            list.add("interfaceName="+rpcService.interfaceName());
        }
        if(StringUtils.isNoneBlank(rpcService.version())){
            list.add("version="+rpcService.version());
        }
        if(StringUtils.isNoneBlank(rpcService.group())){
            list.add("group="+rpcService.group());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if(i == 0){
                sb.append(list.get(0));
            }else {
                sb.append("&").append(list.get(i));
            }
        }
        return sb.toString();
    }
}
