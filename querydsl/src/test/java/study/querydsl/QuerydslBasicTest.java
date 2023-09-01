package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    //member1을 찾아라.
    @Test
    public void startJPQL() {
        String qlString =
                "select m from Member m " +
                 "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //member1을 찾아라.
    @Test
    public void startQuerydsl() {
//        QMember qMember = new QMember("m"); //별칭 직접 지정
//        Member findMember = queryFactory
//                .select(m)
//                .from(m)
//                .where(m.username.eq("member1"))//파라미터 바인딩 처리
//                .fetchOne();
//        assertThat(findMember.getUsername()).isEqualTo("member1");


        QMember qMember = QMember.member; //기본 인스턴스 사용
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    // 이름이 "member1"이면서 age가 10~30 사이인 사람 조회
    @Test
    public void search(){
        Member findMember  = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                .and(member.age.between(10, 30)))
                .fetchOne();  // 단건 조회

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // 이름이 "member1"이면서 age가 10~30 사이인 사람 조회
    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10, 30), // ','로 chain을 사용하여 and 조건 구분
                        null // null 은 무시됨, 메서드 추출을 활용해서 동적 쿼리를 깔끔하게 만들 수 있음.
                )
                .fetchOne();  // 단건 조회

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchExample(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.ne("member1"), // member1이 아니고( .eq("member1").not() 와 동일 ),
                        member.age.goe(30) // age >= 30 인 경우

                        /*
                            member.username.eq("member1") // username = 'member1'
                            member.username.ne("member1") //username != 'member1'
                            member.username.eq("member1").not() // username != 'member1'
                            member.username.isNotNull() //이름이 is not null
                            member.age.in(10, 20) // age in (10,20)
                            member.age.notIn(10, 20) // age not in (10, 20)
                            member.age.between(10,30) //between 10, 30
                            member.age.goe(30) // age >= 30
                            member.age.gt(30) // age > 30
                            member.age.loe(30) // age <= 30
                            member.age.lt(30) // age < 30
                            member.username.like("member%") //like 검색
                            member.username.contains("member") // like ‘%member%’ 검색
                            member.username.startsWith("member") //like ‘member%’ 검색

                     */
                )
                .fetchOne();  // 단건 조회


        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory.selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();

        // 페이징할때 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        // 쿼리가 2번 실행됨 -> 페이징을 위한 totalCount도 가져와야해서
        results.getTotal();
        //  result.getLimit(), results.getOffset 설정
        List<Member> content  = results.getResults();
        // content.get(index) 하면 해당 페이지 가져옴;

        // 페이징을 좀더 편하게 count용 쿼리를 사용
        long totalCount = queryFactory
                .selectFrom(member)
                .fetchCount();

        // 복잡한 페이징을 호출할 경우 .getTotal로 페이징과 totalCount를 묶어서 하지말고, 두개를 나눠야 성능이 좋음

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 단 2에서 회원 이름이 없으면 마지막에 출력
     */

    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        // 검증
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

}
