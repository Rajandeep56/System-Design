import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
class TransferService {
    
    private final ConcurrentHashMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

    public TransactionStatus transfer(Account from, Account to, long amount, String key) {

        IdempotencyRecord newRecord = records.putIfAbsent(key, new IdempotencyRecord(key, id1, id2, amount, TransactionStatus.IN_PROGRESS));

        IdempotencyRecord existing = records.putIfAbsent(key, new IdempotencyRecord(key, id1, id2, amount, TransactionStatus.IN_PROGRESS));

        if(from == null || to == null) {
            throw new NullPointerException(" Account can not be null ");
        }

        int id1 = from.getAccountId();
        int id2 = to.getAccountId();
        
        if( id1==id2 ) {
            throw new IllegalArgumentException("Accounts are same");
        }

        if( amount <= 0 ) {
            throw new IllegalArgumentException("Amount is negative");
        }

        
                IdempotencyRecord activeRecord;
                // duplicate
                if (existing != null) {
                    boolean sameRequest = existing.getFromAccountId() == id1 && existing.getToAccountId() == id2 && existing.getAmount() == amount;
                    if(!sameRequest) {
                        throw new IllegalArgumentException(
                            "Idempotency key reused with diff request details."
                        );
                    }
                    TransactionStatus currentStatus = existing.getStatus();
                    if (currentStatus == TransactionStatus.IN_PROGRESS) {
                        throw new IllegalArgumentException("Transaction is Already underway. ");
                    }
                
                 if (currentStatus == TransactionStatus.FAILED) { 
                    // TODO: Implement retry claiming
                    boolean claimed = existing.tryClaimRetry();

                    if(!claimed){
                        throw new IllegalStateException("Retry already Claimed");
                    }

                    activeRecord = existing;
                    } else {
                        throw new IllegalStateException("Unknown Transaction status")
                    }
                }
                else {

                    activeRecord = newRecord;
                }
       
        // not atomic But what if another thread changes or freezes the receiving account between those two operations?
        // The sender could lose money while the receiver gets nothing.
        // Also, another thread could modify the sender's balance after your initial validation.
        // We need to lock both accounts throughout validation and the balance changes.

        Account first = id1 < id2 ? from : to;
        Account second = id1 < id2 ? to : from;
        
        synchronized (first) {
            synchronized (second) {
                try {
                    from.ensureActive();
                    to.ensureActive();

                    long bal1 = from.getBalance();
                    long bal2 = to.getBalance();

                    if ( bal1 < amount  ) {
                        activeRecord.updateStatus(TransactionStatus.FAILED);
                        throw new IllegalArgumentException("Amount exceeds balance");
                    }
                    Math.addExact(bal2, amount);

                } catch (RuntimeException e)
                    activeRecord.updateStatus(TransactionStatus.FAILED);
                    throw e;
                }

                from.withdraw(amount);
                try {
                    to.deposit(amount);
                } catch (RuntimeException depositError) {
                    try{
                        from.deposit(amount);
                    } catch (RuntimeException rollbackError){
                        depositError.addSuppressed(rollbackError);
                        throw new IllegalStateException(
                            "Transfer outcome uncertain; recovery required", depositError);
                }
                activeRecord.updateStatus(TransactionStatus.FAILED);
                throw depositError;
            }
        }
        activeRecord.updateStatus(TransactionStatus.COMPLETED);
        return TransactionStatus.COMPLETED;
    }
}
