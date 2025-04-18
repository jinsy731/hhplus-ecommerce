package com.hhplus.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@Profile("local") // 로컬 환경에서만 실행
class DataLoader(private val dataSource: DataSource) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            // JPA 초기화 후 data.sql 실행
            val populator = ResourceDatabasePopulator()
            populator.addScript(ClassPathResource("data.sql"))
            populator.setContinueOnError(true) // 에러가 발생해도 계속 진행
            populator.execute(dataSource)
            println("✅ 초기 데이터 로드 완료")
        } catch (e: Exception) {
            println("❌ 초기 데이터 로드 실패: ${e.message}")
            e.printStackTrace()
        }
    }
}
