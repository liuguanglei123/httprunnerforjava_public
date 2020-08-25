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

前一次的失败设计：
项目在第一次尝试使用java实现，应该是在19年10月份，当时尝试学习java，手边没有合适的项目去做，就用hrun重新实现作为一个学习的机会，但是在做的过程中，因为第一次尝试使用java做大一点的项目，各种考虑都是不全面甚至错误的，这才有了rebuild_hrun_java

但是rebuild_hrun_java又是一次失败的尝试。在实现的过程中，总是尝试保持原样的python实现，但是遇到非常多问题，比如python是弱类型语言，同一个变量既可以是dict又可以是list，同一个dict中，value可以是不同类型的对象；hrun的原本实现，基本是面向过程而非面向对象；hrun中用例的执行引入了第三方py库unittest；为了支持可变参数的使用，还加入了lazy_string和lazy_function，同一个variables中，既有普通的String，又有lazy_string的存在；新版的hrun中还有对文件格式的校验，也是使用第三方库完成的；hrun中function的执行，也是和java完全不同的两个概念。

那么问题来了，既然所有的地方都和python是不同的，为何还要保持完整的逻辑照搬？仅仅就是为了以后可以和原版保持同步更新吗？可以接受面向过程的繁琐，各种类型的强制转换，和几乎全部都是静态方法的实现方案吗？

答案是NO。

既然决定要改其实现方法了，那么索性改的彻底一点，让hrun面向对象实现，只需要保证几大步骤间的低耦合就好了。


设计原则一：
1.除了加载部分逻辑，尽量减少强制转换类型
2.全局测试用例中的string都是lazyString类型，只不过根据其内容，有的需要解析，有的取原值



