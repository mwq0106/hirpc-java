package com.hirpc.controller;

import com.hirpc.annotation.RpcReference;
import com.hirpc.service.DemoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tutorial.Test2;

@RestController
public class DemoController {
    @RpcReference
    private DemoService demoService;

    @RequestMapping("/sayHello")
    public String sayHello(@RequestParam String name) {
        Test2.Person.Builder builder = Test2.Person.newBuilder();
        builder.setName(name);
        builder.setAge(111);
        Test2.Person person1 = builder.build();
        builder.setName("ssss");
        Test2.Person person2 = builder.build();
        Test2.Person personResult = demoService.hello(person1,person2);
        return personResult.getName();
    }
    @RequestMapping("/sayHello2")
    public String sayHello2(@RequestParam String name) {
        return demoService.hello2(name);
    }
}
