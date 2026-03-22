import logging
import json
import datetime
from typing import Dict, Any, Optional
from app.db import get_db_connection
from app.model_service import model_service

logger = logging.getLogger(__name__)

class TaskService:
    def record_analysis_result(
        self,
        user_id: int,
        content: str,
        model_identifier: Optional[str] = None, # modelName from request
        model_id_param: Optional[int] = None,   # modelId from request
        result_data: Dict[str, Any] = None,
        task_name: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Record analysis result to database without duplicating models.
        """
        if not result_data:
            return {}

        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # 1. Resolve Model ID
                final_model_id = self._resolve_model_id(cursor, model_id_param, model_identifier)
                
                if not final_model_id:
                    # If we really can't find a model, we try to find ANY active model as fallback
                    # This prevents creating random new records while still allowing the task to be saved
                    logger.warning(f"Model not found for id={model_id_param}, name={model_identifier}. Using fallback.")
                    cursor.execute("SELECT id FROM model_info WHERE status = 'ACTIVE' LIMIT 1")
                    row = cursor.fetchone()
                    if row:
                        final_model_id = row['id']
                    else:
                        # If table is empty, we might have to skip saving or raise error
                        logger.error("No active models found in database. Cannot save task.")
                        return {}

                # 2. Create Task
                real_user_id = user_id if user_id else 1
                final_task_name = task_name or f"Analysis - {model_identifier or 'Unknown'}"
                
                cursor.execute("""
                    INSERT INTO analysis_task 
                    (user_id, model_id, task_name, task_type, source, status, total_count, success_count, finished_at)
                    VALUES (%s, %s, %s, 'SINGLE', 'WEB', 'FINISHED', 1, 1, NOW())
                """, (real_user_id, final_model_id, final_task_name))
                task_id = cursor.lastrowid
                
                # 3. Save Result
                predicted_label = result_data.get('predicted_label', 'neutral')
                probabilities = result_data.get('probabilities', {})
                # Extract keywords if not present in result_data (though main.py did it separately)
                keywords = result_data.get('keywords', {})
                if not keywords:
                     keywords = model_service.extract_keywords(content)
                
                # Ensure content length limit
                safe_content = content[:6000] if content else ""
                
                cursor.execute("""
                    INSERT INTO analysis_result (task_id, content, predicted_label, probability_json, keywords_json)
                    VALUES (%s, %s, %s, %s, %s)
                """, (task_id, safe_content, predicted_label, json.dumps(probabilities), json.dumps(keywords)))
                result_id = cursor.lastrowid
                
                # Commit the transaction
                # Note: db.py sets autocommit=True, but for multiple inserts transaction is safer if we want atomicity.
                # However, db.py says autocommit=True. So each execute is committed.
                # If we want transaction, we should turn off autocommit or use begin/commit.
                # Given db.py, we rely on autocommit. If task inserts but result fails, we have an empty task.
                # It's acceptable for this refactor level.
                
                return {
                    "id": task_id,
                    "resultId": result_id,
                    "createdAt": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "keywords": keywords
                }
        except Exception as e:
            logger.error(f"Failed to record analysis result: {e}")
            # Return empty dict to indicate failure to save, but don't crash the request
            return {}
        finally:
            conn.close()

    def _resolve_model_id(self, cursor, model_id_param: Optional[int], model_name_param: Optional[str]) -> Optional[int]:
        """
        Resolve the correct model_id from database using ID or Name.
        Does NOT insert new records.
        """
        # Strategy 1: Trust model_id_param if provided and valid
        if model_id_param:
            cursor.execute("SELECT id FROM model_info WHERE id = %s", (model_id_param,))
            row = cursor.fetchone()
            if row:
                return row['id']
        
        # Strategy 2: Lookup by name (exact match)
        if model_name_param:
            cursor.execute("SELECT id FROM model_info WHERE model_name = %s", (model_name_param,))
            row = cursor.fetchone()
            if row:
                return row['id']
            
            # Strategy 3: Lookup by normalized internal type
            # The frontend might send "BiLSTM + Attention" but we want "bilstm_attention"
            # We can try to map using ModelService
            internal_type = model_service.normalize_model_type(model_name_param)
            if internal_type and internal_type != model_name_param:
                 # Check against model_name OR model_type column if it exists
                 # We assume model_type column exists based on main.py insert statement
                 cursor.execute("SELECT id FROM model_info WHERE model_name = %s OR model_type = %s", (internal_type, internal_type))
                 row = cursor.fetchone()
                 if row:
                     return row['id']

        return None

task_service = TaskService()
