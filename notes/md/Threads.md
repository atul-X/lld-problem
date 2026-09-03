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
Performance in multithreading
	latency:- the time to completion of a task.Measured in time units.
	throughput:- The amount of tasks completed in a given period.Measured in tasks/Time unit.
Thread Pooling .

HyperThreading

		


	


