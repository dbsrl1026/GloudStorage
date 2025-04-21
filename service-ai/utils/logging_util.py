import logging

class LoggingUtil:
    @staticmethod
    def get_logger(name: str = __name__, level: int = logging.INFO) -> logging.Logger:
        logger = logging.getLogger(name)
        if not logger.hasHandlers():  # 중복 핸들러 방지
            logger.setLevel(level)
            handler = logging.StreamHandler()
            formatter = logging.Formatter("[%(levelname)s] %(asctime)s - %(name)s - %(message)s")
            handler.setFormatter(formatter)
            logger.addHandler(handler)
        return logger