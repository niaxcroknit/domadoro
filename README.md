<p align="center">
  <img src="play-store/icon-512.png" width="112" alt="도마도로 아이콘">
</p>

<h1 align="center">도마도로</h1>

<p align="center">
  <a href="https://niaxcroknit.github.io/domadoro/"><b>▶ 도마도로 바로 열기</b></a>
</p>

## Android Studio에서 열기

1. **File › Open** 으로 이 저장소 폴더를 열어요. 폴더 바로 안에 `settings.gradle.kts`가 있으면 맞게 연 거예요.
   (이미 열어 둔 프로젝트라면 **Git › Pull** 로 최신 내용을 받아요.)
2. Gradle Sync가 끝날 때까지 기다려요. Android 16(API 36) SDK 설치 안내가 뜨면 설치해요.
3. 폰을 USB로 연결(개발자 옵션 › USB 디버깅)하거나 에뮬레이터를 고르고 ▶ 실행.
4. 처음 시작할 때 알림 권한을 **허용**해야 끝날 때 알림이 와요.

APK·스토어용 파일 만드는 법은 [`BUILD-APK.md`](BUILD-APK.md), 스토어 제출 정보는 [`STORE-NOTES.md`](STORE-NOTES.md)와 [`play-store/`](play-store) 폴더에 있어요.

## 기능 자세히


- **토마토 타이머**: 기본 집중 25분 · 휴식 5분. 링이 줄어들고, 끝나면 토마토 하나 수확 + 진동 알림. 이어서 휴식 단계로 넘어가요.
- **작은 창(PIP)**: 타이머가 도는 중에 홈으로 나가면 자동으로 작은 창이 돼요.
  토마토를 누르면 재생·멈춤 버튼 하나가 떠요(건너뛰기는 앱·잠금화면 알림에서). 오른쪽 위 버튼으로 직접 열 수도 있어요.
- **잠금화면 알림**: 반짝이가 들어간 빨간 가로 카드. 토마토 · 큰 남은 시간 · 원형 멈춤·건너뛰기 버튼이 한 줄에 같은 높이로.
  좁은 폰에서는 시간이 잘리지 않게 자동으로 조금 작아져요.
  바깥 알림 틀·위치·AOD 표시는 기기 설정에 따라 달라요.
- **낮/밤 테마**: 낮은 맑은 코랄과 아이보리, 밤은 네이비와 민트. 설정에서 '자동'이면 폰 다크 모드를 따라가요.
- **긴 휴식** (1.1): 집중을 4번 마칠 때마다 15분 긴 휴식. 길이와 간격은 설정에서 바꿔요.
- **하루 목표** (1.1): 오늘 딸 토마토 개수를 정해 두면 타이머 화면에 3 / 6개처럼 보여주고, 다 채우면 축하 화면이 떠요.
- **집중 라벨** (1.1): 타이머 화면의 '# 라벨' 버튼으로 공부·업무·독서 등을 골라요. 라벨은 설정에서 추가·삭제.
- **내 바구니**: 최근 7일 요일별 수확과 한 달 달력(많이 딸수록 진한 색, 목표 달성 날은 점). 라벨별 수확, 하루 평균·목표 달성·연속 기록.
- **홈 화면 위젯** (1.1): 토마토 · 남은 시간 · 오늘 수확 · 재생·멈춤 버튼. 앱을 열지 않고 시작해요.
- **빠른 설정 버튼** (1.1): 알림창을 내려서 도마도로 버튼으로 시작·멈춤. 버튼 아래에 '집중 중 · 12분 남음'.
- **집중 중 방해 금지** (1.1): 켜 두면 집중하는 동안만 방해 금지(중요한 알림만)를 켜고, 휴식 때 다시 풀어요.
- **백색소음**: 타이머 화면 스피커 버튼으로 켜고 끄기. 백색소음·빗소리·깊은 소음·파도 4가지, 소리 크기 조절.
  '휴식 땐 멈추기'를 켜두면 소리가 멈추는 것만으로 쉬는 시간인 걸 알 수 있어요.
- **끝날 때 알려주기**: 화면을 안 봐도 구분되게 집중 끝과 휴식 끝의 소리·진동 리듬이 달라요.
  화면이 꺼져 있어도 정해진 시각에 울리고, '자동으로 이어서'를 켜면 다음 단계가 바로 시작돼요.
- **설정**: 테마, 집중/휴식 시간(25·5 / 50·10 / 90·15 빠른 선택), 알림, 백색소음, 작은 창·잠금화면 알림 설정 바로가기,
  정확한 시간 알림 허용, 배터리 절약 예외 설정 바로가기.
- **미리보기**: `ui/Previews.kt`를 열고 오른쪽 위 Split(또는 Design)을 누르면 낮/밤 화면이 보여요.
- 화면을 꺼도 타이머는 계속 돌고, 상단 알림에서 남은 시간을 볼 수 있어요.

## 폴더 구조

```
app/src/main/java/com/tomatotimer/app/
├── TomatoApp.kt              앱 시작 시 준비
├── data/Prefs.kt             설정·수확 기록·하루 목표·라벨 저장
├── sound/
│   ├── NoisePlayer.kt        백색소음 만들기·재생
│   └── Alerts.kt             끝날 때 소리·진동
├── timer/
│   ├── TimerEngine.kt        타이머의 실제 상태와 동작
│   ├── TimerService.kt       화면이 꺼져도 타이머 유지 + 진행 알림
│   ├── TimerActionReceiver.kt PIP·알림·위젯 버튼, 종료 알람 처리
│   ├── FocusDnd.kt           집중 중 방해 금지 켜고 끄기
│   ├── TimerCheckpoint.kt    앱이 다시 열릴 때 남은 시간 복원
│   ├── NotificationArt.kt    알림 속 토마토 그림
│   └── Notifications.kt      진행·완료 알림 (빨간 가로 카드)
├── widget/TimerWidget.kt     홈 화면 위젯
├── tile/TimerTileService.kt  알림창 빠른 설정 버튼
└── ui/
    ├── MainActivity.kt       화면 전환, PIP 진입
    ├── TimerScreen.kt        메인 타이머
    ├── PipScreen.kt          작은 창 화면
    ├── BasketScreen.kt       내 바구니
    ├── SettingsScreen.kt     설정
    ├── GoalCelebration.kt    하루 목표 달성 축하 화면
    ├── Components.kt         토마토 그림, 버튼, 탭 등 공통 부품
    ├── Previews.kt           Android Studio 미리보기
    └── Theme.kt              낮/밤 색, 글씨체
```

## 알아두면 좋은 점

- 최소 지원 버전은 Android 8.0이에요 (PIP가 이때부터 지원돼요).
- 숫자와 제목은 기기 기본 글꼴(굵게)을 써요. 디자인 기준은 `DESIGN-NOTES.md`에 있어요.
- 스토어에 올릴 때 필요한 정보(앱 ID `com.domadoro.app`, 권한 설명 문구)는 `STORE-NOTES.md`에 있어요.
- 앱이 시스템에 의해 종료됐다가 다시 열려도 남은 시간을 이어서 복원해요. 다만 강제 종료·재부팅 직후 자동 재개는 보장하지 않아요.
- 실기기에서 확인해야 할 항목은 `BACKGROUND-TESTING.md`에 정리돼 있어요.
- 시간 복원 단위 테스트: `app/src/test/.../TimerCheckpointTest.kt` (Android Studio에서 실행).
- 화면 그림 테스트: `app/src/test/.../ui/ScreenshotTest.kt`를 실행하면 `app/build/screens/`에 화면 PNG가 생겨요.
- GitHub에 올리면 Actions가 자동으로 빌드해요. Actions › 최근 실행 › Artifacts에서 테스트용 APK(`domadoro-debug-apk`)와 화면 그림(`domadoro-screens`)을 받을 수 있어요.
