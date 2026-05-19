class TestTypeMismatch {
    public static void main(String[] a){
        System.out.println(new Tester().testMethod());
    }
}

class A {}
class B extends A {}
class C {}

class Tester {
    public int testMethod() {
        int i;
        boolean b;
        int[] arr;
        A objA;
        B objB;
        C objC;

        arr = new int[5];
        objA = new B(); // B는 A의 서브타입이므로 대입 가능 (isSubtype 테스트)

        // 1. 산술 연산자에 잘못된 타입 사용 (int + boolean)
        i = 1 + true; 

        // 2. 조건문에 잘못된 타입 사용 (if 조건에 int)
        if (1) { i = 1; } else { i = 2; } 

        // 3. 변수 대입 시 타입 불일치 (boolean 변수에 int 대입)
        b = 1;

        // 4. 배열 할당 크기에 잘못된 타입 사용 (new int[boolean])
        arr = new int[true];

        // 5. 배열 접근 시 인덱스 타입 오류 (arr[boolean])
        i = arr[false];

        // 6. 배열이 아닌데 배열 접근 시도 (int[0])
        i = i[0];

        // 7. 배열이 아닌데 length 호출 (int.length)
        i = i.length;

        // 8. 메서드 호출 시 인자 개수 불일치 (1개 필요한데 2개 줌)
        i = this.foo(1, 2);

        // 9. 메서드 호출 시 파라미터 타입 불일치 (int 필요한데 boolean 줌)
        i = this.foo(true);

        // 10. 관련 없는 클래스 간의 대입 (C 변수에 A 객체 대입)
        objC = objA;

        // 11. Print 문에 int가 아닌 타입 사용 (System.out.println(boolean))
        System.out.println(true);

        // 12. While 조건에 boolean이 아닌 타입 사용 (while(int))
        while (1) { }

        // 13. And 연산자에 boolean이 아닌 타입 사용 (boolean && int)
        b = true && 1;

        // 14. LessThan 연산자에 int가 아닌 타입 사용 (int < boolean)
        b = 1 < true;

        // 15. Not 연산자에 boolean이 아닌 타입 사용 (!int)
        b = !1;

        // 16. 객체가 아닌 기본형(int)에서 메서드 호출 시도 (int.foo())
        i = i.foo(1);

        // 17. 배열 원소 대입 시 인덱스나 값의 타입 오류 (int 배열에 boolean 대입)
        arr[0] = true;

        // 18. Minus 연산자에 잘못된 타입 사용 (int - boolean)
        i = 1 - true;

        // 19. Times 연산자에 잘못된 타입 사용 (int * boolean)
        i = 1 * true;

        // 20. 메서드 리턴 타입 불일치 (int 리턴해야 하는데 boolean 리턴)
        return true;
    }

    public int foo(int x) {
        // 21. 존재하지 않는 메서드 호출 (objA.unknownMethod())
        A objA;
        objA = new A();
        return objA.unknownMethod();
    }
}
