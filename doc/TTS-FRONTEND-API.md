# TTS 语音参数 — 前端接入说明

## 变更内容

口语练习接口 `/api/v1/practice/text` 和 `/api/v1/practice/session` 的请求体新增 `voice` 字段，用于动态调节 TTS 合成语音。

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
    "audioUrl":  "/audio/uuid-xxx.mp3",
    "suggestion": "grammar suggestion"
  }
}
```

前端拿到 `audioUrl` 后直接播放：

```javascript
const audio = new Audio(result.data.audioUrl);
audio.play();
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
