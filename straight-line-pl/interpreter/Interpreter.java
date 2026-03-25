package interpreter;

import ast.AST.*;
import java.util.HashMap;
import java.util.ArrayList;

public class Interpreter {
    public static void interpStm(Stm stm, HashMap<String, Integer> env) {
        switch (stm) {
            case CompoundStm(Stm stm1, Stm stm2) -> {
                interpStm(stm1, env);
                interpStm(stm2, env);
            }
            case AssignStm(String id, Exp exp) -> {
                int value = interpExp(exp, env);
                env.put(id, value);
            }
            case PrintStm(ExpList expList) -> {
                ArrayList<Integer> results = interpExpList(expList, env);
                for (int i = 0; i < results.size(); i++) {
                    System.out.print(results.get(i) + (i < results.size() - 1 ? " " : ""));
                }
                System.out.println();
            }
        }
    }

    public static Integer interpExp(Exp exp, HashMap<String, Integer> env) {
        return switch (exp) {
            case IdExp(String id) -> env.getOrDefault(id, 0);
            case NumExp(int num) -> num;
            case OpExp(Exp left, Operator op, Exp right) -> {
                int leftVal = interpExp(left, env);
                int rightVal = interpExp(right, env);

                yield switch (op) {
                    case PLUS -> leftVal + rightVal;
                    case MINUS -> leftVal - rightVal;
                    case TIMES -> leftVal * rightVal;
                    case DIV -> leftVal / rightVal;
                };
            }
            case EseqExp(Stm stm, Exp e) -> {
                interpStm(stm, env);
                yield interpExp(e, env);
            }
        };
    }

    public static ArrayList<Integer> interpExpList(ExpList expList, HashMap<String, Integer> env) {
        ArrayList<Integer> list = new ArrayList<>();
        switch (expList) {
            case PairExpList(Exp head, ExpList tail) -> {
                list.add(interpExp(head, env));
                list.addAll(interpExpList(tail, env));
            }
            case LastExpList(Exp head) -> {
                list.add(interpExp(head, env));
            }
        }
        return list;
    }
}
