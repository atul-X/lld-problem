import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class Test {
    class Base {
        synchronized void a() {
            b();
        }
        synchronized void b() {

        }
    }
    class Child extends Base {
        synchronized void b() {
            super.b();
        }
    }


     abstract class Ride{
        abstract long fare();
        public Ride start(String type,int km,boolean shared,boolean night){
            switch (type){
                case "bike":
                    return new bike(km,shared,night);
                default:
                    throw new IllegalStateException();
            }
        }
    }
    class bike extends Ride{
        private int km;
        private boolean shared;
        private boolean night;

        public bike(int km, boolean shared, boolean night) {
            this.km = km;
            this.shared = shared;
            this.night = night;
        }

        @Override
        long fare() {
            return 10+5*km;
        }
    }
    class Foo {
        Semaphore r2;
        Semaphore r3;

        public Foo() {
            r2 = new Semaphore(0);
            r3 = new Semaphore(0);
        }

        public void first(Runnable printFirst) throws InterruptedException {

            // printFirst.run() outputs "first". Do not change or remove this line.
            printFirst.run();
            r2.release();
        }

        public synchronized void second(Runnable printSecond) throws InterruptedException {

            // printSecond.run() outputs "second". Do not change or remove this line.
            r2.acquire();
            printSecond.run();
            r3.release();
        }

        public void third(Runnable printThird) throws InterruptedException {

            // printThird.run() outputs "third". Do not change or remove this line.
            r3.acquire();
            printThird.run();
        }
    }
}
