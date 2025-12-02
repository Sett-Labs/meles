package util.evalcore;

import org.tinylog.Logger;
import util.data.vals.AnyDummy;
import util.data.vals.BaseVal;
import util.data.vals.NumericVal;
import util.data.vals.ValUser;

import java.util.StringJoiner;

public class BaseEvaluator implements ValUser {
    // Info for debugging
    protected String originalExpression;
    protected String normalizedExpression;
    protected String parseResult;

    // Variables for evaluation
    protected NumericVal[] refs;
    protected int highestI=-1;
    protected Integer[] refLookup;
    protected boolean valid = false;

    String id = "";

    void setHighestI( int hI){ highestI=hI; }
    void setRefs( NumericVal[] refs){
        this.refs=refs;
    }

    public void setId(String id) {
        this.id = id;
    }
    public NumericVal[] getRefs() {
        return refs;
    }
    public String getOriginalExpression(){
        return originalExpression;
    }

    public String getInfo(String id) {
        return "";
    }

    protected boolean badInputCount(int length, String data) {
        if (length < highestI) {
            Logger.error(id + " (eval) -> Not enough elements in input data, need " + (1 + highestI) + " got " + length + ": " + data);
            return true;
        }
        return false;
    }

    public boolean isInValid() {
        return !valid;
    }

    @Override
    public boolean isWriter() {
        return false;
    }
    public boolean provideVal( BaseVal newVal){
        boolean noDummies = true;
        for( int a=0;a<refs.length;a++){
            if( refs[a].getClass().isInstance(newVal) || refs[a] instanceof AnyDummy ) {
                if( refs[a].id().equals(newVal.id()))
                    refs[a] = (NumericVal) newVal;
            }
            if( refs[a].isDummy())
                noDummies=false;
        }
        return noDummies;
    }
    public String id(){
        return "eval:"+id;
    }
    @Override
    public String getValIssues() {
        var join = new StringJoiner(";");
        for( var ref : refs ){
            if( ref.isDummy())
                join.add(ref.id());
        }
        return "["+id()+" needs "+join+"]";
    }
}
