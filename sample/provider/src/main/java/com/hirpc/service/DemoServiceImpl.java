package com.hirpc.service;

import com.hirpc.annotation.RpcService;
import tutorial.Test2;

@RpcService
public class DemoServiceImpl implements DemoService{
    @Override
    public Test2.Person hello(Test2.Person person1,Test2.Person person2){
        Test2.Person.Builder builder=Test2.Person.newBuilder();
        builder.setAge(0);
        builder.setName("hi,"+person1.getName()+",and "+person2.getName());
        return builder.build();
    }
    @Override
    public String hello2(String name){
        return "hi," + name;
    }
}
