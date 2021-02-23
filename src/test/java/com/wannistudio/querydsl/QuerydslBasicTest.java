package com.wannistudio.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wannistudio.querydsl.dto.MemberDto;
import com.wannistudio.querydsl.dto.QMemberDto;
import com.wannistudio.querydsl.dto.UserDto;
import com.wannistudio.querydsl.entity.Member;
import com.wannistudio.querydsl.entity.QMember;
import com.wannistudio.querydsl.entity.QTeam;
import com.wannistudio.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.wannistudio.querydsl.entity.QMember.*;
import static com.wannistudio.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.*;

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

  @Test
  public void startJPQL() {
    // member1을 찾아라.
    final String qlString =
            "select m from Member m " +
                    "where m.username = :username";
    final Member findByJPQL = em.createQuery(qlString, Member.class)
            .setParameter("username", "member1")
            .getSingleResult();

    assertThat(findByJPQL.getUsername()).isEqualTo("member1");
  }

  @Test
  public void startQuerydsl() {
    JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//    QMember m = new QMember("m");
//    QMember m = QMember.member;

    final Member findMember = queryFactory
            .select(member)
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void search() {

    Member findMember = queryFactory
            .selectFrom(member)
            .where(
//                    member.username.eq("member1")
//                    .and(member.age.eq(10))
                    member.username.eq("mamber1"),
                    member.age.eq(10)
            ).fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void resultFetch() {
//    final List<Member> fetch = queryFactory
//            .selectFrom(member)
//            .fetch();
//
//    final Member fetchOne = queryFactory
//            .selectFrom(member)
//            .fetchOne();
//
//    final Member fetchFirst = queryFactory
//            .selectFrom(QMember.member)
//            .fetchFirst();
//
//    final QueryResults<Member> results = queryFactory
//            .selectFrom(member)
//            .fetchResults();
//
//    results.getTotal();
//    final List<Member> contents = results.getResults();
//    results.getOffset();

    long total = queryFactory
            .selectFrom(member)
            .fetchCount();
  }

  /**
   * 회원 나이 내림차순
   * 회원 이름 올림차순
   * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
   */
  @Test
  public void sort() {
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));
    em.persist(new Member(null, 100));


    final List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(),
                    member.username.asc().nullsLast()
            ).fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  void paging1() {
    final List<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  void paging2() {
    final QueryResults<Member> memberQueryResults = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();

    assertThat(memberQueryResults.getTotal()).isEqualTo(4);
    assertThat(memberQueryResults.getLimit()).isEqualTo(2);
    assertThat(memberQueryResults.getOffset()).isEqualTo(1);
    assertThat(memberQueryResults.getResults().size()).isEqualTo(2);
  }

  @Test
  public void aggregation() {
    /**
     * 보통 튜플은 실무에선 잘 안쓰이고 DTO로 뽑아오려고 한다.
     * */
    // queryDsl이 제공하는 튜플
    final List<Tuple> result = queryFactory
            .select(member.count(),
                    member.age.sum(),
                    member.age.avg(),
                    member.age.max(),
                    member.age.min()
            ).from(member)
            .fetch();

    final Tuple tuple = result.get(0);
//    assertThat(tuple.get(member.count())).isEqualTo(4);
//    assertThat(tuple.get(member.age.sum())).isEqualTo(40);
//    assertThat(tuple.get(member.age.avg())).isEqualTo(10);
//    assertThat(tuple.get(member.age.max())).isEqualTo(10);
//    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  /**
   * 팀의 이름과 각 팀의 평균 연령을 구해라.
   */
  @Test
  public void groupBy() throws Exception {
    final List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

    final Tuple teamA = result.get(0);
    final Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
//    assertThat(teamA.get(member.age.avg())).isEqualTo(15)
  }

  /**
   * 팀 A에 소속된 모든 회원
   */
  @Test
  public void join() {
    final List<Member> result = queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

    assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2")
    ;
  }

  /**
   * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
   * select m, t from Member m left join m.team t on t.name = 'teamA'
   */
  @Test
  public void join_on_filtering() {
    final List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team).on(team.name.eq("teamA"))
            .fetch();

    for (Tuple tuple : result) {
      System.out.println(tuple);
    }
  }

  /**
   * 연관관계 없는 엔티티 외부 조인
   * 회원의 이름이 팀 이름과 같은 회원을 외부 조인.
   */
  @Test
  public void join_on_no_relation() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    final List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team).on(member.username.eq(team.name))
            .fetch();

    for (Tuple tuple : result) {
      System.out.println(tuple);
    }
  }

  /**
   *
   */

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  public void fetchJoinNo() {
    em.flush();
    em.clear();

    final Member findMember = queryFactory
            .selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();

    final boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("패치 조인 미적용").isFalse();
  }

  /**
   * 나이가 가장 많은 회원 조회
   */
  @Test
  public void subQuery() {
    QMember memberSub = new QMember("memberSub");

    final List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                    JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub)
            ))
            .fetch();

    assertThat(result).extracting("age").containsExactly(40);
  }

  /**
   * 나이가 평균 이상인 회원
   */
  @Test
  public void subQueryGoe() {
    QMember memberSub = new QMember("memberSub");

    final List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub)
            )).fetch();
    assertThat(result).extracting("age").containsExactly(30, 40);
  }

  @Test
  public void subQueryIn() {
    QMember memberSub = new QMember("memberSub");

    final List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                    JPAExpressions
                            .select(memberSub.age)
                            .from(memberSub)
                            .where(memberSub.age.gt(10))
            )).fetch();

    assertThat(result).extracting("age").containsExactly(20, 30, 40);
  }

  @Test
  public void selectSubQuery() {

    QMember memberSub = new QMember("memberSub");

    final List<Tuple> result = queryFactory
            .select(member.username,
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub)
            ).from(member)
            .fetch();
    for (Tuple tuple : result) {
      System.out.println(tuple);
    }
  }

  /**
   * from 절의 서브쿼리 한계
   * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리는 지원하지 않는다.
   * 당연히 Querydsl도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의
   * 서브쿼리는 지원한다. Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를
   * 지원한다.
   */

  /**
   * from 절의 서브쿼리 해결방안
   * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
   * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
   * 3. nativeSQL을 사용한다.
   */

  @Test
  public void basicCase() {
    final List<String> result = queryFactory
            .select(member.age
                    .when(10).then("10살")
                    .when(20).then("20살")
                    .otherwise("기타"
                    )
            ).from(member)
            .fetch();
    for (String s : result) {
      System.out.println(s);
    }
  }

  @Test
  public void complexCase() {
    final List<String> result = queryFactory
            .select(new CaseBuilder()
              .when(member.age.between(0, 20)).then("0 ~ 20살")
              .when(member.age.between(21, 30)).then("21 ~ 30살")
              .otherwise("기타"))
            .from(member)
            .fetch();
    for (String s : result) {
      System.out.println(s);
    }
  }

  @Test
  public void concat() {
    final List<String> result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();

    for (String s : result) {
      System.out.println(s);
    }
  }

  @Test
  public void simpleProjection() {
    final List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println(s);
    }
  }

  @Test
  public void tupleProjection() {
    final List<Tuple> fetch = queryFactory
            .select(member.username, member.age)
            .from(member)
            .fetch();

    for (Tuple tuple : fetch) {
      final String username = tuple.get(member.username);
      final Integer age = tuple.get(member.age);
      System.out.println(username);
      System.out.println(age);
    }
  }

  @Test
  public void findDtoByJPQL() {
    List<MemberDto> resultList = em.createQuery("select  new com.wannistudio.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
            .getResultList();

    for (MemberDto memberDto : resultList) {
      System.out.println(memberDto);
    }
  }

  @Test
  public void findDtoBySetter() {
    final List<MemberDto> result = queryFactory
            .select(Projections.bean(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println(memberDto);
    }
  }

  @Test
  public void findDtoByField() {
    final List<MemberDto> result = queryFactory
            .select(Projections.fields(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    for (MemberDto memberDto : result) {
      System.out.println(memberDto);
    }
  }

  @Test
  public void findDtoByConstructor() {
    final List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    for (MemberDto memberDto : result) {
      System.out.println(memberDto);
    }
  }

  @Test
  public void findUserDto() {
    final QMember memberSub = new QMember("memberSub");

    final List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                    member.username.as("name"),
                    ExpressionUtils.as(JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub), "age"
                    ))
            )
            .from(member)
            .fetch();
    for (UserDto userDto : result) {
      System.out.println(userDto);
    }
  }

  @Test
  public void findUserDtoByConstructor() {
    final List<UserDto> result = queryFactory
            .select(Projections.constructor(UserDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    for (UserDto memberDto : result) {
      System.out.println(memberDto);
    }
  }

  @Test
  public void findDtoByQueryProjection() {
    // 장점 : alias 고민 안해도 됨
    // 단점 : Q컴파일을 따로 해야함.
//          : 의존성 문제 발생 (queryDSL을 빼면 망함)
    final List<MemberDto> fetch = queryFactory
            .select(new QMemberDto(member.username, member.age)) // 컨트롤 p
            .from(member)
            .fetch();

    for (MemberDto memberDto : fetch) {
      System.out.println(memberDto);
    }
  }
}
