enum AccountStatus {
    ACTIVE,
    FROZEN
}

class Account {

    private final int accountId;
    private final int customerId;
    private long balance; // CAD cents
    private AccountStatus status;

    public Account (int accountId,   int customerId) {
           this.accountId = accountId;
           this.customerId =  customerId;
            this.balance = 0;
            this.status = AccountStatus.ACTIVE;
    }
    
   private boolean isFrozen() {
    if ( status == AccountStatus.FROZEN ) {
        return true;
    }
    return false;
   }
    public void ensureActive() {
        if (isFrozen()) {
            throw new IllegalStateException("Account is frozen");
        }
    }
    public int getAccountId() {
        return this.accountId;
    }

    public synchronized long getBalance() {
        return this.balance;
    }
    public synchronized void deposit(long amount) {
            
           if (amount<=0) {
              throw new IllegalArgumentException("Amount can not be under 0");
           }
         ensureActive();
          balance = Math.addExact(balance, amount);
     }

   public synchronized void withdraw(long amount) {
      ensureActive();
       if (amount <= 0){
           throw new IllegalArgumentException("Add digit above0");
       } else if (amount > balance){
           throw new IllegalArgumentException("Balance is insufficient");
       }
       else {
            balance-=amount;
        }
    }
}