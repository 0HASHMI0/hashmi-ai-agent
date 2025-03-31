import json
import os
from typing import Dict, Any

class Configuration:
    def __init__(self, config_data: Dict[str, Any]):
        self.ai_settings = config_data.get("ai_settings", {})
        self.web_scraper_settings = config_data.get("web_scraper_settings", {})
        self.report_settings = config_data.get("report_settings", {})
        self.logging = config_data.get("logging", {})

    def get_ai_mode(self) -> str:
        return self.ai_settings.get("mode", "local")

    def get_web_scraper_mode(self) -> str:
        return self.web_scraper_settings.get("mode", "mobile")

    def get_report_format(self) -> str:
        return self.report_settings.get("format", "excel")

    def get_log_level(self) -> str:
        return self.logging.get("log_level", "INFO")

def load_config(file_path: str = "config.json") -> Configuration:
    try:
        with open(file_path, "r") as file:
            config_data = json.load(file)
            return Configuration(config_data)
    except FileNotFoundError:
        raise Exception("Configuration file not found.")
    except json.JSONDecodeError:
        raise Exception("Invalid JSON format in configuration file.")
    except Exception as e:
        raise Exception(f"Error loading configuration file: {str(e)}")

if __name__ == "__main__":
    try:
        config = load_config()
        print("AI Mode:", config.get_ai_mode())
        print("Web Scraper Mode:", config.get_web_scraper_mode())
        print("Report Format:", config.get_report_format())
        print("Log Level:", config.get_log_level())
    except Exception as e:
        print(f"Error: {e}")
