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

    public long getBalance() {
        return this.balance;
    }
    public void deposit(long amount) {
            
           if (amount<=0) {
              throw new IllegalArgumentException("Amount can not be under 0");
           }
         ensureActive();
          balance = Math.addExact(balance, amount);
     }

   public void withdraw(long amount) {
      ensureActive();
       if(amount > balance){
           throw new IlleagalArgumentException("Balance is insufficient");
       }
       else if(amount <= 0){
           throw new IlleagalArgumentException("Add digit>0");
       }
       else {
            balance-=amount;
        }
    }
}