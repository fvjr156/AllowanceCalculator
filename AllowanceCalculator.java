import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AllowanceCalculator implements Calculator {
    interface OnCalculationResultListener {
        void onResultSend(Object result);
    }

    private static OnCalculationResultListener listener;

    public static void setOnCalculationResultListener(OnCalculationResultListener xlistener) {
        listener = xlistener;
    }

    public static void sendResult(Object result) {
        if (listener != null) {
            listener.onResultSend(result);
        }
    }

    static double dailyAllowance;
    static int months;
    static int semesters;

    @Override
    public MonthlyResultModel calculateTotalMonthlyAllowance() {
        return new MonthlyResultModel(
                months,
                dailyAllowance,
                ((dailyAllowance * WORKING_DAYS_IN_WEEK) * WEEKS_IN_MONTH) * months);
    }
    
    @Override
    public List<SemestralResultModel> calculateTotalSemestralAllowance() {
        List<SemestralResultModel> results = new ArrayList<>();
        //ignore unfinished method
        return results;
    }
    
    public void start() throws IOException {
        int userInput = 0;
        BufferedReader readUserInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(
                "\nALLOWANCE CALCULATOR\n--------------------\n(1) CALCULATE MONTHLY ALLOWANCE\n(2) CALCULATE SEMESTRAL ALLOWANCE\n\nType the number of your preferred operation. -> ");
        try {
            userInput = Integer.parseInt(readUserInput.readLine());
        } catch (NumberFormatException exception) {
            System.out.println("Invalid choice. Please retry.");
            start();
            return;
        }
        switch (userInput) {
            case 1:
                queryMonthly(readUserInput);
                break;
            case 2:
                querySemestral(readUserInput);
                break;
            default:
                System.out.println("Invalid choice. Please retry.");
                start();
        }
        readUserInput.close();
    }
    
    public void queryMonthly(BufferedReader readUserInput) throws IOException {
        System.out.print("\nHow much is your daily allowance? -> ");
        dailyAllowance = Double.parseDouble(readUserInput.readLine());
        System.out.print("How many months? -> ");
        months = Integer.parseInt(readUserInput.readLine());
        sendResult(calculateTotalMonthlyAllowance());
    }

    public void querySemestral(BufferedReader readUserInput) throws IOException {
        System.out.print("\nHow much is your daily allowance? -> ");
        dailyAllowance = Double.parseDouble(readUserInput.readLine());
        System.out.print("How many semesters? -> ");
        semesters = Integer.parseInt(readUserInput.readLine());
        sendResult(calculateTotalSemestralAllowance());
    }

}
