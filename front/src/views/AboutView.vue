<template>
  <div class="about-container">
    <el-card>
      <template #header>
        <h2>关于情感分析平台</h2>
      </template>
      <div class="content">
        <p>这是一个基于BERT模型的中文文本情感分析平台。</p>
        <p>平台可以对中文文本进行情感分析，识别出积极、中性或消极的情感倾向。</p>
        <p>主要功能包括：</p>
        <ul>
          <li>实时情感分析</li>
          <li>分析历史记录查看</li>
          <li>用户账户管理</li>
        </ul>
        <p>技术栈：</p>
        <ul>
          <li>前端：Vue 3 + TypeScript + Element Plus</li>
          <li>后端：Spring Boot + MySQL</li>
          <li>模型：BERT中文情感分析模型</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.about-container {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 30px;
}

.content {
  line-height: 1.7;
  font-size: 16px;
  padding: 20px 0;
}

.content h2 {
  font-size: 1.8rem;
  margin: 20px 0;
  color: #303133;
}

.content p {
  margin-bottom: 20px;
}

.content ul {
  padding-left: 25px;
  margin-bottom: 25px;
}

.content li {
  margin-bottom: 12px;
}

.content li strong {
  color: #303133;
}

.technology-stack {
  margin-top: 30px;
  padding: 20px;
  background-color: #f6f8fa;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .about-container {
    padding: 0 15px;
  }
  
  .content {
    font-size: 15px;
    padding: 15px 0;
  }
  
  .content h2 {
    font-size: 1.5rem;
  }
  
  .technology-stack {
    padding: 15px;
    margin-top: 20px;
  }
}
</style>