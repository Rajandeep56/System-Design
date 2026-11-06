
import java.util.concurrent.*;

public class TransferConcurrencyTest {

    public static void main(String[] args) throws Exception {

        // TODO: Create Account A with $1000
        Account A = new Account(1,1);
        A.deposit(100000);
        // TODO: Create Account B with $0
        Account B = new Account(2,2);
        TransferService service = new TransferService();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        // TODO: Create two concurrent transfer tasks
        // transfer(Account from, Account to, long amount, String key) 

         Future<TransactionStatus> result1 = executor.submit( () -> {
            ready.countDown();
            start.await();
            return service.transfer(A, B, 70000, "test-1");
         });
        Future<TransactionStatus> result2 = executor.submit( () -> {
            ready.countDown();
            start.await();
            return service.transfer(A, B, 70000, "test-2");
         });
         ready.await();
         start.countDown();;
         result1.get();
         result2.get();
        executor.shutdown();
    }
}
