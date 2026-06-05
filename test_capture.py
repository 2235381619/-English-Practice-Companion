import soundcard as sc
import numpy as np
import sys
import wave
import time

# 列出所有 loopback 设备
print("=== 音频设备列表 ===")
devices = sc.all_microphones(include_loopback=True)
for i, d in enumerate(devices):
    print(f"  [{i}] {d.name}  (id={d.id})")

loopback = [d for d in devices if "loopback" in d.name.lower()]
if not loopback:
    print("没有找到 loopback 设备，使用第一个设备")
    target = devices[0]
else:
    target = loopback[0]
    
print(f"\n使用设备: {target.name}")
print(f"采样率: 16000 Hz, 格式: 16-bit PCM")

# 录制 5 秒
DURATION = 5
SAMPLE_RATE = 16000
BLOCK_SIZE = int(SAMPLE_RATE * 0.1)  # 100ms

print(f"\n正在录制 {DURATION} 秒...")
print("请确保电脑正在播放声音...")

all_audio = []

try:
    # 用 soundcard 的 recorder 上下文
    with target.recorder(samplerate=SAMPLE_RATE, channels=1, blocksize=BLOCK_SIZE) as mic:
        start = time.time()
        samples_collected = 0
        while time.time() - start < DURATION:
            data = mic.record(numframes=BLOCK_SIZE)
            if data is not None and len(data) > 0:
                all_audio.append(data.copy())
                samples_collected += len(data)
            sys.stdout.write(f"\r已录制: {time.time()-start:.1f}s / {DURATION}s ({samples_collected} samples)")
            sys.stdout.flush()
except Exception as e:
    print(f"\n\n录制出错: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

if not all_audio:
    print("\n\n没有录制到任何音频数据")
    sys.exit(1)

# 拼接音频
full_audio = np.concatenate(all_audio)
print(f"\n\n录制完成: {len(full_audio)} samples = {len(full_audio)/SAMPLE_RATE:.1f}s")

# 转成 16-bit PCM
if full_audio.dtype == np.float32 or full_audio.dtype == np.float64:
    max_val = np.max(np.abs(full_audio))
    if max_val > 0:
        full_audio = full_audio / max_val  # normalize
    pcm16 = (full_audio * 32767).astype(np.int16)
    print(f"格式: float -> int16, 范围: [{full_audio.min():.3f}, {full_audio.max():.3f}]")
else:
    pcm16 = full_audio.astype(np.int16)

out_file = "captured_python.wav"
with wave.open(out_file, 'wb') as wf:
    wf.setnchannels(1)
    wf.setsampwidth(2)  # 16-bit = 2 bytes
    wf.setframerate(SAMPLE_RATE)
    wf.writeframes(pcm16.tobytes())

print(f"已保存: {out_file} ({len(pcm16) * 2 / 1024:.0f} KB)")
