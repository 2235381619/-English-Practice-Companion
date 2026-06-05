# AI English Practice Companion — 开发计划

## 项目概述

AI 英语口语陪练工具。用户选择场景（面试/点餐/会议），通过麦克风进行英语对话练习，系统实时语音识别 → 语法纠错 → 表达建议 → AI 语音回复，课后生成学习报告。

## 技术栈

- **后端**: Java 17, Spring Boot 3.4.3, Spring AI 1.1.7, Google ADK 0.4.0
- **前端**: Vue 3 + Vite
- **ASR**: Whisper-1 (OpenAI API)
- **TTS**: edge-tts (微软 Edge 免费语音引擎)
- **评估 LLM**: GPT-4o-mini via ADK EvaluatorAgent
- **音频捕获**: `javax.sound.sampled.TargetDataLine`（Java 原生）
- **VAD**: 能量检测（简单 RMS 阈值，800 threshold）
- **构建**: Maven

## 当前进度（2026-06-05）

### 已完成 ✅

1. **WASAPI 系统音频捕获** — `capture.exe`（C# 控制台程序，零依赖）
   - 直接调用 Windows WASAPI COM 接口，以 `AUDCLNT_STREAMFLAGS_LOOPBACK` 模式捕获系统音频输出
   - 输出 48kHz 2ch 32-bit float WAV / 管道模式输出 raw PCM
   - 位置: `projects/capture/`（但 capture.exe 在项目根目录）

2. **同声传译 Demo** — `AudioTranslateDemo.java` / `StreamingTranslateDemo.java`
   - 分片流式捕获 + 后台线程 + Whisper ASR + GPT 翻译
   - 已停产，转为口语陪练方向

3. **口语陪练 Demo** — `EnglishTutorDemo.java`
   - VAD 录音（能量检测，800 threshold，0.8s 静音超时）
   - 麦克风自动选择（优先 PRO X）
   - Whisper ASR → GPT 评测 → edge-tts 语音播报
   - TTS 引擎后台常驻（单 Python 进程，stdin/stdout 通信，4字节size头+MP3数据协议）
   - 回放验证功能（可去掉）
   - 场景选择（面试/点餐/会议）

4. **domain translate 包基础** — 已有：
   - `SegmentBuffer` — 环形缓冲区
   - `TranslationSession` — 翻译会话
   - `TranscriberAgent / TranslatorAgent` — 适配器接口
   - `TranslationServiceImpl` — 服务实现
   - 说明: 这些是为同传设计的，口语陪练需新建 `domain/practice/` 包

### DDD 架构设计

```
ai-agent-scaffold-lite/
├── ai-agent-scaffold-lite-api/           # API 接口 & DTO
│   └── src/main/java/cn/bugstack/ai/api/practice/
│       ├── PracticeController.java       ← API 接口定义
│       └── dto/                           ← 请求/响应对象
│
├── ai-agent-scaffold-lite-domain/        # 领域层 — 纯功能实现
  │   └── src/main/java/cn/bugstack/ai/domain/practice/
  │       ├── model/
  │       │   ├── entity/                   ← PracticeSession, ConversationRound
  │       │   └── valobj/                   ← Scenario, EvaluationResult, SessionReport
  │       ├── service/impl/
  │       │   ├── AudioService.java         ← VAD + ASR（纯功能）
  │       │   ├── EvaluationService.java    ← GPT 评测（纯功能）
  │       │   └── TtsService.java           ← edge-tts 合成（纯功能）
  │       ├── agent/
  │       │   └── EvaluatorAgent.java       ← 评测 Agent 接口
  │       └── adapter/
  │           ├── IAudioRepository.java     ← 音频存储接口
  │           └── ISessionRepository.java   ← 会话持久化接口
  │
  ├── ai-agent-scaffold-lite-case/          # 用例层 — 业务编排
  │   └── src/main/java/cn/bugstack/ai/usecase/practice/
  │       ├── IPracticeService.java         ← 练习总入口接口
  │       └── PracticeServiceImpl.java      ← 编排 Audio/Evaluation/TTS + 会话管理
  │
  ├── ai-agent-scaffold-lite-trigger/       # 触发层
│   └── src/main/java/cn/bugstack/ai/trigger/
│       ├── http/PracticeController.java  ← REST 接口
│       └── listener/PracticeWebSocket.java ← WebSocket 音频流
│
├── ai-agent-scaffold-lite-infrastructure/ # 基础设施层
│   └── src/main/java/cn/bugstack/ai/infrastructure/
│       ├── repository/SessionRepositoryImpl.java
│       └── file/AudioFileRepository.java
│
├── ai-agent-scaffold-lite-app/           # 应用层
│   └── src/main/resources/agent/practice-agent.yml  ← Agent 装配配置
│
└── ai-agent-scaffold-lite-frontend/      # Vue 3 前端
    └── src/
        ├── views/PracticeView.vue        ← 练习主页面
        ├── components/                   ← 场景选择、对话、评测组件
        └── api/practice.js               ← API 调用
```

### 关键设计决策

| 决策 | 方案 | 原因 |
|------|------|------|
| ASR 与 Agent 模型分离 | ASR 走 `OpenAiAudioApi`，Agent 走 `OpenAiApi` + ADK | 不同 endpoint、不同模型，不强行耦合 |
| 不走规则树 | Agent 不定义 workflow，直接 `runner: agent-name` | 音频 + TTS 是纯 Java 非 LLM 环节，不适合链式编排 |
| TTS 引擎 | 常驻 Python edge-tts 进程，stdin/stdout 通信 | 避免每次重启 Python，延迟降低 ~0.5s |
| 音频捕获 | 前端 `MediaRecorder` → WebSocket → 后端 | Web 应用不能直接用 Java Sound API，需浏览器捕获 |
| VAD | 简单能量检测（RMS threshold 800） | 够用，避免引入第三方 VAD 库的复杂度 |

### 待办清单

#### Phase 1: 项目骨架搭建

- [x] ~~初始化项目结构~~（脚手架已有，需整理）
- [x] ~~设计整体架构~~（已完成）
- [x] **创建 domain/practice 包**
  - [x] `model/entity/PracticeSession.java`
  - [x] `model/entity/ConversationRound.java`
  - [x] `model/valobj/Scenario.java`（枚举：INTERVIEW / RESTAURANT / MEETING）
  - [x] `model/valobj/EvaluationResult.java`（纠错 + 表达建议 + 评分）
  - [x] `model/valobj/SessionReport.java`（课后总结报告）
- [x] **创建 domain/practice/service**
  - [x] `IPracticeService.java` 接口
  - [x] `PracticeServiceImpl.java` — 编排主流程
  - [x] `AudioService.java` — VAD + ASR（当前在 Demo 里，搬到 domain）
  - [x] `EvaluationService.java` — GPT 评测（当前在 Demo 里，搬到 domain）
  - [x] `TtsService.java` — edge-tts 后台引擎（当前在 Demo 里，搬到 domain）
- [x] **创建 domain/practice/agent**
  - [x] `EvaluatorAgent.java` — ADK 装配的 LLM 评估 Agent
- [x] **创建 domain/practice/adapter**
  - [x] `IAudioRepository.java`
  - [x] `ISessionRepository.java`

#### Phase 2: API + Trigger 层

- [ ] **api 层**
  - [ ] `api/practice/dto/CreateSessionRequest.java`
  - [ ] `api/practice/dto/CreateSessionResponse.java`
  - [ ] `api/practice/dto/SubmitAudioResponse.java`
  - [ ] `api/practice/dto/SessionReportResponse.java`
  - [ ] `api/practice/dto/ScenarioListResponse.java`
- [ ] **trigger 层**
  - [ ] `trigger/http/PracticeController.java` — REST: `POST /session`, `GET /session/{id}`, `GET /scenarios`
  - [ ] `trigger/listener/PracticeWebSocket.java` — WebSocket: 音频流对接
- [ ] **Agent YAML 配置**
  - [ ] `app/src/main/resources/agent/practice-agent.yml`
  - [ ] `app/src/main/resources/application-dev.yml` 引入 practice-agent.yml
  - [ ] 添加 ASR 和 TTS 配置到 application.yml

#### Phase 3: Infrastructure + 前端

- [ ] **infrastructure 层**
  - [ ] `SessionRepositoryImpl.java`
  - [ ] `AudioFileRepository.java`
  - [ ] MySQL 建表 SQL
- [ ] **前端（Vue 3）**
  - [ ] `PracticeView.vue` — 练习主页面
  - [ ] `ScenarioSelector.vue` — 场景选择组件
  - [ ] `ConversationPanel.vue` — 对话展示组件
  - [ ] `EvaluationPanel.vue` — 评测结果组件
  - [ ] `api/practice.js` — 前端 API 调用
  - [ ] WebSocket 音频流对接

#### Phase 4: 功能打磨

- [ ] 用户系统（登录/注册/历史记录）
- [ ] 发音评测（phoneme-level 反馈）
- [ ] 词汇多样性分析
- [ ] 流利度评分（语速、停顿频率）
- [ ] 课后总结报告（错误统计、建议词库、进步曲线）
- [ ] TTS 多音色选择（男声/女声/语速调节）
- [ ] 对话中断检测（用户插话时暂停 AI）

#### Phase 5: 部署

- [ ] Dockerfile
- [ ] docker-compose（后端 + MySQL + 前端）
- [ ] Nginx 配置
- [ ] CI/CD 流程

### 启动方式

**后端:**
```bash
mvn spring-boot:run -pl ai-agent-scaffold-lite-app
```

**Demo 测试（IDEA）:**
```bash
# 口语陪练 Demo
mvn test -Dtest=EnglishTutorDemo -DfailIfNoTests=false

# 同传 Demo（已停产）
# mvn test -Dtest=AudioTranslateDemo -DfailIfNoTests=false
```

### 新对话恢复指南

如果新建对话后需要继续，告诉 AI：
1. 项目位于 `C:\code\ai-agent-scaffold-lite`
2. 当前进度：口语陪练 Phase 1 骨架搭建阶段
3. 需要先读 `PLAN.md` 和 `README.md`
4. 关键代码入口：`domain/translate/`（已有基础）、`test/.../EnglishTutorDemo.java`（Demo 代码待迁移）
5. 核心待办：创建 `domain/practice/` 包，迁移 Demo 逻辑到正规 Service
6. 架构文档：本文档
7. 同传相关代码可忽略（`SegmentBuffer`, `TranslationSession`, `TranslationServiceImpl` 等）


