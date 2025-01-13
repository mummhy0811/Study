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

# [섹션4] 지연 로딩과 조회 성능 최적화
## 주문 조회 V1: 엔티티 직접 노출
```java
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        return all;
    }
```
#### 🚨[문제] 무한루프에 빠지게 된다. (Order → Member → Order → ...)
  #### 해결 방법1. jsonIgnore
  - ```fetch = LAZY``` 이기 때문에 오류(프록시 객체를 해결하지 못해)
  - ```fetch = LAZY``` : 즉시 객체를 가져오지 않고, PROXY 객체를 생성해서 넣어둠.(ByteBuddyInterceptor가 들어가있음)
  #### 해결 방법2. 강제 지연 로딩
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
#### 해결 방법 3. 선택 강제 로딩
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

#### 🚨[문제] 엔티티 그대로 노출
#### 🚨 [문제] 필요 없는 데이터 → 조회 성능 저하

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
#### 🚨[문제] Lazy loading으로 인해 발생하는 N+1 문제 (v1, v2 공통)
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

## 주문 조회 V4: JPA에서 DTO로 조회
``` java
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }
```
- 기존처럼 컨트롤러에서 매핑하는 것이 아니라 repo에서 직접 매핑하기 때문에 의존관계를 위해 DTO는 repo패키지에 작성한다.
  repository -> order -> simpleQuery -> OrderSimpleQueryDto
``` java
    @Data
    public class OrderSimpleQueryDto {
    
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
    
        public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
            this.orderId = orderId;
            this.name = name;
            this.orderDate = orderDate;
            this.orderStatus = orderStatus;
            this.address = address;
        }
    }
    
    -----
    @Repository
    @RequiredArgsConstructor
    public class OrderSimpleQueryRepository {
    
        private final EntityManager em;
    
        public List<OrderSimpleQueryDto> findOrderDtos() {
            return em.createQuery(
                    "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                            " from Order o" +
                            " join o.member m" +
                            " join o.delivery d", OrderSimpleQueryDto.class)
                    .getResultList();
        }
    }
```
- 일반적인 SQL을 사용할 때처럼 원하는 값을 선택해서 조회
  - DB -> application 네트워크 용량 최적화 가능(미비)
- new 명령어를 이용해서 JPQL의 결과를 DTO로 즉시 변환
### JPA -> DTO 변환 시 객체를 직접 넘기지 않는 이유
  - JPQL에서는 에니티 객체 전체를 DTO로 넘기는 것이 불가능하기 때문.
  - JPA의 식별자 문제 때문
    - JPA는 엔티티의 ID를 기준으로 동작
      - JPA는 @Entity 객체를 관리할 때 객체 자체가 아니라 **식별자(Primary Key)** 를 기준으로 관리
      - JPQL 쿼리를 통해 엔티티 객체를 통째로 반환하면 엔티티 객체와 연관된 데이터가 프록시로 남아 있거나 추가 쿼리를 발생시킬 가능성이 큼
  - 위의 상황에서, address의 경우 Embedded 타입이기 때문에 상관 없음.

``` java
2025-01-10 18:58:31.966 DEBUG 408 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        order0_.order_id as col_0_0_,
        member1_.name as col_1_0_,
        order0_.order_date as col_2_0_,
        order0_.status as col_3_0_,
        delivery2_.city as col_4_0_,
        delivery2_.street as col_4_1_,
        delivery2_.zipcode as col_4_2_ 
    from
        orders order0_ 
    inner join
        member member1_ 
            on order0_.member_id=member1_.member_id 
    inner join
        delivery delivery2_ 
            on order0_.delivery_id=delivery2_.delivery_id
```
- select절에서 원하는 컬럼만 가져오는 것 확인 가능

## JPA DTO 조회가 무조건 좋은가? 
> No! **TradeOff** 존재
- Fetch join으로 조회하는 것
    - 저장된 그대로의 모습을 가져오는 것
    - [👍🏻] 많은 API에서 재사용 가능
    - [👍🏻] 엔티티를 조회했기 때문에 비즈니스 로직에서 응용 가능
    - [👎🏻] 코드 변환 필요
- DTO로 조회하는 것
  - 외부의 모습을 건들인 상태
  - [👍🏻] 화면에 최적화 (but, repo가 화면에 의존한다는 단점 동시 존재)
  - [👍🏻] 필요한 컬럼만 가져옴(조금 더 성능 최적화)
  - [👎🏻] 재사용성 떨어짐
  - [👎🏻] DTO로 조회했기 때문에  비즈니스 로직에서 응용 불가능 (Read의 경우에 적합)

> 대부분의 경우는 두가지의 **성능이 크게 차이나지 않는다** (컬럼이 매우 많은 경우 제외)<br>
> 성능은 보통 **Join**에서 결정됨 <br> <br>
> 💡 나의 API 성질을 고려해서 선택하는 것이 중요 <br>
> ex) admin이면 DTO조회 굳이? / 실시간 유저 트래픽이 매우 많으면 고려 필요

- 참고
- DTO로 조회 시 의존성을 "조금"이나마 줄일 방법
  - DTO형태로 가져오는 코드들의 경로를 분리한다.
  - 리포지토리는 가급적 순수한 엔티티를 조회하는 용도로 사용하는 것이 좋기 때문.
  - `repository.order.simplequery`: 쿼리 맞춤용
  - `repository`: 엔티티 조회용


# [섹션5] 컬렉션 조회 최적화
## 주문 조회 V1: 엔티티 직접 노출
``` java
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll();
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기환
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName()); //Lazy 강제 초기화
        }
        return all;
    }
```
- orderItems도 get을 함으로써 강제 초기화
### 🚨[문제] 엔티티 그대로 노출

## 주문 조회 V2: DTO 변환
``` java
@GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {

        return orderRepository.findAll()// db 조회
                .stream()
                .map(OrderDto::new) // dto로 변환
                .collect(toList());
    }

    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(toList());
        }
    }

    @Data
    static class OrderItemDto {
        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
```
- 엔티티(Order) 내부의 엔티티(OrderItem)도 모두 DTO로 변경해야 한다.
### 🚨[문제] 지연 로딩으로 너무 많은 SQL 실행 
- SQL 실행 수 
  - `order` 1번 `member` 
  - `address` N번(order 조회 수 만큼) 
  - `orderItem` N번(order 조회 수 만큼) 
  - `item` N번(orderItem 조회 수 만큼)
> 참고 <br>
> 지연 로딩은 속성 컨텍스트에 있으면 속성 컨텍스트에 있는 엔티티를 사용하고 없으면 SQL을 실행한다.  <br>
> 따라서 같은 속성 컨텍스트에서 이미 로딩한 회원 엔티티를 추가로 조회하면 SQL을 실행하지 않는다. <br>


## 주문 조회 V3: 페치 조인 최적화
``` java
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {

        return orderRepository.findAllWithItem().stream()//fetch join으로 db 조회
                .map(OrderDto::new) // dto로 변환
                .collect(toList());
    }
    //--------------------------------------------------------------------
    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Order.class)
                .getResultList();
    }
```
### 🚨[문제] 중복 데이터 조회 발생
- 쿼리를 이용해 페치조인으로 데이터를 조회하게 되면 1대 다 조인으로 인해 데이터베이스 row가 증가한다.
- 그 결과, 같은 order 엔티티의 조회 수도 증가된다.
``` 
[
    {
        "orderId": 4,
        "name": "userA",
        "orderDate": "2025-01-13T16:24:38.174888",
        "orderStatus": "ORDER",
        "address": {
            "city": "서울",
            "street": "1",
            "zipcode": "1111"
        },
        "orderItems": [
            {
                "itemName": "JPA1 BOOK",
                "orderPrice": 10000,
                "count": 1
            },
            {
                "itemName": "JPA2 BOOK",
                "orderPrice": 20000,
                "count": 2
            }
        ]
    },
    {
        "orderId": 4,
        "name": "userA",
        "orderDate": "2025-01-13T16:24:38.174888",
        "orderStatus": "ORDER",
        "address": {
            "city": "서울",
            "street": "1",
            "zipcode": "1111"
        },
        "orderItems": [
            {
                "itemName": "JPA1 BOOK",
                "orderPrice": 10000,
                "count": 1
            },
            {
                "itemName": "JPA2 BOOK",
                "orderPrice": 20000,
                "count": 2
            }
        ]
    },
    {
        "orderId": 11,
        "name": "userB",
        "orderDate": "2025-01-13T16:24:38.259094",
        "orderStatus": "ORDER",
        "address": {
            "city": "진주",
            "street": "2",
            "zipcode": "2222"
        },
        "orderItems": [
            {
                "itemName": "SPRING1 BOOK",
                "orderPrice": 20000,
                "count": 3
            },
            {
                "itemName": "SPRING2 BOOK",
                "orderPrice": 40000,
                "count": 4
            }
        ]
    },
    {
        "orderId": 11,
        "name": "userB",
        "orderDate": "2025-01-13T16:24:38.259094",
        "orderStatus": "ORDER",
        "address": {
            "city": "진주",
            "street": "2",
            "zipcode": "2222"
        },
        "orderItems": [
            {
                "itemName": "SPRING1 BOOK",
                "orderPrice": 20000,
                "count": 3
            },
            {
                "itemName": "SPRING2 BOOK",
                "orderPrice": 40000,
                "count": 4
            }
        ]
    }
]
```

### [해결 방법] distinct 사용
- 쿼리에 distinct를 추가하여 같은 엔티티가 조회되면 중복을 거른다
- DB의 distinct와는 조금 다르다.
  - DB의 distinct는 한 row가 `완전히` 같아야 중복이 제거
  - JPA의 distinct는 부모 엔티티(Order)가 `같은 id값`이면 중복을 제거
``` java
public List<Order> findAllWithItem() {
  return em.createQuery(
                  "select distinct o from Order o" +
                          " join fetch o.member m" +
                          " join fetch o.delivery d" +
                          " join fetch o.orderItems oi" +
                          " join fetch oi.item i", Order.class)
          .getResultList();
} 
```
```
[
    {
        "orderId": 4,
        "name": "userA",
        "orderDate": "2025-01-13T16:25:42.797068",
        "orderStatus": "ORDER",
        "address": {
            "city": "서울",
            "street": "1",
            "zipcode": "1111"
        },
        "orderItems": [
            {
                "itemName": "JPA1 BOOK",
                "orderPrice": 10000,
                "count": 1
            },
            {
                "itemName": "JPA2 BOOK",
                "orderPrice": 20000,
                "count": 2
            }
        ]
    },
    {
        "orderId": 11,
        "name": "userB",
        "orderDate": "2025-01-13T16:25:42.876784",
        "orderStatus": "ORDER",
        "address": {
            "city": "진주",
            "street": "2",
            "zipcode": "2222"
        },
        "orderItems": [
            {
                "itemName": "SPRING1 BOOK",
                "orderPrice": 20000,
                "count": 3
            },
            {
                "itemName": "SPRING2 BOOK",
                "orderPrice": 40000,
                "count": 4
            }
        ]
    }
]
```
- SQL쿼리 1번으로 조회 가능.

### 🚨[문제] 페이징 불가능(1:N 조인)
``` 
public List<Order> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Order.class)
                .setFirstResult(1)
                .setMaxResults(100)
                .getResultList();
    }
// 쿼리
2025-01-13 16:35:56.343 DEBUG 7052 --- [nio-8080-exec-2] org.hibernate.SQL                        : 
    select
        distinct order0_.order_id as order_id1_6_0_,
        member1_.member_id as member_i1_4_1_,
        delivery2_.delivery_id as delivery1_2_2_,
        orderitems3_.order_item_id as order_it1_5_3_,
        item4_.item_id as item_id2_3_4_,
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
        delivery2_.status as status5_2_2_,
        orderitems3_.count as count2_5_3_,
        orderitems3_.item_id as item_id4_5_3_,
        orderitems3_.order_id as order_id5_5_3_,
        orderitems3_.order_price as order_pr3_5_3_,
        orderitems3_.order_id as order_id5_5_0__,
        orderitems3_.order_item_id as order_it1_5_0__,
        item4_.name as name3_3_4_,
        item4_.price as price4_3_4_,
        item4_.stock_quantity as stock_qu5_3_4_,
        item4_.artist as artist6_3_4_,
        item4_.etc as etc7_3_4_,
        item4_.author as author8_3_4_,
        item4_.isbn as isbn9_3_4_,
        item4_.actor as actor10_3_4_,
        item4_.director as directo11_3_4_,
        item4_.dtype as dtype1_3_4_ 
    from
        orders order0_ 
    inner join
        member member1_ 
            on order0_.member_id=member1_.member_id 
    inner join
        delivery delivery2_ 
            on order0_.delivery_id=delivery2_.delivery_id 
    inner join
        order_item orderitems3_ 
            on order0_.order_id=orderitems3_.order_id 
    inner join
        item item4_ 
            on orderitems3_.item_id=item4_.item_id
```
- limit, offset을 찾아볼 수 없다.
```
// 페이지네이션 적용했을 때의 로그
2025-01-13 16:35:56.341  WARN 7052 --- [nio-8080-exec-2] o.h.h.internal.ast.QueryTranslatorImpl   : HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!
```
- 하이버네이트는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어오고, **메모리에서 페이징** 한다.
  - ex) 데이터 row가 10,000개라면, 데이터를 모두 가져온 후 메모리에서 페이징 처리 (out of memory 위험)

<참고>
- 컬렉션 페치 조인은 1개만 사용할 수 있다. 
- 컬렉션 둘 이상에 페치 조인을 사용하면 안된다. 
- 데이터가 부정확하게 조회될 수 있다.

## 💡 주문 조회 V3.1: 페이징 적용💡

### 1. ToOne 관계를 모두 페치조인한다
``` java
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
```
### 2. 컬렉션(ToMany)은 지연로딩으로 조회한다.
``` java
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {

        return orderRepository.findAllWithMemberDelivery(offset, limit)
                .stream()
                .map(OrderDto::new)
                .collect(toList());
    }
    
    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(toList());
        }
    }

    @Data
    static class OrderItemDto {
        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
```
- ToOne쿼리 1회 + ToMany 자식만큼 N회 
  - ex) 각 order에 대하여, order이 N개, order에 orderItem이 M개 있다면
  - 전체 order조회 1회 + ( orderItems조회 1회 + 각 orderItem조회 M회 ) * N회
```
2025-01-13 17:09:20.167 DEBUG 19468 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
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
2025-01-13 17:09:20.187 DEBUG 19468 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        orderitems0_.order_id as order_id5_5_0_,
        orderitems0_.order_item_id as order_it1_5_0_,
        orderitems0_.order_item_id as order_it1_5_1_,
        orderitems0_.count as count2_5_1_,
        orderitems0_.item_id as item_id4_5_1_,
        orderitems0_.order_id as order_id5_5_1_,
        orderitems0_.order_price as order_pr3_5_1_ 
    from
        order_item orderitems0_ 
    where
        orderitems0_.order_id=?
2025-01-13 17:09:20.202 DEBUG 19468 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        item0_.item_id as item_id2_3_0_,
        item0_.name as name3_3_0_,
        item0_.price as price4_3_0_,
        item0_.stock_quantity as stock_qu5_3_0_,
        item0_.artist as artist6_3_0_,
        item0_.etc as etc7_3_0_,
        item0_.author as author8_3_0_,
        item0_.isbn as isbn9_3_0_,
        item0_.actor as actor10_3_0_,
        item0_.director as directo11_3_0_,
        item0_.dtype as dtype1_3_0_ 
    from
        item item0_ 
    where
        item0_.item_id=?
2025-01-13 17:09:20.204 DEBUG 19468 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        item0_.item_id as item_id2_3_0_,
        item0_.name as name3_3_0_,
        item0_.price as price4_3_0_,
        item0_.stock_quantity as stock_qu5_3_0_,
        item0_.artist as artist6_3_0_,
        item0_.etc as etc7_3_0_,
        item0_.author as author8_3_0_,
        item0_.isbn as isbn9_3_0_,
        item0_.actor as actor10_3_0_,
        item0_.director as directo11_3_0_,
        item0_.dtype as dtype1_3_0_ 
    from
        item item0_ 
    where
        item0_.item_id=?
2025-01-13 17:09:20.206 DEBUG 19468 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        orderitems0_.order_id as order_id5_5_0_,
        orderitems0_.order_item_id as order_it1_5_0_,
        orderitems0_.order_item_id as order_it1_5_1_,
        orderitems0_.count as count2_5_1_,
        orderitems0_.item_id as item_id4_5_1_,
        orderitems0_.order_id as order_id5_5_1_,
        orderitems0_.order_price as order_pr3_5_1_ 
    from
        order_item orderitems0_ 
    where
        orderitems0_.order_id=?
2025-01-13 17:09:20.207 DEBUG 19468 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        item0_.item_id as item_id2_3_0_,
        item0_.name as name3_3_0_,
        item0_.price as price4_3_0_,
        item0_.stock_quantity as stock_qu5_3_0_,
        item0_.artist as artist6_3_0_,
        item0_.etc as etc7_3_0_,
        item0_.author as author8_3_0_,
        item0_.isbn as isbn9_3_0_,
        item0_.actor as actor10_3_0_,
        item0_.director as directo11_3_0_,
        item0_.dtype as dtype1_3_0_ 
    from
        item item0_ 
    where
        item0_.item_id=?
2025-01-13 17:09:20.208 DEBUG 19468 --- [nio-8080-exec-1] org.hibernate.SQL                        : 
    select
        item0_.item_id as item_id2_3_0_,
        item0_.name as name3_3_0_,
        item0_.price as price4_3_0_,
        item0_.stock_quantity as stock_qu5_3_0_,
        item0_.artist as artist6_3_0_,
        item0_.etc as etc7_3_0_,
        item0_.author as author8_3_0_,
        item0_.isbn as isbn9_3_0_,
        item0_.actor as actor10_3_0_,
        item0_.director as directo11_3_0_,
        item0_.dtype as dtype1_3_0_ 
    from
        item item0_ 
    where
        item0_.item_id=?
```
### 3. 지연로딩 최적화를 위해  `hibernate.default_batch_fetch_size` , `@BatchSize` 를 적용한다.
- 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한꺼번에 설정한 size 만큼 IN 쿼리로 조회한다.
#### hibernate.default_batch_fetch_size: 글로벌 설정 
```
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 1000
```
#### @BatchSize: 개별 최적화 (특정 엔티티)
  ```
  // 컬렉션은 컬렉션 필드에, 엔티티는 엔티티 클래스에 적용
    //toMany
    @BatchSize(size = 1000)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    //toOne
    @BatchSize(size = 1000)
    public abstract class Item {
  ```
<결과>
```
2025-01-13 18:28:05.428 DEBUG 17452 --- [nio-8080-exec-2] org.hibernate.SQL                        : 
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
            on order0_.delivery_id=delivery2_.delivery_id limit ?
2025-01-13 18:28:05.456 DEBUG 17452 --- [nio-8080-exec-2] org.hibernate.SQL                        : 
    select
        orderitems0_.order_id as order_id5_5_1_,
        orderitems0_.order_item_id as order_it1_5_1_,
        orderitems0_.order_item_id as order_it1_5_0_,
        orderitems0_.count as count2_5_0_,
        orderitems0_.item_id as item_id4_5_0_,
        orderitems0_.order_id as order_id5_5_0_,
        orderitems0_.order_price as order_pr3_5_0_ 
    from
        order_item orderitems0_ 
    where
        orderitems0_.order_id in (
            ?, ?
        )
2025-01-13 18:28:05.475 DEBUG 17452 --- [nio-8080-exec-2] org.hibernate.SQL                        : 
    select
        item0_.item_id as item_id2_3_0_,
        item0_.name as name3_3_0_,
        item0_.price as price4_3_0_,
        item0_.stock_quantity as stock_qu5_3_0_,
        item0_.artist as artist6_3_0_,
        item0_.etc as etc7_3_0_,
        item0_.author as author8_3_0_,
        item0_.isbn as isbn9_3_0_,
        item0_.actor as actor10_3_0_,
        item0_.director as directo11_3_0_,
        item0_.dtype as dtype1_3_0_ 
    from
        item item0_ 
    where
        item0_.item_id in (
            ?, ?, ?, ?
        )
```
- 변화된 점은 where절에 in 쿼리가 생겼다는 것.
  - `default_batch_fetch_size: 100` 으로 하면, in 안에 100개까지 들어갈 수 있다.

- [👍🏻]
  - 쿼리 호출 수가 `1 + N` → `1 + 1` 로 최적화 된다. 
  - 조인보다 **DB 데이터 전송량이 최적화** 된다. 
    - Order와 OrderItem을 조인하면 Order가 OrderItem 만큼 중복해서 조회된다. 
    - 이 방법은 각각 조회하므로 전송해야할 `중복 데이터가 없다`.
  - `페이징이 가능`하다.
- [👎🏻]
  - 페치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가한다. (그러나, DB 데이터 전송량은 감소)
- [결론]
  - ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다. 
  - 따라서 ToOne 관계는 페치조인으로 쿼리 수 를 줄여서 해결하고, 나머지(ToMany)는 `hibernate.default_batch_fetch_size` 로 최적화 하자.

> [참고] 
> - `default_batch_fetch_size` 의 크기는 적당한 사이즈를 골라야 하는데, 100~1000 사이를 선택하는 것을 권장한다. <br>
>   - SQL IN 절을 사용하는데, 데이터베이스에 따라 IN 절 파라미터를 1000으로 제한하기도 한다. 
> - 1000으로 잡으면 한번에 1000개를 DB에서 애플리케이션에 불러오므로 DB에 순간 부하가 증가할 수 있다.
> - 하지만 애플리케이션은 100이든 1000이든 결국 전체 데이터를 로딩해야 하므로 메모리 사용량이 같다.
> - `1000으로 설정하는 것이 성능상 가장 좋지만`, 결국 DB든 애플리케이션이든 **순간 부하**를 어디까지 견딜 수 있는 지로 결정하면 된다.
