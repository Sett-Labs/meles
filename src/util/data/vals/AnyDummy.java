package util.data.vals;

import java.math.BigDecimal;

public class AnyDummy extends BaseVal implements NumericVal{

    AnyDummy(String group,String name)
    {
        this.group=group;
        this.name=name;
        markAsDummy();
    }

    static AnyDummy createDummy(String id){
        var split = id.split("_",2);
        return new AnyDummy(split[0],split[1]);
    }
    @Override
    public boolean update(double value) {
        return false;
    }

    @Override
    public boolean update(int value) {
        return false;
    }

    @Override
    public void resetValue() {

    }

    @Override
    public double asDouble() {
        return 0;
    }

    @Override
    public int asInteger() {
        return 0;
    }

    @Override
    public boolean parseValue(String value) {
        return false;
    }

    @Override
    public String asString() {
        return "dummy";
    }

    @Override
    public BigDecimal asBigDecimal() {
        return null;
    }

    @Override
    public void triggerUpdate() {

    }
}
