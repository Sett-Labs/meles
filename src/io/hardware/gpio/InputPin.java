package io.hardware.gpio;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.function.DeviceEventConsumer;
import org.tinylog.Logger;
import util.data.vals.FlagVal;

public class InputPin extends FlagVal implements DeviceEventConsumer<DigitalInputEvent> {
    DigitalInputDevice input;
    long lastTrigger = 0;
    long debounceMs = 50;

    public InputPin(String group, String name, String unit, DigitalInputDevice input) {
        super(group, name, unit);
        this.input = input;
        value = input.getValue();

        input.addListener(this);
    }

    public void setDebounceMs(long ms) {
        this.debounceMs = ms;
    }
    public void close(){
        input.close();
    }
    @Override
    public boolean isUp() {
        value = input.getValue();
        return value;
    }

    @Override
    public void accept(DigitalInputEvent event) {

        if( event.getValue()==value ) // ignore small glitches?
            return;

        if (event.getEpochTime() - lastTrigger < debounceMs ) {
            lastTrigger = event.getEpochTime();
            return;
        }
        Logger.info("Trigger: " + event);
        lastTrigger = event.getEpochTime();
        try {
            super.update(event.getValue());
        }catch (Exception e){
            Logger.error(e);
        }
    }
}
