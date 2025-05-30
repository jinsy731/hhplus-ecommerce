package kr.hhplus.be.server

import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@Configuration
class TestcontainersConfiguration {
    @PreDestroy
    fun preDestroy() {
        if (mySqlContainer.isRunning) mySqlContainer.stop()
        if (redisContainer.isRunning) redisContainer.stop()
        if (kafkaContainer.isRunning) kafkaContainer.stop()
    }

    companion object {
        val mySqlContainer: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("hhplus")
            .withUsername("test")
            .withPassword("test")
            .apply {
                start()
            }

        val redisContainer: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7.0"))
                .withExposedPorts(6379)
                .apply { start() }

        val kafkaContainer: KafkaContainer = 
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
                .apply { start() }

        init {
            System.setProperty("spring.datasource.url", mySqlContainer.getJdbcUrl() + "?characterEncoding=UTF-8&serverTimezone=UTC")
            System.setProperty("spring.datasource.username", mySqlContainer.username)
            System.setProperty("spring.datasource.password", mySqlContainer.password)
            System.setProperty("jakarta.persistence.jdbc.url", mySqlContainer.jdbcUrl)
            System.setProperty("spring.redis.host", redisContainer.host)
            System.setProperty("spring.redis.port", redisContainer.firstMappedPort.toString())
            System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.bootstrapServers)
        }
    }
}
