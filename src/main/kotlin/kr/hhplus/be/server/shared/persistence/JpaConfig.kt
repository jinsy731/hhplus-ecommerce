package kr.hhplus.be.server.shared.persistence

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class JpaConfig {

    @Bean
    fun transactionManager(): PlatformTransactionManager = JpaTransactionManager()
}