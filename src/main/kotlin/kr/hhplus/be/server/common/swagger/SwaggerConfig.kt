package kr.hhplus.be.server.common.swagger

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("이커머스 서비스 백엔드 API")
                    .description("항해플러스 8기 백엔드 - 이커머스 서비스 백엔드 API 문서")
                    .version("v1.0.0")
                    .contact(
                        Contact()
                            .name("HHPlus")
                            .url("https://github.com/jinsy731/hhplus-ecommerce")
                            .email("jinsy731@gmail.com")
                    )
                    .license(
                        License()
                            .name("Apache License Version 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0")
                    )
            )
    }
}
