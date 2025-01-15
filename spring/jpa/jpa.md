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

## 주문 조회 V4: JPA에서 DTO 직접 조회
### QueryRepository 분리
``` java
    public List<OrderQueryDto> findOrderQueryDtos() {
        //루트 조회(toOne 코드를 모두 한번에 조회)
        List<OrderQueryDto> result = findOrders();

        //루프를 돌면서 컬렉션 추가(추가 쿼리 실행)
        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });
        return result;
    }

    /**
     * 1:N 관계(컬렉션)를 제외한 나머지를 한번에 조회
     */
    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderQueryDto.class)
                .getResultList();
    }

    /**
     * 1:N 관계인 orderItems 조회
     */
    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                        " join oi.item i" +
                        " where oi.order.id = : orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }
```

### 리포지토리와 같은 경로에 DTO 작성
``` java
@Data
public class OrderQueryDto {

    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;
    private List<OrderItemQueryDto> orderItems;

    public OrderQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }

    public OrderQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address, List<OrderItemQueryDto> orderItems) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
        this.orderItems = orderItems;
    }
}
//------------------------------------------------

@Data
public class OrderItemQueryDto {

    @JsonIgnore
    private Long orderId; 
    private String itemName;
    private int orderPrice;
    private int count;      

    public OrderItemQueryDto(Long orderId, String itemName, int orderPrice, int count) {
        this.orderId = orderId;
        this.itemName = itemName;
        this.orderPrice = orderPrice;
        this.count = count;
    }
}
```
<결과>
- Query: 루트 1번, 컬렉션 N 번 실행 
  - Order들을 조회하는 것 1회, 그 Order에 해당하는 OrderItems를 조회하는 것 각각 N회
  - Order에 OrderItem이 4개라면, 4회 조회(id로 각각 조회하기 때문)
- ToOne 관계들을 먼저 조회하고, ToMany 관계는 각각 처리한다. 
  - ToOne 관계는 조인해도 데이터 row 수가 증가하지 않기 때문에 먼저 처리.
  - ToMany(1:N) 관계는 조인하면 row 수가 증가하기 때문에 나주에 처리.


## 주문 조회 V5: JPA에서 DTO 직접 조회 최적화
``` java
    public List<OrderQueryDto> findAllByDto_optimization() {

        //루트 조회(toOne 코드를 모두 한번에 조회)
        List<OrderQueryDto> result = findOrders();

        //orderItem 컬렉션을 MAP 한방에 조회
        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        //루프를 돌면서 컬렉션 추가(추가 쿼리 실행X)
        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return result;
    }

    //id를 list형태로 뽑아낸다(where in 절에 사용하기 위함)
    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        return result.stream()
                .map(OrderQueryDto::getOrderId)
                .collect(Collectors.toList());
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                        " join oi.item i" +
                        " where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        return orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
    }
```
<결과>
- Query: 루트 1번, 컬렉션 1번 
  - Order들을 조회하는 것 1회, 그 Order에 해당하는 OrderItems를 조회하는 것 각각 N회
  - Order에 OrderItem이 4개라면, where in을 이용해 한 번에 4개 조회!
- 쿼리를 한 번 날리고(컬렉션 조회용), `메모리에서 매칭`(컬렉션 데이터를 채움)하기 때문에 추가 쿼리가 실행되지 않음.
- ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 `OrderItem` 을 한꺼번에 조회 
- MAP을 사용해서 `매칭 성능 향상(O(1))`

## 주문 조회 V6: JPA에서 DTO 직접 조회 최적화(플랫)
### join을 이용해 flat하게 dto로 조회
```java
    // ------ repo
    public List<OrderFlatDto> findAllByDto_flat() {
      return em.createQuery(
                      "select new jpabook.jpashop.repository.order.query.OrderFlatDto(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count)" +
                              " from Order o" +
                              " join o.member m" +
                              " join o.delivery d" +
                              " join o.orderItems oi" +
                              " join oi.item i", OrderFlatDto.class)
              .getResultList();
    }
    //-----------DTO
    @Data
    public class OrderFlatDto {
    
      private Long orderId;
      private String name;
      private LocalDateTime orderDate; //주문시간
      private Address address;
      private OrderStatus orderStatus;
    
      private String itemName;//상품 명
      private int orderPrice; //주문 가격
      private int count;      //주문 수량
    
      public OrderFlatDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address, String itemName, int orderPrice, int count) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
        this.itemName = itemName;
        this.orderPrice = orderPrice;
        this.count = count;
      }
    
    }
```
<결과>
- [👍🏻]
  - 쿼리 1회로 조회 가능
- [👎🏻]
  - 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가 추가
    - 상황에 따라 V5 보다 더 느릴 수 있음
  - 애플리케이션에서 필요한 추가 작업이 많음
  - 페이징 불가능

### flat 제거
``` java
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }
```
- `@EqualsAndHashCode(of = "orderId")`를 DTO에 추가해야 함
  - 동일한 hashcode를 가진 것을 기준으로 groupBy 하겠다는 의미
- 이 전 버전과 동일한 api 스펙을 이용하기 위한 작업
- 메모리에서 해주는 작업이다.

## ✅API 개발 총 정리✅
### 엔티티 조회
#### V1: 엔티티 그대로 반환
#### V2: 엔티티 조회 후 DTO로 변환
#### V3: 페치조인으로 조회
- 페이징 불가능
#### V3.1: 페치조인 + 지연로딩
- ToOne 관계는 페치 조인으로 쿼리 수 최적회
- 컬렉션은 페이징을 위해 지연로딩을 유지하고, `hibernate.default_batch_fetch_size`, `@BatchSize`로 최적회
### DTO 직접 조회
#### V4: Jpa에서 DTO로 변환
#### V5: 컬렉션 조회 최적화 
- 일대다 관계인 컬렉션은 IN 절을 활용해서 메모리에 미리 조회해서 최적화
#### V6: 플랫 데이터 최적화 
- JOIN 결과를 그대로 조회 후 애플리케이션에서 원하는 모양으로 직접 변환


## 권장 순서
1. 엔티티 조회 방식으로 우선 접근 
   1. 페치조인으로 쿼리 수를 최적화 
   2. 컬렉션 최적화 
      1. 페이징 필요 `hibernate.default_batch_fetch_size` , `@BatchSize` 로 최적화 
      2. 페이징 필요X  페치 조인 사용
2. 엔티티 조회 방식으로 해결이 안되면 DTO 조회 방식 사용 
3. DTO 조회 방식으로 해결이 안되면 NativeSQL or 스프링 JdbcTemplate
> 참고: <br>
> **엔티티 조회** 방식은 페치 조인이나 `hibernate.default_batch_fetch_size` , `@BatchSize` 같이 코드를 거의 수정하지 않고 옵션만 약간 변경해서 다양한 성능 최적화를 시도할 수 있다.<br>
> 반면에 **DTO를 직접 조회**하는 방식은 성능을 최적화 하거나 성능 최적화 방식을 변경할 때 많은 코드를 변경해야 한다.

> 참고: <br>
> 개발자는 성능 최적화와 코드 복잡도 사이에서 줄타기를 해야 한다. <br>
> 항상 그런 것은 아니지만, 보통 성능 최적화는 단순한 코드를 복잡한 코드로 몰고간다. <br>
> **엔티티 조회 방식**은 JPA가 많은 부분을 최적화 해주기 때문에 단순한 코드를 유지하면서 성능을 최적화 할 수 있다. <br>
> 반면에 **DTO 조회 방식**은 SQL을 직접 다루는 것과 유사하기 때문에, 둘 사이에 줄타기를 해야 한다.

- 페치조인까지 했는데 성능이 문제가 되는 것이라면 트래픽이 아주 많은 서비스일 것이다.
- 그럴 경우에는 redis라던지, 로컬 캐시를 이용하는 등의 다른 방법을 찾는 것이 좋다.
  - 캐싱을 할 때는 엔티티를 그대로 쓰지 말 것(영속성이 꼬인다)
  - 꼭 DTO로 변환해서 저장하기

**DTO 조회 방식의 선택지**
- DTO로 조회하는 방법도 각각 장단이 있다. V4, V5, V6에서 단순하게 쿼리가 1번 실행된다고 V6이 항상 좋은 방법인 것은 아니다. 
- V4는 코드가 단순하다.  
  - 특정 주문 한건만 조회하면 이 방식을 사용해도 성능이 잘 나온다. 
  - 예를 들어서 조회한 Order 데이터가 1건이면 OrderItem을 찾기 위한 쿼리도 1번만 실행하면 된다. 
- V5는 코드가 복잡하다. 
  - 여러 주문을 한꺼번에 조회하는 경우에는 V4 대신에 이것을 최적화한 V5 방식을 사용해 야 한다. 
  - 예를 들어서, 조회한 Order 데이터가 1000건일 때
    - V4 방식을 그대로 사용하면 쿼리가 총 1 + 1000번 실행된다. 여기서 1은 Order 를 조회한 쿼리고, 1000은 조회된 Order의 row 수다. 
    - V5 방식으로 최적화 하면 쿼리가 총 1 + 1번만 실행된다. 
  - 상황에 따라 다르겠지만 운 환경에서 100배 이상의 성능 차이가 날 수 있다.
- V6는 완전히 다른 접근방식이다. 
  - 쿼리 한번으로 최적화 되어서 상당히 좋아보이지만, Order를 기준으로 페이징이 불가능하다. 
  - 실무에서는 이정도 데이터면 수백/수천건 단위로 페이징 처리가 필요하므로, 선택하기 어려운 방법이다.  
  - 데이터가 많으면 중복 전송이 증가해서 V5와 비교해서 성능 차이도 미비하다.

> 굳이 DTO 조회를 해야 한다면, V5를 쓰는 것이 좋아보인다.


# [섹션 6]
## OSIV와 성능 최적화
### OSIV란?
- Open Session In View: 하이버네이트
- Open EntityManager In View: JPA

### OSIV ON
 <img src="images/spring/jpa_osiv1.jpg" width="30%"/>  <br>

- `spring.jpa.open-in-view` : true 기본값
- Spring 프로젝트를 시작하면 warn 로그가 뜬다. (이유가 있다.)
  - ```2025-01-15 21:33:16.500  WARN 10008 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning```
- 영속성 컨텍스트 생존 범위: 최초 DB 커넥션(트랜잭션) 시작 ~ API 응답 종료(화면 반환)
- 영속성 컨텍스트 범위가 길다.
  - View Template이나 API 컨트롤러에서 지연 로딩이 가능하다
- 지연 로딩은 영속성 컨텍스트가 살아있어야 가능하고, 영속성 컨텍스트는 기본적으로 데이터베이스 커넥션을 유지한다.

> 이 전략은 너무 오랜시간동안 `데이터베이스 커넥션 리소스`를 사용하기 때문에, **실시간 트래픽**이 중요한 애플리 케이션에서는 커넥션이 모자랄 수 있다. 
> 이것은 결국 `장애로 이어진다`.
> 
> 예를 들어서 컨트롤러에서 외부 API를 호출하면 외부 API 대기 시간 만큼 커넥션 리소스를 반환하지 못하고, 유지해야 한다.

> 하지만, lazy loading을 이용해서 컨트롤러나 뷰에서 활용할 수 있다는 장점이 있다.
> (중복을 줄여서 유지보수성을 높일 수 있다)


### OSIV OFF

<img src="images/spring/jpa_osiv2.jpg" width="30%"/>  <br>

- spring.jpa.open-in-view: false` OSIV 종료
- 영속성 컨텍스트 생존 범위: 트랜잭션 범위와 동일
- 영속성 컨텍스트 범위가 짧다.
  - 트랜잭션을 종료할 때 영속성 컨텍스트를 닫고 DB 커넥션도 종료한다.
    - 커넥션 리소스를 낭비하지 않는다.
  - 모든 지연로딩을 트랜잭션 안에서 처리해야 한다.
  - view template에서 지연로딩이 동작하지 않는다
    - `org.hibernate.LazyInitializationException: could not initialize proxy [jpabook.jpashop.domain.Member#1] - no Session
      `
## [OFF 오류 해결 방법] 커맨드와 쿼리 분리
### QueryService 생성
- 영속성 로직을 관리하는 service를 만들어서 controller에 있던 영속성 코드를 제거한다.
- OrderService
  - OrderService: 핵심 비즈니스 로직
  - OrderQueryService: 화면이나 API에 맞춘 서비스(주로 읽기 전용 트랜잭션 사용)
