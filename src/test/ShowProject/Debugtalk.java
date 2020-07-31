public class Debugtalk {
    public static Integer sum_two(Integer m, Integer n) {
        return m * n;
    }
    public static Double sum_two(Integer m, Double n) {
        return m + n;
    }
    public static String funcWithoutParam(){
        return "android_chuizi";
    }
    public static String funcWithParam(String var1,String var2){
        return var1+var2;
    }
    public void setup_hooks(){
        System.out.println("setup_hooks execute");
    }
    public void setup_testcase(){
        System.out.println("setup_testcase execute");
    }
    public void teardown_hooks(){
        System.out.println("teardown_hooks execute");
    }
    public void func2(){
        System.out.println("func2 execute");
    }
}
