lianxi
--
RPC framework based on netty for kids

Feature
--
1. Extremely lightweight compared with other rpc frameworks.
2. Extremely easy to use, Extermly low cost for learning.
3. Extremely easy to understand, With approximately(大约) 800 lines of code.
4. Base on JSON protocol(协议)
如果是要做一个开源项目，力求非常完美的话
   至少还要考虑一下几点。
    客户端连接池
    多服务进程负载均衡
    日志输出
    参数校验，异常处理
    客户端流量攻击
    服务器压力极限


RPC（Remote Procedure Call）远程过程调用，是通过网络调用远程计算机进程中
的某个方法，从而达到获取和传递数据或状态的实现，调用风格就如同调用本地的方法一样。