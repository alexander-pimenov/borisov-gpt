### Udemy Course 
#### Spring AI или Весна Искусственного Интеллекта. От запуска локального LLM до RAG.
https://www.udemy.com/course/spring-ai-rag/

### Чему вы научитесь:
- Build a local Java microservice with Spring Boot and a running LLM that users can chat with.
- Integrate RAG (Retrieval-Augmented Generation) and connect your own data to improve response accuracy.
- Implement custom Advisors to support chat history and dynamic context injection.
- Optimize the model’s behavior through logs, prompt tuning, and advanced configuration for better results.

### Docker
### LLM Ollama
### RAG
### DB Postgres
### Application Spring AI


Так можно обратиться к запущенной Ollama с вопросом:
```bash
curl http://localhost:11431/api/generate \
-H "Content-Type: application/json" \
-d '{
  "model": "gemma3:4b-it-q4_K_M",
  "prompt": "Дай оригинальный текст песни Bohemian Rhapsody",
  "stream": false,
  "options": {
    "num_predict": 100,
    "temperature": 0.7,
    "top_k": 40,
    "top_p": 0.9,
    "repeat_penalty": 1.1
  }
}'

```

`"stream": false`       - параметр, с помощью которого ответ отдается в виде одного блока. А если `true` то в виде потока (стирима), гладко и комфортно.
`"num_predict": 100`    - количество символов, которое будет генерироваться и отдаваться в ответ.
`"temperature": 0.7`    - температура влияет на алгоритм выбора токена. При очень высокой температуре мы будем получать очень креативный ответ/вымысел, а при низкой - более предсказуемый.
`"top_k": 40`   - говорит из скольких токенов выбирать максимум. Тут выбирать максимум из 40.
`"top_p": 0.9`  - говорит о качестве: дошел до 90% ответов, то отдавай, не стоит еще перебирать 10%.
`"repeat_penalty": 1.1` - штраф за повторение токенов. Он влияет на любовь модели повторять одно и тоже.

---

Сравнение ChatGPT и Ollama.
- не нужно сравнивать их, т.к. ChatGPT намного мощнее Ollama, но мы можем изменить качество ответов Ollama, 
добавляя в контекст нужную специфическую информацию, возможно такую про которую ChatGPT даже не знает.
- например, для работы со Spring Ollama точно не нужно знать столицу Исландии или историю о 300 спартанцах. 
- а вот знания по Spring мы сами добавим в контекст.

---

Как оказывается, внутри веб сервиса Ollama может быть запущенно несколько моделей, и мы можем переключаться между ними в
процессе работы.

---

урок продолжить с видео 6 - переходим на стриминг


