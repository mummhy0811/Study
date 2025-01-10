# [섹션2] 
## 회원 등록 API
### 엔티티는 파라미터로 받지 않는다.
``` java
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member){
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }
```
- 엔티티를 수정했을 때 API 스펙이 변경되는 문제 발생 **(@Valid Member member)** <br>
 +) 엔티티를 **외부에 노출해서는 안 되기**도 함.
- 따라서, API 스펙을 위한 **DTO**를 만들어야 한다. 

``` java
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request){
        Member member = new Member(); // 객체를 만들고
        member.setName(request.getName()); //DTO에서 만든 이름을 매핑
        
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }
    
    @Data
    static class CreateMemberRequest{
        private String name;
    }
```
- 엔티티와 API 스펙을 명확하게 분리할 수 있다. 
- 엔티티가 변해도 API 스펙이 변하지 않는다.
- DTO를 통해 데이터를 받으면 주고 받는 값이 명확해진다.
- API 스펙에 맞추어 데이터를 핏하게 주고받을 수 있다.

## 회원 수정 API
### update함수에서 return은 void
``` java
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id, @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }
    
    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }
```
- DTO에는 롬복 annotation 많이 사용해도 무방


``` java
    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findOne(id);
        member.setName(name);
    }
```
- 커맨드(update)와 쿼리(search)를 분리하기 위해.
- update후 해당 멤버를 그대로 리턴하면 변경성 메소드라는 취지에 맞지 않다.
- id정도를 리턴하는 것은 무방.
- 특별하게 트래픽이 많은 API가 아니면 이슈가 되지 않는다.


## 회원 조회 API
### 엔티티를 그대로 리턴하지 않는다.
```java
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }
```

- 엔티티의 모든 값이 노출된다. **(위험)**
- 응답 스펙을 맞추기 위해 로직이 추가된다. (@JsonIgnore 등)
  - 엔티티에 프레젠테이션 로직이 들어가는 것은 좋지 않다. (의존성 부분에서 최악.)
- 엔티티가 변경되면 API 스펙이 변경된다.
- 컬렉션을 직접 반환하면(array 등) 항후 API 스펙을 변경하기 어렵다.

**<결론>** 
- API 응답 스펙에 맞추어 별도의 DTO를 반환한다. 

### 응답 DTO를 이용해 리턴한다.
```java
    @GetMapping("/api/v2/members")
    public Result membersV2() {

        List<Member> findMembers = memberService.findMembers();
        //엔티티 -> DTO 변환
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
    
    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }
```
- 엔티티를 DTO로 변환해서 반환한다. 
- 엔티티가 변해도 API 스펙이 변경되지 않는다. 
- `Result` 클래스로 컬렉션을 감싸게 되면서, 향후 리턴값 확장이 자유롭다.

<br>
<br>

# [섹션4] 
## 주문 조회 V1: 엔티티 직접 노출
```java
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        return all;
    }
```
### 🚨[문제] 무한루프에 빠지게 된다. (Order → Member → Order → ...)
  ### 해결 방법1. jsonIgnore
  - ```fetch = LAZY``` 이기 때문에 오류(프록시 객체를 해결하지 못해)
  - ```fetch = LAZY``` : 즉시 객체를 가져오지 않고, PROXY 객체를 생성해서 넣어둠.(ByteBuddyInterceptor가 들어가있음)
  ### 해결 방법2. 강제 지연 로딩
  ``` java
        // build.gradle
        implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5'
    
        @Bean
        Hibernate5Module hibernate5Module() {
            Hibernate5Module hibernate5Module = new Hibernate5Module();
            //강제 지연 로딩 설정
            hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true);
            return hibernate5Module;
        }}
    
   ```
  - ```Hibernate5Module``` 모듈 등록 → 강제로 Lazy Loading
### 해결 방법 3. 선택 강제 로딩
``` java
      @GetMapping("/api/v1/simple-orders")
          public List<Order> ordersV1() {
          List<Order> all = orderRepository.findAllByString(new OrderSearch());
          for (Order order : all) {
              order.getMember().getName(); //Lazy 강제 초기화
              order.getDelivery().getAddress(); //Lazy 강제 초기화
          }
          return all;
      }
   ```
- 초기화 된 것은 값, 안 된 것은 null값

### 🚨[문제] 엔티티 그대로 노출
### 🚨 [문제] 필요 없는 데이터 → 조회 성능 저하

## 주문 조회 V2: DTO 변환
```java
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        return orderRepository.findAll() //데이터 검색
                .stream() //변환
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); //LAZY 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); //LAZY 초기화
        }
    }
```
- 리턴용 DTO를 만든 후 변환하여 리턴한다.
  - 엔티티 노출 방지 가능
### 🚨[문제] Lazy loading으로 인해 발생하는 N+1 문제 (v1, v2 공통)
> N+1 이란? <br>
> 첫 쿼리를 위해 부가 쿼리 N번이 추가적으로 발생하는 현상
``` java
2025-01-10 17:36:56.384 DEBUG 23268 --- [nio-8080-exec-2] org.hibernate.SQL                        : 
    select
        order0_.order_id as order_id1_6_,
        order0_.delivery_id as delivery4_6_,
        order0_.member_id as member_i5_6_,
        order0_.order_date as order_da2_6_,
        order0_.status as status3_6_ 
    from
        orders order0_
//----------------------------불필요한 LAZY 쿼리 --------------------------
2025-01-10 17:36:56.440 DEBUG 23268 --- [nio-8080-exec-2] org.hibernate.SQL                        : 
    select
        member0_.member_id as member_i1_4_0_,
        member0_.city as city2_4_0_,
        member0_.street as street3_4_0_,
        member0_.zipcode as zipcode4_4_0_,
        member0_.name as name5_4_0_ 
    from
        member member0_ 
    where
        member0_.member_id in (?, ?)
2025-01-10 17:36:56.454 DEBUG 23268 --- [nio-8080-exec-2] org.hibernate.SQL                        : 
    select
        delivery0_.delivery_id as delivery1_2_0_,
        delivery0_.city as city2_2_0_,
        delivery0_.street as street3_2_0_,
        delivery0_.zipcode as zipcode4_2_0_,
        delivery0_.status as status5_2_0_ 
    from
        delivery delivery0_ 
    where
        delivery0_.delivery_id in (?, ?)
```
- 의도한 쿼리보다 더 많은 쿼리가 사용됨.
- ``` orderRepository.findAll()```: Order 조회 -> SQL 2회 -> Order가 2개 조회됨.
- ```stream```: { 첫 주문서에 member쿼리, delivery 쿼리 생성 -> simpleOrderDto 생성 } x n
> 한 api 조회에 쿼리가 5번이 나감 <br>
> order가 10개라면? 21번 나감 <br>
> **1 + 회원 N + 배송 N (최악의 경우)**
- **최악의 경우**인 이유?
  - 지연 로딩은 **영속성 컨텍스트**를 기반으로 하기 때문에, 이미 조회된 쿼리의 경우 실행하지 않는다.

## 💡 주문 조회 V3: 페치 조인 최적화 💡
### query를 이용해 join fetch로 한 번에 조회
```java
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        return orderRepository.findAllWithMemberDelivery().stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }
```
- order와 member와 delivery를 join해서 한 번에 가져옴
- fetch는 JPA에만 있는 문법
``` java
2025-01-10 18:01:52.091 DEBUG 6200 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        order0_.order_id as order_id1_6_0_,
        member1_.member_id as member_i1_4_1_,
        delivery2_.delivery_id as delivery1_2_2_,
        order0_.delivery_id as delivery4_6_0_,
        order0_.member_id as member_i5_6_0_,
        order0_.order_date as order_da2_6_0_,
        order0_.status as status3_6_0_,
        member1_.city as city2_4_1_,
        member1_.street as street3_4_1_,
        member1_.zipcode as zipcode4_4_1_,
        member1_.name as name5_4_1_,
        delivery2_.city as city2_2_2_,
        delivery2_.street as street3_2_2_,
        delivery2_.zipcode as zipcode4_2_2_,
        delivery2_.status as status5_2_2_ 
    from
        orders order0_ 
    inner join
        member member1_ 
            on order0_.member_id=member1_.member_id 
    inner join
        delivery delivery2_ 
            on order0_.delivery_id=delivery2_.delivery_id

```
- 쿼리가 1번으로 줄어든 모습!