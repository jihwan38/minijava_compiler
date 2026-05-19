class TestUndeclared {
    public static void main(String[] a){
        System.out.println(new A().foo());
    }
}

class A extends B { // 1. 미선언 클래스 상속

    C myObj;        // 2. 미선언 클래스를 타입으로 사용

    public int foo() {
        int x;
        
        x = y;      // 3. 미선언 변수 사용
        z = 10;     // 4. 미선언 변수 사용
        a = b;      // 5, 6. 양쪽 모두 미선언 변수 사용
        
        return new D().bar(); // 7. 미선언 클래스 인스턴스화
    }
}
