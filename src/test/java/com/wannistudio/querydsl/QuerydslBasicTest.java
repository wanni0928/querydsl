package com.wannistudio.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wannistudio.querydsl.entity.Member;
import com.wannistudio.querydsl.entity.QMember;
import com.wannistudio.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static com.wannistudio.querydsl.entity.QMember.*;
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
    Member member2 = new Member("member2", 10, teamA);
    Member member3 = new Member("member3", 10, teamB);
    Member member4 = new Member("member4", 10, teamB);

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
}
