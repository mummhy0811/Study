# 스프링 핵심 원리

## 스프링이란?
- 자바 언어 기반의 프레임워크
-  __객체 지향 언어__ 가 가진 강력한 특징을 살려냄
- \>> 좋은 객체 지향 애플리케이션을 개발할 수 있게 돕는 프레임워크 <<

<br>
<hr>
<br>

## 제어의 역전 IoC(Inversion of Control)
- 프로그램의 __제어 흐름__ 을 직접 제어하는 것이 아니라 __외부에서 관리__ 하는 것을 뜻함
- 기존 프로그램은 클라이언트 구현 객체가 스스로 필요한 서버 구현 객체를 생성하고, 연결하고 실행(구현 객체가 프로그램의 제어 흐름을 스스로 조종함)
- ``` AppConfig```가 등장한 후, 구현 객체는 자신의 로직을 실행하는 역할만 담당하게 되고 제어 흐름에 대한 모든 권한은 AppConfig에 부여

<br>

## 의존관계 주입 DI(Dependency Injection)
- 애플리케이션 실행 시점(런타임)에 외부에서 실제 구현 객체를 생성하고 클라이언트에 전달해서 클라이언트와 서버의 실제 의존관계가 연결되는 것을 ``의존관계 주입`` 이라고 함
- 객체 인스턴스를 생성하고, 그 참조값을 전달해서 연결됨
- 클라이언트 코드를 변경하지 않고, 클라이언트가 호출하는 대상의 타입 인스턴스 변경 가능
- 정적인 클래스 의존관계를 변경하지 않고, 동적인 객체 인스턴스 의존관계를 쉽게 변경 가능

💥 참고    

    ✔ 정적인 클래스 의존관계   
    - 클래스가 사용하는 import 코드만 보고 의존관계 파악 가능   
    - 애플리케이션을 실행하지 않아도 분석 가능   
    ✔ 동적인 객체 인스턴스 의존관계   
    - 애플리케이션 실행 시점에 실제 생성된 객체 인스턴스의 참조가 연결된 의존 관계 ( -> appConfig에서 일어나는 동작같은?)   
 

## IoC 컨테이너, DI 컨테이너
- 객체를 생성하고 관리하면서 의존관계를 연결해주는 것을 IoC컨테이너 또는 DI컨테이너라고 함
- 어샘블러, 오브젝트 팩토리 등으로 불리기도 함

<br>
<hr>
<br>

## 스프링 컨테이너
- ```ApplicationContext``` 를 스프링 컨테이너라고 함
- 스프링 컨테이너는 ```@Configuration``` 이 붙은 appConfig를 구성 정보로 사용하며, 여기서 ```@Bean``` 이 붙은 메서드를 모두 호출하여 반환된 객체를 스프링 컨테이너에 등록함
- 스프링 컨테이너에 등록된 객체를 __스프링 빈__ 이라고 함
- 스프링 빈은 @Bean이 붙은 메서드의 명을 스프링 빈의 이름으로 사용
- ```applicationContext.getBean()``` 메서드를 통해 필요한 스프링 빈(객체)를 찾음

<br>

### 스프링 컨테이너 생성
``` java
ApplicationContext applicationContext = new AnnotationCongifApplicationContext(AppConfig.class);
```
- ApplicationContext는 인터페이스
- 스프링 컨테이너는 XML을 기반으로 만들 수 있고, 애노테이션 기반의 자바 설정 클래스로도 만들 수 있음
- ```AppConfig```를 사용했던 방식이 애노테이션 기반의 자바 설정 클래스로 스프링 컨데이너를 만든 것.

### 과정
1. ```new AnnotationCongifApplicationContext(AppConfig.class)```를 통해 AppConfig정보를 파라미터로 넘기며(구성 정보를 지정해주며) __스프링 컨테이너 생성__
2. 스프링 컨테이는 파라미터로 넘어온 설정 클래스 정보를 사용하여 __스프링빈 등록__.  (```@Bean``` 탐색)
    - ```@Bean```을 찾아가 빈 이름에는 메서드 이름을, 빈 객체에는 리턴값을 저장
    - ```@Bean(name="memberService2")``` 와 같이 빈 이름 직접 부여 가능 -> 빈 이름이 중복되지 않도록 주의 필요
3. 스프링 컨테이너는 설정 정보를 참고해서 __의존관계를 주입(DI)__ 함. (@Bean 리턴값의 파라미터를 통해)

<br>

## 스프링빈 조회 - 테스트코드로
1. 모든 빈 출력
    - 스프링에 등록된 모든 빈 정보를 출력
    - ```ac.getBeanDefinitionNames()``` : 스프링에 등록된 모든 빈 이름을 조회
    - ```ac.getBean()``` : 빈 이름으로 빈 객체(인스턴스)를 조회
2. 애플리케이션 빈 출력
    - 내가 등록한 빈만 출력
    - ROLE_APPLICATION : 일반적으로 사용자가 정의한 빈
    - ROLE_INFRASTRUCTURE : 스프링이 내부에서 사용하는 빈
    ``` java
        if (beanDefinition.getRole() == BeanDefinition.ROLE_APPLICATION) { 
            Object bean = ac.getBean(beanDefinitionName);
            System.out.println("name=" + beanDefinitionName + " object=" +bean);
        }
    ```
3. 기본 조회
    - 빈 이름으로 조회: ```ac.getBean(빈 이름, 타입)```
    - 빈 이름 없이 타입으로 조회: ```ac.getBean(타입)```
    - 조회 대상 스프링빈이 없으면 예외 발생 -> ```NoSuchBeanDefinitionException: No bean named 'xxxxx' available```
    - 예외 조회(예외가 발생해야 테스트 통과) -> ```Assertions.assertThrows(NoSuchBeanDefinitionException.class, () -> ac.getBean("xxxxx", MemberService.class));```   
4. 동일한 타입이 둘 이상인 타입조회
    - 타입으로 조회시 같은 타입의 스프링 빈이 둘 이상이면 오류가 발생 (```NoUniqueBeanDefinitionException```)-> 빈 이름을 지정하면 됨
    - ```ac.getBeansOfType()``` : 해당 타입의 모든 빈 조회
    ```java
        Map<String, MemberRepository> beansOfType = ac.getBeansOfType(MemberRepository.class);
        System.out.println("beansOfType = " + beansOfType);
    ```
5. 상속관계인 경우의 조회
    - 부모 타입으로 조회하면 자식 타입도 함께 조회됨
    - 자바 객체 최고의 부모인 ```Object```타입으로 조회하면 모든 스프링빈 조회 가능
    - 부모 타입으로 조회시, 자식이 둘 이상이면 중복오류 발생 -> 빈 이름 지정 또는 특정 하위타입으로 조회 필요

