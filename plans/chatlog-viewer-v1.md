# Chat Opener v1.0.0 —— 计划与施工日志

> 酒馆系（SillyTavern / Tavo）JSONL 聊天记录查看器，安卓独立 App。
> 需求定稿（用户拍板，2026-09-21）：**只做安卓；独立 App、独立 GitHub 公开仓库；
> 气泡式聊天 UI；不做官网；首发一个版本供真机测试，交付 APK 下载地址。**

## 1. 需求背景

用户在 Tavo（酒馆系 AI 角色扮演应用）里积累了大量长篇故事记录，应用本体浏览卡顿，
希望导出 `.jsonl` 后在手机上高性能回看。文件关联/打开体验对齐 MD Opener。

## 2. 数据画像（本机 5 份真实导出摸底）

- 体积 1.3–2.8MB，1,383–2,317 条消息，最大单文件正文约 83 万字
- 格式：首行 header（`user_name` / `character_name` / `create_date` / `chat_metadata`），
  之后每行一条消息（`name` / `is_user` / `is_system` / `send_date` / `mes` / `original_avatar` / `extra`）
- **导出无 `swipes` 字段**（无候选回复膨胀问题）；解析器仍做 swipes 回退以兼容 ST 原生导出
- 单条消息：中位 190–320 字，P95 ≤1531 字，最长 5,255 字（>2000 字的整文件 <40 条）
- `send_date` 形态：ISO `2026-05-31T02:30:00.000`（Tavo）；需兼容 ST 经典
  `October 11, 2023 5:34:00pm` 与毫秒/秒时间戳

## 3. 关键技术决策

| 决策点 | 结论 | 理由 |
|---|---|---|
| 渲染引擎 | 原生 Compose LazyColumn，**不用 WebView** | 数千条独立卡片是虚拟化列表的标准场景；WebView 巨型 DOM 正是 Tavo 卡顿根源。与 MD Opener（线性文档）不同 |
| 富文本 | 轻量行内标记（粗体/斜体/代码）转 AnnotatedString，remember 缓存 | 酒馆文排版需求轻；正则扫描数百字符成本可忽略 |
| 超长消息 | >1200 字折叠，点击展开；搜索命中强制展开 | 5000+ 字单条全排版会掉帧；折叠后只解析前缀 |
| 搜索 | 内存全文扫描 + 命中高亮 + 逐条跳转（远距 scrollToItem 近距 animate） | 83 万字 contains 扫描毫秒级，无需索引 |
| 解析器 | 纯 Kotlin + kotlinx-serialization JsonElement，不碰 Android API | JVM 单测可直接跑（org.json 在单测是 stub）；逐行容错 |
| 时间解析 | 手写解析（时间戳/ISO/ST 英文），不用 java.time | minSdk 24，java.time 需 API 26 |
| 复用 MD Opener | intent-filter 结构 / 编码检测 / DataStore / 主题配色 / CI 流水线 / 返回键分级 | 同作者视觉语言与工程纪律，降低新仓库维护成本 |
| 版本 | v1.0.0（versionCode 1） | 首个可用版本 |
| 签名 | 新仓库独立 keystore（PKCS12），CI secrets 注入 | 与 MDOpener 密钥隔离；固定签名保证可覆盖安装 |

## 4. 范围（v1.0.0）

**做**：.jsonl 文件关联（VIEW/SEND）、首页选择器、气泡列表（用户/AI/系统三分、
日期分隔、折叠）、行内标记、搜索（高亮+跳转+计数）、统计概览、跳到最新、
主题/字号/时间戳开关/系统消息开关、编码检测、最近记录（内存级）。

**不做（有意留尾）**：官网页面（用户明确不做）、书架页/批量管理、swipes 候选查看、
导出 txt/md、正则搜索、消息内代码块多行渲染、iPad/横屏专项适配。

## 5. 结构

```
├── android/src/main/java/com/honlnk/chat_opener/app/
│   ├── MainActivity.kt          # VIEW/SEND intent 处理
│   ├── MainViewModel.kt         # IO 读取 + Default 解析
│   ├── core/
│   │   ├── ChatLogParser.kt     # JSONL 解析（容错/统计/时间）
│   │   ├── InlineMarkup.kt      # 行内标记 → AnnotatedString
│   │   ├── UriReader.kt         # 编码检测读取（同 MDOpener）
│   │   └── Store.kt             # DataStore 设置
│   ├── model/Models.kt
│   └── ui/                      # AppRoot / Home / Chat / Settings / 主题
└── android/src/test/            # ChatLogParserTest（9 例）
```

## 6. 施工日志

### 2026-09-21 ① 骨架与全部代码（本会话）

- 摸底 MDOpener 工程，复制其 gradle/CI/intent-filter/编码检测/主题骨架，包名
  `com.honlnk.chat_opener.app`，仓库 `honlnk/ChatOpener`
- 解析层 + 气泡 UI + 入口/设置全部完成；解析器 9 例单测随 CI 门禁
- 偏差：无（实现与本计划同步起草）

### 2026-09-21 ② CI / 密钥 / 发版

- 本机 keytool 生成发布 keystore（PKCS12），存 `~/project/ChatOpener-signing/`
  （**仓库外**，密码同目录 password.txt，需用户自行备份）
- secrets：ANDROID_KEYSTORE_BASE64 / ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS
- push main 触发构建（assembleRelease + testReleaseUnitTest 门禁），
  Release published 时自动挂 `ChatOpener-<tag>.apk` + `ChatOpener-latest.apk`
- 验收标准：CI 绿、Release 资产可下载、`latest` 直链 200

### 2026-09-21 ③ 首轮构建修复（施工日志）

- 第一次 CI 红：`BubblePalette` 字段声明 Int，`0xFFxxxxxx` 字面量超 Int.MAX 实为 Long，
  16 处类型不匹配。修复：字段与 `c()` 扩展改 Long。教训记档：
  **零本地 SDK 环境下，Compose 颜色字面量赋给自定义 Int 字段是静态审读盲区，颜色容器一律 Long**。
- 第二次 CI 绿：编译 + 9 例单测 + 签名打包全通过（run 35623794977）
