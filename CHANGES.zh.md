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

### 8. 任务菜单改为带图标与开关的弹出面板

- 位置：`ui/blueprint/BlueprintView.java`（`showTaskMenu()`）、`res/layout/dialog_blueprint_menu.xml`
- 原先是 `androidx.appcompat.widget.PopupMenu`：四项纯文字、字号偏大、右侧是方框勾选，观感与全局对话框不一致。
- 改为 `PopupWindow` + `MaterialCardView` 承载的自定义面板：贴住顶栏「⋯」按钮下方、右边缘对齐，无遮罩、点外部自动关闭，位置与原弹出菜单一致。
- 每行「图标 + 标题」，两个开关项（记录日志细节 / 辅助工具）用 `MaterialSwitch` 直接反映状态，行高统一 56dp。
- 「记录日志细节」仍只在顶层任务显示；「任务截图」的预览/保存/分享流程保持不变（抽成 `showTaskCapture()`）。
- 原来的 `res/menu/menu_blueprint.xml` 已无引用，一并删除。

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

---

## 附录：本分支提交

```
feat(port): 移植 mod 版的日期「今天」按钮与受限设置引导
feat(port): 移植 mod 版的数学公式全屏编辑与常用函数插入
feat(port): 移植 mod 版的图片识别范围与复制区域
feat(port): 移植 mod 版的蓝图辅助工具（缩放条、四边跳转、结果计数）
feat(port): 移植 mod 版的连线跳转与跳回
feat(port): 移植 mod 版的卡片组暂存
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
fix(share): 分享统一走系统分享面板并修正 MIME 类型
```
