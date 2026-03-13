# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

RestfulToolkit-fix 是一个 IntelliJ IDEA 插件，用于 RESTful 服务开发。主要功能：
- URL 跳转到方法定义 (Ctrl+\ 或 Ctrl+Alt+N)
- RESTful 服务树形视图工具窗口
- 支持 Spring MVC/Boot、JAX-RS 和 Feign Client
- 支持 Java 和 Kotlin 语言

## 构建命令

```bash
# 构建插件
./gradlew build

# 启动带插件的 IDE 进行测试
./gradlew runIde

# 清理构建
./gradlew clean build
```

## 架构说明

### 服务解析器 (核心模式)

插件使用解析器链模式来发现 REST 服务：

```
ServiceResolver (接口)
    └── BaseServiceResolver (抽象基类)
            ├── SpringResolver    - 解析 @Controller/@RestController
            ├── FeignResolver     - 解析 @FeignClient 接口
            └── JaxrsResolver     - 解析 JAX-RS 注解
```

- `ServiceHelper.buildRestServiceProjectListUsingResolver()` 协调所有模块的解析
- 每个解析器使用 `JavaAnnotationIndex` 查找带注解的类，然后提取请求映射
- 解析器返回 `RestServiceItem` 对象，包含方法、路径和 PSI 元素引用

### 导航器 UI 组件

```
RestServicesNavigator (ProjectComponent)
    ├── RestServicesNavigatorPanel (UI 面板)
    ├── RestServiceProjectsManager (数据提供者)
    └── RestServiceStructure (树模型)
            ├── RootNode
            ├── ProjectNode (每个模块)
            └── ServiceNode (每个端点)
```

关键初始化流程：
1. `RestServicesNavigator.initComponent()` 在项目打开时被调用
2. `initToolWindow()` 创建工具窗口并注册面板
3. `scheduleStructureUpdate()` 触发数据刷新
4. `RestServiceProjectsManager.getServiceProjects()` 通过解析器收集所有服务

### 关键文件

| 路径 | 用途 |
|------|------|
| `src/main/resources/META-INF/plugin.xml` | 插件配置、动作、扩展点 |
| `src/main/java/com/zhaow/restful/common/resolver/` | 服务发现逻辑 |
| `src/main/java/com/zhaow/restful/navigator/` | 工具窗口和树形 UI |
| `src/main/java/com/zhaow/restful/navigation/action/` | URL 导航和搜索 |

### 插件配置

- 插件 ID: `me.jinghong.restful.toolkit`
- 目标 IDE: IntelliJ IDEA Ultimate (IU) 2023.3.7
- Java 版本: 17
- 依赖: java, kotlin, spring, spring.boot, properties, yaml

## 调试提示

- 插件日志写入目标项目的 `.idea/restful-toolkit-fix/logs/` 目录
- `PluginLogger` 类负责将日志写入项目特定的日志文件
- 使用 `./gradlew runIde` 启动沙箱 IDE 加载插件进行测试
