import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple work queue implementation based on the IBM developerWorks article by
 * Brian Goetz. It is up to the user of this class to keep track of whether
 * there is any pending work remaining.
 *
 * @see <a href="https://www.ibm.com/developerworks/library/j-jtp0730/">
 * Java Theory and Practice: Thread Pools and Work Queues</a>
 * 
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Summer 2021
 */
public class WorkQueue {
	// TODO Modify existing methods if necessary.
	// TODO Add new members and methods if necessary.
	
	/**
	 * Pool of worker threads that will wait in the background until work is
	 * available.
	 */
	private final Worker[] workers;

	/** Queue of pending work requests. */
	private final LinkedList<Runnable> queue;

	/** Used to signal the queue should be shutdown. */
	private volatile boolean shutdown;

	/** The default number of threads to use when not specified. */
	public static final int DEFAULT = 5;

	/** Logger used for this class. */
	private static final Logger log = LogManager.getLogger();
	
	/**
	 * Starts a work queue with the default number of threads.
	 *
	 * @see #WorkQueue(int)
	 */
	public WorkQueue() {
		this(DEFAULT);
	}

	/**
	 * Starts a work queue with the specified number of threads.
	 *
	 * @param threads number of worker threads; should be greater than 1
	 */
	public WorkQueue(int threads) {
		this.queue = new LinkedList<Runnable>();
		this.workers = new Worker[threads];
		this.shutdown = false;

		// start the threads so they are waiting in the background
		for (int i = 0; i < threads; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
		
		log.debug("Work queue initialized with {} worker threads.", workers.length);
	}

	/**
	 * Adds a work request to the queue. A thread will process this request when
	 * available.
	 *
	 * @param task work request (in the form of a {@link Runnable} object)
	 */
	public void execute(Runnable task) {
		synchronized (queue) {
			queue.addLast(task);
			queue.notifyAll();
		}
	}
	
	/**
	 * Waits for all pending work to be finished. Does not terminate the worker
	 * threads so that the work queue can continue to be used.
	 */
	public void finish() {
		// TODO Fix this method implementation.
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	/**
	 * Asks the queue to shutdown. Any unprocessed work will not be finished, but
	 * threads in-progress will not be interrupted.
	 */
	public void shutdown() {
		// safe to do unsynchronized due to volatile keyword
		shutdown = true;

		log.debug("Work queue triggering shutdown...");
		synchronized (queue) {
			queue.notifyAll();
		}
	}
	
	/**
	 * Similar to {@link Thread#join()}, waits for all the work to be finished
	 * and the worker threads to terminate. The work queue cannot be reused after
	 * this call completes.
	 */
	public void join() {
		try {
			finish();
			shutdown();

			for (Worker worker : workers) {
				worker.join();
			}
			
			log.debug("All worker threads terminated.");
		}
		catch (InterruptedException e) {
			System.err.println("Warning: Work queue interrupted while joining.");
			log.catching(Level.DEBUG, e);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Returns the number of worker threads being used by the work queue.
	 *
	 * @return number of worker threads
	 */
	public int size() {
		return workers.length;
	}

	/**
	 * Waits until work is available in the work queue. When work is found, will
	 * remove the work from the queue and run it. If a shutdown is detected, will
	 * exit instead of grabbing new work from the queue. These threads will
	 * continue running in the background until a shutdown is requested.
	 */
	private class Worker extends Thread {
		/**
		 * Initializes a worker thread with a custom name.
		 */
		public Worker() {
			setName("Worker" + getName());
		}

		@Override
		public void run() {
			Runnable task = null;

			try {
				while (true) {
					synchronized (queue) {
						while (queue.isEmpty() && !shutdown) {
							log.debug("Work queue worker waiting...");
							queue.wait();
						}

						// exit while for one of two reasons:
						// (a) queue has work, or (b) shutdown has been called
	
						if (shutdown) {
							log.debug("Worker detected shutdown...");
							break;
						}
						else {
							task = queue.removeFirst();
						}
					}

					try {
						log.debug("Work queue worker found work.");
						task.run();
					}
					catch (RuntimeException e) {
						// catch runtime exceptions to avoid leaking threads
						System.err.println("Warning: Work queue encountered an exception while running.");
						log.catching(Level.DEBUG, e);
					}
				}
				
				log.debug("Worker thread terminating...");
			}
			catch (InterruptedException e) {
				System.err.println("Warning: Worker thread interrupted while waiting.");
				log.catching(Level.DEBUG, e);
				Thread.currentThread().interrupt();
			}
		}
	}
	
	// TODO Remove main method when done testing!
	/**
	 * Demonstrates this class.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		int threads = 3;
		
		Supplier<String> activeThreads = () -> {
			Thread[] found = new Thread[Thread.activeCount() * 2];
			Thread.enumerate(found);
			return Arrays.stream(found).filter(t -> t != null).map(Thread::getName).collect(Collectors.joining(", "));
		};
		
		// demonstrates the workers in the background
		WorkQueue demo = new WorkQueue(threads);
		
		// do a bit of work in the background
		demo.execute(() -> {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		// finish, but do not shutdown
		demo.finish();
		
		// peek at the threads active in the system
		System.out.println("Estimated active threads before join(): " + activeThreads.get());
		
		// trigger threads to shutdown
		demo.join();
		
		// peek at the threads after shutdown and join
		System.out.println("Estimated active threads after join(): " + activeThreads.get());
		
		/*
		 * The thread named "main" runs the main method.
		 * 
		 * If you see any thread names starting with "ForkJoinPool", those
		 * are used internally. They could come from Log4j2 or JUnit.
		 * 
		 * If you see any thread names starting with "WorkerThread", those
		 * are likely your work queue threads.
		 * 
		 * If you see your work queue threads AFTER the join() call, something
		 * is not quite working with the finish and/or shutdown calls.
		 */
	}
}
