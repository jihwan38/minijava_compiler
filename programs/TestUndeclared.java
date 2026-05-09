class TestUndeclared {
    public static void main(String[] a){
        System.out.println(new A().foo());
    }
}

class A extends B { // 1. 미선언 클래스 상속

    C myObj;        // 2. 미선언 클래스를 타입으로 사용

    public int foo() {
        int x;
        
        x = y;      // 3. 미선언 식별자(변수) 사용 (수식 내)
        z = 10;     // 4. 미선언 식별자(변수) 사용 (대입문 좌항)
        
        return new D().bar(); // 5. 미선언 클래스 인스턴스화
    }
}
