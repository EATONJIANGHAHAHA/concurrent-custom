package com.mmall.concurrency.example.basicTheory;

/*
一: CPU多级缓存:
数据的读取和存储都经过高速缓存，CPU核心与高速缓存有一条特殊的快速通道；主存与高速缓存都连在系统总线上（BUS）这条总线还用于其他组件的通信
在高速缓存出现后不久，系统变得越来越复杂，高速缓存与主存之间的速度差异被拉大，直到加入了另一级缓存，新加入的这级缓存比第一缓存更大，并且更慢，而且经济上不合适，所以有了二级缓存，甚至是三级缓存

CPU的频率太快了，快到主存跟不上，这样在处理器时钟周期内，CPU常常需要等待主存，浪费资源，
所以cache的出现，是为了缓解CPU和内存之间速度的不匹配问题（结构：cpu->cache->memort）
1.时间局部性：如果某个数据被访问，那么在不久的将来他很可能被再次访问
2.空间局部性：如果某个数据被访问，那么与他相邻的数据很快也可能被访问

缓存一致性:
M: Modified 修改，指的是该缓存行只被缓存在该CPU的缓存中，并且是被修改过的，因此他与主存中的数据是不一致的，
      该缓存行中的数据需要在未来的某个时间点（允许其他CPU读取主存相应中的内容之前）写回主存，然后状态变成E（独享）
  E：Exclusive 独享 缓存行只被缓存在该CPU的缓存中，是未被修改过的，与主存的数据是一致的，可以在任何时刻当有其他CPU读取该内存时，变成S（共享）状态，当CPU修改该缓存行的内容时，变成M（被修改）的状态
  S：Share 共享，意味着该缓存行可能会被多个CPU进行缓存，并且该缓存中的数据与主存数据是一致的，当有一个CPU修改该缓存行时，其他CPU是可以被作废的，变成I(无效的)
  I：Invalid 无效的，代表这个缓存是无效的，可能是有其他CPU修改了该缓存行

在一个典型的多核系统中，每一个核都会有自己的缓存来共享主存总线，每一个CPU会发出读写（I/O）请求，而缓存的目的是为了减少CPU读写共享主存的次数；
一个缓存除了在Invaild状态，都可以满足CPU 的读请求

一个写请求只有在M状态，或者E状态的时候才能给被执行，如果是处在S状态的时候，他必须先将该缓存行变成I状态，
这个操作通常作用于广播的方式来完成，这个时候他既不允许不同的CPU同时修改同一个缓存行，即使是修改同一个缓存行中不同端的数据也是不可以的，这里主要解决的是缓存一致性的问题，
一个M状态的缓存行必须时刻监听所有试图读该缓存行相对主存的操作，这种操作必须在缓存该缓存行被写会到主存，并将状态变成S状态之前，被延迟执行

一个处于S状态的缓存行，也必须监听其他缓存使该缓存行无效，或者独享该缓存行的请求，并将缓存行变成无效

一个处于E状态的缓存行，他要监听其他缓存读缓存行的操作，一旦有，那么他讲变成S状态

因此对于M和E状态，他们的数据总是一致的与缓存行的真正状态总是保持一致的，
但是S状态可能是非一致的，如果一个缓存将处于S状态的 缓存行作废了，另一个缓存可能已经独享了该缓存行，
但是该缓存缺不会讲该缓存行升迁为E状态，这是因为其他缓存不会广播他们已经作废掉该缓存行的通知，
同样由于缓存并没有保存该缓存行被COPY的数量，因此没有办法确定是否独享了改缓存行，
这是一种投机性的优化，因为如果一个CPU想修改一个处于S状态的缓存行，总线需要将所有使用该缓存行的COPY的值变成Invaild状态才可以，而修改E状态的缓存 却不需要这样做

二: CPU多级缓存的乱序执行优化
处理器为提高运算速度而做出违背代码原有执行顺序的优化

导致的一个问题，如果我们不做任何处理，在多核的情况下，
的实际结果可能和逻辑运行结果大不相同，如果在一个核上执行数据写入操作，
并在最后执行一个操作来标记数据已经写入好了，而在另外一个核上通过该标记位判定数据是否已经写入，这时候就可能出现不一致，标记位先被写入，
但是实际的操作缺并未完成，这个未完成既有可能是没有计算完成，也有可能是缓存没有被及时刷新到主存之中，使得其他核读到了错误的数据

三: Java内存模型

JAVA内存模型规范：
1.规定了一个线程如何和何时可以看到其他线程修改过后的共享变量的值
2.如何以及何时同步的访问共享变量

Heap(堆)：java里的堆是一个运行时的数据区，堆是由垃圾回收来负责的，
         堆的优势是可以动态的分配内存大小，生存期也不必事先告诉编译器，
         因为他是在运行时动态分配内存的，java的垃圾回收器会定时收走不用的数据，
         缺点是由于要在运行时动态分配，所有存取速度可能会慢一些
Stack(栈)：栈的优势是存取速度比堆要快，仅次于计算机里的寄存器，栈的数据是可以共享的，
          缺点是存在栈中的数据的大小与生存期必须是确定的，缺乏一些灵活性
          栈中主要存放一些基本类型的变量，比如int，short，long，byte，double，float，boolean，char，对象句柄，

java内存模型要求调用栈和本地内存变量存放在线程栈（Thread Stack）上，对象存放在堆上。
一个本地变量可能存放一个对象的引用，这时引用变量存放在本地栈上，但是对象本身存放在堆上
成员变量跟随着对象存放在堆上，而不管是原始类型还是引用类型，静态成员变量跟随着类的定义一起存在在堆上

存在堆上的对象，可以被持有这个对象的引用的线程访问
如果两个线程同时访问同一个对象的私有变量，这时他们获得的是这个对象的私有拷贝

CPU：一个计算机一般有多个CPU，一个CPU还会有多核
CPU Registers（寄存器）：每个CPU都包含一系列的寄存器，他们是CPU内存的基础，CPU在寄存器上执行的速度远大于在主存上执行的速度。
CPU Cache（高速缓存）：由于计算机的存储设备与处理器的处理设备有着几个数量级的差距，
                    所以现代计算机都会加入一层读写速度与处理器处理速度接近想通的高级缓存来作为内存与处理器之间的缓冲，
                    将运算使用到的数据复制到缓存中，让运算能够快速的执行，当运算结束后，再从缓存同步到内存之中，这样，CPU就不需要等待缓慢的内存读写了
主（内）存：一个计算机包含一个主存，所有的CPU都可以访问主存，主存比缓存容量大的多

运作原理：通常情况下，当一个CPU要读取主存的时候，他会将主存中的数据读取到CPU缓存中，甚至将缓存中的内容读到内部寄存器里面，然后再寄存器执行操作，
当运行结束后，会将寄存器中的值刷新回缓存中，并在某个时间点刷新回主存

所有线程栈和堆会被保存在缓存里面，部分可能会出现在CPU缓存中和CPU内部的寄存器里面

每个线程之间共享变量都存放在主内存里面，每个线程都有一个私有的本地内存
本地内存是java内存模型中抽象的概念，并不是真实存在的（他涵盖了缓存写缓冲区。寄存器，以及其他硬件的优化）
本地内存中存储了以读或者写共享变量的拷贝的一个副本

从一个更低的层次来说，线程本地内存，他是cpu缓存，寄存器的一个抽象描述，而JVM的静态内存存储模型，
他只是一种对内存模型的物理划分而已，只局限在内存，而且只局限在JVM的内存

如果线程A和线程B要通信，必须经历两个过程：
1、A将本地内存变量刷新到主内存
2、B从主内存中读取变量

1.lock（锁定）：作用于主内存的变量，把一个变量标识变为一条线程独占状态
2.unlock（解锁）：作用于主内存的变量，把一个处于锁定状态的变量释放出来，释放后的变量才可以被其他线程锁定
3.read（读取）：作用于主内存的变量，把一个变量值从主内存传输到线程的工作内存中，以便随后的load动作使用
4.load（载入）：作用于工作内存的变量，它把read操作从主内存中得到的变量值放入工作内存的变量副本中
5.use（使用）：作用于工作内存的变量，把工作内存中的一个变量值传递给执行引擎
6.assign（赋值）：作用于工作内存的变量，它把一个从执行引擎接受到的值赋值给工作内存的变量
7.store（存储）：作用于工作内存的变量，把工作内存中的一个变量的值传送到主内存中，以便随后的write的操作
8.write（写入）：作用于主内存的变量，它把store操作从工作内存中一个变量的值传送到主内存的变量中

同步规则：

1.如果要把一个变量从主内存中赋值到工作内存，就需要按顺序得执行read和load操作，如果把变量从工作内存中同步回主内存中，就要按顺序得执行store和write操作，但java内存模型只要求上述操作必须按顺序执行，没有保证必须是连续执行
2.不允许read和load、store和write操作之一单独出现
3.不允许一个线程丢弃他的最近assign的操作，即变量在工作内存中改变了之后必须同步到主内存中
4.不允许一个线程无原因地（没有发生过任何assign操作）把数据从工作内存同步到主内存中
5.一个新的变量只能在主内存中诞生，不允许在工作内存中直接使用一个未被初始化（load或assign）的变量。即就是对一个变量实施use和store操作之前，必须先执行过了load和assign操作
6.一个变量在同一时刻只允许一条线程对其进行lock操作，但lock操作可以同时被一条线程重复执行多次，多次执行lock后，只有执行相同次数的unlock操作，变量才会解锁，lock和unlock必须成对出现
7.如果一个变量执行lock操作，将会清空工作内存中此变量的值，在执行引擎中使用这个变量前需要重新执行load或assign操作初始化变量的值
8.如果一个变量事先没有被lock操作锁定，则不允许他执行unlock操作，也不允许去unlock一个被其他线程锁定的变量
9.对一个变量执行unlock操作之前，必须先把此变量同步到主内存中（执行store和write操作）

 */