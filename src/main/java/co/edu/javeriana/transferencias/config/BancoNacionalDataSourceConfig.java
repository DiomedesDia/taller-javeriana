package co.edu.javeriana.transferencias.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "co.edu.javeriana.transferencias.repository.nacional",
    entityManagerFactoryRef = "nacionalEntityManagerFactory",
    transactionManagerRef = "nacionalTransactionManager"
)
public class BancoNacionalDataSourceConfig {

    @Primary
    @Bean(name = "nacionalDataSource")
    @ConfigurationProperties(prefix = "spring.datasource-nacional")
    public DataSource nacionalDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "nacionalEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nacionalEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("nacionalDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);

        return builder
                .dataSource(dataSource)
                .packages("co.edu.javeriana.transferencias.model")
                .persistenceUnit("nacional")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean(name = "nacionalTransactionManager")
    public PlatformTransactionManager nacionalTransactionManager(
            @Qualifier("nacionalEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
