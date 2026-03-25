import ast.AST.*;
import interpreter.Interpreter;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {

        Stm stm1 = new AssignStm("a",
                new OpExp(new NumExp(5), Operator.PLUS, new NumExp(3))
        );

        Stm stm2 = new AssignStm("b",
                new EseqExp(
                        new PrintStm(
                                new PairExpList(
                                        new IdExp("a"),
                                        new LastExpList(new OpExp(new IdExp("a"), Operator.MINUS, new NumExp(1)))
                                )
                        ),
                        new OpExp(new NumExp(10), Operator.TIMES, new IdExp("a"))
                )
        );

        Stm stm3 = new PrintStm(
                new LastExpList(new IdExp("b"))
        );

        Stm prg = new CompoundStm(stm1, new CompoundStm(stm2, stm3));

        HashMap<String, Integer> env = new HashMap<>();

        Interpreter.interpStm(prg, env);
    }
}