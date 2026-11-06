
import java.util.concurrent.*;

public class TransferSimpleTest {

    public static void main(String[] args) throws Exception {

        // TODO: Create Account A with $1000
        Account A = new Account(1,1);
        A.deposit(100000);
        // TODO: Create Account B with $0
        Account B = new Account(2,2);
        TransferService service = new TransferService();
        // service.transfer(A, B, 20000, "tx-1");
        // service.transfer(A, B, 20000, "tx-2");
        // if ( A.getBalance() != 80000 || B.getBalance() != 20000){
        //     throw new AssertionError("Transfer Failed");
        // }
        // System.out.println("PASS");
        // concurrency test;
        Thread t1 = new Thread(()->{
            try {
                service.transfer(A, B, 70000, "tx-1");
            } catch (Exception e) {
                // TODO: handle exception
                System.out.println("T1" + e.getMessage());
            }
        });
        Thread t2 = new Thread(()->{
            try {
                service.transfer(A, B, 70000, "tx-2");
            } catch (Exception e) {
                // TODO: handle exception
                System.out.println("T1" + e.getMessage());
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("A: "+ A.getBalance());
        System.out.print("B "+ B.getBalance());
        
        if (A.getBalance() != 30000 || B.getBalance() != 70000) {
            throw new AssertionError("Concurrent transfer failed");
        }
    }
}
