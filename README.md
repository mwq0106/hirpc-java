# hirpc-java
hirpc是一个跨语言的服务治理rpc框架，hirpc-java是其java版本，其拥有很多创新性的设计。

长久以来，跨语言rpc与服务治理一直不能有效结合，比如谷歌的grpc支持跨语言，但是其并不支持服务治理，仍要对其进行二次开发，
并且grpc也并不简洁易用，其对Spring没有做任何支持。在java中阿里的dubbo鼎鼎有名，支持服务治理，但是其并不支持跨语言。所以本框架由此而生，
将会尝试将跨语言rpc与服务治理进行结合，提供出一个具备服务治理功能的跨语言rpc框架，并且简洁易用。
# 总体流程
![image](https://github.com/mwq0106/hirpc-java/blob/master/assert/QQ%E6%88%AA%E5%9B%BE20191218202933.png)
# 特性
- 支持跨语言的rpc，基于ProtoBuf实现，底层协议对跨语言提供了支持，
在进行跨语言rpc时只需编写方法参数对应的.proto文件，并且编译成对应语言的实体类即可进行rpc调用，
而不需要像grpc一样还需安装对应语言的插件然后再通过插件进行编译，同时编写.proto文件的方式也更简单。
相比grpc，此种方式更加的简单易懂，学习成本也更低。
- 同语言内的rpc无需编写.proto文件，同语言内的rpc并没有涉及到跨语言，所以并不需要ProtoBuf的支持，
同语言内方法参数可以灵活定义，只有在有跨语言的需求时才需要编写.proto文件
- 服务注册，服务发现与服务治理，基于ZooKeeper实现了服务治理的核心功能，当提供服务的机器上线时，
会向服务注册中心（即ZooKeeper）注册服务信息，服务消费方可以在服务注册中心进行服务发现，
获取提供服务的机器列表，当提供服务的机器下线时，服务注册中心会删除该机器的相关信息
- 负载均衡机制，框架实现了轮询，随机，一致性哈希等负载均衡机制
- 失败机制，消费端如果发生了服务调用失败的情况，则会触发相应的失败机制，包括快速失败，故障重试，故障转移
- 注解式服务注册与服务发现，在java版本中与spring深度结合，可在springBoot项目中使用注解的方式完成服务注册与服务注入，
全面的提高开发的效率与便捷性，抛弃传统的xml配置。
- 多种序列化方式支持，在进行跨语言rpc时，将会使用ProtoBuf作为序列化框架，当进行非跨语言rpc时，
在java中将会使用kryo作为序列化框架
- 自定义rpc协议，能实现跨语言的rpc，除了使用了ProtoBuf提供支持，还有另外一个关键因素就是自定义传输层的rpc协议，
协议的设计充分考虑到了跨语言的需求以及未来的拓展，随着未来的发展协议可能还会作出相应变化
- TCP长连接及心跳机制
# 使用
- 在sample模块中已经给出使用示例，本地只需运行zookeeper，然后运行consumer与provider模块即可跑起本项目
- 开发的基本流程：

1.在api中定义一个接口
```java
public interface DemoService {
    Test2.Person hello(Test2.Person person1, Test2.Person person2);
    String hello2(String name);
}
```
2.在provider中实现该接口并且加上注解@RpcService
```java
@RpcService
public class DemoServiceImpl implements DemoService{
    @Override
    public Test2.Person hello(Test2.Person person1,Test2.Person person2){
        return "hi" + xxx1 + xxx2;
    }
    @Override
    public String hello2(String name){
        return "hi," + name;
    }
}
```
3.在consumer中使用注解@RpcReference即可完成依赖注入及服务引用
```java
@RestController
public class DemoController {
    @RpcReference
    private DemoService demoService;

    @RequestMapping("/sayHello")
    public String sayHello(@RequestParam String name) {
        Test2.Person person = demoService.hello(xx,xxx);
        return person.getName();
    }
    @RequestMapping("/sayHello2")
    public String sayHello2(@RequestParam String name) {
        return demoService.hello2(name);
    }
}
```
4.运行zookeeper环境

5.运行consumer与provider模块

6.本地访问http://localhost:8082/sayHello?name=aa
# 核心设计
## 如何基于protobuf设计一个跨语言RPC协议
        设计一个跨语言的RPC协议是实现跨语言RPC调用的重中之重，RPC本质是基于TCP长连接的一种通信传输，通信双方需要约定好相应的字节或者精确到比特协议信息。
        协议的设计还是使用了经典的header+body格式，header是一个个约定好的字节顺序，每个字节都有自己独特的作用，body则是protobuf对象在不同语言中序列化后的字节数组，使用header+body这种方式，也能帮我们轻松解决TCP拆包与粘包的问题。
        每个RPC请求都会封装成为此种格式，header中会给出请求的基本信息，如魔数用于标识这是RPC协议的开始，序列化类型标识使用哪种序列化协议，消息ID用于区分不同请求，这些都是一些最基本的信息。
        重点在于，我们怎么包装RPC调用当中的服务路径（对应于java中的包名+类名），服务名（对应于java中的方法名），方法参数类型，方法实参，以及返回值，并且还要考虑包装之后，还能够在各个语言当中解析出来，既然是基于protobuf实现，那么我们可以使用protobuf来做包装，将调用的一些关键信息放到protobuf中，然后序列化成二进制，到各个语言当中，先是读取header，然后根据header中的信息再读取protobuf的二进制数据，将protobuf的二进制反序列化为相应的protobuf对象，所以理论上protobuf支持的语言，我们都可以在该语言上进行RPC调用。
    请求包装proto文件：
        关键参数servicePath，serviceName，parameterType。每次进行RPC调用时都会生成一个相应的包装类，将服务路径，对应的其实就是各个语言中的包名+类名，装入servicePath中，调用方法名装入serviceName中，还有参数类型，这里使用了string类型的数组。在进行RPC调用时，将会通过servicePath定位到各个语言中的服务对象，这个服务对象对应于各个语言中对象，在对应的服务中使用类似于HashMap的方式存储服务对象，key值即是servicePath，value值则是服务对象。serviceName则是对象中的各个方法名。
        parameterType则是一个字符串数组，它是实现跨语言RPC的核心设计之一，parameterType是数组是因为每次调用有可能包含多个参数，是字符串是因为我们会在各个服务中又保存一份HashMap，key值是string，来源于protobuf中的package名+ message名，所以我们要求进行RPC调用时，方法的参数以及返回值都必须要是protobuf生成的对象，并且声明了package，package+message将会用于定位相应的参数类型，value则是该参数在各个语言中的参数类型，可以根据该参数类型生成对应的参数实例，然后用于反序列化接收到的二进制数据，比如在java中就是class对象，在go中则是kind对象，我们可以根据该类型生成对应的对象实例，所以因为这个原因，在进行RPC调用时，每个参数与返回值都必须是在客户端与服务端都必须共同持有。本质上来说，我们使用这种映射解决了二进制数据转为多个protobuf对象的问题。所以我们区分了body1与body2的概念，body1是用于传输调用服务路径，方法名，方法类型。Body2则是调用时的方法实参，每个实参对应的二进制数据前面都有一个int表示当前实参的长度，我们只需按长度读取即可，并且body1中的parameterType数组也能标识出对应实参的类型，parameterType数组长度与body2的数量相等，如假设parameterType[0]是字符串tutorial.Test2.Person，我们可以去HashMap中取得java中的tutorial.Test2.Person class对象，通过该class对象生成类实例，再通过类实例去反序列化二进制数据，从而得到方法实参。通过以上body1转为request包装对象，request包装对象中又含有方法调用的参数信息，参数信息以字符串形式存储，再将字符串映射为参数类型，类型再生成对象，对象再去反序列化二进制数据的方式，解决了使用protobuf作为跨语言rpc调用的关键步骤。
        总结一遍协议的解析过程，在进行一次协议解析时，首先会根据魔数标识这是一个rpc协议的开始，然后读取header中的序列化类型，消息ID等，发现是使用protobuf作为序列化方式，然后再读取body1长度，使用RequestInner类去反序列化body1中的二进制数据，得到调用的一些基本信息，然后又根据RequestInner类中的parameterType字符串数组，得到调用参数类型的信息，本地持有一个parameterType的映射表，key值是字符串，value值是对象类型，接下来读取body2的信息，先根据parameterType的第一个值，得到第一个参数的类型，然后根据该类型得到第一个参数的对象实例，然后使用这个对象实例去反序列化第一个参数的二进制数据，第一个参数的二进制数据我们使用了int+body的格式去传输，先读一个int，得到第一个参数的长度，然后再读取相应长度的数据，即可得到第一个参数的二进制数据，从而完成了第一个参数的反序列化过程，第二第三个等参数的解析也是使用类似的思想，从而完成了一次rpc协议的解析过程，并且通过这种方式，是可以做到跨语言rpc的，理论上只要protobuf支持的语言，我们都可以做rpc。
        在返回响应数据的过程也是类似的原理，感兴趣的同学可以自己想一下应该怎么设计相关协议。
