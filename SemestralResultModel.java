public class SemestralResultModel {
    private int __semester;
    private double __daily_allowance;
    private double __semestral_allowance;
    private double __total_allowance;
    public SemestralResultModel(int xsemester, double xdailyallowance, double xsemestralallowance, double xtotalallowance) {
        this.__semester = xsemester;
        this.__daily_allowance = xdailyallowance;
        this.__semestral_allowance = xsemestralallowance;
        this.__total_allowance = xtotalallowance;
    }
    public void printResult() {
        System.out.printf(
            "\n%d%s SEMESTER\n--------------------\nDAILY ALLOWANCE: %.2f\nSEMESTRAL ALLOWANCE: %.2f\nTOTAL ALLOWANCE: %.2f\n\n",
            __semester,
            getOrdinal(__semester),
            __daily_allowance,
            __semestral_allowance,
            __total_allowance
        );
    }
    private String getOrdinal(int i) {
        String s = String.valueOf(i);
        char c = s.charAt(s.length() - 1);
        switch (c) {
            case '1':
                return "st";
            case '2':
                return "nd";
            case '3':
                return "rd";
            default:
                return "th";
        }
    }
}
