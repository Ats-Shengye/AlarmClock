# AlarmClock

Androidネイティブのアラームアプリ。

- 祝日検知・カスタムアラーム音・スワイプ解除
- Kotlin / Android 8+ (API 26)
- Material Design 3

> 技術詳細は [GLOSSARY.md](./GLOSSARY.md) を参照

## 概要

曜日繰り返し、カスタムアラーム音、祝日スキップ（β版）、スワイプ解除などの機能を搭載したAndroidアラームアプリ。

## 機能

- **アラームCRUD**: UUID-based ID、時刻指定、曜日繰り返し
- **カスタムアラーム音**: システムデフォルト or content:// URI
- **スワイプ解除**: 画面幅80%、左→右でアラーム停止
- **祝日スキップ**: 土日を自動スキップ（β版）
- **背景画像**: 画像選択 + 不透明度調整
- **起動時再スケジュール**: BootReceiverによる自動復元
- **次回アラーム表示**: 次のアラーム時刻とカウントダウン
- **バイブレーション**: 1秒ON/1秒OFF波形

## 必要環境

- Android 8.0 (API 26) 以上
- Android Studio Hedgehog 以降

## セットアップ

```bash
git clone https://github.com/Ats-Shengye/AlarmClock.git
cd AlarmClock
```

Android Studioでプロジェクトを開き、Gradle同期を実行。
エミュレータまたは実機で実行。

## 技術スタック

- Kotlin 1.9.22
- Gradle 8.13.0
- Target SDK 34, Min SDK 26
- AndroidX, Material Design 3
- Gson 2.10.1, Coroutines 1.7.3

## 開発

```bash
# ビルド
./gradlew assembleDebug

# テスト（未実装）
./gradlew test
```
