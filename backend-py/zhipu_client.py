import os
import json
import time
import hashlib
import httpx

API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"


def _get_api_key():
    key = os.getenv("ZHIPU_API_KEY")
    if not key:
        raise ValueError("ZHIPU_API_KEY 未设置，请检查 .env 文件")
    return key


class ZhipuAiClient:
    def __init__(self):
        self.model = "glm-4-flash"
        self.cache = {}

    @property
    def api_key(self):
        return _get_api_key()

    def _cache_key(self, prompt: str, system: str, temperature: float) -> str:
        return hashlib.md5(f"{prompt}{system}{temperature}".encode()).hexdigest()

    def chat(self, prompt: str, system: str, temperature: float = 0.7, max_tokens: int = 1024) -> str:
        cache_key = self._cache_key(prompt, system, temperature)
        if cache_key in self.cache:
            return self.cache[cache_key]

        key = self.api_key
        headers = {
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": prompt},
            ],
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        with httpx.Client(timeout=60) as client:
            resp = client.post(API_URL, headers=headers, json=payload)
            resp.raise_for_status()
            data = resp.json()
            content = data["choices"][0]["message"]["content"]
            self.cache[cache_key] = content
            return content

    def chat_json(self, prompt: str, system: str, temperature: float = 0.3) -> dict:
        content = self.chat(prompt, system, temperature)
        content = content.strip()
        if content.startswith("```json"):
            content = content[7:]
        if content.startswith("```"):
            content = content[3:]
        if content.endswith("```"):
            content = content[:-3]
        return json.loads(content.strip())


zhipu_client = ZhipuAiClient()
