import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class TransferService {
    
    private final ConcurrentHashMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Transaction> history = new ConcurrentHashMap<>();

    public Transaction getTransactionById(String transactionId){
        return history.get(transactionId);
    }

    public List<Transaction> getTransactionsByAccount(long accountId) {
        // Your implementation
        List<Transaction> result = new ArrayList<>();
        for(Transaction tx : history.values()) {
            if(tx.getFromAccountId() == accountId || tx.getToAccountId()== accountId) {
                result.add(tx);
            }
        }
        return result;
    }
 
    public TransactionStatus transfer(Account from, Account to, long amount, String key) {

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

        IdempotencyRecord newRecord =  new IdempotencyRecord( id1, id2, amount, key);
        IdempotencyRecord activeRecord;
        IdempotencyRecord existing = records.putIfAbsent(key, newRecord);
          
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
            boolean claimed = existing.tryClaimRetry();

            if(!claimed){
                throw new IllegalStateException("Retry already Claimed");
            }
            
            history.get(key).updateStatus(TransactionStatus.IN_PROGRESS);

            activeRecord = existing;
            } else {
                throw new IllegalStateException("Unknown Transaction status");
            }
        }
        else {

            activeRecord = newRecord;
            Transaction transaction = new Transaction(key, id1, id2, amount, TransactionStatus.IN_PROGRESS, Instant.now());
            history.putIfAbsent(key, transaction);
        }

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
                        history.get(key).updateStatus(TransactionStatus.FAILED);
                        throw new IllegalArgumentException("Amount exceeds balance");
                    }
                    Math.addExact(bal2, amount);

                } catch (RuntimeException e){
                    if (activeRecord.getStatus() == TransactionStatus.IN_PROGRESS) {
                        activeRecord.updateStatus(TransactionStatus.FAILED);
                    }

                    Transaction tx = history.get(key);
                     if (tx != null &&
                        tx.getStatus() == TransactionStatus.IN_PROGRESS) {
                        tx.updateStatus(TransactionStatus.FAILED);
                    }

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
                history.get(key).updateStatus(TransactionStatus.FAILED);
                throw depositError;
            }
        }
        activeRecord.updateStatus(TransactionStatus.COMPLETED);
        history.get(key).updateStatus(TransactionStatus.COMPLETED);
        return TransactionStatus.COMPLETED;
    }
}
}
