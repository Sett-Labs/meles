package util.data.procs;

import io.Writable;
import util.data.vals.BaseVal;
import util.data.vals.IntegerVal;
import util.data.vals.RealVal;

public class ValPrinter extends RealVal {
    Writable target;
    boolean isAnInt=false;

    public ValPrinter(BaseVal val, Writable wr) {
        super(val.group(), val.name(), val.unit());
        markAsDummy();
        this.target=wr;
        isAnInt = val instanceof IntegerVal;
    }
    public boolean update(double value) {
        String print = isAnInt
                ? Integer.toString((int)value)  // Use Integer.toString for int
                : String.valueOf(value);         // Use String.valueOf for double
        target.writeLine(id(),id()+":"+print+unit());
        return true;
    }
    public boolean matchWritable( Writable wr ){
        return wr==target;
    }
}
