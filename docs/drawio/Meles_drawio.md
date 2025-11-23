# Meles Drawio configuration layer

Meles contains a parser to read Draw.io diagrams. This enables the user to define 'tasks' normally configured through XML in flow charts.
Given it's a parser, no compilation is done nor are intermediate files generated and it's possible to reload at runtime (but no hotplugging).

Each drawn shape corresponds to a module in source code, but this link is made based on the parameters set in the shape, not the visual properties.
Allowing the user (or company) to choose their own visual style.

For now, drawio is a one way. Meles can alter a file (and does to give each shape an id), but for now that's the only thing it can do.

## Advantages of using Draw.io

### Libraries
Meles comes with default libraries containing all shapes for all the different modules, these have a suggested look. The user is free to make their own based on these as a starting 
point, it's just a matter of copying the parameters.

### File format
A drawio file is just XML with a different extension. The format involves nodes that contain all info partaining to a shape, which means that the files are readable ascii and versioning is possible. 

### Development
This can be seen as both a negative and a positive. The positive being that development of Meles isn't halted to improve the GUI. The downside is that i have no control over it, if they
decide to alter the format of the files. But given the statement on their website that they haven't changed the format yet, lessens this risk significantly.

## Features

### Task Blocks

TaskManager blocks have shape alternatives, the goal is to keep this 'low level' meaning give the user basic functionality and thus flexibility.
This is the 'active' side, meaning the user determines when a sequence of blocks is started.
* Time: Add delays and interval or time of day based triggers.
* Read from any source that connects to Meles and wait for specific data (various options for checking)
* Write to any target that is connected to Meles.
* `Condition block` adds branching logic based on realtime data, allows to jump forward or back in a sequence or to another one.
* Counters : Just simple down counting and act (or don't) on reaching 0.
* Command block can issue any command the user could input in telnet. Anythinh possible in the Meles CLI can be triggered.
* Email, send emails and have the reader check the content of received emails
* Normally each shape connects to one other shape per arrow. Split block allows 'one to many' and this concurrently, in sequence or at random.
* Logblock allows for messages to be written to logs, these come in the standard levels (info, warn, error) and can contain placeholders for variables.
* Debounce inputs with a trigger gate block, this prevents succesive triggers to be processed or only after a delay.


### GPIO Blocks
This blends the active and reactive side. GPIO blocks can be mixed with task blocks or be the start of a sequence of taskblocks.
Both the input and the output block inherent the functionality of a flag in Meles, thus both input and output can trigger based on state changes.

If an output gets triggered, the user can decide what happens next based on the state befor the trigger. Meaning:
- High to low
- Low to High
- Stay low
- Stay High

Are distinct options for a new sequence.

Given an input is hardware, only edges are available. Inputs come with built in debouncing (customizable period).

### Rtval Blocks

This is the reactive side and allows for deriving data. This configures how/if Meles responds to any change of a variable.
It allows for defining  a 'base' variable that triggers a cascading effect of other rtvals being updated. This is combined with
the ability to apply math operations before the base value is updated and using both old and new value in conditional checks.