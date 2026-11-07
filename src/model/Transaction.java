import java.time.Instant;
public class Transaction {
    private final String transactionId;
    private final long fromAccountId;
    private  final long toAccountId;
    private final long amount;
    private TransactionStatus status;
    private final Instant createdAt; 
    
    public Transaction(String trId, long aId, long bId, long amount, TransactionStatus status, Instant createdAt){
        this.transactionId=trId;
        this.fromAccountId=aId;
        this.toAccountId=bId;
        this.amount=amount;
        this.createdAt=createdAt;
        this.status=status;
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

    public synchronized TransactionStatus getStatus(){
        return status;
    }

    public Instant getCreatedAt(){
        return createdAt;
    }

    public synchronized void updateStatus (TransactionStatus newStatus) {
        // status is completed update it to completed
        // if status is failed update it to failed
        // maybe history isnt updated but actual thing is completed or left alone - atomic
         if (status == TransactionStatus.IN_PROGRESS && (newStatus == TransactionStatus.COMPLETED || newStatus == TransactionStatus.FAILED)) {
            status = newStatus;
        } else if (status == TransactionStatus.FAILED && newStatus == TransactionStatus.IN_PROGRESS){
               status = newStatus;
        }else{
            throw new IllegalStateException("Invalid State "+ status + newStatus);
        }
        
    }

}
