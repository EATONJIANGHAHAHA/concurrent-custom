package com.mmall.concurrency.example;

/*
线程安全的定义:当多个线程访问某个类时,不管运行时采用何种调度方式或者这些进程将如何交替执行,并且在主调代码中不需要任何额外的同步或协同,
这个类都能表现出正确的行为,那么就称这个类是线程安全的.
 *
原子性:提供了互斥访问,同一时刻只能有一个线程来对他进行操作
可见性:一个线程对主内存的修改可以及时地被其他线程观察到
有序性:一个线程观察其他线程中的指令执行顺序，由于指令重排序的存在，该观察结果一般杂乱无序．

有序性的具体内容: Java内存模型中,允许编译器核处理器对指令进行小红心排序,但是排序过程不会影响到单线程程序的执行,却会影响到多线程并发执行的正确性.
volatile, synchronized, lock
先行发生原则:
1. 程序次序规则：　一个线程内，按照代码顺序，书写在前面的操作线性发生于书写在后面的操作（在单个线程中看起来是有序的）ＪＶＭ会对没有变量依赖性的程序
代码进行指令的重新排序，一直与编程人员在外部观察起来程序依然是有序的．因此，无法保证在多线程中的执行正确性．
２. 锁定规则： 一个unlock操作现行法生育后面对同一个锁的lock操作, 无论在单线程中还是多线程中, 必须先对指定锁进行解锁操作,才能对这个变量执行
枷锁操作.
3. volatile变量规则:对一个变量的写操作线性发生于面对这个变量的读操作.
4. 传递规则:如果操作A线性发生于操作B,而操作B先行有线性发生于操作C, 则操作A线性发生于操作C, 体现了线性发生原则具备传递性
5. 线程启动原则:Thread对象的start()方法线性发生于此线程的每一个动作.
6. 线程中断规则:对线程interrupt()方法的调用先行发生于被中断线程的代码检测到中断时间的发生.
7. 线程终结规则:线程中所有的操作都先行发生于线程的终止检测,我们可以通过thread.join()方法结束,
thread.isAlive()的返回值手段检测到线程已经中止执行.
8. 对象终结规则: 一个对象的初始化完成,线性发生于他的finalize()方法的开始.
如果两个操作的执行次序无法从先行发生原则推导出来,则不能保证两个操作执行次序的有序性,虚拟机可以随意的对她们进行重排序.
发布对象：是一个对象能够在当前范围之外的代码所使用．
对象溢出：一种错误的发布．当一个对象还没有构造完成时，就是他被其他线程所见．

安全发布对象：
1. 在静态初始化函数中初始化一个对象引用
2. 将对象的引用保存到volatile类型域或者AtomicReference对象中．
3. 将对象的引用保存到某个正确构造对象的final类型域中
4. 将对象的引用不去奥存到一个由锁保护的域中

不可变对象:本身就是线程安全的.
创建不可变对象需要满足的条件:
1. 对象创建以后其状态就不能修改
2. 对象所有域都是final类型
3. 对象是正确创建的(在对象创建期间,this引用没有溢出
可以采用的方式包括:
1. 将类声明为final,即不能被继承 (final类中所有方法都会被隐式的修饰成final方法, 一个类中的private方法也会被隐式的修饰成final方法
2. 将所有的成员声明为私有,即不能直接访问这些成员
3. 对变量不提供set方法,将所有成员声明为final,即只能对成员赋值一次.
4. 通过构造器初始化所有成员,
5. 在get方法中不返回对象本身而是创建一个对象的拷贝

线程封闭:
1. Ad-hoc 线程封闭:程序控制实现,最糟糕,忽略.......
2. 对战封闭:局部变量,无并发问题,全局变量容易引发并发问题.
3. ThreadLocal 线程封闭; 特别好的封闭方法

常用线程不安全的类 -> 线程安全的类:
StringBuilder -> StringBuffer
SimpleDateFormat -> JodaTime
ArrayList, HashSet, HashMap 等 Collections
先检查,在执行: if (condition(a)) {handle(a);}

AbstractQueuedSynchronizer - AQS,内部的实现由一个双向列表, 负责维护线程的执行顺序,并且可能存在多个conditionQueue, 这是一个单向列表,
只有当程序中需要使用到condition的时候才会被建立.
1. Node实现FIFO队列, 可以用于构建锁或者其他同步装置的基础框架.
2. 利用了一个int表示了状态, 在大多数同步装置里面这个变量表示了: 0 = 没有线程获取了锁, 1 = 已经有线程获取了锁, > 1 = 存入的锁的数量.
3. 使用的方法是继承
4. 子类需要通过继承并实现他的方法管理其状态, acquire() 和 release() 操纵状态.
5. 可以同时实现排它锁和共享锁模式 (独占, 共享)

CountDownLatch组件:并发控制,这个组件允许阻塞调用它的线程,可以使一个线程或多个线程阻塞,在其他线程完成任务后再继续执行这个线程的后续工作.
它的计数器不可以被重置.
它的使用主要是由两个方法组成:

Semaphor组件: 信号量, 并发控制, 可以控制并发访问的线程个数. 常用于仅能提供有限访问的资源, 比如数据库链接数
1. 构造器中传入同时访问的线程数

CyclicBarrier: 循环屏障, 允许一组线程相互等待, 通过它可以完成多个线程相互等待, 只有当多个线程满足条件才继续执行等待的线程. 它与countdownlatch很相似,
内部也维护了一个计数器, 但是这个计数器是可以重置的. 计数器是从0开始向上递增, 直到递增到计数器的目标值后继续执行等待线程.
多用于多线程计算数据, 然后合并计算结果.

锁:
synchronize关键字修饰的锁, 还有就是Lock借口实现的锁.
synchronized: JVM实现的,源码不可查,在JDK]5.0之前,它的性能是非常底下的, 官方对现版本已经进行了优化,非公平锁, 只能唤醒一个或者全部线程,
在调试的时候可以包括线程信息,因为他是JVM中的实现
ReentrantLock: 可重入锁,自行实现, 使用没有synchronized方便, 有时可能忘记释放锁.可以选择公平锁(先等待的线程先获得锁)还是非公平锁,
并且提供了condition类,可以实现分组唤醒线程, 提供了可以终端等待锁的机制,
ReentrantReadWriteLock: 在没有任何读,写锁的时候,才可以取得写锁.实现了悲观读取,然而如果读取情况很多,写入线程会进入饥饿状态,即迟迟无法竞争到锁.
StampedLock: 三种模式: 写,读,乐观读, 他的状态是由版本和模式组成,锁获取方法返回的是一个数字最为票据,用相应的所状态来表示并控制相应的访问,
数字0表示没有写锁被访问,读锁分为悲观锁和乐观锁, 乐观读的意思是, 如果读的操作很多,写的操作很少,我们可以乐观地认为这两个操作同时发生的几率很小,
因此不悲观的使用完全的悲观锁定,程序可以查看读取资料之后,是否遭到写入执行的变更,在采取相应的措施.他的吞吐量相比其他锁在高并发时有巨大的性能提升.

Condition类: 一个多线程间协调通讯的工具类,只有当该条件具备,等待线程才会被唤醒.条件是有用户自己判断与实现的,必须调用相应的方法来通知.

总结: 在只有少量线程竞争者时,使用synchronized是最好的实现, 但是如果线程增长的趋势是可以预估的,这时候reentrantLock是很好的实现,
除了synchronize, 其他锁有可能出现死锁.


 */