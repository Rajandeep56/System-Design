class TransferService {
    public void transfer(Account from, Account to, long amount) {
        if(from == null || to == null) {
            throw new NullPointerException("Account can not be null ");
        }
        from.ensureActive();
        to.ensureActive();
        int id1 = from.getAccountId();
        int id2 = to.getAccountId();

        long bal1 = from.getBalance();
        long bal2 = to.getBalance();
        
        if(id1==id2) {
            throw new IllegalArgumentException("Accounts are same");
        }
        if(bal1<amount  ) {
            throw new IllegalArgumentException("Amount exceeds balance");
        }
        if( amount <=0 ){
            throw new IllegalArgumentException("Amount is negative");
        }
        bal2 = Math.addExact(bal2,amount);
        // not atomic But what if another thread changes or freezes the receiving account between those two operations?
        //The sender could lose money while the receiver gets nothing.

        // Also, another thread could modify the sender's balance after your initial validation.
        //We need to lock both accounts throughout validation and the balance changes.
        from.withdraw(amount);
        to.deposit(amount);

    }
}
