# AutoDev

<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" width="64px" height="64px" />
</p>

[![Build](https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml/badge.svg)](https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/21520-autodev.svg)](https://plugins.jetbrains.com/plugin/21520-autodev)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/21520-autodev.svg)](https://plugins.jetbrains.com/plugin/21520-autodev)

> AutoDev 是一款高度自动化的 AI 辅助编程工具。AutoDev 能够与您的需求管理系统（例如 Jira、Trello、Github Issue 等）直接对接。在
> IDE 中，您只需简单点击，AutoDev 会根据您的需求自动为您生成代码。您所需做的，仅仅是对生成的代码进行质量检查。

与 GitHub Copilot 差异：

| 特征    | GitHub Copilot    | AutoDev                   |
|-------|-------------------|---------------------------|
| LLM训练 | 经过大模型训练，结果更加准确    | 引入 ChatGPT 或自己训练，结果不是那么准确 |
| 返回结果  | 一次返回多个结果          | 一次返回一个完整的方法               |
| 返回类型  | 返回一个完整的类          | 返回一个完整的方法                 |
| 支持语言  | 支持多种语言            | 仅支持Java                   |
| 支持功能  | 生成代码、生成注释、生成测试用例等 | 生成代码、寻找bug、生成注释等          |
| 开源    | 不开源               | 开源                        |

结论：AutoDev 的唯一优势是开源，GitHub Copilot 完胜。

## Todos

- [X] Languages Support by PSI
    - [x] Java
- [ ] Integration with Co-mate DSL
    - [ ] Assistant architecture DSL
    - [ ] Assistant API Design
- [x] DevTi Protocol
    - [x] format 1: `devti://story/github/1102`
- [ ] Intelli code change
    - [x] Endpoint modify suggestions
    - [x] Controller Suggestion
        - [x] import all common imports
    - [x] Service Suggestion
    - [ ] Repository Suggestion
- [ ] Code AI
    - [x] Generate code
    - [x] Add comments
    - [x] Code Suggestions
    - [x] Find bug...
    - [x] Explain code by selection
    - [x] Trace Exception
- [x] Chat with IDE
    - [ ] Generate test
    - [ ] 实现：`重现 xx 功能`, `devti:/chat/feature`
    - [ ] 重构：`重构 xx 方法`
    - [ ] 替换：`替换 xx 方法`，`devti:/refactor/method`
- [x] Custom LLM Server
- [ ] Context Engineering
    - [x] Related Files
    - [ ] with DependencyContext
        - [ ] parse Gradle for dependencies

## Usage

1. Install
    - method 1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)
    - method 2. Download plugin from release page: [release](https://github.com/unit-mesh/auto-dev/releases) and install
      plugin in your IDE
2. configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `DevTi`

![Token Configure](https://unitmesh.cc/auto-dev/config-token.png)

### CodeCompletion mode

Right click on the code editor, select `AutoDev` -> `CodeCompletion` -> `CodeComplete`

![Code completion](https://unitmesh.cc/auto-dev/completion-mode.png)

### Copilot mode

1. click as you want:

![Copilot Mode](https://unitmesh.cc/auto-dev/copilot-mode.png)

### AutoCRUD mode

1. add `// devti://story/github/1` comments in your code.
2. configure GitHub repository for Run Configuration.
3. click `AutoDev` button in the comments' left.

Run Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/init-instruction.png)

Output Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/blog-controller.png)

### Custom OpenAI proxy example

![Custom Config](https://unitmesh.cc/auto-dev/autodev-config.png)

## Development

1. `git clone https://github.com/unit-mesh/AutoDev.git`
2. open in IntelliJ IDEA
3. `./gradlew runIde`

### LSP Json RPC

```json
{
  "range": {
    "start": {
      "line": 0,
      "column": 0
    },
    "end": {
      "line": 0,
      "column": 0
    }
  },
  "text": "",
  "language": "xxx",
  "uuid": "xxx"
}
```

### API Spec

authorization: `Bearer ${token}`

```json
{
  "auto_complete": {
    "instruction": "补全如下的代码。要求：\n - 直接调用 repository 的方法时，使用 get, find, count, delete, save, update 这类方法。\nService 层应该捕获并处理可能出现的异常。通常情况下，应该将异常转换为应用程序自定义异常并抛出。",
    "input": ""
  },
  "auto_comment": {
    "instruction": "",
    "input": ""
  },
  "code_review": {
    "instruction": "",
    "input": ""
  },
  "refactor": {
    "instruction": "",
    "input": ""
  },
  "write_test": {
    "instruction": "",
    "input": ""
  }
}
```

## License

ChatUI based
on: [https://github.com/Cspeisman/chatgpt-intellij-plugin](https://github.com/Cspeisman/chatgpt-intellij-plugin)

@Thoughtworks AIEE Team. This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
