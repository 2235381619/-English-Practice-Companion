# TTS 语音参数 — 前端接入说明

## 待办清单

- [x] **请求接入 `voice` 字段** — 请求体已支持 `voice.speed/volume/pitch`
- [ ] **播放 `audioUrl`** — 响应里的 `audioUrl` 需前端 `new Audio(url).play()` 播出来

---

## 请求参数

```json
{
  "sessionId":    "uuid-xxx",
  "text":         "Hello, how are you?",
  "scenarioCode": "interview",
  "voice": {
    "speed":  70,
    "volume": 80,
    "pitch":  60
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `voice` | object | 否 | 语音参数，不传时用默认值 |
| `voice.speed` | int | 否 | 语速 0-100，默认 50 |
| `voice.volume` | int | 否 | 音量 0-100，默认 50 |
| `voice.pitch` | int | 否 | 音高 0-100，默认 50 |

---

## 响应

```json
{
  "code": "0000",
  "data": {
    "asrText":   "user speech text",
    "replyText": "LLM reply text",
    "audioUrl":  "http://localhost:8091/audio/sessionId.mp3",
    "suggestion": "grammar suggestion"
  }
}
```

---

## 调用示例

### 默认音色（不传 voice）
```json
{
  "sessionId": "abc",
  "text": "Hello!",
  "scenarioCode": "interview"
}
```

### 快速 + 高音量
```json
{
  "sessionId": "abc",
  "text": "Hello!",
  "scenarioCode": "interview",
  "voice": { "speed": 80, "volume": 90, "pitch": 50 }
}
```
