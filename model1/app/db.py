import pymysql
import logging
from config import settings

logger = logging.getLogger(__name__)

def get_db_connection():
    return pymysql.connect(
        host=settings.db_host,
        user=settings.db_user,
        password=settings.db_password,
        database=settings.db_name,
        port=getattr(settings, 'db_port', 3306),
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
        connect_timeout=getattr(settings, 'timeout', 30)
    )
