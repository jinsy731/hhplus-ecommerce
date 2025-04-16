package kr.hhplus.be.server.product.domain

import jakarta.persistence.*
import kr.hhplus.be.server.common.entity.BaseTimeEntity

@Entity
@Table(name = "option_values")
class OptionValue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_value_id")
    val id: Long = 0L,

    @Column(nullable = false)
    var value: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_spec_id", nullable = false)
    var optionSpec: OptionSpec? = null
) : BaseTimeEntity() {
}
