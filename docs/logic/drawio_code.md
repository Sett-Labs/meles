# How the Draw.io Parser works

**How draw.io will be used**

Since there's no 'one style fits all' when it comes to diagrams, I wanted to keep it appearance-agnostic. As such, the 
only rule regarding visual aspects, is that every arrow must have a value (text displayed on it), which we'll call label 
from now on. This label is used to define the kind of relationship between the connected shapes.

The rest of the configuration is 'invisible', given it's done using shape properties (accessed by right clicking on a 
shape and selecting 'Edit data'). The mandatory property for any shape is `melestype` which defines what it should map to 
in code. Other required properties depend on this `melestype`.

Below is an example of two diagrams. Left and right result in the same logic, just different aesthetic. The bold text is 
just a suggestion, the flavor text underneath uses placeholders to show properties. It's those properties that actually 
define the logic.

<insert image>

## From shape to DrawioCell: parsing draw.io xml

Below is a snippet showing how the majority of a draw.io xml file looks. I won't go into detail explaining it... because 
I don't know given I only cared about the things I needed anyway.

```xml
<mxfile agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) draw.io/26.2.15 Chrome/134.0.6998.205 Electron/35.2.1 Safari/537.36" host="Electron" version="26.2.15" pages="4">
  <diagram id="IfgC_i9sJ90nQT9nQvYs" name="Basics">
    <mxGraphModel arrows="1" connect="1" fold="1" grid="1" gridSize="10" guides="1" math="0" page="1" pageHeight="1169" pageScale="1" pageWidth="827" shadow="0" tooltips="1">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
        <mxCell edge="1" id="aRKCRy_f5RSsDbNbkwyN-4" parent="1" source="aRKCRy_f5RSsDbNbkwyN-1" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;" target="aRKCRy_f5RSsDbNbkwyN-3" value="next">
          <mxGeometry as="geometry" relative="1" />
        </mxCell>
```
### First pass

When a property is added to a shape, the mxCell get encapsulated in an object node.
```xml
<object melestype="writerblock" id="aRKCRy_f5RSsDbNbkwyN-1" label="label;Runs:<span style="background-color: transparent; color: light-dark(rgb(51, 51, 51), rgb(193, 193, 193));">%runs%</span></div>" placeholders="1">
  <mxCell parent="1" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;fontColor=#333333;" vertex="1">
      <mxGeometry as="geometry" height="60" width="120" x="120" y="450" />
  </mxCell>
</object>
```
So the first pass through the file involves finding and parsing these object nodes.  (the one above has an added `melestype` 
property.)

This means that the first pass involves:

* Finding the object nodes
* Grabbing the id and `melestype` attribute (and later any other relevant attribute)
* Creating a `DrawioCell` in java containing this info.

Below is what this class contains.

```java
public static class DrawioCell{
  String drawId;  // The drawio id, so id attribute in that file
  String type;  // The type the object represents (earlier mentioned melestype)
  String melesId;  // The id given to the final object in meles

  HashMap<String, String> params = new HashMap<>();  // Store all added attributes
  HashMap<String, DrawioCell> arrows = new HashMap<>(); // Store the arrow by abel and a reference to the parsed object it connects this one to
}
```
>Note: Yes, i know it's params instead of attributes, just felt it fits better.

Those `DrawioCells` are in turn stored in a hashmap called `cells` that uses the drawio id as key.

Now that we've done the object nodes, we can add the linked arrows.

### Second pass

The second pass is finding and adding arrows to the `DrawioCells`, the tricky parts here were:

* Arrows can have two different kinds of style attribute. (see snippet below).
* The arrow data and label data can be in separate nodes.
* We want to retain arrows that have a source and label but no target. To potentially inform the user of unconnected arrows.

```xml
<!-- style="edgeStyle=orthogonalEdgeStyle -->
<mxCell edge="1" id="aRKCRy_f5RSsDbNbkwyN-4" parent="1" source="aRKCRy_f5RSsDbNbkwyN-1" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;" target="aRKCRy_f5RSsDbNbkwyN-3" value="next">
     <mxGeometry as="geometry" relative="1" />
</mxCell>
<!--  And style="endArrow -->
<mxcell edge="1" id="H1HTW9UBntW4KhIGkTkH-4" parent="1" source="H1HTW9UBntW4KhIGkTkH-1" style="endArrow=classic;html=1;rounded=0;exitX=-0.017;exitY=0.44;exitDx=0;exitDy=0;exitPerimeter=0;" value="Stop">
  <mxgeometry as="geometry" height="50" relative="1" width="50">
     <mxpoint as="sourcePoint" x="370" y="340"></mxpoint>
     <mxpoint as="targetPoint" x="60" y="346"></mxpoint>
  </mxgeometry>
</mxcell>
```
Therefore, another collection is needed to (temporary) store the arrows. Record fits because the data is immutable.
```java
public record Arrow(DrawioCell source, String label, DrawioCell target) { }
```
These are stored in yet another hashmap called arrows with the drawio id as key.

So the parser:
* Checks if parent attribute is 1, meaning it's not an internal shape. (i think)
* Checks the style attribute to match one of the earlier mentioned ones.
* If the source is matching an id in cells and it has a label, add it to the created Drawiocell.
* If it hasn't got a label, add to the arrows map.

### Final pass
So the third and final pass, involves finding the label for the content of arrows.
The nodes to look for, are like the one shown below.
```xml
<mxCell connectable="0" id="H1HTW9UBntW4KhIGkTkH-3" parent="H1HTW9UBntW4KhIGkTkH-2" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];" value="Trigger" vertex="1">
   <mxGeometry as="geometry" relative="1" x="-0.1292" y="-2">
     <mxPoint as="offset" />
   </mxGeometry>
</mxCell>
```

Meaning the parser needs to find nodes:

* With the parent referring to an item in arrows
* Value attribute not empty (this is the label)
* Style starting with edgelabel

While parsing, found labels are added to the relevant arrow and those in turn get assigned to the `DrawioCell`.

Or diagram style:
<insert image>
That's in a nutshell how the parser prepares the data for  the next step.

Below is an image to help explain how the final DrawioCell hasmap looks.
<insert image>

## From DrawioCell to meles*Block

>Note: All snippets might not be up to date when reading this.

The TaskManager code (written before I even considered this project) is already suited for this kind of usage. At the 
moment, there are 13 distinct blocks that each fulfill a single function.

* OriginBlock - Acts as the entrypoint to a task, provides the task with an id and keeps track of  runs.
* ClockBlock - Creates a time of day based trigger
* CmdBlock - Adds a command to the general pipeline
* ConditionBlock - Checks a condition based on realtime values
* Controlblock - Can start or stop/reset another task
* ...

For a full list and explanation go <add link>.

All of these blocks extend AbstractBlock and this class has two variables which are also `Abstractblocks` called next and 
altRoute. This means that each block can link to up to two others. For example, in a `ConditionBlock`, next links to a 
block triggered on a pass, while altRoute handles the fail case. This also means a block is blissfully unaware of what 
happened before it.

The full code for the conversion is a class named `TaskParser`.(probably should rename it because it no longer contains 
the parser logic...)

### Recursion

Given diagrams involve repeating the same thing while following the arrows, recursion is used to traverse it.

At the center of this lies the createBlock method in TaskParser, even though it's a simple lookup. It takes a `DrawioCell` 
among other things and returns the build `AbstractBlock` (or null).

```java
    public static AbstractBlock createBlock(Drawio.DrawioCell cell, TaskTools tools, String id) {
        if (cell == null)
            return null;

        return switch (cell.type) {
            case "basicvalblock" -> doAlterValBlock(cell, tools, id);
            case "clockblock" -> doClockBlock(cell, tools, id);
            case "controlblock" -> doControlBlock(cell, tools, id);
            case "counterblock" -> doCounterBlock(cell, tools, id);
            case "commandblock" -> doCmdBlock(cell, tools, id);
            case "conditionblock" -> doConditionBlock(cell, tools, id);
            case "delayblock" -> doDelayBlock(cell, tools, id);
            case "errorblock", "warnblock", "infoblock" -> doLogBlock(cell, tools, id);
            case "emailblock" -> doEmailBlock(cell,tools,id);
            case "flagblock" -> doFlagBlock(cell, tools, id);
            case "outputpin" -> doOutputPin(cell, tools, id);
            case "triggergateblock" -> doTriggerGateBlock(cell, tools, id);
            case "intervalblock" -> doIntervalBlock(cell, tools, id);
            case "mathblock" -> doMathBlock(cell, tools, id);
            case "originblock" -> doOriginBlock(cell);
            case "readerblock" -> doReaderBlock(cell, tools, id);
            case "splitblock" -> doSplitBlock(cell, tools, id);
            case "writerblock" -> doWritableBlock(cell, tools, id);

            default -> null;
        };
    }
```
As you can see, each block has a method. These all follow the same pattern.

* Check if required parameters are available, if not log and stop.
* Try building the block with those parameters, if not log (done at source of issue) and stop.
* Give an id (explained later) to the current block,  used to back annotate the drawio.
* Check if there are arrows for the next and altRoute based on the labels given.

Below for `ConditionBlock`:
```java
    private static ConditionBlock doConditionBlock(Drawio.DrawioCell cell, TaskTools tools, String id) {
        var blockId = alterId(id);
        Logger.info(blockId + " -> Processing Conditionblock");

        if (!cell.hasParam("expression")) {
            Logger.error(blockId + " -> ConditionBlock without an expression specified.");
            return null;
        }
        var cb = ConditionBlock.build(cell.getParam("expression", ""), tools.rtvals, null);
        cb.ifPresent(block -> {
            block.id(blockId);
            var target = cell.getArrowTarget("update");
            if (target != null && target.type.equals("flagval")) {
                var flagId = target.getParam("group", "") + "_" + target.getParam("name", "");
                tools.rtvals().getFlagVal(flagId).ifPresent(block::setFlag);

                // Grab the next of the flagval and make it the next of the condition
                if (target.hasArrow("next")) {
                    tools.blocks.put(cell.drawId, block);
                    if (block.id().isEmpty())
                        Logger.info("Id still empty?");
                    tools.idRef.put(block.id(), cell.drawId);
                    if (target.hasArrow("next")) {
                        var targetCell = target.getArrowTarget("next");
                        var targetId = targetCell.drawId;
                        var existing = tools.blocks.get(targetId);
                        if (existing == null) { // not created yet
                            block.addNext(createBlock(targetCell, tools, block.id()));
                        } else {
                            block.addNext(existing);
                        }
                    }
                }
            } else {
                addNext(cell, block, tools, "next", "pass", "yes", "ok", "true");
                addAlt(cell, block, tools, "fail", "no", "false", "failed");
            }
        });
        return cb.orElse(null);
    }
```
To explain the addNext method.

* Uses String... next to make it easier to expand the list of approved labels for this block.
* Fills in two HashMaps 
  * blocks - Uses drawio id as key and the build block as value, mainly used to check if a block exists or not
  * idRef - Lookup table to go from drawio id to meles id.
* Prevent endless looping by checking if the `DrawioCell` about to be converted already has a block counterpart or not. 
  * If so, use that instead of building a new one and backtrack.
  * If not, create the next one and give the reference to the current block once recursion is done.

```java
    private static boolean addNext(Drawio.DrawioCell cell, AbstractBlock block, TaskTools tools, String... nexts) {

        tools.blocks.put(cell.drawId, block);
        if (block.id().isEmpty())
            Logger.info("Id still empty?");
        tools.idRef.put(block.id(), cell.drawId);
        for (var next : nexts) {
            if (cell.hasArrow(next)) {
                var targetCell = cell.getArrowTarget(next);
                var targetId = targetCell.drawId;
                var existing = tools.blocks.get(targetId);
                if (existing == null) { // not created yet
                    block.addNext( createBlock(targetCell, tools, block.id()) );
                } else {
                    block.addNext(existing);
                }
                return true;
            }
        }
        return false;
    }
```
The addAlt method is pretty much the same except for how the id is given. The reason for the added pipe (|) is 
explained in the next section.

```java
private static boolean addAlt(Drawio.DrawioCell cell, AbstractBlock block, TaskTools tools, String... labels) {
        tools.blocks.put(cell.drawId, block);
        for (var label : labels) {
            if (cell.hasArrow(label)) {
                var id = block.id();
                id = id + "|";
                tools.idRef.put(block.id(), cell.drawId);

                var targetCell = cell.getArrowTarget(label);
                var targetId = cell.getArrowTarget(label).drawId;
                var existing = tools.blocks.get(targetId);
                if (existing == null) {
                    block.setAltRouteBlock(createBlock(targetCell, tools, id));
                } else {
                    block.setAltRouteBlock(existing);
                }
                return true;
            }
        }
        return false;
    }
```

Below an example of the shapes processing order is shown.

* If infinite looping wasn't prevented, three would also be six,nine,twelve and so on.
* The actual six, now is the first alternative route taken.
* Backtracking result in seven being processed as alternative route from four.
* Moving back even further till two, adds the alternative route to eight and nine.

<insert image>

## Giving blocks an id

I wanted to give each block an id which at the same time gave info on where it can be found in the diagram. This was made harder because:

* I wanted it done while the sequence was being converted. (Although I forgot why...)
* It needs to be clear if it's the main route or the alternate one
* It needs to differentiate between an alternate route and the result of the SplitBlock. Normally blocks are 'one-to-two' at most (next/alt), splitblocks allow 'one-to-many'.

### How it works

Starting point is the id given to the `OriginBlock` by the user.

The next block in the sequence will get @ appended followed by the index (starting at 0) in the main route. If an 
alternate route is started, this appends a | (pipe) to the id of the starter and again starts counting from 0. If 
another alternative is branching off, another | (pipe) is added and so on.

Below is an example showing the id's on each block (in tiny letters). The sequence starts at the `OriginBlock` with the 
id 'simpletask' and splits on the purple `Counterblock`, the alternate route goes up while main route goes down.

Hence the orange block on top gets the id of the `CounterBlock` 'simpletask@1' with |0 appended to show it's the start 
of an alternative route.

<insert image>

Fairly straightforward(?). Until you introduce that splitblock... I needed a way do differentiate but didn't want to 
add another splitter. So I finally decided to represent the SplitBlock with brackets similar to an array.

Below is a screenshot showing a subset of a task called blocktask (this was actually the first task made).

The green IntervalTask (blocktask@0) alternative route goes to a splitblock. Which means it gets the id 
blocktask@0|[0].

To decode this:
* blocktask@0 is the parent
* | alternate route
* [0] first block in this route and it's a SplitBlock

<insert image>

To decode the bright red Error block with id blocktask@0|[0][3]|0|0
* blocktask@0|[0] this is already known and points to the SplitBlock.
* [3] - It's the third option of the SplitBlock (note the numbers on the arrows, user skipped 0).
* |0 - First block of that SplitBlock route (up to here is the id of that Conditionblock).
* |0 - Alternate/fail route, first block again.

The code for this is seems fairly simple (but it took me a couple hours). Simplicity is mainly because we made it so 
that the parser will first traverse the main route before backtracking and doing the alternative routes one by one.

The alterId method is called just before that addNext/addAlt method so a block gets an id before moving on. (see the 
earlier shown addConditionBlock). The id given is that of the previously created block.

```java
    private static String alterId(String id) {
        return alterId(id, "", "");
    }

    private static String alterId(String id, String prefix, String suffix) {
        if (!id.contains("@")) // No @ yet, so first block after origin
            return id + "@" + prefix + "0" + suffix;

        if (id.endsWith("|")) // Ends with | so first block in alt route
            return id + prefix + "0" + suffix;

        var splitter = id.contains("|") ? "|" : "@";
        // Split the id on the last occurence of splitter (so lastIndex version of split)
        var split = Tools.endSplit(id, splitter); 
        // Put it back together adding prefix/suffix
        return split[0] + splitter + prefix + (NumberUtils.toInt(split[1]) + 1) + suffix;
    }
 ```

(The prefix/suffix is to handle the SplitBlock id's. )

First blocks in a (alternative) route are easily found:

* If there's no @ present, it's the first block after OriginBlock, so add @0 or @[0] if SplitBlock, return.
* If the id ends with a pipe (|). This means the addAlt method added it to the id of starter of an alternative route 
(not the block itself, just what is passed to the createBlock method). Using that, this method knows that the id is
generated for the first block in that alternative route so just add a 0.

The parser always traverses to the end once it started a route. This means it's now just a matter of:

* Splitting the id on the last pipe (|), if there's already one present, or @ if not.
* Parsing the second portion to a number and incrementing it.
* Putting the two portions back together, adding prefix/suffix if provided.

## Conclusion

At this point:

* Drawio xml is be parsed to a hashmap of DrawioCell's, which other classes use for further conversion.
  * TaskParser is the first of these, converting the DrawioCell's to task Blocks. 
* The shapes that represent task blocks can be given id's.  Meaning the drawio id's are no longer needed.
* Tasks can now be created using a diagram instead of plain XML.