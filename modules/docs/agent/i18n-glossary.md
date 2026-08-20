# i18n glossary — ko / ja / zh_CN

The agreed terminology for all jvmguard localization: the **UI** (`vaadin-i18n/translations_*.properties`),
the **docs** (`src/content/docs/<locale>/`), and the **website** (`src/i18n/`). Translators and AI-assisted
passes must follow this table so the three surfaces stay consistent. When a term appears in the UI, use
exactly this rendering; when a new term is needed, extend this file first.

## Hard rules (all languages)

- **"jvmguard"** stays lowercase latin, never translated, never capitalized (sentence start included).
- **Product/technology names** stay in English: JProfiler, JFR (Java Flight Recorder), MBean, JVM, LDAP,
  SSO/OIDC, TOTP, H2, install4j, SMTP, REST, API, URL.
- **Config keys, enum constants, file names, and code** are never translated
  (`jvmguard.httpPort`, `recordingConfig`, `event.log`).
- **MessageFormat patterns**: apostrophes are quoting characters — write `''` for a literal apostrophe
  (rare in CJK, but watch English loanwords). Every `{n}` placeholder in the English source must appear in
  the translation; the parity test enforces this.
- **Plurals**: English `choice` patterns may distinguish singular/plural; ko/ja/zh_CN translations normally
  use a single form (drop the `choice` wrapper only if the placeholder still appears — keep the `{0}`).
- **Never interpolate a type noun into a sentence via `{n}`** (e.g. "No saved {0} yet." with
  {0}="threshold sets"). Korean particles (은/는/이/가/을/를) depend on the final consonant of the
  substituted word and cannot be written correctly for a parameter. Instead, either word the message
  generically ("No saved sets yet.") or duplicate the message per object type
  (`recording.transaction.dialog.add.declared`, …). Parameters are for user-provided names (quoted),
  numbers, URLs, and identifiers. If a name parameter would be followed by a Korean particle, reword to a
  colon/end construction ("삭제할 수 없습니다: \"{0}\"").
- **Punctuation**: use each language's native sentence punctuation (。、for ja; 。,for zh_CN; standard
  Korean punctuation for ko). The English "no em dash / no semicolon" prose rules do not apply to CJK text.
- **UI labels quoted in docs prose** use double quotes and match the UI translation once the UI is
  localized (until then, quote the English label).
- **Tone**: ja — polite desu/masu; ko — polite 합니다/하세요; zh_CN — neutral instructional (您 only in
  warnings/errors addressed at the user, otherwise omit the pronoun).

## Core domain terms

| English | ko | ja | zh_CN |
|---|---|---|---|
| threshold | 임계값 | しきい値 | 阈值 |
| trigger | 트리거 | トリガー | 触发器 |
| telemetry | 텔레메트리 | テレメトリ | 遥测 |
| recording | 기록 | 記録 | 记录 |
| recording configuration | 기록 설정 | 記録設定 | 记录配置 |
| transaction | 트랜잭션 | トランザクション | 事务 |
| snapshot | 스냅샷 | スナップショット | 快照 |
| thread snapshot | 스레드 스냅샷 | スレッドスナップショット | 线程快照 |
| memory snapshot | 메모리 스냅샷 | メモリスナップショット | 内存快照 |
| VM / monitored VM | VM / 모니터링되는 VM | VM / 監視対象VM | VM / 受监控的 VM |
| VM pool | VM 풀 | VMプール | VM 池 |
| group (VM group) | 그룹 | グループ | 组 |
| inbox | 수신함 | 受信トレイ | 收件箱 |
| event log | 이벤트 로그 | イベントログ | 事件日志 |
| audit log | 감사 로그 | 監査ログ | 审计日志 |
| connection log | 연결 로그 | 接続ログ | 连接日志 |
| server log | 서버 로그 | サーバーログ | 服务器日志 |
| access level | 액세스 수준 | アクセスレベル | 访问级别 |
| Viewer (role) | 뷰어 | ビューアー | 查看者 |
| Profiler (role) | 프로파일러 | プロファイラー | 分析员 |
| Admin (role) | 관리자 | 管理者 | 管理员 |
| agent (Java agent) | 에이전트 | エージェント | Java 代理 |
| collector | 수집기 | コレクター | 收集器 |
| overhead | 오버헤드 | オーバーヘッド | 开销 |
| sampling | 샘플링 | サンプリング | 采样 |
| guardrails | 가드레일 | ガードレール | 护栏 |
| naming element | 명명 요소 | 命名要素 | 命名元素 |
| webhook | 웹훅 | Webhook | Webhook |
| set (saved config set) | 세트 | セット | 集 |
| action (trigger action) | 액션 | アクション | 操作 |
| data retention | 데이터 보관 | データ保持 | 数据保留 |
| heap | 힙 | ヒープ | 堆 |
| garbage collection / GC | 가비지 컬렉션 / GC | ガベージコレクション / GC | 垃圾回收 / GC |
| thread | 스레드 | スレッド | 线程 |
| call tree | 호출 트리 | コールツリー | 调用树 |
| hot spot | 핫스팟 | ホットスポット | 热点 |
| single sign-on | Single Sign-On(SSO) | シングルサインオン(SSO) | 单点登录(SSO) |
| authenticator app | 인증 앱 | 認証アプリ | 身份验证器应用 |
| API key | API 키 | APIキー | API 密钥 |
| dashboard | 대시보드 | ダッシュボード | 仪表板 |
| interval | 간격 | 間隔 | 间隔 |
| schedule | 예약 | スケジュール | 计划 |
| archive | 아카이브 | アーカイブ | 归档 |
| update available | 업데이트 사용 가능 | 利用可能なアップデート | 可用更新 |
| dark / light theme | 다크 / 라이트 테마 | ダーク / ライトテーマ | 深色 / 浅色主题 |
| profile (JFR profile) | 프로파일 | プロファイル | 配置(profile) |
| policy (transaction policy) | 정책 | ポリシー | 策略 |
| slow / very slow (policy state) | 느림 / 매우 느림 | 低速 / 非常に低速 | 慢 / 很慢 |
| overdue | 기한 초과 | 期限超過 | 超时未完成 |

## Common UI actions (keep short)

| English | ko | ja | zh_CN |
|---|---|---|---|
| Save | 저장 | 保存 | 保存 |
| Cancel | 취소 | キャンセル | 取消 |
| Delete | 삭제 | 削除 | 删除 |
| Edit | 편집 | 編集 | 编辑 |
| Add | 추가 | 追加 | 添加 |
| Close | 닫기 | 閉じる | 关闭 |
| OK | 확인 | OK | 确定 |
| Import / Export | 가져오기 /보내기 | インポート / エクスポート | 导入 / 导出 |
| Settings | 설정 | 設定 | 设置 |
| Log out | 로그아웃 | ログアウト | 退出登录 |
| Account settings | 계정 설정 | アカウント設定 | 账户设置 |
| Add VMs | VM 추가 | VMを追加 | 添加 VM |
| Search | 검색 | 検索 | 搜索 |
| Refresh | 새로 고침 | 更新 | 刷新 |
| Download | 다운로드 | ダウンロード | 下载 |
| Upload | 업로드 | アップロード | 上传 |
| Enabled / Disabled | 사용 / 사용 안 함 | 有効 / 無効 | 启用 / 禁用 |
