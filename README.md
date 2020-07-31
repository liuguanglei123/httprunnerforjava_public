之前学习python的时候有一个非常好用的接口测试工具httprunner，一直在java平台没有找到这种快捷易用的工具，本着独立做项目学习java的目的，决定使用java重写一遍httprunner的功能。

项目重复做了多遍，因为一开始设计不完善，导致最终实现时存在层层阻碍，最近的一次基本已经把整体框架设计好，虽然还存在许多bug，但是后期希望能一直维护下去，原版HttpRunner已经非常优秀了，但是在java项目中如果想引入这种测试代码，是一件相对比较麻烦的事情。

以下内容除非特殊说明，均已通过java实现。

# 快速上手
## 用例结构

httprunner 是一款面向 HTTP(S) 协议的通用测试框架，只需编写维护一份 YAML 脚本，即可实现自动化测试、性能测试、线上监控、持续集成等多种测试需求。

测试用例结构共分三级，分别是api case suite，范围从小到大。

api主要用来定义单个http接口请求和校验内容，例如
```
request:
  url: https://movie.douban.com/j/search_tags?type=$type&source=index
  method: GET
  headers:
    Accept: "*/*"
    Accept-Language: zh-CN,zh;q=0.9
    Connection: keep-alive
    Host: movie.douban.com
    Referer: https://movie.douban.com/
    User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3775.400 QQBrowser/10.6.4208.400
    X-Requested-With: XMLHttpRequest
validate:
  - equals: ["status_code", 200]
```
很容易看出其中包含了请求连接url，请求类型method，请求头headers 请求rul的参数，当然除了请求，还包含结果的校验部分validate，可以校验响应结果码，或是验证返回的请求中某个节点的某个值。

case是对api的封装，内容大致如下：
```
- config:
    name: caseExample1

- test:
    api: api/searchTags.yml
    name: searchTagsMovie
    variables:
      type: movie

- test:
    api: api/searchTags.yml
    name: searchTagsTv
    variables:
      type: tv
```
文件中包含了config节点，可以用来配置各项参数，test节点则是一个个单独的测试用例组成，case文件支持嵌套其他的case级别的用例。

suite文件则是对case级别的更高级别的封装，比如
```
config:
  name: suiteExample1

testcases:
  singleApi:
    testcase: testcases/testCaseExample1.yml
```
这样可以一次性执行多个用例。

因为HTTP是无状态的，平时我们浏览某些网站，是依赖cookie去识别用户是否登录/有效的，httprunner中，也有会话的标记，在同一个case中的所有用例，理论上是可以共用cookie的，比如我们可以通过第一个case实现登录，后续要做查看订单/个人信息等就不在需要在header中人为写入cookie。

suite是操作用例的集合，一个整体的项目中可能包含非常多的用例，每个用例之间是相对独立的，但也可以通过output/extract等参数实现用例间数据的共享。

这三级用例结构是httprunner原版就已经实现的，在使用java实现的时候保留了原本的结构。

简单看一下用例执行日志和结果：
![report.png](https://guanglei.site/storage/thumbnails/_signature/2FEA51NKSJJIOB8KGOOTS665EI.png)

执行完用例后，会在对应的目录下生成测试报告，内容如下：
![testReport.png](https://guanglei.site/storage/thumbnails/_signature/2PNHQ0BC2HP3L0N51KDGE8363I.png)

# 基本功能描述
## 变量及作用域Context
在测试用例内部，hrun 划分了两层变量空间作用域（context）。

config：作为整个测试用例的全局配置项，作用域为整个测试用例；

test：测试步骤的变量空间（context）会继承或覆盖 config 中定义的内容；

1.若某变量在 config 中定义了，在某 test 中没有定义，则该 test 会继承该变量

2.若某变量在 config 和某 test 中都定义了，则该 test 中使用自己定义的变量值

3.各个测试步骤（test）的变量空间相互独立，互不影响；

4.如需在多个测试步骤（test）中传递参数值，则需要使用 extract 关键字，并且只能从前往后传递

实际用例说明：

仍然以上面一节的用例结构进行说明，httprunner支持使用variables来指定请求中的参数，在case文件中，第一个用例指定type为movie，第二个用例指定type为tv。

在httprunner读取到api文件时，发现http请求参数中的param包含了如下内容
`https://movie.douban.com/j/search_tags?type=$type&source=index `
判断此处需要读取指定的变量，很明显，第一个用例执行时type为"movie"，
![movie.png](https://guanglei.site/storage/thumbnails/_signature/3880J9BSUQIRFCTEFFNI1FDFFT.png)
第二个用例执行时type取config节点中的值"tv"
![tv.png](https://guanglei.site/storage/thumbnails/_signature/3TBE2H58SA8F0RE41Q7BFHCKLG.png)

## 参数化
在实际测试工作中，经常会遇到，重复录入多个用户，重复调用一个接口多次这样的场景，这些用例的特点是接口调用的都是同一个，只不过某几个参数不同而已，hrun也支持参数化的配置，一个parameter节点便能解决这个问题。同样是上面这个示例，我们可以将suite文件内容修改如下：
```java
config:
  name: suiteExample1

testcases:
  singleApi:
    testcase: testcases/testCaseExample1.yml
    parameters:
      type: ["movie","tv"]
```
对比第一节的suite文件，可以看到，这里多了一个parameters，其内容是我们需要制定的type的参数列表。

case文件修改如下
```java
- config:
    name: caseExample1

- test:
    api: api/searchTags.yml
    name: searchTagsMovie
```
case文件中不再指定type这个变量的值了，执行用例时，https://movie.douban.com/j/search_tags?type=$type&source=index这个链接会重复请求两次，第一次请求时type为movie，第二次请求时type为tv，这便是参数化的作用。

# 源码设计说明
该项目是基于maven管理的，引入的第三方项目主要是：

jcommander 用来解析命令行参数

snakeyaml  解析yml结构的文件

fastjson   解析生成json结构体

httpclient 高效http请求工具

jinjava    html模板嵌套工具

1.httprunner原版是使用了unittest第三方工具管理执行用例，在java中并没有找到合适的替代工具，只能尝试通过Api TestCase 和TestSuite三个类，来管理和执行用例。

2.测试用例的执行过程可以划分为多个独立的阶段，用例（文件）的读取，解析（是否存在参数，是否需要参数化生成多个用例），执行，结果校验，生成报告，因此采用低耦合的设计原则，每个阶段的方法和处理都独立出来，只要规范化每个环节之间传输的对象结构，每个环节都可以单独提取出来进行使用，这样设计的优点有三：

a) 提升功能特性迭代开发效率

b) 提高代码单元测试覆盖率

c) 保证框架本身的灵活性

3.httprunner的执行过程中免不了针对用例调用特定的方法，比如上面的用例中，有equals的校验，同时后期还需要实现钩子函数的执行，设计思路和原版httprunner一样，增加一个debugtalk.java文件，文件内可以实现某些测试用例执行前后需要执行的方法，httprunner可以自动读取该文件，并且编译，反射调用其中的方法。


