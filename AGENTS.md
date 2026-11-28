# AGENTS.md — Material3Tim 协作指南

给在本仓库工作的 AI Agent / 协作者的实战指南。**先读完再动手。**

## 1. 项目是什么

**Material3Tim**：自写 Jetpack Compose UI 全面接管 TIM（QQ NT，`com.tencent.tim`）官方界面。

- **同一个 APK 两种形态**：
  - 装进 TIM 进程 = **LSPosed 模块**（宿主模式，全部数据来自 TIM 内核/QRoute）；
  - 独立安装 = 普通演示 App（`AppDataSources` 全为 null，用示例数据，不能崩溃）。
- **两套 UI**：由 `LocalUiMode` 分发
  - `*Material.kt`：Material 3 Expressive（M3E），组件在 `ui/component/material/`；
  - `*Miuix.kt`：Miuix（`top.yukonga.miuix.kmp`）。
  - 两者共享 Screen/ViewModel/数据层，只在视觉与弹层组件上分开。
- **技术栈**：Kotlin 2.4.21 / AGP 9.4.x / Gradle 9.7.x（JDK 21）、Compose BOM 2026.11.01、Miuix 0.9.5-beta03、minSdk 26。
- **功能范围（已完成）**：消息列表/聊天详情、群文件（搜索/文件夹独立页面）、群管理（踢人/管理员/禁言/退群/解散/群名/群昵称/群简介/发布公告）、系统通知（免打扰过滤/点击跳转）、全局搜索（分类分组）、用户资料页、群成员、群公告、语音（播放/系统录音机录制发送/听筒扬声器切换）、图片（原图/长图/动图）、贴纸与收藏表情、超级表情/QQ 小黄脸、群助手（转发进入列表）、新的朋友（含过滤通知）、好友管理（删好友/备注）、ARK 卡片（聊天记录/群公告点击）、M3E 弹窗（Expressive + 预测性返回）、首页会话分组、通知角标/未读跳转、下拉刷新、滑动引用、会话事件标签与聊天页事件跳转、DataLine 设备会话、双击加一、聊天顶栏头像、原生 TIM 页面入口、支持开发页、M3E 专属 Launcher3 悬浮底栏等。
- **已知限制/待办**：真 MarketFace 贴纸发送部分场景、APNG/魔法表情发送、TIM 原生输入法状态下的一些宿主兼容问题、部分二级页仍示例数据（我的收藏/我的文件/钱包/服务/标签/公众号/表情包）、附件面板剩余项、编辑资料写回、长按菜单「截图」、宽屏 Sidebar Style、Miuix 固定头页面（搜索/群文件面包屑）的玻璃顶栏穿内容效果、通话页暂用 TIM 原生 UI、通知栏消息与已读回执未接（已读回执不做）。Miuix Glass/Voice/File/红包等已完成。详细变更记录见 README 与 git tag。

## 2. 架构分层

```
app/src/main/java/com/material3tim/
├── xposed/            # 宿主侧：Xposed 入口、TimBridge（内核/QRoute 桥）、UiHost、TimUiHook
├── data/
│   ├── model/         # Message.kt / Content.kt / Emoji.kt 等纯数据模型
│   ├── source/        # AppDataSources（接口 + registry）、LiveEvents（数据刷新事件）
│   └── repository/    # 示例数据 + LiveChats/LiveContacts 等内存仓库
├── ui/
│   ├── component/     # 共享组件（material/、miuix/、ime/、AnimatedImage 等）
│   ├── screen/        # 页面（xxxScreen.kt + xxxMaterial.kt + xxxMiuix.kt）
│   ├── navigation3/   # Route 定义（Routes.kt）
│   └── M3TimRoot.kt   # 导航入口、页面分发
└── MainActivity / service 等
```

### 数据通路约定
- UI **只**通过 `AppDataSources` 的接口取数据；接口默认实现安全（null/false），宿主由 `UiHost` 注册 `Tim*Source`。
- 阻塞接口一律 IO 线程调用（`withContext(Dispatchers.IO)`）。
- 后台数据刷新用 `LiveEvents`（`messageEvents` / `dataUpdates`）通知 UI 局部刷新，不要整页重载。
- 常用缓存：`chatMessagesPreview`（进入聊天首帧预览）、`chatInfoCache`、`groupMembersCache`、`downloadMediaByElement`、`LiveChats` 等；新缓存要设 TTL/LRU，避免内存膨胀。

### TimBridge 实现优先级（重要）
桥接能力按以下顺序找通道：
1. **TIM 自己的 QRoute / 仓库 API**（`TroopOperationRepo`、`IEmojiManagerService`、`IAIOEmotionAddApi` 等，与 TIM 原生功能同款，最可靠）；
2. **Kernel / NT 原生接口**（`IKernelMsgService`、`IKernelGroupService` …），部分命令被产品禁用会返回 `-10122`；
3. **旧版 OIDB / servlet / SSO proto**（例如 `0x89a_0`、`FavEmoRoamingHandler`、`ProfileService.Pb.ReqSystemMsgAction.Friend`）；
4. 最后才自实现（HTTP 下载、本地 DB 操作）。
每一步都要 `try/catch` + 日志，失败返回 false/null，**绝不崩溃**。

## 3. 构建 / 发布 / 安装规则

```bash
# 构建 release
./gradlew :app:assembleRelease --console=plain

# 拷贝 APK（按版本命名）
cp app/build/outputs/apk/release/app-release.apk Material3Tim-vX.Y.Z.apk

# 安装 + 重启 TIM（设备序列号见下）
adb -s <DEVICE_SERIAL> install -r app/build/outputs/apk/release/app-release.apk
adb -s <DEVICE_SERIAL> shell am force-stop com.tencent.tim
adb -s <DEVICE_SERIAL> shell am start -n com.tencent.tim/com.tencent.mobileqq.activity.SplashActivity
```

- **版本号**：`app/build.gradle.kts` 中 `versionName` + `versionCode`（当前 **0.2.0 / 2**；0.1.0 为首个公开版本）；**只有用户明确要求发版时才递增，日常修复不要 bump 版本号**。
- **Git**：按你的本地 Git 配置提交（如需签名可用 `git commit -S`）；
  发布时 `git tag -a vX.Y.Z -m "..."`。
- **安装策略（用户已授权）**：构建成功后默认 **安装 + 强制停止 TIM + 重新打开**，不必每次询问；
  但**不允许模拟点击/输入去做功能测试**，也不允许擅自启动其它页面。跑 adb 前确认设备：
  把 `adb devices` 里你自己的测试机序列号填到上面的 `<DEVICE_SERIAL>`。
- 独立 App 形态也应能编译/运行（所有 live source 为 null 时走示例数据或空态）。

## 4. 开发流程（建议）

1. **先分析**：读代码、日志，必要时用本地反编译源码确认 TIM 类/方法/字段（混淆名会随版本变化）。
2. **小步实现**：桥接与 UI 尽量拆成独立改动；改前先想好失败兜底（所有桥方法失败返回 false/null，绝不崩溃）。
3. **本地编译**：`./gradlew :app:compileReleaseKotlin` 通过后再出包。
4. **构建安装**：`./gradlew :app:assembleRelease` → `adb install -r` → force-stop TIM → 重新启动，真机回归。
5. **真机验证**：功能由人工点击验证（不要用 adb 模拟点击/输入）；问题用 logcat（TAG `Material3Tim`）定位。
6. **提交发布**：一个功能/修复一个 commit；只有用户明确要求发版时才递增 `versionName`/`versionCode` 并打 annotated tag，根目录 APK 不入库。

## 5. UI 规范

- **优先官方组件，非非常必要不要自绘**（用户明确要求）。弹窗/Sheet/Loading 用 Material3 或 Miuix 官方实现；M3E 自定义弹窗统一使用 `ExpressiveAlertDialog`（自绘遮罩 + 进出动画 + 预测性返回）。
- **两套 UI 必须同步**：新页面/新交互都要有 `*Material.kt` 与 `*Miuix.kt` 并保持行为一致（视觉各随其风格）。
- **LazyColumn/滚动**：
  - 聊天列表进入时滚到底部，并用 `requestScrollToItem` 在 measure 前定位（避免闪顶部）；
  - 滚动位置用 `rememberSaveable(chat.id)` 保存，且**必须校验 `savedAnchorChatId == chat.id`**，防止跨会话串锚点（曾导致私信跳顶端 bug）；
  - `key(chat.id)` 包裹聊天页子树，避免切换会话时状态复用；
  - 键盘高度用 `KeyboardInsets.rememberKeyboardHeightDp()`（宿主窗口的 Compose ime insets 常不可靠），按「底部 padding 变化量」逐帧同步滚动列表。
- **IME**：统一走 `ChatImeController`（`ui/component/ime/`）——状态机 + 首焦点延迟重绑 + 兜底；不要在各输入栏里散落 `show()/restartInput()` 补丁。聊天页面板打开时返回键先关面板（`BackHandler` 优先级：包详情 > 表情/附件/@ 面板 > 退出页面）。
- **图片**：本地动图用 `AnimatedOrStaticImage`（GIF/APNG/动态 WebP 循环，静态自动回退）；消息图片支持原图下载与长图解码。
- **文案**：新增文案放 `app/src/main/res/values/strings.xml`，不在代码里硬编码（菜单 label 等既有惯例可跟随）。
- **空态/加载/失败重试**：每个新页面都要有，宿主外（source null）显示空态而不是崩溃。
- **日志**：统一 TAG `Material3Tim`，前缀按模块：`perf:`、`ime:`、`emoji:`、`stickerPack:`、`voiceRecord:`、`groupAssistant:`、`manage:`、`atMe:` 等，便于真机抓 logcat。
- **注释**：重要逻辑写中文注释，说明「为什么」而不是「是什么」。

## 5.1 基础控件与弹层约定（优先复用，禁止另造轮子）

### 搜索栏统一规范
- **非特殊页面一律复用首页消息列表顶部的搜索栏**，不要自创搜索样式：
  - M3E：`ui/component/material/SearchBar.kt` 的 `SearchAppBar`（标题 + 胶囊搜索栏 + 展开搜索层）；
  - Miuix：`ui/component/miuix/SuperSearchBar.kt` 的 `SearchBarFake` / `InputField`（与消息页一致的胶囊）；需要完整搜索浮层时用 `SearchStatus.SearchBox` / `SearchPager`；
- 搜索结果展示样式跟页面列表一致（M3E `SegmentedColumn/SegmentedListItem`，Miuix `groupedCardItems`）；
- 输入搜索需加防抖（约 300ms），查询期间取消/丢弃旧请求结果。

### 群文件 / 历史消息约定
- **群文件文件夹是独立路由页面**（`Route.ChatFiles(chatId, folderId, folderName)`），点文件夹只 push，不做当前页刷新；
- 群文件搜索走 TIM SSO `GroupFileAppSvr.GetFileSearch`（`tencent.im.cs.cmd0x383`），不要使用 kernel `searchGroupFileByWord`；
- 拉取文件夹列表后必须校验条目的 `parentFolderId` 与请求目录一致：TIM 部分 OIDB 组合会忽略 folderId 返回上层目录内容（空文件夹显示父目录文件的根因）；
- 群文件加载动画：M3E 用 `ContainedLoadingIndicator`，Miuix 用 `InfiniteProgressIndicator`；
- **更早消息分页缓存原始 MsgRecord**（`OlderState.rawRecords`），返回时重新 map；图片/视频缩略图异步下载完成后通过 `reloadOlderMessages` 回填，聊天页做有限次轮询，避免历史图片永久空白。

### 长按/点击弹出的 popup 菜单（消息气泡、群成员、收藏表情、会话卡片、附件相机等）
- **M3E**：`OffsetAnchoredExpressiveMenu(expanded, onDismissRequest, anchorOffset)` + `SelectableDropdownMenuItem`
  （`ui/component/material/ExpressiveMenu.kt`）。
  - 锚点用 `Modifier.trackPressPosition`（或手势 `awaitFirstDown` 的 `down.position`）得到 `IntOffset` 传入
    `anchorOffset`，实现「从长按/点击位置弹出」；
  - 菜单项 = 图标 + 文字，危险项 `MaterialTheme.colorScheme.error`；点击项后**先关闭**再执行业务动作。
- **Miuix**：`WindowListPopup` + `ListPopupColumn` + `DropdownImpl`/`DropdownItem`/`DropdownDefaults`。
  - 定位**必须复用** `MessageActionMenu.kt` 里的 `BubbleAbovePositionProvider`（`internal`）：
    `BubbleAbovePositionProvider(isMe = false/true, gapPx = MenuBubbleGap.toPx(), marginPx = 8.dp.toPx())`，
    `alignment = Align.BottomStart/BottomEnd`（**正上方优先**，放不下落到下方，x/y 自动 clamp）；
  - `enableWindowDim = false` + `LocalBubbleMenuOverlay`（页面级全屏遮罩，可盖状态栏）+
    `TransparentSystemBarForPopup()`，`onDismissFinished = onDismiss`；
  - 不要自己另写 PositionProvider（曾因成员菜单自写「下方优先」与气泡菜单不一致而被用户发现）。
- 两套的弹出方位、间距（gap=`MenuBubbleGap`、margin 8dp）、遮罩、动画必须一致。

### M3E 弹窗（带输入框 / 确认 / 选择）
- **基座**：`ExpressiveAlertDialog(show, onDismissRequest, onExitFinished, content)`
  （`ui/component/material/ExpressiveAlertDialog.kt`：自绘全屏遮罩 + 进出动画 + 预测性返回）。
  所有自定义弹窗都走它，**不要直接使用 material3 `AlertDialog`**（会话卡片菜单的删除确认是历史例外，新代码不要模仿）。
- **现成变体**（同文件，直接调用，别复制粘贴实现）：
  - `ExpressiveConfirmDialog`：危险确认（删好友/踢人/清空聊天记录等）。
  - `ExpressiveInputDialog(show, title, initialText, placeholder, confirmText, onConfirm, onDismissRequest,
    singleLine, maxLines, minHeight, confirmEnabled)`：单输入框（改群名、群名片、备注、创建文件夹等）。
  - `ExpressiveDurationDialog(show, title, onConfirm(days, hours, minutes), onDismissRequest)`：
    天/时/分三输入框（群成员「自定义禁言时长」）。
  - `ExpressiveListDialog(show, title, options, onSelect, onDismissRequest)`：列表单选。
- **分段列表**（设置/资料页风格）：`SegmentedColumn` + `SegmentedListItem`（含 `SegmentedSwitchItem`/
  `SegmentedDropdownItem`/`SegmentedRadioItem`/`SegmentedCheckboxItem`/`SegmentedTextField`）；
  卡片用 `TonalCard`；页面骨架用 `ExpressiveScaffold` + `TopBarBackButton`；搜索用 `SearchAppBar`。
- **底部 sheet**：M3E 现在只有「@ 成员面板」用 `ModalBottomSheet`；管理类动作已全部迁到 popup
  （群成员长按菜单就是范例），不要新增底部 sheet。

### Miuix 对应控件（行为要跟 M3E 一致，视觉随 Miuix）
- **弹窗（输入/确认/选择）**：`OverlayDialog(show, title, summary, onDismissRequest, content)`；
  内容用 `TextField`（`top.yukonga.miuix.kmp.basic.TextField`）+ 底部按钮行：
  `Row { TextButton(取消); TextButton(确认, colors = ButtonDefaults.textButtonColorsPrimary()) }`。
  参考实现：`ChatMembersMiuix.kt` 编辑群名片/自定义禁言、`GroupFilesMiuix.kt` 创建文件夹、
  `ui/component/miuix/ScaleDialog.kt`。
- **底部 Sheet**：`OverlayBottomSheet`（@ 面板 `ChatAtSheetMiuix`、资料页成员操作 `MemberActionsDialogMiuix`），
  自带跟手拖拽/预测性返回，别自己写 Dialog 平移。
- **popup 菜单**：见上文「Miuix」条目（`WindowListPopup` + `BubbleAbovePositionProvider` + 页面级遮罩）。
- **页面骨架/列表**：miuix `Scaffold`、`TopAppBar`/`SmallTopAppBar`、`TabRow`、`Card`；
  搜索栏用 `ui/component/miuix/SuperSearchBar.kt`；Miuix 没有 `SegmentedList`/`ExpressiveMenu`
  一一对应的组件，用官方 `Card`/`BasicComponent` 拼，但交互、位置、动画要与 M3E 对齐。
- **会话卡片长按菜单**：`ChatCardActionMenu.kt` 内按 UI 模式分发（M3E AlertDialog+Dropdown / Miuix OverlayDialog+WindowListPopup），
  是「两套 UI 同一交互」的参考写法。

## 5.2 聊天记录（合并转发）专项约定

- **内核语义**：`getMultiMsg(contact, rootMsgId, parentMsgId)`——`rootMsgId` = **最外层**记录消息 id，`parentMsgId` = **直接外层**记录消息 id；从聊天页直接打开记录时两者都为 0。
  参数组合由 `TimBridge.buildChatRecordCombos` 集中管理：
  - 普通入口：`(msgId,msgId)` → `(0,msgId)` → 旧兜底；
  - 嵌套（parent/root 都有值）：`(root,msgId)` → `(msgId,root)` → `(parent,msgId)` → `(msgId,parent)` → 兜底；
  - `code=2 (server return failed)` 只重试一次（800ms），总预算 **12s**，超时返回 null（UI 显示失败+重试），不要把所有组合都重试（曾累计 30s）。
- **缓存**：`loadChatRecord` 成功后缓存**原始 MsgRecord**（LRU 20 / TTL 5min，key=`chatId:msgId:elementId:parentMsgId:rootMsgId`）；命中后重新 map（几 ms，可拿到刚下载的媒体路径）立即返回，日志 `loadChatRecord cache hit`。
- **记录内媒体**：
  - 图片/视频缩略图/视频文件请求需做配对：`msgId ∈ {innerMsgId, rootMsgId}` × `peer ∈ {inner record peer, outer chat peer}` × downloadType 变体；日志 `richmedia request: pair=..`；
  - 图片拿不到 rich media 时用 `gchat.qpic.cn`（md5）兜底成功率高；
  - **记录内视频无法播放**：内核 `getVideoPlayUrlV2`/旧 API 在 inner/root 上下文全部返回 `code=4`，rich media 下载也不回调；这是 TIM 未开放转发记录视频下载通道的限制。UI 侧视频显示占位卡片（时长/大小/播放图标），点击提示「视频暂无法播放」，不要反复尝试 Hook TIM 内部下载器（稳定性风险）。
- **UI 传参链**：`Route.ChatRecord(chatId,msgId,elementId,parentMsgId,rootMsgId)`；记录页通过 `LocalChatRecordParentMsgId`（当前页 msgId）/`LocalChatRecordRootMsgId`（最外层 root，顶层页 = 自身 msgId）向子树提供，内嵌卡片点击时带上两者。
- **日志关键字**：`loadChatRecord enter/try/ok/no result/budget exceeded/cache hit|miss`、`chatRecord: screen enter/loaded`、`chatRecord: open from bubble`、`richmedia request/complete`、`videoPlayUrl ctx/api/code`。

## 5.3 Miuix 0.9.5-beta03 升级与主题引擎约定（v0.5.262 起）

### 依赖版本（2026-11）
- Kotlin **2.4.21**、Compose BOM **2026.11.01**（Compose 1.13）、Miuix **0.9.5-beta03**；
- **material3 必须由 Compose BOM 管理（当前 1.5.0），不要显式 pin 旧 alpha 版本**：
  升级 Compose 1.13 后曾把 material3 固定为 `1.5.0-alpha28`（按旧 Compose 编译），
  M3E 组件在 1.13 上走兼容路径，导致首页滚动 p90 26~34ms、janky ~13%；
  改为 BOM 管理后恢复到 p90 15ms / 3.37%（Slow UI thread 回到升级前水平）。
  升级 Compose / Miuix 时必须同步检查 resolved material3 版本是否与 BOM 对齐。
- 新增 `miuix-glass`、`miuix-glass-icons`（Phase 2 使用）；`compileSdk = 37.2`，`buildToolsVersion = 37.0.0`（SDK 只装了 37.2 platform，build-tools 仍 37.0.0，勿改成 37.2.0 否则构建失败）；
- 本地 liquid 组件（`ui/component/liquid/*`）与 `FloatingBottomBar` 已迁移到 miuix 新版 API：
  `runtimeShaderEffect`、`BackdropEffectScope.lens`、`innerShadow`、`vibrancy`、
  `miuix-material` 的 `EdgeStroke/EdgeLightSource/EdgePeak/drawEdgeStroke`、
  `isRuntimeShaderSupported`（`top.yukonga.miuix.kmp.material`）、`material.sensor.rememberDeviceTilt`；
  `PagerInterceptionMode.CrossAxisInterceptor` → `CrossAxis`；`pagerGestureOverride` 需传 `flingBehavior`。

### 主题引擎：MaterialKolor 6.0.0（v0.5.291 起）
1. **v0.5.262 曾移除 materialkolor**：当时最新发布是 5.0.1（按 material-color-utilities **5.x** 编译，`DynamicScheme` 在 `com.materialkolor.scheme`），而 miuix 0.9.5 依赖 utilities **6.0.0**（`DynamicScheme` 在 `com.materialkolor.dynamiccolor`），运行时只保留高版本 → 主题页 `NoClassDefFoundError`、R8 missing class。
2. **v0.5.291 已重新接入 MaterialKolor 6.0.0**（正式版，非 beta）：它已适配 utilities 6.0.0，与 miuix 一致，冲突消失。
   - 6.0 拆分了 artifact：`com.materialkolor:material-kolor-core`（PaletteStyle/Contrast 等）与
     `com.materialkolor:material-kolor-material3`（`rememberDynamicColorScheme` → Material3 ColorScheme）；
     **`com.materialkolor:material-kolor` 聚合 artifact 在 6.0 不再发布**，不要按旧 GAV 引入。
   - 项目仍保留本地 `PaletteStyle` / `ColorSpecVersion` 枚举（设置持久化、UI 下拉用，常量名与旧库一致）；
     `ui/theme/DynamicColorSchemeCompat.kt` 负责映射到 MaterialKolor 类型后调用官方 `rememberDynamicColorScheme`。
3. **SPEC 2026 只支持 CMF**（v0.5.292）：MaterialKolor 6.0 的 `PaletteStyle.Cmf` 会强制 SPEC_2026（其它 spec 会被忽略）。
   项目约定：`colorSpec == SPEC_2026` 时**风格强制锁定为 `PaletteStyle.Cmf`**，主题页「色彩风格」下拉置灰不可点并显示说明；
   切回 SPEC_2021/2025 解锁并恢复用户之前保存的风格（持久化的 colorStyle 在锁定期间不变）。
   归一化入口：`Theme.kt` 的 `ColorSpecVersion.effectiveStyle(style)` / `effectiveFor(style)`，
   `ThemeExt.rememberM3TimColorScheme`、`MiuixTheme`、`DynamicColorSchemeCompat`、主题页预览都必须走这两个函数。
   miuix 的 `ThemeColorSpec` 只有 2021/2025，但 `ThemePaletteStyle.Cmf` 在 miuix ThemeController 内部同样强制 SPEC_2026。
4. **红线**：不要降级到 materialkolor 5.x（会重新引入 utilities 5/6 冲突）；也不要恢复手写 DynamicScheme→ColorScheme 映射；
   升级 materialkolor 时必须确认其 `material-color-utilities` 版本与 miuix 依赖一致（当前都是 6.0.0）。

### 首页滚动性能约定与基线（v0.5.284 起）
- **基线数据（M3E 首页 6s 连续滑动，gfxinfo）**：v0.5.260（升级前）janky 1.04% / p90 13ms / Slow UI 21；
  v0.5.284（material3 对齐后）janky 3.37% / p90 15ms / Slow UI 20。若显著高于此值（如 janky >8% 或 p90 >25ms），先查以下红线。
- **material3 版本**：由 BOM 管理，禁止 pin 旧 alpha（见上）。
- **全局 blurBackdrop（MainActivity）只在「Miuix + 标准（非浮动）底栏 + 模糊」时录制**：
  M3E 或 Miuix 浮动底栏不需要它；无条件录制会把整个 pager 每帧录进 GraphicsLayer，是首页滑动卡顿的常见原因。
- **消息推送刷新合并**：`TimBridge.scheduleLiveChatsRefresh` 为 900ms trailing debounce（刷新期间新事件结束后再等完整周期），
  头像下载/`refreshChatsSoon`/`refreshContactsAndChatsSoon` 都走统一调度；刷新频率应 ≤1.2 次/s。
  `LiveChats.update` 新旧列表相等时跳过 StateFlow 推送。
- **UI 侧滑动策略**：`ChatViewModel` 在首页列表滚动中暂存 `LiveChats` 推送（只保留最新），停止滚动后补刷一次；
  `applyChats` 等值短路；`ChatActions.onListScrollStateChange` 由两个首页列表上报 `isScrollInProgress`。
  **不要**恢复"滚动暂停动图/图片加载"（用户明确要求保留）；但 `basicMarquee` 跑马灯在滚动中会暂停（布局/动画开销），停手恢复。
- **验证方法**：分 UI 模式单独测——先切到目标模式、等 3~5s 预热、`adb shell dumpsys gfxinfo com.tencent.tim reset`、
  让用户连续滑动 6s、再 `dumpsys gfxinfo` 读 janky/p90/p95/p99/Slow UI；不要混入切换 UI/页面转场的帧。

### Phase 2（已基本完成）
已把 Miuix 顶栏/底栏/弹窗/Sheet 迁移到 `miuix-glass`：`GlassTopAppBar`（`HyperTopAppBar`）、`GlassNavigationBar`、`GlassPopup`/`GlassTransformPopup`（长按菜单/顶栏菜单，支持预测性返回）、`GlassDialog`/`GlassBottomSheet`、`GlassDropdownPreference`、`GlassSegmentedTabRow`；页面通过 `rememberPageBackdrop`/`LocalPageBackdrop` 采样，`isRuntimeShaderSupported()` 为 false 时自动回退纯色。主题外观页提供 example Layout 的同款设置（显示顶栏/顶栏风格/显示底栏/导航栏模式/悬浮底栏等）。
**未做**：宽屏 `Sidebar Style`（example 的平板 rail 风格）、Miuix 固定头页面（搜索/群文件面包屑等）的玻璃顶栏穿内容效果。

### 应用图标（v0.5.293 起）
- 图标为 **M3 / TIM 两行居中文字**（白色文字 + `#1C1B1F` 深色背景），不是图片：`res/drawable/ic_launcher_foreground.xml`
  （自适应图标前景）、`ic_launcher_monochrome.xml`（Android 13+ 主题图标，`mipmap-anydpi/ic_launcher.xml` 已配 `<monochrome>`）、
  `ic_launcher_splash.xml`（Android 12 启动页图标）。
- 字体为 **Google Sans Flex**（SIL OFL 1.1，Google Fonts），文字轮廓由 `tools/gen_launcher_text_icon.py` 生成（依赖 fonttools）：
  改字号/行距/换字后重跑脚本即可，不要手改 XML 路径；关于页「开源许可」已列出该字体（OFL 1.1）。
- 颜色：`values/colors.xml` 的 `ic_launcher_background` / `ic_splash_background` 为深色；改背景色时同步保证白色文字对比度，
  关于页 icon 预览底也使用同色（`Color(0xFF1C1B1F)`）。

### 宿主 Context 主题与平台组件（v0.5.295 起，重要）
- 宿主 ComposeView 使用 `ModuleContext`（Resources 指向模块 APK）；**`getTheme()` 不能返回 `moduleResources.newTheme()` 空主题**：
  平台组件会从 `view.context.theme` 读取属性，空主题里 `magnifierWidth/Height/Zoom` 全为 0，
  长按文本输入框时 `android.widget.Magnifier.performPixelCopy` 会 `Bitmap.createBitmap(0,0)` 崩溃
  （`IllegalArgumentException: width and height must be > 0`）。
- 当前实现：`ModuleContext.getTheme()` 在模块主题上 `applyStyle(android.R.style.Theme_DeviceDefault, true)`，
  保证系统文本选区放大镜、文本选择工具栏等平台组件有正常默认值；Compose 的 Material/Miuix 主题仍在 Compose 层单独提供。
- 以后若新增宿主侧平台组件（PopupWindow/Dialog/TextToolbar 等），同样用这个主题，不要另建空主题。

## 6. 桥接开发要点

- **反编译优先**：TIM 4.1 大量混淆名会变（JADX 合成字段名运行时不存在，如 `c.a.f118264c` 就踩过坑）。**先用反编译确认真实类/方法/字段名，再用反射**；优先公开方法，避免私有混淆字段。
- **产品级禁用**：kernel 命令返回 `-10122 Product does not have permission` 时，改走 TIM 自己的 QRoute/旧版通道（历史上管理员、踢人、支持/拒绝好友申请、群简介、语音发送、贴纸发送都这样解决）。
- **回调**：kernel 回调接口用 `Proxy` + `awaitCallback`（支持宿主 ClassLoader）；主线程阻塞操作严禁在 UI 线程执行。
- **映射完整性**：新元素类型（如 MarketFace）要同时考虑单元素 `describeElements` 与混排 `describeElementsWithParts` 两条路径，并确保文本合并循环不会吞掉它。
- **失败安全**：所有桥方法失败返回 false/null、打日志，不抛到 UI；独立 App 默认实现必须安全。
- **性能**：首屏消息立即加载；非首屏（@成员预取、撤回权限、群资料刷新）延后到进入转场后（约 400ms）。重 IO 用缓存 + LiveEvents 增量刷新。

## 7. 设备与验证

- 测试机：一台已 root / 装了 LSPosed 的 Android 设备（Android 14+，adb 连接；具体序列号不写进文档）。
- 用户授权范围：**安装 + force-stop + 启动 App**；**不模拟点击/手势**，功能验证由用户点或用户授权后截图/日志取证。
- 拉截图：`adb -s <serial> exec-out screencap -p > x.png` 或从 `/sdcard/Pictures/Screenshots/` pull 最新图。
- 拉日志：`adb -s <serial> logcat -d | grep -aE 'Material3Tim|ime:|emoji:|perf:'`。
- 常见随机问题先怀疑：宿主窗口焦点/IME、NavDisplay 组合重建、缓存竞争、TIM 产品权限。

## 8. 提交纪律

- 许可证：项目整体 **GPL-3.0**（`LICENSE`）；主题设置页移植自 KernelSU ColorPalette（GPL-3.0），新增代码需与该许可证兼容。
- 一个功能/修复一个 commit，message 用中文概述 + 关键点（默认不 bump 版本）。
- 提交前：`./gradlew :app:assembleRelease` 通过、`git diff --check` 干净、无新增 warning 更好。
- 同步打 annotated tag、拷贝根目录 APK，并向用户汇报：版本、提交/tag/APK、验证点。
- 不要提交临时日志/调试文件；调试日志改为正式诊断日志时保留必要的前缀。
