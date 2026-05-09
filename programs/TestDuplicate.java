class TestDuplicate {
    public static void main(String[] a){
        System.out.println(new Fac().ComputeFac(10));
    }
}


class Fac {

    int num;
    int num; // 1. 중복 필드 변수

    public int ComputeFac(int num) {
        int num_aux;
        int num_aux; // 2. 중복 지역 변수
        
        int num; // 3. 중복 지역 변수 (파라미터 이름과 겹침)
        
        return num_aux;
    }
    
    public int ComputeFac(int num) { // 4. 중복 메서드
        return 0;
    }

}

class Fac { // 5. 중복 클래스
}
