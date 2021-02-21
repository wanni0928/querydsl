package com.wannistudio.querydsl;


import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wannistudio.querydsl.QuerydslApplication;
import com.wannistudio.querydsl.entity.Hello;
import com.wannistudio.querydsl.entity.QHello;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = QuerydslApplication.class)
@Transactional
class QuerydslApplicationTests {
  @Autowired
  EntityManager em;

  @Test
  void contextLoads() {
    Hello hello = new Hello();
    em.persist(hello);

    JPAQueryFactory query = new JPAQueryFactory(em);
//    QHello qHello = new QHello("h");
    QHello qHello = QHello.hello;

    Hello result = query
            .selectFrom(qHello)
            .fetchOne();

    assertThat(result).isEqualTo(hello);
    assertThat(result.getId()).isEqualTo(hello.getId());
  }

}
