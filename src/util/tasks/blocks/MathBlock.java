package util.tasks.blocks;

import util.data.procs.MathEvalForVal;

public class MathBlock extends AbstractBlock {
    MathEvalForVal eval;

    public MathBlock(MathEvalForVal eval) {
        this.eval = eval;
    }
    public String type(){ return "MathBlock";}
    public static MathBlock build(MathEvalForVal eval) {
        return new MathBlock(eval);
    }

    @Override
    public boolean start() {
        eval.eval(0, 0, 0);
        clean = false;
        doNext();
        return true;
    }
}
