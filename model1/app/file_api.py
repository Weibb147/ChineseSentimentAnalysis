from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from typing import Optional, List
import pandas as pd
import os
import io
import json
import logging
import shutil
import traceback
import re
import unicodedata
from datetime import datetime
from app.db import get_db_connection
from app.model_service import model_service

router = APIRouter()
logger = logging.getLogger(__name__)

UPLOAD_DIR = "data/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

def clean_text_content(text):
    if not isinstance(text, str):
        return str(text)
    
    # 1. Normalize unicode (handles full-width/half-width, some char variants)
    text = unicodedata.normalize('NFKC', text)
    
    # 2. Remove URLs
    text = re.sub(r'(https|http)?://(\w|\.|\/|\?|\=|\&|\%)*\b', '', text, flags=re.MULTILINE)
    
    # 3. Remove email addresses
    text = re.sub(r'\S+@\S+', '', text)
    
    # 4. Remove @mentions (e.g., @user) - simplistic approach
    text = re.sub(r'@\S+', '', text)
    
    # 6. Remove HTML tags
    text = re.sub(r'<[^>]+>', '', text)

    # 7. Remove Emojis and Special Characters (Basic Range)
    # This regex covers many common emoji ranges and symbols
    try:
        # High surrogate pairs for emojis
        text = re.sub(r'[\U00010000-\U0010ffff]', '', text)
    except:
        pass # In case of regex compilation error on some systems

    # 8. Normalize whitespace (collapse multiple spaces/tabs/newlines to single space)
    text = re.sub(r'\s+', ' ', text).strip()
    
    return text

@router.post("/api/analysis/upload_predict")
async def upload_and_predict(
    file: UploadFile = File(...),
    user_id: Optional[int] = Form(1), # Default to 1 if missing
    model_name: str = Form("roberta_base"),
    task_name: Optional[str] = Form(None)
):
    """
    Upload file -> Parse -> Predict -> Save to DB
    """
    # Normalize model name for batch prediction (strip display names or descriptions if accidentally passed)
    # The frontend should pass the correct ID/Key, but let's be safe.
    
    # Ensure user_id_int is defined for DB operations
    try:
        user_id_int = int(user_id) if user_id is not None else 1
    except (ValueError, TypeError):
        user_id_int = 1

    model_name = model_service.normalize_model_type(model_name)

    valid_models = list(model_service.MODEL_CLASSES.keys())
    if model_name not in valid_models:
        logger.warning(f"Invalid model_name received: {model_name}, falling back to roberta_base")
        model_name = 'roberta_base'
            
    try:
        # 1. Save file
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        filename = f"{timestamp}_{file.filename}"
        file_path = os.path.join(UPLOAD_DIR, filename)
        
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        file_size_kb = os.path.getsize(file_path) // 1024
        
        # 2. Parse file
        texts = []
        try:
            # Read file efficiently based on extension
            # Strategy: Read with header=None first to capture all rows, then detect if first row is header
            if filename.endswith('.csv'):
                df = pd.read_csv(file_path, header=None)
            elif filename.endswith(('.xls', '.xlsx')):
                df = pd.read_excel(file_path, header=None)
            elif filename.endswith('.txt'):
                with open(file_path, 'r', encoding='utf-8') as f:
                    texts = [line.strip() for line in f if line.strip()]
                df = None
            else:
                raise HTTPException(status_code=400, detail="Unsupported file format")

            if df is not None:
                # Heuristic to detect header
                # Check if first row contains common header keywords
                first_row_values = [str(val).lower() for val in df.iloc[0].tolist()]
                potential_headers = ['content', 'text', 'comment', 'review', '内容', '文本', '评论']
                
                has_header = False
                for val in first_row_values:
                    if val in potential_headers:
                        has_header = True
                        break
                
                if has_header:
                    # If header detected, reload with header=0 (default)
                    if filename.endswith('.csv'):
                        df = pd.read_csv(file_path)
                    else:
                        df = pd.read_excel(file_path)
                    
                    # Find the text column
                    text_col = None
                    for col in potential_headers:
                        # Check columns case-insensitive
                        matching_col = next((c for c in df.columns if str(c).lower() == col), None)
                        if matching_col:
                            text_col = matching_col
                            break
                    
                    if not text_col:
                        text_col = df.columns[0] # Fallback to first column
                    
                    texts = df[text_col].dropna().astype(str).tolist()
                else:
                    # No header detected, treat all rows as data
                    # Use the first column (index 0)
                    texts = df.iloc[:, 0].dropna().astype(str).tolist()
                
        except Exception as e:
            logger.error(f"File parse error: {e}")
            raise HTTPException(status_code=400, detail=f"Failed to parse file: {str(e)}")

        if not texts:
            raise HTTPException(status_code=400, detail="No valid text found in file")

        # Clean texts
        texts = [clean_text_content(t) for t in texts if t and str(t).strip()]
        
        if not texts:
             raise HTTPException(status_code=400, detail="No valid text found after cleaning")

        # 3. DB Operations: Create Task (Transaction 1)
        # Use a short-lived connection for task creation
        conn = get_db_connection()
        task_id = None
        try:
            conn.begin() 
            with conn.cursor() as cursor:
                # 3.1 Insert file_upload
                # Handle file_type length (DB limit is 50 chars)
                safe_file_type = file.content_type
                if len(safe_file_type) > 50:
                    # Try to use extension or truncate
                    ext = os.path.splitext(file.filename)[1].lower().replace('.', '')
                    if ext in ['csv', 'xls', 'xlsx', 'txt']:
                         safe_file_type = f"application/{ext}"
                    else:
                         safe_file_type = safe_file_type[:50]
                
                sql_file = """
                    INSERT INTO file_upload (user_id, file_name, file_path, file_type, file_size_kb, status)
                    VALUES (%s, %s, %s, %s, %s, 'UPLOADED')
                """
                cursor.execute(sql_file, (user_id_int, file.filename, file_path, safe_file_type, file_size_kb))
                file_id = cursor.lastrowid
                
                # 3.2 Insert analysis_task
                if not task_name:
                    task_name = f"批量分析_{file.filename}_{timestamp}"
                
                # Check model_id
                cursor.execute("SELECT id FROM model_info WHERE model_name = %s OR model_type = %s LIMIT 1", (model_name, model_name))
                model_row = cursor.fetchone()
                
                if model_row:
                    model_id = model_row['id']
                else:
                    # Fallback to any active model if not found, DO NOT auto-register
                    logger.warning(f"Model {model_name} not found in DB. Trying fallback.")
                    cursor.execute("SELECT id FROM model_info WHERE status = 'ACTIVE' LIMIT 1")
                    fallback_row = cursor.fetchone()
                    if fallback_row:
                        model_id = fallback_row['id']
                        logger.info(f"Using fallback model id {model_id}")
                    else:
                        raise HTTPException(status_code=400, detail=f"Model {model_name} not found and no active models available.")
                
                sql_task = """
                    INSERT INTO analysis_task 
                    (user_id, model_id, file_id, task_name, task_type, source, status, total_count)
                    VALUES (%s, %s, %s, %s, 'BATCH', 'WEB', 'RUNNING', %s)
                """
                cursor.execute(sql_task, (user_id_int, model_id, file_id, task_name, len(texts)))
                task_id = cursor.lastrowid
                
                conn.commit() # Commit initial records so task is visible
                logger.info(f"Created task {task_id} for file {file.filename}")
        except Exception as e:
            conn.rollback()
            logger.error(f"Error creating task: {e}")
            raise HTTPException(status_code=500, detail=f"Database error during task creation: {str(e)}")
        finally:
            conn.close() # Close connection to release resources during prediction

        # 4. Predict (No DB connection held)
        try:
            results = model_service.batch_predict(texts, model_type=model_name)
            logger.info(f"Prediction completed for task {task_id}, got {len(results)} results")
        except Exception as e:
            # If prediction fails, we need to update task status to FAILED
            logger.error(f"Prediction failed: {e}")
            conn = get_db_connection()
            try:
                with conn.cursor() as cursor:
                    cursor.execute("UPDATE analysis_task SET status = 'FAILED' WHERE id = %s", (task_id,))
                conn.commit()
            except:
                pass
            finally:
                conn.close()
            raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")
            
        # 5. Save Results (Transaction 2)
        # Open a NEW connection for saving results
        conn = get_db_connection()
        try:
            conn.begin()
            with conn.cursor() as cursor:
                success_count = 0
                fail_count = 0
                
                result_values = []
                for res in results:
                    predicted_label = res.get('predicted_label', 'unknown')
                    probabilities = res.get('probabilities', {})
                    # Ensure JSON compatible
                    try:
                        keywords = model_service.extract_keywords(res.get('text', ''), top_k=5)
                    except:
                        keywords = {}
                    
                    if 'error' in res:
                        fail_count += 1
                    else:
                        success_count += 1
                        
                    result_values.append((
                        task_id,
                        res.get('text', '')[:6000], # Truncate if too long for TEXT
                        predicted_label,
                        json.dumps(probabilities),
                        json.dumps(keywords)
                    ))
                
                if not result_values:
                    logger.warning(f"No results to save for task {task_id}")
                else:
                    # Chunked insert
                    chunk_size = 100
                    sql_result = """
                        INSERT INTO analysis_result (task_id, content, predicted_label, probability_json, keywords_json)
                        VALUES (%s, %s, %s, %s, %s)
                    """
                    
                    for i in range(0, len(result_values), chunk_size):
                        chunk = result_values[i:i+chunk_size]
                        if chunk:
                            cursor.executemany(sql_result, chunk)
                    
                    logger.info(f"Inserted {len(result_values)} results for task {task_id}")

                # 6. Update Task Status
                sql_update = """
                    UPDATE analysis_task 
                    SET status = 'FINISHED', success_count = %s, fail_count = %s, finished_at = NOW()
                    WHERE id = %s
                """
                cursor.execute(sql_update, (success_count, fail_count, task_id))
                
                # Update File Status
                cursor.execute("UPDATE file_upload SET status = 'PARSED' WHERE id = %s", (file_id,))
                
                conn.commit()
                logger.info(f"Task {task_id} completed successfully")
                
                return {
                    "success": True,
                    "taskId": task_id,
                    "message": "Analysis completed successfully",
                    "total": len(texts),
                    "success_count": success_count
                }

        except Exception as e:
            conn.rollback()
            # Try to update task status to FAILED
            if task_id:
                try:
                    # Need a fresh cursor/connection if the current one is broken?
                    # But we are in the catch block of the same connection.
                    # Usually better to try with the same connection if it's still open, or new one if closed.
                    # For simplicity, try to reuse but if it fails, ignore.
                    with conn.cursor() as cursor:
                         cursor.execute("UPDATE analysis_task SET status = 'FAILED' WHERE id = %s", (task_id,))
                    conn.commit()
                except:
                    pass
            logger.error(f"DB Error saving results: {e}")
            logger.error(traceback.format_exc())
            raise HTTPException(status_code=500, detail=f"Database processing error: {str(e)}")
        finally:
            conn.close()

    except HTTPException as he:
        raise he
    except Exception as e:
        logger.error(f"Upload process error: {e}")
        logger.error(traceback.format_exc())
        print(f"CRITICAL UPLOAD ERROR: {e}")
        print(traceback.format_exc())
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/api/analysis/results/{task_id}")
async def get_task_results(task_id: int, pageNum: int = 1, pageSize: int = 10):
    """
    Get analysis results by task ID with pagination
    """
    try:
        conn = get_db_connection()
        with conn:
            with conn.cursor() as cursor:
                # Count total
                cursor.execute("SELECT COUNT(*) as total FROM analysis_result WHERE task_id = %s", (task_id,))
                total = cursor.fetchone()['total']
                
                # Fetch records
                offset = (pageNum - 1) * pageSize
                sql = """
                    SELECT id, content, predicted_label, probability_json, keywords_json, created_at
                    FROM analysis_result
                    WHERE task_id = %s
                    ORDER BY id ASC
                    LIMIT %s OFFSET %s
                """
                cursor.execute(sql, (task_id, pageSize, offset))
                rows = cursor.fetchall()
                
        records = []
        for row in rows:
            prob = {}
            if row['probability_json']:
                try:
                    prob = json.loads(row['probability_json'])
                except:
                    pass
            
            keywords = {}
            if row['keywords_json']:
                try:
                    keywords = json.loads(row['keywords_json'])
                except:
                    pass

            records.append({
                "id": row['id'],
                "content": row['content'],
                "predictedLabel": row['predicted_label'],
                "probability": prob,
                "keywords": keywords,
                "createdAt": row['created_at'].isoformat() if row['created_at'] else None
            })
            
        return {
            "code": 0,
            "message": "success",
            "data": {
                "records": records,
                "total": total,
                "pageNum": pageNum,
                "pageSize": pageSize
            }
        }
    except Exception as e:
        logger.error(f"Get task results error: {e}")
        return {
            "code": 500,
            "message": str(e)
        }
