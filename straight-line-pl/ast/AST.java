package ast;

public class AST {
    public sealed interface Stm permits CompoundStm, AssignStm, PrintStm {}
    public sealed interface Exp permits IdExp, NumExp, OpExp, EseqExp {}
    public sealed interface ExpList permits PairExpList, LastExpList {}

    public enum Operator {
        PLUS, MINUS, TIMES, DIV
    }

    public record CompoundStm(Stm stm1, Stm stm2) implements Stm {}
    public record AssignStm(String id, Exp exp) implements Stm {}
    public record PrintStm(ExpList expList) implements Stm {}

    public record IdExp(String id) implements Exp {}
    public record NumExp(int num) implements Exp {}
    public record OpExp(Exp left, Operator op, Exp right) implements Exp {}
    public record EseqExp(Stm stm, Exp exp) implements Exp {}

    public record PairExpList(Exp head, ExpList tail) implements ExpList {}
    public record LastExpList(Exp head) implements ExpList {}
}
