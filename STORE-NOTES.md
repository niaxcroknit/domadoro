# 플레이스토어 제출 메모

## 앱 ID
- `applicationId = "com.domadoro.app"` (스토어에 등록되는 이름, 한 번 올리면 변경 불가)
- 코드의 패키지 이름(`com.tomatotimer.app`)은 사용자에게 보이지 않아서 그대로 둬도 돼요.
- 예전 ID(`com.tomatotimer.app`)로 설치해 둔 테스트 앱이 있다면 새 앱으로 따로 설치돼요. 예전 앱은 지워 주세요.

## 권한과 심사 때 적을 내용

### 포그라운드 서비스: 특수 용도(specialUse)
Play Console › 앱 콘텐츠 › 포그라운드 서비스 권한에서 아래처럼 설명하면 돼요.

> 사용자가 직접 시작한 집중(포모도로) 타이머가 진행되는 동안, 다른 앱을 쓰거나 화면을 꺼도 남은 시간을
> 알림으로 계속 보여주고 정해진 시각에 종료를 알리기 위해 사용합니다. 사용자가 시작 버튼을 눌렀을 때만
> 실행되며, 멈추거나 타이머가 끝나면 즉시 종료됩니다. 위치·카메라·마이크 등 다른 기능은 사용하지 않습니다.

- 매니페스트의 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 값: `pomodoro_focus_timer`
- 심사에서 시연 영상을 요구할 수 있어요: 시작 → 홈으로 나가기(작은 창) → 화면 잠금(알림) → 종료 알림 순서로 찍으면 돼요.

### 정확한 알람: SCHEDULE_EXACT_ALARM
- `USE_EXACT_ALARM`은 알람·캘린더 앱 전용이라 넣지 않았어요(넣으면 거절될 수 있음).
- Android 14부터는 처음에 꺼져 있어요. 앱 설정의 **"정확한 시간에 알림 받기"** 버튼으로 사용자가 직접 켜요.
- 꺼져 있어도 타이머는 동작하지만, 화면이 오래 꺼져 있으면 종료 알림이 몇 분 늦을 수 있어요.

### 그 외
- 알림(POST_NOTIFICATIONS): 첫 시작 때 요청.
- 진동(VIBRATE): 종료 알림 진동.
- 배터리 최적화 예외는 권한으로 요청하지 않고, 설정 화면으로 이동하는 버튼만 둬요(스토어 정책상 안전한 방식).

## 출시 전 체크
- `BACKGROUND-TESTING.md`의 실기기 항목 확인
- 서명 키 만들기(Android Studio › Build › Generate Signed App Bundle), 키 파일은 잃어버리지 않게 따로 보관
- 개인정보처리방침 URL: https://app.notion.com/p/4024235ad3ee80378962c8d896e7c424
  (Play Console › 앱 콘텐츠 › 개인정보처리방침 칸에 붙여 넣기. 원문은 `play-store/privacy-policy.md`)
- 연락처 이메일: kimseohappiness@gmail.com
- 데이터 보안 양식: "사용자 데이터를 수집하거나 공유하지 않음"으로 답하면 돼요(인터넷 권한 없음).
