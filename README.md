# 极流桌面 (JiLiu Launcher)

国潮风格Android车机桌面应用

## 项目简介

极流桌面是一款专为Android车机和手机设计的桌面启动器，采用独特的国潮风格设计语言，带来视觉与功能的双重体验。

## 主要特性

### 🎨 国潮风格设计
- 深色背景配金色/红色点缀
- 圆角卡片设计
- 流畅过渡动画
- 适配横屏(车机)与竖屏(手机)

### 📱 核心功能
- **双主页模式**: 主页一(简洁) / 主页二(丰富)
- **自由布局**: 长按拖拽调整插件位置
- **悬浮音乐胶囊**: 全局音乐控制浮窗
- **悬浮Dock栏**: 底部快捷应用栏
- **边缘手势**: 屏幕边缘滑动触发快捷操作

### 💎 会员系统
- 激活码验证机制
- VIP特权功能解锁
- 会员到期提醒

### 🎬 在线壁纸
- 壁纸分类浏览
- 壁纸下载与预览
- 自动更换壁纸

### 📁 文件管理
- 本地文件浏览
- USB设备文件访问
- 文件搜索、复制、移动、删除
- 文件分类(图片、视频、音乐、文档)

### 📦 应用管理
- 应用列表展示
- 应用卸载
- 应用信息查看
- 应用分组

### ⚙️ 原生设置入口
- 快速跳转系统设置
- 常用设置快捷开关

### 🎵 视频播放器
- 本地视频播放
- USB设备视频播放
- 本地音乐播放
- 播放列表管理
- 字幕支持

## 技术栈

- **语言**: Kotlin 1.9.22
- **构建工具**: Gradle 8.11.1
- **目标平台**: Android 7.0+ (API 24+)
- **核心框架**:
  - AndroidX 组件库
  - ViewBinding + ViewModel
  - Coroutines + Flow
  - Material Design 3
  - Media3 (ExoPlayer)
  - Android Car API

## 项目结构

```
极流桌面/
├── app/
│   ├── src/main/java/com/jiliu/launcher/
│   │   ├── ui/                    # UI层
│   │   │   ├── main/              # 主桌面
│   │   │   ├── home1/             # 主页一(简洁)
│   │   │   ├── home2/             # 主页二(丰富)
│   │   │   ├── dock/              # Dock栏
│   │   │   ├── member/            # 会员中心
│   │   │   ├── wallpaper/         # 壁纸
│   │   │   ├── filemanager/       # 文件管理
│   │   │   ├── appmanager/        # 应用管理
│   │   │   ├── settings/          # 设置
│   │   │   └── player/            # 播放器
│   │   ├── viewmodel/             # ViewModel
│   │   ├── model/                 # 数据模型
│   │   ├── repository/            # 数据仓库
│   │   ├── service/               # 后台服务
│   │   ├── receiver/              # 广播接收器
│   │   └── util/                  # 工具类
│   └── src/main/res/
│       ├── layout/                # 布局文件
│       ├── drawable/              # 图标资源
│       ├── values/                # 配置文件
│       └── xml/                   # XML配置
├── build.gradle.kts               # 构建配置
└── README.md                      # 项目文档
```

## 编译构建

### 环境要求
- Android SDK 34
- JDK 17+
- Gradle 8.11.1

### 本地构建

```bash
# 克隆项目
git clone https://github.com/qq00150610-cpu/jiliu-launcher.git

# 进入项目目录
cd jiliu-launcher

# 同步Gradle
./gradlew sync

# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK
./gradlew assembleRelease
```

### GitHub Actions 自动构建

项目已配置GitHub Actions，每次推送到main分支会自动构建APK。

构建产物会通过 nightly.link 提供下载。

## 安装使用

1. 下载最新APK
2. 允许安装来自未知来源的应用
3. 安装APK
4. 将极流桌面设置为默认启动器
5. 首次使用需要授予必要权限

## 权限说明

| 权限 | 用途 |
|------|------|
| SYSTEM_ALERT_WINDOW | 悬浮窗功能 |
| READ_EXTERNAL_STORAGE | 读取存储文件 |
| WRITE_EXTERNAL_STORAGE | 写入存储文件 |
| BIND_ACCESSIBILITY_SERVICE | 全局返回功能 |
| REQUEST_INSTALL_PACKAGES | 安装应用 |
| SET_WALLPAPER | 设置壁纸 |
| CAR_* | 车载API功能 |

## 开发说明

### 代码规范
- 遵循Kotlin编码规范
- 使用ViewBinding进行视图绑定
- 使用ViewModel管理UI状态
- 使用Repository模式管理数据

### 主题配色
```kotlin
// 主色调 - 金色
primaryColor = #D4AF37

// 强调色 - 中国红  
accentColor = #C41E3A

// 背景色
backgroundColor = #1A1A1A
```

## 许可证

本项目仅供学习研究使用，禁止用于商业目的。

## 联系方式

- GitHub: https://github.com/qq00150610-cpu

## 更新日志

### v1.0.0 (2024)
- 初始版本发布
- 实现核心桌面功能
- 双主页模式
- 会员系统
- 在线壁纸
- 文件管理器
- 应用管理器
- 视频播放器
