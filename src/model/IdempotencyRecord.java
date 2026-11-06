
class IdempotencyRecord {
    private final  String key;
    private final  int  fromAccountId;
    private final int toAccountId;
    private final long amount;
    private volatile TransactionStatus status;

    public IdempotencyRecord(int fromAccountId, int toAccountId, long amount, String key) {
        this.key = key;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = TransactionStatus.IN_PROGRESS;
    }
    public int getFromAccountId(){
        return this.fromAccountId;
    }
    public int getToAccountId(){
        return this.toAccountId;
    }
    public long getAmount(){
        return this.amount;
    }
    public TransactionStatus getStatus(){
        return this.status;
    }

    public synchronized boolean updateStatus( TransactionStatus newStatus){
        if(status != TransactionStatus.IN_PROGRESS){
            return false;
        }
        if(newStatus != TransactionStatus.COMPLETED && newStatus != TransactionStatus.FAILED){
            return false;
        }
        status = newStatus;
        return true;
    }

    public synchronized boolean tryClaimRetry() {
        if(this.status != TransactionStatus.FAILED){
            return false;
        } 
       status = TransactionStatus.IN_PROGRESS;
       return true;
    }
}