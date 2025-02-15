public class SemestralResultModel {
    private int __semester;
    private double __daily_allowance;
    private double __semestral_allowance;
    private double __total_allowance;

    public int get__semester() {
        return this.__semester;
    }

    public void set__semester(int __semester) {
        this.__semester = __semester;
    }

    public double get__daily_allowance() {
        return this.__daily_allowance;
    }

    public void set__daily_allowance(double __daily_allowance) {
        this.__daily_allowance = __daily_allowance;
    }

    public double get__semestral_allowance() {
        return this.__semestral_allowance;
    }

    public void set__semestral_allowance(double __semestral_allowance) {
        this.__semestral_allowance = __semestral_allowance;
    }

    public double get__total_allowance() {
        return this.__total_allowance;
    }

    public void set__total_allowance(double __total_allowance) {
        this.__total_allowance = __total_allowance;
    }

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
