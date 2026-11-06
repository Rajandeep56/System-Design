
import java.util.concurrent.*;

public class TransferSimpleTest {

    public static void main(String[] args) throws Exception {

        Account A = new Account(1,1);
        A.deposit(100000);
        Account B = new Account(2,2);
        TransferService service = new TransferService();
        testInsufficientFunds(A, B, service);
        
    }

    public static void testInsufficientFunds(Account a, Account b, TransferService service){
        try {
            service.transfer(a, b, 150000, "txt-1");
        } catch (Exception e) {
             System.out.println( e.getMessage());
        }
        System.out.println("A: "+ a.getBalance());
        System.out.print("B "+ b.getBalance());
        
        if (a.getBalance() != 100000 || b.getBalance() != 0) {
            throw new AssertionError("Insufficient funds test failed");
        }
    }
}
