import java.util.List;

public interface Calculator {
    public final static int WEEKS_IN_MONTH = 4;
    public final static int WORKING_DAYS_IN_WEEK = 5;
    public final static int WEEKS_IN_SEMESTER = 18;
    public final static double ALLOWANCE_INCREASE_MULTIPLIER = 0.03;
    
    MonthlyResultModel calculateTotalMonthlyAllowance();
    List<SemestralResultModel> calculateTotalSemestralAllowance();
}