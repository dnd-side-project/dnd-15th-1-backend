<div> <img src="https://github.com/user-attachments/assets/66de79f2-213b-42a6-a35e-a686461d0b9b" alt="logo_app" width="220px" height="220px"></div>

# 둘픽 (DulPick)

  숏폼과 링크 공유를 바탕으로 커플이 함께 데이트 장소를 발견하고, 저장하고, 
  같이 데이트 코스로 계획할 수 있는 장소 큐레이션 서비스입니다.

  ## 주요 기능

  - Instagram 게시물·릴스, Naver, Kakao, Tistory 링크 기반 장소 분석
  - Gemini를 활용한 장소 후보 추출과 Kakao 장소 정보 검증
  - 커플 연결 코드 발급·공유 및 커플 저장 장소 조회
  - 장소 검색, 상세 조회, 지도 필터, 데이트 유형별 추천
  - 저장한 장소를 활용한 데이트 코스 생성·저장·조회
  - 코스 내 장소 간 Kakao 도보 경로 조회
  - 데이트 코스 및 커플 활동 FCM 푸시 알림
  - Google, Kakao, Apple 소셜 로그인
  - 회원 프로필·데이트 성향·알림 설정 관리

  ## 서비스 흐름

  1. iOS 앱에서 공유하기 기능으로 둘픽에 장소가 포함된 콘텐츠의 링크를 전달합니다.
  2. Gemini API를 통해 장소 후보를 추출하고 KakaoMap 기반으로 검증해 반환합니다.
  3. 사용자는 분석 결과에서 장소를 선택해 저장 장소로 등록합니다.
  4. 저장한 장소를 연결한 상대와 조합해 데이트 코스를 만들고 공유할 수 있습니다.

  ## 기술 스택

  - Java 21
  - Spring Boot
  - Spring Data JPA, Querydsl
  - MySQL, Flyway
  - Spring Security, OAuth 2.0 Resource Server, JWT
  - Gemini API, Kakao Local·Mobility API
  - Firebase Cloud Messaging
  - Docker, GitHub Actions
