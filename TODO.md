# Analysis BC TODO

## A 담당

- [x] enums/ 전체 (8개)
- [x] entity/AnalysisJob.java
- [x] entity/ContentSentence.java
- [x] entity/BiasAnalysisResult.java
- [x] repository/ 전체 (3개)
- [x] dto/ 전체 (입력 DTO: ContentPreparedEventDto, SentenceInputDto / 출력: BiasAnalysisResultDto)
- [x] exception/code/AnalysisErrorCode.java
- [x] exception/AnalysisException.java
- [x] service/AnalysisService.java — createAnalysisJob (stub), getSentenceInputs

## B 담당

- [ ] service/ — Python FastAPI POST /analyze 호출 구현
- [ ] Python 분석 결과 수신 및 BiasAnalysisResult DB 저장
- [ ] JobStatus RUNNING → SUCCESS / FAILED 전이 완성

## C 담당

- [ ] controller/ — Content BC 수신 엔드포인트
- [ ] converter/ — Entity ↔ DTO 변환
- [ ] dto/ — 조회 응답 DTO
- [ ] exception/ — 예외 처리 (GlobalExceptionHandler에 AnalysisException 추가)
- [ ] 통합 테스트