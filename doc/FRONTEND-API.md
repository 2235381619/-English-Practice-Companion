# 口语练习 API — 前端接入说明

## 待办清单

- [x] **场景注册** — 对话前先调 `POST /api/v1/practice/scenario` 注册场景
- [x] **`voice` 字段** — 请求体已支持 `voice.speed/volume/pitch`
- [x] **`audioData` 播放** — 响应里 base64 音频，前端 `new Audio("data:audio/mp3;base64," + data.audioData).play()`

---

## 1. 注册场景（对话前先调用）

用户在界面上选择场景后，先调这个接口，后端注册该会话的系统提示词。

```
POST /api/v1/practice/scenario
```

```json
{
  "sessionId": "abc-123",
  "scenarioCode": "interview"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | 是 | 会话 ID |
| `scenarioCode` | string | 是 | `interview` / `restaurant` / `meeting` / `default` |

```json
{
  "code": "0000",
  "info": "success"
}
```

> 选完场景到开始说话有自然时间间隔，注册瞬间完成，不用担心延迟。

---

## 2. 发送文本对话

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

> 注意：`scenarioCode` 不用再传了，已在注册场景时设定好。

```json
{
  "code": "0000",
  "data": {
    "asrText":   "hello hello",
    "replyText": "Hello! I'm ready to begin...",
    "audioData": "//UZRAAAAW252Y..."
  }
}
```

前端拿到 `audioData` 后直接播放：

```javascript
const audio = new Audio("data:audio/mp3;base64," + result.data.audioData);
audio.play();
```

---

## 3. 发送音频对话

```
POST /api/v1/practice/session
```

请求体与文本接口相同，区别是后端会先做 ASR 语音识别再回复。

---

## 4. WebSocket 音频输入

```
ws://localhost:8091/practice/audio/{sessionId}
```

1. 连接后持续发送二进制 PCM 音频帧（16kHz 16bit Mono）
2. 发送文本 `"END"` 触发识别 + 回复
3. 服务端返回 JSON（与 HTTP 接口一致）

WebSocket 场景语音参数固定用默认值 50/50/50。

---

## 5. 异步评测结果

评测已改为异步，不阻塞回复流程。若通过 WebSocket 连接，评测完成后会推送消息：

```json
{
  "type": "evaluation",
  "correctedText": "Hello! I'm fine.",
  "grammarIssues": ["Missing subject"],
  "suggestions": ["Try starting with a greeting"],
  "score": 7
}
```

前端收到后更新界面上的评测区域即可。

---

## 6. 获取会话报告

```
GET /api/v1/practice/session/{sessionId}/report
```
