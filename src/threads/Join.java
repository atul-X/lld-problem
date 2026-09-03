package threads;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Join {
    public static void main(String[] args) throws InterruptedException {
        List<Long> inputNumber= Arrays.asList(1000000000L,3435L,2324L,4656L,23L,5556L);
        List<FactorialThread> threads=new ArrayList<>();
        for (Long in:inputNumber){
            threads.add(new FactorialThread(in));
        }
        for (Thread thread:threads){
            thread.setDaemon(true);
            thread.start();
        }
        for (Thread thread: threads){
            thread.join(20);
        }
        for (int i=0;i<inputNumber.size();i++) {
            FactorialThread factorialThread=threads.get(i);
            if (factorialThread.isFinished){
                System.out.println("Factorial of "+inputNumber.get(i)+" is "+factorialThread.getResult());
            }else {
                System.out.println("the calculations for "+inputNumber.get(i)+" is still in progress");
            }
        }
    }
    public static class FactorialThread extends  Thread{
        private long inputNumber;
        private BigInteger result=BigInteger.ZERO;
        private boolean isFinished=false;

        public FactorialThread(long inputNumber) {
            this.inputNumber = inputNumber;
        }

        @Override
        public void run(){
            this.result=factorial(inputNumber);
            this.isFinished=true;
        }
        public BigInteger factorial(long n){
            BigInteger tempResult=BigInteger.ONE;
            for (long i=n;i>0;i--){
                tempResult=tempResult.multiply(new BigInteger(Long.toString(i)));
            }
            return tempResult;
        }

        public BigInteger getResult() {
            return result;
        }

        public boolean isFinished() {
            return isFinished;
        }
    }

    public BigInteger calculateResult(BigInteger base1, BigInteger power1, BigInteger base2, BigInteger power2) {
        BigInteger result;
        /*
            Calculate result = ( base1 ^ power1 ) + (base2 ^ power2).
            Where each calculation in (..) is calculated on a different thread
        */
        PowerCalculatingThread thread1=new PowerCalculatingThread(base1,power1);
        PowerCalculatingThread thread2=new PowerCalculatingThread(base2,power2);
        try {
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        result=thread1.getResult().add(thread2.getResult());

        return result;
    }

    private static class PowerCalculatingThread extends Thread {
        private BigInteger result = BigInteger.ONE;
        private BigInteger base;
        private BigInteger power;

        public PowerCalculatingThread(BigInteger base, BigInteger power) {
            this.base = base;
            this.power = power;
        }

        @Override
        public void run() {
            this.result=pow(base,power);
        }
        public BigInteger pow(BigInteger base,BigInteger power){
            return base.pow(power.intValue());
        }

        public BigInteger getResult() { return result; }
    }
}
