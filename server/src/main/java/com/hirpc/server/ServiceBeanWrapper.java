package com.hirpc.server;

/**
 * 服务的bean以及其注解信息
 * @author mwq0106
 * @date 2019/11/6
 */
public class ServiceBeanWrapper {
    private Object serviceBean;
    private String annotationInfo;
    public ServiceBeanWrapper(Object serviceBean,String annotationInfo){
        this.serviceBean = serviceBean;
        this.annotationInfo = annotationInfo;
    }
    public void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
    }

    public void setAnnotationInfo(String annotationInfo) {
        this.annotationInfo = annotationInfo;
    }

    public Object getServiceBean() {
        return serviceBean;
    }

    public String getAnnotationInfo() {
        return annotationInfo;
    }
}
