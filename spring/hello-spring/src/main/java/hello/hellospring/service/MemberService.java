package hello.hellospring.service;

import hello.hellospring.domain.Member;
import hello.hellospring.repository.MemberRepository;
import hello.hellospring.repository.MemoryMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public class MemberService {


    private final MemberRepository memberRepository;

    //외부에서 멤버리포지토리를 넣어줌(DI)
    public MemberService(MemberRepository memberRepository){

        this.memberRepository=memberRepository;
    }

    //회원가입
    public Long join(Member member){
        /* 함수로 뺌
        memberRepository.findByName(member.getName())
            .ifPresent(m ->{ //결과가 optional 멤버라서 쓸 수 있는 코드
                throw new IllegalStateException("이미 존재하는 회원입니다.");
            });

         */
        validateDuplicateMember(member);//중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member){
        //이름 중복 불가능
        memberRepository.findByName(member.getName())
                .ifPresent(m ->{ //결과가 optional 멤버라서 쓸 수 있는 코드
                    throw new IllegalStateException("이미 존재하는 회원입니다.");
                });
    }

    public List<Member> findMembers(){
        return memberRepository.findAll();
    }

    public Optional<Member> findOne(Long memberId){
        return memberRepository.findById(memberId);
    }
}
