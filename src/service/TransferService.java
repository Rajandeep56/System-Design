import java.util.List;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class TransferService {
    
    private final ConcurrentHashMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Transaction> history = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Transaction>> accountHistory = new ConcurrentHashMap<>();

    public Transaction getTransactionById(String transactionId){
        return history.get(transactionId);
    }

    
    public List<Transaction> getTransactionsByAccount(long accountId) {
        // Your implementation
            List<Transaction> transactions = accountHistory.get(accountId);

        if (transactions == null) {
        return new ArrayList<>();
        }
            return new ArrayList<>(transactions);

    }
    
    private void indexTransaction(Transaction tx) {
        // Add tx to sender's account history
        // Add tx to receiver's account history
         long fromId = tx.getFromAccountId();
            long toId = tx.getToAccountId();
            accountHistory.computeIfAbsent(
        fromId,
        id -> new CopyOnWriteArrayList<>()
        ).add(tx);
        accountHistory.computeIfAbsent(
        toId,
            id -> new CopyOnWriteArrayList<>()
        ).add(tx);
    }
    
    public List<Transaction> getTransactionsByStatus( long accountId, TransactionStatus status) {
            List<Transaction> result = new ArrayList<>();
            for (Transaction tx : getTransactionsByAccount(accountId)) {
            if (tx.getStatus() == status) {
                result.add(tx);
            }
        }

        return result;
    }

    public List<Transaction> getTransactionsByDateRange(long accountId, Instant start, Instant end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("Invalid date range");
        }
        List<Transaction> result = new ArrayList<>();
         for (Transaction tx : getTransactionsByAccount(accountId)) {
            Instant createdAt = tx.getCreatedAt();
            if (!createdAt.isBefore(start) && createdAt.isBefore(end)) {
                result.add(tx);
            }
         }

        result.sort(
            Comparator.comparing(Transaction::getCreatedAt).reversed()
        );
        return result;

    }

    public List<Transaction> getTransactionPage( long accountId, int page, int pageSize){
            if (page < 0 || pageSize <= 0 || pageSize > 100) {
                throw new IllegalArgumentException("Invalid pagination");
            }
            List<Transaction> transactions = getTransactionsByAccount(accountId);
             transactions.sort(
            Comparator.comparing(Transaction::getCreatedAt)
                    .reversed()
                    .thenComparing(Transaction::getTransactionId)
            );
            long offset = (long) page * pageSize;
            if (offset >= transactions.size()) {
                return new ArrayList<>();
             }
            int start = (int) offset;
            int end = (int) Math.min(offset + pageSize, transactions.size());

            return new ArrayList<>(transactions.subList(start, end));

    }
    public AccountSummary getAccountSummary(long accountId) {

    long incoming = 0;
    long outgoing = 0;
    int count = 0;

    for (Transaction tx : getTransactionsByAccount(accountId)) {

        if (tx.getStatus() != TransactionStatus.COMPLETED) {
            continue;
        }

        if (tx.getFromAccountId() == accountId) {
            outgoing = Math.addExact(outgoing, tx.getAmount());
        }

        if (tx.getToAccountId() == accountId) {
            incoming = Math.addExact(incoming, tx.getAmount());
        }

        count++;
    }

        return new AccountSummary(incoming, outgoing, count);
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
            if (currentStatus == TransactionStatus.COMPLETED) {
                return TransactionStatus.COMPLETED;
            }
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
            indexTransaction(transaction);
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
                        activeRecord.updateStatus(TransactionStatus.RECOVERY_REQUIRED);
                        history.get(key).updateStatus(TransactionStatus.RECOVERY_REQUIRED);
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
