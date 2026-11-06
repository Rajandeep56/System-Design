import java.time.Instant;
public class Transaction {
    private final String transactionId;
    private final long fromAccountId;
    private  final long toAccountId;
    private final long amount;
    private TransactionStatus status;
    private final Instant createdAt; 
    
    Transaction(String trId, long aId, long bId, long amount, TransactionStatus status, Instant createdAt){
        this.transactionId=trId;
        this.fromAccountId=aId;
        this.toAccountId=bId;
        this.amount=amount;
        this.createdAt=createdAt;
    }

    public long getAmount(){
        return amount;
    }
    public String getTransactionId(){
        return transactionId;
    }
    public long getFromAccountId(){
        return fromAccountId;
    }
    public long getToAccountId(){
        return toAccountId;
    }
    public TransactionStatus getStatus(){
        return status;
    }
    public Instant getCreatedAt(){
        return createdAt;
    }

}
