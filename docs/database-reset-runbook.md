# 둘픽 DB 초기화 준비 및 실행 절차

이 문서는 운영 DB 초기화 전 준비 절차와 승인 후 실행 절차를 분리해 관리하기 위한 문서다. 현재 브랜치의 Flyway 실행 경로는 새 DB 기준 `V1`부터 `V10`까지의 최종 스키마 체인으로 재정립되어 있다.

## 현재 기준

- DBMS: MySQL 8
- 신규 기준 migration: `V1__create_member_and_auth_schema.sql` ~ `V10__create_email_announcement_schema.sql`
- JPA: `ddl-auto: validate`
- Flyway: `baseline-on-migrate: false`
- Flyway clean: `clean-disabled: true`

새 migration 체인에는 최종 스키마만 포함하며, 기존 운영 데이터에 의존하는 UPDATE, seed 장소, 이미지 백필은 포함하지 않는다. 과거 누적 migration 파일은 이 브랜치의 실행 경로에서 제거했지만 Git 이력에는 남아 있다.

## 초기화 전에 확인할 항목

1. 서비스 쓰기 요청을 중지할 점검 시간을 확정한다.
2. 운영 DB 전체 dump를 별도 저장소에 보관하고 복구 테스트를 완료한다.
3. `/home/ubuntu/dulpick/content-images`와 장소 이미지 저장 디렉터리를 DB dump와 별도로 백업한다.
4. DB의 `content_images.storage_key`, `place_images.storage_key`와 실제 파일을 대조한다.
5. 복구 대상 원본 URL 목록과 기본 장소 seed 목록을 확정한다.
6. 새 DB에서 기본 Flyway 경로의 `V1`~`V10` 실행 후 애플리케이션 기동과 `ddl-auto: validate` 통과를 확인한다.
7. 운영자 로그인, 회원 로그인, 장소·콘텐츠 조회, 이미지 조회, 백로그 재처리를 점검한다.

## 로컬 또는 별도 검증 DB 리허설

운영 접속 정보 대신 별도 MySQL 검증 DB와 테스트용 환경변수를 사용한다.

```bash
./gradlew clean test --no-daemon
```

실제 MySQL 빈 DB에서 검증할 때는 애플리케이션의 datasource를 검증 DB로 지정하고, Flyway history에 `V1`부터 `V10`까지 기록되는지와 전체 테이블이 생성되는지 확인한다.

## 승인 후 운영 실행 순서

아래 절차는 준비 문서이며, 별도 승인 없이 실행하지 않는다.

1. 서비스 점검 모드 전환 및 쓰기 요청 차단
2. DB dump 생성 및 dump 파일 복구 확인
3. DB 이미지 파일 저장 디렉터리 백업
4. 기존 DB를 보존한 상태로 새 DB 생성
5. 새 DB에 운영 프로필로 애플리케이션을 1회 기동해 `V1`부터 `V10`까지 실행
6. Flyway와 JPA validate 성공 확인
7. 승인된 기준 데이터만 입력
8. 필요한 이미지 백필을 별도 작업으로 실행
9. health, 인증, 장소, 콘텐츠, 이미지, 운영자 콘솔 점검
10. 이상이 있으면 새 DB 사용을 중지하고 기존 DB와 이미지 백업으로 롤백

운영 DB를 직접 DROP, TRUNCATE, DELETE하거나 기존 DB 이름을 변경하는 명령은 이 준비 작업에 포함하지 않는다.

## 데이터 및 파일 복구 원칙

- 회원·인증·토큰·푸시 데이터는 기본 seed로 생성하지 않는다.
- 장소·콘텐츠 seed는 중복 URL과 동일 Instagram media key를 정규화한 뒤 멱등적으로 입력한다.
- 이미지 파일이 없는 콘텐츠는 PUBLIC으로 강제 전환하지 않는다.
- 이미지 백필은 DB migration과 분리된 백그라운드 작업으로 실행한다.
- DB 복구와 이미지 디렉터리 복구는 함께 검증한다.

## 검증 명령

```bash
./gradlew clean build --no-daemon
./gradlew extendedTest --no-daemon
```

실제 운영 초기화는 위 검증이 성공하고 백업 복구 리허설까지 끝난 뒤 별도 승인 후 진행한다.
