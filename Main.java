import java.io.IOException;
import java.util.List;

public class Main implements AllowanceCalculator.OnCalculationResultListener{
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        AllowanceCalculator.setOnCalculationResultListener(main);
        AllowanceCalculator calculator = new AllowanceCalculator();
        calculator.start();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onResultSend(Object result) {
        if(result instanceof MonthlyResultModel) {
            ((MonthlyResultModel) result).printResult();
        }
        if(result instanceof List<?>) {
            List<?> __results = (List<?>) result;
            if(__results.stream().allMatch(e -> e instanceof SemestralResultModel)) {
                List<SemestralResultModel> semestralResults = (List<SemestralResultModel>) __results;
                for(SemestralResultModel __result : semestralResults) {
                    __result.printResult();
                }
            }
        }
    }
    
}
