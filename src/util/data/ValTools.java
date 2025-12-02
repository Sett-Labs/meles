package util.data;

import org.apache.commons.lang3.math.NumberUtils;
import org.tinylog.Logger;
import util.data.vals.*;
import util.evalcore.ParseTools;
import util.math.MathUtils;
import util.tasks.blocks.NoOpBlock;
import util.tools.TimeTools;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class ValTools {

    private final static Pattern words = Pattern.compile("[a-zA-Z]+[_:\\d]*[a-zA-Z\\d]+\\d*");

    /**
     * Checks the exp for any mentions of the numerical rtvals and if found adds these to the nums arraylist and replaces
     * the reference with 'i' followed by the index in nums+offset.
     * The reason for doing this is mainly parse once, use expression many times

     * fe. {r:temp}+30, nums still empty and offset 1: will add the RealVal temp to nums and alter exp to i1 + 30
     * @param exp The expression to check
     * @param nums The Arraylist to hold the numerical values
     * @param offset The index offset to apply
     * @return The altered expression
     */
    public static String buildNumericalMem(Rtvals rtvals, String exp, ArrayList<NumericVal> nums, int offset) {
        if( nums==null)
            nums = new ArrayList<>();

        // Find all the real/flag/int pairs
        var pairs = ParseTools.extractKeyValue(exp, true); // Add those of the format {d:id}

        for( var p : pairs ) {
            boolean ok=false;
            if (p.length != 2) {
                Logger.error("Pair containing odd amount of elements: " + String.join(":", p));
                continue;
            }
            for (int pos = 0; pos < nums.size(); pos++) { // go through the known realVals
                var d = nums.get(pos);
                if (d.id().equalsIgnoreCase(p[1])) { // If a match is found
                    exp = exp.replace("{" + p[0] + ":" + p[1] + "}", "i" + (offset + pos));
                    exp = exp.replace(p[0] + ":" + p[1], "i" + (offset + pos));
                    ok = true;
                    break;
                }
            }
            if (ok)
                continue;

            int index = findOrAddValue(p[1], p[0], rtvals, nums, offset);
            if (index == -1)
                return "";

            exp = exp.replace("{" + p[0] + ":" + p[1] + "}", "i" + index);
            exp = exp.replace(p[0] + ":" + p[1], "i" + index);
        }
        // Figure out the rest?
        var found = words.matcher(exp).results().map(MatchResult::group).toList();
        for( String fl : found){
            if( fl.matches("^i\\d+") )
                continue;
            int index = findOrAddValue( fl, fl.contains("flag:") ? "f" : "d",  rtvals, nums, offset);
            if (index == -1) {
                return "";
            }
            exp = exp.replace(fl, "i" + index);
        }
        nums.trimToSize();
        return exp;
    }

    private static int findOrAddValue(String id, String type, Rtvals rtvals, ArrayList<NumericVal> nums, int offset) {

        var val = switch (type) {
            case "d", "double", "r", "real" -> rtvals.getRealVal(OneTimeValUser.get(),id);
            case "int", "i" -> rtvals.getIntegerVal(OneTimeValUser.get(),id);
            case "f", "flag", "b" -> rtvals.getFlagVal(OneTimeValUser.get(),id);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
        int foundAt=-1;
        for( int in = 0; in<nums.size();in++){
            if( nums.get(in).id().equals(id) ) {
                foundAt = in;
                break;
            }
        }
        if (foundAt == -1) {
            nums.add(val);
            foundAt = nums.size() - 1;
        }
        return foundAt + offset;
    }
    /**
     * Process an expression that contains both numbers and references and figure out the result
     *
     * @param expr The expression to process
     * @param rv The realtimevalues store
     * @return The result or NaN if failed
     */
    public static double processExpression(String expr, Rtvals rv) {
        double result=Double.NaN;

        expr = ValTools.parseRTline(expr,"",rv);
        expr = expr.replace("true","1");
        expr = expr.replace("false","0");

        expr = ValTools.simpleParseRT(expr,"",rv); // Replace all references with actual numbers if possible

        if( expr.isEmpty()) // If any part of the conversion failed
            return result;

        var parts = ParseTools.extractParts(expr);
        if( parts.size()==1 ){
            result = NumberUtils.createDouble(expr);
        }else if (parts.size()==3){
            result = Objects.requireNonNull(ParseTools.decodeDoublesOp(parts.get(0), parts.get(2), parts.get(1), 0)).apply(new double[]{});
        }else{
            try {
                result = MathUtils.noRefCalculation(expr, Double.NaN, false);
            }catch(IndexOutOfBoundsException e){
                Logger.error("Index out of bounds while processing "+expr);
                return Double.NaN;
            }
        }
        if( Double.isNaN(result) )
            Logger.error("Something went wrong processing: "+expr);
        return result;
    }
    /**
     * Stricter version to parse a realtime line, must contain the references within { }
     * Options are:
     * - RealVal: {d:id} and {real:id}
     * - FlagVal: {f:id} or {b:id} and {flag:id}
     * This also checks for {utc}/{utclong},{utcshort} to insert current timestamp
     * @param line The original line to parse/alter
     * @param error Value to put if the reference isn't found
     * @return The (possibly) altered line
     */
    public static String parseRTline(String line, String error, Rtvals rtvals) {

        if( !line.contains("{"))
            return line;

        var pairs = ParseTools.extractKeyValue(line, true);
        for( var p : pairs ){
            if (p.length != 2) {
                line = replaceTime(p[0], line, rtvals);
                continue;
            }
            switch (p[0]) {
                case "d", "r", "double", "real" -> {
                    var d = rtvals.getRealVal(OneTimeValUser.get(),p[1]);
                    if (!d.isDummy())
                        line = line.replace("{" + p[0] + ":" + p[1] + "}",d.asString());
                }
                case "i", "int", "integer" -> {
                    var i = rtvals.getIntegerVal(OneTimeValUser.get(),p[1]);
                    if ( !i.isDummy() )
                        line = line.replace("{" + p[0] + ":" + p[1] + "}", String.valueOf(i));
                }
                case "t", "text" -> {
                    var t = rtvals.getTextVal(OneTimeValUser.get(),p[1]);
                    if (!t.isDummy())
                        line = line.replace("{" + p[0] + ":" + p[1] + "}", t.asString());
                }
                case "f", "b", "flag" -> {
                    var d = rtvals.getFlagVal(OneTimeValUser.get(),p[1]);
                    if (!d.isDummy())
                        line = line.replace("{" + p[0] + ":" + p[1] + "}", d.asString());
                }
                case "rand","random" -> {
                    var max = NumberUtils.createInteger(p[1]);
                    if( max!=null ){
                        var random = new Random();
                        var roll = String.valueOf(random.nextInt(1, max+1));
                        line = line.replace("{" + p[0] + ":" + p[1] + "}", roll);
                    }
                }
            }
        }
        var lower = line.toLowerCase();
        if ((lower.contains("{d:") || lower.contains("{r:") ||
                lower.contains("{f:") || lower.contains("{i:")) && !pairs.isEmpty()) {
            Logger.warn("Found a {*:*}, might mean parsing a section of "+line+" failed");
        }
        return line;
    }
    private static String replaceTime(String ref, String line, Rtvals rtvals) {
        return switch(ref){
            case "ref" -> line.replace("{utc}", TimeTools.formatLongUTCNow());
            case "utclong" -> line.replace("{utclong}", TimeTools.formatLongUTCNow());
            case "utcshort"-> line.replace("{utcshort}", TimeTools.formatShortUTCNow());
            default ->
            {
                var val = rtvals.getBaseVal( OneTimeValUser.get(), ref );
                if( !val.isDummy())
                    yield line.replace("{" + ref + "}", val.asString());
                yield line;
            }
        };
    }
    /**
     * Simple version of the parse realtime line, just checks all the words to see if any matches the hashmaps.
     * If anything goes wrong, the 'error' will be returned. If this is set to ignore if something is not found it
     * will be replaced according to the type: real-> NaN, int -> Integer.MAX
     * @param line The line to parse
     * @param error The line to return on an error or 'ignore' if errors should be ignored
     * @return The (possibly) altered line
     */
    public static String simpleParseRT(String line, String error, Rtvals rv) {

        var found = words.matcher(line).results().map(MatchResult::group).toList();
        for( var word : found ){
            // Check if the word contains a : with means it's {d:id} etc, if not it could be anything
            String replacement = word.contains(":") ? findReplacement(rv, word, line, error)
                    : checkRealtimeValues(rv, word, line, error);
            assert replacement != null;
            if (replacement.equalsIgnoreCase(error))
                return error;
            if (!replacement.isEmpty())
                line = line.replace(word, replacement);
        }
        return line;
    }

    private static String findReplacement(Rtvals rtvals, String word, String line, String error) {
        var id = word.split(":")[1];
        error =  error.equalsIgnoreCase("ignore")?id:error;
        return switch (word.charAt(0)) {
            case 'd', 'r' -> {
                var rv = rtvals.getRealVal(OneTimeValUser.get(),id);
                if (rv.isDummy()) {
                    Logger.error("No such real " + id + ", extracted from " + line); // notify
                    yield error;
                }
                yield rv.asString();
            }
            case 'i' -> {
                var i = rtvals.getIntegerVal(OneTimeValUser.get(),id);
                if (i.isDummy()) { // ID not found
                    Logger.error("No such integer " + id + ", extracted from " + line); // notify
                    yield error;
                }
                yield i.asString();
            }
            case 'f' -> {
                var fv = rtvals.getIntegerVal(OneTimeValUser.get(),id);
                if (fv.isDummy()) {
                    Logger.error("No such flag " + id + ", extracted from " + line);
                    yield error;
                }
                yield fv.asString();
            }
            case 't', 'T' -> {
                var tv = rtvals.getTextVal(OneTimeValUser.get(),id);
                if (tv.isDummy()) {
                    Logger.error("No such text " + id + ", extracted from " + line);
                    yield error;
                }
                yield tv.asString();
            }
            default -> {
                Logger.error("No such type: " + word.charAt(0));
                yield error;
            }
        };
    }

    private static String checkRealtimeValues(Rtvals rtvals, String word, String line, String error) {
        var val = rtvals.getBaseVal(OneTimeValUser.get(),word);
        if( val.isDummy() ){
            Logger.error("Couldn't process " + word + " found in " + line); // log it and abort
            return error;
        }
        return val.asString();
    }
}
