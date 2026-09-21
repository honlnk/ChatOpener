# Chat Opener

一个轻量的酒馆系（SillyTavern / Tavo）AI 聊天记录查看器。从聊天应用导出 `.jsonl` 记录后，在任意文件管理器里点击即可用本应用打开，以气泡聊天界面流畅回看整段故事——百万字级记录滚动不卡顿。

无联网权限、无广告、无后台行为——只做一件事：把你的聊天记录漂亮地回放出来。

## 功能特性

- 📂 **系统级关联**：注册为 `.jsonl` 文件的默认打开方式（大小写扩展名均可），支持 `content://` 与 `file://` 两种来源；按扩展名 / 按 MIME 的文件管理器均有兜底，也可接收其他应用的「分享」
- 💬 **气泡式聊天回放**：用户 / AI 消息左右分侧、独立配色，系统/旁白消息居中弱化显示，日期自动分组分隔
- ⚡ **原生高性能**：原生 Compose 虚拟化列表（LazyColumn），只渲染可见消息——百万字、数千条记录滚动依然流畅，这是与网页端卡顿体验的根本区别
- ✂️ **超长消息折叠**：超过 1200 字的消息默认折叠，点击展开/收起，避免单条巨幅排版掉帧
- 🔤 **酒馆文轻量排版**：`*星号动作*` 斜体柔色、`**粗体**`、`` `代码` `` 等行内标记原生渲染
- 🔍 **全文搜索**：百万字内存级扫描，命中高亮、逐条跳转、显示当前位置/总数
- 📊 **记录概览**：角色名 / 消息数 / 各方字数 / 起止时间一目了然
- 🔝 **一键跳到最新**：距底部较远时浮现快捷按钮；顶栏实时显示当前阅读位置
- 🌏 **编码检测**：BOM 嗅探 → UTF-8 严格校验 → GB18030 回退（覆盖 GBK / GB2312），中文老文件不乱码
- 🎨 **主题**：跟随系统 / 浅色 / 深色（暖纸 / 深炭双配色，与 MD Opener 同一视觉语言）；字号 12sp–28sp 可调
- 🔙 **分级返回**：外部应用直接打开的记录按系统返回直接退出；应用内打开的记录、设置页逐级退回

## 下载

在 [GitHub Releases](https://github.com/honlnk/ChatOpener/releases) 下载最新 APK（[直链](https://github.com/honlnk/ChatOpener/releases/latest/download/ChatOpener-latest.apk)），签名固定，可直接覆盖安装。

## 支持的格式

每行一个 JSON 对象的 `.jsonl` 聊天记录：

- 首行（可选）header：`{"user_name":"…","character_name":"…","create_date":"…"}`
- 之后每行一条消息：`{"name":"…","is_user":false,"is_system":false,"send_date":"…","mes":"正文"}`

容错：坏行自动跳过；`mes` 缺失时回退 `swipes[0]`；`send_date` 支持毫秒/秒时间戳、ISO 形态（Tavo）与英文形态（SillyTavern 经典），解析失败则不显示时间。

## 技术栈

- **Kotlin** + **Jetpack Compose**（Material 3），原生 LazyColumn 虚拟化渲染（无 WebView）
- JSONL 解析：kotlinx-serialization（JsonElement API，逐行容错）
- 设置持久化：Jetpack DataStore (Preferences)
- 最低支持：Android 7.0（API 24）；目标 SDK：Android 14（API 34）

## 开发方式

无本地构建环境的纯 CI 开发——编译验证全部交给 GitHub Actions，方法见 [MDOpener/DEVELOPMENT.md](https://github.com/honlnk/MDOpener/blob/main/DEVELOPMENT.md)（同一套工作法）。

```bash
gradle assembleRelease   # 需要 JDK 17 + Android SDK 34
```

## License

MIT
