# TIM 4.1.0.4050：QQ 空间接入笔记

当前模块的「QQ 空间」通过 `mqqapi://qzone/open_homepage?uin=…` 交给 TIM 打开个人主页，不读取动态数据。它不是朋友动态流入口。

从本地 TIM APK 的清单和类索引可以确认：

| 线索 | 类 / 进程 | 已确认范围 |
| --- | --- | --- |
| 原生页面容器 | `com.qzone.reborn.base.QZoneShellActivity`、`QZoneShellInQZoneProcessActivity` | 清单里同时有主进程容器和显式的 `:qzone` 容器。不能假定个人主页的数据或 ViewModel 始终在 TIM 主界面进程。 |
| 个人空间页面 | `com.qzone.reborn.feedx.fragment.QZoneFeedxUserHomeFragment`、`com.qzone.reborn.feedx.viewmodel.QZoneUserHomeViewModel` | 出现在类索引；还未确认页面初始化和订阅动态的方法签名。 |
| 请求候选 | `com.qzone.homepage.business.protocol.QZoneGetUserInfoRequest`、`com.qzone.feed.business.protocol.QZoneGetFriendFeedsRequest` | 类名只提供搜索线索，不能视为可调用接口；好友动态请求尤其不能当作指定用户的个人动态。 |

后续应从个人主页入口验证原生请求和回调的完整调用链。先确认进程、目标 QQ 号、访问权限与分页结果，再复用 TIM 原生服务或已解析的结果，给 Compose MD3 页提供只读数据。空动态、无权限和加载失败需要分别显示；发布、评论等交互可保留原生入口。不要抽取会话凭据，也不要按猜测的接口地址直接请求。
