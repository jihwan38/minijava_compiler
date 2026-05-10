class TestOverload {
    public static void main(String[] a){
        System.out.println(new Child().validOverride(10, true));
    }
}

class Parent {
    public int foo(int a) { return a; }
    public boolean bar() { return true; }
    public int baz(int x) { return x; }
    public int qux(int a, int b) { return a + b; }
    public int validOverride(int a, boolean b) { return 1; }
}

class Child extends Parent {
    public boolean foo(int a) { return true; }     // 1. 리턴 타입 다름 (int -> boolean)
    public boolean bar(int x) { return true; }     // 2. 파라미터 개수 다름 (0개 -> 1개)
    public int baz(boolean x) { return 1; }        // 3. 파라미터 타입 다름 (int -> boolean)
    public int qux(int a, boolean b) { return a; } // 4. 파라미터 중 하나의 타입 다름
    public int validOverride(int a, boolean b) { return 2; }
}

class GrandParent {
    public int grandMethod(int a) { return 1; }
}

class Parent2 extends GrandParent {
}

class Child2 extends Parent2 {
    public int grandMethod(boolean a) { return 1; } // 5. 조부모 클래스의 메서드를 오버로딩
}
