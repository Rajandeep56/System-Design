public class AccountSummary {
    private final long totalIncoming;
    private final long totalOutgoing;
    private final int transactionCount;

    public AccountSummary(long totalIncoming,
                          long totalOutgoing,
                          int transactionCount) {
        this.totalIncoming = totalIncoming;
        this.totalOutgoing = totalOutgoing;
        this.transactionCount = transactionCount;
    }

    public long getTotalIncoming() {
        return totalIncoming;
    }

    public long getTotalOutgoing() {
        return totalOutgoing;
    }

    public int getTransactionCount() {
        return transactionCount;
    }
}