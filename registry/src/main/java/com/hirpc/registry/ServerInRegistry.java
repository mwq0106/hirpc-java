package com.hirpc.registry;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 增加字段需重写equals与hashCode，所有字段都必须为String
 * @author mwq0106
 * @date 2019/11/16
 */
public class ServerInRegistry {
    private String host;
    private String port;
    private String applicationName;
    private String version;
    private String group;
    private String pid;
    private String timestamp;
    public String getAddress(){
        return host + ":" + port;
    }
    public static ServerInRegistry parse(String serverInfo){
        ServerInRegistry serverInRegistry = new ServerInRegistry();
        if(StringUtils.isBlank(serverInfo)){
            return serverInRegistry;
        }
        String[] variables = serverInfo.split("&");
        Field[] fields = ServerInRegistry.class.getDeclaredFields();
        for (String variable:variables){
            String name = variable.split("=")[0];
            String value = variable.split("=")[1];
            boolean haveThisVariable = false;
            for (Field f:fields){
                if(f.getName().equals(name)){
                    f.setAccessible(true);
                    try {
                        f.set(serverInRegistry,value);
                    }catch (Exception e){
                        throw new RuntimeException("Fail to set ServerInRegistry's value:" + variable + "," + e);
                    }
                    haveThisVariable = true;
                }
            }
            if(!haveThisVariable){
                throw new RuntimeException("The variable:" + variable + " is not defined in ServerInRegistry");
            }
        }
        return serverInRegistry;
    }
    public String toRegistryString(){
        List<String> list = new LinkedList<>();
        Field[] fields = ServerInRegistry.class.getDeclaredFields();
        for (Field f:fields){
            f.setAccessible(true);
            try {
                if(f.get(this) != null) {
                    String variable = f.getName() + "=" + f.get(this);
                    list.add(variable);
                }
            }catch (Exception e){
                throw new RuntimeException("fail to convert ServerInRegistry to registry string:"+e.getMessage());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if(i == 0){
                sb.append(list.get(i));
            }else {
                sb.append("&").append(list.get(i));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        ServerInRegistry that = (ServerInRegistry) o;
        return Objects.equals(host, that.host) &&
                Objects.equals(port, that.port) &&
                Objects.equals(applicationName, that.applicationName) &&
                Objects.equals(version, that.version) &&
                Objects.equals(group, that.group) &&
                Objects.equals(pid, that.pid) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, applicationName, version, group, pid, timestamp);
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPid() {
        return pid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getVersion() {
        return version;
    }

    public String getGroup() {
        return group;
    }

}
