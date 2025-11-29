package io.mqtt;

import io.Writable;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.tinylog.Logger;
import util.data.vals.BaseVal;
import util.data.vals.FlagVal;
import util.data.vals.IntegerVal;
import util.data.vals.RealVal;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

public class MqttWork {

    private final Instant createdAt = Instant.now();
    private Duration ttl = Duration.ZERO; // configurable
    MqttProperties properties = new MqttProperties();
    private String topic;
    private int qos=1;
    private int attempt = 0;
    private int maxAttempts=5;
    private boolean valid=true;
    private byte[] data;
    private Writable origin;

    public MqttWork(String topic){
        this.topic=topic;
    }
    /**
     * Constructor that also adds a value
     * @param group The group this data is coming from
     * @param parameter The parameter to update
     * @param value The new value
     */
    public MqttWork( String group, String parameter, Object value) {
        topic=group+"/"+parameter;
        setValue(value);
    }
    public MqttWork( String topic, Object value) {
        if( checkTopic(topic) )
            setValue(value);
    }

    public MqttWork( String topic, byte[] value, Writable origin){
        if( checkTopic(topic) ){
            this.origin=origin;
            data=value;
        }
    }

    public static MqttWork toTopic(String topic){
        return new MqttWork(topic);
    }

    public MqttWork topic( String topic ){
        this.topic=topic;
        return this;
    }
    public MqttWork payload(String data ){
        this.data=data.getBytes();
        addUserProperty("datatype","ascii_string");
        return this;
    }
    public MqttWork payload(BaseVal val ){

        if( val instanceof IntegerVal ) {
            addUserProperty("datatype", "ascii_int");
        }else if( val instanceof RealVal){
            addUserProperty("datatype","ascii_float");
        }else if( val instanceof FlagVal ) {
            addUserProperty("datatype", "ascii_bool");
        }else{
            addUserProperty("datatype","ascii_string");
        }

        this.data=val.asString().getBytes();
        return this;
    }
    public MqttWork qos(int qos){
        this.qos=qos;
        return this;
    }
    public MqttWork includeTimestamp(){
        properties.getUserProperties().add( new UserProperty("timestamp", createdAt.toString()) );
        return this;
    }
    public MqttWork addUserProperty(String key, String value ){
        properties.getUserProperties().add( new UserProperty(key,value));
        return this;
    }
    public MqttWork inform( Writable wr){
        this.origin=wr;
        return this;
    }
    public MqttWork expiresAfter(int count, TemporalUnit unit){
        if( count != 0)
            ttl = Duration.of(count,unit);
        return this;
    }
    public boolean isExpired(){
        if( ttl.isZero() )
            return false;
        return Duration.between(createdAt, Instant.now()).compareTo(ttl) > 0;
    }

    private boolean checkTopic( String topic ){
        if( !topic.contains("/")){
            Logger.error( "No topic given in mqttwork: "+topic+ "(missing / )");
            valid=false;
            return false;
        }
            this.topic=topic;
        return true;
    }
    public Optional<Writable> getOrigin(){
        return Optional.ofNullable(origin);
    }
	private void setValue( Object val){
        if (val instanceof Double d) {
            data = Double.toString(d).getBytes();
        } else if (val instanceof Integer i) {
            data = Integer.toString(i).getBytes();
        } else if (val instanceof Boolean b ) {
            data = Boolean.toString(b).getBytes();
        } else if (val instanceof String s) {
            data = s.getBytes();
        }else{
            Logger.error("mqtt -> Invalid class given, topic:"+topic);
            valid=false;
        }
	}
	public String toString(){
		return "Topic: "+topic+" -> data:"+new String(data) +" -> qos: "+qos;
	}
	/*  SETTINGS */
	/**
	 * Change te QoS for this message
	 * @param qos The new QoS value to use
	 */
	public void alterQos( int qos ) {
		this.qos=qos;
	}
	/**
	 * Get the device name given 
	 * @return The name of the device this data relates to
	 */
	public String getTopic() {
		return topic;
	}
	public boolean isInvalid(){
		return !valid;
	}
	public MqttMessage getMessage(){
        var message = new MqttMessage(data);
        message.setQos(qos);
		return message;
	}
    public MqttProperties getProperties(){
        return properties;
    }
	/* ********************************* ADDING DATA ******************************************************** */

	public boolean incrementAttempt() {
		attempt++;
        return attempt < maxAttempts;
    }
}
