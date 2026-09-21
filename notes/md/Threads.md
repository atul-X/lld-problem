why we need threads 
	responsiveness 
	preformance 
concurrency=multitasking

what threads are and where they live
single threaded application process
what the thread contains
	stack-region in memory ,where local variable are stored and passed into functions
	instruction pointer- Address of the next instrucation to execute.
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
	Members of classes- exist as long as their parent objects exist (same life cycles as their parents)
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
	



Critical Section
	
	Synchronized-Monitor/Lock
		Locking mechanism
		Used to restrict access to a critical section or entire method to a single thread at a time .
	Synchronized-Monitor -> Synchronized key word on method if we use only one thread can excute that method at a time. these class level
	Synchronized-Lock -> 
		Object lockingObject=new Object();
		Synchronized block is rentrant
		A thread cannot prevent itself from entering a critical section.





















