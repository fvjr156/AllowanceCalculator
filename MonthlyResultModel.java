public class MonthlyResultModel {
    private int __months;
    private double __daily_allowance;
    private double __total_allowance;

    public int get__months() {
        return this.__months;
    }

    public void set__months(int __months) {
        this.__months = __months;
    }

    public double get__daily_allowance() {
        return this.__daily_allowance;
    }

    public void set__daily_allowance(double __daily_allowance) {
        this.__daily_allowance = __daily_allowance;
    }

    public double get__total_allowance() {
        return this.__total_allowance;
    }

    public void set__total_allowance(double __total_allowance) {
        this.__total_allowance = __total_allowance;
    }

    public MonthlyResultModel(int months, double daily_allowance, double total_allowance) {
        this.__daily_allowance = daily_allowance;
        this.__months = months;
        this.__total_allowance = total_allowance;
    }

    public void printResult() {
        System.out.printf("\nYour daily allowance is: %.2f.\nYour total allowance for %d months is: %.2f\n",
                __daily_allowance,
                __months,
                __total_allowance);
    }
}
