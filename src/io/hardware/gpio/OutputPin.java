package io.hardware.gpio;

import com.diozero.api.DigitalOutputDevice;
import org.tinylog.Logger;
import util.data.vals.FlagVal;

public class OutputPin extends FlagVal {
    DigitalOutputDevice output;

    public OutputPin(String group, String name, String unit, DigitalOutputDevice output) {
        super(group, name, unit);
        this.output = output;
    }

    @Override
    public void update(boolean state) {
        super.update(state);
        value(state);
    }

    @Override
    public void value(boolean state) {
        output.setOn(state);
        value = state;
    }

    @Override
    public void resetValue() {
        super.resetValue();
        output.setOn(defValue);
    }

    @Override
    public boolean isUp() {
        return output.isOn();
    }
}
