# Photo Border App

## 项目概述
基于JSON模板配置的摄影边框APP，支持像素无损处理和多品牌LOGO。

## 功能特性
- **JSON模板驱动**：通过AI对话生成模板，导入APP即可使用
- **像素无损**：JPEG quality=100输出，保留原始像素
- **多品牌支持**：Sony、Nikon、Fujifilm、Canon、Panasonic、Olympus
- **自适应缩放**：基于3840×2560基准分辨率自动调整
- **30款字体**：内置丰富字体选择
- **批量处理**：一次最多处理100张图片
- **实时预览**：选择图片后显示预览

## 项目结构
```
PhotoBorderApp/
├── app/src/main/
│   ├── java/com/zzeng/photoborder/
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   ├── model/          # 数据模型（Template、ExifData等）
│   │   │   └── exif/           # EXIF读取（ExifReader）
│   │   ├── engine/
│   │   │   ├── BorderEngine.kt      # 边框渲染引擎
│   │   │   ├── TemplateManager.kt   # 模板管理（加载/保存/导入）
│   │   │   └── ExifFormatter.kt     # EXIF格式化
│   │   └── ui/
│   │       ├── screens/        # 界面（MainScreen）
│   │       └── theme/          # 主题
│   ├── assets/
│   │   ├── templates/          # 边框模板JSON
│   │   └── logos/              # 品牌LOGO
│   └── res/                    # 资源文件
├── .github/workflows/build.yml # GitHub Actions自动编译
└── build.gradle.kts
```

## 使用方法

### 1. 本地编译（推荐）
**前提条件：**
- 安装 [Android Studio](https://developer.android.com/studio)
- 安装 JDK 17+
- 有网络连接（下载依赖）

**步骤：**
1. 把 `PhotoBorderApp/` 文件夹复制到你的电脑
2. 用 Android Studio 打开
3. 点击 **Sync Project with Gradle Files**（同步Gradle）
4. 连接手机（开启USB调试）或启动模拟器
5. 点击 **Run** 按钮（绿色三角形）

### 2. GitHub Actions自动编译
1. 在GitHub创建新仓库
2. 上传代码
3. 进入 Actions 页面
4. 下载编译好的APK

## 模板格式
模板为JSON格式，参考 `app/src/main/assets/templates/zzeng_minimal.json`

### AI对话增加模板流程：
1. 描述想要的边框样式（如"我要一个黑色背景、金色文字的胶片风边框"）
2. 生成对应的JSON配置文件
3. 把JSON文件放入APP的 `templates/` 目录
4. APP自动识别并可用

## 开发计划
- [x] 项目初始化
- [x] EXIF读取模块
- [x] 像素无损图像处理
- [x] JSON模板系统
- [x] 品牌LOGO管理
- [x] 基础UI界面
- [x] 批量处理功能
- [x] 模板预览
- [x] GitHub Actions自动编译
- [ ] 字体选择UI（需完善）
- [ ] 真实品牌LOGO替换（Fujifilm/Canon等当前为文字版）

## 技术栈
- Kotlin + Jetpack Compose
- metadata-extractor（EXIF读取）
- Gson（JSON解析）
- Coil（图片加载）

## 版本历史
- v1.0 - 初始版本，支持基础边框渲染和批量处理
