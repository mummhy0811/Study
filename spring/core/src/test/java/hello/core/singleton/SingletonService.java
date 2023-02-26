package hello.core.singleton;

public class SingletonService {

    private static final SingletonService instance = new SingletonService();
    //private static으로 선언 -> 자기 안에 딱 하나만 존재. 객체를 생성해서 instance에 참조를 넣어둠.

    public static SingletonService getInstance(){ //조회
        return instance;
    }

    private SingletonService(){ }
}
