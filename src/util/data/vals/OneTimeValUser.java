package util.data.vals;

public class OneTimeValUser implements ValUser{

    private static final OneTimeValUser INSTANCE = new OneTimeValUser();

    private OneTimeValUser() {
        // Private constructor for singleton
    }
    public static boolean isOneTime(ValUser user) {
        return user == INSTANCE;
    }
    public static OneTimeValUser get() {
        return INSTANCE;
    }
    public boolean isWriter(){
        return false;
    }

    @Override
    public boolean provideVal(BaseVal val) {
        return true;
    }
    public String id(){
        return "otvu";
    }
}
