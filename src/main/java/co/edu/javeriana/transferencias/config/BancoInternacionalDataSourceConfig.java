package co.edu.javeriana.transferencias.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    basePackages = "co.edu.javeriana.transferencias.repository.internacional",
    entityManagerFactoryRef = "internacionalEntityManagerFactory",
    transactionManagerRef = "internacionalTransactionManager"
)
public class BancoInternacionalDataSourceConfig {

    @Bean(name = "internacionalDataSource")
    @ConfigurationProperties(prefix = "spring.datasource-internacional")
    public DataSource internacionalDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "internacionalEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean internacionalEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("internacionalDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.jdbc.batch_size", 20);
        properties.put("hibernate.order_inserts", true);
        properties.put("hibernate.order_updates", true);

        return builder
                .dataSource(dataSource)
                .packages("co.edu.javeriana.transferencias.model")
                .persistenceUnit("internacional")
                .properties(properties)
                .build();
    }

    @Bean(name = "internacionalTransactionManager")
    public PlatformTransactionManager internacionalTransactionManager(
            @Qualifier("internacionalEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
