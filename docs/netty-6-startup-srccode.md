现在我们已经基本了解了netty底层使用的组件，就明白了netty为什么是事件驱动模型：(netty极简教程（四）：[Selector事件驱动以及SocketChannel
的使用](https://www.jianshu.com/p/63a725c0646e)，接下来追踪下netty的启动源码，验证reactor模型在netty的实现

-----
示例源码： [https://github.com/jsbintask22/netty-learning](https://github.com/jsbintask22/netty-learning)

## 示例
```java
NioEventLoopGroup bossLoopGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workLoopGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossLoopGroup, workLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new DiscardServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = serverBootstrap.bind(port).addListener(f -> {
                System.out.println("started.");
            }).sync();

            channelFuture.channel().closeFuture().addListener(future ->
                    System.out.println("DiscardServerApp.operationComplete")).sync();
        } finally {
            bossLoopGroup.shutdownGracefully();
            workLoopGroup.shutdownGracefully();
        }
```