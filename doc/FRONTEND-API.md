# 口语练习 API — 前端接入说明

## 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/practice/scenario/{sessionId}/{scenarioCode}` | 注册场景 |
| POST | `/api/v1/practice/text` | 文本对话 |
| POST | `/api/v1/practice/session` | 音频对话 |
| GET  | `/api/v1/practice/session/{sessionId}/report` | 会话报告 |
| GET  | `/api/v1/practice/session/{sessionId}/export` | 导出会话记录 |

---

## 1. 注册场景（对话前先调用）

```
POST /api/v1/practice/scenario/{sessionId}/{scenarioCode}
```

无需请求体，路径里直接传 sessionId 和场景名。

场景值：`interview` / `restaurant` / `meeting` / `default`

```javascript
fetch("/api/v1/practice/scenario/abc-123/interview", { method: "POST" })
```

---

## 2. 文本对话

```
POST /api/v1/practice/text
```

```json
{
  "sessionId": "abc-123",
  "text": "Hello!",
  "voice": { "speed": 80, "volume": 90, "pitch": 50 }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | 是 | 会话 ID |
| `text` | string | 是 | 用户说的文本 |
| `voice` | object | 否 | 语音参数 |
| `voice.speed` | int | 否 | 语速 0-100，默认 50 |
| `voice.volume` | int | 否 | 音量 0-100，默认 50 |
| `voice.pitch` | int | 否 | 音高 0-100，默认 50 |

响应：

```json
{
  "code": "0000",
  "data": {
    "asrText":   "hello hello",
    "replyText": "Hello! I'm ready...",
    "audioData": "//UZRAAAAW252Y..."
  }
}
```

播放音频：

```javascript
new Audio("data:audio/mp3;base64," + result.data.audioData).play()
```

---

## 3. 音频对话

```
POST /api/v1/practice/session
```

请求体与文本接口相同，后端先做 ASR 语音识别再回复。

---

## 4. WebSocket 音频输入

WebSocket 收到主响应后请保持连接，等待异步评测结果推送：

```javascript
ws.onmessage = function(ev) {
  var d = JSON.parse(ev.data);
  if (d.type === "evaluation") {
    handleEvaluation(d);
    ws.close(); // 评测结果到齐，可以关WS
  } else {
    handleResult(d);
    // ⚠️ 不要在这里关 ws，等待 type: "evaluation" 消息
    // 设置 10~15s 超时兜底
  }
};
```



```
ws://localhost:8091/practice/audio/{sessionId}
```

1. 连接后持续发送二进制 PCM 音频帧（16kHz 16bit Mono）
2. 发送文本 `"END"` 触发识别 + 回复
3. 服务端返回 JSON（与 HTTP 接口一致）

WebSocket 场景语音参数固定用默认值 50/50/50。

---


## 5. 异步评测结果

评测不阻塞对话。WebSocket 场景下有结果时异步推送：

```json
{
  "type": "evaluation",
  "correctedText": "Hello! I'm fine.",
  "grammarIssues": ["Missing subject"],
  "suggestions": ["Try starting with a greeting"],
  "score": 7
}
```

**ISE 发音评测分数**（推送时一同携带）：

```json
{
  "type": "evaluation",
  "correctedText": "Hello! I'm fine.",
  "grammarIssues": ["Missing subject"],
  "suggestions": ["Try starting with a greeting"],
  "score": 7,
  "iseTotalScore": 75.5,
  "iseAccuracyScore": 68.2,
  "iseFluencyScore": 82.1,
  "iseIntegrityScore": 70.0
}
```

| 字段 | 说明 | 范围 |
|------|------|------|
| `iseTotalScore` | 发音总分 | 0-100 |
| `iseAccuracyScore` | 准确度评分 | 0-100 |
| `iseFluencyScore` | 流利度评分 | 0-100 |
| `iseIntegrityScore` | 完整度评分 | 0-100 |

**前端需要处理：** 收到 `type: "evaluation"` 消息后，提取四项 ISE 分数展示。

[ ] **前端待办：** WebSocket 收到 `type: "evaluation"` 后解析 `iseTotalScore` / `iseAccuracyScore` / `iseFluencyScore` / `iseIntegrityScore`，在界面上用雷达图展示发音四个维度的评分。

HTTP 场景请轮询报告接口获取评测数据。

---

## 6. 会话报告

```
GET /api/v1/practice/session/{sessionId}/report
```

返回评测数据和轮次列表。

---

## 7. 导出会话记录

```
GET /api/v1/practice/session/{sessionId}/export
```

下载 `.txt` 文件，包含全部对话轮次 + 评测数据。

```javascript
window.open("/api/v1/practice/session/abc-123/export")
```

---

## 8. 发音评测 ISE 雷达图

[ ] **前端待办：** 对话结束后，在总结页面展示发音评测的 ISE 雷达图（总分、准确度、流利度、完整度四项）。


