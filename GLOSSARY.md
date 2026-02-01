# Glossary

本プロジェクトの全モジュール・クラス・定数の一覧。
コードリーディングの補助資料として使用。

updated: 2026-02-01

## モジュール・クラス

| 名前 | 種別 | ファイル | 役割 |
|------|------|----------|------|
| `App` | Application | App.kt | 未処理例外のログ記録 |
| `MainActivity` | Activity | MainActivity.kt | Navigation host |
| `AlarmStopActivity` | Activity | activities/AlarmStopActivity.kt | スワイプ解除画面（フルスクリーン） |
| `MainFragment` | Fragment | fragments/MainFragment.kt | 時計表示・次回アラーム表示 |
| `SettingsFragment` | Fragment | fragments/SettingsFragment.kt | アプリ設定画面 |
| `AlarmService` | Service | services/AlarmService.kt | フォアグラウンドサービス（MediaPlayback type） |
| `AlarmReceiver` | BroadcastReceiver | receivers/AlarmReceiver.kt | アラーム発火イベント受信 |
| `BootReceiver` | BroadcastReceiver | receivers/BootReceiver.kt | 起動時アラーム再スケジュール |
| `AlarmAdapter` | RecyclerView.Adapter | adapters/AlarmAdapter.kt | アラーム一覧のAdapter |
| `AlarmRepository` | Repository | repository/AlarmRepository.kt | アラーム永続化・スケジューリング |
| `AppSettings` | Repository | repository/AppSettings.kt | SharedPreferences wrapper |
| `HolidayRepository` | Repository | repository/HolidayRepository.kt | 祝日データ管理（ダミー実装） |
| `Alarm` | data class | models/Alarm.kt | アラームデータモデル |
| `Holiday` | data class | models/Holiday.kt | 祝日モデル |

## Alarmモデルのフィールド

| 名前 | 型 | 役割 |
|------|------|------|
| `id` | String | UUID-based ID |
| `hour` | Int | 時（0-23） |
| `minute` | Int | 分（0-59） |
| `enabled` | Boolean | 有効/無効フラグ |
| `soundUri` | String? | アラーム音URI（null=システムデフォルト） |
| `repeatDays` | Set\<Int\> | 繰り返し曜日（0-6: 日曜=0〜土曜=6） |
| `skipHolidays` | Boolean | 祝日スキップフラグ（β版: 土日スキップ） |

## AlarmRepository主要メソッド

| 名前 | 役割 |
|------|------|
| `saveAlarm()` | アラームをSharedPreferencesに保存 |
| `getAllAlarms()` | 全アラームを取得 |
| `deleteAlarm()` | アラームを削除 |
| `scheduleAlarm()` | AlarmManager経由でスケジュール |
| `cancelAlarm()` | スケジュール済みアラームをキャンセル |
| `rescheduleAllAlarms()` | 全アラーム再スケジュール（起動時） |

## AlarmStopActivity定数

| 名前 | 値 | 役割 |
|------|------|------|
| `requiredSwipePercent` | 0.8f | スワイプ判定閾値（画面幅の80%） |

## 権限一覧

| 権限 | 用途 |
|------|------|
| `SET_ALARM` | アラームスケジュール |
| `SCHEDULE_EXACT_ALARM` | 正確なアラーム設定 |
| `USE_FULL_SCREEN_INTENT` | ロック画面上でのActivity表示 |
| `RECEIVE_BOOT_COMPLETED` | 起動時再スケジュール |
| `VIBRATE` | バイブレーション |
| `FOREGROUND_SERVICE` | フォアグラウンドサービス起動 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | MediaPlayback type指定 |
| `POST_NOTIFICATIONS` | 通知表示（Android 13+） |

## イベントチェーン

```
AlarmManager
  ↓ (時刻トリガー)
AlarmReceiver
  ↓ (startService)
AlarmService (音声再生・バイブ・通知)
  ↓ (startActivity)
AlarmStopActivity (スワイプ解除)
  ↓ (stopService)
AlarmService停止
```

## データ永続化

- **保存形式**: SharedPreferences + Gson JSON直列化
- **保存キー**: `alarms`（JSON配列）
- **設定キー**: `background_image_uri`, `background_opacity`, `skip_holidays_global`
