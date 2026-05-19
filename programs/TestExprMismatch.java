class TestExprMismatch {
    public static void main(String[] a){
        System.out.println(new ExprTester().testMethod());
    }
}

class A {}
class B extends A {}

class ExprTester {
    public int testMethod() {
        int i;
        boolean b;
        int[] arr;
        A objA;

        arr = new int[5];
        objA = new B();

        // 1. Plus
        i = 1 + true; 
        // 2. Minus
        i = 1 - true;
        // 3. Times
        i = 1 * true;
        // 4. LessThan
        b = 1 < true;
        // 5. And
        b = true && 1;
        // 6. Not
        b = !1;
        // 7. ArrayLookup index
        i = arr[false];
        // 8. ArrayLookup array
        i = i[0];
        // 9. ArrayLength
        i = i.length;
        // 10. NewArray size
        arr = new int[true];
        // 11. Call arg count
        i = this.foo(1, 2);
        // 12. Call arg type
        i = this.foo(true);
        // 13. Call non-class
        i = i.foo(1);

        return 0;
    }

    public int foo(int x) {
        A objA;
        objA = new A();
        // 14. Call missing method
        return objA.unknownMethod();
    }
}
