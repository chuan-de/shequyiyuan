# -*- coding: utf-8 -*-
"""
Vision API smoke test — supports Kimi and Doubao (volcengine).

Reads ../key, base64-encodes ../testbingli.jpg, asks the model to OCR/structure
the medical record image, writes the response to scripts/test_<provider>.out.md.

Usage:
    python scripts/test_kimi_vision.py kimi [.cn|.ai]
    python scripts/test_kimi_vision.py doubao
"""
import base64
import json
import os
import sys
import time
import urllib.request
import urllib.error

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KEY_FILE = os.path.join(ROOT, "key")
IMAGE_FILE = os.path.join(ROOT, "testbingli.jpg")


def _load_key_block(prefix_token):  # -> (api_key, base_url_or_None)
    """Read the key file as ordered blocks; return (api_key, base_url_if_present)."""
    with open(KEY_FILE, "r", encoding="utf-8") as f:
        lines = [ln.strip() for ln in f.readlines() if ln.strip()]
    for i, ln in enumerate(lines):
        if prefix_token in ln.lower():
            api_key = lines[i + 1] if i + 1 < len(lines) else ""
            base_url = None
            if i + 2 < len(lines) and lines[i + 2].lower().startswith("baseurl"):
                base_url = lines[i + 2].split(None, 1)[1].strip()
            return api_key, base_url
    raise RuntimeError(f"key block with token {prefix_token!r} not found")


def image_to_data_url(path: str) -> str:
    with open(path, "rb") as f:
        b64 = base64.b64encode(f.read()).decode("ascii")
    ext = os.path.splitext(path)[1].lstrip(".").lower() or "jpeg"
    if ext == "jpg":
        ext = "jpeg"
    return f"data:image/{ext};base64,{b64}"


USER_PROMPT = (
    "请识别这张病历图片，只输出一个 JSON 对象，不要解释、不要总结、不要 markdown 代码块。字段：\n"
    "{patient_name, gender, age, visit_date, department, "
    "chief_complaint(主诉), present_illness(现病史), "
    "diagnosis(诊断), prescription(处方/医嘱), doctor}。无法识别的字段填 null。"
)


def call_vision(base_url: str, api_key: str, model: str, data_url: str) -> dict:
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": "你是一名专业医学助手，擅长识别中文病历图片并结构化输出。"},
            {
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": data_url}},
                    {"type": "text", "text": USER_PROMPT},
                ],
            },
        ],
    }

    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url=base_url.rstrip("/") + "/chat/completions",
        data=body,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )

    started = time.time()
    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            raw = resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code} {e.reason}: {err_body}") from None
    elapsed = time.time() - started

    return {"elapsed_sec": round(elapsed, 2), "raw": json.loads(raw)}


def main() -> int:
    provider = sys.argv[1] if len(sys.argv) > 1 else "kimi"

    if provider == "kimi":
        suffix = sys.argv[2] if len(sys.argv) > 2 else ".cn"
        base_url = f"https://api.moonshot{suffix}/v1"
        model = "kimi-k2.6"
        api_key, _ = _load_key_block("kimi")
    elif provider == "doubao":
        api_key, base_url = _load_key_block("doubao")
        if base_url is None:
            base_url = "https://ark.cn-beijing.volces.com/api/v3"
        model = "doubao-seed-2.0-lite"
    else:
        print(f"unknown provider {provider!r}, use kimi or doubao")
        return 2

    print(f"==> provider : {provider}")
    print(f"==> endpoint : {base_url}")
    print(f"==> model    : {model}")
    print(f"==> image    : {IMAGE_FILE} ({os.path.getsize(IMAGE_FILE)} bytes)")
    print(f"==> key      : {api_key[:8]}...{api_key[-4:]}")

    data_url = image_to_data_url(IMAGE_FILE)
    print(f"==> data url : len={len(data_url)} (base64 inline)")

    print(f"==> calling {provider} ...")
    try:
        result = call_vision(base_url, api_key, model, data_url)
    except Exception as e:
        print(f"!! request failed: {e}")
        return 1

    print(f"==> elapsed  : {result['elapsed_sec']}s")
    raw = result["raw"]
    usage = raw.get("usage", {})
    print(f"==> usage    : {usage}")

    choices = raw.get("choices") or []
    if not choices:
        print("!! no choices in response:")
        print(json.dumps(raw, ensure_ascii=False, indent=2))
        return 1

    content = choices[0].get("message", {}).get("content", "")
    out_path = os.path.join(ROOT, "scripts", f"test_{provider}_vision.out.md")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(f"# {provider} vision smoke test\n\n")
        f.write(f"- endpoint: `{base_url}`\n")
        f.write(f"- model: `{model}`\n")
        f.write(f"- image: `{IMAGE_FILE}` ({os.path.getsize(IMAGE_FILE)} bytes)\n")
        f.write(f"- elapsed: {result['elapsed_sec']}s\n")
        f.write(f"- usage: `{usage}`\n\n")
        f.write("## Response\n\n")
        f.write(content)
    print(f"==> wrote   : {out_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
