package com.dou.mysqlstarter.config;

import com.dou.mysqlstarter.condition.HibernateCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ConditionalOnClass(DataSource.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@PropertySource("classpath:mysql.properties")
public class MySqlAutoconfiguration {

  @Autowired
  private Environment environment;

  @Bean
  @ConditionalOnProperty(name = "usemysql", havingValue = "local")
  @ConditionalOnMissingBean
  public DataSource dataSource() {
    final DriverManagerDataSource dataSource = new DriverManagerDataSource();

    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl("jdbc:mysql://localhost:3306/myDb?createDatabaseIfNotExist=true&&serverTimezone=UTC");
    dataSource.setUsername("mysqluser");
    dataSource.setPassword("mysqlpass");

    return dataSource;
  }

  @Bean(name = "dataSource")
  @ConditionalOnProperty(name = "usemysql", havingValue = "custom")
  @ConditionalOnMissingBean
  public DataSource dataSource2() {
    final DriverManagerDataSource dataSource = new DriverManagerDataSource();

    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl(environment.getProperty("mysql.url"));
    dataSource.setUsername(
        environment.getProperty("mysql.user") != null ? environment.getProperty("mysql.user") : "");
    dataSource.setPassword(
        environment.getProperty("mysql.pass") != null ? environment.getProperty("mysql.pass") : "");

    return dataSource;
  }

  @Bean
  @ConditionalOnBean(name = "dataSource")
  @ConditionalOnMissingBean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    final LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
    entityManagerFactoryBean.setDataSource(dataSource());
    entityManagerFactoryBean.setPackagesToScan("com.dou");
    entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    if (additionalProperties() != null) {
      entityManagerFactoryBean.setJpaProperties(additionalProperties());
    }
    return entityManagerFactoryBean;
  }

  @Bean
  @ConditionalOnMissingBean(type = "JpaTransactionManager")
  JpaTransactionManager transactionManager(final EntityManagerFactory entityManagerFactory) {
    final JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(entityManagerFactory);
    return transactionManager;
  }

  @ConditionalOnResource(resources = "classpath:mysql.properties")
  @Conditional(HibernateCondition.class)
  final Properties additionalProperties() {
    final Properties hibernateProperties = new Properties();

    if (environment.getProperty("mysql-hibernate.hbm2ddl.auto") != null) {
      hibernateProperties.setProperty("hibernate.hbm2ddl.auto", environment.getProperty("mysql-hibernate.hbm2ddl.auto"));
    }
    hibernateProperties.setProperty("hibernate.dialect", environment.getProperty("mysql-hibernate.dialect") != null ? environment
        .getProperty("mysql-hibernate.dialect") : "org.hibernate.dialect.MySQL8Dialect");
    hibernateProperties.setProperty("hibernate.show_sql", environment.getProperty("mysql-hibernate.show_sql") != null ? environment
        .getProperty("mysql-hibernate.show_sql") : "false");

    return hibernateProperties;
  }

}
