public class MonthlyResultModel {
    private int __months;
    private double __daily_allowance;
    private double __total_allowance;
    public MonthlyResultModel(int months, double daily_allowance, double total_allowance) {
        this.__daily_allowance = daily_allowance;
        this.__months = months;
        this.__total_allowance = total_allowance;
    }
    public void printResult() {
        System.out.printf("\nYour daily allowance is: %.2f.\nYour total allowance for %d months is: %.2f\n",
            __daily_allowance,
            __months, 
            __total_allowance
        );
    }
}
