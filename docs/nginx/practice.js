/**
 * English Practice Mode — VAD auto-conversation
 */

const PRACTICE_API = 'http://localhost:8091/api/v1/practice';

const VAD_THRESHOLD = 2500;
const SILENCE_TIMEOUT = 2000;
const MAX_RECORD_DURATION = 15000;  // 15 seconds max per recording

const practice = {
  scenarios: [], selectedScenario: null, sessionId: '',
  rounds: [], latestEval: null,
  mode: 'idle', // idle | listening | processing
  loading: false
};

// VAD state (not exposed in practice object to keep it clean)
let vad = { stream: null, context: null, analyser: null, recorder: null, chunks: [], silenceTimer: null, rafId: null };

// ===== Init =====
async function initPractice() {
  try {
    const res = await fetch(PRACTICE_API + '/scenarios');
    const json = await res.json();
    if (json.code === '0000' && json.data?.scenarios) {
      practice.scenarios = json.data.scenarios;
      renderScenarioCards();
    }
  } catch (e) { console.warn('Failed to load scenarios:', e); }
}

// ===== Scenario Cards =====
function renderScenarioCards() {
  const icons = { interview: '💼', restaurant: '🍽️', meeting: '📋' };
  const descs = {
    interview: 'Practice a senior tech interview with real-time feedback',
    restaurant: 'Role-play ordering food at a restaurant',
    meeting: 'Simulate a business meeting conversation'
  };
  document.getElementById('scenarioCards').innerHTML = practice.scenarios.map((s, i) => `\n    <div class="scenario-card" style="--i:${i}" onclick="startPracticeSession('${s.code}')">\n      <div class="scenario-card-icon">${icons[s.code] || '🎯'}</div>\n      <div class="scenario-card-name">${s.name}</div>\n      <div class="scenario-card-desc">${descs[s.code] || ''}</div>\n    </div>\n  `).join('');
}

// ===== Session =====
async function startPracticeSession(code) {
  practice.selectedScenario = code;
  try {
    const res = await fetch(PRACTICE_API + '/session', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ scenarioCode: code })
    });
    const json = await res.json();
    if (json.code === '0000' && json.data?.sessionId) {
      practice.sessionId = json.data.sessionId;
      practice.rounds = []; practice.latestEval = null;
      showSessionUI(code);
    }
  } catch (e) { console.warn('Session creation failed:', e); }
}

function showSessionUI(code) {
  const scenario = practice.scenarios.find(s => s.code === code);
  document.getElementById('scenarioSelection').style.display = 'none';
  document.getElementById('practiceSession').style.display = 'flex';
  document.getElementById('practiceInput').style.display = 'block';
  document.getElementById('practiceEndBtn').style.display = 'block';
  document.getElementById('practiceTitle').textContent = '🎯 ' + (scenario?.name || 'Practice');
  document.getElementById('practiceScenario').textContent = 'Click 🎤 to start a hands-free conversation';
  document.getElementById('conversationEmpty').style.display = 'block';
  document.getElementById('practiceMessageList').innerHTML = '';
  document.getElementById('evaluationPanel').style.display = 'none';
  document.getElementById('practiceTextInput').focus();
}

async function endPracticeSession() {
  if (!practice.sessionId) return;
  stopConversation();
  try { await fetch(PRACTICE_API + '/session/' + practice.sessionId, { method: 'DELETE' }); } catch (e) {}
  practice.sessionId = ''; practice.rounds = []; practice.latestEval = null;
  document.getElementById('scenarioSelection').style.display = 'block';
  document.getElementById('practiceSession').style.display = 'none';
  document.getElementById('practiceInput').style.display = 'none';
  document.getElementById('practiceEndBtn').style.display = 'none';
  document.getElementById('practiceTitle').textContent = 'English Speaking Practice';
  document.getElementById('practiceScenario').textContent = '';
}

// ===== Text =====
async function sendPracticeText() {
  const input = document.getElementById('practiceTextInput');
  const text = input.value.trim();
  if (!text || !practice.sessionId || practice.loading) return;
  input.value = '';
  practice.loading = true;
  loadingUi(true);
  try {
    const p = new URLSearchParams({ sessionId: practice.sessionId, text });
    const res = await fetch(PRACTICE_API + '/text', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: p
    });
    const json = await res.json();
    if (json.code === '0000' && json.data) handleResponse(json.data);
  } catch (e) { console.warn('Text failed:', e); }
  finally { practice.loading = false; loadingUi(false); }
}

// ===== VAD Conversation =====
async function toggleConversation() {
  if (practice.mode === 'listening') {
    stopConversation();
  } else if (practice.mode === 'idle') {
    await startConversation();
  }
  // 'processing' state: ignore click
}

async function startConversation() {
  if (!practice.sessionId || practice.loading) return;

  // Acquire mic stream once
  if (!vad.stream) {
    try {
      vad.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      vad.context = new (window.AudioContext || window.webkitAudioContext)();
      const source = vad.context.createMediaStreamSource(vad.stream);
      vad.analyser = vad.context.createAnalyser();
      vad.analyser.fftSize = 256;
      source.connect(vad.analyser);
    } catch (e) {
      console.warn('Mic access denied:', e);
      return;
    }
  }

  beginListening();
}

function beginListening() {
  vad.voiceHold = 0;
  vad.speaking = false;
  vad.maxTimer = setTimeout(() => {
    if (practice.mode === 'listening') stopForProcessing();
  }, MAX_RECORD_DURATION);
  vad.chunks = [];
  vad.recorder = new MediaRecorder(vad.stream, { mimeType: 'audio/webm' });
  vad.recorder.ondataavailable = e => { if (e.data.size > 0) vad.chunks.push(e.data); };
  vad.recorder.onstop = () => processAudio();
  vad.recorder.start();

  practice.mode = 'listening';
  updateMicUi();
  document.getElementById('practiceScenario').textContent = '🎤 Listening... speak now';

  vad.silenceTimer = null;
  vadLoop();
}

function vadLoop() {
  if (practice.mode !== "listening") return;

  var data = new Uint8Array(vad.analyser.frequencyBinCount);
  vad.analyser.getByteTimeDomainData(data);

  var sum = 0;
  for (var i = 0; i < data.length; i++) {
    var v = (data[i] - 128) / 128;
    sum += v * v;
  }
  var rms = Math.sqrt(sum / data.length) * 10000;

  if (rms > VAD_THRESHOLD) {
    vad.voiceHold += 50;
    if (vad.voiceHold > 200) {
      vad.speaking = true;
      clearTimeout(vad.silenceTimer);
      vad.silenceTimer = null;
    }
  } else {
    vad.voiceHold = 0;
    if (vad.speaking && !vad.silenceTimer) {
      vad.silenceTimer = setTimeout(function() {
        if (practice.mode === "listening") stopForProcessing();
      }, SILENCE_TIMEOUT);
    }
  }

  vad.rafId = requestAnimationFrame(vadLoop);
}
function stopForProcessing() {
  practice.mode = "processing";
  clearTimeout(vad.silenceTimer);
  clearTimeout(vad.maxTimer);
  cancelAnimationFrame(vad.rafId);
  if (vad.recorder && vad.recorder.state !== 'inactive') vad.recorder.stop();
  updateMicUi();
}

function stopConversation() {
  practice.mode = 'idle';
  clearTimeout(vad.silenceTimer);
  clearTimeout(vad.maxTimer);
  cancelAnimationFrame(vad.rafId);
  if (vad.recorder && vad.recorder.state !== 'inactive') vad.recorder.stop();
  if (vad.stream) { vad.stream.getTracks().forEach(t => t.stop()); vad.stream = null; }
  if (vad.context) { vad.context.close(); vad.context = null; }
  updateMicUi();
  document.getElementById('practiceScenario').textContent = 'Click 🎤 to start a hands-free conversation';
}

async function processAudio() {
  if (vad.chunks.length === 0) { autoRelisten(); return; }
  const blob = new Blob(vad.chunks, { type: 'audio/webm' });
  if (blob.size < 8000) { autoRelisten(); return; }

  document.getElementById('practiceScenario').textContent = '⏳ Processing...';

  try {
    const fd = new FormData();
    fd.append('sessionId', practice.sessionId);
    fd.append('file', blob, 'recording.webm');
    const res = await fetch(PRACTICE_API + '/audio', { method: 'POST', body: fd });
    const json = await res.json();
    if (json.code === '0000' && json.data) handleResponse(json.data);
  } catch (e) { console.warn('Audio failed:', e); }

  autoRelisten();
}

function autoRelisten() {
  if (!practice.sessionId || practice.mode === 'idle') return;
  setTimeout(beginListening, 2000);
}

// ===== Response =====
function handleResponse(data) {
  if (!data) return;
  const e = {
    asrText: data.asrText || '', aiReply: data.aiReply || '',
    score: data.score || 0, correctedText: data.correctedText,
    grammarIssues: data.grammarIssues || [], suggestions: data.suggestions || []
  };
  practice.latestEval = e;
  practice.rounds.push({ userInput: data.asrText || '', asrText: data.asrText || '', evaluation: e });
  document.getElementById('conversationEmpty').style.display = 'none';
  renderMessages();
  renderEval(e);
}

function renderMessages() {
  const list = document.getElementById('practiceMessageList');
  list.innerHTML = practice.rounds.map(r => {
    const e = r.evaluation;
    const fix = e.correctedText && e.correctedText !== e.asrText;
    return `
      <div class="pmsg user">
        <div class="pmsg-bubble user-bubble">
          <div class="pmsg-user-label">You</div>
          <div class="pmsg-text">${esc(r.asrText || r.userInput)}</div>
        </div>
      </div>
      <div class="pmsg tutor">
        <div class="pmsg-bubble tutor-bubble">
          <div class="pmsg-user-label">Tutor</div>
          <div class="pmsg-text">${esc(e.aiReply)}</div>
          ${fix ? `<div class="pmsg-correction">✓ ${esc(e.correctedText)}</div>` : ''}
          ${e.grammarIssues?.length ? `<div class="pmsg-issues">${e.grammarIssues.map(g => `<div class="pmsg-issue">• ${esc(g)}</div>`).join('')}</div>` : ''}
        </div>
      </div>`;
  }).join('');
  scrollBottom();
}

function renderEval(d) {
  const p = document.getElementById('evaluationPanel');
  p.style.display = 'flex';
  const c = d.score >= 8 ? 'high' : d.score >= 5 ? 'mid' : 'low';
  p.innerHTML = `
    <div class="eval-header">
      <span class="eval-label">Evaluation</span>
      <span class="eval-score ${c}">${d.score}/10</span>
    </div>
    ${d.correctedText ? `<div class="eval-section"><div class="eval-section-title">✓ Corrected</div><div class="eval-text">${esc(d.correctedText)}</div></div>` : ''}
    ${d.grammarIssues?.length ? `<div class="eval-section"><div class="eval-section-title">Issues</div>${d.grammarIssues.map(g => `<div class="eval-issue">${esc(g)}</div>`).join('')}</div>` : ''}
    ${d.suggestions?.length ? `<div class="eval-section"><div class="eval-section-title">Suggestions</div>${d.suggestions.map(s => `<div class="eval-suggestion">${esc(s)}</div>`).join('')}</div>` : ''}`;
}

// ===== UI =====
function updateMicUi() {
  const btn = document.getElementById('practiceMicBtn');
  if (!btn) return;
  btn.classList.remove('recording', 'processing');
  if (practice.mode === 'listening') {
    btn.classList.add('recording');
    btn.title = 'Click to stop conversation';
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>`;
  } else if (practice.mode === 'processing') {
    btn.classList.add('processing');
    btn.title = 'Processing...';
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin"><circle cx="12" cy="12" r="10" stroke-dasharray="30 70" stroke-linecap="round"/></svg>`;
  } else {
    btn.title = 'Start listening (hands-free)';
    btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="2" width="6" height="11" rx="3"/><path d="M5 10a7 7 0 0 0 14 0"/><line x1="12" y1="19" x2="12" y2="22"/></svg>`;
  }
}

function loadingUi(v) {
  const el = document.getElementById('practiceTextInput');
  if (el) el.disabled = v;
}
function scrollBottom() { requestAnimationFrame(() => { const c = document.getElementById('practiceConversation'); if (c) c.scrollTop = c.scrollHeight; }); }
function esc(s) { if (!s) return ''; const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

// ===== Mode Switch =====
function switchMode(mode) {
  if (practice.mode !== 'idle') stopConversation();
  document.querySelectorAll('.nav-tab').forEach(t => t.classList.toggle('active', t.dataset.mode === mode));
  const chat = document.getElementById('chatMode');
  const prac = document.getElementById('practiceMode');
  if (mode === 'practice') {
    chat.style.display = 'none'; prac.style.display = 'flex';
    document.querySelector('.sidebar').style.display = 'none';
    document.querySelector('.main-area').style.flex = '1';
    if (!practice.scenarios.length) initPractice();
  } else {
    chat.style.display = 'flex'; prac.style.display = 'none';
    document.querySelector('.sidebar').style.display = '';
    document.querySelector('.main-area').style.flex = '';
  }
}

// Also need to update the HTML onclick. Add a small polyfill to redirect old name.
window.togglePracticeMic = toggleConversation;

console.log('Practice module loaded (VAD auto-conversation)');



