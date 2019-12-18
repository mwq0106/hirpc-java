package com.hirpc.service;

import tutorial.Test2;

public interface DemoService {
    Test2.Person hello(Test2.Person person1, Test2.Person person2);
    String hello2(String name);
}
