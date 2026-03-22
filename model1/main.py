from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional, Dict
import logging
import os
# Allow multiple OpenMP runtimes (fixes 0xC0000005 crash on Windows)
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import json

# Import configuration and model service
try:
    from config import settings
except ImportError:
    from config_simple import settings
from app.model_service import model_service
from app.task_service import task_service
from app.file_api import router as file_router
from app.db import get_db_connection

# Set up logging
os.makedirs("logs", exist_ok=True)
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler(settings.log_file, encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Sentiment Analysis Service",
    description="Backend service for Chinese sentiment analysis using RoBERTa models",
    version="1.0.0",
    host=settings.host,
    port=settings.port
)

# Global Exception Handlers
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.error(f"Validation error for request {request.url}: {exc}")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors(), "body": exc.body},
    )

@app.on_event("startup")
async def startup_event():
    logger.info("Service is starting up...")
    # Pre-load the default model to avoid delay on first request
    try:
        model_service.load_model(settings.default_model)
        logger.info(f"Default model {settings.default_model} pre-loaded successfully.")
    except Exception as e:
        logger.warning(f"Could not pre-load default model: {e}")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=settings.cors_methods,
    allow_headers=settings.cors_headers,
)

# Include file_api router
app.include_router(file_router)

# Pydantic models for request/response
class SingleAnalysisRequest(BaseModel):
    content: str
    modelId: Optional[int] = None  # For compatibility with Spring Boot, but we'll ignore it
    modelName: Optional[str] = "roberta_base"  # Use this instead
    taskName: Optional[str] = None # Task name for history recording
    userId: Optional[int] = 1 # User ID for history tracking

class SingleAnalysisResponse(BaseModel):
    id: Optional[int] = None
    resultId: Optional[int] = None
    createdAt: Optional[str] = None
    predictedLabel: str
    probabilityJson: str  # JSON string of probabilities
    keywordsJson: str     # JSON string of keywords (we'll return empty for now)
    success: bool
    errorMessage: Optional[str] = None

class BatchAnalysisRequest(BaseModel):
    contents: List[str]
    modelId: Optional[int] = None
    modelName: Optional[str] = "roberta_base"

class BatchAnalysisResult(BaseModel):
    content: str
    predictedLabel: str
    probabilityJson: str
    keywordsJson: str

class BatchAnalysisResponse(BaseModel):
    results: List[BatchAnalysisResult]
    success: bool
    errorMessage: Optional[str] = None

# Endpoints
@app.post("/api/analysis/single", response_model=SingleAnalysisResponse)
async def analyze_single(request: SingleAnalysisRequest):
    try:
        if not request.content or not request.content.strip():
             return SingleAnalysisResponse(
                predictedLabel="neutral",
                probabilityJson="{}",
                keywordsJson="{}",
                success=False,
                errorMessage="Content cannot be empty"
            )
        
        # Use model_type from request
        result = model_service.predict(request.content, model_type=request.modelName)
        # Extract keywords
        keywords = model_service.extract_keywords(request.content)

        # Save to Database via TaskService
        saved_info = task_service.record_analysis_result(
            user_id=request.userId,
            content=request.content,
            model_identifier=request.modelName,
            model_id_param=request.modelId,
            result_data=result,
            task_name=getattr(request, 'taskName', None)
        )
        
        if saved_info:
            result.update(saved_info)
        
        return SingleAnalysisResponse(
            id=result.get('id'),
            resultId=result.get('resultId'),
            createdAt=result.get('createdAt'),
            predictedLabel=result.get('predicted_label', 'neutral'),
            probabilityJson=json.dumps(result.get('probabilities', {})),
            keywordsJson=json.dumps(keywords),
            success=True
        )
    except Exception as e:
        logger.error(f"Single analysis error: {str(e)}")
        return SingleAnalysisResponse(
            predictedLabel="error",
            probabilityJson="{}",
            keywordsJson="{}",
            success=False,
            errorMessage=str(e)
        )

@app.post("/api/analysis/batch", response_model=BatchAnalysisResponse)
async def analyze_batch(request: BatchAnalysisRequest):
    try:
        if not request.contents:
            return BatchAnalysisResponse(results=[], success=False, errorMessage="No contents provided")
            
        results = model_service.batch_predict(request.contents, model_type=request.modelName)
        
        response_results = []
        for res in results:
            keywords = model_service.extract_keywords(res.get('text', ''))
            response_results.append(BatchAnalysisResult(
                content=res.get('text', ''),
                predictedLabel=res.get('predicted_label', 'neutral'),
                probabilityJson=json.dumps(res.get('probabilities', {})),
                keywordsJson=json.dumps(keywords)
            ))
            
        return BatchAnalysisResponse(results=response_results, success=True)
    except Exception as e:
        logger.error(f"Batch analysis error: {str(e)}")
        return BatchAnalysisResponse(results=[], success=False, errorMessage=str(e))

@app.get("/api/models")
async def get_models():
    """Get available models"""
    try:
        # Return raw list for Java backend compatibility
        return model_service.get_available_models()
    except Exception as e:
        logger.error(f"Get models error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/models/list")
async def get_models_list():
    """Alias for get_models to match some frontend calls"""
    return await get_models()

@app.get("/api/visualization/data")
async def get_visualization_data(user_id: Optional[int] = None, limit: int = 1000):
    """Get visualization data for charts"""
    try:
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # Build query
                sql = """
                    SELECT r.id, r.content, r.predicted_label, r.probability_json, r.keywords_json, r.created_at,
                           t.id as task_id, u.username
                    FROM analysis_result r
                    JOIN analysis_task t ON r.task_id = t.id
                    LEFT JOIN user u ON t.user_id = u.id
                    WHERE 1=1
                """
                params = []
                
                if user_id:
                    sql += " AND t.user_id = %s"
                    params.append(user_id)
                
                # Order by time desc and limit
                sql += " ORDER BY r.created_at DESC LIMIT %s"
                params.append(limit)
                
                cursor.execute(sql, params)
                rows = cursor.fetchall()
                
                results = []
                for row in rows:
                    results.append({
                        "id": row['id'],
                        "content": row['content'],
                        "predictedLabel": row['predicted_label'],
                        "probabilityJson": row['probability_json'],
                        "keywordsJson": row['keywords_json'],
                        "createdAt": row['created_at'].isoformat() if row['created_at'] else None,
                        "taskId": row['task_id'],
                        "username": row['username'] or 'Unknown'
                    })
                
                return {
                    "code": 0,
                    "message": "success",
                    "data": results
                }
        finally:
            conn.close()
    except Exception as e:
        logger.error(f"Visualization data error: {e}")
        return {"code": 500, "message": str(e)}

@app.get("/api/visualization/wordcloud")
async def get_wordcloud_data(user_id: Optional[int] = None, top_k: int = 100):
    """Get word cloud data"""
    # This is a simplified version that returns empty for now, 
    # letting frontend calculate from visualization data if needed.
    # Or we can implement aggregation here if performance requires.
    return {
        "code": 0,
        "message": "success",
        "data": []
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=settings.host, port=settings.port)
