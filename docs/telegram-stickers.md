# Telegram 表情包同步

在 TIM 的「我的 → 设置 → 同步 Telegram 表情包」中登录，然后点击「同步已安装的表情包」。可以使用手机号和验证码，也可以用另一台已登录 Telegram 的设备扫码。启用两步验证的账号还需要输入密码。

同步会读取账号已安装的普通贴纸、面具和自定义表情集合，并保存到当前 TIM 账号。再次同步会更新顺序并移除已卸载的集合。空集合列表也会替换上次的结果。网络中断或存储空间不足时，聊天面板仍可使用上次成功同步的内容。

在聊天表情面板底部左右滑动选择集合，点贴纸即可发送。放大镜按集合名称搜索。静态 WebP 直接发送。TGS 和 WebM 首次发送时转换成 GIF 并缓存，生成的 GIF 使用白色背景，最长边为 256 像素。TIM 自带收藏表情仍可长按置顶或删除。

## 安装和构建

Telegram 登录在独立安装的 Material3TIM APK 中完成。使用 NPatch 内嵌模块时，也需在手机上独立安装同版本的 APK。登录会话保存在模块私有目录，TIM 通过一次只读文件授权接收表情包。

维护者可以在本机的 Gradle 用户配置文件 `~/.gradle/gradle.properties` 中提供 `TELEGRAM_API_ID` 和 `TELEGRAM_API_HASH`。使用项目自己的应用凭据构建后，普通用户可直接登录。未预配置凭据的构建会在首次连接时提示手动输入。应用凭据由开发者在 Telegram 登记应用后取得。[Telegram 官方申请说明](https://core.telegram.org/api/obtaining_api_id)

同步使用 TDLib 的账号登录和已安装集合接口。可以在同步页退出当前 Telegram 会话，再登录其他账号。已导入 TIM 的贴纸可离线使用，下一次成功同步会替换它们。[TDLib 登录流程](https://core.telegram.org/tdlib/getting-started)
