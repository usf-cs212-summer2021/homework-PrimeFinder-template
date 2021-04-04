import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Attempts to test if {@link PrimeFinder#findPrimes(int, int)} and
 * {@link WorkQueue#finish()} implementations are correct. Tests are not perfect
 * and may not catch all implementation issues.
 *
 * @see PrimeFinder#findPrimes(int, int)
 * @see WorkQueue#finish()
 * 
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Spring 2021
 */
@TestMethodOrder(MethodName.class)
public class PrimeFinderTest {
	/** Maximum amount of time to wait per test. */
	public static final Duration GLOBAL_TIMEOUT = Duration.ofSeconds(60);

	/** Number of warmup rounds to run when benchmarking. */
	public static final int WARMUP_ROUNDS = 10;

	/** Number of timed rounds to run when benchmarking. */
	public static final int TIMED_ROUNDS = 20;

	/**
	 * Hard-coded set of known primes to compare against.
	 */
	public static final Set<Integer> KNOWN_PRIMES = Set.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59,
			61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179,
			181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311,
			313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449,
			457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601,
			607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751,
			757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911,
			919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997);

	/**
	 * Tests the results are consistently correct for different numbers of threads.
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class A_ThreadTests {
		/**
		 * Verify the multithreaded implementation finds the correct primes with one
		 * worker thread.
		 *
		 * @see PrimeFinder#findPrimes(int, int)
		 */
		@RepeatedTest(3)
		@Order(1)
		public void testFindPrimes1Thread() {
			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				TreeSet<Integer> actual = PrimeFinder.findPrimes(1000, 1);
				Assertions.assertEquals(KNOWN_PRIMES, actual);
			});
		}

		/**
		 * Verify the multithreaded implementation finds the correct primes with two
		 * worker threads.
		 *
		 * @see PrimeFinder#findPrimes(int, int)
		 */
		@RepeatedTest(3)
		@Order(2)
		public void testFindPrimes2Thread() {
			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				TreeSet<Integer> actual = PrimeFinder.findPrimes(1000, 2);
				Assertions.assertEquals(KNOWN_PRIMES, actual);
			});
		}

		/**
		 * Verify the multithreaded implementation finds the correct primes with five
		 * worker threads.
		 *
		 * @see PrimeFinder#findPrimes(int, int)
		 */
		@RepeatedTest(3)
		@Order(3)
		public void testFindPrimes5Thread() {
			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				TreeSet<Integer> actual = PrimeFinder.findPrimes(1000, 5);
				Assertions.assertEquals(KNOWN_PRIMES, actual);
			});
		}
	}

	/**
	 * Tests the results are correct for single versus multithreaded approaches.
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class B_ResultsTests {
		/**
		 * Verify the single-threaded implementation also passes the tests
		 *
		 * @see PrimeFinder#trialDivision(int)
		 */
		@Test
		@Order(1)
		public void testTrialDivision() {
			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				TreeSet<Integer> actual = PrimeFinder.trialDivision(1000);
				Assertions.assertEquals(KNOWN_PRIMES, actual);
			});
		}

		/**
		 * Test single and multithreaded results return the same results.
		 *
		 * @see PrimeFinder#findPrimes(int, int)
		 */
		@Test
		@Order(2)
		public void testSingleVersusMulti() {
			int max = 3000;
			int threads = 5;

			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				TreeSet<Integer> expected = PrimeFinder.trialDivision(max);
				TreeSet<Integer> actual = PrimeFinder.findPrimes(max, threads);

				Assertions.assertEquals(expected, actual);
			});
		}
	}

	/**
	 * Benchmarks the multithreading code.
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class C_SingleVersusMultiBenchmarks {
		/**
		 * Verifies multithreading is faster than single threading for a large maximum
		 * value.
		 */
		@Test
		@Order(1)
		public void benchmarkSingleVersusMulti() {
			// TURN OFF LOGGING BEFORE RUNNING THIS!
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.OFF);

			int max = 5000;
			int threads = 5;

			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				double single = new SingleBenchmarker().benchmark(max);
				double multi = new MultiBenchmarker(threads).benchmark(max);

				String debug = String.format("  Single: \t%.4f, \t    Multi: \t%.4f, \tSpeedup: \t%.4fx", single, multi, single / multi);

				System.out.println(debug);
				Assertions.assertTrue(single >= multi, debug);
			});
		}
	}

	/**
	 * Benchmarks the multithreading code.
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class D_OneVersusThreeBenchmarks {
		/**
		 * Verifies having one worker thread is faster than three worker threads.
		 */
		@Test
		@Order(7)
		public void benchmarkOneVersusThree() {
			// TURN OFF LOGGING BEFORE RUNNING THIS!
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.OFF);

			int max = 5000;

			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				double multi1 = new MultiBenchmarker(1).benchmark(max);
				double multi3 = new MultiBenchmarker(3).benchmark(max);

				String debug = String.format("1 Thread: \t%.4f, \t3 Threads: \t%.4f, \tSpeedup: \t%.4fx", multi1, multi3, multi1 / multi3);

				System.out.println(debug);
				Assertions.assertTrue(multi1 > multi3, debug);
			});
		}
	}

	/**
	 * Tests the work queue code.
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class E_WorkQueueTests {
		/**
		 * Verifies the work queue functions as expected.
		 */
		@Test
		@Order(1)
		public void testWorkQueue() {
			int tasks = 10;
			int sleep = 10;
			int workers = tasks / 2;

			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				WorkQueue queue = new WorkQueue(workers);
				CountDownLatch count = new CountDownLatch(tasks);

				for (int i = 0; i < tasks; i++) {
					queue.execute(new Runnable() {

						@Override
						public void run() {
							try {
								Thread.sleep(sleep);
								count.countDown();
							}
							catch (InterruptedException ex) {
								Assertions.fail("Task interrupted; queue did not complete in time.");
							}
						}
					});
				}

				// if you get stuck here then finish() isn't working
				queue.finish();
				queue.shutdown();
				queue.join();

				count.await();
			});
		}

		/**
		 * Verifies the worker threads are shutdown. If not, more worker threads
		 * will be active after the {@link PrimeFinder#findPrimes(int, int)} call.
		 *
		 * @throws InterruptedException if unable to sleep
		 */
		@Test
		@Order(2)
		public void testShutdown() throws InterruptedException {
			Assertions.assertTimeoutPreemptively(GLOBAL_TIMEOUT, () -> {
				List<String> start = activeThreads();

				PrimeFinder.findPrimes(1000, 3);
				Thread.sleep(500); // short pause for threads to shutdown (not necessary if joining properly)

				List<String> end = activeThreads();

				System.out.println("Threads at Start: " + start);
				System.out.println("Threads at End: " + end);

				Assertions.assertEquals(start, end);
			});
		}
	}

	/**
	 * Poor attempts to verify the approach is correct.
	 */
	@Nested
	@TestMethodOrder(OrderAnnotation.class)
	public class F_ApproachTests {
		/**
		 * Tests that the java.lang.Thread class does not appear in the implementation
		 * code.
		 *
		 * @throws IOException if unable to read source code
		 */
		@Test
		@Order(1)
		public void testThreadClass() throws IOException {
			Path path = Path.of("src", "main", "java", "PrimeFinder.java");
			String source = Files.readString(path, StandardCharsets.UTF_8);
			Assertions.assertFalse(source.matches("(?is).*\\bextends\\s+Thread\\b.*"));
		}

		/**
		 * Tests that the pending variable is not used in the implementation code.
		 *
		 * @throws IOException if unable to read source code
		 */
		@Test
		@Order(2)
		public void testPending() throws IOException {
			Path path = Path.of("src", "main", "java", "PrimeFinder.java");
			String source = Files.readString(path, StandardCharsets.UTF_8);
			Assertions.assertFalse(source.contains("incrementPending"));
			Assertions.assertFalse(source.contains("decrementPending"));
			Assertions.assertFalse(source.contains("int pending"));
		}

		/**
		 * Tests that the TaskManager class is not used in the implementation code.
		 *
		 * @throws IOException if unable to read source code
		 */
		@Test
		@Order(3)
		public void testTaskManager() throws IOException {
			Path path = Path.of("src", "main", "java", "PrimeFinder.java");
			String source = Files.readString(path, StandardCharsets.UTF_8);
			Assertions.assertFalse(source.contains("TaskManager"));
		}
	}

	/**
	 * Returns a list of the active thread names (approximate).
	 *
	 * @return list of active thread names
	 */
	public static List<String> activeThreads() {
		int active = Thread.activeCount(); // only an estimate
		Thread[] threads = new Thread[active * 2]; // make sure large enough
		Thread.enumerate(threads);
		return Arrays.stream(threads)
				// remove null values
				.filter(thread -> thread != null)
				// only keep the thread name
				.map(Thread::getName)
				// remove threads used internally by junit and surefire
				.filter(name -> !name.startsWith("junit") && !name.startsWith("surefire"))
				.collect(Collectors.toList());
	}

	/**
	 * Used to benchmark code. Benchmarking results may be inconsistent, and are
	 * written to favor multithreading.
	 */
	private static abstract class Benchmarker {
		/**
		 * Method that returns a set of primes.
		 *
		 * @param max the maximum size to use
		 * @return set of primes
		 */
		public abstract Set<Integer> run(int max);

		/**
		 * Benchmarks the run method up to the max provided.
		 *
		 * @param max the maximum size to use
		 * @return average runtime
		 */
		public double benchmark(int max) {
			Integer first = Integer.MIN_VALUE;

			// warmup
			for (int i = 0; i < WARMUP_ROUNDS; i++) {
				Set<Integer> results = run(max);
				first = Math.max(first, results.iterator().next());
			}

			// timed
			Instant start = Instant.now();

			for (int i = 0; i < TIMED_ROUNDS; i++) {
				Set<Integer> results = run(max);
				first = Math.max(first, results.iterator().next());
			}

			Instant end = Instant.now();

			if (!KNOWN_PRIMES.contains(first)) {
				Assertions.fail("Unexpected test result. Make sure results are correct before running this test.");
			}

			// averaged result
			Duration elapsed = Duration.between(start, end);
			return (double) elapsed.toMillis() / TIMED_ROUNDS;
		}
	}

	/**
	 * Used to benchmark single threaded code.
	 */
	private static class SingleBenchmarker extends Benchmarker {
		@Override
		public Set<Integer> run(int max) {
			return PrimeFinder.trialDivision(max);
		}
	}

	/**
	 * Used to benchmark multithreaded code.
	 */
	private static class MultiBenchmarker extends Benchmarker {
		/** Number of threads to use. */
		private final int threads;

		/**
		 * Initializes the number of threads.
		 *
		 * @param threads the number of threads to use
		 */
		public MultiBenchmarker(int threads) {
			this.threads = threads;
		}

		@Override
		public Set<Integer> run(int max) {
			try {
				return PrimeFinder.findPrimes(max, threads);
			}
			catch (IllegalArgumentException e) {
				Assertions.fail("Unexpected exception.");
				return null;
			}
		}
	}
}
