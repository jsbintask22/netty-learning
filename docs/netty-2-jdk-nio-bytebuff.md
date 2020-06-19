我们知道，netty直接与jdk的原生nio开发的，可以说是jdk nio的增强，所以理解jdk nio的机制就变得非常重要，接下来将介绍jdk中关于nio的几个非常重要的组件
示例源码： [https://github.com/jsbintask22/netty-learning](https://github.com/jsbintask22/netty-learning)
<!-- more -->

## ByteBuffer介绍
对比jdk bio的写法，在从Socket读取数据的时候，不管是客户端还是服务端，我们通常需要将字节读取到一个字节数组，知道读取的数据长度满足所需要的条件：
![socket通信模型](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/1.png)
```java
InputStream is = socket.getInputStream();
byte[] buff = new byte[1024];
int len;

while ((len = is.read(buff)) != -1) {
    System.out.println("client msg: " + new String(buff, 0, len));
}
```
这里的**buff**我们就可以认为是一个数据容器，后续的其他处理（比如这里的 new String）都会从这个数据容器中取。

-----
而在nio编程中，这个字节数组改为了一个可重复使用的容器**Buffer(java.nio.Buffer)**，它有多种同类型的基本类型子类，它的结构如下：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/2.png)
假设有一个长度为**n**的容器。
1. position指针代表洗一个可以读或者可以写入的位置索引，初始状态则指向0
2. limit代表*下一个*不能写入或者读取的位置，初始位置则指向n（注意索引是从0开始的）
3. capacity总是表示容器的大小，总是为n
4. mark作为一个标志为，当调用mark()方法时，会将position赋值给mark， 这样在下次调用reset()方法的时候可以让position回到上次标志的位置
有了以上这些，在写入或者读取时总是会移动position的位置，而如果想重置复原则需要移动.  它们直接总是会满足这样的关系：
**mark <= position <= limit <= capacity**

----
这里指的注意的是 position 代表的是下一个可以读或者可以写的位置，所以读写均会移动位置。

## Buffer使用
以IntBuffer为例，介绍下它的使用,IntBuffer继承自Buffer，内部使用一个整型数组存储数据 `final int[] hb;`

-----
1. 向容器中存储数据： `put(int i);`, `put(int index, int i);` 从指定位置的存储不会移动 position 指针的位置。
2. 获取数据： `int get();`, `int get(int index);` 从指定索引位置获取数据不会移动 positon 位置.
3. 检查是否有有效数据： 检查position 是否小于limit即可 .`return position < limit;`

```java
IntBuffer intBuffer = IntBuffer.allocate(10); // 1
for (int i = 0; i < intBuffer.capacity(); i++) {
    intBuffer.put((i + 1));  // 2
}

intBuffer.flip();  // 3
while (intBuffer.hasRemaining()) {  // 4
    int i = intBuffer.get();  // 5
    System.out.println(i);
}

intBuffer.rewind();
while (intBuffer.hasRemaining()) {
    int i = intBuffer.get();
    System.out.println(i);
}
```
1. 分配一个字节数据，默认在heap上（内部一个长度为10的整型数据）
2. 写入数据，会移动 position 的位置
3. 翻转数据区域，将已经写入了数据的区域用limit和position标识出来，即： `limit = position; position = 0; remark = -1;`
4. 检查是否有有效数据.
5. 获取当前position指针的数据，并且向前移动

-----
最后介绍下 *rewind, flip, clear*三个方法的区别
1. clear为将所有指针重置为初始状态：`position = 0;  limit = capacity;  mark = -1;`，一般用于在进行一系列读写操作后需要重用该容器进行重置.
2. flip为将有效数据隔离，比如put数据后，调用该方法将数据读取出来 `limit = position; position = 0; remark = -1;`
3. rewind用于读取了数据后需要重复读取数据，即将position指针重置为0即可, `position = 0; mark = -1;`

## 总结
1. 在bio编程中，我们通常使用字节数据向socket写入数据或者读取数据到字节数据，而在nio中则一般使用Buffer进行该操作，它有多个基本类型子类，最常用的即是*ByteBuffer*
2. Buffer的读写原理为移动其中的position，limit指针
3. rewind，flip， clear三者区别

---
扩展： 对于Buffer而言，直接使用子类的allocate方法分配数组在heap上，它还可以分配在堆外内存，称为直接内存，可以实现零拷贝，提高效率