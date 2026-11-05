class TransferService {
    public void transfer(Account from, Account to, long amount) {
         from.ensureActive();
        to.ensure.Active();
          int id1 = from.getAccountId();
          int id2 = to.getAccountId();
 
          long bal1 = from.getBalance();
          long bal2 = to.getBalance();
         if(from==null || to==null) {
          throw new IllegalArgumentException("Account can not be null ");
         }
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
    }
}
