package util.data.procs;

import io.Writable;
import util.data.vals.BaseVal;
import util.data.vals.RealVal;

public class ValPrinter extends RealVal {
    Writable target;

    public ValPrinter(BaseVal val, Writable wr) {
        super(val.group(), val.name(), val.unit());
        markAsDummy();
        this.target=wr;
    }
    public boolean update(double value) {
        target.writeLine(id(),id()+":"+String.valueOf(value)+unit());
        return true;
    }
    public boolean matchWritable( Writable wr ){
        return wr==target;
    }
}
