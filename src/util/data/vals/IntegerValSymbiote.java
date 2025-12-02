package util.data.vals;

import io.Writable;
import org.apache.commons.lang3.ArrayUtils;
import util.data.procs.ValPrinter;

import java.util.ArrayList;
import java.util.Arrays;

public class IntegerValSymbiote extends IntegerVal {

    NumericVal[] underlings;
    boolean passOriginal = true;
    int level = 0;
    IntegerVal host;

    public IntegerValSymbiote(int level, IntegerVal host, NumericVal... underlings) {
        super(host.group(), host.name(), host.unit());

        this.underlings = underlings;
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

    public void addUnderling(NumericVal underling) {
        underlings = ArrayUtils.add(underlings, underling);
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
