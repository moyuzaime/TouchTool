# 功能改动说明

本分支在 TouchTool 原版基础上做了两类改动：

1. **移植**：从「点击助手 M」（mod 版）反编译结果里按功能逐个重写进项目，不是直接搬运反编译代码。
2. **新增/改造**：在移植基础上继续做的交互、动效与稳定性改进。

每项都标注了代码位置；移植项另附「与原版差异」。

---

## 一、移植自 mod 版的功能

### 1. 日期「今天」按钮

- 位置：`app/src/main/java/top/bogey/touch_tool/ui/blueprint/pin_widget/PinWidgetNumber.java`
- 行为：日期类型的数字针脚在键盘旁多一个「今」按钮，点击填入今天的日期。
- 与原版差异：mod 使用 `MaterialDatePicker.todayInUtcMilliseconds()`（UTC 零点），这里改为取本机日期再换算到当天零点，避免时区造成日期偏移。

### 2. 受限设置引导

- 位置：`ui/setting/SettingView.java`、`res/menu/menu_setting.xml`
- 行为：设置页菜单新增「授予受限设置」，选中后弹出可直接执行的命令：
  `appops set <包名> ACCESS_RESTRICTED_SETTINGS allow`
- 背景：Android 13+ 上通过非商店渠道安装的 app 会被限制开启无障碍服务，需要手动解除。

### 3. 数学公式编辑与常用函数

- 位置：`ui/blueprint/selecter/select_edit_text/SelectEditTextDialog.java`、`PinWidgetString.java`、`res/values/string_array.xml`
- 行为：字符串针脚在「自动」子类型下进入表达式编辑时，编辑框下方显示一组常用函数与运算符，点击插入到光标处；编辑框高度跟随可视区域，输入内容中的换行会被去掉。
- 与原版差异：函数与说明文案统一由 `string_array` 的 `math_expression_functions` 维护。

### 4. 图片识别的范围与复制区域

- 位置：`ui/blueprint/picker/ImagePickerPreview.java`、`res/layout/float_picker_image_preview.xml`、`ui/blueprint/pin/PinView.java`
- 行为：
  - 识别范围可在「全屏 / 区域」之间切换，选区域时进入框选流程；
  - 支持把框选的区域直接复制成一个 `PinArea` 针脚（`PinView.copyPinValue()`）；
  - 显示识别相似度标记。
- 与原版差异：复制区域按钮放在底部按钮组内。

### 5. 连线跳转与跳回

- 位置：`ui/blueprint/CardLayoutView.java`（`focusPinLink()`、`jumpBack()`）、`ui/blueprint/BlueprintView.java`（`jumpBackButton`）
- 行为：点击有连线的针脚时，跳转到对端卡片并居中选中；顶栏出现「跳回」按钮，可返回上一次跳转的位置。
- 与原版差异：用「选中并居中」表达跳转结果，不额外绘制临时连线标记。

### 6. 蓝图辅助工具

- 位置：`CardLayoutView`（缩放、四边跳转、结果计数）、`res/menu/menu_blueprint.xml` 的「辅助工具」开关
- 行为：
  - **缩放条**：显示当前缩放比例，可拖动调整；
  - **四边跳转**：把画布快速移到最左 / 最上 / 最右 / 最下边界；
  - **结果计数**：搜索时在顶栏显示命中的卡片数量。

### 7. 卡片组暂存（移植基础功能）

- 位置：`CardLayoutView` 的暂存组区块、`res/layout/view_blueprint.xml`
- 行为：把选中的若干卡片存为一个「组」；组以面板形式列出，点击可聚焦并高亮组内卡片，也可把组移到视图中心或删除。

---

## 二、新增与改造

### 1. 暂存组命名

- 新建组时按卡片位置排序（先 y 后 x）取第一张：有备注用备注（`Action.getDescription()`），没有备注用卡片名（`Action.getTitle()`），超过 16 字截断加省略号。
- 长按组列表行可改名。

### 2. 暂存组持久化

- 新增：`bean/save/SavedCardGroup.java`（组模型：名称 + 动作 id 集合）、`bean/save/SavedGroupSaver.java`（存储）
- 按**任务 id** 存进独立 MMKV（`SAVED_GROUP_DB`）。
- 由于不写入任务数据，暂存组**不随 `.tt` 导出**，属于视图书签。
- 增、删、改名、清空以及卡片被删除后都会回写。

### 3. 暂存组面板与入口

- 面板：宽度固定 176dp，展开/收起带淡入与位移，当前组用圆角底色高亮，点画布任意位置可收起。
- 底栏常驻入口：只要存在暂存组，底部常驻工具栏最右侧就显示入口按钮；无暂存组时不显示。长按入口可清空全部组（有确认框）。
- 高亮与选中态区分：
  - 选中态：卡片外沿虚线（primary 色）；
  - 组高亮：卡片内侧实线（tertiary 色，内缩 3dp）；
  - 组高亮只叠加描边，不改写卡片自身的底色与描边，避免与选中样式互相覆盖。

### 4. 分享

- 位置：`ui/tool/task_manager/ExportTaskDialog.java`、`utils/AppUtil.java`（`shareImage` / `shareText`）、`bean/action/system/ShareToAction.java`
- 行为：导出与分享统一走 `Intent.createChooser` 系统分享面板；MIME 修正为 `application/octet-stream`（`.tt`）、`image/jpeg`、`text/plain`。
- 说明：`.tt` 在分享面板里显示为未知类型图标，是自定义文件类型的必然结果。

### 5. 任务日志实时显示

- 位置：`bean/save/log/LogSaver.java`、`service/TaskRunnable.java`、`bean/other/log/LogInfo.java`、`bean/save/log/LogSave.java`、`ui/tool/log/LogFloatView.java`、`ui/tool/log/LogFloatViewAdapter.java`
- 原来的问题：开启「细节日志」后，所有日志都挂在开头的动作节点下面，而该节点要等任务结束出栈时才落盘，日志窗口运行中读不到任何根节点，于是整份日志在结束时一次性刷出。
- 改动：
  - 顶层动作节点**开始时即落盘**，结束时改为更新同一条；`LogInfo` 增加 `transient` 的 `saved` 标记避免重复落盘。
  - 子日志写入后重写整条祖链节点，嵌套项随执行实时出现。
  - `LogSaver` 对子日志同样发出通知，并新增 `updateLog(key, log)` 用于按原 uid 重写。
  - 日志窗口改为**逐条增量插入**：适配器维护 `uid → 节点` 映射，只 `notifyItemInserted` / `notifyItemRangeInserted` 新增行，不再整表重建。
  - 滚动改为**粘底**：只有本来就在底部（最后可见项在倒数两行内）才跟随；展开/折叠节点或手动拖动后暂停跟随，拖回底部松手才恢复。
  - 500ms 兜底轮询只比较根节点数量，需要时增量补齐。

### 6. 日志窗口上下箭头

- 搜索框为空时不再是「两个箭头都跳最后一行」：
  - **上箭头**：跳转到当前所在块的那个展开节点所在行；
  - **下箭头**：跳转到当前所在层的最后一行（该展开节点子树末尾；新日志持续插入时即为最新一行）；
  - 「所在块」以视口第一行为锚点，向上查找最近的已展开祖先得出。没有展开节点时退回最顶行 / 最新一行。
- 顺带修正搜索命中第 0 行时不滚动的问题。

### 7. 日志窗口布局整理

- 位置：`res/layout/float_log.xml`、`ui/tool/log/LogFloatView.java`
- 标题栏由「FrameLayout + 72dp 固定边距」的重叠定位改为横向排列：左侧展开/导出，中间标题按权重占位，右侧清空/关闭，窄窗口下不再互相挤压。
- 去掉列表外层的 `MaterialCardView` 背景，窗口只保留一层表面；列表上下留白，多显示一到两行日志。
- 底部搜索区重组：上下箭头由竖排改为并排（点击区域更大）；四个搜索开关（拼音/序号/区分大小写/正则）收进右侧 `⋯` 展开的选项条，默认收起、不占窗口空间（悬浮窗里用 `PopupMenu` 会被裁到屏幕外，所以做成窗口内展开）。
- 缩放拖拽手柄固定在整个悬浮窗的右下角，并给底部行右侧留出空位避免压住按钮；缩放手势由手柄自身的触摸监听处理，不再依赖父容器拦截（手柄贴角后与子控件争事件会失效）；手柄放在内容区内，折叠成单按钮时会一起隐藏。
- 无日志时列表中央显示「暂无日志」，不再是空白。
- 无障碍服务未就绪时不再尝试加窗（会抛 `BadTokenException` 并留下关不掉的幽灵窗口），改为提示「无障碍服务未启用」。

### 8. 任务菜单：上游弹出菜单 + 「辅助工具」开关

- 位置：`ui/blueprint/BlueprintView.java`（`showTaskMenu()`）、`res/menu/menu_blueprint.xml`
- 菜单沿用上游的 `PopupMenu`：`打开任务日志`、`记录日志细节`（可勾选，仅顶层任务可见）、`任务截图` 三项的行为与上游一致。
- 本项目只多一项可勾选的「辅助工具」，控制方向盘/缩放条的显隐。
- 「任务截图」仍走本项目的流式导出（`showTaskCapture()`），预览、保存、分享流程见第 9 节。

### 9. 任务截图：按原始尺寸流式导出

- 位置：`ui/blueprint/BlueprintView.java`（`showTaskCapture()` / `showCapturePreview()`）、`ui/blueprint/CardLayoutView.java`（`prepareTaskCapture()` / `drawCaptureBand()` / `restoreTaskCapture()`）、`utils/PngStreamWriter.java`、`utils/AppUtil.java`（`writePictureFile` / `shareImageFile`）
- 原来是「一次性 new 出整张 Bitmap + 缩放上限 4096」：图一大就 OOM，且分辨率被压到 4096。
- 改为按原始比例（`scale = 1`）分带渲染、流式写 PNG：每带只在内存里放「宽 × 单带高」的位图，带宽按 32MB 预算自动换算，因此尺寸不再受内存限制（实测 29289×7854、约 2.3 亿像素导出正常，旧实现会先压到 4096 以内）。
- 导出区域 = 卡片包围盒 ∪ 所有连线的路径包围盒（`Path.computeBounds`），再留 2 格边距：连线是贝塞尔曲线、会弯出卡片范围，只按卡片包围盒留边会被裁。
- 绘制时不再把区域左上角写进 `offsetX/offsetY`：卡片坐标本身就在内容坐标系里，对齐统一交给 `drawCaptureBand` 的 `translate`；两处都加偏移会让内容整体错位、右/下边缘被切。
- 导出期间卡片不必在视口内，全部临时置 `needDraw = true` 后逐带绘制。
- `PngStreamWriter` 自行拼 PNG 结构：IHDR 固定 RGBA8，逐行 None 过滤后喂给 `Deflater`，按 256KB 攒成连续的 IDAT 块，最后写 IEND；CRC 按块计算。
- 线程分工：`prepareTaskCapture()` 与 `restoreTaskCapture()` 会改卡片布局，必须在 UI 线程执行；后台线程只做画布渲染与编码（直接调 `updateCardsPos()` 会抛 CalledFromWrongThreadException，这点已在实现中规避）。
- 生成期间显示「正在按原始尺寸生成截图…」，完成后用 `inSampleSize` 采样解码出预览（不整张解码），保存与分享都用原始 PNG 文件：`AppUtil.writePictureFile` 按 `image/png` 写入相册，`AppUtil.shareImageFile` 走 FileProvider + `createChooser`。
- 预览下方显示原始像素尺寸；比例不符时 `fitCenter` 留边，不拉伸。

### 10. 界面动效统一

- 新增 `utils/AnimUtil.java`：统一的「淡入淡出 + 轻微位移」显隐动画（180ms）。
- 蓝图底部两条工具栏（常驻 / 选中）改为**串行**切换：旧的完全收走后再浮出新的，避免交叉淡入时重叠成一块；串行切换带序号，连续切换时只有最后一次生效，避免先前排队的回调把底栏切回去。CardLayoutView 框选过程中批量改选中、结束后只刷新一次工具栏。
- 任务列表长按进入选择模式时：添加按钮下沉收走，选择工具栏再浮起；退出时反向。
- 搜索栏与搜索选项从上方落下展开、收回。
- 画布新卡片淡入并轻微放大，删除时先淡出再移除。
- 任务列表增加逐项入场动画（`res/anim/layout_enter.xml`）。
- 进入选择模式时底部导航栏**立即**隐藏，避免其 `GONE` 引起容器重排、导致选择工具栏先落在旧位置再下滑。

### 12. 「控件是否存在」增加控件输出

- 位置：`bean/action/node/IsNodeExistAction.java`
- 行为：卡片末尾新增输出针脚「控件」，命中时输出找到的 `NodeInfo`，未命中输出空；布尔结果针脚保持不变。
- 输出取本次命中的第一个控件（文本/ID/类名/描述模式取首个匹配项，路径文本模式取首条路径结果）。
- 另新增输出针脚「控件列表」（PinList<PinNode>），输出本次命中的全部控件；路径模式只能命中单个控件，该针脚在此模式下隐藏。
- 针脚追加在声明末尾，老任务经 `reAddPins` 自动补齐，不影响已有连线。

### 13. 设置页右上角菜单改为卡片弹窗

- 位置：`ui/setting/SettingView.java`、`res/layout/popup_setting_menu.xml`、`res/menu/menu_setting.xml`
- 行为：工具栏只留一个「更多」按钮，点击弹出卡片式菜单（帮助文档、视频教程、设置自动开启录屏、设置允许受限设置），每行带图标，帮助类与设置类之间加分隔线。
- 替代系统 PopupMenu：系统菜单无法控制行高、内边距与图标，字体放大时特别松散。
- 菜单宽度按内容自适应；末尾新增「日志」入口，原先只能点版本文本才能打开应用内日志。

### 14. 「图片是否存在」增加区域与相似度输出

- 位置：`bean/action/image/IsImageExistAction.java`
- 行为：新增两个输出针脚——「区域」输出目标图片在原图中的位置（原图坐标，含区域裁剪偏移），「匹配相似度」输出实际相似度（0~100）；未命中时区域为空、相似度为 0。
- 实现改用 `DisplayUtil.matchTemplateResult`，与「查找图片」共用同一套结果。

### 15. 控件信息面板补齐更多属性

- 位置：`ui/custom/float_view/NodeInfoFloatView.java`、`res/values/string_array.xml`（`node_info_type`）
- 行为：在原有的类名/ID/路径/文本/描述/序号/可点击/可见/区域/大小/父节点/子节点之外，新增包名、可长按、可滚动、可获得焦点、可编辑、已选中、已勾选、已聚焦、是否启用、提示文本、层级深度、支持操作数、窗口ID、输入类型。
- 取值直接来自 `AccessibilityNodeInfo`；存档节点（无实时 node 对象）这些项输出空。

### 16. 「获取控件信息」补齐输出针脚

- 位置：`bean/action/node/GetNodeInfoAction.java`、`res/values/task_strings.xml`
- 行为：在原有类名/控件ID/文本/描述/区域/是否可用/是否可见/控件身份之外，新增包名、可长按、可滚动、可获得焦点、可编辑、已选中、已勾选、已聚焦、是否启用、提示文本、层级深度、支持操作数、窗口ID、输入类型、同级序号、子节点数共 16 个输出针脚。
- 这些属性直接读 `AccessibilityNodeInfo`，存档节点（无实时 node）时布尔为 false、数值为 0、文本为空。

### 17. 悬浮窗「点了没反应」修复

- 位置：`utils/float_window_manager/FloatWindow.java`、`FloatWindowHelper.java`、`ui/tool/ToolView.java`
- 原因：无障碍服务被系统停用后单例仍在，`addView` 抛 `BadTokenException`（token null is not valid），而窗口注册表里已留下同名 tag，之后每次 `show()` 都因 tag 已存在直接返回，表现为永久无响应。
- 修复：`show()` 检查记录对应的窗口是否真的添加成功（`addFailed`），失效则清理后重建；工具页入口改为要求 `isConnected()`；加窗口失败时弹提示并把异常写入「日志 → 异常」。
- 建议：安装、强停或清理后台后，进系统设置把无障碍服务关一次再开，避免系统保持「已崩溃」状态。

### 18. 悬浮窗三项优化

- 事件日志开关：`MainAccessibilityService` 不再逐条拼 `event.toString()`，改由 `SettingSaver.APP_LOG_EVENT` 控制，日志弹窗顶部可开关。
- 窗口位置持久化：`FloatWindow` 按 tag 把拖动后的位置存进 `SettingSaver.FLOAT_WINDOW_LOCATION`，打开时恢复，进程重启不丢。
- 延时任务清理：`ToastFloatView`、`KeepAliveFloatView`、`PlayFloatView` 的 `dismiss()` 先 `removeCallbacksAndMessages(null)`。

### 19. 悬浮窗复用与重复点击反馈

- `FloatWindowHelper` 关闭窗口时先把内容视图从容器摘掉，视图可以再次挂载复用。
- 应用信息、任务日志改为 `showTool(context)` 复用同一实例（弱引用缓存，Activity 重建后自动重建）。
- 窗口已在屏幕上时再次打开会提示「窗口已打开」，不再静默无反应。

### 20. 悬浮窗层级管理

- `FloatWindow` 增加 `setLevel(int)`：数值大的窗口在上层。
- 显示新窗口后，把层级更高的已开窗口重新加入以置顶，避免被后开的低层级窗口盖住。
- 层级约定：选择器（选控件/颜色/区域）100，标记区域 70，日志/应用信息/控件信息 60，播放条 30，其余默认 0。

### 21. 收起后的悬浮窗显示功能图标

- 任务日志、应用信息、控件信息三个可收起窗口，收起时按钮图标改为各自功能图标（`icon_note_stack` / `icon_apps` / `icon_widgets`），不再统一显示放大图标。
- 展开后仍显示收起图标 `icon_zoom_in_map`，点击可再次收起。

### 22. 修复取色/选控件空指针

- 位置：`ui/blueprint/picker/FullScreenPicker.java`、`ColorPicker.java`
- 原因：`screenInfo` 在 `onShow()` 里延迟 300ms 初始化，而窗口加上后可能立刻绘制，`ColorPicker.dispatchDraw` 取 `screenInfo.getScreenShot()` 时空指针；服务不在时更会直接为空。
- 修复：`show()` 里先检查服务并同步准备好 `screenInfo`；绘制与取色路径全部加空判断；延迟初始化处再兜一次服务为空则关窗。

### 23. 「控件是否存在」「查找控件」增加查找方式

- 位置：`bean/action/node/IsNodeExistAction.java`、`FindNodeAction.java`、`res/values/string_array.xml`（`find_node_type`）
- 新增 5 种：坐标（命中当前位置最上层控件）、区域（区域内可用控件）、全部控件、可点击控件、可编辑控件。
- 坐标模式只命中单个控件，卡片新增「位置」输入针脚，同时隐藏控件列表输出；其余新方式都输出列表。
- 查找控件在超时重试前会清空结果列表，避免重复累加。

### 24. 合并重复卡片（第二步）

- **布尔四张 → 条件组合**：`BooleanAndAction` 改造为可切换「与/或」并支持「短路」，隐藏 任意条件达成/短路任意/短路所有。
- **数值比较三张 → 数值比较**：`NumberResultAction` 增加比较方式（大于/小于/等于/大于等于/小于等于/不等于），隐藏 数值比较 < 与 数值比较 =。
- 被合并的旧卡片类、枚举与映射已一并删除（无老任务包袱）。
- 同时删除「获取区域内控件」「关闭所有贴图」两个动作类及其枚举与映射。
- 删除清单：NumberLessAction、NumberEqualAction、BooleanOrAction、BooleanOrShortCircuitAction、BooleanAndShortCircuitAction、GetNodesInAreaAction、StickCloseAllAction，以及 ActionType 中对应枚举。
- 同时清理这些卡片对应的 17 条无用文案。
- 全量扫描（1011 条字符串定义，含 Manifest 引用）后再删除 51 条无引用文案；保留 13 条「未实现功能」文案备用（自动重启无障碍、开机自启、重启服务、缓存清理等）。

### 25. 设置菜单新增「重启无障碍服务」

- 位置：`ui/setting/SettingView.java`、`utils/AppUtil.java`、`res/layout/popup_setting_menu.xml`、`AndroidManifest.xml`
- 行为：菜单点击后通过 `WRITE_SECURE_SETTINGS` 把本服务从已启用列表摘掉再写回，触发系统重新拉起；成功后提示「重启中」并回填启用状态，失败则提示并跳转系统无障碍设置。
- 需 adb 或 root 授予 `WRITE_SECURE_SETTINGS`（`appops` 或 `pm grant`），否则走手动引导。

### 26. 设置菜单补齐无障碍相关功能

- **自动重启无障碍**：开关开启后，App 回到前台时若服务未连接，自动通过安全设置重绑（`MainActivity.onResume`）。
- **开机自启**：开关开启后由 `BootReceiver` 在开机广播时拉起界面并重绑服务（需 `RECEIVE_BOOT_COMPLETED`）。
- 重启服务入口改为先弹说明确认；两个开关首次开启时显示对应说明文案。
- 日志弹窗标题显示当前条数，清空按钮文案改「清理」。

### 27. 日志支持长按复制与导出

- 日志列表长按任意条目复制「时间 分类 标签 内容」。
- 日志弹窗新增「导出日志」，走系统文件保存框写成 UTF-8 的 txt。

### 28. 选择器与预览窗保持官方行为

- `utils/float_window_manager/`、`ui/blueprint/picker/`、`float_picker_node_preview.xml` 回到 origin/master；移除窗口 `setLevel` 与实例复用。
- 预览卡与选择器都是普通悬浮窗：选择器显示时框架 `hideAll` 隐藏预览卡，关闭时 `showLast` 恢复。
- 本工程还有保活窗、播放条等窗口，它们关闭时同样触发 `showLast`，可能让预览卡提前出现；业务层不再干预窗口显隐。
- 尝试过的「选择期间隐藏预览卡」方案（hide / dismiss / 轮询 / 延迟隐藏 / 收起成圆点）全部回退。
- 保留修复：取色器 `screenInfo` 为空导致的崩溃；补回官方引用的 `picker_image_offset` 文案。

### 29. 文件操作卡片

- 位置：`bean/action/file/`，独立分组「文件」（`ActionGroupType.FILE`，标签排在最后）。
- 共 10 张：读取文件、读取一行、读取所有行、写入文件、编辑行、删除文件、文件是否存在、列出目录、复制文件、创建。
- 读取类为计算动作；写入、编辑、删除、创建、复制为执行动作；路径与文本按 UTF-8 处理。
- 写入/创建自动补父目录，删除文件夹递归删除，行号从 1 开始。
- 合并：写入文件含「写入方式（覆盖/追加）」；创建含「类型（文件/文件夹）」；编辑行含「操作（写入/替换/删除）」。
- 依赖「所有文件访问权限」才能读写 `/sdcard` 任意路径（Android 11+ 的 `WRITE_EXTERNAL_STORAGE` 无效）。

### 30. 文件访问权限入口

- Manifest 增加 `MANAGE_EXTERNAL_STORAGE`；`AppUtil.hasAllFilesAccess()` / `requestAllFilesAccess()` 负责查询与跳转系统授权页。
- 设置页「权限设置」分区新增布尔开关：开关只反映系统授权状态，应用无法自行授予或撤销，点击统一跳系统页，返回时在 `onResume` 同步。

### 31. 设置弹窗整理

- 字号由 `BodyLarge` 降到 `BodyMedium`(14sp)；带开关的行改为 `match_parent`，文本单行省略、开关右对齐，不再被裁切。
- 删除重复的「文件访问权限」与「重启无障碍服务」入口（后者在主界面「功能启用」卡片已有）。
### 35. 合并 mod 261110 的新改动

- 图片预览：恢复识别范围（全屏/区域、框选、复制区域），新增「粘贴区域」，从针脚剪贴板取 PinArea 作为识别范围。
- 蓝图缩放条改为竖向 ZoomBarView（0~1 对应缩放 0.2~2.0），旧滑块隐藏；新增 canvasControl 主题色属性。
- 蓝图新增方向盘 SteeringWheelView：中心点切换「跳边界 / 平移画布」，四向操作；CardLayoutView 增加 panBy。
- 暂存组按钮新增数量角标 savedGroupBadge，随暂存组变化刷新。

---

## 三、数据与兼容

| 项 | 说明 |
|---|---|
| 新增 MMKV | `SAVED_GROUP_DB`，键为任务 id，值为暂存组的 JSON |
| 日志数据 | `LogInfo` 仅新增 `transient` 字段，不参与序列化，旧日志可直接读取 |
| 任务文件 | `.tt` 格式未变；暂存组不写入任务数据 |
| 分享 | MIME 变化不影响既有文件内容 |

---

## 四、已知差异与待验证

- 暂存组高亮形状与 mod 不同：mod 在卡片外围画虚线框，这里用内侧实线，以与选中态区分。
- 日期「今天」使用本地日期，mod 为 UTC。
- 动效、手势、悬浮窗这类行为无法靠构建验证，需真机确认。
- 日志窗口的展开状态在增量插入下得以保留；但若某条通知丢失，嵌套项可能要在下次展开或重新打开时才出现（500ms 轮询只兜底根节点）。
- 文件操作卡片依赖「所有文件访问权限」：未授权时读写 `/sdcard` 任意路径会失败，未单独提示，可在「设置 → 权限设置 → 文件访问权限」跳系统页授权。

---

## 附录：本分支提交

```
fix(share): 分享统一走系统分享面板并修正 MIME 类型
feat(port): 移植 mod 版的日期「今天」按钮与受限设置引导
feat(port): 移植 mod 版的数学公式全屏编辑与常用函数插入
feat(port): 移植 mod 版的图片识别范围与复制区域
feat(port): 移植 mod 版的蓝图辅助工具（缩放条、四边跳转、结果计数）
feat(port): 移植 mod 版的卡片组暂存
feat(port): 移植 mod 版的连线跳转与跳回
Merge branch 'fix/share-chooser-mime' into feat/port-mod-tools
feat(port): 卡片组暂存按钮移入底部卡片工具栏
feat(port): 优化卡片组暂存交互
feat(port): 优化卡片组暂存面板的观感
feat(port): 卡片组暂存支持命名、持久化与虚线高亮
feat(port): 新建卡片组时用卡片备注作为默认名
feat(port): 区分卡片组高亮与选中态
feat(port): 底栏常驻暂存入口
feat(port): 底栏暂存入口支持长按清空
feat(port): 统一界面显隐动画，减少割裂感
feat(port): 工具栏切换改为串行动画
fix(port): 修复工具栏首次显示时先出现在错误位置再下滑
fix(port): 修复选择工具栏先出现在旧位置再下滑
feat(port): 任务日志改为实时显示
fix(port): 日志运行中即可见
feat(port): 日志窗口改为逐条增量插入
fix(port): 日志不再无条件跳到底部
feat(port): 展开或折叠日志节点后暂停自动跟随
feat(port): 日志窗口空搜索时上下箭头在展开块内跳转
feat(port): 日志上下箭头按当前所在层跳转
feat(port): 日志上箭头改为跳转到展开节点
docs: 补充移植与新增功能说明
feat(port): 整理任务日志悬浮窗的布局
feat(port): 日志搜索选项收进展开条并修复缩放失效
fix(port): 缩放手柄回到悬浮窗右下角并恢复缩放
fix(port): 折叠后不再残留缩放手柄
feat(port): 任务菜单改为带图标与开关的对话框
feat(port): 任务菜单改为贴近右上角的弹出面板
feat(port): 重做任务截图预览窗口
feat(port): 任务截图改为按原始尺寸流式导出 PNG
fix(port): 修复框选后底栏回退，并放宽截图导出留边
fix(port): 修复任务截图被裁切
docs: 修正任务截图导出区域与偏移的说明
feat(port): 控件是否存在新增控件输出针脚
feat(port): 控件是否存在新增控件列表输出针脚
fix(port): 工具页悬浮窗在服务未就绪时给出提示
fix(port): 无障碍服务关键回调加异常兜底
feat(port): 设置页菜单改为卡片弹窗
fix(port): 更多按钮图标改为跟随主题色
fix(port): 设置菜单宽度自适应并增加日志入口
feat(port): 图片是否存在增加区域与匹配相似度输出
docs: 补充图片是否存在输出针脚说明
feat(port): 控件信息面板补齐更多属性
feat(port): 获取控件信息补齐输出针脚
fix(port): 修复悬浮窗点了没反应的残留 tag 问题
perf(port): 悬浮窗事件日志开关、位置持久化与延时任务清理
perf(port): 工具窗口复用实例并提示重复打开
feat(port): 悬浮窗增加层级管理
feat(port): 收起后的悬浮窗显示功能图标
fix(port): 修复取色器屏幕信息为空导致的崩溃
feat(port): 控件是否存在与查找控件增加查找方式
feat(port): 收敛重复动作入口
feat(port): 合并布尔与数值比较卡片
chore(port): 删除已合并的旧动作类
chore(port): 清理已删除动作的无用文案
chore(port): 全量清理无引用字符串
feat(port): 设置菜单新增重启无障碍服务
feat(port): 实现自动重启无障碍与开机自启
feat(port): 选择时隐藏预览悬浮窗
fix(port): 选择时销毁预览窗而非隐藏
fix(port): 修复窗口销毁后闪回
chore(port): 预览窗重新显示时记录调用栈
feat(port): 日志支持长按复制与导出
fix(port): 选择器还开着时不提前显示预览窗
feat(port): 选择器选完立即关闭
fix(port): 选择器先关闭再回调结果
fix(port): NodePicker 先关闭再回调
feat(port): 选择期间关闭控件预览窗，选择器关闭后恢复
feat(port): 控件信息工具改为先进选择器再出预览卡
feat(port): 预览卡在用隐藏代替销毁，选择器关闭后恢复
fix(port): 预览卡延迟隐藏，避开入场动画覆盖
tune(port): 预览卡隐藏延迟改为 500ms
feat(port): 控件预览卡支持收起成小圆点
fix(port): 选择器截屏期间隐藏控件预览卡
chore(port): hide 时记录注册表内容用于定位预览卡未隐藏
fix(port): 预览卡改由选择回调恢复，去掉轮询
fix(port): 选择器打开期间持续压制控件预览卡
feat(port): 新增 7 个文件操作卡片
feat(port): 文件动作独立分组
feat(port): 文件操作支持所有文件访问权限
feat(port): 文件访问权限并入权限设置区
feat(port): 文件访问权限改为布尔开关
fix(port): 设置弹窗移除残留文件权限行并修正开关溢出
style(port): 设置弹窗菜单字号调小
refactor(port): 移除设置弹窗里重复的重启无障碍入口
fix(port): 文件访问权限开关只反映系统状态
feat(port): 文件卡片补充读取行/删除行/建文件/建目录
feat(port): 文件卡片新增写入指定行与替换指定行
refactor(port): 文件卡片合并为 10 张
fix(thread): 并行卡超时收尾与分支启动失败计数
fix(thread): 取消排队中的运行时也触发收尾
fix(thread): 取消异步句柄时清理父运行的子集合
fix(thread): 暂停/恢复统一传播到子分支
fix(log): 日志节点 children 并发写保护
perf(thread): 等待异步分支改用完成信号
perf(thread): 线程命名、空闲回收与池容量
feat(thread): 每任务分支上限
feat(thread): 运行态指标与调试页
feat(thread): 分支进度角标与停止宽限提示
perf(thread): 并行卡记录提交耗时，超时口径可判断
docs: 补充多线程收尾与调度的改动说明
fix(thread): 收尾回调只触发一次，并行分支计数不被日志异常卡住
fix(thread): 收敛并发早停、等待退出与分支上限默认值
fix(thread): setFuture 补登 future 时的取消与收尾
fix(thread): 收尾回调去重判定，等待信号缓存到运行上
docs: 补充并行超时语义与分支上限默认值说明
perf(thread): 并行卡统计日志写明结束原因
fix(setting): 清理缓存多选列表越界崩溃，并去掉分享到多出的线程选项
feat(blueprint): 任务菜单改为与设置页同款卡片弹窗
feat(setting): 系统辅助开关一键开启点按反馈与指针位置
fix(setting): 系统辅助无超级用户时兜底，横屏设置页补缺失行
feat(setting): 自动重启无障碍与开机自启移到权限设置
feat(theme): 默认主色调改为黑色
feat(node): 获取控件信息默认收起扩展属性
```

## 36. 线程卡片：加锁 / 解锁 / 原子累加

- 新增 `MutexLockAction`：按「锁名」互斥，支持超时；拿不到锁走「失败」分支，输出「是否拿到锁」
- 新增 `MutexUnlockAction`：释放同名的锁，与加锁配对
- 新增 `AtomicAddAction`：跨并行分支安全累加，输出累加后的值
- 共享结构集中在 `utils/thread/MutexUtil`：锁、计数器、以及按任务运行维度的持锁登记
- 任务结束时统一释放该次运行持有的锁，避免分支异常退出后死锁
## 37. 线程卡片：异步执行 / 等待完成 / 取消子任务

- `AsyncAction`：把「异步」针脚后面的动作放到独立线程启动，主流程继续；输出短句柄
- `AwaitAsyncAction`：按句柄等待结束（留空等待本次运行的全部异步分支），支持超时，输出「是否超时」
- `CancelAsyncAction`：按句柄结束（留空结束全部），输出是否成功
- `AsyncUtil` 维护句柄登记与父子关系；父运行结束时自动收掉尚未结束的异步分支
## 38. 并行执行卡的日志与针脚整理

- 分支日志实时写入：去掉内部运行强制缓存，分支动作日志带 `[分支名]` 前缀直接进日志库
- 内部分支运行不再清空任务日志（只有主运行清空），避免并发启动互相抹掉
- 分支名存在动作的 `branchNames` 列表并回填针脚，编辑后立即保存任务
- 新建并行卡只有「完成」出口，分支由「添加执行」生成；继承的 outPin 已从卡面移除
- 蓝图顶栏新增「运行」按钮，直接执行当前任务的手动开始动作
## 39. 日志悬浮窗（本轮）

- 浮窗结构对齐上游：外层 filled 卡片 + 内层 `?attr/colorSurface` 卡片承载列表，去掉阴影
- 动态取色：直接使用传入 Context 的主题（Activity 已含动态色/自定义色），不再二次 `wrapContextIfAvailable` 覆盖用户选色
- 日志存储：GUID 作 key + `order` 顺序表，读取按 uid 遍历，修复「日志加载失败」
- 父子关系：`LogInfo` 新增 `parentLogUid`，内容行也能实时嵌套；重建时缺 children 的条目自动补挂
- 稳定性：`pop` 判空；回退会截断日志栈的 20ms 全局拦截，只对计算日志去重

### 已知问题

- 并行分支内的「输出日志」在同一毫秒会记录两条（渲染层按「时间+actionId」去重仍无法合并，说明两条 actionId 不同）
- 定位方法：`LogSaver.addLog` 打印 `actionId + 调用栈`，对比两次写入的来源
## 40. 蓝图里的任务日志入口与日志浮窗回退为上游

- 入口：蓝图顶栏不再单独放「日志」按钮，任务日志统一从「⋯ → 打开任务日志」进；「记录日志细节」回到上游的可勾选菜单项（切换任务的 `FLAG_DEBUG` 并保存，仅顶层任务可见）。
- 浮窗：`ui/tool/log/LogFloatView`、`LogFloatViewAdapter`、`res/layout/float_log.xml` 与上游完全一致，第 5、6、7、39 节描述的日志改造均已回退。
- 保留：任务菜单里本项目的「辅助工具」开关，以及任务截图的流式导出。

## 41. 并行执行的分支日志挂回并行节点

- 位置：`bean/action/logic/ParallelExecuteAction.java`、`service/TaskRunnable.java`（新增 `getCurrentLogInfo()`）
- 问题：分支 runnable 的日志在 `onFinish` 里才落盘，此时主流程可能已经走完并行卡、日志栈已清空，`runnable.addLog()` 于是把分支写成新的根节点——日志窗口里表现为并行节点没有子项、分支条目跑到同级甚至根级，顺序也乱。
- 改动：执行并行卡时先取当前日志节点作为容器，分支结束时把分支日志节点直接挂到它下面并补写一次父节点；容器为空（未开细节日志）时仍走原来的 `addLog()`。
- 结果：结构回到上游那种 `循环 → 并行执行 → 并行执行(分支) → 条件循环/输出日志` 的嵌套。

## 42. 日志浮窗收起后的按钮图标

- 位置：`ui/tool/log/LogFloatView.java`
- 收起成单个圆按钮时图标改为 `icon_note_stack`（与「任务日志」入口一致），不再用通用的 `icon_zoom_out_map`；和「应用信息」收起用 `icon_apps`、「控件信息」收起用 `icon_widgets` 的做法统一。
- 展开后图标仍回到 `icon_zoom_in_map`。

## 43. 任务执行期的多线程收尾与调度

- 位置：`bean/action/logic/ParallelExecuteAction.java`、`bean/action/thread/AsyncAction.java`、`AwaitAsyncAction.java`、`CancelAsyncAction.java`、`service/TaskRunnable.java`、`service/MainAccessibilityService.java`、`utils/ThreadUtil.java`、`utils/thread/AsyncUtil.java`、`MutexUtil.java`、`BranchLimiter.java`、`BranchMonitor.java`、`TaskThreadPoolExecutor.java`、`bean/other/log/LogInfo.java`、`bean/save/log/LogSaver.java`
- 超时收尾：并行卡在 `latch` 返回后停掉仍在跑的分支，再给 200ms 宽限；超时/提前完成/任务停止后，分支不会再被当成还在正常执行，分支日志挂回并行节点而不是升到根级（忽略中断的动作仍可能再执行一小段，属尽力而为）。
- 未启动计数：`runTask` 返回 `null`（池满/服务不可用/达分支上限）时计入「未启动」并推进计数，不再每次运行都等满超时，也不再出现对 `null` 调 `stop()` 的空指针。
- 排队中取消：`TaskRunnable` 新增 `finishOnce()` 统一收尾出口；取消还没开始的任务时由 `stop()` 补一次 `onFinish`，并行 `latch`、异步句柄、分支名额都能归零。已在跑的任务仍走 `run()` 的 `finally`，不会提前触发。收尾时先把监听从集合摘掉再回调，补挂在已收尾运行上的监听只补一次；并行分支的日志挂载失败也照样推进计数。
- 完成数：按实际完成的分支数判断（原实现会提前一个分支触发），达成后只停一次其余分支；结果针脚按是否达成赋值。提交期间就达成时，循环结束再补一次早停，避免后提交的分支白跑。
- 异步等待：`AwaitAsync` 改用 `TaskRunnable.getDone()` / `getStopSignal()` 与 `AsyncUtil.awaitAll()`，去掉 50ms 轮询；子被停止或取消时立即退出，不会卡在不可中断的动作上；取消句柄时同步清理父运行的子集合，`size(parent)` 能真正归零。
- 暂停传播：`runTask(task, startAction, listener, parent)` 在分支提交成功后给父运行挂暂停桥接，父运行暂停/恢复时同步传给分支，并行卡与异步卡语义一致，删掉并行卡里重复的实现。
- 分支上限：`BranchLimiter` 按任务运行统计在跑的分支数，超过上限的分支按「未启动」处理（不阻塞等名额，避免与子分支互等）。默认「不限制」，设置-任务设置可选 4/8/16/32；选上限后并行、异步与嵌套分支共用同一名额；任务池同步调到 48 线程 / 32 队列。
- 线程命名：池线程 `tt-aux-N` / `tt-pool-N`，运行期线程名 `tt-task-标题` / `tt-branch-标题`，结束后还原；核心线程允许超时回收。
- 可观测性：线程池活跃/排队/拒绝/排队等待、异步句柄、锁与计数器汇总到「设置 → 长按版本号 → 线程状态」弹窗；并行卡写「启动/完成/未启动/停止/结果（超时/任务停止/达成完成数）+ 提交/总用时」，异步卡记句柄、运行时长与取消原因。
- 日志并发：`LogInfo.children` 与 `LogSaver` 监听集合改为并发安全，分支线程挂子节点与主线程重写父节点不再互相丢项。
- 体验：蓝图里并行/异步卡右上角显示「运行中 N」；根运行停止 200ms 后仍有分支未收尾时记一条「强制放弃 N 个分支」。
- 边界：并行卡超时设为 0 表示一直等；分支停止是协作式的，忽略中断的动作只能靠停止任务收场。
- 待真机验证：超时收尾后的日志树结构、开启分支上限后现有任务是否够用、卡片角标位置（顶部针脚多的卡片可能与角标重叠）。

## 44. 清理缓存对话框崩溃与「分享到」多出的线程选项

- 位置：`ui/setting/SettingView.java`、`res/values/string_array.xml`
- 崩溃：`R.array.cache_dir_name` 里多了一项「线程」（线程分组那笔提交顺带加的），与 `SettingView` 实际清理的三个缓存目录（log/task/document）数量对不上。`setMultiChoiceItems` 的条目与勾选数组长度不一致，AppCompat 在多选列表测量时按条目下标取勾选状态，`AlertController$AlertParams$1.getView` 越界崩溃（`length=3; index=3`），打开「清理缓存」必现。
- 处理：删掉多余的「线程」；对话框的条目、勾选、文件数组统一按两个列表的较短长度构造，长度不一致时记一条日志，以后再加名字也不会崩。
- 「分享到」：`R.array.share_type` 同样被加了「线程」，但 `ShareToAction` 只按 index 0（文字）/1（文件）分发，选第三项会发出没有 type 的 SEND intent；已删除该项。
- 旧数据：`PinSingleSelect.getIndex()` 找不到原值时回落到 0（文字），旧任务里误选过的「线程」不会再走空 intent。

## 45. 蓝图任务菜单改为卡片弹窗

- 位置：`ui/blueprint/BlueprintView.java`、`res/layout/popup_blueprint_menu.xml`（替换 `res/menu/menu_blueprint.xml`）
- 蓝图顶栏「⋯」不再用系统 `PopupMenu`，改成与设置页右上角菜单同款的卡片弹窗：圆角卡片、带图标的「任务日志 / 任务截图」行、分隔线，以及「记录日志细节 / 辅助工具」开关行（原来是对勾菜单项）。
- 行为不变：任务日志打开根任务的日志浮窗；记录日志细节只对顶层任务显示、勾选后保存任务；任务截图与辅助工具保持原逻辑。开关行的整行点击也能切换。
- 弹窗内容宽 200dp，开关行的长文字不会被压成省略号。

## 46. 系统辅助：一键开关点按反馈与指针位置

- 位置：`ui/setting/SettingView.java`、`res/layout/view_setting.xml`、`res/layout-land/view_setting.xml`、`res/values/strings.xml`
- 设置页在「应用设置」与「权限设置/任务设置」之间新增「系统辅助」分组，两项开关对应系统「开发者选项」里的「显示点按操作反馈」与「指针位置」；开关状态直接读 `Settings.System` 的 `show_touches` / `pointer_location`。
- 有 Root/Shizuku 时一键执行 `settings put system <key> 0/1` 并回读系统状态，Toast 提示已开启/已关闭；没有超级用户时回滚开关，弹窗给出可复制的 adb 命令。
- 回到设置页（`onResume`）会重新同步两个开关，避免与系统设置里改过的状态不一致。
- 没有超级用户时的兜底：`ShizukuSuperUser.init()` 在 Shizuku 没运行时会立即回报失败（此前一直等不到回调，开关会停在点击后的假状态），另外按钮点击后 3 秒没有结果也会按失败弹出命令框。
- 真机验证：Root 模式下开关一次生效（`show_touches` / `pointer_location` 都变成 1，顶部出现指针坐标叠层），关闭后恢复 0；「不启用」模式下点击只弹出可复制命令的对话框，开关回滚。

## 47. 横屏设置页缺少行导致的空指针崩溃

- 位置：`res/layout-land/view_setting.xml`
- 原因：设置页有两套布局，`SettingView` 用 `ViewSettingBinding` 绑定所有行；横屏布局一直缺少 `fileAccessSwitch`（上游就有）和 `branchLimitSelect`（线程分组时只加到了竖屏），横屏创建页面时对应字段为 null，`onCreateView` 直接空指针崩溃。
- 处理：横屏布局补上「文件访问权限」与「并行分支上限」两行，位置与竖屏一致。
- 真机验证：强制横屏启动后进入设置页不再崩溃，两行都能正常显示与操作；竖屏不受影响。
- 待真机验证：横屏下其它行的功能性（本次只确认渲染与绑定正常）。

## 48. 自动重启无障碍与开机自启移到权限设置

- 位置：`ui/setting/SettingView.java`、`res/layout/popup_setting_menu.xml`、`res/layout/view_setting.xml`、`res/layout-land/view_setting.xml`
- 设置页右上角菜单里的两个开关行（自动重启无障碍、开机自启）移到设置页「权限设置」分组末尾，竖屏/横屏布局同步加了对应行；菜单只剩帮助文档、视频教程、录屏/受限设置命令与日志入口。
- 行为不变：仍读写 `SettingSaver.APP_AUTO_RESTART_SERVICE` / `APP_AUTO_START`，开启时弹原来的说明对话框；控件换成与其它行一致的 `SettingWidgetSwitch`，整行可点。
- 真机验证：两行在权限设置里显示、切换正常，开启弹原说明框；重启应用后状态保持开启；⋯ 菜单不再出现这两项。

## 49. 默认主色调改为黑色

- 位置：`res/values/colors.xml`、`res/values-night/colors.xml`、`res/values/themes.xml`、`bean/save/setting/special/DynamicColor.java`、`res/values/strings.xml`
- `Theme.TouchTool` 直接覆盖 Material3 的 primary/secondary/tertiary 三组颜色：浅色模式是纯黑主色（白字、浅灰容器），夜间模式换成浅灰主色，避免黑底上看不清。
- 动态颜色：默认「纯黑」不再套壁纸/动态取色（Material 的黑色种子会退回默认紫色），直接用上面这套固定黑白配色；自选其它颜色仍按原有动态取色逻辑走。
- 设置页「动态颜色」说明改为「开启后应用将使用自选颜色作为主色调（点击此处自选颜色，默认纯黑）」。
- 真机验证：浅色模式功能启用卡片、开关、按钮、选中态均为黑色；切「总是开启」深色模式后主色变浅灰、文字可读；恢复「跟随系统」回到黑色。

## 50. 获取控件信息卡默认半展开，扩展属性收进全展开

- 位置：`bean/action/node/GetNodeInfoAction.java`、`res/values/task_strings.xml`
- 不再加额外开关，直接用卡片眼睛按钮的三态：不展开只留已连线针脚；半展开显示 控件 / 类名 / 控件ID / 文本 / 描述 / 区域 / 是否可用 / 是否可见 / 控件身份 9 个；全展开显示全部 24 个。
- 16 个扩展属性针脚标记 `hide`，只在全展开或已连线时显示；新建卡片跟随全局「卡片展开状态」设置（全展开就全展开、半展开只显示常用），老卡片保留自己保存的展开状态。
- 顺带把上游标了 `hide` 的 控件ID / 描述 / 是否可用 / 是否可见 / 控件身份 改成半展开也显示，半展开的常用集合与改造前一致。
- 真机验证：新建卡片跟随全局「全展开」显示 24 个输出；点眼睛切到半展开是 8 个输出 + 控件输入，状态随卡片保存。

## 51. 列表 / 字典值对象与派生动作用类型修补（方案 Step 1）

- 位置：`bean/pin/pin_objects/pin_list/PinList.java`、`bean/pin/pin_objects/PinMap.java`、`bean/pin/pin_objects/PinTypeUtil.java`、`bean/action/map/MapGetAction.java`、`MapGetKeysAction.java`、`MapGetValuesAction.java`、`MapAppendAction.java`、`MakeMapAction.java`、`MapSetAction.java`、`bean/action/list/ListSubListAction.java`、`ListAppendAction.java`、`MakeListAction.java`、`ListAddAction.java`、`ListSetAction.java`、`res/values/task_strings.xml`、`res/values/strings.xml`
- 相等判断：`PinList.equals` 改为 `getValueType()` 按内容比较（原来用 `==` 比类型对象，内容相同但类型对象不是同一实例的列表判不相等），`PinMap` 的 `keyType` / `valueType` 同理；两者的 `hashCode`、`isDynamic()` 都加了类型对象为 null 的兜底，反序列化缺字段或写成 null 时按动态类型处理。列表/字典嵌套比较、容器作字典 key 的判定因此变宽松。
- 坏数据：`PinMap` 反序列化按 2 步配对扁平数组，长度不是偶数时丢弃末尾多余项并记一条日志，不再越界崩溃；数组里成对的 null 直接跳过。
- 容器作 key：`PinMap.put` 对 `PinList` / `PinMap` 键先拷贝再放入（`putAll` 也走 `put`），来源内容被改后不再破坏哈希。
- 派生结果类型：`MapGetKeys` / `MapGetValues` 的结果列表分别继承字典的键类型 / 值类型，`ListSubList` 继承源列表的元素类型，`ListAppend` / `MapAppend` 继承第一个非空输入的元素类型 / 键值类型；`MakeList` 重建结果列表时保留元素类型（原实现每次运行都换回动态列表，运行一次类型就丢）。
- `MapGet` 默认值：末尾新增「默认值」输入针脚，未命中时输出默认值，`existPin` 显式给 `false`；默认值针脚追加在针脚列表最后，老任务前四个针脚按原顺序匹配，未设默认值时输出空对象，与原版未命中表现一致。默认值针脚同时在 `getDynamicTypePins` / `getDynamicValueTypePins` 里，字典来源带类型时会跟着定型。
- 软类型校验：新增 `PinTypeUtil` 作为统一入口（元素能否放进声明类型的容器，动态类型一律放行）。`MakeList` / `MakeMap` / `ListAdd` / `ListSet` / `MapSet` 共用它：运行期类型不一致只记一条日志、元素照常放入，卡片 `check()` 追加一条警告，不自动断开连线。默认 warn-only，不改变已有任务的运行结果。
- `PinList.containsAll` 去掉每次 `new HashSet<>` 的分配。
- 兼容：`equals` 放宽只影响容器作 key、容器包含判断与去重，不改变已有运行结果；类型继承只让原本是动态类型的派生结果变具体，下游不兼容时 `check()` 报错但不自动清链。
- 待真机验证：老任务（含新建列表/新建字典、获取所有键、排序、获取值）加载与运行结果不变；列表里包含结构相同的另一个列表时「包含」判断正确；手改任务把字典的 `values` 改成奇数长度后能正常加载；`MapGet` 命中输出值、未命中输出默认值；`MapGetKeys → 遍历列表` 的元素针脚显示具体类型。

## 52. 列表排序的方式选择与遍历快照（方案 Step 2）

- 位置：`bean/action/list/ListSortAction.java`、`ListForeachAction.java`、`bean/action/map/MapForeachAction.java`、`res/values/task_strings.xml`、`res/values/string_array.xml`
- 排序方式：`ListSort` 末尾新增「排序方式」针脚（`R.array.list_sort_mode`：升序 / 降序 / 按字典键 / 自定义比较）与「比较键」针脚，两者都追加在针脚列表最后，老任务前五个针脚仍按原顺序匹配。
- 老任务：反序列化时先看 `tmpPins` 里有没有方式针脚，没有就固定为「自定义比较」，比较流程、针脚 id、连线与排序结果都与旧版一致；新建卡片默认「升序」。
- 快速模式：升序 / 降序按元素自身比较（数字按数值、文本按字符串、布尔 false 在前，其它类型视为相等），不执行子流程，复杂度从「O(n log n) 次子流程」降到纯内存比较；按字典键取元素里指定键的值比较，键为空时记一条日志并保持原顺序。
- 自定义比较加固：用自实现的稳定归并排序替换 `List.sort`，比较器抛异常（含子流程异常）时中止排序、保留原顺序并记日志，不再触发 TimSort 的契约异常；比较次数上限 20000 次，超出即中止并提示。
- 遍历快照：`ListForeach` / `MapForeach` 进入循环时先对容器做一次浅拷贝快照，按快照遍历；循环体内对同一容器增删不再 `ConcurrentModificationException`，也不会跳过元素或错位（元素本身仍是引用，与容器引用语义一致）。
- 兼容：排序卡方式缺省走自定义比较；遍历快照属于行为变更——旧版在循环体内改容器是实时读取（可能跳过元素或崩溃），新版按进入循环时的内容遍历。
- 待真机验证：老任务排序卡仍按自定义比较排；数字/文本列表升降序、字典列表按键排序；比较结果未写或比较器不一致时不崩溃且有日志提示；`ListForeach` / `MapForeach` 循环体内增删同一容器不崩溃；1000 / 10000 元素列表在快速模式与自定义比较下的耗时对比。

## 53. 列表 / 字典常量编辑器与索引提示（方案 Step 3）

- 位置：新增 `ui/blueprint/pin_widget/PinWidgetListValue.java`、`PinWidgetMapValue.java`、`ui/blueprint/selecter/pin_container/PinContainerValueDialog.java`、`bean/pin/pin_objects/PinValueUtil.java`、`res/layout/pin_widget_container_value.xml`、`res/layout/dialog_pin_container_value.xml`、`res/layout/item_pin_value_row.xml`；改动 `bean/pin/PinInfo.java`、`bean/pin/Pin.java`、`bean/action/list/ListAddAction.java`、`res/values/strings.xml`、`res/values/task_strings.xml`
- 注册方式：`PinInfo` 的 `LIST_INFO` / `MAP_INFO` 从「没有编辑器」改为分别挂 `PinWidgetListValue` / `PinWidgetMapValue`，`DYNAMIC_OBJECT_INFO` 保持无编辑器。
- 显示条件：编辑器只在「输入针脚、未连线、值是列表 / 字典」时出现（已连线的输入针脚运行期会被来源值覆盖，输出针脚没有常量语义），其余情况控件隐藏、不占高度。
- 编辑流程：针脚上显示「共 N 项」和一个编辑按钮；点开是弹窗，每行「类型（只读）+ 值输入 + 上移 / 下移 / 删除」，底部「添加」；字典行是键 / 值双列。改的都是副本，点「确定」才写回，取消不改原值。
- 可编辑类型：文本、整数 / 浮点 / 双精度 / 长整数、布尔；单选项、图片、控件、区域等只读，行首的类型后面标「只读」，顶部提示「其它类型只能用动作生成」。
- 字典重复键：确定时若有重复键，弹「覆盖 / 跳过 / 取消」——覆盖保留后面的、跳过保留前面的；键的重复判定用 `PinString` 等值比较。
- 槽位摘要：列表 / 字典的槽位本身只有 18dp，放不下文字，改为在自动生成的针脚标题后补长度（如「文本列表[3]」「文本-数字[2]」），标题随值变化刷新；编辑器行首另显示「共 N 项」。
- 索引提示：`ListGet` / `ListSet` / `ListRemove` 等共用「索引」、`ListAdd` 的「位置」、`ListSubList` 的起止索引标题统一补「(1起，负数倒数)」；`ListAdd` 的位置针脚不再默认隐藏，老任务里存下来的隐藏标记在加载时也一并取消。
- 未做：方案 Step 3 的可选项「针脚长按菜单增加清空 / 复制为常量」。
- 待真机验证：新建列表 / 字典常量 → 增删改 → 确定 → 保存任务 → 重开内容一致；取消后原值不变；已连线输入针脚看不到编辑器；行上移 / 下移 / 删除后确定的结果；字典键重复时覆盖 / 跳过结果；老任务列表 / 字典针脚标题多出的 `[n]` 观感是否可接受；弹窗高度在行多时是否正常滚动。

## 54. 列表 / 字典动作合并（方案 Step 4 前半）

- 位置：新增 `bean/action/list/ListRemoveByAction.java`、`bean/action/map/MapContainByAction.java`、`MapKeysValuesAction.java`；改动 `bean/action/ActionType.java`、`ActionInfo.java`、`ActionMap.java`、`bean/action/list/ListSortAction.java`、`res/values/string_array.xml`、`res/values/task_strings.xml`
- 合并规则（旧类型只从选择面板隐藏，类与 `ActionType` 都保留，老任务照常加载、运行、导出导入）：
  - 「移除索引元素」+「移除元素对象」→ **移除元素**（`LIST_REMOVE_BY`）：方式「按索引 / 按对象」；按索引时复用原来的索引规则，「移除的对象」针脚输出被移除的元素，按对象时不使用该针脚。
  - 「字典是否包含键」+「字典是否包含值」→ **字典是否包含**（`MAP_CONTAIN_BY`）：方式「键 / 值」。元素针脚保持动态类型，避免被单侧键 / 值类型拦住连线。
  - 「获取所有键」+「获取所有值」→ **获取所有键或值**（`MAP_KEYS_VALUES`）：方式「键 / 值」，结果列表继承字典的键类型或值类型（沿用 Step 1 的类型继承）。
  - 「列表翻转」并入**列表排序**的方式（`R.array.list_sort_mode` 末尾追加「反转」，前四个下标不变），`LIST_REVERSE` 卡从面板移除。
- 面板：列表动作 20 → 17、字典 16 → 14；`ActionMap` 的 LIST / MAP 分组列表替换为合并后的类型。
- 版本兼容：合并卡是新 `ActionType`，用旧版本 App 打开含新卡的任务会加载不出该卡；旧卡在新版不受影响。
- 待真机验证：三个合并卡的两种方式各自的运行结果；按索引移除时「移除的对象」输出正确；排序卡「反转」与旧「列表翻转」结果一致；老任务里的旧卡仍能加载、运行、导出再导入；动作选择面板只剩合并后的卡。

## 55. 新增列表 / 字典计算动作（方案 Step 4 后半之一）

- 位置：新增 `bean/action/list/ListDeduplicateAction.java`、`ListStatisticsAction.java`、`ListJoinAction.java`、`bean/action/map/MapSortAction.java`、`MapInvertAction.java`；改动 `bean/action/ActionType.java`、`ActionInfo.java`、`ActionMap.java`、`res/values/string_array.xml`、`res/values/task_strings.xml`
- 列表去重（`LIST_DEDUPLICATE`）：输入列表 +「忽略大小写」，输出结果列表与去重数量；保持首次出现的顺序；忽略大小写只对文本元素生效，其它类型按自身比较（容器的 `equals` 沿用 Step 1 的内容比较）。
- 列表统计（`LIST_STATISTICS`）：方式「求和 / 最大 / 最小 / 平均」，输出统计结果与参与统计的数字个数；非数字元素直接跳过（不计入个数）。
- 列表连接文本（`LIST_JOIN`）：列表 + 分隔符 → 文本，元素逐个 `toString`。
- 字典按键排序（`MAP_SORT`）：方式「升序 / 降序」→ 新字典；键比较按「类型序号 + 值」给出全序（同类型内数字按数值、文本按字符），混合类型也不会触发排序契约异常。
- 字典键值互换（`MAP_INVERT`）：→ 新字典，互换后键类型取原值类型、值类型取原键类型，值需要能作为键。
- 结果容器都继承输入的类型（沿用 Step 1 的类型继承），输出的列表 / 字典在卡片上显示具体元素类型。
- 待真机验证：去重的空列表 / 全重复 / 含数字与文本混合 / 忽略大小写；统计的空列表与全是非数字（结果 0、个数 0）；连接文本的空元素与分隔符为空；按键排序的混合类型键；键值互换后 `MapGet` 命中、原字典不被动过。

## 56. 列表筛选 / 映射 / 查找首个（方案 Step 4 后半之二）

- 位置：新增 `bean/action/list/ListFilterAction.java`、`ListMapAction.java`、`ListFindFirstAction.java`；改动 `bean/action/ActionType.java`、`ActionInfo.java`、`ActionMap.java`、`res/values/task_strings.xml`
- 列表筛选（`LIST_FILTER`）：输入列表，每个元素经「条件」执行针脚跑一次子流程，`条件结果` 针脚为真则保留；结果列表继承源列表的元素类型。按进入循环时的快照遍历，子流程里改列表不会跳过或中断。
- 列表映射（`LIST_MAP`）：每个元素跑一次「映射」子流程，`映射结果` 针脚给出的值组成新列表；结果列表的类型跟随第一个映射出来的值。
- 列表查找首个（`LIST_FIND_FIRST`）：条件为真即停止，输出 `元素`、`索引`（从 1 起）与 `是否存在`；未命中时元素输出空对象、索引 0、是否存在 false。
- 三张卡与 `ListSort` 同一套子流程写法：执行针脚（`PinExecute`）接子流程，结果针脚作为子流程的输入回读；每跑一次子流程不写调试日志（只记外层动作），避免日志树被逐元素撑爆。
- 中断处理：任务停止或子流程跳到别的卡片时立即返回，不再写结果。
- 未做：列表 / 字典转 JSON、JSON 转列表 / 字典、字典转列表（键值对列表）。
- 待真机验证：空列表 / 单元素 / 全不满足条件；条件流程里改列表；映射容器元素（列表里放列表）；查找首个的命中与未命中；子流程里停止任务、跳出、抛异常时的表现。

## 57. 列表字典转 JSON、JSON 解析支持数组、键值对列表（方案 Step 4 后半之三）

- 位置：新增 `bean/action/string/ToJsonAction.java`；改动 `bean/action/ActionInfo.java`、`ActionMap.java`、`bean/action/map/MapKeysValuesAction.java`、`bean/action/string/ParseJsonAction.java`、`res/values/string_array.xml`、`res/values/task_strings.xml`
- 列表字典转 JSON（`TO_JSON`，枚举本来就有但一直没注册）：输入任意对象，列表转数组、字典转对象、数字与布尔按 JSON 基本类型、其它类型按文本；输出文本可被「JSON解析」还原，两者配对。
- JSON 解析补数组：原来 `calculate()` 在结果针脚是动态类型时只处理 `{...}`，`[...]` 解析不出任何东西（`sync()` 却两种都处理）；改为按开头字符决定数组还是对象。
- 字典转列表：并入「获取所有键或值」的第三种方式「键值对」，每个元素是一个只含一对键值的字典，结果列表的元素类型是「键-值」字典；不新增独立动作，避免和键 / 值列表重复。
- 版本兼容：`TO_JSON` 是本次新注册的动作类型，旧版本 App 打不开含该卡的任务；`PARSE_JSON` / `MAP_KEYS_VALUES` 的针脚与类型没变，旧任务照常加载。
- 待真机验证：容器 → JSON → 解析 JSON 的往返（含嵌套列表 / 字典、数字、布尔、空容器）；旧任务里「JSON解析」卡原来对 `[...]` 解析为空，现在会得到列表；键值对列表的遍历与 `MapGet` 取值。

## 58. 真机测试发现的两处修正

- 位置：`bean/pin/pin_objects/PinValueUtil.java`、`ui/blueprint/selecter/pin_container/PinContainerValueDialog.java`、新增 `bean/action/list/ListToJsonAction.java`、`bean/action/map/MapToJsonAction.java`、`utils/GsonUtil.java`（`toJsonElement`）；移除 `bean/action/string/ToJsonAction.java`
- **常量编辑器对新列表 / 新字典无效**：列表 / 字典针脚的元素类型默认是「动态」，`isTextEditable` 把动态类型判成不可编辑，行显示「对象（只读）」、值输入被禁用，等于空容器什么也填不了。改为：动态类型按文本处理（新建元素是 `PinString`、可直接输入），行首类型按元素实际类型显示；容器声明的类型仍不改，避免影响连线规则。
- **「列表字典转JSON」连不上容器**：输入针脚是 `PinObject(DYNAMIC)`，而列表 / 字典分别是 LIST / MAP 组，`linkFromAble` 按组判等直接拒绝，等于这张卡收不到列表或字典。拆成 **列表转JSON**（`LIST_TO_JSON`，输入 `PinList`）与 **字典转JSON**（`MAP_TO_JSON`，输入 `PinMap`），转换逻辑抽到 `GsonUtil.toJsonElement`，两张卡分别放在列表 / 字典分组。
- 真机测试记录（Redmi K70 / Android，debug 包）：
  - 老任务加载与蓝图渲染正常（含循环、并行、条件循环的复杂任务），无崩溃、logcat 无应用层异常。
  - 动作选择面板：列表 17 项、字典 14 项，`移除元素` / `字典是否包含` / `获取所有键或值` 已合并，`移除索引元素` / `移除元素对象` / `字典是否包含键` / `字典是否包含值` / `获取所有键` / `获取所有值` / `列表翻转` 不再出现；新增的去重 / 统计 / 连接文本 / 筛选 / 映射 / 查找首个 / 字典按键排序 / 键值互换 / 转 JSON 均正常列出并显示标题与说明。
  - `获取元素` 卡：列表针脚显示「共0项」+ 编辑按钮，索引针脚标题为「索引(1起，负数倒数)」、默认值 -1；点编辑弹出「编辑列表」，空状态、添加行、行内上移 / 下移 / 删除、取消 / 确定均正常。
  - 任务导入可用：手写 .tt（5 个任务，含常量列表、链路连线）导入成功；故意把字典输出接到列表输入的任务在列表页显示「针脚类型不匹配」红条，说明 `check()` 正常。
- 仍未在真机跑通的项（需人工点运行后再看）：连接文本 / 去重 / 统计 / 转 JSON 的运行结果、遍历与排序卡在子流程里的实际表现、编辑器改动保存后重开的一致性；本轮只验证到 UI 与加载层面。

## 59. 对照方案补齐：JSON 转列表、容器长度摘要、README 动作表

- 位置：新增 `bean/action/list/JsonToListAction.java`；改动 `bean/action/ActionType.java`、`ActionInfo.java`、`ActionMap.java`、`ui/blueprint/pin/PinView.java`、`bean/pin/Pin.java`、`res/values/task_strings.xml`、`README.md`
- **JSON 转列表**（`JSON_TO_LIST`）：方案「JSON 转列表/字典，需要类型选择」原来只有 `PARSE_JSON` 一张卡，它的结果针脚声明为字典，解析数组时值变成列表、声明类型却还是字典（真机导入测试里下游连列表会被判「针脚类型不匹配」）。新增「JSON转列表」：输入 JSON 文本、输出列表，与「列表转JSON」配对；`PARSE_JSON` 保持字典结果，数组仍能解析但建议用新卡。
- **容器长度摘要**改为方案里的写法：列表标题后 `[n]`、字典标题后 `{n}`，统一在 `PinView.refreshPin` 里按针脚当前值追加（空容器不显示），不再只对自动生成标题的针脚生效；`Pin.getTitle()` 里原来的 `[n]` 追加撤掉，避免重复。针脚槽位本身只有 18dp，仍然放不下文字，所以计数落在标题上。
- **README 动作表同步**（方案 §9.6）：列表 / 字典两张表按当前面板重写——并入「移除元素」「字典是否包含」「获取所有键或值」「排序方式里的反转」，补上去重 / 统计 / 连接文本 / 筛选 / 映射 / 查找首个 / 列表转JSON / JSON转列表 / 字典按键排序 / 键值互换 / 字典转JSON，并注明旧卡只在老任务保留。
- 遗留（与方案的偏差或后续项）：
  - 编辑器的值输入是自建文本行 + `cast()` 回写，没有抽用 `PinWidgetString/Number/Boolean` 的编辑控件；单选项元素仍只读。
  - 索引规则写进针脚标题（`索引(1起，负数倒数)`），没有走 `Pin.description`——该字段界面上没有显示位置。
  - 方案 Step 3 可选项「针脚长按菜单增加清空 / 复制为常量」未做。
  - 排序「按字典键」只有升序一种方向（方案里该方式本身没区分升降序）。

## 60. 收尾：索引提示走针脚说明、编辑器清空、按键降序

- 位置：`bean/pin/Pin.java`（新增 `withIndexTip`）、`ui/blueprint/pin/PinView.java`、`res/values/strings.xml`、`res/values/task_strings.xml`、`res/layout/dialog_pin_container_value.xml`、`ui/blueprint/selecter/pin_container/PinContainerValueDialog.java`、`res/values/string_array.xml`、`bean/action/list/ListSortAction.java`，以及 `ListGetAction` / `ListSetAction` / `ListAddAction` / `ListRemoveByAction` / `ListSubListAction` / `ListForeachAction` 的索引针脚
- **索引规则改走 `Pin.description`**：`PinView.refreshPin` 把针脚说明挂到标题的长按提示（`setTooltipText`），索引针脚标题还原成「索引 / 位置 / 起始索引 / 结束索引」，规则文字「从 1 开始，负数表示从末尾数，0 等同 1」由 `Pin.withIndexTip` 统一写进说明。卡片不再被长标题撑宽，说明也有了显示入口。
- **编辑器加「清空」**：与「添加」并排，只清编辑副本，点「取消」仍然不改原值。方案里的「复制为常量」由针脚自带的复制 / 粘贴按钮承担，没有另外做长按菜单。
- **排序方式追加「按键降序」**：数组末尾新增（前 5 个下标不变，老任务不受影响），`sortByKey` 增加方向参数，「按字典键」走升序、「按键降序」走降序。
- 仍未收的：编辑器值输入仍是文本行 + `cast()`，单选项元素只读；容器长度 `[n]`/`{n}` 在针脚标题上而不是 18dp 槽位内；真机运行结果（连接文本 / 去重 / 统计 / 转JSON / 筛选等）尚未实际验证。









