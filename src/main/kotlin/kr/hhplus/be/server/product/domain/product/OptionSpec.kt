package kr.hhplus.be.server.product.domain.product

import jakarta.persistence.*
import kr.hhplus.be.server.common.entity.BaseTimeEntity
import java.math.BigDecimal

@Entity
@Table(name = "option_specs")
class OptionSpec(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_spec_id")
    val id: Long = 0L,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var displayOrder: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    @OneToMany(mappedBy = "optionSpec", cascade = [CascadeType.ALL], orphanRemoval = true)
    val optionValues: MutableList<OptionValue> = mutableListOf()
) : BaseTimeEntity() {

    fun addOptionValue(optionValue: OptionValue) {
        optionValues.add(optionValue)
        optionValue.optionSpec = this
    }
}
