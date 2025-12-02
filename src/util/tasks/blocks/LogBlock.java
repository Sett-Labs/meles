package util.tasks.blocks;

import org.tinylog.Logger;
import util.data.vals.AnyDummy;
import util.data.vals.BaseVal;
import util.data.vals.NumericVal;
import util.data.vals.ValUser;

public class LogBlock extends AbstractBlock implements ValUser {
    enum LEVEL {INFO, WARN, ERROR}

    LEVEL level;
    String message;
    BaseVal[] refs;

    private LogBlock(LEVEL level, String message, BaseVal[] refs) {
        this.level = level;
        this.message = id() + " -> " + message;
        this.refs=refs;
    }
    public String type(){ return "LogBlock";}

    public static LogBlock info(String message, BaseVal[] refs) { return new LogBlock(LEVEL.INFO, message,refs); }
    public static LogBlock warn(String message, BaseVal[] refs) {
        return new LogBlock(LEVEL.WARN, message,refs);
    }
    public static LogBlock error(String message, BaseVal[] refs) {
        return new LogBlock(LEVEL.ERROR, message,refs);
    }

    @Override
    public boolean start() {
        return start(Double.NaN);
    }

    public boolean start(double... input) {
        var m = message;
        clean = false;
        if (!Double.isNaN(input[0]) && input.length == 3) {
            m = m.replace("{new}", String.valueOf(input[0]));
            m = m.replace("{old}", String.valueOf(input[1]));
            m = m.replace("{math}", String.valueOf(input[2]));
        }
        if( refs!=null ){
            for( var nf : refs )
                m = m.replace("{"+nf.id()+"}",nf.asString()+nf.unit());
        }
        switch (level) {
            case INFO -> Logger.tag("TASK").info(m);
            case WARN -> Logger.tag("TASK").warn(m);
            case ERROR -> Logger.tag("TASK").error(m);
        }
        doNext(input);
        return true;
    }
    void doNext(double... input) {
        if (next != null) {
            if (next instanceof ConditionBlock cb) {
                cb.start(input);
            } else if (next instanceof LogBlock lb) {
                lb.start(input);
            } else if (next instanceof MathBlock mb) {
                mb.start(input);
            } else {
                next.start();
            }
        }
        sendCallback(id() + " -> OK");
    }
    public boolean isWriter(){
        return false;
    }

    @Override
    public boolean provideVal(BaseVal val) {
        boolean noDummies=true;
        for( int a=0;a<refs.length;a++ ){
            var old = refs[a];
            if( old.isDummy())
                noDummies=false;
            if( old.id().equals(val.id()) ){
                if( old.getClass().isInstance(val) || old instanceof AnyDummy) {
                    refs[a] = val;
                    Logger.info("Replaced "+old.id());
                }
            }
        }
        return noDummies;
    }
}
