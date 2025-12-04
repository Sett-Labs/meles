package util.data.vals;

import io.Writable;
import org.apache.commons.lang3.ArrayUtils;
import util.data.procs.ValPrinter;

import java.util.ArrayList;
import java.util.Arrays;

public class IntegerValSymbiote extends IntegerVal {

    NumericVal[] underlings;
    boolean passOriginal = false;
    int level = 0;
    IntegerVal host;

    public IntegerValSymbiote(int level, IntegerVal host ) {
        super(host.group(), host.name(), host.unit());
        underlings = new NumericVal[]{host};
        this.level=level;
        this.host=host;
    }
    public IntegerValSymbiote(int level, IntegerVal host, NumericVal... underlings) {
        super(host.group(), host.name(), host.unit());

        this.underlings = ArrayUtils.insert(0,underlings,host);
        this.level = level;
        this.host = host;
    }
    @Override
    public boolean update(int val) {
        var result = host.update(val);
        this.value = host.value();

        var forwardedValue = passOriginal ? val : value;
        if (result || passOriginal)
            Arrays.stream(underlings, 1, underlings.length).forEach(rv -> rv.update(forwardedValue));

        return result;
    }

    public int value() {
        return host.value();
    }

    public int level() {
        return level;
    }

    @Override
    public void resetValue() {
        host.defValue(defValue);
        value = host.value();
    }

    public void defValue(int defValue) {
        host.defValue(defValue);
        this.defValue = defValue;
    }
    public NumericVal[] getUnderlings(){
        return underlings;
    }
    public void addUnderling(NumericVal underling) {
        underlings = ArrayUtils.add(underlings, underling);
    }

    public NumericVal[] getDerived() {
        return Arrays.copyOfRange(underlings, 1, underlings.length);
    }
    public NumericVal getHost(){
        return host;
    }
    public boolean replaceUnderling( NumericVal repl ){
        for( int i=1;i<underlings.length;i++ ){
            if( underlings[i].id().equals(repl.id() )){
                underlings[i]=repl;
                return true;
            }
        }
        return false;
    }
    public void removePrinterUnderling(Writable wr){
        int index=-1;
        for( int a=1;a<underlings.length;a++ ){
            if( underlings[a] instanceof ValPrinter vp ){
                if( vp.matchWritable(wr)) {
                    index = a;
                    break;
                }
            }
        }
        if( index>=1 )
            underlings = ArrayUtils.remove(underlings,index);
    }
}
