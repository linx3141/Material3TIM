# Material3 TIM

聊天表情面板底部可直接切换 TIM 和 Telegram 表情包。首次登录与构建配置见 [Telegram 表情包同步](docs/telegram-stickers.md)。

用自写的 Jetpack Compose 界面完整替换 **TIM（QQ NT 架构）** 官方 UI 的 Xposed 模块。同一个 APK 同时是**独立 App**（可直接安装预览 UI）和 **LSPosed 模块**（在 TIM 进程内接管界面），分阶段开发中。

> **当前状态（v0.1.0 / versionCode 1，首个公开版本）**
>
> - **第 1 步 ✅**：独立 Compose UI APK —— 全部页面 UI 已完成（示例数据）。
> - **第 2 步 ✅**：同一个 APK 作为 LSPosed 模块在 TIM 进程内接管界面（测试机 + TIM 4.1.0.4050 真机验证通过）。
> - **第 3 步 🔄 进行中**：真实 TIM 数据读写大部分打通（会话 / 消息收发 / 联系人 / 资料 / 聊天信息 / 群公告…），v0.5.77 已接入**实时消息推送**、v0.5.78 补全**单条转发与 +1（复读）**、v0.5.79 修复**打开聊天时首帧闪到列表顶部**、v0.5.80 修复**多选点击拦截与整行选择**、v0.5.81 修复**Miuix 长按菜单 / M3E 表情面板的沉浸式系统栏**、v0.5.82 适配**表情面板「收藏」页（用户自己收藏的表情图片）并移除「最近」**、v0.5.83 调整**收藏表情排序（最新添加的排最前）**、v0.5.84 支持**点击收藏表情直接发送**、v0.5.85 接入**聊天信息页置顶/消息免打扰**并实现**查找聊天记录**、v0.5.86 实现**消息页右上角加号菜单**（创建群聊 / 加好友群 / 扫一扫，M3E 与 Miuix 各自原生弹出菜单）、v0.5.87 接入**加号菜单三项真实功能**（创建群聊、QQ号/手机号/群号搜索加好友/群、扫一扫），v0.5.88 修复**建群成员丢失**与**非好友/非成员搜索不到**，v0.5.89 支持**纯昵称搜群**与**手机号搜索**；语音播放、文件打开、红包领取已接入，系统通知栏消息与已读回执尚未完成。
> - **NPatch 嵌入模式增强（2026-11）**：主题页新增「跟随系统取色」开关（默认开）；「我的」与「他人资料」页新增 QQ 空间入口；扫一扫改为**应用内 CameraX 预览 + 扫码框 + ZXing 实时识别**（不再拉起系统相机，保留相册图片兜底）；联系人页 / 新的朋友页的「加好友」入口接通搜索与验证流程；设置页新增「回到原版 TIM」；TIM 原生设置页新增「Material3 TIM」入口，用于关闭覆盖层后重新启用。
>
> **数据口径（重要）**：
>
> - **独立 App**：使用内置示例数据，不装 TIM 也能预览完整 UI；
> - **宿主（TIM 进程）**：使用真实 TIM 数据（会话、消息、联系人、本人资料、聊天信息、群公告等），并支持发送文本 / 图片 / 文件、引用、删除、转发与 +1（复读）；
> - **子页面**（我的收藏 / 我的文件 / 钱包 / 服务 / 新的朋友 / 标签 / 公众号 / 表情包）在两种形态下**仍为示例数据**。
>
> 界面参考 [KernelSU Manager](https://github.com/tiann/KernelSU) 的 UI 与交互实现（Material 3 Expressive / Miuix 双风格、莫奈取色引擎、导航栏系统）。

---

## 链接

- GitHub 仓库：<https://github.com/linx3141/Material3Tim>
- Telegram 频道：<https://t.me/Linx3141Channel>

## 分步计划

| 步骤 | 内容 | 状态 |
|---|---|---|
| 1 | 制作自写 UI（Compose），独立 APK 运行 | ✅ 已完成（示例数据） |
| 2 | 制作成 Xposed 模块，替换 TIM 的 UI | ✅ 已完成（真机验证通过） |
| 3 | 保证自写 UI 能正常调用 TIM 的功能 | 🔄 进行中（真实数据大部分打通，v0.5.77 实时推送、v0.5.78 转发与 +1、v0.5.79 打开聊天闪帧修复、v0.5.80 多选点击修复、v0.5.81 系统栏沉浸修复、v0.5.82 收藏表情页、v0.5.83 收藏表情倒序、v0.5.84 收藏表情发送、v0.5.85 会话置顶/免打扰/查找聊天记录、v0.5.86 消息页加号菜单、v0.5.87 创建群聊/加好友群/扫一扫、v0.5.88 建群成员与陌生人搜索修复、v0.5.89 昵称搜群/手机号搜索已接入；语音/文件/红包已接入，通知栏消息/已读回执等未完成） |

## v0.1.0 — 首个公开版本

- **版本号重置**：公开前内部开发版本号是 0.5.295 / versionCode 318；对外首个版本改为 **0.1.0 / versionCode 1**（后续非用户要求不再自动 bump）。
- 关于页新增 **Telegram 频道**入口（GitHub 下方）；README 增加 GitHub / Telegram 链接。
- 包含内部开发期间的全部功能：双 UI（Material 3 Expressive / Miuix）、LSPosed 宿主接管、真实 TIM 数据、HyperOS4 玻璃组件、MaterialKolor 6.0 主题引擎（SPEC 2021/2025/2026 + CMF）、新文字图标等。
- 修复宿主长按输入框文字选择崩溃（系统 Magnifier），README/AGENTS 脱敏并移除所有真实聊天截图，许可证为 GPL-3.0。

## v0.5.295 更新（内部开发版，历史）

1. **修复长按输入框文字崩溃**：宿主 `ModuleContext` 之前给 ComposeView 返回的是模块 APK 的空主题，系统文本选择放大镜（`android.widget.Magnifier`）读到的 `magnifierWidth/Height/Zoom` 全为 0，`performPixelCopy` 里 `Bitmap.createBitmap(0,0)` 抛 `width and height must be > 0` 直接崩溃（日志确认）。现已给宿主 Context 主题叠加系统 `Theme.DeviceDefault` 默认属性，长按输入框恢复正常系统文字选择/放大镜。
2. **仓库文档整理**：README 更新技术栈/版本/当前未完成事项；新增 GPL-3.0 `LICENSE`（主题设置页移植自 KernelSU ColorPalette，KernelSU Manager 为 GPL-3.0，项目整体采用 GPL-3.0）。


1. **关于页开源许可按项目区分**：KernelSU Manager（GPL-3.0）、Miuix（Apache-2.0）、material-kolor（MIT）、Google Sans Flex（SIL OFL 1.1）。
2. **重新接入 MaterialKolor 6.0.0**：6.0 已适配 material-color-utilities 6.0.0，与 Miuix 0.9.5 一致，不再有 5.0.1 的 scheme 包冲突；`DynamicColorSchemeCompat` 改为调用官方 `rememberDynamicColorScheme`。
3. **SPEC 2026 / CMF**：色彩标准选 SPEC 2026 时色彩风格锁定为 CMF 并置灰，切回 2021/2025 恢复原风格；主题引擎统一走 `effectiveStyle`/`effectiveFor` 归一化。
4. **新应用图标**：M3 / TIM 两行居中文字（Google Sans Flex 轮廓生成的矢量自适应图标），支持 Android 13+ 主题图标；文字已缩进安全区避免被启动器圆角裁切。
5. **Miuix 顶栏/布局收尾**：顶栏按钮跟随 Glass/Original 风格；Glass 顶栏内容可滚到栏下真正采样；主题外观新增 example Layout 分组（显示顶栏/顶栏风格/显示底栏/导航栏模式/悬浮底栏等）；Original 风格下加号菜单回退普通下拉。
6. **修复升级后首页滑动卡顿**：根因是 Compose 1.13 下 material3 仍 pin 旧 alpha；改为 BOM 管理后首页滚动 janky 从 ~13% 回到 ~3.4%。
7. **其它**：设置页分组卡片边距、Miuix HyperOS4 玻璃组件全量覆盖等。

## v0.5.294 更新

1. **Miuix 全量升级到 HyperOS 4 风格**（仅 Miuix；M3E 视觉保持原样）：
   - 全页面 `HyperTopAppBar`（玻璃/纯色自适应）+ 页面级 backdrop + squircle 分段卡片；
   - popup 菜单迁移到 miuix-glass `GlassPopup`/`GlassTransformPopup`：消息气泡/会话卡片/群成员/收藏表情/附件相机，支持按钮形状 morph 与预测性返回，无全屏压暗，长按锚点=手指位置；
   - 顶栏圆形 Glass 按钮 + `GlassMenuHost` 页面级返回优先级；对话框/Sheet 换成 `GlassDialog`/`GlassBottomSheet`；表情/附件 Tab、设置/主题下拉玻璃化；
   - 主题外观页「底栏」分组对齐 miuix example：导航栏模式（图标+文本/仅图标/选中显示文本）、悬浮底栏风格（Original/Glass/iOS-like）、Original 位置（居中/靠左/靠右），并真实驱动底栏渲染；
   - 首页恢复为每条会话独立卡片（不采用分段卡片）。
2. **修复升级后首页滑动卡顿**：根因是升级 Compose 1.13 后 material3 仍 pin 了旧 `1.5.0-alpha28`，组件走兼容路径；
   改为 Compose BOM 管理 material3（1.5.0）后 M3E 首页滑动 janky 从 ~13% 恢复到 ~3.4%（p90 15ms）。
   另：消息推送刷新 900ms 合并、滚动中延迟应用列表更新、滚动中暂停跑马灯、全局 blurBackdrop 按需录制。
3. **修复系列崩溃**：materialkolor 与 miuix utilities 5/6 冲突（设置/主题页）、玻璃 morph 负圆角、主题页 TabRow 自采样 RenderNode 栈溢出等。
4. **版本号**：升至 **0.5.284 / versionCode 307**。

## v0.5.262 更新

1. **Miuix 升级到 0.9.5-beta03**（Maven Central 最新），同步升级 Kotlin 2.4.21 / Compose BOM 2026.11.01（Compose 1.13）/ compileSdk 37.2，并新增 `miuix-glass`、`miuix-glass-icons` 依赖：
   - 本地 liquid 玻璃组件（`Lens` / `CombinedBackdrop` / `InnerShadow` / `Vibrancy`）迁移到新版上游实现（`runtimeShaderEffect` / `BackdropEffectScope.lens` / `innerShadow` / `vibrancy`）；
   - 浮动底栏改用新版 `miuix-material` 的 `EdgeStroke` / `EdgeLightSource` / `drawEdgeStroke`（替换已移除的 `blur.highlight.*` / `blur.sensor.*`），保留原有拖拽/角标/设置交互；
   - `PagerInterceptionMode.CrossAxis`、`pagerGestureOverride(flingBehavior)`、`Slider` 状态 API 等适配。
2. **修复升级后进入设置/主题页崩溃**：miuix 0.9.5 依赖 `material-color-utilities 6.0.0`，而旧 `materialkolor 5.0.1` 编译于 utilities 5.x（`DynamicScheme` 包名在 6.x 中变更），运行到主题页时 `NoClassDefFoundError`，R8 也报 Missing class。
   - 移除 `materialkolor` 依赖，新增本地 `PaletteStyle` / `ColorSpecVersion` 枚举（常量名保持兼容，已保存主题设置不受影响）；
   - 新增 `DynamicColorSchemeCompat`：直接用 `material-color-utilities 6` 公开 API 生成 Material3 `ColorScheme`，主题入口与调色板预览统一使用；
   - 项目显式依赖 `material-color-utilities:6.0.0`，彻底消除 5/6 冲突。
3. **版本号**：升至 **0.5.262 / versionCode 285**。

## v0.5.138 更新

1. **底部 Sheet 拖拽条统一为 material3 官方 `BottomSheetDefaults.DragHandle()`**：@ 菜单与群成员操作菜单去掉此前自绘的 36×4dp 药丸，改为官方组件（32×4dp、官方间距/颜色），外观与 Compose Catalog 一致。
   - 仍保留 `containerColor = Transparent` + 自绘 Surface（预测性返回整块跟手位移所必需，属于必要自绘）；
   - 成员操作菜单保留内容自适应高度、顶部 28dp 圆角与左右 12dp 内容边距；按需求移除「XXX 的操作」标题行，直接显示操作列表。
2. **版本号**：升至 **0.5.138 / versionCode 161**。

## v0.5.137 更新

1. **成员操作菜单（M3E）细节规范化**（接 v0.5.136 预测性返回）：
   - 补回**顶部拖拽药丸条**（与 @ 面板同款自绘 36×4dp 圆角条）；
   - SegmentedList 操作行增加左右 12dp 内边距（标题同步对齐），不再贴 sheet 边缘；
   - sheet 保持内容自适应高度 + 顶部 28dp 圆角 + `surfaceContainer` 背景 + `navigationBarsPadding`。
2. **版本号**：升至 **0.5.137 / versionCode 160**。

## v0.5.136 更新

1. **群成员操作菜单（M3E）接入预测性返回手势**：`ChatMembersMaterial` 的成员操作 `ModalBottomSheet` 之前是标准 material3 实现，挂到 Dialog 窗口后默认拿不到跟手返回动画，返回键还会被当成页面返回。现在复用 @ 菜单同款方案：`DialogBackDispatchEffect`（API34+ `OnBackAnimationCallback` 跟手 → 内容根 `translationY`，取消回弹、完成后 hide+dismiss；API33 普通返回回调兜底），Dismiss 行为与 Miuix `OverlayBottomSheet` 一致。
2. **版本号**：升至 **0.5.136 / versionCode 159**。

## v0.5.135 更新

1. **系统通知 / 后台提醒**（宿主模式）：
   - `IKernelMsgListener` 新消息 → 单线程队列汇总 → Android 通知栏提醒（channel `m3tim_messages`，IMPORTANCE_HIGH，icon 用系统自带的聊天图标）；
   - 跳过条件：正在查看该会话、自己发的消息、免打扰会话（最近会话 `isMuted`，30s 缓存）、灰条/系统提示、重复 msgId；
   - 同会话 1 秒内多条合并为「N 条新消息（最近：…）」；通知按会话分组，同一会话复用同一个通知 id；
   - 点击通知 → `PendingIntent` 打开 TIM 并带 `m3tim_chat_id` extra → 模块 `TimUiHook`（onNewIntent/onResume）→ `NavigationRequests` → `M3TimRoot` push `Route.ChatDetail`，直达对应聊天（真机已验证：用户收到通知点击后正确进入会话）。
   - Android 13+ 若 TIM 无通知权限会写日志 `notify: notifications disabled`（不回退崩溃）。
2. **群管理操作**（聊天信息 → 成员/群资料，M3E + Miuix 两套 UI）：
   - 群资料页：群主/管理员可点群名重命名、切换「全员禁言」、群主可「解散群聊」、非群主「退出群聊」；
   - 群成员页：点其他成员弹出操作菜单——禁言 10 分钟/1 小时/1 天、解除禁言、设为/取消管理员（仅群主）、修改群名片、移出群聊，含权限约束与确认弹窗；
   - 群公告页：群主/管理员右上「发布公告」（纯文本）；
   - 桥接走 NT 内核 `IKernelGroupService`（native 优先、wrapper 回退），回调 code==0 判成功；
   - 真机已验证：**群名修改、群名片修改、全员禁言开/关、解除禁言** 成功（code=0）；
   - 已知问题（下一轮修）：**设置/取消管理员** 内核返回 `-10122 Product does not have permission`（TIM 产品未放行该 cmd，需改走 TIM 自身旧版通道）；**禁言成员** 返回 `120101203`（待确认参数/通道语义）；**踢人/退群/解散/发布公告** 已接线但未在测试群实测（破坏性操作）。
3. **群资料页冷启动修复**：`loadChatInfo` 增加最多 4 次重试（间隔 800ms，群聊要求成员列表非空），加载中显示 loading 而不是回退示例数据；修复「刚进群资料页显示示例成员、管理入口不出现」；成员数显示改用真实成员列表数量。
4. **版本号**：升至 **0.5.135 / versionCode 158**。

## v0.5.134 更新

1. **群文件修复与优化**：
   - **Toast 文案修复**：群文件页面状态对象之前用了宿主 applicationContext，模块资源 ID 被解析成宿主字符串（表现为所有操作成功/失败 Toast 都显示 `false`）；改用模块资源上下文后，「操作成功/上传成功/下载完成」等文案正常；
   - **操作后自动刷新**：新建/重命名/移动/删除/上传成功后立即刷新，并延迟 1.2s 再补刷一次，避免服务端短暂不一致导致列表不变；
   - **进入页面自动加载**：先发列表请求再异步查管理权限（权限查询慢不再导致首屏空白），进入即显示内容；
   - **M3E 列表改为分段式（Segmented List）**：文件夹与文件分成两个独立分段列表，标题分别为「文件夹」「文件」；管理模式下分段项带复选框，操作栏与原有功能一致。
2. **版本号**：升至 **0.5.134 / versionCode 157**。

## v0.5.133 更新

1. **群文件页面**（聊天信息 → 群文件）：新增 `GroupFilesScreen` + M3E/Miuix 两套 UI。
   - **列表/目录**：群文件根目录同时显示文件夹与文件（修复此前只显示文件夹的问题）；支持进入子目录、面包屑/返回上级、分页加载、刷新、空态/错误重试；文件按上传时间倒序、文件夹优先；
   - **下载/打开**：文件行显示「下载」按钮，下载进度实时更新（OIDB 0x6d6_2 取 UFT 地址 + HTTP 下载），完成后变「打开」；已下载文件重启后通过本地缓存（`cache/group_files/`，按文件名/大小匹配）回填，不会又显示成「下载」；
   - **上传**：管理模式下可「上传」到当前目录（系统文件选择器 → 复制到 cache → 群文件库 UFT 上传：0x6d6_0 申请上传 + 二进制分片 HTTP 上传），上传成功后列表刷新可见；单文件上限 200MB（流式分片，内存安全）；
   - **管理**（群主/管理员）：管理模式下可多选，支持**新建文件夹、重命名（文件/文件夹）、移动（文件）、删除（文件/文件夹）**；权限通过群成员 `MemberRole` 判定（`ADMIN` 实测可用）；
   - **协议说明**：TIM 4.1.0 的 NT kernel 群文件 API 未注册（`api_fun Not Found`），因此改走旧版 OIDB 协议（0x6d8_1 列表 / 0x6d6_2 下载 / 0x6d6_0 上传 / 0x6d6_3/4/5 与 0x6d7_0/1/2 管理）；列表请求按 TIM 源码默认 `filterCode=3` 优先、失败自动回退组合矩阵。
2. **红包卡片配色**改为 `primary + onPrimary`（上一版为暖色渐变；图标/标题/副标题/状态文字统一 onPrimary）。
3. **版本号**：升至 **0.5.133 / versionCode 156**。

## v0.5.132 更新

1. **红包消息适配**（之前只显示「[消息]」）：新增 `ChatMessageType.RED_PACKET` 解析（elementType=9 `WalletElement`，取 `receiver.title/subTitle` 作标题/副标题、billNo/authkey/channel/senderUin/grabState），M3E/Miuix 都换成暖色红包卡片；
2. **红包状态与查看详情**：通过 TIM 本地钱包接口 `INewQWalletApi.getHbStateAsync(listId)` 异步取状态文案（已领取/已领完…），卡片右侧显示「领取红包 / 已领取」；已领取的卡片点击打开 TIM 原生红包详情页（`redgiftDetail` PayBridge，展示金额/领取记录/已存入余额）；
3. **原生领取**：未领取卡片点击直接调 TIM 自己的 `GrabHbViewModel.i0`（内部完成 pskey + `hb_pre_grap` 预领取），弹出和原生 AIO 一致的「開」红包弹窗；真机实测（实名账号）点開后领取 0.01 元成功，TIM 本地状态变为「已领取」；
4. **已知限制**：领取依赖 TIM 自带 QQ 钱包；未实名/未成年校验的账号会触发 TIM 的 `qwallet_minorreg_query.cgi`，老版本 TIM 被服务端提示「当前QQ版本过低」而无法完成领取（与卡片 UI 无关）；
5. **版本号**：升至 **0.5.132 / versionCode 155**。

## v0.5.131 更新

1. **ARK 卡片点击与聊天气泡链接统一走 `ExternalLink`**：之前小程序卡片直接 ACTION_VIEW，会触发 TIM 原生的「即将离开 TIM，打开其他应用」确认弹窗。
   - 现在复用文字气泡链接打开的同一套逻辑：非 QQ/腾讯域名用 **Chrome Custom Tabs 应用内打开**（多任务里仍显示 TIM，无“离开 TIM”弹窗）；QQ/腾讯自家域名走 TIM 内置 `QQBrowserActivity`；
   - 并通过 `UiHost.expectExternalLaunch()` 让模块的外跳 hook 直接放行，避免 TIM 的离开确认；
   - 实测 B 站小程序卡片点击直接在当前任务内打开 B 站视频页，不再弹确认框。
2. **版本号**：升至 **0.5.131 / versionCode 154**。

## v0.5.130 更新

1. **ARK 卡片消息适配**（之前只显示「[消息]」）：新增 `ChatMessageType.ARK` 与字段（app/source/title/desc/tag/preview/jumpUrl/prompt）。
   - 解析 `elementType=10` 的 `ArkElement.bytesData` JSON：兼容 meta 为对象/数组/JSON 字符串；递归取卡片字段（title/desc/preview/jump 等，大小写不敏感）；`linkInfo`、prompt 作兜底；
   - 针对**小程序卡片**（`com.tencent.miniapp_01`）做字段换位：`meta.title` 是品牌名（如「哔哩哔哩」）、`meta.desc` 是内容标题，卡片正确显示「来源=哔哩哔哩 / 标题=内容」；
   - 预览图：HttpURLConnection 后台下载（手机 UA、4s 超时、3MB 上限）缓存到 `cacheDir/m3tim_ark`，聊天页轮询自动回填；
   - 点击：http(s) 链接直接 ACTION_VIEW；其他 schema 先尝试 TIM `JumpActivity`/JumpAction；
   - UI：M3E/Miuix 都新增 ARK 卡片气泡（预览图 + 来源 + 标题 + 描述 + tag + 页脚），字段全空时回退文本气泡；长按菜单/多选沿用原有逻辑。
2. **真机验证**：用户发的 B 站小程序卡片（`com.tencent.miniapp_01`）显示预览图/哔哩哔哩/视频标题/「QQ小程序」页脚；点击打开 B 站视频页。
3. **版本号**：升至 **0.5.130 / versionCode 153**。

## v0.5.129 更新

1. **相册支持图片+视频混合多选**：附件面板「照片」改名「相册」（图标不变）；系统 Photo Picker 由 `ImageOnly` 改为 `ImageAndVideo`，仍保持最多 9 项多选；按 MIME 分流（图片走原链路，视频走新视频链路）。
2. **接收视频缩略图修复（不再纯黑）**：
   - 根因：接收消息 `VideoElement.filePath/thumbPath` 常为空，TIM 用 `assembleMobileQQRichMediaFilePath` 拼真实缓存路径；缩略图请求参数应为 `downloadType=2/downSourceType=1/triggerType=1`（旧实现用了视频本体参数，缩略图一直没下载）。
   - 修复：真实路径组装 + TIM `shortvideo` 目录扫描（按 md5/fileName/fileSize 匹配）+ 视频首帧截取兜底 + RichMedia 下载完成后按 elementId 回填；视频气泡现在显示真实封面、时长，可点击本地播放。
3. **发送视频（真正视频消息，而非文件）**：
   - 选中视频后复制到缓存，`MediaMetadataRetriever` 读取时长/宽高（含旋转）+ 抽首帧，插入乐观 VIDEO 气泡，再走桥接发送；
   - 桥接改用 TIM 原生 `IMsgSendHelper.sendVideo`（QRoute 4 参 `createVideoElement` 内部依赖 `IGuildVideoApi`，TIM 包缺失必然抛异常，已弃用；失败才回退 1 参元素）；
   - `sentVideoMeta`（按文件大小）记录本地视频/缩略图/时长，reload 成服务端消息后仍能显示缩略图、时长并本地播放；
   - 真机验证：发送 1.9MB h264(720x1280/6s) 视频到群，电脑端 Linux QQ 已可见，60s+ 后消息仍持久存在，气泡缩略图/0:06 正常，点击可播放。
4. **异常文件处理**：损坏/不可解码的旧缓存视频（如 651288B TIM 缓存残片）探测失败时会 Toast 跳过，不会发出黑视频、不崩溃。
5. **版本号**：升至 **0.5.129 / versionCode 152**。

## v0.5.128 更新

1. **聊天页恢复横滑返回（预测性返回）手势**：聊天页此前因消息行「左滑引用」手势会消费所有水平拖动，导致 `NavDisplay` 的 `navSwipeDismiss` 无法接管右滑返回。
   - `SwipeToReplyRow` 改为自定义手势循环：只有**左滑**（dx < 0 且超过 touch slop）才消费事件并驱动引用；**右滑完全不消费**，交给 NavDisplay 的 predictive back 手势接管，页面跟手右移/可取消/松手回列表；
   - 手势方向在过 slop 时锁定：右滑开始返回后，后续即使手指向左回拉也只拖动返回页面，**不会触发消息左滑引用的穿透行为**；
   - 左滑引用行为保持不变（仍可左滑消息弹出引用栏）。
   - 真机（Miuix）验证：聊天页右滑返回有跟手转场动画并回到消息列表；左滑消息仍正常弹出「回复 …」引用栏。
2. **版本号**：升至 **0.5.128 / versionCode 151**。

## v0.5.127 更新

1. **输入框按“行数”固定高度，不随中英文字形变化**：v0.5.126 统一了占位符/文本行高，但中文字形与英文字形的实际测量高度仍有差异（输入汉字高度不变、输入英文会矮一点）。
   - 通过 `BasicTextField.onTextLayout` 取当前布局行数 `lineCount`，输入框高度按行数固定：
     - Miuix：`height = 42.dp + (lineCount-1) * 20.dp`（1~4 行分别固定）
     - M3E：`height = 44.dp + (lineCount-1) * 24.dp`
   - 单行输入（空态/中文/英文）高度完全一致；2/3/4 行各自固定，不再由文字内容实际字形决定；
   - 用户真机验证：中英文输入前后、空态与有文本状态下输入框高度均不再变化。
2. **版本号**：升至 **0.5.127 / versionCode 150**。

## v0.5.126 更新

1. **聊天输入栏高度/宽度不再随文本变化**（M3E + Miuix 共有问题）：
   - 原布局：空态时麦克风按钮在输入框内部，输入文本后麦克风消失、右侧出现发送按钮，导致输入框变窄；同时占位符与输入文本的行高不一致，输入框高度有 1~3px 抖动。
   - 修复：把麦克风/发送按钮都移到输入框右侧的**固定占位槽**（空态显示麦克风、有文本显示发送），输入框宽度恒定；
   - 输入框加 `heightIn(min = 40.dp/44.dp)`，并给 Miuix 文本框/占位符显式统一 `lineHeight = 20.sp`，空态与有文本时高度一致（多行文本仍会正常增高）。
   - 真机实测对比：空态/有文本两种状态下输入框宽度一致、高度一致。
2. **版本号**：升至 **0.5.126 / versionCode 149**。

## v0.5.125 更新

1. **M3E @ 菜单搜索栏改为消息列表顶部同款胶囊搜索框**：原 material3 `SearchBar`（contained state）在 Sheet Dialog 里点击会尝试展开全屏搜索，但 Dialog 内没有配套的 `ExpandedFullScreenContainedSearchBar`，导致点击无反应、输入法都弹不出来。
   - 改为与主页消息列表一致的胶囊搜索框（`Surface` + `BasicTextField`，`CircleShape`、56dp 高、`surfaceContainerHighest` 背景、搜索图标 + 提示文字）；
   - 点击即聚焦并弹出输入法，输入实时过滤成员列表，输入非空时显示清除按钮；
   - Miuix 的搜索栏保持原样（其 InputField 本就正常）。
2. **版本号**：升至 **0.5.125 / versionCode 148**。

## v0.5.124 更新

1. **移除 @ 菜单顶部标题行**：M3E 与 Miuix 的 Sheet 都不再显示「选择提醒的人 ×」标题/关闭按钮，直接从拖拽条 + 搜索框开始；关闭仍可用遮罩点击、返回手势/返回键或底部「取消」。
2. **Miuix 搜索成员框背景对齐卡片**：`InputField` 显式传 `color = colorScheme.secondaryContainer`（与下方成员 Card 同色），放大镜图标改用 `onSecondaryContainer`；搜索框与列表卡片形成统一的层级背景。
3. **版本号**：升至 **0.5.124 / versionCode 147**（M3E 仅做编译验证，按要求未在真机测试）。

## v0.5.123 更新

1. **Miuix @ 菜单卡片配色对齐官方示例**：此前 Miuix BottomSheet 内的卡片默认色与 Sheet 背景同为 `surfaceContainer`，视觉上糊在一起。
   - 参考 Miuix 官方 example（`example/.../component/BottomSheetSection.kt`）中 BottomSheet 内容 Card 的写法，改为 `CardDefaults.defaultColors(color = colorScheme.secondaryContainer)`（只覆盖卡片底色，contentColor 与文字颜色沿用 Miuix 默认，与官方示例一致）；
   - 标题/徽标文字保持原有 `onSurface` / `onSurfaceVariantSummary`，与 Miuix 默认内容色一致；
   - dark/AMOLED 下实测卡片 `#434343`、Sheet 背景 `#242424`，层级清晰。
2. **版本号**：升至 **0.5.123 / versionCode 146**。

## v0.5.122 更新

1. **缩短“键盘消失 → @ Sheet 弹出”的间隔**：v0.5.121 在 IME 完全收起后又额外 `delay(180)` 等窗口 inset 稳定，导致中间空白感明显。
   - 现在改为 **`WindowInsets.isImeVisible` 变为 false 的下一帧立即挂载 Sheet Dialog**（仍保留最多 400ms 兜底，防止 IME 异常时打不开）；
   - 既保留“等 IME 收起再开 Dialog”的抗闪帧效果，又把键盘收起后的空白时间压到最小（录屏逐帧：键盘消失后 1~2 帧内 Sheet 开始滑入，无双重残影）；
   - 仅 M3E 分支生效，Miuix overlay 逻辑未动。
2. **版本号**：升至 **0.5.122 / versionCode 145**。

## v0.5.121 更新

1. **M3E 输入 `@` 键盘快速闪动修复**：v0.5.120 只是提前收起键盘，但标准 `ModalBottomSheet` 是 Dialog，打开瞬间仍会与 IME 收起动画/窗口 inset 变化叠加，出现 1~2 帧键盘/界面闪动。
   - 现在先等 IME **完全收起**（`WindowInsets.isImeVisible` 变为 false，最多 400ms 兜底），再额外延迟 180ms 让窗口 inset 动画稳定，然后才挂载 Dialog；
   - 只在 M3E 分支生效（`ChatDetailMaterial`），Miuix 的内嵌 overlay 保持原来的即时打开行为；
   - 录屏逐帧确认：键盘收起 → 短暂稳定 → Sheet 单次滑入，不再出现双重 Sheet 残影/闪帧。
2. **版本号**：升至 **0.5.121 / versionCode 144**。

## v0.5.120 更新

1. **输入 `@` 唤起成员面板时自动收起键盘**：在输入框 `onValueChange` 里检测到本次输入触发了 @ 面板时，先调用 `keyboardController.hide()` 收起 IME 再打开面板。
   - **M3E**：避免标准 `ModalBottomSheet`（Dialog）抢焦点时 IME 收起/复位造成的键盘闪动；
   - **Miuix**：修复 OverlayBottomSheet 打开后输入法键盘/焦点仍停留在原输入框、不自动关闭的问题；
   - 只隐藏键盘、**不清除输入框焦点**：避免 IME composing 状态被打断导致已输入的 `@` 文本丢失；关闭面板后输入内容保持不变。
2. **真机验证**：M3E 输入 @ 时键盘只收起一次、面板正常弹出（录屏确认）；Miuix 输入 @ 后键盘关闭、面板弹出且输入内容保留；两套 UI 均无崩溃。
3. **版本号**：升至 **0.5.120 / versionCode 143**。

## v0.5.119 更新

1. **M3E @ Sheet 回归标准组件**：`ChatAtSheetMaterial` 恢复为标准 material3 `ModalBottomSheet`（scrim、大屏限宽、insets、拖拽关闭由组件负责），不再自绘整体 Sheet；内容仍为联系人页同款 SearchBar + A/B/C 分组 `SegmentedList`（无 checkbox，选中=圆角+背景色）。
2. **标准 Sheet 也能预测性返回**：在 Sheet 内容（Dialog 窗口 composition）里用 `LocalView.findOnBackInvokedDispatcher()` 注册 `OnBackAnimationCallback`（API 34+）：`onBackProgressed` → `Animatable` 驱动内容根 `translationY` 跟手下滑，`onBackCancelled` → 回弹，`onBackInvoked` → `sheetState.hide()` + dismiss；API 33 用普通 `OnBackInvokedCallback` 兜底；`postDelayed` 重试处理 Dialog 首帧未 attach，注册失败时静默降级。
   > 实现细节与排查要点见 [docs/at-sheet-predictive-back.md](docs/at-sheet-predictive-back.md)。
3. **背景延伸导航栏（沉浸化）**：`contentWindowInsets = { WindowInsets(0,0,0,0) }` 让 Sheet 背景一直画到屏幕底部/导航栏后面，底部取消/确定按钮用 `atSheetBottomInset()` 避开手势条，内容不被安全区遮挡。
4. **修复 @ 面板无法重新唤起**：输入框已有 `@` 时（例如关闭 Sheet 后残留）再输入新的 `@`，现在会重新打开成员面板，不再需要删光重输；全角 `＠` 同样处理。
5. **真机验证**：返回手势跟手下滑并关闭（录屏帧确认）、返回键可关闭；Sheet 背景延伸至导航栏、按钮在安全区上方；`@@` 二次输入可重新唤起面板。
6. **版本号**：升至 **0.5.119 / versionCode 142**。

## v0.5.118 更新

1. **@ Sheet 完全对齐联系人页样式**：搜索框改为联系人页同款（M3E expressive `SearchBar` + `SearchBarDefaults.InputField`；Miuix `InputField`，与 `SearchBarFake` 一致）；列表改为联系人页组件（M3E `SegmentedItem + SegmentedListItem`、Miuix `Card` 行），保留 A/B/C 首字母分组头与行间距；「全体成员」同样按列表项呈现。
2. **多选选中样式**：M3E 按用户偏好**不使用 checkbox**——选中态由 `SegmentedListItem(checked)` 的圆角 + 卡片背景色变化表达；Miuix 没有等价的「选中圆角/背景」列表规范，保留 Miuix Checkbox（蓝色勾选）。确认后仍一次插入多个 @。
3. **M3E Bottom Sheet 改为 Compose 内嵌 + 预测性返回**：不再使用 material3 `ModalBottomSheet`（Dialog 收不到 TIM 的 navigationevent 返回），改为自绘 overlay；通过 `NavigationEventHandler` + `NavigationEventTransitionState.InProgress.progress` 驱动，返回手势可关闭 Sheet，预测性返回开启时 Sheet 跟手下滑/松手回弹（Miuix 原本正常，未改）。
4. **平板/大屏适配**：M3E Sheet 限宽 640dp 居中、两侧露出背景与圆角，键盘弹起时整个 Sheet 抬到键盘上方，底部按钮不再被遮挡。
5. **真机验证**：M3E 返回手势关闭 Sheet；Miuix/ M3E 双风格列表与搜索框与联系人页一致；多选 2 人 `sendTextWithAt ats=2` 保持正常。
6. **版本号**：升至 **0.5.118 / versionCode 141**。

## v0.5.117 更新

1. **@ 菜单重做为底部 Bottom Sheet**：群聊输入「@」/全角「＠」不再弹内联面板，而是弹出底部 Sheet（M3E 用 material3 `ModalBottomSheet`，Miuix 用 Miuix 自己的 `OverlayBottomSheet`）：
   - 顶部搜索框（按成员名/QQ号过滤）；
   - 列表**仿联系人页**：A/B/C… 首字母分组头 + 成员行（头像/昵称/群主/管理员/我 徽标），「全体成员」固定第一项；
   - **默认多选**：每行 Checkbox，可同时勾选多人；已选与「全体成员」互斥；
   - 底部「取消 / 确定(n)」，确定后一次性把每个已选成员插入一个 @（选几个就加几个 @）；
   - 支持全角 ＠ 触发；删除 @ 自动关闭；表情/附件面板互斥逻辑不变。
2. **真机验证**：群聊选择 2 名成员 → 输入框「@A4-5000 @ArchZero喵~ 」→ 发送日志 `sendTextWithAt: ats=2 ok=true`，气泡正常显示两个 @；Miuix/M3E Sheet 真机验证。
3. **版本号**：升至 **0.5.117 / versionCode 140**。

## v0.5.116 更新

1. **修复多元素消息被截断（文本+表情+图片）**：一条消息里同时含文本、表情、图片时，之前只显示第一个能解析的元素（文本+[表情]），后面的图片被丢弃。
   - 根因：`TimBridge.describeElements` 遍历 elements 遇到第一个可解析元素就 return，且文本合并循环遇到图片元素直接 break；
   - 修复：新增多元素解析 `describeElementsWithParts`——按真实元素顺序把 TEXT/FACE 累积为文本段、PIC 解析为图片段（复用原图片路径/缩略图/缓存/下载请求逻辑），产物为 `ChatMessage.parts = [TEXT, IMAGE, ...]`；单元素消息仍走原有逻辑（parts 为空），完全兼容；
   - UI 新增 `MultiPartBubbleMaterial/Miuix`：在同一个气泡内竖排渲染文本段 + 图片段（图片 ≤200dp、保持宽高比、圆角、可点开大图；表情图片小图显示），文本链接可点；
   - 图片下载轮询扩展：气泡内任意 part 图片缺路径时也会持续刷新，下载完成后自动回填空位。
2. **真机验证**：某群消息（TEXT+IMAGE、IMAGE+TEXT 混排）双风格真机验证；日志 `multiPart msg=.. parts=2 types=[TEXT, IMAGE]`，气泡内文本与图片都正常显示，不再截断。
3. **版本号**：升至 **0.5.116 / versionCode 139**。

## v0.5.115 更新

1. **群聊 @ 成员 / @全体成员输入**：群聊里输入「@」（全角「＠」同样识别）自动弹出「选择提醒的人」面板：
   - 第一行「全体成员」（@全体），下面是群成员列表（头像/昵称/群主/管理员/我 徽标）；
   - 选择后输入框自动插入「@昵称 」，发送时作为真正的 at 元素发出（TIM QRoute `createAtTextElement`：成员 `atType=2`、@全体 `atType=1`），可同时携带引用；
   - 双风格面板（M3E / Miuix）均已实现。
2. **撤回其他成员消息（群主/管理员）**：聊天页新增长按菜单权限判断——当前账号是该群群主/管理员时，其他成员 2 分钟内的消息长按菜单也会出现「撤回」，走内核同款 `recallMsg`；服务端权限/时效不足时 Toast 失败。
3. **真机验证**：`某群` 群发送 @全体 / @成员成功（日志 `sendTextWithAt: chat=.. ats=1 text=@全体成员 ok=true` / `text=@某成员 ok=true`，气泡正常显示）；@ 面板双风格实测。撤回他人功能已实现，待用「当前账号为群主/管理员」的群 + 他人新消息做最终服务端验证。
4. **版本号**：升至 **0.5.115 / versionCode 138**。

## v0.5.114 更新

1. **修复点击输入框时键盘上方出现空白区域（Miuix / M3E 双风格）**：
   - 原因：TIM SplashActivity 默认 `adjustResize`，输入法弹出时会先把窗口内容缩小一个键盘高度；同时我们的全屏 Compose 覆盖层又用 `WindowInsets.ime` 给输入区加了同样的键盘高度 padding，两者叠加 → 输入框先被顶高「键盘高度 × 2」，屏幕上出现与键盘几乎等高的空白，等 TIM 窗口 resize 稳定后 ime inset 归零、空白才收起（表现为闪一下）。
   - 修复：在 `UiHost.applyImmersiveWindowFlags` 里把 TIM Activity 的 `softInputMode` 改为 `SOFT_INPUT_ADJUST_NOTHING`（仅改 adjust 位，不动 state），窗口不再随 IME resize，只由 Compose 的 `WindowInsets.ime` 精细动画抬升输入框，输入框与键盘同步升起、无空白。
   - 真机逐帧验证：输入法上滑过程中输入框始终紧贴键盘顶部，无空白区域。
2. **版本号**：升至 **0.5.114 / versionCode 137**。

## v0.5.113 更新

1. **语音暂停改为保留播放位置**：播放中点击暂停后，进度条停在当前位置（不再归零），再次点击从该位置继续播放；
2. 实现：`VoicePlaybackSource` 新增 `pauseVoice()/resumeVoice()`，bridge 反射 TIM `CommonAudioPlayer` 内部解码器接口 `h` 调 `pause()`/`start()`（无解码器时回退 `stop()` + `A(path, positionMs)` 重播）；暂停位置记录并用于续播/进度条显示；控制器新增 `pausedMessageId/pausedProgress` 与 `progressFor()`；
3. 状态语义：暂停后 `isVoicePlaying()=false`、图标回到 Play，但进度条停在暂停位置；自然播完才归零；切到其它语音/离开聊天页会清空暂停状态；
4. **真机验证（某群 4" 语音）**：播放 1.2s 点击暂停 → `voicePause: ... positionMs=1140 decoder=true ok=true`，进度条停住；再次点击 → `voiceResume: ... positionMs=1140 ok=true`，从 1140ms 继续播放。
5. **版本号**：升至 **0.5.113 / versionCode 136**。

## v0.5.112 更新

1. **文件下载进度改为真实百分比**：不再用一直为 0 的 `FileElement.progress`，改为接入内核 `IKernelMsgListener.onRichMediaProgerssUpdate(FileTransNotifyInfo)`（`fileProgress`/`totalSize`），进度 0-100 实时刷新，未知时才显示不确定态。真机 164.7MB zip 日志 `richMedia progress: pct=1→3→4→12→13→15…→87→100`，气泡内进度条同步爬升。
2. **语音气泡重做**：删除中间的波形竖条；改为「播放/暂停图标 + 中间常驻进度条 + 时长」一行布局，点开前也显示进度条（0%），播放中实时推进。
3. **进度条可拖动 seek**：语音进度条支持横向拖动调整播放位置（松手后从新位置继续播放），实现走 TIM `CommonAudioPlayer` 的 `A(path, offsetMs)`；真机日志 `voiceSeek: ... positionMs=2841 ok=true`。
4. **M3E 波浪加大**：语音进度条 10dp、文件下载进度条 8dp（amplitude 4dp），波浪更明显；Miuix 保持 4dp 标准直线进度条。
5. **真机验证（测试平板，某群）**：linux-arch.zip 164.7MB 实时百分比；4" 语音播放 + 拖动 seek；M3E/Miuix 双风格实测。
6. **版本号**：升至 **0.5.112 / versionCode 135**。

## v0.5.111 更新

1. **文件下载进度条（气泡内）**：点击未下载文件后，文件气泡在文件名/大小下方显示进度条，下载中隐藏状态图标；
   - M3E：Material 3 Expressive **波浪进度条**（`LinearWavyProgressIndicator`，6dp，带停止圆点）；
   - Miuix：Miuix 标准 `LinearProgressIndicator`（4dp）；
   - 进度来自内核 `FileElement.progress`（`MessageSource.fileDownloadProgress` 轮询，500ms 节流），未知时显示不确定态；下载完成后自动打开并恢复「打开」图标。
2. **语音播放进度条**：播放中在波形下方显示同款进度条（M3E 波浪 / Miuix 标准），进度来自 TIM `CommonAudioPlayer.m()/n()`（当前位置/总时长）；播完/停止自动消失，颜色随气泡内容色（深色气泡上白色、浅色气泡上深色），避免与气泡背景混在一起。
3. **真机验证（测试平板）**：
   - 文件：`fileDownload: progress ... 0 → 100`，气泡内进度条出现（M3E 波浪/ Miuix 直线）→ 下载完成自动调系统打开方式；
   - 语音：4" 语音播放时进度条随播放推进（波浪/直线均验证），播完恢复 Play 图标。
4. **版本号**：升至 **0.5.111 / versionCode 134**。

## v0.5.110 更新

1. **文件消息支持下载 + 打开**：文件气泡右侧显示状态图标（未下载 `下载`、已下载 `打开`），点击气泡时：
   - 已下载 → 通过 TIM 的 `FileProvider7Helper` 取 `content://` URI，`ACTION_VIEW` 交给系统应用打开；
   - 未下载 → 按 TIM `AIOFileManagerApiNtImpl.downloadFile` 同款 `RichMediaElementGetReq{downloadType=1}` 触发内核下载，轮询下载完成回调拿到本地路径（`Tencent/TIMfile_recv/xxx`），随后自动用系统应用打开。
2. **打开方式**：MIME 由扩展名推断（如 `video/mp4`），`FLAG_GRANT_READ_URI_PERMISSION` 授权；真机视频文件弹出系统「打开方式」选择器，选「相册」后成功播放。
3. **真机验证（测试平板，某群 11.8MB 视频文件）**：
   - 点击前气泡显示下载图标；点击后 `richmedia complete ... path=.../TIMfile_recv/PXL_....mp4`（约 1 秒下载完 12MB）；
   - `fileOpen: ok name=PXL_....mp4 mime=video/mp4 authority=FileProvider7Helper`，系统 Resolver 弹出，选「相册」→ Google Photos 正常播放视频；
   - 返回聊天页后气泡图标切换为「打开」。
4. **版本号**：升至 **0.5.110 / versionCode 133**。

## v0.5.109 更新

1. **修复语音时长显示**：`PttElement.duration` 单位是**秒**（TIM `AIOPttUtil.b(int)` 即按秒格式化），bridge 原来误当毫秒 `/1000` → 所有语音都显示 1s；现按秒显示（真机 4s 语音显示 `4"`）。
2. **语音文件自动下载**：收到语音时 `PttElement.filePath` 为空，打开聊天页会按 TIM `PttMsgItem.h2()` 同款 `RichMediaElementGetReq`（msgId/peerUid/chatType/elementId/downloadType=1）触发内核下载；下载完成回调写入元素路径，聊天页轮询刷新后取得本地文件（真机路径 `Tencent/Tim/<uin>/ptt/xxx.amr`）。
3. **播放改用 TIM 自带播放器（silk/amr 都能播）**：新增 `VoicePlaybackSource` 数据源，宿主里用 `com.tencent.mobileqq.qqaudio.audioplayer.CommonAudioPlayer`（TIM 同款音箱/解码器、自动选 SilkPlayer/AmrPlayer），`playVoice/stopVoice/isVoicePlaying` 反射封装；独立 App 回退 MediaPlayer。
4. **播放状态 UI**：点击语音条播放/暂停，图标 Play↔Pause、波形高亮；播放中每 350ms 轮询 `isVoicePlaying()`，播完自动恢复 Play；切歌/离开聊天页停止；失败 Toast「语音播放失败」。
5. **真机验证（测试平板，某交流群，4" 语音）**：
   - 时长显示 `4"`，点击后日志 `voicePlay: path=...ptt/xxx.amr offsetMs=0 ok=true`；
   - `dumpsys audio` 出现 `AudioTrack state:started usage=VOICE_COMMUNICATION content=SPEECH sampleRate=16000`，确认真实出声；
   - 播放中气泡显示 Pause 图标，4 秒后自动恢复 Play。
6. **版本号**：升至 **0.5.109 / versionCode 132**。

## v0.5.108 更新

1. **语音消息播放**：语音条可点击播放/暂停（`VoicePlaybackController` + `MediaPlayer`，同一时间只播一条；播放完成/切换/离开聊天页自动停止），播放中图标切换、波形高亮；M3E 与 Miuix 两套气泡均支持。语音文件用 TIM 缓存的 PTT 本地路径，文件缺失/格式不支持时 Toast 提示。
2. **消息撤回**：长按自己的消息（2 分钟内、非本地草稿）菜单新增「撤回」；确认后走内核 `recallMsg`（wrapper `recallMsg(Contact,long,cb)`，失败自动回退 native `recallMsg(Contact,ArrayList<Long>,cb)`），成功后刷新消息列表与会话列表。
3. **真机验证（测试平板）**：发送测试消息 → 长按菜单出现「撤回」→ 点击后日志 `chatSetting: recallMessage(...) ok=true code=0 impl=wrapper`，气泡变为系统灰条「测试账号B 撤回了一条消息」，会话列表摘要同步更新。
4. **语音验证说明**：当前 TIM 账号的会话里未检索到语音消息（最近会话/历史页均无 PTT 元素），播放链路已接好，待用户在会话里发送语音后验证；TIM 语音若是 silk 格式，Android `MediaPlayer` 可能不支持（会走失败 Toast），后续可按需接 TIM 自带 `CommonAudioPlayer` 兜底。
5. **版本号**：升至 **0.5.108 / versionCode 131**。

## v0.5.107 更新

1. **动图支持（GIF / 动态 WebP / APNG）**：聊天里的表情包图片不再只显示静态首帧，气泡内与点开预览页都**无限循环播放、不中断**。
   实现：新增 `ui/component/AnimatedImage.kt`（API 28+ `ImageDecoder` 解出 `AnimatedImageDrawable`，`repeatCount=REPEAT_INFINITE`，ImageView 挂载自动播放、onRelease 停止；静态图/低版本自动回退原 Bitmap 路径）。
   已接入 M3E/Miuix 的表情图片气泡、普通图片气泡与图片预览页。
2. **修复平板模式导航栏不沉浸**：TIM 在大屏/平板模式下会把窗口 `decorFitsSystemWindows` 改回 true / 重设窗口属性，导致我们的 Compose 覆盖层被底部导航栏 inset 掉，导航栏区域露出 TIM 窗口背景（灰色），Miuix liquid glass 底栏本应透明的地方变成灰填充。
   修复：`UiHost` 在挂载后给 decorView 挂 layout / insets 监听，并在 Activity resume 时（节流 250ms）重新应用 edge-to-edge（`setDecorFitsSystemWindows(false)` + 状态栏/导航栏透明 + 关闭导航栏对比度强制）；平板横屏/竖屏、M3E/Miuix 均恢复沉浸。
3. **真机验证（测试平板）**：
   - 表情包 GIF 在气泡与预览页均循环播放；
   - 平板竖屏/横屏导航栏区域像素与页面内容一致（此前为 TIM 窗口背景灰色 `(28,28,28)`），Miuix liquid glass 底栏下方恢复透明；
   - 手机模式沉浸保持正常。
4. **版本号**：升至 **0.5.107 / versionCode 130**。

## v0.5.106 更新

1. **修复「@某人 + 正文」消息只显示 @ 不显示正文**：TIM 内核会把这类消息拆成多个 `TextElement`
   （如 `[Reply, Text("@测试账号B"), Text(" aaa")]`），此前只取第一个文本元素，导致正文丢失。
   现在合并后续连续的文本元素（以及夹在中间的表情元素）为一条完整正文，引用气泡/普通气泡都生效。
2. **真机验证（测试平板，某群）**：测试账号A 引用图片并发送「@测试账号B aaa」，
   修复后气泡显示 `回复 我 [图片]` + `@测试账号B aaa`，与 LinuxQQ 一致。
3. 该合并逻辑对其它被内核拆分的文本（@、表情混排等）同样生效。
4. **版本号**：升至 **0.5.106 / versionCode 129**。

## v0.5.105 更新

1. **进入聊天即标记已读**（对齐 TIM 原生行为）：打开会话页时调用内核 `setChatUnread(chatId, false)`
   （`IKernelMsgService.setMsgRead`），随后刷新会话列表；退出后该会话角标清零、提醒标签（[有人@我] 等）也随内核已读清除。
2. **在聊天页内收到新消息也同步标记已读**：`LiveEvents.messageEvents` 收到当前会话的更新后再次置已读（conflate 合并、不刷屏），
   避免“停留聊天页时新消息把未读/角标又加回来”。
3. 两套 UI（M3E / Miuix）共用 `ChatDetailScreen` 入口逻辑，行为一致。
4. **真机验证（测试平板）**：从列表打开 99+ 的「某群」群 → 日志 `chatSetting: setChatUnread(tim_2_291597771, unread=false) ok=true`，
   返回列表后该群角标消失；打开会话期间的推送也会触发同样置已读。
5. **版本号**：升至 **0.5.105 / versionCode 128**。

## v0.5.104 更新

1. **消息列表聊天卡片增加「提醒标签」**（TIM 风格，红色前缀，位于最新消息预览最前）：
   - `[有人@我]` / `[@全体成员]`：来自内核最近会话 `atType` 位掩码（bit4=@我、bit1=@全体，语义从 TIM NTTroopNotificationProcessor 核对）；
   - `[有新文件]` / `[群公告]` / `[新的作业]` / `[@我的红包]` / `[@我的礼物]` / `[QQ红包]` / `[有人回复我]`：来自 `listOfSpecificEventTypeInfosInMsgBox` 事件类型（1002/1004/1005/1007/2001/2003/2004/2005）；
   - `[特别关心]`：`specialCareFlag`；仅未读时显示，有草稿时只显示 `[草稿]`。
2. **数据源修正**：`loadRecentChats` 的 wrapper 最近会话列表是精简版（`atType` 恒 0），新增 `getRecentContactListSnapShot` 内核快照（`CompleteRecentContactInfo.changedList`）补全标签字段；快照失败/超时回退原字段（60s 冷却，不影响列表其它字段）。
3. **双风格渲染**：M3E 与 Miuix 均用 `AnnotatedString`（标签 `colorScheme.error` 红、其余保持次级文字色；Miuix 确认支持 AnnotatedString 重载），单行省略/跑马灯不破坏布局。
4. **真机验证（测试平板）**：`[有人@我]`（某群 @测试账号B）与 `[有新文件]`（某群文件事件）在 M3E/Miuix 均显示为红色前缀；日志 `chatTag: chatId=.. atType=6 event=1000 source=snapshot -> AtMe`。
5. **版本号**：升至 **0.5.104 / versionCode 127**。

## v0.5.103 更新

1. **修复群聊免打扰判定（GroupMsgMask 语义写反了）**：TIM 群设置四项与 `GroupMsgMask` 的真实对应是——
   `NOTIFY` = 接收消息并提醒（正常）、`RECEIVE` = 接收消息但不提醒（免打扰）、`ASSISTANT` = 收进群助手且不提醒、`SHIELD` = 屏蔽群消息（从 TIM 资源字符串 `esm/esr/esn/esj` 核对）。
   之前把 NOTIFY/RECEIVE 理解反了，导致正常提醒的群显示成免打扰（或反之）。
2. **群免打扰读取改为内核群资料 `cmdUinMsgMask`**（`batchQueryCachedGroupDetailInfo`，与 TIM 群设置页同源），不再用群聊恒为 false 的 `RecentContactInfo.isMsgDisturb`；单聊仍用 `isMsgDisturb`。
   本机实测：76 个群中 **68 个免打扰**（与 TIM 设置一致），免打扰群显示**淡色 `surfaceVariant` 徽标 + 静音图标**；正常提醒群（如 某交流群）显示 primary 徽标、无静音图标。
3. **修正聊天信息页「消息免打扰」开关写入值**：群聊开关此前也是反向写（免打扰→NOTIFY），现改为免打扰→`RECEIVE`、正常→`NOTIFY`；群聊读回不再依赖不可靠的 `isMsgDisturb`。
4. **真机验证（测试平板）**：列表顶部聊群全部显示免打扰外观（与 TIM 一致）；某交流群为正常提醒（聊天信息里开关为关）；免打扰/正常未读徽标颜色可区分。
5. **版本号**：升至 **0.5.103 / versionCode 126**。

## v0.5.102 更新

1. **未读徽标区分免打扰**：消息列表的未读角标按会话状态区分配色——
   - 正常未读：主题主色（`primary` / `onPrimary`）；
   - 消息免打扰的未读：使用相对卡片更淡的 `surfaceVariant` / `onSurfaceVariant`（Miuix 用其等价角色），一眼可区分免打扰会话。
2. **长按会话卡片菜单**（与长按消息气泡同款弹层，M3E + Miuix 双风格）：长按聊天卡片弹出三项菜单——
   - **置顶 / 取消置顶**（按当前置顶状态动态显示）；
   - **标为未读 / 标为已读**（按未读状态动态显示，角标实时变化）；
   - **删除**（红色，点击后先弹确认框「仅从消息列表移除，不删除聊天记录」）。
   菜单锚点为按下的位置；Miuix 版带页面级全屏遮罩（含状态栏压暗，沉浸）；长按不会触发打开聊天。
3. **bridge 能力**：新增 `setChatUnread`（标未读 `setMarkUnreadFlag`；标已读 `setMsgRead` + `clearMsgUnreadCount` 兜底）与 `deleteChat`（`deleteRecentContacts` 真实 RecentContactInfo，兜底 `deleteRecentContactsVer2`），成功后刷新会话列表。
4. **真机验证（测试平板，Material& Miuix 双风格）**：置顶/取消置顶、标未读/已读（免打扰会话显示淡色角标）、删除（确认框 → 列表移除）全部生效；日志 `chatSetting: ... ok=true`。
5. **版本号**：升至 **0.5.102 / versionCode 125**。

## v0.5.101 更新

1. **扫一扫按钮卡片移到底部**：M3E 与 Miuix 的操作卡片从屏幕中央移到屏幕底部（左右 24dp、底部 24dp）。
2. **修复 Miuix 首页加号菜单的沉浸问题**：此前 Miuix 弹窗是独立 Dialog window，自身的全屏阴影不覆盖状态栏，导致状态栏区域不被压暗。现改为「页面级全屏遮罩 + 弹窗 Dialog 状态栏/导航栏透明」（与长按消息菜单同款方案）：
   - 状态栏区域一起压暗，全屏沉浸；
   - 页面遮罩点击可关闭菜单；
   - 弹窗系统栏透明逻辑抽到公共 `TransparentSystemBarForPopup()`，长按菜单与首页加号菜单共用。
3. **版本号**：升至 **0.5.101 / versionCode 124**。

## v0.5.100 更新

1. **扫一扫按钮卡片化**：M3E 与 Miuix 都把「打开相机 / 选择图片 / 取消」三个按钮收进同一张卡片内。
2. **M3E 配色**：「选择图片」与「打开相机」同为填充主色按钮；「取消」改为填充 tonal 按钮（不再是纯文字视觉）。
3. **Miuix 配色**：「打开相机」与「选择图片」统一为同款主色按钮。
4. **版本号**：升至 **0.5.100 / versionCode 123**。

## v0.5.99 更新

1. **扫一扫改三按钮操作页**：点扫一扫不再自动拉起相机，默认显示「打开相机」（全宽主按钮）/「选择图片」「取消」三个按钮；任意识别失败会显示原因与可复制的识别文本，可反复重试。
2. **相册图片识别**：「选择图片」调用系统媒体选择器（单选），选中图片后本地识别二维码，识别与跳转逻辑同拍照一致。
3. **聊天「照片」改多选**：聊天加号菜单的照片选择改为系统媒体选择器多选（最多 9 张），按选择顺序逐张发送；图片复制/尺寸解析在后台线程顺序处理，避免卡顿。
4. **真机验证**：从相册选 bilibili 二维码 → `opened custom tab via com.android.chrome` 打开 b站（用户确认为正确行为）；选 QQ 名片二维码 → `qr: openContent -> JumpAction` 打开 TIM 个人资料卡；聊天多选 2 张图片逐张发送成功。
5. **版本号**：升至 **0.5.99 / versionCode 122**。

## v0.5.98 更新

1. **扫一扫**（消息页加号菜单 → 扫一扫）：调用系统相机拍照 UI，拍照后本地识别二维码（ZXing），识别成功按内容类型跳转：
   - 普通/第三方链接（如 bilibili）：与聊天气泡链接一致，走 Chrome Custom Tabs；QQ/腾讯域名走 TIM 自带浏览器；
   - QQ 功能性二维码（`mqqapi://`、`mqq://`、`qq://`，如个人名片、加群）：交给 TIM 原生 JumpAction 打开对应页面；
   - 其它（小程序码、公众号、QQID、纯文本等）：交给 TIM 原生扫码结果分发页面（DispatchScanResultActivity）；非链接文本会显示可复制的识别结果。
2. 拍照使用系统相机（`ACTION_IMAGE_CAPTURE` + MediaStore 输出），无需相机/存储权限（Android 10+）；取消/未识别/识别失败均有重拍/取消。
3. 说明：聊天页面的「拍照」功能未改动；此前测试扫一扫时曾临时停用系统相机 App，恢复后拍照正常。
4. **版本号**：升至 **0.5.98 / versionCode 121**。

## v0.5.97 更新

1. **验证页按目标加好友设置自适应**：进入验证页先查询目标 `friend_setting`（TIM NT getBuddySetting），按设置渲染：
   - `0` 允许任何人：隐藏验证消息输入框，提示「对方允许任何人添加，无需验证信息」，发送空验证消息；
   - `1` 需要验证信息：保持验证消息输入框（默认「我是 Material3 TIM 用户」）；
   - `2/3/4` 需要回答问题类：按问题逐条显示「问题N：…」+「回答」输入框，发送时按 TIM 原生模板组装（单题=答案原文；多题=`问题1:<q>\n回答:<a>…`）；答案非空才可发送。
2. **真机验证**：测试账号B 设为「需要回答问题」（friend_setting=4，两个问题）后，通过本 UI 填写答案发送，对方正常收到好友申请与问题回答 ✅。
3. **bridge 支持**：`requestAddFriend` 放开 friend_setting=2/3/4 发送（此前有“暂不支持问答”的拦截）；新增 `queryFriendSettingInfo` 供 UI 预查设置。
4. **版本号**：升至 **0.5.97 / versionCode 120**。

## v0.5.96 更新

1. **加好友/搜索实现收尾清理（净 −573 行）**：删除已无调用者的死代码——旧内核加好友 `requestAddFriendByKernel`、`addFriend(user)` 及原生页拉起 `launchAddFriendPage`、ContactCard 手机号解析簇（`resolveUinByPhone`/`contactCardByPhone`/`requestProfileCardByMobile` 等）、群公告内核探测 `probeGroupBulletins`、建群后临时成员校验；`searchFacadeOnce`/`searchFacadePaged` 合并为统一 `searchFacade(...maxPages)`（找人/群号 1 页、昵称搜群 25 页行为不变）。
2. **日志降噪**：删除逐页/逐条探测日志、token 全量 utf8/base64/hex 打印、`NT addBuddy sent` 等重复行；token 改为 `len+hex 前缀`，verify 仅失败时打印 32B；保留 search 汇总、friend_setting、precheck 一行、addBuddy prepare/回调 result/errorCode、ok/failed 总结。
3. **UI/资源清理**：删除未引用字符串 `add_friend_sent`/`add_friend_send_failed`、KDoc-only import；移除已无调用者的 `AddFriendSource.addFriend` 接口方法；验证页签名与行为不变。
4. **冒烟验证**：测试机 真机搜索 QQ号结果/验证页/昵称搜群分页（wrt → 7 页 7 群）均正常；加好友链路代码路径未变（friend_setting 查询 + NT addBuddy + msg）。
5. **版本号**：升至 **0.5.96 / versionCode 119**。

## v0.5.95 更新

1. **需要验证信息的加好友流程真机跑通**：在 测试机（测试账号A 登录）上通过本 UI 添加「需要验证信息」的 测试账号B，先查 `friend_setting=1`，再按原 setting 带验证消息发送，`NT addBuddy result=0 errorCode=0`，验证消息（msgLen=19）已真实发出 ✅。
2. **加好友预检**：发送前调用 `queryUinSafetyFlag`（NT 0xfe1）与 `getAddBuddyRequestTag`（NT 0x1004）；仅记录不阻断（TIM 产品对 safety cmd 无权限 -10122，tag 请求正常返回）。
3. **sig 重试**：NT addBuddy 失败且响应带 `AddBuddyRsp.sig` 时，用同 setting 带 sig 重试一次（对齐 TIM NTFriendAddReceiver 的重发逻辑）。
4. **减少无谓请求**：查到 `friend_setting` 后只按该值发送（+sig 重试一次），不再反复试 0/1/3，避免加剧服务端风控。
5. **verify 诊断**：始终打印 `errorCode/len/hex64`，并按 TIM 语义解析 `FunctionLockMsg$FunctionLockBeatRsp`（功能锁 wording/btns）或 `AddContactVerifyInfo$AddFriendVerifyInfo`（安全验证 tips/url），解析到的文案回填 UI 失败提示。
6. **此前 168 的结论**：`errorCode=168` 是 QQ 加好友账号风控/功能锁（TIM 原生同样会弹功能锁），已由用户人工解除；解除后本版本发送成功。
7. **版本号**：升至 **0.5.95 / versionCode 118**。

## v0.5.94 更新

1. **QQ号搜索兜底**：搜索 5~12 位纯数字且本地/远端都没有用户、也没有同数字群结果时，显示「QQ用户 / QQ号: xxx」占位卡片，可直接点进验证页添加（手机号形态关键词则显示「手机号用户」）。
2. **手机号→QQ号解析尝试**：新增内核 `IKernelAddBuddyService.requestInfoByAccount(AccountInfo.phoneNum)` 解析路径；真机返回 `-10122 Product does not have permission to access cmd`（TIM 产品未开放该 cmd），目前降级为 phone-style 发送。
3. **验证页失败提示**：Toast 会优先显示服务端返回的文案（之前已接）。
4. **当前真机状态（重要）**：两台测试发送账号（10000002 / 10000003）从本设备发送加好友请求均返回 `errorCode=168`（QQ 加好友功能锁/风控），换不同目标（测试账号A、测试账号C）结果一致，TIM 原生同样会被限；推测是开发期高频测试触发的设备/IP 级风控，需冷却后再验证「需要验证信息」流程端到端。
5. **版本号**：升至 **0.5.94 / versionCode 117**。

## v0.5.93 更新

1. **按 TIM 原生流程先查好友设置再发送**：加好友请求前先查对方 `friend_setting`（`AddFriendNTServlet` + ResultReceiver 捷径，等价 TIM `NTAddFriendHandler.N1` → `receiver g`）：需要验证信息的目标返回 `friend_setting=1`，随后用该 setting 带验证消息（`AddBuddyReq.answer`）发送；0=允许任何人、2/4=需要回答问题（暂不支持问答，直接提示失败）。
2. **服务端错误文案透传**：发送失败时 UI Toast 优先显示服务端返回的文案（如“添加失败，请稍后再试”），不再只显示通用失败提示。
3. **已知限制（当前测试账号状态）**：由于开发期间对同一账号发送了大量加好友请求，服务端返回 `errorCode=168`（功能锁/加好友风控），此时任何客户端（含 TIM 原生）都无法继续添加；需等限制解除后再复测。允许任何人目标的请求不带验证消息属于 TIM 原生预期行为。
4. **版本号**：升至 **0.5.93 / versionCode 116**。

## v0.5.92 更新

1. **好友验证消息真正生效**：加好友请求改用 TIM 原生的 NT addBuddy 链路（`AddBuddyReq`，Bundle `msg` → `AddBuddyReq.answer`），验证消息会随请求送达；原 kernel `reqToAddFriends.verifyInfo` 在 TIM 产品里很可能被忽略，只在 NT 失败时作为回退（并补充 `answer` 字段）。
2. **不再弹 TIM 原生加好友页**：手机号/QQ号目标的验证页发送失败时只提示「发送失败」，彻底移除了 requestAddFriend 里的原生页兜底（之前手机号可能弹原生页并提示“隐私设置”）。
3. **真机验证**：QQ号 10000001 与手机号 13800000000（测试账号A）发送后日志均为 `NT addBuddy(... msgLen=19 ...) code=0 result=0`，当前焦点始终是 TIM 主界面（未启动原生 Activity），进程无崩溃。
4. **版本号**：升至 **0.5.92 / versionCode 115**。

## v0.5.91 更新

1. **加好友验证流程**：搜索结果点个人账号 → 进入「加好友」验证页（目标头像/名称/QQ号或手机号 + 验证消息输入框，默认「我是 Material3 TIM 用户」）→ 点发送调用内核 `reqToAddFriends`（`verifyInfo` + `Verify` token）真正发送好友请求；成功提示「好友请求已发送」。
2. **手机号目标**：手机号搜索结果会先通过 TIM 资料卡接口（`IProfileDataService.getContactCardByMobileNo` + profile 请求）解析出真实 QQ 号/uid，再走同一条内核请求；真机用 13800000000（测试账号A）验证发送成功。
3. **崩溃修复**：`getUidByUin` 的 ArrayList 元素类型必须为 Long，之前传 String 会触发 JNI FatalError（SIGABRT）；已修复。
4. **真机验证**：QQ号入口（10000001）与手机号入口（13800000000）两条路径均返回 `code=0 success`，测试账号 测试账号A 已收到好友请求。
5. **版本号**：升至 **0.5.91 / versionCode 114**。

## v0.5.90 更新

1. **昵称搜群支持分页**：搜索群名关键词时会按页拉取远端群（每页约 4 条），最多翻 25 页并按群号去重（真机搜 `wrt`：远端群从 4 个提升到 7 个；`某群` 仍为 2 本地 + 4 远端）。
2. **搜索结果解析更健壮**：不再依赖返回 item 的 type 字段，改为按记录内容（群号/uin/手机号）判断群或人，避免 TIM 版本差异导致结果丢失；同时过滤内核空壳资料造成的假个人卡片。
3. **已知差距**：原版 TIM 搜群名能出上百个群，是因为「找群」页走独立的 findtroop 群搜索协议（`TroopSearchRecommendHandler.V`，每页约 15 条）。已定位并可在请求层拿到数据，但 `RecommendTroopItem` 到界面的映射/翻页还没接通，当前版本结果量仍少于原版，后续版本继续对齐。
4. **版本号**：升至 **0.5.90 / versionCode 113**。

## v0.5.89 更新

1. **纯昵称搜群**：加好友/群页输入群名关键词（非数字也可以）时，会走 TIM 的 `SummaryCard.ReqSearch` 群搜索，本地已加入的群 + 远端同名群一起显示（真机搜索 `某群` 返回 2 个本地群 + 4 个远端群，均带群号/人数）。
2. **手机号搜索**：`SummaryCard.ReqSearch` 找人支持手机号（真机用 13800000000 搜到 测试账号A，卡片显示「手机号: 13800000000」）。手机号结果没有 uid/uin，点击后回落 TIM 原生加好友页完成验证/添加。
3. **版本号**：升至 **0.5.89 / versionCode 112**。

## v0.5.88 更新

1. **修复创建群聊只建出「自己一个人」的群**：创建改走 TIM 自己的建群链路 `TroopCreateInfo → com.tencent.qqnt.bean.a → ITroopOperationRepoApi.createGroupV2`（含 `troopSize = 选中人数 + 1`），建群成功后按 uid 逐个调用 `ITroopMemberOperationRepoApi.inviteToGroup` 邀请选中成员。真机实测三人群（自己 + 两个小号）成员正常进群。
2. **修复加好友/群搜索非好友、非成员搜不到**：移除 TIM 产品无权限的内核 `searchStranger/searchGroup`，改用 TIM 自己的 `SummaryCard.ReqSearch`（SearchFacade）——QQ号可搜到非好友用户（真机用群成员 QQ 号验证），群号可搜到未加入的群（真机验证），手机号会走找人请求。
3. **过滤假个人结果**：内核 `getUserDetailInfoByUin` 对群号会返回空壳 ProfileDetailInfo，之前会冒出「QQ用户 / QQ号: 群号」假卡片；现在 uid 与昵称都为空时直接丢弃。
4. **已知限制**：短时间连续建群会触发 TIM 服务端风控（返回 code=1006），稍后再试即可；纯昵称搜索只匹配本地联系/会话；需验证的群加群会回落 TIM 原生加群页。
5. **版本号**：升至 **0.5.88 / versionCode 111**。

## v0.5.87 更新

1. **创建群聊**：加号菜单「创建群聊」→ 联系人多选页（每行 Checkbox、整行点选、顶部「确认(N)」）；确认后用 TIM 内核 `createGroupWithMembers(群名, uid 列表, callback)` 创建群（群名自动生成 `TIM群聊 MM-dd HH:mm`），成功后直接进入新群聊天页。
2. **加好友/群**：新增搜索页，支持 **QQ号 / 手机号 / 群号**；结果沿用消息列表行 UI，**个人账号在上、群聊在下，两块独立卡片**，卡片上方分别为「个人」「群聊」section header（联系人页字母分组同款样式）；点个人行发送好友请求（内核 `reqToAddFriends`），点群行申请加群（内核 `joinGroup`，需验证群回落原生加群页）。
3. **扫一扫**：拉起 TIM 原生扫一扫（`IQRJumpApi.getScannerActivityCls()` → ScannerActivity）。
4. **搜索实现与限制**：本机已有好友/最近会话/可查到的 QQ 号可用（如自己的小号）；群号支持精确查询（`getGroupInfoForJoinGroup` + 本地群数据）；TIM 内核的陌生人搜索/群搜索 cmd 在 TIM 产品上无权限（-10122）或返回空，因此**完全陌生的 QQ 号与手机号搜索可能没有结果**（与 TIM 自身能力限制一致）。
5. **版本号**：升至 **0.5.87 / versionCode 110**。

## v0.5.86 更新

1. **消息页右上角加号菜单**：点击「+」弹出菜单，条目为 **创建群聊 / 加好友/群 / 扫一扫**（功能本身待后续接入，当前点击为占位提示）。
2. **M3E 使用 Material 原生下拉**：复用设置页「色彩风格」同一套 `OffsetAnchoredExpressiveMenu`（`DropdownMenuPopup` + `DropdownMenuGroup` + `DropdownMenuItem`，带前置图标），锚定在「+」按钮处弹出，不自行绘制菜单。
3. **Miuix 使用 Miuix 原生弹窗**：`WindowCascadingListPopup` + `DropdownEntry/DropdownItem`（Miuix 下拉菜单同款，右对齐锚定在「+」下方），图标用 Miuix Icons（Community / AddCircle / Scan）。
4. **版本号**：升至 **0.5.86 / versionCode 109**。

## v0.5.85 更新

1. **聊天信息页「置顶聊天」接真实功能**：宿主里走 TIM 内核 `RecentContactService.setContactListTop(isTop, [ContactTopData], cb)`（与 TIM 自己会话页同一路径；`ContactTopData` 按单聊/群聊填 uid/uin/groupCode），成功后刷新会话列表；置顶排序/标签与重启持久化已在真机验证。
2. **「消息免打扰」接真实功能**：单聊走 `IKernelBuddyService.setMsgNotify(uid, noDisturb)`，群聊走 `IKernelGroupService.setGroupMsgMask(groupCode, GroupMsgMask.NOTIFY/RECEIVE)`；开关初始状态从最近会话 `isMsgDisturb` 读取，乐观更新、失败回滚。
3. **删除「强提醒」开关**：TIM 没有该功能，M3E 与 Miuix 两套聊天信息页均已移除。
4. **实现「查找聊天记录」**：新增搜索页（M3E / Miuix），用内核 `IKernelSearchService.searchChatMsgs(关键字列表, SearchChatMsgsParams, cb)` 在指定会话内搜索（CONTENT|SENDER、分页 `searchMoreChatMsgs`），结果列表后点击可跳回聊天并定位到该条消息（进入聊天后按 seq 载入附近历史、`animateScrollToItem` 定位，并自动收起搜索键盘）。
5. **兼容性修正**：`loadHistoryAround` 在内核返回 `err=2004000` 但仍带记录时不再当作失败（否则聊天记录跳转会定位不到）。
6. **版本号**：升至 **0.5.85 / versionCode 108**。

## v0.5.84 更新

1. **点击收藏表情直接发送**：收藏页点击表情会以「表情」形态发给当前会话——宿主里通过 `IMsgUtilApi.createPicElement(path, original, picSubType=1)` 构造 `PicElement`（`picSubType = 1` = 表情图片，不是普通大图），TIM/QQ 客户端会按表情渲染。
2. **表情图片按表情尺寸渲染**：解析消息 `PicElement.picSubType`，`picSubType == 1` 的消息（发出的收藏表情、别人发的表情图片）在聊天页按表情样式显示（无大图气泡框、最大约 132dp），M3E 与 Miuix 双风格一致。
3. **交互细节**：点击后先本地回显、后台发送，成功后由宿主推送刷新替换为真实消息；本地文件缺失或发送失败时 Toast 提示。
4. **版本号**：升至 **0.5.84 / versionCode 107**。

## v0.5.83 更新

1. **收藏表情排序调整**：TIM 返回的收藏表情列表按添加时间正序（最早添加的在前），现在收藏页反转为**最后添加的排最前**，符合直觉。
2. **版本号**：升至 **0.5.83 / versionCode 106**。

## v0.5.82 更新

1. **表情面板适配「收藏」页**：收藏页现在展示**用户自己收藏的表情**（在 TIM 里「添加到表情」收集的单张表情图片，真实数据来自宿主 `IFavroamingDBManagerService.getEmoticonDataList()`，读取本机已缓存的图片文件），M3E 与 Miuix 两套 UI 都支持；没有收藏时显示「还没有收藏的表情」。
2. **移除「最近」入口**：按需求把表情包底部栏的「最近」删掉，M3E 与 Miuix 底部都只剩「收藏 / 全部表情包」两个入口。
3. **版本号**：升至 **0.5.82 / versionCode 105**。

## v0.5.81 更新

1. **修复 Miuix 长按菜单弹出时状态栏不沉浸**：Miuix 的菜单弹窗是独立 Dialog window，默认用自己的主题 `statusBarColor` 给状态栏画不透明底色，且其 window dim 只覆盖窗口内容区域。现在改为：页面根布局统一绘制**全屏压暗层（含状态栏/导航栏区域）**、弹窗自身不再做 window dim，并把弹窗 Dialog window 的状态栏/导航栏改为透明（关闭对比度遮罩），状态栏区域跟随全屏阴影压暗。
2. **修复 M3E 聊天页「表情包」面板底部小白条（导航栏）不沉浸**：面板本身在导航栏 inset 之上结束，导致导航栏区域露出页面背景色、与面板之间出现一条色差。现在表情/附件面板展开时把面板底色（`surfaceContainerLow`）延伸进系统导航栏区域，底部栏与小白条区域连成一体。
3. **版本号**：升至 **0.5.81 / versionCode 104**。

## v0.5.80 更新

1. **修复多选模式下点击非文本气泡触发原点击行为**：此前多选时点图片/视频会打开预览、点链接会打开网页、点聊天记录会进入记录页。现在行级在 Initial pass 消费抬起事件，多选时点击只切换选中，不再触发子组件点击；同时多选时不再挂长按菜单、禁用左滑引用。
2. **多选触控优化**：从「只能点 checkbox」改为**点击消息行任意位置**（气泡 / 头像 / 昵称 / 时间 / 行内空白）都切换选中/取消；拖动仍可正常滚动列表，长按不会误切换。Material 与 Miuix 两套聊天页同步实现。
3. **版本号**：升至 **0.5.80 / versionCode 103**。

## v0.5.79 更新

1. **修复打开聊天时列表闪一下**：此前进入聊天页时，消息列表先在首帧渲染列表顶部（最旧消息），下一帧再由 `LaunchedEffect` 里的 `scrollToItem` 跳到最新消息，视觉上会闪一下。
   现在改为在**布局之前**用 `LazyListState.requestScrollToItem` 预定位到底部（`SideEffect` 中调用，measure 前生效），首帧即落在最新消息；Material 与 Miuix 两套聊天页均已修复。
2. **版本号**：升至 **0.5.79 / versionCode 102**。

## v0.5.78 更新

> 说明：以下改动已完成真机回归（2026-11-15，测试机 / Android 17 / TIM 4.1.0.4050）：长按菜单 6 项、单条与多选转发（含置顶「某交流群」实测）、+1 复读（文本重发；非文本走同会话转发兜底）、Material/Miuix 双风格、独立 App 兜底提示均已实测通过。已实测。

1. **长按菜单移除「截图」项**：长按菜单精简为 +1 / 复制 / 转发 / 多选 / 引用 / 删除 6 项；「截图」按需求先移除，计划后续放到「多选」内部实现。
2. **单条转发与转发页完善**：长按菜单「转发」已接线——单条消息携带摘要进入转发页；转发页新增来源预览卡（「转发 N 条消息」+ 摘要首行）、宿主内目标会话实时刷新、加载中状态与空态（「没有可转发的会话」）、成功/失败 Toast；多选转发保持可用。本地乐观消息（尚无 TIM msgId）会提示「该消息暂不支持转发」。
3. **「+1」= 复读消息**：TIM 原生没有该能力（内核表情回应接口 `setMsgEmojiLikes` 在 TIM 上无权限返回 `-10122`，已放弃该路线），改为自实现复读——文本消息原样重发一条；图片 / 文件 / 语音 / 视频 / 聊天记录 / 红包等非文本消息尝试「转发到当前会话自身」兜底，失败 Toast；点击成功后本页气泡旁显示本地「+1」小标记（不落库，退出聊天页即失效）。
4. **转发内核修复（TIM 4.1.0）**：`forwardMsg` 第 4 个参数从旧版 `HashMap` 变为 `ArrayList<MsgAttributeInfo>`，旧实现会抛 `IllegalArgumentException` 导致转发必失败；已按真机探测到的新签名修复（普通转发传空属性列表）。
5. **版本号**：升至 **0.5.78 / versionCode 101**。

## v0.5.77 更新

> 说明：以下改动均已落地并完成真机回归（2026-11-15，测试机 / Android 17 / TIM 4.1.0.4050 + LSPosed）：实时消息推送、真实未读角标、真实群聊入口、独立 App 群公告详情与转发页均已实测通过。已实测。

1. **底栏未读角标使用真实数据**：宿主（TIM 进程）内订阅 `LiveChats.liveFlow` 汇总真实未读数；独立 App 仍用示例会话兜底；关闭「导航栏角标」开关后固定为 0。
2. **群聊入口页接入真实会话**：宿主内从真实最近会话中过滤 `isGroup` 展示，进入群聊页时若缓存为空会主动拉一次；独立 App 回落示例群。
3. **群公告详情的独立 App 兜底**：从群公告列表点进详情不再空白——独立 App 用示例会话合成公告；宿主内公告列表为空时用群资料里的公告兜底。
4. **转发页兜底与不可用提示**：转发页会话列表在无数据源 / 拉取失败时回落 `LiveChats.current`（独立 App 显示示例会话）；独立 App 点击转发会明确提示「当前为独立 App 预览，转发功能需在 TIM 宿主内使用」，不再静默无反应。
5. **独立 App 示例消息补稳定 msgId**：示例消息带稳定的 `chatRecordMsgId`，「多选 → 转发」的选择流程在独立 App 里可完整演示（真正发送仍需宿主内核）。
6. **实时消息推送（第 3 步新能力）**：`TimBridge` 在 `IKernelMsgListener` 上分发 `onRecvMsg` / `onMsgInfoListAdd` / `onAddSendMsg` / `onMsgInfoListUpdate` / `onContactUnreadCntUpdate` / `onMsgDelete` / `onMsgRecall` 等回调（注册时打印真实回调清单便于适配 TIM 小版本），整理成 `MessageEvent` 经 `LiveEvents` 发布——会话列表 400ms 去抖自动刷新（底栏角标随之更新）、当前聊天页自动重载新消息；监听注册失败按 10s 间隔重试（修复此前“先置已注册标志、失败后不再重试”的问题）。真机实测：TIM 前台运行时 130 个会话实时刷新、自己发送消息触发 `onAddSendMsg` 后会话列表摘要即时更新，无需手动下拉。
7. **工程基线**：仓库完成首个提交 `9c58363` 并打 tag `v0.5.76`；`.gitignore` 新增 `*.apk` / `*.aab` / `*.jks` / `*.keystore`；版本号升至 **0.5.77 / versionCode 100**。

## 第 1 步已完成：独立 Compose UI（功能清单）

> 以下 UI 能力在 Miuix / Material 3 Expressive 两套风格下均有实现（独立 App 的数据为示例数据）。

- **双 UI 风格**：`Miuix`（小米澎湃风）与 `Material 3 Expressive` 实时切换，全部页面均提供两套实现。
- **莫奈取色引擎**：基于 `material-kolor` 的 Material 3 动态配色、种子色、PaletteStyle、ColorSpec（2021/2025）与 AMOLED 纯黑。
- **导航栏系统**：三段式底栏（消息 / 联系人 / 我的）、未读角标、平板/横屏自动切 Wide Navigation Rail、可选 iOS 风悬浮底栏（含实时模糊与折射）。三种页面切换手势（默认 / 跨轴拦截 / iOS 风格）、横移返回、顶部栏/底栏模糊。
- **消息页**：聊天列表，参考 KernelSU 超级用户列表的卡片/分段列表形式；支持搜索、下拉刷新、置顶/群聊/未读/草稿/免打扰状态。
- **聊天详情页**：Material 3 Expressive 风格聊天气泡 —— 同一发送者的连续消息自动分组；单条为胶囊形，组内**只在头像/对齐那一侧的相邻角**收成 6dp 小圆角（我发的在右侧、对方的在左侧），另一侧始终保留胶囊大圆角，因此长度不同的气泡也能对齐（类似 M3E Segmented List 的分段做法）；**Miuix 风格下气泡不做分段圆角，任何情况都保持胶囊形**，以更符合 Miuix 的设计语言；组内头像/昵称/时间已读只在组首组末出现。时间分隔条、系统消息、左右气泡、常用消息类型（文本 / 图片 / 文件 / 语音波形 / 红包 / 链接 / 视频 / 聊天记录 / 灰条系统消息）。输入栏支持表情面板（M3E 下为表情 / 表情包两段 **Connected Button Group**；**Miuix 下改用 Miuix 原生 TabRow / TextField / Card 组件**；最近使用、96 个 emoji、3 列表情包卡片、搜索、最近/收藏/全部表情包底栏）与加号面板（**M3E：28dp 圆角 surfaceContainerLow 底部面板 + 4×3 横向大胶囊按钮（56dp 高，图标居中、文字在胶囊下方）+ 胶囊提示条**；**Miuix：输入栏下方两列圆角卡片（Card 与图标统一 12dp 圆角 + 彩色图标 + 标题），左右滑动分页、每页最多 3 行**（12 项 = 2 页），**底栏直接使用 Miuix `TabRowWithContour`（无额外 Card 容器，容器/选中胶囊配色对调且明暗自适应；常用/更多）切换页面**；旧实现保留在 `ChatAttachPanelMiuixCardList` / `ChatAttachPanelMiuixGrid` 可随时回退；两组统一使用 Material Rounded 图标族保证字形一致：相册/拍摄/语音通话/视频通话/文件/收藏/分享屏幕/红包/名片/位置/收钱/腾讯文档），可本地发送消息并自动滚动。
- **聊天信息页**：单聊/群资料卡、群成员宫格（群主标记）、群公告、置顶聊天 / 消息免打扰 / 强提醒开关、查找聊天记录 / 清空聊天记录 / 投诉 / 退出群聊·删除好友。
- **联系人页**：快捷入口 + 拼音首字母分组的联系人列表，支持搜索。
- **联系人详情页**：大头像资料卡、TIM 号/签名/在线状态、发消息（可直接进入聊天）、语音/视频通话、备注/手机号/邮箱/地区/分组/标签、删除好友。
- **新的朋友**：添加好友入口 + 好友申请列表（接受后变已添加，仅本地状态）。
- **群聊 / 标签 / 公众号**：群列表（成员数、可进入聊天）、标签分组（成员头像 chips）、公众号列表（未读角标、最新文章）。
- **我的页**：TIM 风格的资料卡与功能入口。
- **编辑资料**：头像、昵称、个性签名编辑，性别/生日/地区。
- **我的收藏 / 我的文件**：收藏分类筛选（文字/链接/图片/文件/语音），文件页含存储空间统计与按时间分组的文件列表。
- **钱包 / 服务**：余额卡片、服务宫格、账单/银行卡/支付设置/安全保障，服务页分组宫格 + 搜索。
- **设置页**：界面风格、主题设置、关于（已裁剪掉 KernelSU 的 root / 模块相关项）。
- **关于页**：版本、界面风格、开源致谢。
- **详细主题设置页**（从 KernelSU ColorPalette 页移植）：主题预览卡、种子色选择、深浅色/AMOLED、色彩风格/标准、模糊、悬浮底栏、角标、页面缩放手势等。

## 第 2 步已完成：Xposed 模块（接管 TIM 界面）

同一个 APK 现在既是独立 App，也是 **LSPosed 模块**（libxposed API 102）：

- 入口：`com.material3tim.xposed.MainModule`（`META-INF/xposed/java_init.list`）
- 作用域：`com.tencent.tim`（`module.prop` 声明 staticScope）
- 接管方式：hook TIM `SplashActivity` 的生命周期，在 **decorView 最上层**挂一个全屏 `ComposeView`，把它换成我们自写的 Compose UI；TIM 自己的 view 树保留在下面继续运转（不破坏宿主逻辑，避免 `findViewById(...).addView(...)` 之类 NPE）
- 模块资源：优先用隐藏 API `AssetManager.addAssetPath` 自建一套模块 Resources，失败时回退到 `ResourcesLoader` 注入宿主 Resources
- 宿主缺的运行时：TIM 的 Activity 不是 `LifecycleOwner`/`ViewModelStoreOwner`，模块自造 `HostOwners`（Lifecycle / ViewModelStore / SavedStateRegistry / OnBackPressedDispatcher / NavigationEventDispatcher）挂到 decorView 上
- 返回键：TIM 的返回走 `SplashActivity.doOnKeyDown()`，hook 后先交给 Compose 的 `NavigationBackHandler` 处理，处理不了再透传给 TIM
- 预测性返回：模块按设置里的「预测性返回手势」开关（`enable_predictive_back`）给 TIM 设置 `ApplicationInfo.setEnableOnBackInvokedCallback`，开启时注册 `OnBackAnimationCallback` 把手势进度接进导航层；切换开关在宿主内会提示「重启 TIM 后生效」（不会 recreate 黑屏）
- 设置：宿主内**读**模块 App 的远端偏好（`getRemotePreferences`），**写**落到 TIM 私有目录的 `material3tim_host`（进程内立即生效；跨进程写回留到后续版本）
- 外跳安全确认：TIM 的「即将离开 TIM」确认框只在**我们主动发起的**照片/拍照/文件选择器时自动放行，用户自己点外链仍会正常弹确认框
- 模块资源健壮性：模块 APK 路径短暂失效时依次回退（codeSource / classloader / PackageManager / /proc/self/maps），并把模块 APK 复制到宿主私有目录缓存（`files/m3tim_module_apk/module-<版本>.apk`）；连缓存都拿不到时自动重启 TIM 一次（带 90 秒防循环保护），避免 UI 文案退化成 `false`

在 测试机（Android 17、KernelSU + LSPosed v2.2.1）+ TIM 4.1.0.4050 上实测通过：聊天/联系人/我的三页、二级页面、返回键、设置页均正常，冷启动不崩溃。

**启用方法**：安装 APK → LSPosed 管理器里启用「Material3 TIM」模块（作用域 `com.tencent.tim`）→ 强制停止 TIM 再打开。停用模块即可恢复 TIM 原界面。

**当前边界**：只接管 TIM 主框架 `SplashActivity`（聊天详情等 TIM 内部 Activity 仍由 TIM 原生 UI 呈现）；作用域只有 TIM（未适配 QQ）。

## 第 3 步进行中：接入真实 TIM 数据

宿主进程里通过反射调用 TIM 的 NT Kernel API，把真实数据喂给自写 UI：

```
mqq.app.MobileQQ.sMobileQQ.peekAppRuntime()
    .getRuntimeService(IKernelService::class.java, "")
    ├─ getRecentContactService()  → 最近会话列表（同步 a(): List<RecentContactInfo>）
    ├─ getMsgService()            → 会话消息（getAioFirstViewLatestMsgs）+ 发消息（sendMsg）
    ├─ getBuddyService()          → 好友列表 / 联系人详情
    └─ IKernelMsgListener         → 消息推送 / 媒体下载完成等回调（v0.5.77 起用于实时刷新）
```

### 数据层与模式切换

| 数据 | 独立 App | 宿主（TIM 进程） |
|---|---|---|
| 会话列表 | 示例 18 条 | 真实最近会话（`LiveChats` + 实时推送自动刷新） |
| 消息 / 发送 | 示例消息，发送仅本地内存 | 真实消息（最近 40 条 + 向上分页），发送/引用/删除/转发/+1 走 TIM 内核 |
| 联系人 / 详情 | 示例 30 位 | 真实好友 + 真实详情（`LiveContacts`） |
| 本人资料 | 示例 | 真实昵称 / QQ / 头像（`LiveProfile`） |
| 聊天信息 / 群成员 / 群公告 | 示例 | 真实数据（`ChatSource`） |
| 实时推送 | 无 | v0.5.77 接入（`LiveEvents`） |
| 收藏 / 文件 / 钱包 / 服务 / 新的朋友 / 标签 / 公众号 / 表情包 | 示例 | 仍为示例 |

- 三个数据源接口 `ChatSource` / `MessageSource` / `ContactSource` 统一在 `data/source/AppDataSources.kt` 注册，只有宿主 `UiHost.install()` 会注册真实实现；独立 App 永远为 null，UI 自动回落示例数据。
- 真实数据缓存：`LiveChats` / `LiveContacts` / `LiveProfile`（内存 + `StateFlow`，进程退出即失）；实时事件：`LiveEvents`（`SharedFlow`）。
- 持久化只有 SharedPreferences（独立 App 的 `settings`；宿主内 `material3tim_host`，**单向**：读模块 App 远端偏好、写 TIM 本地，不写回模块 App）+ 群公告 JSON / 媒体临时缓存；没有自有数据库，消息不落库。

### 已完成的真实数据能力

> 下列能力在 v0.5.76 及之前已落地（历史真机验证环境为 测试机 + TIM 4.1.0.4050）；v0.5.77 新增项已于 2026-11-15 在同一台真机完成回归（实时推送 / 真实角标 / 真实群聊入口 / 独立 App 兜底）；v0.5.78 新增的转发补全（含 `forwardMsg` 参数修复）与 +1 复读已于同日完成真机回归；v0.5.79 的打开聊天首帧定位修复已用 24fps 录屏逐帧确认无顶部闪帧；v0.5.80 的多选点击拦截与整行选择已在真机 Material / Miuix 双风格验证；v0.5.81 的 Miuix 长按菜单全屏阴影与 M3E 表情面板导航栏沉浸已在真机确认；v0.5.82 的收藏表情页（M3E / Miuix）已于真机加载出用户真实收藏表情并验证；v0.5.83 的收藏表情倒序（最新在前）已在真机确认；v0.5.84 的收藏表情发送（M3E / Miuix，发出后按表情样式显示）已在真机验证；v0.5.85 的置顶/免打扰（单聊+群聊、重启持久化）与查找聊天记录（搜索+结果跳转定位）已在真机（测试机 与 测试平板）验证；v0.5.86 的消息页加号菜单（M3E / Miuix 各自原生弹出菜单）已在 测试平板验证；v0.5.87 的创建群聊（测试账号A + 测试账号C 实测建群成功）、QQ号/群号搜索与加好友请求、扫一扫拉起均已在同一平板验证；v0.5.88 的建群成员进群（三人群实测）、非好友 QQ 号搜索（群成员 测试账号C）、未加入群号搜索均在平板验证；v0.5.89 的纯昵称群搜索（某群）与手机号搜索（13800000000 → 测试账号A）也在同一平板验证。

**会话与实时消息**

- ✅ **消息页真实会话列表**：真实会话名/最后一条消息/时间/未读数/置顶/群标识（真机统计约 130 个会话）；首次启动时 TIM 内核可能还没准备好最近会话，带 8s 重试。
- ✅ **v0.5.77 实时消息推送**：`IKernelMsgListener` 的新消息 / 消息更新 / 删除 / 撤回回调经 `LiveEvents` 分发；会话列表 400ms 去抖自动刷新，底栏未读角标随之更新，当前聊天页自动重载新消息；监听注册失败按 10s 重试。
- ✅ **v0.5.77 未读角标真实数据**：宿主内底栏角标汇总真实会话未读数（独立 App 仍为示例数据）。
- ✅ **v0.5.77 群聊入口真实数据**：联系人页「群聊」入口从真实最近会话中过滤群聊；缓存为空时进入页面会主动刷新一次。
- ✅ **聊天详情真实消息**：拉取最近 40 条，映射文本/图片/语音/文件/表情/视频/聊天记录，群聊显示发送者昵称，按 5 分钟间隔插入时间分隔条。
- ✅ **发送文本消息**：`sendMsg` 桥已实现（反射构造 TextElement/MsgElement + IOperateCallback），发送成功后回读消息并刷新会话列表。
- ✅ **加载更早消息（向上翻历史）**：
  - 数据层：`MsgService.getMsgsBySeqAndCount(contact, seq, 40, asc=true)`，以当前最早消息的 seq 为游标向上分页；`TimBridge.olderStates` 按会话缓存游标与已加载历史
  - UI：聊天页消息列表改为普通布局（进入自动到底部、新消息在底部时跟随），M3E 用 `PullToRefreshBox`、Miuix 用 `PullToRefresh`（四段文案「下拉加载更早消息/松开加载/正在加载/已加载」），滑到顶部下拉即加载一页更早消息；没有更早消息时 Toast「没有更早的消息了」
  - 加载完成后按「第一条可见消息 id」恢复视口；时间分隔条按 `timestampMs` 跨批次重算；刷新进行中再次下拉会被忽略

**聊天详情、消息操作与交互**

- ✅ **引用（回复）支持**：消息带 `ReplyElement`（elementType=7）时气泡上方展示「回复 昵称 + 原文摘要」；左滑整行触发引用（回弹曲线按 UI 模式选择 Miuix `folmeSpring` / M3E `motionScheme`）；发送时用 TIM `IMsgUtilApi.createReplyElement(...)` + TextElement 组合发送；被引用消息不在当前页时按 msgId/seq 从内核补拉预览。
- ✅ **点击引用条跳转原消息**：已加载则直接滚动定位；不在当前页则**静默**逐页向上取历史（不触发下拉刷新动画），优先按引用 seq 直接取目标附近 80 条，带 8 秒超时；支持 msgId 与 seq 两种定位。
- ✅ **转发与多选**：长按菜单「多选」进入多选（自己消息 checkbox 在整行最左侧、对方消息在整行最右侧）；聊天页多选顶栏可退出 + 显示已选数量 + 转发；转发页与主页消息列表同构，可一次转发到多个会话，转发走 TIM 内核 `forwardMsg`。
  - v0.5.78 补全：长按菜单「转发」可直接转发单条消息（携带摘要进入转发页，顶部预览卡显示「转发 N 条消息」+ 摘要首行）；转发页支持目标会话实时刷新（宿主订阅 `LiveChats.liveFlow`）、loading、空态提示与成功/失败 Toast；本地乐观消息（尚无 TIM msgId）会提示「该消息暂不支持转发」。
- ✅ **v0.5.133 群文件**：聊天信息→群文件（M3E+Miuix），根目录文件+文件夹列表/子目录/分页/刷新，下载（进度+打开+缓存回填）、上传（UFT 0x6d6_0+HTTP 分片）、新建文件夹/重命名/移动/删除（管理员）。协议改走旧版 OIDB（NT kernel 未注册群文件 API）；真机验证。
- ✅ **v0.5.134 群文件优化**：Toast 文案修复、操作后自动刷新、进入自动加载、M3E 分段列表（文件夹/文件两段）；真机验证。
- ✅ **v0.5.135 通知/群管理**：系统通知+点击直达聊天（已验证）；群名/群名片/全员禁言/解除禁言可用，群管理 UI（成员禁言/管理员/踢人/公告/退群/解散）已接入（管理员设置与成员禁言内核权限问题待修）；真机验证。
- ✅ **v0.5.132 红包消息适配**：WalletElement 解析 + 暖色红包卡片（标题/副标题/状态），未领取点击走 TIM 原生 `GrabHbViewModel` 弹出「開」领取弹窗，已领取点击打开原生红包详情页；真机实名账号实测领取 0.01 成功。
- ✅ **v0.5.131 ARK 点击走应用内浏览器**：复用聊天气泡链接的 `ExternalLink`（Chrome Custom Tabs / TIM 内置浏览器 + `expectExternalLaunch` 放行），不再弹「即将离开 TIM」确认。
- ✅ **v0.5.130 ARK 卡片适配**：解析 ArkElement JSON（小程序/结构化卡片），M3E+Miuix 卡片气泡（预览图/来源/标题/页脚），点击打开链接；B 站小程序卡片实测显示与跳转正常。
- ✅ **v0.5.129 视频消息**：相册图片/视频混合多选；接收视频缩略图不再黑（TIM 缓存路径/缩略图参数修复）；发送视频走 TIM 原生 IMsgSendHelper，群内实测持久、电脑 QQ 可见、可播放。
- ✅ **v0.5.128 聊天页横滑返回手势**：消息行只消费左滑（引用），右滑不消费交给 NavDisplay predictive back；返回手势开始后左拉只拖动返回页面、不触发引用穿透。
- ✅ **v0.5.127 输入框按行数固定高度**：`onTextLayout.lineCount` 驱动高度，1/2/3/4 行各自固定；中英文/空态高度完全一致（用户真机验证通过）。
- ✅ **v0.5.126 输入栏高度/宽度稳定**：麦克风/发送固定在右侧占位槽，输入框宽度恒定；统一 lineHeight + minHeight，空态与有文本高度一致。
- ✅ **v0.5.125 M3E @ 菜单搜索框**：换成消息列表同款胶囊搜索框，点击即弹输入法、实时过滤；修复原 SearchBar 在 Dialog 内无法唤起输入法的问题。
- ✅ **v0.5.124 @ 菜单标题行移除 + Miuix 搜索框同卡片底色**：两套 UI 都去掉顶部「选择提醒的人 ×」；Miuix `InputField` 背景改用 `secondaryContainer`，与成员卡片一致。
- ✅ **v0.5.123 Miuix @ 菜单卡片配色**：卡片改用 `secondaryContainer`（对齐 Miuix 官方 BottomSheet 示例），与 Sheet 背景形成清晰层级，dark/AMOLED 下也明显更亮。
- ✅ **v0.5.122 缩短 @ Sheet 弹出间隔**：IME 收起后下一帧立即挂载标准 Sheet Dialog，去掉 180ms 额外延迟，键盘收起后 Sheet 快速滑入。
- ✅ **v0.5.121 M3E 输入 @ 键盘闪动修复**：等 IME 完全收起 + 180ms 稳定后再挂载标准 Sheet Dialog，消除打开瞬间的双重残影/闪帧。
- ✅ **v0.5.120 输入 @ 自动收起键盘**：M3E 修复键盘闪动、Miuix 修复键盘/焦点不关闭；保留输入内容与焦点，面板正常唤起。
- ✅ **v0.5.119 M3E @ Sheet 回归标准 ModalBottomSheet + Dialog 预测性返回**：标准组件外观；`OnBackAnimationCallback` 驱动跟手下滑/回弹/关闭；背景延伸到导航栏、按钮避开手势条；修复已有 `@` 时不能再唤起面板。
- ✅ **v0.5.118 @ Sheet 对齐联系人页 + M3E 预测性返回/平板限宽**：联系人同款搜索框与分组列表；M3E 无 checkbox（选中=圆角+背景色）、Miuix 保留 checkbox；M3E 返回手势可关、预测性返回跟手；平板 640dp 居中两侧露背景。
- ✅ **v0.5.117 @ 改底部 Sheet**：搜索框 + A/B/C 分组列表 + 默认多选；确定一次性插入多个 @（真机 ats=2 验证），双风格各自 Sheet 实现。
- ✅ **v0.5.116 多元素消息不再截断**：文本+表情+图片同条消息按元素顺序解析为 parts，在同一气泡内混排文本与图片（双风格），真机 TEXT/IMAGE 两种顺序验证。
- ✅ **v0.5.115 群聊 @ 输入 + 管理员撤回他人消息**：@ 成员/全体选择面板（双风格）、真 at 元素发送；群主/管理员可撤回他人 2 分钟内消息。
- ✅ **v0.5.114 修复键盘弹出空白**：TIM 窗口 adjustResize 与 Compose imePadding 双叠加 → 改为 ADJUST_NOTHING，只由 ime insets 动画，输入框与键盘同步升起。
- ✅ **v0.5.113 语音暂停保留位置**：暂停停在当前位置、再点从原位置继续；解码器 pause/start + stop/A 兜底，真机 1140ms 位置验证。
- ✅ **v0.5.112 下载真实进度 + 语音气泡重做**：内核 richmedia 进度回调驱动真实百分比；语音气泡「图标+常驻可拖进度条+时长」、拖动 seek、M3E 大波浪。
- ✅ **v0.5.111 气泡内进度条**：文件下载进度（M3E 波浪 / Miuix 标准）+ 语音播放进度，颜色随气泡内容色，真机双风格验证。
- ✅ **v0.5.110 文件消息下载/打开**：文件气泡状态图标 + 点击下载（内核 RichMedia）后自动用 TIM FileProvider content URI 交给系统应用打开；真机 11.8MB 视频验证通过。
- ✅ **v0.5.109 语音完整可用**：时长按秒修复、语音文件自动下载、播放改走 TIM CommonAudioPlayer（silk/amr），播放中 Pause/播完自动恢复，真机 dumpsys 确认出声。
- ✅ **v0.5.108 语音播放 + 消息撤回**：语音条点击播放/暂停（单条、离开即停），双风格波形高亮；长按自己的消息 2 分钟内可「撤回」（内核 recallMsg，wrapper/native 双兜底，成功后灰条+列表同步）。
- ✅ **v0.5.107 动图支持 + 平板导航栏沉浸修复**：GIF/动态 WebP 在表情气泡与预览页无限循环播放；UiHost 重新应用 edge-to-edge（layout/insets/resume 节流重试），平板横竖屏、M3E/Miuix 导航栏恢复沉浸（liquid glass 底栏下透明）。
- ✅ **v0.5.106 修复 @ 正文丢失**：内核把「@某人 + 正文」拆成多个 TextElement，现合并连续文本/表情元素，引用图片消息也能完整显示 `@测试账号B aaa`。
- ✅ **v0.5.105 进入聊天标记已读**：打开会话页即内核置已读并刷新列表角标；停留期间新消息也同步置已读，退出不再残留 99+。
- ✅ **v0.5.104 聊天卡片提醒标签**：`[有人@我]`/`[@全体成员]`/`[有新文件]`/`[群公告]` 等红色前缀（未读时显示，草稿优先），M3E+Miuix 双风格；数据来自内核 recent 快照（atType + 事件类型 + specialCareFlag）。
- ✅ **v0.5.103 修复群免打扰判定**：GroupMsgMask 语义纠正（NOTIFY=接收并提醒、RECEIVE=接收但不提醒）+ 改用内核 `cmdUinMsgMask`（68/76 群免打扰，与 TIM 一致）；修正聊天信息页免打扰开关写入值。
- ✅ **v0.5.102 会话列表增强**：未读角标区分免打扰（淡色）；长按会话卡片弹出三项菜单（置顶/标未读已读/删除+确认），M3E 与 Miuix 双风格真机验证通过。
- ✅ **v0.5.101 扫一扫卡片置底 + Miuix 菜单沉浸修复**：两套 UI 操作卡片移到底部；Miuix 首页加号菜单改为页面级全屏遮罩 + 弹窗系统栏透明，状态栏一起压暗（与长按菜单同款方案）。
- ✅ **v0.5.100 扫一扫按钮卡片化**：两套 UI 三按钮同卡片；M3E 选择图片=打开相机同色、取消改 tonal 按钮；Miuix 打开相机与选择图片同色。
- ✅ **v0.5.99 扫一扫三按钮 + 相册单选 + 聊天多选**：默认操作页（打开相机/选择图片/取消）；相册兼容识别；聊天照片最多选 9 张逐张发送、后台 IO 处理。真机验证 b站链接/QQ 名片码/多图发送均正常。
- ✅ **v0.5.98 扫一扫**：加号菜单扫一扫 → 系统相机拍照 → ZXing 识别二维码 → 链接走 Custom Tabs / QQ 功能码走 TIM JumpAction / 其它走 TIM 扫码分发；支持重拍与复制结果。
- ✅ **v0.5.97 验证页按目标设置自适应**：允许任何人隐藏输入/需要验证信息显示消息框/需要回答问题显示问题+答案输入；测试账号B（问答设置）真机发送成功，对方收到申请与回答。
- ✅ **v0.5.96 加好友/搜索实现收尾清理**：净 −573 行（删死代码/合并 searchFacade/日志降噪/未用资源与接口），行为不变；测试机 冒烟：搜索结果/验证页/昵称搜群分页正常。
- ✅ **v0.5.95 需验证信息加好友真机跑通**：先查 friend_setting=1 再带验证消息发送，测试机 上 测试账号A→测试账号B `NT addBuddy result=0`，验证消息真实送出；补 safety/tag 预检与 sig 重试。
- ✅ **v0.5.94 QQ号搜索占位/手机号解析尝试**：纯数字无结果时显示「QQ用户」占位可加；requestInfoByAccount 被 TIM 产品拒绝（-10122）；当前双账号均触发 168 风控需冷却验证。
- ✅ **v0.5.93 好友设置查询 + 错误文案透传**：发送前按 TIM 原生流程查 friend_setting（需要验证→1），带验证消息发送；失败 Toast 显示服务端文案；当前测试账号触发 168 功能锁需等解除后复测。
- ✅ **v0.5.92 验证消息/原生页修复**：加好友改走 TIM 原生 NT addBuddy（msg→AddBuddyReq.answer），验证消息真正送达；手机号/QQ号发送失败不再弹 TIM 原生页（只提示失败）。真机两入口 result=0。
- ✅ **v0.5.91 加好友验证流程**：搜索结果 → 加好友验证页（验证消息）→ 内核 reqToAddFriends 真实发送；手机号目标先解析出 QQ 号再发；QQ号/手机号两条路径真机均 code=0，测试账号已收到请求。
- ✅ **v0.5.90 昵称搜群分页**：群名关键词按页拉取远端群（每页约 4 条，最多 25 页去重）；搜索解析按记录内容判断群/人。注：原版 TIM「找群」走 findtroop 协议（每页约 15 条），完整结果量对齐待后续版本。
- ✅ **v0.5.89 昵称搜群 / 手机号搜索**：群名关键词走 SummaryCard 群搜索（本地+远端群一起显示）；手机号走 SummaryCard 找人（真机 13800000000 搜到 测试账号A），手机号结果点击回落 TIM 原生加好友页。
- ✅ **v0.5.88 建群/搜索修复**：创建群聊改 TIM 建群+邀请成员原链路（三人群实测成员进群）；加好友/群搜索改用 SummaryCard.ReqSearch，非好友 QQ 号与未加入群号可搜到；过滤内核空壳资料造成的假个人卡片。
- ✅ **v0.5.87 加号菜单三项功能**：创建群聊（联系人多选 → 内核建群 → 进入新群聊天）；加好友/群（QQ号/手机号/群号搜索，个人/群聊两块卡片 + section header，点击发请求/申请）；扫一扫（TIM 原生扫码页）。
- ✅ **v0.5.86 消息页加号菜单**：右上角「+」弹出 创建群聊 / 加好友/群 / 扫一扫；M3E 复用设置页「色彩风格」的 Expressive 下拉组件，Miuix 使用原生 `WindowCascadingListPopup`（功能待接入）。
- ✅ **v0.5.85 聊天信息页功能**：置顶聊天（内核 `setContactListTop`）、消息免打扰（单聊 `setMsgNotify` / 群聊 `setGroupMsgMask`）真机可用且重启持久化；删除「强提醒」；新增**查找聊天记录**页（内核搜索 + 结果点击跳转定位，M3E / Miuix 双风格）。
- ✅ **v0.5.84 收藏表情发送**：点击收藏页表情以 `picSubType=1` 的表情形态发送到当前会话；消息列表按表情尺寸渲染（M3E / Miuix 双风格）。
- ✅ **v0.5.83 收藏表情排序**：收藏页按添加时间倒序展示（最后添加的排最前）。
- ✅ **v0.5.82 收藏表情页**：表情面板「表情包」底部新增「收藏」页，展示用户在 TIM 里「添加到表情」收集的真实收藏表情图片（`IFavroamingDBManagerService.getEmoticonDataList()` + 本机缓存图，M3E / Miuix 双风格）；按需求删除「最近」入口。
- ✅ **v0.5.81 系统栏沉浸修复**：Miuix 长按菜单改为页面级全屏压暗层（覆盖状态栏/导航栏）+ 弹窗 Dialog window 状态栏透明，状态栏跟随阴影压暗；M3E 表情/附件面板展开时面板底色延伸进系统导航栏区域，底部栏与小白条区域无接缝。
- ✅ **v0.5.80 多选点击与整行选择**：多选模式下点击图片/视频/链接/聊天记录/引用条等任意气泡只切换选中，不再触发子组件点击；点击消息行任意位置（气泡/头像/时间/空白）均可选中或取消，拖动仍可滚动列表；多选时禁用长按菜单与左滑引用。Material / Miuix 两套实现同步。
- ✅ **v0.5.79 打开聊天首帧定位**：进入聊天页时在布局前 `requestScrollToItem` 预定位到底部，不再出现「首帧列表顶部 → 下一帧跳到底部」的闪屏；Material / Miuix 两套聊天页同步修复。
- ✅ **v0.5.78「+1」复读**：长按菜单「+1」把消息内容复读一条——文本消息原样重发；图片/文件/语音/视频/聊天记录/红包等非文本消息走「转发到当前会话自身」兜底；失败 Toast。点击成功后本页气泡旁显示本地「+1」小标记（不落库，退出聊天页即失效）。TIM 内核表情回应接口在 TIM 上无权限（`-10122`），因此未采用原生表情回应。
- ✅ **删除消息**：长按菜单「删除」调用内核 `deleteMsg`，删除后回读消息；失败（超时/无权限）有 Toast 提示。
- ✅ **发送后“双气泡”修复**：乐观本地消息与内核刷新回来的真实消息按内容 + 发送者 + 时间窗口去重。
- ✅ **所有类型气泡都能长按弹出操作菜单**（文字/图片/视频/文件/语音/红包/链接/聊天记录），长按检测在 Initial pass 处理，不会被图片/视频/聊天记录自身的 clickable 吃掉；长按有触感振动。
- ✅ **长按菜单**：Miuix 用 `WindowListPopup` + `DropdownImpl` 官方行（自己的消息从右弹出、别人的从左弹出，带全屏压暗遮罩）；Material 用设置页同款 M3E Expressive Dropdown（`OffsetAnchoredExpressiveMenu` + `SelectableDropdownMenuItem`），以气泡为锚点、长按位置即弹出位置。
  - 已实现：+1（复读）、复制、转发（单条）、多选、引用、删除；长按菜单动作已全部接线，不再有占位项。
- ✅ **修复 Material 3 Expressive 下自己发送的短文本气泡被左对齐**（v0.5.75）：根因是 `OffsetAnchoredExpressiveMenu` 锚点 Box 把父级撑到 284dp，修复为锚点 Box 报 0×0 + Popup 锚点取被 place 的子节点坐标；实测短气泡与长文本气泡右边缘一致（都贴住头像）。
- ✅ **气泡内链接可点击**：文本气泡里的 `http(s)://` / `www.` 链接自动变成可点链接；非 QQ 域名 → Chrome Custom Tabs（跳过 TIM「即将离开 TIM」确认框），QQ/腾讯自家域名 → TIM 自带 QQBrowserActivity；群公告详情页的链接同样可点。

**媒体与富消息**

- ✅ **图片消息真实显示**：历史图片的 PicElement 本地路径为空时用 `MsgService.getRichMediaElement(...)` 触发内核下载，注册 `IKernelMsgListener.onRichMediaDownloadComplete` 捕获 `chatthumb/chatraw` 本地路径映射回气泡；下载完成后消息页轮询自动刷新（最大约 10～14 秒）。
- ✅ **图片清晰度**：气泡优先用 TIM 本地缓存的**原图/720 大图**（`AIOPicDownloader.m` 按 ORI > BIG_THUMB > HD_THUMB > 198_THUMB 顺序查找）；图片详情页按原图解码，本地缓存 key 带解码尺寸避免低清结果顶掉高清。
- ✅ **图片详情页**（`Route.ChatImage`）：点击图片气泡进入全屏页，M3E `ExpressiveScaffold + TopAppBar` 样式；单击显隐顶栏（3 秒自动隐藏）、双指缩放/拖动、双击放大/还原、右上角保存到相册（Pictures/Material3Tim）；手势 `pointerInput` 放在 `graphicsLayer` 外层保证 1:1 跟手。
- ✅ **视频消息适配**：气泡为 16:9 缩略图 + 居中播放按钮 + 右下角时长；点击进入「视频」页，优先播放本地文件，未下载时用 `RichMediaService.getVideoPlayUrl(...)` 取在线地址；播放页顶栏与控制条显隐同步、3 秒自动隐藏、全屏时顶栏隐藏，进度条吸底、可拖动。
  - 播放器回退说明：尝试过 Media3 ExoPlayer，但 TIM 进程内播放 H.265(t26) 会触发 native 崩溃，已回退系统 VideoView。
- ✅ **聊天记录（合并转发）适配**：气泡预览与原版一致（标题「群聊的聊天记录」+ 前 3 条预览 + 「查看 N 条消息」，预览来自 `MultiForwardMsgElement.xmlContent`）；点击进入独立「聊天记录」页（`Route.ChatRecord`），复用聊天页气泡渲染；内容通过内核 `MsgService.getMultiMsg(contact, 0, msgId, callback)` 拉取。
- ✅ **语音 / 文件元数据**：语音时长、文件大小、本地路径已带在模型里；语音波形、文件气泡可展示（播放/打开未接入，见「功能占位与待接入」）。
- ✅ **灰条系统消息**（elementType=8）：群主撤回、成员撤回、加退群等提示解析为纯文本，渲染成居中的系统消息，不再显示成带头像的「[消息]」气泡；XML 灰条等未知类型兜底为 `[系统消息]`。

**联系人与资料**

- ✅ **联系人真实列表**：`BuddyService.getBuddyListFromCache("", KNOMAL)` + `getBuddyNick` / `getBuddyRemark`，真机统计约 101 位好友，按首字母分组（非 ASCII 归到 #），头像用 legacy 头像缓存。
- ✅ **真实联系人详情**：`ProfileService.getCoreAndBaseInfo` 取昵称/备注/QQ 号 + 真实头像；「发消息」可进入该好友的真实 C2C 会话（已有最近会话则复用，没有则合成并缓存 Contact）；补充真实邮箱/手机号/个性签名。
- ✅ **真实「我的」资料**：昵称 / QQ 号 / 头像（`getCurrentNickname` / `getCurrentUin`）。
- ✅ **真实头像（含 C2C）**：通过 `ProfileService.getUinByUid` 把 uid 映射成 uin，再用头像缓存取图；TIM 还有一套 legacy 头像磁盘缓存 `Tencent/Tim/head/_hd/<md5(md5(md5(uin)+uin)+uin)>.jpg_`（全大写）作为兜底；聊天列表、消息发送者头像、我的头像都已使用真实图片（无缓存时回落首字渐变）。
- ✅ **头像覆盖率修复**：最近会话头像 128/130（C2C 49/51、群 79/79）、好友头像 98/101、聊天页发送者头像全部真实（v0.5.76 前后真机统计）。
- ✅ **聊天页缺失头像自动下载**：对没有本地缓存的群成员/好友调用 TIM `FaceDecoder.requestDecodeFace(uin, 1, false)` 走 TIM 头像下载管线（顺带保留内核 `forceDownloadAvatar` 兜底），下载完成后聊天页轮询自动替换成真实头像，缓存持久化。

**聊天信息 / 群公告 / 聊天记录**

- ✅ **聊天信息页接真实数据**：
  - 群资料：群头像、群号、群简介（`getGroupDetailInfo` + `batchQueryCachedGroupDetailInfo`）
  - 群简介 / 群公告分字段显示：群简介 = `GroupDetailInfo.fingerMemo`（fallback `richFingerMemo`）；群公告 = `GroupDetailInfo.groupMemo`，信息页卡片显示前 3 行，点击进入独立「群公告」页
  - 群头像兜底：会话列表 avatarPath 为空时重新解析并必要时 `forceDownloadGroupAvatar` 强制下载后刷新
  - 群成员：`getAllMemberListV2` 拉取真实成员列表（群主/管理员/我 标记、真实头像、QQ 号）
  - 「查看全部成员」独立页面（`Route.ChatMembers`），A/B/C… 首字母分组 + 分段列表（M3E `SegmentedItem` / Miuix Card），忽略大小写排序
  - 成员页顶部搜索栏（M3E `SearchAppBar` / Miuix 展开态 `SearchBar`），按名称或 QQ 号实时过滤
  - 单聊资料：对方头像、QQ 号、个性签名（`getCoreAndBaseInfo`）
- ✅ **群公告页升级为列表 + 详情**：
  - 最新群公告全文走 TIM 自己的 SSO 通道：`TroopAnnouncementHandler.S()` → `OidbSvc.0x8f9_14` → 回写 `TroopInfo.latestMemo`
  - 全部群公告列表走 TIM 的 `OidbSvc.0x852_48`（群公告列表流），自动触发群公告鉴权并按逆向出的私有流格式解析全部公告 feeds
  - H5 通道：`POST https://web.qun.qq.com/cgi-bin/announce/list_announce` 分页拉取（`SwiftBrowserCookieMonster.r(url)` 拿完整 H5 cookie、bkn = `TroopUtils.h(skey)`、`s=<next_index>` 分页、HTML 实体解码）；实测一次拉回 19 条历史公告（官方页面显示 18 条），正文完整不截断
  - 远端（H5/SSO）成功时**以远端为准**，不混入本地旧缓存；本地缓存升级为 v2 格式，旧 v1（截断数据）首次加载自动删除
  - 列表页（`Route.ChatNotice`）展示多条公告卡片（标题/时间/完整正文），点击进入详情页（`Route.ChatNoticeDetail`）展示**完整正文不截断**、可复制
  - 兜底：TIM `TroopNotificationCache` 公告 feeds 解析失败时扫描群聊历史里的公告卡片 + 群资料当前公告；看过的公告累积到 `filesDir/m3tim_notices/<群号>.json` 长期保留
  - v0.5.77：独立 App 的群公告详情用示例数据兜底，不再空白
- ✅ **聊天记录（合并转发）页面**：见「媒体与富消息」。

**模块与工程健壮性**

- ✅ **Compose lifecycle 修复**：TIM 切后台再回前台后，Compose 的 lifecycle owner 未恢复 RESUMED，导致帧时钟暂停（点击不重组、列表 fling 没惯性、预测性返回动画卡住）；现在 `onResume` 会恢复 lifecycle，并在 pause/stop 时取消未完成的返回手势。
- ✅ **模块化后莫奈取色修复**：宿主里 `LocalContext` 是模块 Context（只有模块 APK 资源），`dynamicLightColorScheme()` 读不到系统壁纸强调色；现在通过 `LocalMonetContext` 传入宿主 Activity，并直接读 `android.R.color.system_accent1_200` 作为种子交给 material-kolor（M3E 与 Miuix 共用）。
- ✅ **Miuix 聊天页加号面板底栏（TabRowWithContour）修复**：深色模式容器/选中胶囊三层可见层次；关闭「长按快捷方式可拖动排序」提示后卡片与 TabRow 保持 0 间距。
- ✅ **v0.5.77 工程基线**：首个 git 提交 `9c58363` + tag `v0.5.76`；`.gitignore` 忽略 APK/AAB/签名文件；版本号 0.5.77 / versionCode 100。

### 当前未完成 / 待做事项

- ⏳ **系统通知栏消息与已读回执**：新消息实时刷新已接入，但系统通知栏推送、已读/未读回执与多端已读同步未接入（已读回执短期内不做）。
- ⏳ **部分二级页真实数据**：我的收藏 / 我的文件 / 钱包 / 服务 / 新的朋友 / 标签 / 公众号 / 表情包仍为示例数据（宿主内未接真实数据源）。
- ⏳ **附件面板剩余项**：语音通话、视频通话、收藏、分享屏幕、红包、名片、位置、收钱、腾讯文档等入口仍是占位（照片 / 拍照 / 文件已接系统选择器）。
- ⏳ **贴纸 / 表情发送**：MarketFace 真实贴纸发送部分场景、APNG / 魔法表情发送未完成；表情包点击目前是 Toast 占位。
- ⏳ **长按菜单「截图」**：入口已按需求移除，计划后续放入「多选」内部实现。
- ⏳ **编辑资料保存**：头像 / 昵称 / 个性签名编辑目前仅本地状态，未写回 TIM。
- ⏳ **系统消息兜底**：XML 灰条等未知类型显示为 `[系统消息]`；未知消息元素显示为 `[消息]`。
- ⏳ **宽屏侧边栏风格**：example Layout 的 Sidebar Style 未实现（手机不显示该行，平板/宽屏暂用现有 rail）。
- ⏳ **通话页**：仍使用 TIM 原生通话 UI，未自写。
- ⏳ **工程化**：单元测试、CI、lint 门禁、可复现构建、死代码/未引用字符串清理未做。

### 已知限制（按形态区分）

**独立 App**

- 会话 / 消息 / 联系人 / 资料为示例数据；收藏、文件、钱包、服务、新的朋友、标签、公众号、表情包也是示例数据。
- 发送消息只写本地内存（不落库、退出即失）；转发与 +1 需在 TIM 宿主内使用（有明确 Toast 提示）。
- 示例消息没有真实媒体文件，图片/语音/视频无法真正查看或播放。

**宿主（TIM 进程）**

- 群聊入口页只覆盖「最近会话中的群」，不在最近会话列表里的群不会出现。
- 图片/视频详情页为单一 M3E 风格，不随 Miuix / Material 切换。
- AMOLED 纯黑仅在 Material 模式下生效（Miuix 深色回落到 MonetDark）。
- 实时模糊与悬浮底栏为 Miuix 专属；Material 模式使用标准底栏 / Wide Navigation Rail。
- 设置跨进程不写回：宿主内修改不会写回模块 App，TIM 重启后以模块 App 设置为准。
- 反射桥与 TIM 版本强绑定：当前基于 TIM 4.1.0.4050 验证，TIM 升级可能失效（未知元素/回调会降级为日志 + 兜底）。
- 删除/撤回等操作受 TIM 权限与消息时效限制。
- 「+1」为自实现的复读：文本消息会原样重发一条相同内容（不是 TIM 原生表情回应，会话里会出现两条相同文本）；图片/文件/语音/视频/聊天记录/红包等非文本消息依赖「转发到当前会话自身」兜底，TIM 内核拒绝时 +1 失败。
- 本地「+1」标记只存在于当前聊天页内存中，退出页面或重启后消失，不代表原消息的真实状态。
- 单条转发依赖消息已有 TIM msgId：自己刚发送、内核尚未回读的本地乐观消息会提示「该消息暂不支持转发」。
- 长按菜单「截图」项已移除（计划后续放入多选内部实现）；当前需要实测请使用系统截屏。

**通用**

- 仍有多处占位入口会弹出「该功能将在后续版本实现」（主要见「当前未完成 / 待做事项」）。
- 系统通知栏消息与已读回执未接入；语音播放/文件打开/红包领取已接入。

### 清理项（代码/文档待整理）

- 表情面板 GIF Tab 已按 TIM 实际入口删除，但 `GifPlaceholderMiuix` 与字符串 `chat_v2_gif_placeholder` 仍是无调用方的死代码，待清理。
- `res/values/strings*.xml` 中约 42 条字符串未被引用（UI 迭代遗留），待清理。
- UI 层仍有少量硬编码中文文案（如「已复制」「无法定位原消息」），后续可统一迁入 `strings.xml`。
- 根目录历史 APK 不再入库（`.gitignore` 已忽略 `*.apk`），Release 通过 Releases / 网盘分发。

## 安装

```bash
# 构建产物（当前 versionName 0.1.0 / versionCode 1）
adb install -r app/build/outputs/apk/release/app-release.apk
adb -s <DEVICE_SERIAL> install -r app/build/outputs/apk/release/app-release.apk   # 指定设备时
```

- 包名 `io.github.linx3141.material3tim`；Debug 包带 `.debug` 后缀（applicationId 与 versionName 均带 debug 后缀），可与 Release 共存。
- minSdk 26 / targetSdk 37；需要 Android 14+ 设备做宿主测试（LSPosed 环境）。
- 分发给他人时建议把产物重命名为 `Material3Tim-v0.1.0.apk`；根目录 APK 与构建产物不入库（`.gitignore` 已忽略 `*.apk` / `*.aab`），Release 通过 GitHub Releases 分发。
- 历史版本与提交记录见 git tag（`v0.5.76` 起）；不在仓库内保存截图/聊天数据。

**启用方法（LSPosed）**：安装 APK → LSPosed 管理器里启用「Material3 TIM」模块（作用域 `com.tencent.tim`）→ 强制停止 TIM 再打开。停用模块即可恢复 TIM 原界面。

**NPatch 集成模式（`--embed`）**：NPatch 会把模块 APK 解包到宿主
`cache/code_cache/mods/io.github.linx3141.material3tim/`，`ModuleResources`
用该路径构建**独立的模块 Resources**（模块 APK + framework-res），不再把
模块资源注入宿主；设置写入 TIM 本地的 `material3tim_host`，不依赖管理器
的远端偏好服务。可直接与 QAuxv 一起内嵌：

```bat
java -jar npatch.jar -m Material3Tim.apk -m QAuxv.apk -o out TIM.apk
```

**与 QAuxv 共存**：Material3TIM 只覆盖 `SplashActivity`，不注入宿主
Resources，因此不会和 QAuxv 的 `Parasitics` 抢 0x7f 资源 ID。需要改 QAuxv
设置时，在 Material3TIM「设置 → 打开 TIM 原生设置」进入 TIM 原生设置页，
QAuxv 的入口就在那里；返回后自动回到 Material3TIM。

## 技术栈

- Kotlin **2.4.21** / AGP **9.4.1** / Gradle **9.7.1** / JDK **21**
- Compose BOM **2026.11.01**（Compose 1.13）+ Material3 **BOM 管理（1.5.0）**
- [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix) **0.9.5-beta03**（`miuix-ui` / `icons` / `nav` / `preference` / `blur` / `glass`）
- [material-kolor](https://github.com/jordond/materialkolor) **6.0.0**（`material-kolor-core` + `material-kolor-material3`；已适配 material-color-utilities 6.0.0）
- miuix-nav（类型安全导航 + 转场）、libxposed API 102（`compileOnly`，运行时由 LSPosed 注入）
- minSdk 26 / targetSdk 37 / compileSdk 37.2（build-tools 37.0.0）
- 当前版本 **v0.1.0 / versionCode 1**（首个公开版本；后续非用户要求不自动 bump）

## 目录结构（主要部分）

```
app/src/main/
├── AndroidManifest.xml              # Application / MainActivity / Xposed 模块声明
├── resources/META-INF/xposed/       # java_init.list / module.prop / scope.list（LSPosed 模块）
├── res/                             # 很薄的资源层：strings / themes / colors / launcher 图标
└── java/com/material3tim/
    ├── M3TimApplication.kt          # Application（预测性返回开关等）
    ├── AppEnvironment.kt            # 全局 Context 抽象（独立 App / 宿主模块 Context 包装）
    ├── data/
    │   ├── model/
    │   │   ├── Chat.kt              # ChatItem / ContactItem / UserProfile
    │   │   ├── Message.kt           # ChatMessage / ChatMember / ChatInfoData / ContactDetail
    │   │   ├── Content.kt           # 收藏 / 文件 / 钱包 / 服务 / 好友申请 / 标签 / 公众号
    │   │   └── Emoji.kt             # StickerPack
    │   ├── repository/
    │   │   ├── ChatRepository.kt    # 会话 / 联系人 / 消息示例数据（MessageRepository 同文件）
    │   │   ├── ContentRepository.kt # 我的页与联系人子页面示例数据
    │   │   ├── EmojiRepository.kt   # 表情 / 表情包示例数据
    │   │   ├── LiveChats.kt         # 宿主真实会话缓存（含 extra 会话 + StateFlow）
    │   │   ├── LiveContacts.kt      # 宿主真实好友缓存
    │   │   ├── LiveProfile.kt       # 宿主真实本人资料缓存
    │   │   ├── SettingsRepository.kt
    │   │   └── SettingsRepositoryImpl.kt
    │   └── source/
    │       ├── AppDataSources.kt    # ChatSource / MessageSource / ContactSource 注册表
    │       └── LiveEvents.kt        # v0.5.77 新增：内核推送事件流（MessageEvent）
    ├── xposed/                      # LSPosed 模块实现（第 2 / 3 步）
    │   ├── MainModule.kt            # 模块入口（onPackageReady → TimUiHook）
    │   ├── TimUiHook.kt             # TIM Activity 生命周期/返回键/外跳/群公告 hook
    │   ├── UiHost.kt                # 在 decorView 挂 ComposeView、注册真实数据源
    │   ├── HostOwners.kt            # 给 TIM Activity 造 Lifecycle / ViewModelStore 等 owner
    │   ├── HostSettingsPrefs.kt     # 宿主设置门面（远端只读 + 本地可写）
    │   ├── ModuleContext.kt         # 模块 Context（resources / sharedPreferences 包装）
    │   ├── ModuleResources.kt       # 模块资源加载 / 缓存 / 重启恢复
    │   └── TimBridge.kt             # 反射调用 TIM / NT Kernel（约 4000 行）
    └── ui/
        ├── MainActivity.kt          # 单 Activity + 三页 HorizontalPager + NavDisplay
        ├── M3TimRoot.kt             # 根 Composable（主题 / 语言 / 状态注入）
        ├── UiMode.kt                # Miuix / Material 双风格
        ├── theme/                   # 双主题引擎（Material3 + Miuix）、ColorMode、本地设置
        ├── navigation3/             # Navigator + Route（miuix-nav）
        ├── component/
        │   ├── bottombar/           # 底栏 / 侧边导航栏（两套实现）
        │   ├── material/            # Expressive 组件（SegmentedList、SearchBar 等）
        │   ├── miuix/               # Miuix 组件（搜索框、动效等）
        │   ├── liquid/              # 悬浮底栏的玻璃/折射效果
        │   └── FloatingBottomBar.kt
        ├── screen/
        │   ├── chat/                # 消息列表 / 聊天详情 / 聊天信息 / 成员 / 公告 / 聊天记录 / 图片 / 视频
        │   ├── contacts/            # 联系人 / 联系人详情 / 新的朋友 / 群聊 / 标签 / 公众号
        │   ├── profile/             # 我的 / 编辑资料 / 收藏 / 文件 / 钱包 / 服务
        │   ├── settings/            # 设置
        │   ├── colorpalette/        # 主题设置（KernelSU ColorPalette 移植）
        │   ├── about/               # 关于
        │   └── forward/             # 转发页
        ├── viewmodel/               # MainActivity / Settings ViewModel
        └── util/                    # 模糊、窗口尺寸、延迟内容等工具
```

## 构建

```bash
# 调试包
./gradlew :app:assembleDebug

# 发布包（读取 ~/.gradle/gradle.properties 里的 TM_KEYSTORE 等签名配置，
# 未配置时自动回退 debug 签名）
./gradlew :app:assembleRelease
```

产物：`app/build/outputs/apk/{debug,release}/`。Release 经 R8 压缩 + 资源收缩，调试包带 `.debug` 后缀可与 Release 共存。
Release 签名使用本机 `~/.gradle/gradle.properties` 中的 `TM_KEYSTORE` / `TM_KEYSTORE_PASS` / `TM_KEY_ALIAS` / `TM_KEY_PASS`（密钥与配置不入库）。

## 参考与致谢

- UI / 交互 / 主题设置页参考 **KernelSU Manager**（GPL-3.0 / Apache-2.0 组件，仅作设计与实现参考，本项目未直接复用其 root、模块等业务代码）
- **Miuix**（Apache-2.0）及其 Compose 组件
- **Google Sans Flex**（SIL Open Font License 1.1，仅用于启动图标文字轮廓）
- **material-kolor**（MIT License）
- 配色与深色主题的前作 [TimMonet](https://github.com/linx3141/TimMonet)

## 许可证

本项目采用 **GNU General Public License v3.0**（GPL-3.0）发布，完整文本见 [LICENSE](LICENSE)。

原因：主题设置页参考/移植了 [KernelSU Manager](https://github.com/tiann/KernelSU) 的 ColorPalette 页实现（KernelSU 为 GPL-3.0），为保持许可证兼容，项目整体按 GPL-3.0 发布。第三方依赖各自的许可证见「参考与致谢」与 App 内「关于 → 开源许可」。

## 后续步骤（路线图）

1. **功能收尾**：二级页真实数据、附件面板剩余项、MarketFace/APNG 发送、编辑资料写回、长按菜单截图、宽屏 sidebar 风格。
2. **通知栏消息与已读回执**（可选）。
3. **系统消息/未知元素兜底完善**。
4. **工程化**：单元测试、CI、lint 门禁、可复现构建（版本号 / 签名 / 依赖校验）。
5. **清理项**：死代码、未引用字符串、硬编码文案。
