# feat/#9 장소 추출·검증·저장 PRD

## 1. 목표

Instagram 게시물과 릴스 URL을 하나의 API로 받아 공개적으로 허용된 텍스트 메타데이터(제목, 캡션, 설명)를 분석하고, Gemini 2.5 Flash로 장소 후보를 추출한 뒤 Kakao Local API로 실재 장소를 검증한다. 사용자가 확인한 장소는 공용 장소 정보와 회원·커플 전용 저장 정보로 분리해 저장한다.

## 2. 정책 및 범위

- 현재 지원: Instagram 게시물(`/p`, `/posts`)과 릴스(`/reel`)
- 현재 제외: YouTube Shorts, 영상 파일·프레임·음성 분석, 게시물 이미지 OCR
- 릴스와 게시물 모두 제목·캡션·설명 텍스트를 1순위로 사용한다.
- `igsh`, `img_index`를 포함한 query string은 canonical URL에서 제거한다.
- 동일 URL의 콘텐츠 hash·수정 시각이 같으면 기존 결과를 재사용한다.
- 장소 후보는 최대 10개다.

## 3. Instagram 데이터 수집 정책

Meta oEmbed 응답을 임베드 표시 이외의 장소 분석·가공·영구 저장 목적으로 사용하는 경로는 production에서 사용하지 않는다. 공식 API가 분석 목적의 데이터를 허용하는지 별도 법무·플랫폼 검토가 끝나기 전까지 공식 oEmbed provider는 비활성화한다.

공개 HTML 수집 provider는 별도 심화 기능으로 격리한다.

- 기본값 `false`
- 로그인·쿠키·토큰·비공개 콘텐츠 접근 금지
- redirect hostname 재검증, 응답 크기·Content-Type·timeout 제한 필수
- 플랫폼 정책과 법적 검토 후에만 제한적으로 활성화
- 운영 중 즉시 끌 수 있는 feature flag 제공

공식·자체 수집 모두 실패하면 분석 실패로 반환한다. 수집할 수 없는 콘텐츠를 우회하거나 영상 자체를 다운로드하지 않는다.

## 4. 단일 API 계약

`POST /api/v1/place-imports`는 게시물·릴스를 모두 받는다. 서버가 URL 경로로 `INSTAGRAM_POST` 또는 `INSTAGRAM_REEL`을 판별하고, 내부 전략만 분기한다.

```text
ContentAnalysisStrategy
├── ReelTextAnalysisStrategy
└── PostTextAnalysisStrategy
```

처음에는 동일 Gemini 모델에 콘텐츠 유형별 prompt를 적용한다. 실제 모델 분리는 품질·비용 지표가 쌓인 뒤 결정한다.

## 5. 분석·검증

- Gemini 입력은 제목·캡션·설명 텍스트다.
- OCR은 이번 범위에 포함하지 않는다.
- Gemini 후보를 Kakao keyword search에 전달한다.
- 장소명 정규화와 주소 힌트의 지역·주소 일치도를 확인한다.
- 첫 검색 결과를 무조건 확정하지 않는다.
- 애매한 동명이인 결과는 제외하거나 검증 후보로 반환한다.
- Kakao timeout·429·5xx는 provider 장애로 보고 전체 작업을 재시도한다.
- 검색 결과 없음은 후보 제외이며, 모든 후보가 제외되면 `PLACE_NOT_VERIFIED`다.

## 6. 작업 생명주기

```text
RECEIVED → PROCESSING → REVIEW_REQUIRED → COMPLETED
                         └──────────────→ FAILED
```

- 외부 API 호출은 DB 트랜잭션 밖에서 수행한다.
- DB 트랜잭션은 작업 생성·상태 전이·결과 저장으로 짧게 나눈다.
- 자동 재시도는 1회다.
- `PROCESSING`이 stale timeout을 넘으면 다시 처리할 수 있다.
- 서버가 중단되어도 stale 작업을 재처리할 수 있다.
- `FAILED` 작업은 동일 URL 재요청 시 재시도 조건을 만족하면 다시 처리한다.
- 사용자 confirm 성공 후 `COMPLETED`로 전이한다.

## 7. 데이터 공개 범위

- `places`: Kakao 검증을 통과한 공용 장소 정보. 모든 회원의 검색·게시물에서 재사용 가능하다.
- `member_places`: 저장 회원, 별칭, 메모, 저장 시각. 본인과 현재 연결된 상대방에게만 공개한다.
- 커플 해제 후 본인 저장 정보만 유지하고 상대방 저장 정보는 차단한다.
- 새 커플에게 과거 커플의 별칭·메모를 노출하지 않는다.

## 8. 수동 장소 검색

AI 분석 실패와 별개로 Kakao 장소 검색 및 개인 저장을 제공한다.

- `GET /api/v1/places/search`
- `POST /api/v1/places`

검색 API는 조회 전용이며 결과를 DB에 저장하지 않는다. 저장 요청 시 서버가 Kakao 장소 ID를 재검증한 뒤 공용 장소와 회원 저장 관계를 생성한다.

## 9. 비용·보안

- 회원별 일일 분석 100건
- 동일 URL 재사용으로 중복 호출 방지
- Gemini·Kakao API key만 Secret으로 관리한다.
- `GEMINI_ENABLED`, `KAKAO_LOCAL_ENABLED`, crawler flag는 비밀이 아닌 환경 설정으로 관리할 수 있다.
- Gemini key는 URL query parameter가 아니라 `x-goog-api-key` header로 전달한다.
- Gemini 응답은 JSON schema 기반 구조화 응답을 사용한다.
- URL hash를 로그 식별자로 사용하고 원문 URL·API key·인증 헤더를 로그에 남기지 않는다.

## 10. 수용 기준

- 게시물·릴스가 하나의 endpoint에서 유형별 전략으로 처리된다.
- query string의 `igsh`, `img_index`가 canonical URL에 남지 않는다.
- 공식 oEmbed가 production 분석 경로로 사용되지 않는다.
- crawler는 기본 비활성화이며 안전한 redirect·응답 제한을 갖는다.
- 외부 API 호출 중 회원 행 잠금이 유지되지 않는다.
- timeout·429·5xx 재시도와 stale 작업 복구가 동작한다.
- 실패 작업 재요청이 가능하다.
- Kakao 첫 결과를 무조건 확정하지 않는다.
- 잘못된 후보는 공통 BusinessException 오류로 반환된다.
- 검색 API가 조회 중 DB를 변경하지 않는다.
- 장소 저장과 FCM 발송은 서로 독립적으로 성공·실패한다.
- 커플 해제 후 상대방 장소·별칭·메모가 노출되지 않는다.

## 11. 테스트

- 제공된 릴스·게시물 URL canonicalization
- 동일 URL 멱등성 및 content hash 변경 재분석
- metadata·Gemini·Kakao timeout/429/5xx
- 실패 후 1회 재시도와 stale `PROCESSING` 복구
- Kakao 동명이인·주소 불일치·검색 결과 없음
- 잘못된 import/candidate 접근 및 후보 중복
- 검색 API read-only 보장
- 수동 저장 재검증
- 동시 장소 저장 및 milestone event 중복 방지
- 커플 연결 중·해제 후 장소 가시성
