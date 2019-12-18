package com.hirpc;

import static org.junit.Assert.assertTrue;

import com.hirpc.annotation.RpcReference;
import com.hirpc.service.DemoService;
import org.junit.Test;
import tutorial.Test2;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }
    static long a;
    static long b;
    @RpcReference
    private DemoService demoService;
    @Test
    public void shouldAnswerWithTrue() throws Exception
    {
        a=System.currentTimeMillis();
        int testTimes=60000;

        final CountDownLatch latch = new CountDownLatch(testTimes);
        Test2.Person.Builder builder=Test2.Person.newBuilder();
        builder.setName("aaa");
        builder.setAge(111);
        final Test2.Person person = builder.build();
        Test2.Person person2 = demoService.hello(person,person);
        System.out.println(person2.getName());

        for (int i = 0; i < testTimes; i++) {
            submit(new Runnable() {
                @Override
                public void run() {
                    Test2.Person person2 = demoService.hello(person,person);
//                    System.out.println(person2.getName());
                    latch.countDown();
                }
            });
        }
        latch.await();
        b=System.currentTimeMillis();
        System.out.println("hirpc调用，时间:"+(b-a));
    }
}
