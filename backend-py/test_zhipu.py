import os
from dotenv import load_dotenv
os.chdir(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(encoding='utf-8')
print("API_KEY present:", os.getenv("ZHIPU_API_KEY") is not None)
print("API_KEY prefix:", os.getenv("ZHIPU_API_KEY", "")[:10] if os.getenv("ZHIPU_API_KEY") else "NONE")
from zhipu_client import zhipu_client
print("Client created")
try:
    result = zhipu_client.chat("你好", "你是一个助手", 0.7, 128)
    print("Result:", result)
except Exception as e:
    print("Error:", type(e).__name__, str(e))
