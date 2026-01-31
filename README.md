## 📄 License

This project is licensed under the MIT License.  
See the [LICENSE](./LICENSE) file for details.

Tunnel 是一个基于 Java/Netty/Spring Boot 的内网穿透代理
支持 HTTP 全透传、WebSocket 隧道、多客户端路由及可管理 UI。
适用于云桌面网络隔离场景。

## 🌟 功能

- 多客户端注册同一路由
- HTTP 请求全量原样透传（Header + Body）
- WebSocket 代理通道
- 路由负载（轮询/权重扩展）
- 管理页面：查看客户端 & 路由 & 强制下线
- 心跳 & 自动剔除失活客户端

