# 租房评估（House Assessment）

本仓库现在同时包含原有的移动端 Web 版本，以及可直接构建 APK 的原生 Android 版本。Android App 面向现场看房使用：所有评估项都允许留空，房源可以随时保存、继续编辑或删除；系统只根据已经填写的项目，自动生成 1～5 分综合评价、优缺点短评、风险提示和评分可信度。

## Android App 已实现功能

- 房源列表、新增、编辑、删除
- 月租金、目标预算、押金、额外费用、面积、楼层等基础信息
- 多张现场照片：直接拍照或从相册选择，最多 8 张
- 自动获取当前位置并反向填写地址
- 手动输入地址
- 无需 Google Maps API Key 的 OpenStreetMap 地图选点
- 指南针读取朝向，并支持手动选择八方位
- 联系人姓名、电话、微信 / LINE、沟通备注
- 可留空的详细看房指标：
  - 独立阳台、水质、空调能耗、洗衣机
  - 空间布局、插座、噪音、采光、通风
  - 卫生、潮湿霉菌、卫生间、厨房、家具家电、收纳、网络
  - 电梯、宠物、门锁消防与周边安全、交通、通勤、周边配套
  - 合同与房源可信度
- 初次记录时间与最后编辑时间
- 离线 SQLite 数据库存储
- 自动综合评分：
  - 1.0～5.0 分
  - 推荐等级与一句话短评
  - 最多 3 项主要优点和缺点
  - 待确认项目与评分可信度
  - 潮湿霉菌、安全或合同存在严重风险时自动提示并限制最高分

## 下载与安装 APK

每次提交到 `main` 分支后，GitHub Actions 会自动构建一个可直接安装的 Debug APK，并上传为 `house-assessment-apk` 构建产物。

1. 打开仓库的 **Actions** 页面。
2. 进入最新的 **Build Android APK** 成功任务。
3. 在页面底部下载 `house-assessment-apk`。
4. 解压后安装 `app-debug.apk`。

Debug APK 使用 Android 调试签名，可直接安装测试；正式上架前应改用自己的 Release 签名。

## Android 技术栈

- Kotlin
- Jetpack Compose + Material 3
- SQLiteOpenHelper
- Google Play Services Location
- Android Photo Picker / Camera Activity Result API
- Android Rotation Vector Sensor
- WebView + Leaflet + OpenStreetMap
- GitHub Actions

## Android 本地构建

要求 JDK 17、Android SDK 34 和 Gradle 8.7：

```bash
gradle :app:assembleDebug
```

APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Web 版本

原有 React / Vite Web 版本仍保留在仓库中，可使用以下命令运行：

```bash
npm install
npm run dev
```

## 隐私说明

Android 版房源数据和复制后的照片默认只保存在设备本地。定位只在用户主动点击“当前位置”并授权后使用。地图瓦片和地图脚本由 OpenStreetMap / Leaflet 在线加载，反向地址解析由设备的 Android Geocoder 提供。
