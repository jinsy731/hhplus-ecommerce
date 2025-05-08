package kr.hhplus.be.server.user.entrypoint.http

import kr.hhplus.be.server.shared.web.CommonResponse
import kr.hhplus.be.server.user.application.UserPointCommand
import kr.hhplus.be.server.user.application.UserPointService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class UserPointController(private val userPointService: UserPointService) : UserPointApiSpec {

    override fun charge(userId: Long, req: UserPointRequest.Charge): ResponseEntity<CommonResponse<UserPointResponse.Charge>> {
        val result = userPointService.charge(req.toCmd(userId))
        return ResponseEntity.ok(CommonResponse(UserPointResponse.Charge(
            userId = result.userId,
            point = result.pointAfterCharge.amount,
            updatedAt = result.updatedAt
        )))
    }

    override fun retrieve(userId: Long): ResponseEntity<CommonResponse<UserPointResponse.Retrieve>> {
        val result = userPointService.retrievePoint(UserPointCommand.Retrieve(userId))
        return ResponseEntity.ok(CommonResponse(UserPointResponse.Retrieve(
            userId = result.userId,
            point = result.point.amount,
            updatedAt = result.updatedAt
        )))
    }
}

