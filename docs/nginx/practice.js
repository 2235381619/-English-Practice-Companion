
const WS_BASE = "ws://localhost:8091/practice/audio";
const PRACTICE_API = API_BASE + "/practice";

let sessionId = "";
let mode = "idle";
let messages = [];
let selectedScenario = null;

// Voice settings state
let voiceSettings = { speed: 50, volume: 50, pitch: 50 };
let voiceLocked = { speed: false, volume: false, pitch: false };

let rec = { stream: null, ctx: null, source: null, analyser: null, processor: null, ws: null, rafId: null, canvasCtx: null };

function initPractice() {
  sessionId = "session_" + Date.now() + "_" + Math.random().toString(36).slice(2, 8);
  selectedScenario = null;
  document.getElementById("practiceScenario").textContent = "Choose a scenario";
  document.getElementById("scenarioSelection").style.display = "block";
  document.getElementById("practiceSession").style.display = "none";
  document.getElementById("practiceInput").style.display = "none";
  document.getElementById("practiceEndBtn").style.display = "none";
  voiceSettings = { speed: 50, volume: 50, pitch: 50 };
  voiceLocked = { speed: false, volume: false, pitch: false };
  var cards = document.getElementById("scenarioCards");
  if (cards) {
    cards.innerHTML =
      "<div class=\"scenario-card\" onclick=\"selectScenario('default')\"><div class=\"scenario-card-icon\">&#x1F4AC;</div><div class=\"scenario-card-name\">Free Chat</div><div class=\"scenario-card-desc\">Casual conversation</div></div>" +
      "<div class=\"scenario-card\" onclick=\"selectScenario('interview')\"><div class=\"scenario-card-icon\">&#x1F4BC;</div><div class=\"scenario-card-name\">Interview</div><div class=\"scenario-card-desc\">Tech interview</div></div>" +
      "<div class=\"scenario-card\" onclick=\"selectScenario('restaurant')\"><div class=\"scenario-card-icon\">&#x1F37D;</div><div class=\"scenario-card-name\">Restaurant</div><div class=\"scenario-card-desc\">Ordering food</div></div>" +
      "<div class=\"scenario-card\" onclick=\"selectScenario('meeting')\"><div class=\"scenario-card-icon\">&#x1F4CB;</div><div class=\"scenario-card-name\">Meeting</div><div class=\"scenario-card-desc\">Business meeting</div></div>";
  }
  initVoiceSettings();
  updateUI();
}


function initVoiceSettings() {
  var params = ["speed", "volume", "pitch"];
  params.forEach(function(param) {
    var id = 'voice' + param.charAt(0).toUpperCase() + param.slice(1);
    var slider = document.getElementById(id);
    var valSpan = document.getElementById(id + 'Val');
    if (!slider || !valSpan) return;
    slider.value = 50;
    valSpan.textContent = '50';
    slider.disabled = false;
    voiceSettings[param] = 50;
    voiceLocked[param] = false;
    slider.oninput = function() {
      valSpan.textContent = slider.value;
      voiceSettings[param] = parseInt(slider.value, 10);
    };
    slider.onchange = function() {
      slider.disabled = true;
      voiceLocked[param] = true;
      voiceSettings[param] = parseInt(slider.value, 10);
    };
  });
}


function registerScenario(sessionId, code) {
  fetch(PRACTICE_API + "/scenario/" + sessionId + "/" + code, {
    method: "POST",
    headers: { "Content-Type": "application/json" }
  }).catch(function(e) { console.warn("Scenario registration failed:", e); });
}

function selectScenario(code) {
  selectedScenario = code;
  var labels = { "default":"Free Chat", "interview":"Interview", "restaurant":"Restaurant", "meeting":"Meeting" };
  document.getElementById("practiceScenario").textContent = "Connected: " + (labels[code] || "Free Chat");
  document.getElementById("scenarioSelection").style.display = "none";
  document.getElementById("practiceSession").style.display = "flex";
  document.getElementById("practiceInput").style.display = "block";
  document.getElementById("practiceEndBtn").style.display = "block";
  registerScenario(sessionId, code);
  initVoiceSettings();
  updateUI();
}

function getScenarioLabel(code) {
  var labels = { "default":"Free Chat", "interview":"Interview", "restaurant":"Restaurant", "meeting":"Meeting" };
  return labels[code] || "Free Chat";
}

function startRecording() {
  if (!sessionId || mode !== "idle") return;
  try {
    var ws = new WebSocket(WS_BASE + "/" + sessionId);
    ws.binaryType = "arraybuffer";
    ws.onopen = function() { ws.send(JSON.stringify({ type: "config" })); };
    ws.onmessage = function(ev) {
      if (typeof ev.data === "string") { try { var d = JSON.parse(ev.data); if (d.type === "evaluation") { handleEvaluation(d); } else { handleResult(d); } } catch(e) {} }
    };
    ws.onerror = function() {};
    ws.onclose = function() { if (mode === "recording") stopRecording(); };
    rec.ws = ws;
  } catch(e) { return; }

  navigator.mediaDevices.getUserMedia({ audio: { sampleRate: 16000, channelCount: 1, echoCancellation: true, noiseSuppression: true } })
  .then(function(stream) {
    rec.stream = stream;
    rec.ctx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 });
    rec.source = rec.ctx.createMediaStreamSource(stream);
    rec.analyser = rec.ctx.createAnalyser(); rec.analyser.fftSize = 256;
    rec.source.connect(rec.analyser);
    rec.processor = rec.ctx.createScriptProcessor(4096, 1, 1);
    rec.source.connect(rec.processor); rec.processor.connect(rec.ctx.destination);
    rec.processor.onaudioprocess = function(e) {
      if (mode !== "recording") return;
      var input = e.inputBuffer.getChannelData(0);
      var i16 = new Int16Array(input.length);
      for (var i = 0; i < input.length; i++) { var s = Math.max(-1, Math.min(1, input[i])); i16[i] = s < 0 ? s * 0x8000 : s * 0x7FFF; }
      if (rec.ws && rec.ws.readyState === WebSocket.OPEN) rec.ws.send(i16.buffer);
    };
    var canvas = document.getElementById("waveCanvas");
    if (canvas) {
      rec.canvasCtx = canvas.getContext("2d");
      var rect = canvas.parentElement.getBoundingClientRect();
      var dpr = window.devicePixelRatio || 1;
      canvas.width = rect.width * dpr; canvas.height = rect.height * dpr; rec.canvasCtx.scale(dpr, dpr);
    }
    mode = "recording";
    var vis = document.getElementById("practiceVisualizer"); if (vis) vis.style.display = "block";
    document.getElementById("practiceScenario").textContent = "Recording...";
    updateUI(); drawWaveform();
  }).catch(function() {});
}

function drawWaveform() {
  if (!rec.canvasCtx || !rec.analyser) return;
  var canvas = document.getElementById("waveCanvas");
  var d = new Uint8Array(rec.analyser.frequencyBinCount);
  var w = canvas.width / (window.devicePixelRatio || 1);
  var h = canvas.height / (window.devicePixelRatio || 1);
  function draw() {
    if (mode !== "recording") return;
    rec.rafId = requestAnimationFrame(draw);
    rec.analyser.getByteTimeDomainData(d);
    rec.canvasCtx.clearRect(0, 0, w, h);
    rec.canvasCtx.strokeStyle = "#2563eb"; rec.canvasCtx.lineWidth = 2; rec.canvasCtx.beginPath();
    for (var i = 0; i < d.length; i++) { var x = (i / d.length) * w; var y = (d[i] / 128.0) * h / 2; i === 0 ? rec.canvasCtx.moveTo(x, y) : rec.canvasCtx.lineTo(x, y); }
    rec.canvasCtx.lineTo(w, h / 2); rec.canvasCtx.stroke();
  }
  draw();
}

function stopRecording() {
  if (mode !== "recording") return;
  mode = "processing";
  document.getElementById("practiceScenario").textContent = "Processing...";
  var vis = document.getElementById("practiceVisualizer"); if (vis) vis.style.display = "none";
  updateUI();
  cancelAnimationFrame(rec.rafId); rec.rafId = null;
  if (rec.canvasCtx) { var cv = document.getElementById("waveCanvas"); if (cv) rec.canvasCtx.clearRect(0, 0, cv.width / (window.devicePixelRatio || 1), cv.height / (window.devicePixelRatio || 1)); }
  if (rec.processor) { try { rec.processor.disconnect(); } catch(e) {} rec.processor = null; }
  if (rec.source) { try { rec.source.disconnect(); } catch(e) {} rec.source = null; }
  if (rec.analyser) { try { rec.analyser.disconnect(); } catch(e) {} rec.analyser = null; }
  if (rec.stream) { rec.stream.getTracks().forEach(function(t) { t.stop(); }); rec.stream = null; }
  if (rec.ctx) { rec.ctx.close().catch(function(){}); rec.ctx = null; }
  if (rec.ws && rec.ws.readyState === WebSocket.OPEN) rec.ws.send("END");
}

function toggleRecording() {
  if (mode === "idle") startRecording(); else if (mode === "recording") stopRecording();
}

function sendPracticeText() {
  var inp = document.getElementById("practiceTextInput");
  var text = inp.value.trim();
  if (!text || !sessionId || mode === "processing") return;
  inp.value = "";
  addMessage("user", text);
  fetch(PRACTICE_API + "/text", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sessionId: sessionId, text: text })
  }).then(function(r) { return r.json(); }).then(function(json) {
    if (json.code === "0000" && json.data) handleResult(json.data);
  }).catch(function(e) { console.warn(e); });
}

function handleResult(data) {
  if (data.asrText) addMessage("user", data.asrText);
  var reply = data.replyText || data.reply || data.aiReply || "";
  if (reply) {
    messages.push({ role: "assistant", content: reply, correctedText: data.correctedText || "", grammarIssues: data.grammarIssues || [], suggestions: data.suggestions || [], score: data.score || 0 });
    renderMessages();
  }
  if (data.audioData) {
    new Audio("data:audio/mp3;base64," + data.audioData).play().catch(function(e) { console.warn("Audio playback failed:", e); });
  }
  mode = "idle";
  if (rec.ws) { try { rec.ws.close(); } catch(e) {} rec.ws = null; }
  document.getElementById("practiceScenario").textContent = "Press mic to speak";
  updateUI();
  setTimeout(fetchSessionReport, 500);
}


function handleEvaluation(data) {
  if (messages.length > 0) {
    var last = messages[messages.length - 1];
    if (last.role === "assistant") {
      if (data.correctedText) last.correctedText = data.correctedText;
      if (data.grammarIssues) last.grammarIssues = data.grammarIssues;
      if (data.suggestions) last.suggestions = data.suggestions;
      if (data.score) last.score = data.score;
      if (data.iseTotalScore) last.iseTotalScore = data.iseTotalScore;
      if (data.iseAccuracyScore) last.iseAccuracyScore = data.iseAccuracyScore;
      if (data.iseFluencyScore) last.iseFluencyScore = data.iseFluencyScore;
      if (data.iseIntegrityScore) last.iseIntegrityScore = data.iseIntegrityScore;
      renderMessages();
    }
  }
}



function fetchSessionReport(retries) {
  if (!sessionId) return;
  if (retries === undefined) retries = 0;
  if (retries >= 10) return;
  fetch(PRACTICE_API + "/session/" + sessionId + "/report")
    .then(function(r) { return r.json(); })
    .then(function(json) {
      if (json.code === "0000" && json.data && json.data.rounds) {
        var rounds = json.data.rounds;
        var last = rounds[rounds.length - 1];
        if (last && (last.correctedText || last.score)) {
          handleEvaluation(last);
        } else if (retries < 9) {
          setTimeout(function() { fetchSessionReport(retries + 1); }, 800);
        }
      } else if (retries < 9) {
        setTimeout(function() { fetchSessionReport(retries + 1); }, 800);
      }
    })
    .catch(function(e) {
      console.warn("Report fetch failed:", e);
      if (retries < 9) {
        setTimeout(function() { fetchSessionReport(retries + 1); }, 800);
      }
    });
}

function generateRadarChart(acc, flu, integ, total) {
  var cx = 70, cy = 70, r = 50;
  var labels = ["Accuracy","Fluency","Integrity","Total"];
  var vals = [acc, flu, integ, total];
  var svg = '<svg width="140" height="140" viewBox="0 0 140 140" style="display:block;margin:6px auto">';
  for (var ring = 1; ring <= 3; ring++) {
    var rr = (ring / 3) * r;
    svg += '<circle cx="' + cx + '" cy="' + cy + '" r="' + rr + '" fill="none" stroke="#e2e8f0" stroke-width="1"/>';
  }
  var angles = [0, 90, 180, 270];
  for (var i = 0; i < 4; i++) {
    var rad = angles[i] * Math.PI / 180;
    var ex = cx + r * Math.sin(rad);
    var ey = cy - r * Math.cos(rad);
    svg += '<line x1="' + cx + '" y1="' + cy + '" x2="' + ex + '" y2="' + ey + '" stroke="#e2e8f0" stroke-width="1"/>';
  }
  var pts = "";
  for (var i = 0; i < 4; i++) {
    var rad = angles[i] * Math.PI / 180;
    var vr = (vals[i] / 100) * r;
    pts += (cx + vr * Math.sin(rad)).toFixed(1) + "," + (cy - vr * Math.cos(rad)).toFixed(1) + " ";
  }
  svg += '<polygon points="' + pts.trim() + '" fill="rgba(37,99,235,0.15)" stroke="#2563eb" stroke-width="2"/>';
  for (var i = 0; i < 4; i++) {
    var rad = angles[i] * Math.PI / 180;
    var vr = (vals[i] / 100) * r;
    svg += '<circle cx="' + (cx + vr * Math.sin(rad)).toFixed(1) + '" cy="' + (cy - vr * Math.cos(rad)).toFixed(1) + '" r="3" fill="#2563eb"/>';
  }
  for (var i = 0; i < 4; i++) {
    var rad = angles[i] * Math.PI / 180;
    svg += '<text x="' + (cx + (r + 14) * Math.sin(rad)).toFixed(1) + '" y="' + (cy - (r + 14) * Math.cos(rad)).toFixed(1) + '" text-anchor="middle" dominant-baseline="middle" font-size="9" fill="#64748b">' + labels[i] + '</text>';
  }
  svg += '</svg>';
  return svg;
}


function exportSession() {
  if (!sessionId) return;
  window.open(API_BASE + "/practice/session/" + sessionId + "/export");
}

function addMessage(role, content) { messages.push({ role: role, content: content }); renderMessages(); }

function renderMessages() {
  var list = document.getElementById("practiceMessageList");
  list.innerHTML = messages.map(function(m) {
    if (m.role === "user") {
      return '<div class="pmsg user"><div class="pmsg-bubble user-bubble"><div class="pmsg-user-label">You</div><div class="pmsg-text">' + esc(m.content) + '</div></div></div>';
    }
    var evalHtml = "";
    var hasEval = m.correctedText || (m.grammarIssues && m.grammarIssues.length > 0) || (m.suggestions && m.suggestions.length > 0) || m.score > 0 || m.iseTotalScore > 0;
    if (hasEval) {
      evalHtml += "<div class=\"msg-eval\">";
      if (m.correctedText && m.correctedText !== m.content) {
        evalHtml += "<div class=\"eval-line eval-correct\">\u2713 " + esc(m.correctedText) + "</div>";
      }
      if (m.grammarIssues && m.grammarIssues.length > 0) {
        m.grammarIssues.forEach(function(g) { evalHtml += "<div class=\"eval-line eval-issue\">\u2022 " + esc(g) + "</div>"; });
      }
      if (m.suggestions && m.suggestions.length > 0) {
        evalHtml += "<div class=\"eval-label\">\uD83D\uDCA1 Suggestions:</div>";
        m.suggestions.forEach(function(s) { evalHtml += "<div class=\"eval-line eval-suggestion\">- " + esc(s) + "</div>"; });
      }
      if (m.score > 0) {
        evalHtml += "<div class=\"eval-line eval-score\">Score: " + m.score + "/5</div>";
      }
      if (m.iseTotalScore > 0 || m.iseAccuracyScore > 0 || m.iseFluencyScore > 0 || m.iseIntegrityScore > 0) {
        evalHtml += '<div class="eval-section-title" style="margin-top:6px;padding-top:6px;border-top:1px solid #e2e8f0;text-align:center">Pronunciation</div>';
        evalHtml += generateRadarChart(m.iseAccuracyScore, m.iseFluencyScore, m.iseIntegrityScore, m.iseTotalScore);
      }
      evalHtml += "</div>";
    }
    return '<div class="pmsg tutor"><div class="pmsg-bubble tutor-bubble"><div class="pmsg-user-label">AI</div><div class="pmsg-text reply">' + esc(m.content) + '</div>' + evalHtml + '</div></div>';
  }).join("");
  document.getElementById("conversationEmpty").style.display = messages.length ? "none" : "block";
  scrollBottom();
}

function updateUI() {
  var btn = document.getElementById("practiceMicBtn");
  if (!btn) return; btn.classList.remove("recording"); btn.disabled = false;
  if (mode === "recording") {
    btn.classList.add("recording");
    btn.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>';
  } else if (mode === "processing") {
    btn.disabled = true;
    btn.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="30 70" stroke-linecap="round"/></svg>';
  } else {
    btn.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="2" width="6" height="11" rx="3"/><path d="M5 10a7 7 0 0 0 14 0"/><line x1="12" y1="19" x2="12" y2="22"/></svg>';
  }
}

function scrollBottom() { requestAnimationFrame(function() { var c = document.getElementById("practiceConversation"); if (c) c.scrollTop = c.scrollHeight; }); }

function esc(s) { if (!s) return ""; var d = document.createElement("div"); d.textContent = s; return d.innerHTML; }

function switchMode(modeVal) {
  document.querySelectorAll(".nav-tab").forEach(function(t) { t.classList.toggle("active", t.dataset.mode === modeVal); });
  var chat = document.getElementById("chatMode"); var prac = document.getElementById("practiceMode");
  if (modeVal === "practice") {
    chat.style.display = "none"; prac.style.display = "flex";
    document.querySelector(".sidebar").style.display = "none";
    document.querySelector(".main-area").style.flex = "1";
    initPractice();
  } else {
    chat.style.display = "flex"; prac.style.display = "none";
    document.querySelector(".sidebar").style.display = "";
    document.querySelector(".main-area").style.flex = "";
  }
}

function endPracticeSession() {
  stopRecording();
  document.getElementById("scenarioSelection").style.display = "block";
  document.getElementById("practiceSession").style.display = "none";
  document.getElementById("practiceInput").style.display = "none";
  document.getElementById("practiceEndBtn").style.display = "none";
  voiceSettings = { speed: 50, volume: 50, pitch: 50 };
  voiceLocked = { speed: false, volume: false, pitch: false };
  document.getElementById("practiceTitle").textContent = "AI Voice Chat";
}

setTimeout(initPractice, 500);
console.log("Practice loaded");
