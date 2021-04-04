PrimeFinder
=================================================

![Points](../../blob/badges/points.svg)

For this homework, you will modify the work queue implementation and use it to find primes. Specifically, you must:

  - Add a `pending` variable to the `WorkQueue` class to track unfinished work and implement a `finish()` method that waits until there is no more pending work. Use this new functionality in `PrimeFinder`; there should *not* be a `pending` variable or `TaskManager` nested class in the `PrimeFinder` class!

  - Implement the multithreaded `findPrimes(int, int, int)` method in `PrimeFinder` using your modified `WorkQueue` class. There should be 1 task or `Runnable` object for each number being tested and the `run()` method of these objects should use the `isPrime(int)` method.

## Hints ##

Below are some hints that may help with this homework assignment:

  - If you move tracking of the `pending` variable into the `WorkQueue` class properly, there is no need to create a nested `TaskManager` class within the `PrimeFinder` class. You will need to create an inner "task" class, however.

  - Do not try to use lambda expressions or streams here, since data must be mutated outside of a thread's scope.

These hints are *optional*. There may be multiple approaches to solving this homework.

## Requirements ##

See the Javadoc and `TODO` comments in the template code in the `src/main/java` directory for additional details. You must pass the tests provided in the `src/test/java` directory. Do not modify any of the files in the `src/test` directory.

See the [Homework Guides](https://usf-cs212-spring2021.github.io/guides/homework/) for additional details on homework requirements and submission.
