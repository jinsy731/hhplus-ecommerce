package kr.hhplus.be.server.shared

import io.kotest.matchers.shouldBe
import kr.hhplus.be.server.order.domain.model.Order
import kr.hhplus.be.server.order.domain.model.OrderItem
import kr.hhplus.be.server.order.domain.model.OrderStatus
import kr.hhplus.be.server.point.domain.model.UserPoint
import kr.hhplus.be.server.product.domain.product.model.Product
import kr.hhplus.be.server.product.domain.product.model.ProductStatus
import kr.hhplus.be.server.shared.domain.Money
import org.junit.jupiter.api.Test

/**
 * TestEntityUtils의 실제 사용 예시를 보여주는 예제 테스트
 * 
 * 이 테스트는 단위 테스트에서 엔티티의 ID가 필요한 상황들을 시뮬레이션하고,
 * TestEntityUtils를 사용하여 이를 해결하는 방법을 보여줍니다.
 */
class TestEntityUtilsExampleTest {

    @Test
    fun `주문과 주문 아이템 간의 관계 테스트에서 ID 설정 예시`() {
        // given: 주문 생성
        val order = Order(
            userId = 1L,
            status = OrderStatus.CREATED
        )
        
        // 주문에 ID 설정 (예: 100L)
        TestEntityUtils.setEntityId(order, 100L)
        
        // 주문 아이템 생성
        val orderItem = OrderItem(
            productId = 1L,
            variantId = 1L,
            quantity = 2,
            unitPrice = Money.of(10000)
        )
        
        // 주문 아이템에 ID 설정 (예: 200L)
        TestEntityUtils.setEntityId(orderItem, 200L)
        
        // when: 주문과 주문 아이템 연결
        orderItem.order = order
        
        // then: ID가 정상적으로 설정되었는지 확인
        order.id shouldBe 100L
        orderItem.id shouldBe 200L
        orderItem.order?.id shouldBe 100L
    }
    
    @Test
    fun `상품 목록에서 특정 ID를 가진 상품 검증 예시`() {
        // given: 여러 상품 생성
        val products = listOf(
            Product(
                name = "상품1",
                basePrice = Money.of(10000),
                status = ProductStatus.ON_SALE
            ),
            Product(
                name = "상품2", 
                basePrice = Money.of(20000),
                status = ProductStatus.ON_SALE
            ),
            Product(
                name = "상품3",
                basePrice = Money.of(30000),
                status = ProductStatus.ON_SALE
            )
        )
        
        // when: 순차적으로 ID 설정 (1L부터 시작)
        TestEntityUtils.setEntityIds(products, startId = 1L)
        
        // then: 각 상품의 ID가 올바르게 설정되었는지 확인
        products[0].id shouldBe 1L
        products[1].id shouldBe 2L
        products[2].id shouldBe 3L
        
        // 특정 ID를 가진 상품 찾기 테스트
        val targetProduct = products.find { it.id == 2L }
        targetProduct?.name shouldBe "상품2"
    }
    
    @Test
    fun `사용자 포인트에서 ID를 활용한 비즈니스 로직 테스트 예시`() {
        // given: 사용자 포인트 생성
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(10000)
        )
        
        // 특정 ID로 설정 (예: 데이터베이스에서 조회한 것처럼)
        TestEntityUtils.setEntityId(userPoint, 999L)
        
        // when: 포인트 사용
        val usageAmount = Money.of(3000)
        userPoint.use(usageAmount, java.time.LocalDateTime.now())
        
        // then: ID는 그대로 유지되고 잔액만 변경되었는지 확인
        userPoint.id shouldBe 999L
        userPoint.balance shouldBe Money.of(7000)
    }
    
    @Test
    fun `엔티티 동등성 비교에서 ID 활용 예시`() {
        // given: 같은 속성을 가진 두 사용자 포인트 생성
        val userPoint1 = UserPoint(userId = 1L, balance = Money.of(10000))
        val userPoint2 = UserPoint(userId = 1L, balance = Money.of(10000))
        
        // when: 다른 ID 설정
        TestEntityUtils.setEntityId(userPoint1, 100L)
        TestEntityUtils.setEntityId(userPoint2, 200L)
        
        // then: ID가 다르므로 다른 엔티티로 구분됨
        userPoint1.id shouldBe 100L
        userPoint2.id shouldBe 200L
        
        // 비즈니스 로직에서 ID를 활용한 구분 가능
        val entityIds = listOf(userPoint1, userPoint2).mapNotNull { it.id }
        entityIds shouldBe listOf(100L, 200L)
    }
    
    @Test
    fun `version 필드 설정을 통한 낙관적 락 테스트 예시`() {
        // given: 사용자 포인트 생성
        val userPoint = UserPoint(
            userId = 1L,
            balance = Money.of(10000)
        )
        
        // when: ID와 version 설정
        TestEntityUtils.setEntityId(userPoint, 123L)
        TestEntityUtils.setEntityField(userPoint, "version", 5L)
        
        // then: 설정된 값들이 올바른지 확인
        userPoint.id shouldBe 123L
        userPoint.version shouldBe 5L
        
        // version을 활용한 낙관적 락 시뮬레이션
        val currentVersion = userPoint.version
        // ... 비즈니스 로직 수행 ...
        TestEntityUtils.setEntityField(userPoint, "version", currentVersion!! + 1)
        userPoint.version shouldBe 6L
    }
} 