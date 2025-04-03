# ✅ API 명세서
> 본 문서에서는 공통 요청형식 및 응답형식에 대한 명세와 잔액 충전/조회, 쿠폰 발급/조회, 주문/결제, 인기 상품 조회 API에 대한 명세를 기술한다.

# 목차

- [공통](#공통)
    - [요청 형식](#요청-형식)
    - [응답 형식](#응답-형식)
- [1. 잔액 충전](#1-잔액-충전)
- [2. 잔액 조회](#2-잔액-조회)
- [3. 상품 목록 조회](#3-상품-목록-조회)
- [4. 쿠폰 발급](#4-쿠폰-발급)
- [5. 보유 쿠폰 목록 조회](#5-보유-쿠폰-목록-조회)
- [6. 주문 생성](#6-주문-생성)
- [7. 인기 상품 조회](#7-인기-상품-조회)
- [✍️ 작성 정보](#✍️-작성-정보)

## 공통
### 요청 형식

#### 요청 헤더
- `Accept`: `application/json`
- `Authorization`: `Bearer ...`
#### 응답 헤더
- `Content-Type`: `application/json` (별도로 명시되지 않는한)


### 응답 형식
#### 정상 응답
```JSON
{
	"code": "SUCCESS",
	"message": "...",
	"data": { 
		"products": [...],
		"pageInfo": {
			"page": 0,
			"size": 20,
			"totalElement": 31,
			"totalPages": 4
		}
	}
}
```

| 필드                       | 타입     | 필수  | 설명                             |
| ------------------------ | ------ | --- | ------------------------------ |
| `code`                   | String | ✅   | 성공 코드                          |
| `message`                | String | ✅   | 성공 메시지                         |
| `data`                   | Object | ❌   | 응답 데이터 객체, 경우에 따라 존재하지 않을 수 있음 |
| `data.pageInfo`          | Object | ❌   | 페이징 요청 시 페이지 관련 정보             |
| `pageInfo.page`          | Int    | ✅   | 현재 페이지 번호                      |
| `pageInfo.size`          | Int    | ✅   | 요청한 페이지 크기                     |
| `pageInfo.totalElements` | Int    | ✅   | 전체 항목 수                        |
| `pageInfo.totalPages`    | Int    | ✅   | 전체 페이지 수                       |

#### 에러 응답
```json
{
	"code": "...",
	"message": "...",
	"fieldErrors": [
		{
			"field": "...",
			"value": "...",
			"errorMessage": "...",
		}
	]
}
```

| 필드                           | 타입       | 필수  | 설명                  |
| ---------------------------- | -------- | --- | ------------------- |
| `code`                       | String   | ✅   | 에러 코드               |
| `message`                    | String   | ✅   | 에러 메시지              |
| `fieldErrors`                | Object[] | ❌   | 필드 검증 에러 (nullable) |
| `fieldErrors[].field`        | String   | ✅   | 에러가 발생한 필드명         |
| `fieldErrors[].value`        | String   | ✅   | 에러가 발생한 입력값         |
| `fieldErrors[].errorMessage` | String   | ✅   | 필드 검증 에러 메시지        |

## 1. 잔액 충전
- 사용자에게 지정된 금액만큼 잔액을 충전합니다.
- **충전 금액은 0보다 커야 하며**, 존재하지 않는 사용자에 대해 실패합니다.

### POST `/api/v1/users/{userId}/balance`

#### Path Parameters
| 이름       | 타입   | 필수  | 설명      |
| -------- | ---- | --- | ------- |
| `userId` | Long | ✅   | 사용자 식별자 |

#### Request Body
```json
{
  "amount": 5000
}
```

| 필드     | 타입  | 필수  | 설명            |
| ------ | --- | --- | ------------- |
| amount | Int | ✅   | 충전할 금액 (0 초과) |

#### Response (200 OK)
```json
{
  "code": "SUCCESS",
  "message": "잔액이 충전되었습니다.",
  "data": {
    "userId": 1,
    "balance": 15000
  }
}
```

| 필드        | 타입   | 필수  | 설명    |
| --------- | ---- | --- | ----- |
| `userId`  | Long | ✅   | 유저 ID |
| `balance` | Int  | ✅   | 유저 잔액 |

#### 오류 예시
| 상태코드 | 코드               | 설명          |
| ---- | ---------------- | ----------- |
| 404  | `USER_NOT_FOUND` | 존재하지 않는 사용자 |
| 400  | `INVALID_AMOUNT` | 충전 금액이 0 이하 |

---

## 2. 잔액 조회
- 사용자의 잔액을 조회합니다.

### GET `/api/v1/users/{userId}/balance`

#### Response (200 OK)
```json
{
  "code": "SUCCESS",
  "message": "...",
  "data": {
    "userId": 1,
    "balance": 12000
  }
}
```

| 필드        | 타입   | 필수  | 설명    |
| --------- | ---- | --- | ----- |
| `userId`  | Long | ✅   | 유저 ID |
| `balance` | Int  | ✅   | 유저 잔액 |

---

## 3. 상품 목록 조회
- 상품 목록을 조회합니다.
- 상품이 없는 경우 빈 배열을 반환합니다.

### GET `/api/v1/products`

#### Query Parameters
| 이름     | 타입  | 필수  | 기본값 | 설명     |
| ------ | --- | --- | --- | ------ |
| `page` | Int | ❌   | 1   | 페이지 번호 |
| `size` | Int | ❌   | 20  | 페이지 크기 |

#### Response
```json
{
	"code": "SUCCESS",
	"message": "상품 목록이 조회되었습니다.",
	"data": {
		"products": [
			{
			  "productId": 1,
			  "name": "티셔츠",
			  "variants": [
				{
				  "variantId": 101,
				  "option": "검정 / L",
				  "price": 20000,
				  "stock": 10
				}
			  ]
			}	
		],
		"pageInfo": {
			"page": 0,
			"size": 20,
			"totalElement": 31,
			"totalPages": 4
		}
	}
}
```

| 필드                       | 타입       | 필수  | 설명                 |
| ------------------------ | -------- | --- | ------------------ |
| `productId`              | Long     | ✅   | 유저 ID              |
| `name `                  | Int      | ✅   | 유저 잔액              |
| `variants`               | Object[] | ✅   | 옵션                 |
| `variants[].variantId`   | Long     | ✅   | 옵션 ID              |
| `variants[].option`      | String   | ✅   | 옵션 이름              |
| `variants[].price`       | Int      | ✅   | 가격                 |
| `variants[].stock`       | Int      | ✅   | 재고                 |
| `pageInfo`               | Object   | ✅   | 페이징 요청 시 페이지 관련 정보 |
| `pageInfo.page`          | Int      | ✅   | 현재 페이지 번호          |
| `pageInfo.size`          | Int      | ✅   | 요청한 페이지 크기         |
| `pageInfo.totalElements` | Int      | ✅   | 전체 항목 수            |
| `pageInfo.totalPages`    | Int      | ✅   | 전체 페이지 수           |


---

## 4. 쿠폰 발급
- 사용자에게 쿠폰을 발급합니다.
- 잔여 수량이 부족한 경우 발급에 실패합니다.

### POST `/api/v1/users/{userId}/coupons`

#### Path Variable

| 필드     | 타입   | 필수  | 설명    |
| ------ | ---- | --- | ----- |
| userId | Long | ✅   | 유저 ID |


#### Request Body
```json
{
	"couponId": 12
}
```

| 필드       | 타입   | 필수  | 설명         |
| -------- | ---- | --- | ---------- |
| couponId | Long | ✅   | 발급받을 쿠폰 ID |

#### Response
```json
{
  "code": "SUCCESS",
  "message": "쿠폰이 발급되었습니다.",
  "data": {
    "userCouponId": 1234,
    "status": "UNUSED"
  }
}
```

| 필드             | 타입     | 필수  | 설명      |
| -------------- | ------ | --- | ------- |
| `userCouponId` | Long   | ✅   | 유저쿠폰 ID |
| `status `      | String | ✅   | 쿠폰 상태   |

#### 에러 코드

| 코드               | 설명          |
| ---------------- | ----------- |
| `OUT_OF_STOCK`   | 쿠폰 잔여 수량 부족 |
| `ALREADY_ISSUED` | 이미 발급받은 쿠폰  |

---

## 5. 보유 쿠폰 목록 조회
- 보유한 쿠폰 목록을 조회합니다.

### GET `/api/v1/users/{userId}/coupons`

#### Path Variable

| 필드     | 타입   | 필수  | 설명    |
| ------ | ---- | --- | ----- |
| userId | Long | ✅   | 유저 ID |

#### Response
```json
{
	"code": "SUCCESS",
	"message": "쿠폰 조회에 성공하였습니다.",
	"data": {
	    "coupons": [
		    {
		      "userCouponId": 123,
		      "couponName": "신규가입 5천원",
		      "discountType": "FIXED",
		      "value": "5000",
		      "status": "UNUSED",
		      "expiredAt": "2025-05-01"
		    }
		],
		"pageInfo": {
			"page": 0,
			"size": 20,
			"totalElement": 31,
			"totalPages": 4
		}
	}
}
```

| 필드                       | 타입        | 필수  | 설명                 |
| ------------------------ | --------- | --- | ------------------ |
| `coupons[].userCouponId` | Long      | ✅   | 유저쿠폰 ID            |
| `coupons[].couponName`   | String    | ✅   | 쿠폰 이름              |
| `coupons[].discountType` | String    | ✅   | 할인 타입              |
| `coupons[].value`        | String    | ✅   | 할인값(금액 또는 할인율)     |
| `coupons[].status`       | String    | ✅   | 쿠폰 상태              |
| `coupons[].expiredAt`    | LocalDate | ✅   | 가격                 |
| `pageInfo`               | Object    | ✅   | 페이징 요청 시 페이지 관련 정보 |
| `pageInfo.page`          | Int       | ✅   | 현재 페이지 번호          |
| `pageInfo.size`          | Int       | ✅   | 요청한 페이지 크기         |
| `pageInfo.totalElements` | Int       | ✅   | 전체 항목 수            |
| `pageInfo.totalPages`    | Int       | ✅   | 전체 페이지 수           |

---

## 6. 주문 생성
- 주문을 요청합니다.
- 구매할 상품과 상품옵션, 수량을 입력합니다.
- 쿠폰을 적용하려면 유저쿠폰 ID를 요청에 포함해야 합니다.

### POST `/api/v1/orders`

#### Request Body
```json
{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "variantId": 101,
      "quantity": 2
    }
  ],
  "userCouponId": 123
}
```

| 필드                  | 타입       | 필수  | 설명           |
| ------------------- | -------- | --- | ------------ |
| `userId`            | Long     | ✅   | 유저 ID        |
| `items`             | Object[] | ✅   | 구매할 상품 목록    |
| `items[].productId` | Long     | ✅   | 상품 ID        |
| `items[].variantId` | Long     | ✅   | 상품 옵션조합 ID   |
| `quantity`          | Int      | ✅   | 구매 수량        |
| `userCouponId`      | Long     | ❌   | 사용할 유저쿠폰 ID |


#### Response
```json
{
  "code": "SUCCESS",
  "data": {
    "orderId": 999,
    "status": "PAID",
    "finalTotal": 15000
  }
}
```

| 필드           | 타입     | 필수  | 설명       |
| ------------ | ------ | --- | -------- |
| `orderId`    | Long   | ✅   | 주문 ID    |
| `status`     | String | ✅   | 주문 상태    |
| `finalTotal` | Int    | ✅   | 최종 결제금액 |

#### 에러 코드

| 코드                      | 설명                             |
|---------------------------|----------------------------------|
| `INSUFFICIENT_BALANCE`    | 잔액 부족                        |
| `COUPON_EXPIRED`          | 쿠폰 만료                        |
| `OUT_OF_STOCK`            | 재고 부족                        |

---

## 7. 인기 상품 조회
- 최근 3일간 판매량을 기준으로 인기 상품 5개를 반환합니다.

### GET `/api/v1/products/popular`

#### Response
```json
{
  "data": [
    {
      "productId": 1,
      "name": "반팔티",
      "totalSold": 37
    }
  ]
}
```

| 필드          | 타입     | 필수  | 설명      |
| ----------- | ------ | --- | ------- |
| `productId` | Long   | ✅   | 상품 ID   |
| `name`      | String | ✅   | 상품 이름   |
| `totalSold` | Int    | ✅   | 총 판매수량 |

## ✍️ 작성 정보
| 수정내역  | 작성자 | 작성일        |
| ----- | --- | ---------- |
| 최초 작성 | 진승연 | 2025-04-03 |
