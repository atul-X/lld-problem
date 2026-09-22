why we need threads 
	responsiveness 
	preformance 
concurrency=multitasking

what threads are and where they live
single threaded application process
what the thread contains
	stack-region in memory ,where local variable are stored and passed into functions
	instruction pointer-address of the next instrucation to execute.
context switch
	stop thread 1
	schedule thread 1 out
	schedule thread 2 in
	start thread 2 
context switch cost 
context switching is not cheap and is the price of multitasking.

context switch key takeaways 
	too many threads -thrashing spending more time in management than real productive work.
	thread consume less resouces than processing
When to prefer MultiThreaded Architecuture
	Prefer if the tasks share a lot of data .
	thread are much faster to create and destory,
	switching between threads of the same process is faster.

Runnable interface 
thread class

Thread termination why and when?
	thread consume resources 
		memory and kernal resources
		cpu cycles and cache memory
	if a  thread finished its work, but the application still running we want to clean up the threads resources
	if misbehaving 
When can we interrupt a thread?
	if the thread is executing a method that throws an intrerruptedExceptuion.
	if the threads code is handing the interrupt signal explicitly 
	thread.interpt()
Daemon theads 
	Background threads that do not precent the application from exiting if the main thread terminates 

	Daemon threads -scenario1 
		1.background task that not block our application from terminationg.

		2.code in a worker thread is not under our control and we do not want it to block our application terminating.
Thread coordination
	Different threads run independently
	order of execution is out of our control

	dependency
		what if one thread depends on another threads
Thread.join()
	More control over independent threads
	safely collect and aggregates result
	gracefully handle runaway threads using Thread.join(timeout)

Performance in multithreading
	latency:- the time to completion of a task.Measured in time units.
	throughput:- The amount of tasks completed in a given period.Measured in tasks/Time unit.
Thread Pooling .

HyperThreading

stack
 	memory region where 
		Methods are called
		Arguments are passed 
		local variables are stored
	Stack +Instruction Pointer =State of each Threads execution 
Stack Properties
	All variables belong to the thread executing on that stack.
	Statiically allocated when the thread is  created.
	The stacks size is fixed and relatively small (platform specifc).
 	if our calling hierarchy is too deep we may get an stackOverflow Exception.(Risky with recurssion)

What is allocated on the Heap?
	objects( anthing created with the new operator)
		-String
		-Object
		-Collection
	Members of classes
	static variables
	Governed and managed by Garbage collector
	Objects stay as long as we have a reference to then.
	Members of classes-exist as long as their parent objects exist (same life cycles as their parents)
	Static variables -stay forever
Objects Vs References
	references 
		can be allocated on the stack;
		can be allocated on the heap if they are members of a class

	Objects
		Always allocated on the heap 
	Resource Sharing B?W Threads
		What is a resource?
			variables(integers,Strings..)
			Data Structure 
			File or connection handles
			Message or work queues
			Any Objects 

		Problem with shareing resource
Atomic Operation
	An Operation or a set of operations is Considered atomic if 
		it appears to the rest of the systen as if it occured at once 
	Single step ="all or nothing"
	No intermediate states 
	
	All reference assignments are atomic
	we can get and set references to objects atomically 
	all assignments to primitive types are safe except long and
double.
	Assignments to long and double if declared volatile.
	

Critical Section
	
	Synchronized-Monitor/Lock
		Locking mechanism
		Used to restrict access to a critical section or entire method to a single thread at a time .
	Synchronized-Monitor -> Synchronized key word on method if we use only one thread can excute that method at a time. these class level
	Synchronized-Lock -> 
		Object lockingObject=new Object();
		Synchronized block is rentrant
		A thread cannot prevent itself from entering a critical section.

Race Condition 	- when multiple threads are accessing a shared resource
	At least one thread is modifying the resource
	The Timing of threads scheduling may cause incorrect result.
	The core of the problem is non-atomic operations performed on the 
	resource.

Data Race 
	Compiler and CPU may execute the instructions out of order to 
	optime performance and utilization.
	They will do so while maintaining the logical correctness
	of the code.
	Out of Order Execution by the compiler and cpu are important
	features to speed up the code.

	The Compiler re-arrange instructions for better 
		Branch predication 	
		Vectorization -parallel instruction execution(SIMD)
		Prefetching instructions- better cache performance
	CPU re-arranges instructions for better hardware units 
	utilization.

Data race Consequences 
	May lead to unexpected ,paradoxical and incorrect results

Data Race -Solutins 
	Establish a Happens -before semantics by one of these 	methods
		Synchronization of methods which modify shared variables.
		Declaration of shared variables with the volatile keyword.
Locking statergy
	Fine-Grained Locking and Coarse-Gained Locking 

Fine-Grained Locking
	Fine-grained locking uses many specific locks to protect small, 
	distinct parts of a data structure or code block rather than one large lock.
How It Work
	High Parallelism: Multiple threads can access different parts of a shared 
	data structure at the same time.
	Reduced Contention: Threads do not wait in line if they work on separate sections 
	(like different buckets in a hash table or separate rows in a database).Example: 
	Instead of locking an entire hash table, you use a separate lock for each bucket or node.
Pros and Cons
	Pros: 
		Speeds up multi-threaded programs by allowing true concurrent execution.
	Cons: 
	Increases memory usage and CPU overhead from managing many locks.
	It also raises the risk of complex bugs like deadlocks if locks are not handled carefully.
Coarse-Gained Locking
	Coarse-grained locking is a concurrency 
	control strategy where a single, large lock 
	protects an entire data structure, a large 
	segment of code, or a group of related objects.

How It Works
	Single Protection Point:
		One lock guards multiple resources or a whole component 
		(such as an entire hash table or a customer record along with all their addresses).
	Simplicity: It is easy to design, implement, and reason about, minimizing the risk of complex 
		deadlocks or race conditions associated with managing many smaller locks.
	Low Overhead: The application spends minimal time acquiring and releasing 
		locks because there is only one global lock to manage.
The Drawbacks
	Reduced Concurrency: 
		Threads must wait for the single lock to become free, even if they want to access completely 
		independent parts of the data structure.
	Bottlenecks and Contention: Under high multi-threaded loads, 
		performance drops significantly as operations become serialized, 
		turning a multi-core system into an effective single-threaded queue.

deadlock
	A thread deadlock is a programming condition where two or more threads 
	are blocked forever because each is waiting for a resource or lock held by another.
Conditions for deadlock
	Mutual Exclusion :
		only one thread can have exclusive access to resources.
	Hold and wait 	
		at least one thread is hodling a resource and waiting for another resource.
	Non-preemptive allocation
		A resource is released only after the thread done using it.
	circular wait 
		A chain of atleast two threads each one is holding one resource and waiting for another resource.
Solution 
	Avoid circular wait enforce stric order in lock acquisition.

	






