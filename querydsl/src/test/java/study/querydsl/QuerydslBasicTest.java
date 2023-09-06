package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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

//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();

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


    /**
     *  JPQL vs queryDSL
     *  offset과 limit을 jpql에서는 직접 넣지 않음
     *  쿼리에서 넣는게 아니라 밖에서 넣어줌
     */
    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    // 전체 조회수가 필요한 경우
    // ( countTotal 쿼리를 따로 분리해서 쓰는 경우가 있음 -> count쿼리를 분리해서 더 단순하게 조회할수 있음음
    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2); // contents의 사이즈
    }


    // 집계함수 (집합)
    @Test
    public void aggresgation(){
        // Tuple 형태는 QueryDSL에서 제공
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     * groupBy() 사용 가능
     */
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(team)
                .join(team.members, member) // jpql의 join과 동일
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     *  팀 A에 소속된 모든 회원 조회 ( join() 사용)
     *  join 종류
     *  join() , innerJoin() : 내부 조인(inner join)
     *  leftJoin() : left 외부 조인(left outer join)
     *  rightJoin() : rigth 외부 조인(rigth outer join)
     */
    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * ----------------------------------
     * from 절에 여러 엔티티를 선택해서 세타 조인
     * but 외부 조인 불가능.
     * 조인 on을 사용하면 외부 조인 가능
     */

    // 회원의 이름이 팀 이름과 같은 회원 조회
    @Test
    public void theta_join(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        // 모든 회원, 팀을 가지고 와서 다 조인을 하고 where절로 필터링하게됨
        // db가 최적화를 하긴함. (db마다 성능 최적화 방법이 다름)
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     * ----------------------------------------------------------------------------------------
     * on 절을 활용해 조인 대상을 필터링 할때 외부조인이 아니라 내부조인(inner join)을 사용하면,
     * where 절에서 필터링 하는 것과 기능이 동일하다.
     * 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때, 내부조인 이면 익숙한 where 절로 해결하고
     * 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.
     */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    // 테스트 증명시 사용
    @PersistenceUnit
    EntityManagerFactory emf; // EntityManager를 만드는 팩토리

    @Test
    public void NofetchJoin(){
        em.flush();
        em.flush();

        // Member 엔티티에서 Team 엔티티와의 조인 매칭을 LAZY으로 해서
        // DB에서 조회할때 Member만 조회됨.
        Member findMemeber = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //  isLoaded()를 통해 로딩된 엔티티인지(초기화) 아닌지를 알수 있음.
        //  패치 조인이 적용이 안되면 로딩이 되면 안됨 (false가 나오는게 맞음)
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMemeber.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();

    }


    /**
     *  패치 조인 사용 -> 실무에서 많이 씀.
     */
    @Test
    public void fetchJoin(){
        em.flush();
        em.flush();

        // 패치 조인을 사용하여
        // member1 를 조회할때 연관된 team을 한 쿼리로 다 끌고옴
        Member findMemeber = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMemeber.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();

    }

    /** 서브쿼리
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /** 서브쿼리
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting(  "age")
                .containsExactly(30, 40);
    }

    /** 서브쿼리 여러 건 처리 in 사용
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                        .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting(  "age")
                .containsExactly(20, 30, 40);
    }

    /** 서브쿼리
     *  select절에 서브쿼리 사용
     *  --------------------------
     *  from 절의 서브쿼리 한계
     * - JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl도 지원하지 않는다.
     * - 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
     *
     * from 절의 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     */
    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions.select(memberSub.age.avg()) // static import 가능
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    // DB는 데이터를 최소한의 필터링으로 가져오는 걸로만 하고 나머지 복잡한 쿼리같은 경우에는 애플리케이션에서 로직으로 풀것.
    // 한방 쿼리가 항상 옳지 않다 !
    // 대용량 트래픽일때, 쿼리 한방이 소중하긴 하지만 복잡/길게 하는것보다 쿼리를 두번, 세번 나누는게 나을수도 있다.

    /**
     * Case문 - 단순 조건
     */
    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for(String s : result){
            System.out.println("s = "+s);
        }
    }

    /**
     * Case문 - 복잡한 조건
     */
    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for(String s : result){
            System.out.println("s = "+s);
        }
    }


    /**
     *  상수+문자 더하기
     *  ----------------
     *  member.age.stringValue() 부분이 중요한데,
     *  문자가 아닌 다른 타입들은 stringValue() 로 문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다
     */
    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat(){
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for(String s : result){
            System.out.println("s = "+ s);
        }
    }

    /**
     *  프로젝션: select 대상 지정
     *  프로젝션 대상이 하나인 경우, 타입을 명확하게 지정할 수 있음
     */
    @Test
    public void simpleProjection(){

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for(String s : result){
            System.out.println("s = "+s);
        }
    }

    /**
     *  프로젝션 대상이 여럿인 경우, 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
     *  ------------------------
     *  Tuple을 respository를 넘어가는건 옳지 않음.
     *  respository계층 (DAO) 안에서만 쓰도록 설계할 것.
     */
    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for(Tuple tuple : result){
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * 순수 JPA에서 DTO 조회 코드
     * -------------------------------------------------
     * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
     * DTO의 package이름을 다 적어줘야해서 지저분함
     * 생성자 방식만 지원함
     */
    @Test
    public void findDtoByJPQL() {
        // Member 엔티티를 조회
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                        "from Member m", MemberDto.class)
                .getResultList();
    }

    /**
     * Querydsl 빈 생성(Bean population)
     * 결과를 DTO 반환할 때 사용
     * 다음 3가지 방법 지원
     * -------------------------------
     * 1. 프로퍼티 접근 (setter 활용)
     * 2. 필드 직접 접근
     * 3. 생성자 사용


     * 프로퍼티 접근
     * -------------------
     * 기본생성자가 필요함 (DTO에 기본생 성자 필수)
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 필드 직접 접근
     * -------------
     * getter, setter 없어도 됨.
     * 필드에 바로 값을 꽂는다.
     */
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 생성자 사용
     * -------------
     * 생성자 파라미터 타입(필드명)이 맞아야함.
     */
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        /*
            만약 필드명이 다른 경우,
            프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
            username.as("name") : 필드에 별칭 적용
            ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용

            List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                    member.username.as("name"),
                        ExpressionUtils.as(
                            JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")    // 서브쿼리의 이름을 age로 사용 (alias)
                        )
                 ).from(member)
                 .fetch();
         */
    }

    /**
     * QueryProjection 사용하기
     * --------------------------------------
     * 제일 깔끔한 해결책이지만, 약간의 단점이 있음.
     * 이 방법은 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다.
     * 다만 DTO에 QueryDSL 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다
     * (DTO가 여러 계층에서 사용되기 때문에 어노테이션 때문에 queryDSL에 의존적이게됨.)
     */
    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto : result){
            System.out.println("memberDto = "+memberDto);
        }
    }

    /*
    distinct( JPQL의 distinct와 같다.) 사용법

        List<String> result = queryFactory
             .select(member.username).distinct()
             .from(member)
             .fetch();
     */

    /**
     *  동적 쿼리 - BooleanBuilder 사용
     */
    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        // 필수로 넣고 싶을때
        // BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));

        if(usernameCond != null){
            // 빌더에 and 조건을 넣음
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        // 파라미터의 조건대로 조회함
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적 쿼리 -Where 다중 파라미터 사용
     * --------------------------------
     * BooleanBuilder보다 깔끔함
     * where 조건에 null 값은 무시된다.
     * 메서드를 다른 쿼리에서도 재활용 할 수 있다.
     * 쿼리 자체의 가독성이 높아진다.
     */
    @Test
    public void dynamicQuery_WherePara(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
        if(usernameCond == null) return null;
        return member.username.eq(usernameCond);
    }

    private Predicate ageEq(Integer ageCond) {
        if(ageCond == null) return null;
        return member.age.eq(ageCond);
    }

    //조합 가능 -> 쓰려면 관련 매서드 타입이 모두 BooleanExpression 여야함
//    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
//        return usernameEq(usernameCond).and(ageEq(ageCond));
//    }

    /**
     * 수정, 삭제 벌크 연산
     * ------------------
     * 쿼리 한번으로 대량 데이터 수정하기
     * but, 벌크 연산을 해버리면 DB에는 값이 바뀌지만 영속성 컨텍스트에는 값이 바뀌지 않는다
     * (영속석 컨텍스트가 우선권을 가지게됨)
     * 때문에 항상 배치 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다
     */
    @Test
    // @Commit test에서 @Transaction이 되어있으면 테스트 후에 rollback을 해서 DB에 안남음
    public void bulkUpdate(){
        // 회원의 나이가 28 미만인 경우, 비회원으로 바꿈
        // count : 영향을 받은 데이터 개수
        long count1 = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 기존 숫자에 1 더하기
        long count2 = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        // 기존 숫자에 2씩 곱하기
        long count3 = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();

        // 쿼리 한번으로 대량 데이터 삭제
        // 18살 이상의 회원 모두 삭제
        long count4 = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        // 영속성 컨텍스트 초기화
        em.flush();
        em.clear();
    }

    /**
     * SQL function 호출하기
     * ----------------------------------------------------------
     * SQL function은 JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다
     */
    @Test
    public void sqlFunction(){
        // member M으로 변경하는 replace 함수 사용
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();


        for(String s : result){
            System.out.println("s = "+s);
        }

        //  s = M1
        //  s = M2
        //  s = M3
        //  s = M4
    }

    @Test
    public void sqlFunction2(){
        //소문자로 변경하기
//        List<String> result = queryFactory
//                .select(member.username)
//                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
//                .fetch();

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for(String s : result) System.out.println("s = " +s);
    }
}
