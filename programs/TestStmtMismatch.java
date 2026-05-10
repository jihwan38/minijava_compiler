class TestStmtMismatch {
    public static void main(String[] a){
        System.out.println(new StmtTester().testMethod());
    }
}

class A {}
class C {}

class StmtTester {
    public int testMethod() {
        int i;
        boolean b;
        int[] arr;
        A objA;
        C objC;

        arr = new int[5];
        objA = new A();

        // 1. If
        if (1) { i = 1; } else { i = 2; } 
        // 2. While
        while (1) { }
        // 3. Print
        System.out.println(true);
        // 4. Assign primitive
        b = 1;
        // 5. Assign class
        objC = objA;
        // 6. ArrayAssign
        arr[0] = true;

        // 7. Return type mismatch
        return true;
    }
}
