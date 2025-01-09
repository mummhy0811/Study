# [섹션1] 
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